package org.mineskin.request;

import org.mineskin.GenerateOptions;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;

public abstract class AbstractRequestBuilder implements RequestBuilder {

    private GenerateOptions options = GenerateOptions.create();

    @Override
    public RequestBuilder options(GenerateOptions options) {
        this.options = options;
        return this;
    }

    @Override
    public RequestBuilder visibility(Visibility visibility) {
        this.options.visibility(visibility);
        return this;
    }

    @Override
    public RequestBuilder variant(Variant variant) {
        this.options.variant(variant);
        return this;
    }

    @Override
    public RequestBuilder name(String name) {
        this.options.name(name);
        return this;
    }

    @Override
    public GenerateOptions options() {
        return options;
    }

}
