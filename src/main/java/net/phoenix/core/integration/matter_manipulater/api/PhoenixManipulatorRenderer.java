package net.phoenix.core.integration.matter_manipulater.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

public class PhoenixManipulatorRenderer {

    public static void renderSelection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof PhoenixManipulatorItem tool)) return;

        BlockPos start = tool.getStartPos(stack);
        BlockPos end = tool.getEndPos(stack);
        if (start == null || end == null) return;

        PhoenixManipulatorMode mode = tool.getMode(stack);
        List<BlockPos> targets = PhoenixPlacementEngine.getTargetPositions(start, end, mode);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        float r = 0.2f, g = 1.0f, b = 1.0f, a = 0.4f;
        if (mode == PhoenixManipulatorMode.CONNECT_ONLY) {
            r = 0.2f;
            g = 1.0f;
            b = 0.2f;
        }

        for (BlockPos pos : targets) {
            LevelRenderer.renderLineBox(poseStack, buffer,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    r, g, b, a);
        }

        drawBoundingBox(poseStack, buffer, start, end, 1.0f, 1.0f, 1.0f, 0.8f);

        poseStack.popPose();
    }

    private static void drawBoundingBox(PoseStack matrix, VertexConsumer buffer, BlockPos start, BlockPos end, float r,
                                        float g, float b, float a) {
        double minX = Math.min(start.getX(), end.getX());
        double minY = Math.min(start.getY(), end.getY());
        double minZ = Math.min(start.getZ(), end.getZ());
        double maxX = Math.max(start.getX(), end.getX()) + 1.05;
        double maxY = Math.max(start.getY(), end.getY()) + 1.05;
        double maxZ = Math.max(start.getZ(), end.getZ()) + 1.05;

        LevelRenderer.renderLineBox(matrix, buffer, minX - 0.05, minY - 0.05, minZ - 0.05, maxX, maxY, maxZ, r, g, b,
                a);
    }
}
