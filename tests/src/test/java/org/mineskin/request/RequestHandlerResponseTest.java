package org.mineskin.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.mineskin.data.UserInfo;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.exception.UnexpectedResponseException;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;
import org.mineskin.response.UserResponseImpl;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RequestHandlerResponseTest {

    private static final String CLOUDFLARE_521_PAGE = """
            <!DOCTYPE html>
            <html lang="en-US">
            <head>
                <title>api.mineskin.org | 521: Web server is down</title>
            </head>
            <body>
                <span class="code-label">Error code 521</span>
            </body>
            </html>
            """;

    private final TestHandler handler = new TestHandler();

    @Test
    public void validJsonIsParsed() {
        UserResponseImpl response = handler.handle(200, Map.of(), "{\"success\":true,\"user\":\"abc\",\"grants\":{}}");
        assertTrue(response.isSuccess());
        assertEquals("abc", response.getUser().uuid());
    }

    @Test
    public void jsonErrorStillThrowsRequestException() {
        MineSkinRequestException e = assertThrows(MineSkinRequestException.class, () ->
                handler.handle(401, Map.of(), "{\"success\":false,\"errors\":[{\"code\":\"invalid_api_key\",\"message\":\"Invalid API key\"}]}"));
        assertEquals("Invalid API key", e.getMessage());
        assertEquals(401, e.getResponse().getStatus());
    }

    @Test
    public void cloudflarePlainTextError() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(530, Map.of("server", "cloudflare", "cf-ray", "8f3a1b2c3d4e5f60-FRA", "content-type", "text/plain"), "error code: 1033"));
        assertEquals(530, e.getStatus());
        assertTrue(e.isCloudflare());
        assertTrue(e.isRetryable());
        assertEquals(Optional.of(1033), e.getCloudflareErrorCode());
        assertEquals(Optional.of("8f3a1b2c3d4e5f60-FRA"), e.getCfRay());
        assertEquals(Optional.of("text/plain"), e.getContentType());
        assertEquals("HTTP 530 from Cloudflare (error 1033, ray 8f3a1b2c3d4e5f60-FRA): error code: 1033", e.getMessage());
    }

    @Test
    public void cloudflareHtmlPageUsesTitle() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(521, Map.of("server", "cloudflare", "content-type", "text/html"), CLOUDFLARE_521_PAGE));
        assertTrue(e.isCloudflare());
        assertEquals(Optional.of(521), e.getCloudflareErrorCode());
        assertEquals("api.mineskin.org | 521: Web server is down", e.getBodySnippet());
        assertEquals("HTTP 521 from Cloudflare (error 521): api.mineskin.org | 521: Web server is down", e.getMessage());
    }

    @Test
    public void emptyBody() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(502, Map.of(), ""));
        assertFalse(e.isCloudflare());
        assertTrue(e.isRetryable());
        assertEquals("HTTP 502: empty response body", e.getMessage());

        assertThrows(UnexpectedResponseException.class, () -> handler.handle(502, Map.of(), null));
    }

    @Test
    public void truncatedJsonKeepsParseCause() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(200, Map.of(), "{\"success\":true,\"user\":"));
        assertNotNull(e.getCause());
        assertFalse(e.isRetryable());
    }

    @Test
    public void nonObjectJsonIsUnexpected() {
        assertThrows(UnexpectedResponseException.class, () -> handler.handle(200, Map.of(), "[1, 2, 3]"));
        assertThrows(UnexpectedResponseException.class, () -> handler.handle(200, Map.of(), "\"ok\""));
    }

    @Test
    public void longBodyIsTruncated() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(500, Map.of(), "x".repeat(1000)));
        assertEquals(203, e.getBodySnippet().length());
    }

    @Test
    public void retryAfterHeaderIsParsed() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(503, Map.of("retry-after", "30"), "Service Unavailable"));
        assertEquals(Optional.of(30_000L), e.getRetryAfterMillis());

        UnexpectedResponseException dateForm = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(503, Map.of("retry-after", "Wed, 21 Oct 2026 07:28:00 GMT"), "Service Unavailable"));
        assertEquals(Optional.empty(), dateForm.getRetryAfterMillis());
    }

    @Test
    public void unexpectedResponseIsMineskinException() {
        // existing callers catching MineskinException for parse failures keep working
        assertThrows(MineskinException.class, () -> handler.handle(530, Map.of(), "error code: 1033"));
    }

    private static class TestHandler extends RequestHandler {

        TestHandler() {
            super("", "test", null, 0, new Gson());
        }

        UserResponseImpl handle(int status, Map<String, String> headers, String body) {
            return handleResponse(status, headers, body, UserInfo.class, UserResponseImpl::new);
        }

        @Override
        public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) {
            throw new UnsupportedOperationException();
        }
    }

}
