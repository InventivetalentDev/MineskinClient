package org.mineskin.request.url;

import org.mineskin.GenerateOptions;
import org.mineskin.request.RequestBuilder;

import java.net.URL;

public interface UrlRequestBuilder extends RequestBuilder {

    URL getUrl();

    @Override
    UrlRequestBuilder options(GenerateOptions options);
}
