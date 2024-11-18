package org.mineskin.request;

import org.mineskin.GenerateOptions;

public abstract class AbstractRequestBuilder implements RequestBuilder {

    private GenerateOptions options = GenerateOptions.create();

    @Override
    public RequestBuilder options(GenerateOptions options) {
        this.options = options;
        return this;
    }

    @Override
    public GenerateOptions getOptions() {
        return options;
    }

}
