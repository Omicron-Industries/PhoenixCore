package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.phoenix.core.client.worldfx.SkyRenderContext;
import net.phoenix.core.client.worldfx.WorldFXManager;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At("RETURN"))
    private void phoenixcore$onSkyRendered(PoseStack poseStack, Matrix4f projectionMatrix,
                                           float partialTick, Camera camera,
                                           boolean isFoggy, Runnable setupFog,
                                           CallbackInfo ci) {
        WorldFXManager.renderSkyLayers(new SkyRenderContext(poseStack, projectionMatrix, partialTick, camera));
    }
}
