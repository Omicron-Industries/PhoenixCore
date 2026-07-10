package net.phoenix.core.client.worldfx;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

/**
 * Snapshot of rendering state passed to every {@link PhoenixSkyLayer} during the sky pass.
 *
 * <p>The pose stack is already in camera space (translated to the camera position,
 * rotated for the camera pitch/yaw). Sky layers should NOT translate by the camera
 * position — just rotate to point at the sky feature you want to render.
 */
public record SkyRenderContext(
        PoseStack poseStack,
        Matrix4f projectionMatrix,
        float partialTick,
        Camera camera,
        /** Camera position in world space. Handy for view-direction math. */
        Vec3 cameraPos) {

    public SkyRenderContext(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera) {
        this(poseStack, projectionMatrix, partialTick, camera, camera.getPosition());
    }
}
