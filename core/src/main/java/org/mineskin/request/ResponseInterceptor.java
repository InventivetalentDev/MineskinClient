package org.mineskin.request;

/**
 * Interceptor invoked after a response is received, before the body is parsed
 * or the response is checked for errors. The type parameter exposes the
 * underlying {@link RequestHandler}'s native response type.
 *
 * @param <R> native response type for the target {@link RequestHandler}
 */
@FunctionalInterface
public interface ResponseInterceptor<R> {

    void intercept(R response);

}
