package net.phoenix.core.integration.ponder.util;

import net.minecraft.resources.ResourceLocation;

import com.google.common.collect.Multimap;

// NOT a mixin — just a plain interface for casting
public interface TagRegistryInternalAccess {

    Multimap<ResourceLocation, ResourceLocation> ponderjs$getComponentTagMap();
}
