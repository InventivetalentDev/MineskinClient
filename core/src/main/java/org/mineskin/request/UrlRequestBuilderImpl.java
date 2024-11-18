package org.mineskin.request;

import java.net.URL;

public class UrlRequestBuilderImpl extends AbstractRequestBuilder implements UrlRequestBuilder {

    private final URL url;

    UrlRequestBuilderImpl(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

}
