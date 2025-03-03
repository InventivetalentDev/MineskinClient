package org.mineskin.request;

import org.mineskin.GenerateOptions;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;

import java.util.UUID;

public abstract class AbstractRequestBuilder implements GenerateRequest {

    private GenerateOptions options = GenerateOptions.create();

    @Override
    public GenerateRequest options(GenerateOptions options) {
        this.options = options;
        return this;
    }

    @Override
    public GenerateRequest visibility(Visibility visibility) {
        this.options.visibility(visibility);
        return this;
    }

    @Override
    public GenerateRequest variant(Variant variant) {
        this.options.variant(variant);
        return this;
    }

    @Override
    public GenerateRequest name(String name) {
        this.options.name(name);
        return this;
    }

    @Override
    public GenerateRequest cape(UUID cape) {
        this.options.cape(cape);
        return this;
    }

    @Override
    public GenerateOptions options() {
        return options;
    }

}
