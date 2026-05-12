package net.phoenix.core.mixin.gtceu;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.utils.PositionedRect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WorldSceneRenderer.class, remap = false)
public abstract class MixinWorldSceneRenderer {

    @Shadow private Vector3f eyePos;
    @Shadow private Vector3f lookAt;
    @Shadow
    private Vector3f worldUp;
    @Shadow protected boolean ortho;

    /**
     * @author Phantasia
     * @reason Fixes the vertical offset by providing a "Pure" camera matrix
     * and bypasses the expensive setupCamera calls that cause stutter.
     */
    @Overwrite
    protected void setupCamera(PositionedRect viewport) {
        int x = viewport.getPosition().x;
        int y = viewport.getPosition().y;
        int width = viewport.getSize().width;
        int height = viewport.getSize().height;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.viewport(x, y, width, height);
        RenderSystem.depthMask(true);

        // CLEARING: Use the fixed clear view logic
        int clearColor = 0; // Or fetch from shadow
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(16640, Minecraft.ON_OSX);

        RenderSystem.backupProjectionMatrix();
        float aspectRatio = (float)width / (float)height;

        // OFFSET FIX: We define a very specific perspective matrix that
        // doesn't allow LDLib to "nudge" the Y-offset.
        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(60.0f), aspectRatio, 0.05f, 1000.0f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.byDistance(0, 0, 0));

        PoseStack posesStack = RenderSystem.getModelViewStack();
        posesStack.pushPose();
        posesStack.setIdentity();

        // Use the native JOML lookAt for maximum precision
        posesStack.last().pose().lookAt(
                eyePos.x, eyePos.y, eyePos.z,
                lookAt.x, lookAt.y, lookAt.z,
                worldUp.x, worldUp.y, worldUp.z
        );

        RenderSystem.applyModelViewMatrix();
    }
}