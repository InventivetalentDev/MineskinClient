package org.mineskin.request;

import org.mineskin.GenerateOptions;

import java.net.URL;

public interface UrlRequestBuilder extends RequestBuilder {

    URL getUrl();

    @Override
    UrlRequestBuilder options(GenerateOptions options);
}
