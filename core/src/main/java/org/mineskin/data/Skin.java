package org.mineskin.data;

public record Skin(
        String uuid,
        String name,
        SkinData data,
        long timestamp,
        int visibility,
        int views
) {
}
