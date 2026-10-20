package org.mineskin.exception;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thrown when the API returns a response that isn't a MineSkin JSON object,
 * e.g. a Cloudflare error page (520-530) or an empty/plain-text proxy error.
 */
public class UnexpectedResponseException extends MineskinException {

    private static final int SNIPPET_LENGTH = 200;
    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CLOUDFLARE_ERROR_CODE = Pattern.compile("error code:?\\s*(\\d{3,4})", Pattern.CASE_INSENSITIVE);

    private final int status;
    private final String contentType;
    private final String bodySnippet;
    private final String cfRay;
    private final Integer cloudflareErrorCode;
    private final boolean cloudflare;
    private final Long retryAfterMillis;

    private UnexpectedResponseException(String message, @Nullable Throwable cause,
                                        int status, String contentType, String bodySnippet,
                                        String cfRay, Integer cloudflareErrorCode, boolean cloudflare,
                                        Long retryAfterMillis) {
        super(message, cause);
        this.status = status;
        this.contentType = contentType;
        this.bodySnippet = bodySnippet;
        this.cfRay = cfRay;
        this.cloudflareErrorCode = cloudflareErrorCode;
        this.cloudflare = cloudflare;
        this.retryAfterMillis = retryAfterMillis;
    }

    /**
     * @param headers response headers with lowercase keys
     */
    public static UnexpectedResponseException from(int status, Map<String, String> headers, @Nullable String body) {
        return from(status, headers, body, null);
    }

    /**
     * @param headers response headers with lowercase keys
     */
    public static UnexpectedResponseException from(int status, Map<String, String> headers, @Nullable String body, @Nullable Throwable cause) {
        String rawBody = body == null ? "" : body;
        String contentType = headers.get("content-type");
        String cfRay = headers.get("cf-ray");
        boolean cloudflare = cfRay != null
                || "cloudflare".equalsIgnoreCase(headers.get("server"))
                || (status >= 520 && status <= 530);
        Integer cloudflareErrorCode = null;
        if (cloudflare) {
            Matcher matcher = CLOUDFLARE_ERROR_CODE.matcher(rawBody);
            if (matcher.find()) {
                cloudflareErrorCode = Integer.parseInt(matcher.group(1));
            }
        }
        String snippet = summarize(rawBody);

        StringBuilder message = new StringBuilder("HTTP ").append(status);
        if (cloudflare) {
            message.append(" from Cloudflare");
            if (cloudflareErrorCode != null || cfRay != null) {
                message.append(" (");
                if (cloudflareErrorCode != null) {
                    message.append("error ").append(cloudflareErrorCode);
                }
                if (cfRay != null) {
                    if (cloudflareErrorCode != null) message.append(", ");
                    message.append("ray ").append(cfRay);
                }
                message.append(")");
            }
        }
        message.append(": ").append(snippet.isEmpty() ? "empty response body" : snippet);

        return new UnexpectedResponseException(message.toString(), cause,
                status, contentType, snippet, cfRay, cloudflareErrorCode, cloudflare,
                parseRetryAfter(headers.get("retry-after")));
    }

    private static String summarize(String body) {
        Matcher title = HTML_TITLE.matcher(body);
        String text = title.find() ? title.group(1) : body;
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > SNIPPET_LENGTH) {
            text = text.substring(0, SNIPPET_LENGTH) + "...";
        }
        return text;
    }

    private static Long parseRetryAfter(@Nullable String value) {
        if (value == null) return null;
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0 ? seconds * 1000 : null;
        } catch (NumberFormatException e) {
            // HTTP-date form isn't supported
            return null;
        }
    }

    public int getStatus() {
        return status;
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    /**
     * Shortened body, or the page title for HTML responses
     */
    public String getBodySnippet() {
        return bodySnippet;
    }

    public Optional<String> getCfRay() {
        return Optional.ofNullable(cfRay);
    }

    /**
     * Cloudflare error code from the body, e.g. 1033 for "error code: 1033"
     */
    public Optional<Integer> getCloudflareErrorCode() {
        return Optional.ofNullable(cloudflareErrorCode);
    }

    public boolean isCloudflare() {
        return cloudflare;
    }

    /**
     * Value of the Retry-After header in milliseconds, if present in seconds form
     */
    public Optional<Long> getRetryAfterMillis() {
        return Optional.ofNullable(retryAfterMillis);
    }

    /**
     * Whether the error is likely temporary (timeouts, rate limits, gateway and Cloudflare origin errors)
     */
    public boolean isRetryable() {
        return status == 408 || status == 429
                || (status >= 500 && status != 501 && status != 505);
    }

}
