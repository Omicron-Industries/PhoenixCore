package net.phoenix.core.mixin;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.ponder.util.TagRegistryInternalAccess;

import com.google.common.collect.Multimap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.createmod.ponder.foundation.registration.PonderTagRegistry", remap = false)
public interface PonderTagRegistryAccessor extends TagRegistryInternalAccess {

    @Accessor(value = "componentTagMap", remap = false)
    Multimap<ResourceLocation, ResourceLocation> ponderjs$getComponentTagMap();
}
