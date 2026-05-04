package net.phoenix.core.integration.phantasia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomSceneRenderer {

    private final PhantasiaFakeLevel world;
    private RenderTarget fbo;
    private int resolutionWidth, resolutionHeight;

    private Vector3f eyePos = new Vector3f(0, 0, 10f);
    private Vector3f lookAt = new Vector3f(0, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);
    private final float fov = 70.0f;
    private int clearColor = 0xFF0B0B10;

    /**
     * Passed to {@code model.getModelData()} so CTM and LDLib overlay systems can
     * read REAL neighbor block states when computing connection bitmasks.
     *
     * This view returns the real delegate state for every position, which is what
     * CTM needs. It is NOT used for face-culling — that is handled by CullRenderView.
     */
    private static class CtmRenderView implements BlockAndTintGetter {

        private final PhantasiaFakeLevel delegate;

        CtmRenderView(PhantasiaFakeLevel delegate) {
            this.delegate = delegate;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return delegate.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return delegate.getBlockEntity(pos);
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return delegate.getMinBuildHeight();
        }

        @Override
        public float getShade(Direction dir, boolean shade) {
            return 1.0f;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return delegate.getLightEngine();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return delegate.getBlockTint(pos, resolver);
        }
    }

    /**
     * Passed to {@code tesselateBlock()} for face-culling evaluation only.
     *
     * Returns the real BlockState for the block currently being rendered
     * ({@code currentRenderPos}) so model selection and tinting are correct.
     * Returns AIR for every other position so that
     * {@code BlockState.skipRendering(adjacentState, face)} never culls a face —
     * this fixes glass blocks showing holes on non-NORTH sides.
     *
     * CTM connection data is NOT re-queried here at tessellation time; it was
     * already baked into ModelData by {@code getModelData()} using CtmRenderView.
     * Returning AIR for neighbors here therefore does not break CTM.
     */
    private static class CullRenderView implements BlockAndTintGetter {

        private final PhantasiaFakeLevel delegate;
        BlockPos currentRenderPos = BlockPos.ZERO;

        CullRenderView(PhantasiaFakeLevel delegate) {
            this.delegate = delegate;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            // Real state only for the block being rendered; AIR for all neighbors
            // so skipRendering() never culls any face.
            if (pos.equals(currentRenderPos)) {
                return delegate.getBlockState(pos);
            }
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        // Must return the real BE for all positions — some models (GTCEu overlays,
        // LDLib CTM) fetch ModelData from the BE at currentRenderPos during getQuads().
        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return delegate.getBlockEntity(pos);
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return delegate.getMinBuildHeight();
        }

        @Override
        public float getShade(Direction dir, boolean shade) {
            return 1.0f;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return delegate.getLightEngine();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return delegate.getBlockTint(pos, resolver);
        }
    }

    private final CtmRenderView ctmView;
    private final CullRenderView cullView;

    public CustomSceneRenderer(PhantasiaFakeLevel world, int width, int height) {
        this.world = world;
        this.ctmView = new CtmRenderView(world);
        this.cullView = new CullRenderView(world);
        setFBOSize(width, height);
    }

    public void setFBOSize(int width, int height) {
        this.resolutionWidth = width;
        this.resolutionHeight = height;
        releaseFBO();
        try {
            fbo = new MainTarget(width, height);
        } catch (Exception e) {
            System.err.println("[Phantasia] FBO creation failed: " + e);
        }
    }

    public void setClearColor(int color) {
        this.clearColor = color;
    }

    public void setCameraLookAt(Vector3f lookAt, double radius, double rotPitch, double rotYaw) {
        float p = (float) rotPitch, y = (float) rotYaw;
        float x = (float) (radius * Math.cos(p) * Math.sin(y));
        float z = (float) (radius * Math.cos(p) * Math.cos(y));
        float yc = (float) (radius * Math.sin(p));
        this.eyePos = new Vector3f(x, yc, z).add(lookAt);
        this.lookAt = lookAt;
    }

    public void render(@Nonnull GuiGraphics guiGraphics, int gx, int gy, int gw, int gh) {
        if (fbo == null) return;
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int winW = mc.getWindow().getWidth();
        int winH = mc.getWindow().getHeight();

        int prevFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        fbo.bindWrite(true);
        GL11.glViewport(0, 0, resolutionWidth, resolutionHeight);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);

        float r = ((clearColor >> 16) & 0xFF) / 255f;
        float g = ((clearColor >> 8) & 0xFF) / 255f;
        float b = (clearColor & 0xFF) / 255f;
        float a = ((clearColor >> 24) & 0xFF) / 255f;
        GL11.glClearColor(r, g, b, a);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        RenderSystem.backupProjectionMatrix();
        float aspect = (float) resolutionWidth / resolutionHeight;
        RenderSystem.setProjectionMatrix(
                new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.1f, 1000f),
                VertexSorting.DISTANCE_TO_ORIGIN);
        PoseStack mvs = RenderSystem.getModelViewStack();
        mvs.pushPose();
        mvs.setIdentity();
        mvs.last().pose().mul(new Matrix4f().lookAt(
                eyePos.x(), eyePos.y(), eyePos.z(),
                lookAt.x(), lookAt.y(), lookAt.z(),
                worldUp.x(), worldUp.y(), worldUp.z()));
        RenderSystem.applyModelViewMatrix();

        drawBlocks();

        RenderSystem.restoreProjectionMatrix();
        mvs.popPose();
        RenderSystem.applyModelViewMatrix();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
        GL11.glViewport(0, 0, winW, winH);

        int px = (int) (gx * scale), py = (int) (gy * scale);
        int pw = (int) (gw * scale), ph = (int) (gh * scale);
        int dstY0 = winH - py - ph, dstY1 = winH - py;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(
                0, 0, resolutionWidth, resolutionHeight,
                px, dstY0, px + pw, dstY1,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
    }

    private void drawBlocks() {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        PoseStack poseStack = new PoseStack();
        RandomSource rng = RandomSource.create();

        List<BlockPos> opaque = new ArrayList<>();
        List<BlockPos> translucent = new ArrayList<>();

        for (var entry : world.getBlocks().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || state.isAir()) continue;
            RenderType rt = net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(state);
            if (rt == RenderType.translucent() || rt == RenderType.translucentMovingBlock()) {
                translucent.add(entry.getKey());
            } else {
                opaque.add(entry.getKey());
            }
        }

        // ── Pass 1: Opaque ────────────────────────────────────────────────────
        for (BlockPos pos : opaque) {
            renderBlock(pos, blockRenderer, bufferSource, poseStack, rng);
        }
        bufferSource.endBatch(RenderType.solid());
        bufferSource.endBatch(RenderType.cutout());
        bufferSource.endBatch(RenderType.cutoutMipped());

        // ── Pass 2: Translucent (glass etc.) — back-to-front ─────────────────
        translucent.sort(Comparator.comparingDouble(
                (BlockPos pos) -> -eyePos.distanceSquared(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f)));

        GL11.glDepthMask(false);
        for (BlockPos pos : translucent) {
            renderBlock(pos, blockRenderer, bufferSource, poseStack, rng);
        }
        bufferSource.endBatch(RenderType.translucent());
        bufferSource.endBatch();
        GL11.glDepthMask(true);
    }

    /**
     * Renders one block at {@code pos}.
     *
     * Two separate views are used deliberately:
     *
     * 1. {@code ctmView} → passed to {@code model.getModelData()}.
     * Returns REAL neighbor states so LDLib CTM and GTCEu overlay systems can
     * compute correct connection bitmasks. Using the cull view here broke CTM
     * because it returned AIR for all neighbors.
     *
     * 2. {@code cullView} → passed to {@code tesselateBlock()}.
     * Returns the real state ONLY for {@code pos}; every neighbor is AIR.
     * This stops {@code BlockState.skipRendering()} from culling faces between
     * adjacent blocks of the same type (glass, connected casings, etc.).
     */
    private void renderBlock(BlockPos pos, BlockRenderDispatcher blockRenderer,
                             MultiBufferSource bufferSource, PoseStack poseStack, RandomSource rng) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;

        BlockEntity be = world.getBlockEntity(pos);
        net.minecraftforge.client.model.data.ModelData baseData = be != null ? be.getModelData() :
                net.minecraftforge.client.model.data.ModelData.EMPTY;

        var model = blockRenderer.getBlockModel(state);

        // Use ctmView so CTM/LDLib overlay sees real neighbors for connection bits
        net.minecraftforge.client.model.data.ModelData functionalData = model.getModelData(ctmView, pos, state,
                baseData);

        // Use cullView so face-culling sees AIR neighbors → no faces skipped
        cullView.currentRenderPos = pos;

        for (RenderType rt : model.getRenderTypes(state, rng, functionalData)) {
            VertexConsumer consumer = bufferSource.getBuffer(rt);
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            blockRenderer.getModelRenderer().tesselateBlock(
                    cullView,         // BlockAndTintGetter — AIR neighbors, no face culling
                    model,
                    state,
                    pos,
                    poseStack,
                    consumer,
                    false,            // checkSides = false — we control culling via cullView
                    rng,
                    state.getSeed(pos),
                    OverlayTexture.NO_OVERLAY,
                    functionalData,   // ModelData with real CTM connection bits
                    rt);
            poseStack.popPose();
        }
    }

    public void deleteCacheBuffer() {
        releaseFBO();
    }

    private void releaseFBO() {
        if (fbo != null) {
            fbo.destroyBuffers();
            fbo = null;
        }
    }
}
