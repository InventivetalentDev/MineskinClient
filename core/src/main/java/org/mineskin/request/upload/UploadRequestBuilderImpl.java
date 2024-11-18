package org.mineskin.request.upload;

import org.mineskin.GenerateOptions;
import org.mineskin.MineSkinClient;
import org.mineskin.request.AbstractRequestBuilder;
import org.mineskin.request.source.UploadSource;
import org.mineskin.response.QueueResponse;

import java.util.concurrent.CompletableFuture;

public class UploadRequestBuilderImpl extends AbstractRequestBuilder implements UploadRequestBuilder {

    private final UploadSource uploadSource;

    UploadRequestBuilderImpl(UploadSource uploadSource) {
        this.uploadSource = uploadSource;
    }

    @Override
    public UploadSource getUploadSource() {
        return uploadSource;
    }

    @Override
    public UploadRequestBuilder options(GenerateOptions options) {
        super.options(options);
        return this;
    }

    public CompletableFuture<QueueResponse> queue(MineSkinClient client) {
        return client.queue(this);
    }


}
