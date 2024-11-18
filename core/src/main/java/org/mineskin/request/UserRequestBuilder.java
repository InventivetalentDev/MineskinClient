package org.mineskin.request;

import org.mineskin.GenerateOptions;

import java.util.UUID;

public interface UserRequestBuilder extends RequestBuilder{

    UUID getUuid();

    @Override
    UserRequestBuilder options(GenerateOptions options);
}
