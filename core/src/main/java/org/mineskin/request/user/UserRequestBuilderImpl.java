package org.mineskin.request.user;

import org.mineskin.GenerateOptions;
import org.mineskin.request.AbstractRequestBuilder;

import java.util.UUID;

public class UserRequestBuilderImpl extends AbstractRequestBuilder implements UserRequestBuilder {

    private final UUID uuid;

    UserRequestBuilderImpl(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public UserRequestBuilder options(GenerateOptions options) {
        super.options(options);
        return this;
    }
}
