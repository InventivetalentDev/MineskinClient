package org.mineskin.request;

import org.mineskin.GenerateOptions;
import org.mineskin.request.source.UploadSource;

public interface UploadRequestBuilder extends RequestBuilder {

    UploadSource getUploadSource();

    @Override
    UploadRequestBuilder options(GenerateOptions options);
}
