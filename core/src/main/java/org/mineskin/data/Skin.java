package org.mineskin.data;

public interface Skin {

    String uuid();

    String name();

    SkinData data();

    long timestamp();

    int visibility();

    int views();

}
