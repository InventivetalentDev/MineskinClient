package org.mineskin.request;

import org.mineskin.GenerateOptions;
import org.mineskin.request.source.UploadSource;
import org.mineskin.request.upload.UploadRequestBuilderImpl;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.InputStream;

public interface RequestBuilder {

    static RequestBuilder upload(UploadSource uploadSource) {
        return new UploadRequestBuilderImpl(uploadSource);
    }

    static RequestBuilder upload(InputStream inputStream) {
        return upload(UploadSource.of(inputStream));
    }

    static RequestBuilder upload(File file) {
        return upload(UploadSource.of(file));
    }

    static RequestBuilder upload(RenderedImage renderedImage) {
        return upload(UploadSource.of(renderedImage));
    }

    RequestBuilder options(GenerateOptions options);

    GenerateOptions getOptions();

}