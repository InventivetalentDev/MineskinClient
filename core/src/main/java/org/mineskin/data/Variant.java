package org.mineskin.data;

import com.google.gson.annotations.SerializedName;

public enum Variant {
    @SerializedName("")
    AUTO(""),
    @SerializedName("classic")
    CLASSIC("classic"),
    @SerializedName("slim")
    SLIM("slim");

    private final String name;

    Variant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
