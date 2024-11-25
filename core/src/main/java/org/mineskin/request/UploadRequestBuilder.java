package org.mineskin.request;

import org.mineskin.request.source.UploadSource;

public interface UploadRequestBuilder extends GenerateRequest {

    UploadSource getUploadSource();

}
