package org.mineskin.request.user;

import org.mineskin.GenerateOptions;
import org.mineskin.request.RequestBuilder;

import java.util.UUID;

public interface UserRequestBuilder extends RequestBuilder {

    UUID getUuid();

    @Override
    UserRequestBuilder options(GenerateOptions options);
}
