package org.mineskin.request;

/**
 * Interceptor invoked just before a request is sent. The type parameter exposes
 * the underlying {@link RequestHandler}'s native request type (e.g.
 * {@code HttpRequest.Builder} for the Java 11 handler, {@code HttpUriRequest}
 * for the Apache handler, {@code org.jsoup.Connection} for the Jsoup handler).
 *
 * @param <R> native request type for the target {@link RequestHandler}
 */
@FunctionalInterface
public interface RequestInterceptor<R> {

    void intercept(R request);

}
