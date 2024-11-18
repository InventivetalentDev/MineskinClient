package org.mineskin.request;

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

}
