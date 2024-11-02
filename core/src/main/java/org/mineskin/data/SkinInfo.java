package org.mineskin.data;

import java.util.Objects;

public class SkinInfo implements Skin{

    private final String uuid;
    private final String name;
    private final Variant variant;
    private final Visibility visibility;
    private final TextureInfo texture;
    private final GeneratorInfo generator;

    private final int views;
    private final boolean duplicate;

    public SkinInfo(String uuid, String name, Variant variant, Visibility visibility, TextureInfo texture, GeneratorInfo generator, int views, boolean duplicate) {
        this.uuid = uuid;
        this.name = name;
        this.variant = variant;
        this.visibility = visibility;
        this.texture = texture;
        this.generator = generator;
        this.views = views;
        this.duplicate = duplicate;
    }

    @Override
    public String uuid() {
        return uuid;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Visibility visibility() {
        return visibility;
    }

    @Override
    public int views() {
        return views;
    }


}
