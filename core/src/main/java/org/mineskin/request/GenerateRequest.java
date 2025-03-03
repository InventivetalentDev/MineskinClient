package org.mineskin.request;

import org.mineskin.GenerateOptions;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.source.UploadSource;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public interface GenerateRequest {

    ///

    static UploadRequestBuilder upload(UploadSource uploadSource) {
        return new UploadRequestBuilderImpl(uploadSource);
    }

    static UploadRequestBuilder upload(InputStream inputStream) {
        return upload(UploadSource.of(inputStream));
    }

    static UploadRequestBuilder upload(File file) {
        return upload(UploadSource.of(file));
    }

    static UploadRequestBuilder upload(RenderedImage renderedImage) {
        return upload(UploadSource.of(renderedImage));
    }

    ///

    static UrlRequestBuilder url(URL url) {
        return new UrlRequestBuilderImpl(url);
    }

    static UrlRequestBuilder url(URI uri) throws MalformedURLException {
        return url(uri.toURL());
    }

    static UrlRequestBuilder url(String url) throws MalformedURLException {
        return url(URI.create(url));
    }

    ///

    static UserRequestBuilder user(UUID uuid) {
        return new UserRequestBuilderImpl(uuid);
    }

    static UserRequestBuilder user(String uuid) {
        return user(UUID.fromString(uuid));
    }

    ///

    GenerateRequest options(GenerateOptions options);

    GenerateRequest visibility(Visibility visibility);

    GenerateRequest variant(Variant variant);

    GenerateRequest name(String name);

    GenerateRequest cape(UUID cape);

    GenerateOptions options();

}