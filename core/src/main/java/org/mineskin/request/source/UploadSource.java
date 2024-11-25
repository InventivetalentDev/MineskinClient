package org.mineskin.request.source;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface UploadSource {

    static UploadSource of(InputStream inputStream) {
        return new InputStreamUploadSource(inputStream);
    }

    static UploadSource of(File file) {
        return new FileUploadSource(file);
    }

    static UploadSource of(RenderedImage renderedImage) {
        return new RenderedImageUploadSource(renderedImage);
    }

    InputStream getInputStream() throws IOException;

}
