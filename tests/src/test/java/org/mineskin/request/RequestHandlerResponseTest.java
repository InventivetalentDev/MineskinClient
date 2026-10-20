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

    // Structure of a real Cloudflare 502 error page, trimmed, hostnames and Ray ID replaced
    private static final String CLOUDFLARE_502_PAGE = """
            <!DOCTYPE html>
            <html class="no-js" lang="en-US">
            <head>
            <title>example.com | 502: Bad gateway</title>
            </head>
            <body>
            <div id="cf-wrapper">
                <div id="cf-error-details" class="p-0">
                    <header class="mx-auto pt-10 lg:pt-6 lg:px-8 w-240 lg:w-full mb-8">
                        <h1 class="inline-block sm:block sm:mb-2 font-light text-60 lg:text-4xl text-black-dark leading-tight mr-2">
                            <span class="inline-block">Bad gateway</span>
                            <span class="code-label">Error code 502</span>
                        </h1>
                        <div>
                            Visit <a href="https://www.cloudflare.com/5xx-error-landing?utm_source=errorcode_502&utm_campaign=example.com" target="_blank" rel="noopener noreferrer">cloudflare.com</a> for more information.
                        </div>
                    </header>
                    <div class="cf-error-footer cf-wrapper">
                        <p class="text-13">
                            <span class="cf-footer-item sm:block sm:mb-1">Cloudflare Ray ID: <strong class="font-semibold">0123456789abcdef</strong></span>
                        </p>
                    </div>
                </div>
            </div>
            </body>
            </html>
            """;

    // Real nginx error page
    private static final String NGINX_502_PAGE = """
            <html>
            <head><title>502 Bad Gateway</title></head>
            <body>
            <center><h1>502 Bad Gateway</h1></center>
            <hr><center>nginx/1.28.0</center>
            </body>
            </html>
            """;

    private static final Map<String, String> VIA_CLOUDFLARE = Map.of(
            "server", "cloudflare",
            "cf-ray", "a4da9b2dbeae3a73-FRA",
            "content-type", "text/html"
    );

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
    public void cloudflareCapturedPlainTextError() {
        // Real response from a Cloudflare edge for a hostname that isn't on Cloudflare
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(409, Map.of("server", "cloudflare", "cf-ray", "a4da9b2dbeae3a73-FRA", "content-type", "text/plain; charset=UTF-8"), "error code: 1001"));
        assertEquals(UnexpectedResponseException.Source.CLOUDFLARE, e.getSource());
        assertEquals(Optional.of(1001), e.getCloudflareErrorCode());
        assertFalse(e.isRetryable());
        assertEquals("HTTP 409 from Cloudflare (error 1001, ray a4da9b2dbeae3a73-FRA): error code: 1001", e.getMessage());
    }

    @Test
    public void cloudflare52xCodeComesFromStatus() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(521, Map.of(), ""));
        assertTrue(e.isCloudflare());
        assertEquals(Optional.of(521), e.getCloudflareErrorCode());
        assertTrue(e.isRetryable());
        assertEquals("HTTP 521 from Cloudflare (error 521): empty response body", e.getMessage());
    }

    @Test
    public void cloudflareGenerated502Page() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(502, Map.of("server", "cloudflare", "cf-ray", "0123456789abcdef-FRA", "content-type", "text/html; charset=UTF-8"), CLOUDFLARE_502_PAGE));
        assertEquals(UnexpectedResponseException.Source.CLOUDFLARE, e.getSource());
        assertEquals(Optional.of(502), e.getCloudflareErrorCode());
        assertEquals("example.com | 502: Bad gateway", e.getBodySnippet());
        assertTrue(e.isRetryable());
        assertEquals("HTTP 502 from Cloudflare (error 502, ray 0123456789abcdef-FRA): example.com | 502: Bad gateway", e.getMessage());
    }

    @Test
    public void cloudflare530CodeAllowsMarkup() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(530, VIA_CLOUDFLARE, "<html><body><span>Error code</span> <span>1033</span></body></html>"));
        assertEquals(Optional.of(1033), e.getCloudflareErrorCode());
    }

    @Test
    public void nginxPageBehindCloudflare() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(502, VIA_CLOUDFLARE, NGINX_502_PAGE));
        assertEquals(UnexpectedResponseException.Source.NGINX, e.getSource());
        assertFalse(e.isCloudflare());
        assertEquals(Optional.of("nginx/1.28.0"), e.getServerSoftware());
        assertEquals(Optional.of("a4da9b2dbeae3a73-FRA"), e.getCfRay());
        assertEquals(Optional.empty(), e.getCloudflareErrorCode());
        assertEquals("502 Bad Gateway", e.getBodySnippet());
        assertTrue(e.isRetryable());
        assertEquals("HTTP 502 from nginx/1.28.0 (ray a4da9b2dbeae3a73-FRA): 502 Bad Gateway", e.getMessage());
    }

    @Test
    public void nginxFromServerHeader() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(504, Map.of("server", "nginx/1.28.0"), ""));
        assertEquals(UnexpectedResponseException.Source.NGINX, e.getSource());
        assertEquals("HTTP 504 from nginx/1.28.0: empty response body", e.getMessage());
    }

    @Test
    public void originErrorThroughCloudflareIsNotCloudflare() {
        UnexpectedResponseException e = assertThrows(UnexpectedResponseException.class, () ->
                handler.handle(502, VIA_CLOUDFLARE, "Bad Gateway"));
        assertEquals(UnexpectedResponseException.Source.UNKNOWN, e.getSource());
        assertFalse(e.isCloudflare());
        assertEquals("HTTP 502 (ray a4da9b2dbeae3a73-FRA): Bad Gateway", e.getMessage());
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
