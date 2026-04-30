package net.phoenix.core.mixin;

import net.createmod.ponder.api.scene.SpecialInstructions;
import net.createmod.ponder.api.scene.WorldInstructions;
import net.createmod.ponder.foundation.PonderScene;
import net.phoenix.core.integration.ponder.util.SceneBuilderInternalAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.createmod.ponder.foundation.PonderSceneBuilder", remap = false)
public interface SceneBuilderAccessor extends SceneBuilderInternalAccess {

    @Accessor(value = "scene", remap = false)
    PonderScene ponderjs$getPonderScene();

    @Accessor(value = "world", remap = false)
    @Mutable
    void ponderjs$setWorldInstructions(WorldInstructions worldInstructions);

    @Accessor(value = "special", remap = false)
    @Mutable
    void ponderjs$setSpecialInstructions(SpecialInstructions specialInstructions);
}
