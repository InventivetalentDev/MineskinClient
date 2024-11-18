package org.mineskin.request.source;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamUploadSource implements UploadSource {

    private final InputStream inputStream;

    InputStreamUploadSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }
}
