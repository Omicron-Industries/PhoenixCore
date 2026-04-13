package net.phoenix.core.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixFissionBlocks;
import net.phoenix.core.common.block.entity.NukePrimedEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

public class NukePrimedRenderer extends EntityRenderer<NukePrimedEntity> {

    private static final ResourceLocation GLOW = PhoenixCore.id("textures/misc/nuke_glow.png");
    private static final ResourceLocation RING = PhoenixCore.id("textures/misc/nuke_ring.png");

    private final BlockRenderDispatcher blockRenderer;

    public NukePrimedRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(NukePrimedEntity e, float entityYaw, float partialTicks,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int packedLight) {
        int fuse = e.getFuse();
        float fuseF = fuse - partialTicks;

        float danger = 1.0f - (fuseF / 80.0f);
        danger = Mth.clamp(danger, 0.0f, 1.0f);

        float pulse = 0.5f + 0.5f * Mth.sin((e.tickCount + partialTicks) * (0.2f + danger * 1.2f));
        float pulseStrong = Mth.lerp(danger, 0.15f, 1.0f) * pulse;

        float baseScale = 1.4f;

        float swell = 1.0f;
        if (fuseF < 20.0f) {
            float t = 1.0f - fuseF / 20.0f;
            t = Mth.clamp(t, 0.0f, 1.0f);
            t *= t;
            swell = 1.0f + t * 0.8f;
        }

        boolean flash = fuseF < 60.0f && ((int) (fuseF / 2) % 2 == 0);

        ps.pushPose();

        ps.translate(0.0D, 0.25D, 0.0D);
        ps.scale(baseScale * swell, baseScale * swell, baseScale * swell);

        float spin = (e.tickCount + partialTicks) * (1.2f + 8.0f * danger);
        ps.mulPose(Axis.YP.rotationDegrees(spin));

        ps.pushPose();
        ps.translate(-0.5D, 0.0D, -0.5D);
        BlockState state = PhoenixFissionBlocks.NUKE_BLOCK.getDefaultState();
        blockRenderer.renderSingleBlock(state, ps, buffer, packedLight,
                flash ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10) : OverlayTexture.NO_OVERLAY);
        ps.popPose();

        ps.popPose();

        renderBillboardGlow(e, partialTicks, ps, buffer, danger, pulseStrong, flash);

        renderShockwaveRing(e, partialTicks, ps, buffer, danger);

        super.render(e, entityYaw, partialTicks, ps, buffer, packedLight);
    }

    private void renderBillboardGlow(NukePrimedEntity e, float partialTicks, PoseStack ps, MultiBufferSource buffer,
                                     float danger, float pulseStrong, boolean flash) {
        float alpha = Mth.clamp(0.15f + danger * 0.65f + pulseStrong * 0.35f, 0.0f, 1.0f);
        if (flash) alpha = 1.0f;

        float size = 1.2f + danger * 2.0f + pulseStrong * 0.8f;

        ps.pushPose();
        ps.translate(0.0D, 1.0D, 0.0D);

        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        ps.mulPose(cam.rotation());

        ps.scale(size, size, size);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(GLOW));
        PoseStack.Pose p = ps.last();

        addQuad(vc, p, alpha);

        ps.popPose();
    }

    private void renderShockwaveRing(NukePrimedEntity e, float partialTicks, PoseStack ps, MultiBufferSource buffer,
                                     float danger) {
        float t = Mth.clamp(danger, 0.0f, 1.0f);
        float radius = 0.5f + t * 10.0f;
        float alpha = (1.0f - t) * 0.25f + t * 0.55f;
        alpha = Mth.clamp(alpha, 0.0f, 0.8f);

        ps.pushPose();
        ps.translate(0.0D, 0.05D, 0.0D);
        ps.mulPose(Axis.XP.rotationDegrees(90.0f));
        ps.scale(radius, radius, radius);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(RING));
        PoseStack.Pose p = ps.last();

        addQuad(vc, p, alpha);

        ps.popPose();
    }

    private static void addQuad(VertexConsumer vc, PoseStack.Pose p, float a) {
        vc.vertex(p.pose(), -0.5f, -0.5f, 0.0f).color(255, 255, 255, (int) (a * 255)).uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(p.normal(), 0, 0, 1).endVertex();
        vc.vertex(p.pose(), 0.5f, -0.5f, 0.0f).color(255, 255, 255, (int) (a * 255)).uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(p.normal(), 0, 0, 1).endVertex();
        vc.vertex(p.pose(), 0.5f, 0.5f, 0.0f).color(255, 255, 255, (int) (a * 255)).uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(p.normal(), 0, 0, 1).endVertex();
        vc.vertex(p.pose(), -0.5f, 0.5f, 0.0f).color(255, 255, 255, (int) (a * 255)).uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(p.normal(), 0, 0, 1).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NukePrimedEntity entity) {
        return GLOW;
    }
}
