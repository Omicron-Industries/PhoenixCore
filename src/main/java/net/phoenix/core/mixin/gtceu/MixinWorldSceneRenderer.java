package net.phoenix.core.mixin.gtceu;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;

import net.minecraft.client.Minecraft;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldSceneRenderer.class, remap = false)
public abstract class MixinWorldSceneRenderer {

    // 1. Create an Invoker to bypass 'protected' access
    @Invoker("clearView")
    public abstract void invoker$clearView(int x, int y, int width, int height);

    @Redirect(
              method = "setupCamera",
              at = @At(value = "INVOKE",
                       target = "Lcom/lowdragmc/lowdraglib/client/scene/WorldSceneRenderer;clearView(IIII)V"))
    private void phantasia$conditionalClear(WorldSceneRenderer instance, int x, int y, int width, int height) {
        if (Minecraft.getInstance().screen instanceof PhantasiaSceneScreen) {
            // Your custom logic for your specific screen
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(16640, Minecraft.ON_OSX);
        } else {
            // Use the invoker to call the original protected method
            this.invoker$clearView(x, y, width, height);
        }
    }

    @Redirect(
              method = "setupCamera",
              at = @At(value = "INVOKE",
                       target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexSorting;)V"))
    private void phantasia$conditionalProjection(Matrix4f proj, VertexSorting sorting) {
        if (Minecraft.getInstance().screen instanceof PhantasiaSceneScreen) {
            // Calculate your "Pure" camera matrix here if needed
            // Otherwise, just pass through the fixed one
            RenderSystem.setProjectionMatrix(proj, sorting);
        } else {
            RenderSystem.setProjectionMatrix(proj, sorting);
        }
    }
}
