package org.mineskin.exception;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thrown when the API returns a response that isn't a MineSkin JSON object,
 * e.g. a Cloudflare error page (520-530, 1xxx), an nginx 502 page or an empty/plain-text proxy error.
 */
public class UnexpectedResponseException extends MineskinException {

    /**
     * What generated the error response
     */
    public enum Source {
        /**
         * Cloudflare itself, e.g. 521 (origin down) or 530 with error 1033
         */
        CLOUDFLARE,
        /**
         * An nginx error page, e.g. a 502 from the reverse proxy in front of the API
         */
        NGINX,
        UNKNOWN
    }

    private static final int SNIPPET_LENGTH = 200;
    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // "error code: 1033" (plain text), also tolerates markup between label and number in HTML pages
    private static final Pattern CLOUDFLARE_ERROR_CODE = Pattern.compile("error code:?\\s*(?:<[^>]*>\\s*)*(\\d{3,4})", Pattern.CASE_INSENSITIVE);
    // nginx error page footer: <hr><center>nginx/1.28.0</center>
    private static final Pattern NGINX_FOOTER = Pattern.compile("<center>\\s*(nginx(?:/[\\w.]+)?)\\s*</center>", Pattern.CASE_INSENSITIVE);

    private final int status;
    private final String contentType;
    private final String bodySnippet;
    private final Source source;
    private final String serverSoftware;
    private final String cfRay;
    private final Integer cloudflareErrorCode;
    private final Long retryAfterMillis;

    private UnexpectedResponseException(String message, @Nullable Throwable cause,
                                        int status, String contentType, String bodySnippet,
                                        Source source, String serverSoftware,
                                        String cfRay, Integer cloudflareErrorCode,
                                        Long retryAfterMillis) {
        super(message, cause);
        this.status = status;
        this.contentType = contentType;
        this.bodySnippet = bodySnippet;
        this.source = source;
        this.serverSoftware = serverSoftware;
        this.cfRay = cfRay;
        this.cloudflareErrorCode = cloudflareErrorCode;
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
        String serverHeader = headers.get("server");
        String cfRay = headers.get("cf-ray");
        // Behind Cloudflare every response has these headers, so they only say the response
        // passed through Cloudflare, not that Cloudflare generated it
        boolean viaCloudflare = cfRay != null || "cloudflare".equalsIgnoreCase(serverHeader);

        Source source = Source.UNKNOWN;
        String serverSoftware = null;
        Integer cloudflareErrorCode = null;

        Matcher nginxFooter = NGINX_FOOTER.matcher(rawBody);
        Matcher errorCode = CLOUDFLARE_ERROR_CODE.matcher(rawBody);
        if (nginxFooter.find()) {
            source = Source.NGINX;
            serverSoftware = nginxFooter.group(1);
        } else if (serverHeader != null && serverHeader.toLowerCase().startsWith("nginx")) {
            source = Source.NGINX;
            serverSoftware = serverHeader;
        } else if (status >= 520 && status <= 530) {
            source = Source.CLOUDFLARE;
            if (status <= 527) {
                cloudflareErrorCode = status;
            } else if (errorCode.find()) {
                // 530 wraps a 1xxx error, e.g. 1033
                cloudflareErrorCode = Integer.parseInt(errorCode.group(1));
            }
        } else if (viaCloudflare && errorCode.find()) {
            // e.g. 409 "error code: 1001"
            source = Source.CLOUDFLARE;
            cloudflareErrorCode = Integer.parseInt(errorCode.group(1));
        }
        String snippet = summarize(rawBody);

        StringBuilder message = new StringBuilder("HTTP ").append(status);
        if (source == Source.CLOUDFLARE) {
            message.append(" from Cloudflare");
        } else if (source == Source.NGINX) {
            message.append(" from ").append(serverSoftware);
        }
        List<String> details = new ArrayList<>();
        if (cloudflareErrorCode != null) details.add("error " + cloudflareErrorCode);
        if (cfRay != null) details.add("ray " + cfRay);
        if (!details.isEmpty()) {
            message.append(" (").append(String.join(", ", details)).append(")");
        }
        message.append(": ").append(snippet.isEmpty() ? "empty response body" : snippet);

        return new UnexpectedResponseException(message.toString(), cause,
                status, contentType, snippet, source, serverSoftware, cfRay, cloudflareErrorCode,
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

    public Source getSource() {
        return source;
    }

    /**
     * Server software that generated the error page, e.g. "nginx/1.28.0"
     */
    public Optional<String> getServerSoftware() {
        return Optional.ofNullable(serverSoftware);
    }

    /**
     * Cloudflare Ray ID, present on every response that passed through Cloudflare (not only Cloudflare-generated errors)
     */
    public Optional<String> getCfRay() {
        return Optional.ofNullable(cfRay);
    }

    /**
     * Cloudflare error code, e.g. 521, or 1033 for a 530 with "error code: 1033"
     */
    public Optional<Integer> getCloudflareErrorCode() {
        return Optional.ofNullable(cloudflareErrorCode);
    }

    /**
     * Whether Cloudflare itself generated the error.
     * Errors from the origin (e.g. an nginx 502) passed through Cloudflare return false, see {@link #getCfRay()}
     */
    public boolean isCloudflare() {
        return source == Source.CLOUDFLARE;
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
