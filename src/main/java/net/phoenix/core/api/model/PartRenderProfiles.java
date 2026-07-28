package net.phoenix.core.api.model;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PartRenderProfiles {

    private static final Map<ResourceLocation, PartRenderProfile> REGISTRY = new LinkedHashMap<>();

    public static void register(ResourceLocation id, PartRenderProfile profile) {
        REGISTRY.put(id, profile);
    }

    @Nullable
    public static PartRenderProfile get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static Map<ResourceLocation, PartRenderProfile> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private PartRenderProfiles() {}
}
