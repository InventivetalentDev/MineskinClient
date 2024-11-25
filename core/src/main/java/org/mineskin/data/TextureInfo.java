package org.mineskin.data;

public record TextureInfo(
        ValueAndSignature data,
        SkinHashes hash,
        SkinUrls url
) {
}
