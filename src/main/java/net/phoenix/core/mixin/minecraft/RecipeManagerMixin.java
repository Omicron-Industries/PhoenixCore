package net.phoenix.core.mixin.minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.phoenix.core.api.RecipeBlacklist;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RecipeManager.class, priority = 100)
public abstract class RecipeManagerMixin {

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void phoenix$onApplyHead(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                     ProfilerFiller profiler, CallbackInfo ci) {
        RecipeBlacklist.load();

        map.entrySet().removeIf(entry -> RecipeBlacklist.shouldRemoveRaw(entry.getKey(), entry.getValue()));
    }
}
