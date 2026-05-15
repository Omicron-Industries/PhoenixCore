package net.phoenix.core.mixin.gtceu;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSceneRenderer.class)
public interface AccessorWorldSceneRenderer {

    @Accessor(value = "endBatchLast", remap = false)
    void setEndBatchLast(boolean value);

    @Accessor(value = "endBatchLast", remap = false)
    boolean getEndBatchLast();
}
