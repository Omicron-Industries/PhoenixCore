package net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeslaHighlightRenderer {

    private static final Map<BlockPos, Long> HIGHLIGHT_MAP = new ConcurrentHashMap<>();

    private static class ShardAccessor extends RenderStateShard {

        public ShardAccessor(String name, Runnable setup, Runnable clear) {
            super(name, setup, clear);
        }

        public static class LineState extends LineStateShard {

            public LineState(double width) {
                super(OptionalDouble.of(width));
            }
        }

        public static class DepthState extends DepthTestStateShard {

            public DepthState(String function, int glValue) {
                super(function, glValue);
            }
        }
    }

    private static final RenderType XRAY_LINES = RenderType.create(
            "tesla_xray_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setLineState(new ShardAccessor.LineState(3.0D))
                    .setLayeringState(new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
                        PoseStack posestack = RenderSystem.getModelViewStack();
                        posestack.pushPose();
                        posestack.scale(0.99975F, 0.99975F, 0.99975F);
                        RenderSystem.applyModelViewMatrix();
                    }, () -> {
                        RenderSystem.getModelViewStack().popPose();
                        RenderSystem.applyModelViewMatrix();
                    }))
                    .setTransparencyState(
                            new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.defaultBlendFunc();
                            }, RenderSystem::disableBlend))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setDepthTestState(new ShardAccessor.DepthState("always", 519))
                    .createCompositeState(false));

    public static void highlight(BlockPos pos, int durationTicks) {
        HIGHLIGHT_MAP.put(pos, System.currentTimeMillis() + (durationTicks * 50L));
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (HIGHLIGHT_MAP.isEmpty()) return;

        long now = System.currentTimeMillis();
        HIGHLIGHT_MAP.entrySet().removeIf(entry -> now > entry.getValue());

        PoseStack stack = event.getPoseStack();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        for (BlockPos pos : HIGHLIGHT_MAP.keySet()) {
            stack.pushPose();

            stack.translate(pos.getX() - cam.x(),
                    pos.getY() - cam.y(),
                    pos.getZ() - cam.z());

            renderGlowingBox(stack);

            stack.popPose();
        }
    }

    private static void renderGlowingBox(PoseStack stack) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = 0.74f, g = 0.0f, b = 1.0f, a = 1.0f;
        drawBox(stack, bufferbuilder, 0, 0, 0, 1, 1, 1, r, g, b, a);

        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void drawBox(PoseStack matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2,
                                float y2, float z2, float r, float g, float b, float a) {
        var pose = matrix.last().pose();

        line(pose, buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(pose, buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(pose, buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(pose, buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);

        line(pose, buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(pose, buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(pose, buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(pose, buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);

        line(pose, buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(pose, buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(pose, buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(pose, buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(Matrix4f pose, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2,
                             float z2, float r, float g, float b, float a) {
        buffer.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
