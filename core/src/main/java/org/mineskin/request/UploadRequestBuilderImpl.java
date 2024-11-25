package org.mineskin.request;

import org.mineskin.request.source.UploadSource;

public class UploadRequestBuilderImpl extends AbstractRequestBuilder implements UploadRequestBuilder {

    private final UploadSource uploadSource;

    UploadRequestBuilderImpl(UploadSource uploadSource) {
        this.uploadSource = uploadSource;
    }

    @Override
    public UploadSource getUploadSource() {
        return uploadSource;
    }
}
