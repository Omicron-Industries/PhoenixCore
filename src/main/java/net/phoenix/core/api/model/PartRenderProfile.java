package net.phoenix.core.api.model;

import org.jetbrains.annotations.Nullable;

public record PartRenderProfile(@Nullable String casingTexturePath, String overlayName) {

    public static PartRenderProfile tieredHull(String overlayName) {
        return new PartRenderProfile(null, overlayName);
    }

    public static PartRenderProfile customCasing(String casingTexturePath, String overlayName) {
        return new PartRenderProfile(casingTexturePath, overlayName);
    }
}
