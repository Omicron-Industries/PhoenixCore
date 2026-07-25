package net.phoenix.core.client.worldfx;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

public record SkyRenderContext(
                               PoseStack poseStack,
                               Matrix4f projectionMatrix,
                               float partialTick,
                               Camera camera,

                               Vec3 cameraPos) {

    public SkyRenderContext(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera) {
        this(poseStack, projectionMatrix, partialTick, camera, camera.getPosition());
    }
}
