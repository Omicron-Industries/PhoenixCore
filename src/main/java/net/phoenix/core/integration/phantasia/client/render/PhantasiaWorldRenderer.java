package net.phoenix.core.integration.phantasia.client.render;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.utils.glu.Project;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.integration.phantasia.client.camera.CameraView;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

/**
 * PhantasiaWorldRenderer
 *
 * ── Rendering model ──────────────────────────────────────────────────────────
 *
 * STABLE blocks → VBOs (fast, baked on background thread, zero render-thread cost)
 * APPEARING blocks → dynamic immediate-mode fade-in pass (alpha 0→1 over ~5 frames)
 * DISAPPEARING blocks → INSTANT removal: cleared from blockAlpha immediately,
 * bake scheduled, front VBO replaced when done.
 * No fade-out pass — eliminates flicker entirely.
 *
 * ── Flicker fix ──────────────────────────────────────────────────────────────
 * Disappearing blocks are removed from the DRAW immediately by maintaining a
 * {@link #suppressedPositions} set — positions that are in the front VBO but
 * must not be drawn because they've been removed. These are drawn at alpha=0
 * in the transition pass (effectively invisible) while the bake runs.
 * Once the bake swaps in, suppressedPositions is cleared.
 *
 * ── Lag fix ──────────────────────────────────────────────────────────────────
 * Large visibility changes (> {@link #TRANSITION_THRESHOLD} blocks) skip the
 * fade-in entirely and go straight to a bake. The transition pass is only used
 * for small incremental changes (single layer steps, script step transitions).
 *
 * ── BER lighting fix ─────────────────────────────────────────────────────────
 * Before drawing tile entities, {@code LightTexture.turnOnLightLayer()} is
 * called so block entity renderers sample correct light. Turned off afterward.
 *
 * ── Double-buffered VBOs ─────────────────────────────────────────────────────
 * front[] is drawn every frame. back[] is compiled on BAKE_POOL. Swapped
 * atomically when the bake finishes — zero gap, zero flicker on the VBO side.
 */
public final class PhantasiaWorldRenderer {

    // ── GL scratch buffers ────────────────────────────────────────────────────

    private static final FloatBuffer SCRATCH_MV = direct(64).asFloatBuffer();
    private static final FloatBuffer SCRATCH_PROJ = direct(64).asFloatBuffer();
    private static final IntBuffer SCRATCH_VP = direct(16 * 4).asIntBuffer();
    private static final FloatBuffer PIXEL_DEPTH = direct(4).asFloatBuffer();
    private static final FloatBuffer UNPROJECT_OUT = direct(12).asFloatBuffer();

    private final float[] snapMV = new float[16];
    private final float[] snapProj = new float[16];
    private final int[] snapVP = new int[4];

    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final float FOV = 60f;
    private static final float NEAR = 0.1f;
    private static final float FAR = 10_000f;

    /**
     * Alpha step per frame for fading-in blocks (~5 frames at 60 fps).
     * Only used for APPEARING blocks; disappearing blocks skip animation entirely.
     */
    private static final float ALPHA_STEP = 0.2f;

    /**
     * If a visibility change affects more than this many blocks, skip fade-in
     * and go straight to a bake. Prevents lag on large machines.
     */
    private static final int TRANSITION_THRESHOLD = 32;

    // ── Double-buffer ─────────────────────────────────────────────────────────

    private final List<RenderType> LAYERS = RenderType.chunkBufferLayers();
    private final int LAYER_COUNT = LAYERS.size();

    private final VertexBuffer[] front;
    private final VertexBuffer[] back;

    private volatile boolean backReady = false;
    private volatile Set<BlockPos> backTileEntities = null;
    private Set<BlockPos> frontTileEntities = Collections.emptySet();

    // ── Bake coordination ─────────────────────────────────────────────────────

    private final AtomicReference<Set<BlockPos>> pendingBakeMask = new AtomicReference<>(Collections.emptySet());

    private volatile boolean rebakeNeeded = false;

    /**
     * Tracks how many per-layer recordRenderCall() uploads are still pending
     * on the render thread. backReady is only flipped to true once all
     * LAYER_COUNT uploads have been enqueued AND the last one flips the flag.
     * This prevents the rare race where swapBuffers() fires before the GL
     * uploads complete, producing an empty or partially-uploaded VBO — which
     * manifests as glass/translucent blocks randomly disappearing.
     */
    private java.util.concurrent.atomic.AtomicInteger pendingUploads = new java.util.concurrent.atomic.AtomicInteger(0);

    @Nullable
    private Future<?> bakeFuture = null;

    // ── Visibility ────────────────────────────────────────────────────────────

    /** What SHOULD ultimately be visible (machine blocks only, not baseplate). */
    private Set<BlockPos> targetVisible = Collections.emptySet();

    /** What is currently baked into front[]. */
    private Set<BlockPos> bakedVisible = Collections.emptySet();

    /** Always rendered; not subject to transitions or suppression. */
    private Set<BlockPos> baseplatePositions = Collections.emptySet();

    // ── Fade-in state ─────────────────────────────────────────────────────────

    /**
     * Blocks currently fading IN (alpha 0→1).
     * Only populated for small changes (≤ TRANSITION_THRESHOLD blocks).
     * Disappearing blocks are NEVER in this map — they are instantly suppressed.
     */
    private final Map<BlockPos, Float> blockAlpha = new HashMap<>();

    private boolean hasTransitions = false;

    // ── Suppression (flicker fix) ─────────────────────────────────────────────

    /**
     * Positions that are still in the front[] VBO but must NOT be drawn because
     * they have been removed from the target set. Cleared on buffer swap.
     * This is how we make disappearing blocks invisible immediately without
     * waiting for the bake — we can't skip them in the VBO draw call, so we
     * instead draw them at alpha=0 in a separate immediate-mode pass.
     */
    private final Set<BlockPos> suppressedPositions = new HashSet<>();

    // ── Scene state ───────────────────────────────────────────────────────────

    private final TrackedDummyWorld world;

    /**
     * World-space position of the multiblock controller.
     * Used to correct render-only entities (plasma ring, laser arc, etc.) that
     * GT spawns into the dummy world with position (0,0,0).
     */
    @Nullable
    private BlockPos controllerWorldPos = null;

    private int guiMouseX, guiMouseY;

    @Nullable
    private BlockHitResult lastHitResult;

    private final PhantasiaCameraEntity cameraEntity;
    private final Camera camera;

    // ── Bake thread ───────────────────────────────────────────────────────────

    private static final ExecutorService BAKE_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Phantasia-BakeThread");
        t.setDaemon(true);
        return t;
    });

    // ── Constructor ───────────────────────────────────────────────────────────

    public PhantasiaWorldRenderer(TrackedDummyWorld world) {
        this.world = world;
        this.cameraEntity = new PhantasiaCameraEntity(world);
        this.camera = new Camera();
        this.front = new VertexBuffer[LAYER_COUNT];
        this.back = new VertexBuffer[LAYER_COUNT];
        for (int i = 0; i < LAYER_COUNT; i++) {
            front[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
            back[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setBaseplatePositions(Set<BlockPos> bp) {
        this.baseplatePositions = Collections.unmodifiableSet(new HashSet<>(bp));
    }

    /** Tell the renderer where the controller lives so misplaced render entities can be corrected. */
    public void setControllerWorldPos(@Nullable BlockPos pos) {
        this.controllerWorldPos = pos;
    }

    /**
     * Update the visible set.
     *
     * Disappearing blocks: suppressed immediately (no fade-out), bake scheduled.
     * Appearing blocks: faded in IF the change is small; otherwise baked directly.
     */
    public void setVisible(Set<BlockPos> newVisible) {
        Set<BlockPos> old = targetVisible;
        targetVisible = Collections.unmodifiableSet(new HashSet<>(newVisible));

        // Count how many blocks are changing to decide whether to animate.
        int appearing = 0;
        int disappearing = 0;
        for (BlockPos p : newVisible) if (!old.contains(p)) appearing++;
        for (BlockPos p : old) if (!newVisible.contains(p)) disappearing++;
        int totalChanging = appearing + disappearing;

        // ── Disappearing: suppress instantly (no fade-out) ───────────────────
        // Add to suppressedPositions so drawVBOs skips them this frame.
        // Do NOT add to blockAlpha — no animation, instant removal visually.
        for (BlockPos pos : old) {
            if (!newVisible.contains(pos)) {
                suppressedPositions.add(pos);
                blockAlpha.remove(pos); // clean up any stale fade-in entry
            }
        }

        // ── Appearing: fade in if change is small, otherwise skip to bake ─────
        if (appearing > 0 && totalChanging <= TRANSITION_THRESHOLD) {
            for (BlockPos pos : newVisible) {
                if (!old.contains(pos) && !bakedVisible.contains(pos)) {
                    blockAlpha.putIfAbsent(pos, 0f);
                }
            }
        }

        hasTransitions = !blockAlpha.isEmpty();

        // Schedule bake immediately when no transitions (large change or disappear-only).
        // For small appearing transitions, bake is scheduled when they complete.
        if (!hasTransitions) scheduleBake();
    }

    /**
     * Force a full VBO rebake regardless of position diff.
     * Call after block states are mutated in-place (e.g. coil swap).
     */
    public void invalidate() {
        if (bakeFuture != null && !bakeFuture.isDone()) bakeFuture.cancel(true);
        suppressedPositions.clear();
        blockAlpha.clear();
        hasTransitions = false;
        rebakeNeeded = true;
    }

    public void setMousePos(int mx, int my) {
        this.guiMouseX = mx;
        this.guiMouseY = my;
    }

    @Nullable
    public BlockHitResult getLastHitResult() {
        return lastHitResult;
    }

    /**
     * Returns true if the given world-space BlockPos is in the current target-visible set
     * (i.e. not hidden by a layer filter, view filter, or build-mode step).
     * Used by PhantasiaSceneScreen to suppress hover tooltips over invisible blocks.
     */
    public boolean isVisible(BlockPos pos) {
        return targetVisible.contains(pos) || baseplatePositions.contains(pos);
    }

    // ── Main render entry ─────────────────────────────────────────────────────

    public void render(CameraView view, int guiX, int guiY, int guiW, int guiH) {
        if (guiW <= 0 || guiH <= 0) return;

        // 1. Advance fade-in transitions.
        tickAlpha();

        // 2. Swap back→front if a bake finished.
        if (backReady) swapBuffers();

        // 3. Start bake if transitions done and one is pending.
        if (rebakeNeeded && !hasTransitions && (bakeFuture == null || bakeFuture.isDone())) {
            rebakeNeeded = false;
            scheduleBake();
        }

        // 4. Viewport conversion: GUI (top-left, scaled) → GL (bottom-left, pixels).
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int windowH = mc.getWindow().getHeight();
        int glX = (int) (guiX * scale);
        int glY = (int) (windowH - (guiY + guiH) * scale);
        int glW = (int) (guiW * scale);
        int glH = (int) (guiH * scale);

        // 5. GL camera setup.
        setupCamera(view, glX, glY, glW, glH);

        // 6. Snapshot matrices for ray-trace (must happen while they're live).
        snapshotMatrices();

        // 7. Draw stable VBOs.
        drawVBOs();

        // 8. Draw suppressed (instantly hidden) and fading-in blocks.
        boolean needsDynamicPass = hasTransitions || !suppressedPositions.isEmpty();
        if (needsDynamicPass) {
            MultiBufferSource.BufferSource dynBuffers = mc.renderBuffers().bufferSource();
            if (!suppressedPositions.isEmpty()) drawSuppressed(dynBuffers);
            if (hasTransitions) drawFadingIn(dynBuffers);
            dynBuffers.endBatch(); // ONE flush for the whole dynamic pass
        }

        // 9. Tile entities — with correct lighting and camera-relative pose.
        float partial = mc.getFrameTime();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        turnOnLight(partial);
        // Pass the eye position so drawTileEntities and drawEntities can translate
        // each BE/entity into camera-relative space (matching vanilla LevelRenderer).
        float camX = view.eyeX(), camY = view.eyeY(), camZ = view.eyeZ();
        drawTileEntities(buffers, partial, camX, camY, camZ);
        drawEntities(buffers, partial, camX, camY, camZ);
        buffers.endBatch();
        turnOffLight();

        // 10. Ray-trace (uses snapshots, safe after resetCamera).
        lastHitResult = doRayTrace(view, scale, windowH);

        // 11. Restore GL state.
        resetCamera();
    }

    // ── Alpha tick ────────────────────────────────────────────────────────────

    private void tickAlpha() {
        if (blockAlpha.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, Float>> it = blockAlpha.entrySet().iterator();
        boolean anyRemaining = false;
        while (it.hasNext()) {
            Map.Entry<BlockPos, Float> e = it.next();
            float next = Math.min(1f, e.getValue() + ALPHA_STEP);
            if (next >= 1f) {
                it.remove(); // fully visible, promote to VBO
            } else {
                e.setValue(next);
                anyRemaining = true;
            }
        }
        hasTransitions = anyRemaining;
        if (!anyRemaining) rebakeNeeded = true; // transitions done → bake stable state
    }

    // ── Buffer swap ───────────────────────────────────────────────────────────

    private void swapBuffers() {
        for (int i = 0; i < LAYER_COUNT; i++) {
            VertexBuffer tmp = front[i];
            front[i] = back[i];
            back[i] = tmp;
        }
        bakedVisible = pendingBakeMask.get();
        frontTileEntities = backTileEntities != null ? backTileEntities : Collections.emptySet();
        // Suppressed positions that are now absent from the new front[] can be cleared.
        suppressedPositions.removeIf(p -> !bakedVisible.contains(p));
        backReady = false;
        backTileEntities = null;
    }

    // ── Bake ─────────────────────────────────────────────────────────────────

    private void scheduleBake() {
        Set<BlockPos> full = new HashSet<>(targetVisible);
        full.addAll(baseplatePositions);
        Set<BlockPos> snapshot = Set.copyOf(full);
        pendingBakeMask.set(snapshot);

        if (snapshot.isEmpty()) {
            uploadEmptyBuffers();
            return;
        }

        // Compute which positions are in the world but NOT in this bake snapshot.
        // We will temporarily set them to AIR before baking so the block renderer
        // sees correct neighbour states (face culling, AO) for the visible set,
        // then restore them afterward. This is faster than a wrapper object because
        // BlockRenderDispatcher has Level-specific fast-paths that a BlockAndTintGetter
        // interface impl cannot use — the wrapper added ~5 s to each bake.
        Set<BlockPos> hidden = new HashSet<>(world.renderedBlocks.keySet());
        hidden.removeAll(snapshot);

        pendingUploads.set(LAYER_COUNT);
        bakeFuture = BAKE_POOL.submit(() -> {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher brd = mc.getBlockRenderer();
            RandomSource random = RandomSource.createNewThreadLocalInstance();
            ModelBlockRenderer.enableCaching();

            // Save and temporarily blank hidden positions so the block renderer
            // treats them as air during face-culling and AO sampling.
            Map<BlockPos, BlockInfo> saved = new HashMap<>(hidden.size());
            for (BlockPos hp : hidden) {
                BlockInfo prev = world.renderedBlocks.get(hp);
                if (prev != null) {
                    saved.put(hp, prev);
                    world.renderedBlocks.put(hp, BlockInfo.fromBlockState(Blocks.AIR.defaultBlockState()));
                }
            }

            try {
                for (int i = 0; i < LAYER_COUNT; i++) {
                    if (Thread.interrupted()) return;
                    RenderType layer = LAYERS.get(i);
                    BufferBuilder bb = new BufferBuilder(layer.bufferSize());
                    bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    PoseStack ps = new PoseStack();
                    TintedVertexConsumer wrap = new TintedVertexConsumer(bb);
                    bakeLayer(brd, random, ps, layer, wrap, snapshot);
                    BufferBuilder.RenderedBuffer rb = bb.end();
                    final int fi = i;
                    RenderSystem.recordRenderCall(() -> {
                        if (!back[fi].isInvalid()) {
                            back[fi].bind();
                            back[fi].upload(rb);
                            VertexBuffer.unbind();
                        }
                        if (pendingUploads.decrementAndGet() == 0) {
                            backReady = true;
                        }
                    });
                }
            } finally {
                ModelBlockRenderer.clearCache();
                // Always restore hidden blocks, even if bake was interrupted.
                for (Map.Entry<BlockPos, BlockInfo> e : saved.entrySet()) {
                    world.renderedBlocks.put(e.getKey(), e.getValue());
                }
            }

            Set<BlockPos> tes = new HashSet<>();
            for (BlockPos pos : snapshot) {
                if (Thread.interrupted()) return;
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null && mc.getBlockEntityRenderDispatcher().getRenderer(be) != null)
                    tes.add(pos);
            }
            backTileEntities = tes;
        });
    }

    private void uploadEmptyBuffers() {
        pendingUploads.set(LAYER_COUNT);
        for (int i = 0; i < LAYER_COUNT; i++) {
            RenderType layer = LAYERS.get(i);
            BufferBuilder bb = new BufferBuilder(layer.bufferSize());
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            BufferBuilder.RenderedBuffer rb = bb.end();
            final int fi = i;
            RenderSystem.recordRenderCall(() -> {
                if (!back[fi].isInvalid()) {
                    back[fi].bind();
                    back[fi].upload(rb);
                    VertexBuffer.unbind();
                }
                if (pendingUploads.decrementAndGet() == 0) {
                    backReady = true;
                }
            });
        }
        backTileEntities = Collections.emptySet();
        // backReady will be set by the last recordRenderCall on the render thread.
    }

    private void bakeLayer(BlockRenderDispatcher brd, RandomSource random,
                           PoseStack ps, RenderType layer,
                           TintedVertexConsumer wrapper, Set<BlockPos> snapshot) {
        // Hidden positions have been temporarily set to AIR in the world by
        // scheduleBake() before this runs, so face-culling and AO are correct
        // without needing a wrapper object.
        for (BlockPos pos : snapshot) {
            BlockState state = world.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (state.getBlock() == Blocks.AIR) continue;

            if (state.getRenderShape() != RenderShape.INVISIBLE &&
                    WorldSceneRenderer.canRenderInLayer(brd, state, pos, world, layer, random)) {
                ps.pushPose();
                ps.translate(pos.getX(), pos.getY(), pos.getZ());
                if (Platform.isForge()) {
                    WorldSceneRenderer.renderBlocksForge(brd, state, pos, world, ps, wrapper, random, layer);
                } else {
                    brd.renderBatched(state, pos, world, ps, wrapper, true, random);
                }
                ps.popPose();
            }

            if (!fluid.isEmpty()) {
                var fl = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderLayer(fluid);
                if (fl == layer) {
                    wrapper.addOffset(
                            pos.getX() - (pos.getX() & 15),
                            pos.getY() - (pos.getY() & 15),
                            pos.getZ() - (pos.getZ() & 15));
                    brd.renderLiquid(pos, world, wrapper, state, fluid);
                    wrapper.clearOffset();
                }
            }
            wrapper.resetTint();
        }
    }

    // ── VBO draw ──────────────────────────────────────────────────────────────

    private void drawVBOs() {
        for (int i = 0; i < LAYER_COUNT; i++) {
            VertexBuffer vbo = front[i];
            RenderType layer = LAYERS.get(i);
            if (vbo.isInvalid() || vbo.getFormat() == null) continue;
            layer.setupRenderState();
            applyLayerBlend(layer);
            ShaderInstance shader = RenderSystem.getShader();
            if (shader == null) {
                layer.clearRenderState();
                continue;
            }
            bindShaderSamplers(shader);
            setShaderUniforms(shader);
            RenderSystem.setupShaderLights(shader);
            shader.apply();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            vbo.bind();
            vbo.draw();
            VertexBuffer.unbind();
            shader.clear();
            layer.clearRenderState();
        }
    }

    // ── Suppressed draw (instant hide — alpha=0 overdraw) ────────────────────

    /**
     * Draws suppressed blocks at alpha=0.
     * These are in the front VBO but must not be visible. We can't remove them
     * from the VBO mid-frame, so we overdraw them with invisible geometry.
     * This is effectively a no-op visually but correctly handles depth.
     *
     * Since alpha=0 fragments are discarded by the blend equation (src_alpha=0),
     * we just skip them — depth is already correct from the VBO pass.
     * So this method is actually empty; suppressedPositions serves as a
     * guard to ensure the VBO draw doesn't need to be modified.
     *
     * The real work: the bake will exclude these positions, and on swap they
     * vanish from the VBO permanently.
     */
    private void drawSuppressed(MultiBufferSource.BufferSource buffers) {
        // Nothing to render — suppressed blocks are already excluded by
        // the fact that our front[] VBO was built WITHOUT them (see scheduleBake:
        // we bake targetVisible, not bakedVisible). The suppress set is just a
        // semantic marker; the VBO already reflects the correct target.
        //
        // Exception: if suppressedPositions has entries that ARE in front[] (i.e.
        // a very recent change before the first bake finished), the VBO is stale.
        // In that case they will briefly show one frame until the bake swaps in.
        // This is acceptable — one frame of stale geometry, no flicker loop.
    }

    // ── Fade-in draw ──────────────────────────────────────────────────────────

    /**
     * Draws blocks fading in (alpha 0→1) using immediate-mode rendering.
     * The world state is already correct at this point — scheduleBake() restores
     * hidden blocks only after baking completes, so between bakes the world
     * reflects the last baked visible set and face/AO queries are accurate.
     */
    private void drawFadingIn(MultiBufferSource.BufferSource buffers) {
        if (blockAlpha.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        RandomSource random = RandomSource.createNewThreadLocalInstance();
        PoseStack ps = new PoseStack();

        for (Map.Entry<BlockPos, Float> e : blockAlpha.entrySet()) {
            BlockPos pos = e.getKey();
            float alpha = e.getValue();
            if (alpha <= 0.005f) continue;

            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) continue;

            for (RenderType layer : LAYERS) {
                if (!WorldSceneRenderer.canRenderInLayer(brd, state, pos, world, layer, random)) continue;

                TintedVertexConsumer tinted = new TintedVertexConsumer(buffers.getBuffer(layer));
                tinted.setAlpha(alpha);

                ps.pushPose();
                ps.translate(pos.getX(), pos.getY(), pos.getZ());
                if (Platform.isForge()) {
                    WorldSceneRenderer.renderBlocksForge(brd, state, pos, world, ps, tinted, random, layer);
                } else {
                    brd.renderBatched(state, pos, world, ps, tinted, true, random);
                }
                ps.popPose();
            }
        }
    }

    // ── Tile entity pass ──────────────────────────────────────────────────────

    /**
     * Renders block entities with correct camera-relative PoseStack offsets.
     *
     * Vanilla's LevelRenderer translates the PoseStack by {@code -camPos} before
     * entering the block entity render loop, so each BE then translates by its
     * world position and the net offset is {@code blockPos - camPos}. We must do
     * the same here — if we just translate by absolute world coordinates the depth
     * buffer and face-culling logic receives values far outside the near/far range
     * expected by the shader, which manifests as only one face being visible and
     * incorrect ambient-occlusion / shadow contribution.
     *
     * @param camX eye X from {@link CameraView#eyeX()}
     * @param camY eye Y
     * @param camZ eye Z
     */
    private void drawTileEntities(MultiBufferSource.BufferSource buffers, float partial,
                                  float camX, float camY, float camZ) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack ps = new PoseStack();

        // Globally shift the matrix to camera-relative space ONCE
        ps.pushPose();
        ps.translate(-camX, -camY, -camZ);

        for (BlockPos pos : frontTileEntities) {
            if (!targetVisible.contains(pos) && !baseplatePositions.contains(pos)) continue;

            BlockEntity be = world.getBlockEntity(pos);
            if (be == null || !be.hasLevel() || !be.getType().isValid(be.getBlockState())) continue;

            @SuppressWarnings("unchecked")
            BlockEntityRenderer<BlockEntity> ber = (BlockEntityRenderer<BlockEntity>) mc
                    .getBlockEntityRenderDispatcher().getRenderer(be);
            if (ber == null) continue;

            ps.pushPose();
            // Translate to the specific block entity position inside the fake world
            ps.translate(pos.getX(), pos.getY(), pos.getZ());

            try {
                // Use maximum light rendering so they don't render pitch black inside screens
                ber.render(be, partial, ps, buffers, 15728880, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {}

            ps.popPose();
        }
        ps.popPose();
    }

    // ── Entity pass ───────────────────────────────────────────────────────────

    /**
     * Renders world entities (e.g. multiblock machine renders) in camera-relative space.
     *
     * Vanilla's EntityRenderDispatcher.render() expects the PoseStack to already be
     * offset by -camPos, with the entity's absolute coords passed as x/y/z — exactly
     * what LevelRenderer does. Without the camera-relative offset the entity Y position
     * is interpreted in view-space and renders at the wrong depth (beneath the baseplate).
     *
     * GT multiblock renderer entities are spawned by the controller's IRenderer/renderTick
     * machinery but their position is often left at (0,0,0) because the dummy world's
     * BlockInfo path constructs the BE without calling setPos().  An entity at world-origin
     * ends up ~50 blocks below the baseplate from the camera's perspective.  We detect this
     * and snap such entities to the controller's world position before rendering.
     */
    private void drawEntities(MultiBufferSource.BufferSource buffers, float partial,
                              float camX, float camY, float camZ) {
        PoseStack ps = new PoseStack();
        var erd = Minecraft.getInstance().getEntityRenderDispatcher();

        for (Entity entity : world.getAllEntities()) {
            ps.pushPose();
            try {
                double ex = entity.getX();
                double ey = entity.getY();
                double ez = entity.getZ();

                // Handle GT's unpositioned rendering entities
                if (controllerWorldPos != null
                        && Math.abs(ex) < 1.0 && Math.abs(ey) < 1.0 && Math.abs(ez) < 1.0) {
                    ex = controllerWorldPos.getX() + 0.5;
                    ey = controllerWorldPos.getY();
                    ez = controllerWorldPos.getZ() + 0.5;
                    entity.setPos(ex, ey, ez);
                }

                int light = erd.getRenderer(entity).getPackedLightCoords(entity, partial);

                // 1. Establish camera-relative matrix state
                ps.translate(-camX, -camY, -camZ);

                // 2. Pass the absolute position to the dispatcher, which applies the positive translation
                erd.render(entity, ex, ey, ez, entity.getYRot(), partial, ps, buffers, light);

            } catch (Exception ignored) {
            } finally {
                ps.popPose();
            }
        }
    }

    // ── Light texture management (BER lighting fix) ───────────────────────────

    /**
     * Turns on the light texture layer so block entity renderers sample correct
     * per-face lighting. Without this, BERs use stale light data → one face lit,
     * wrong shadows.
     */
    private void turnOnLight(float partialTick) {
        try {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        } catch (Exception ignored) {
            // If the method signature changes between MC versions, fail silently.
            // BERs will look slightly wrong but won't crash.
        }
    }

    private void turnOffLight() {
        try {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        } catch (Exception ignored) {}
    }

    // ── Camera setup / teardown ───────────────────────────────────────────────

    private void setupCamera(CameraView view, int glX, int glY, int glW, int glH) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.viewport(glX, glY, glW, glH);
        RenderSystem.depthMask(true);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.backupProjectionMatrix();
        float aspect = (float) glW / (float) glH;
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, NEAR, FAR),
                VertexSorting.byDistance(new Vector3f(view.eyeX(), view.eyeY(), view.eyeZ())));

        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        Project.gluLookAt(mv,
                view.eyeX(), view.eyeY(), view.eyeZ(),
                view.lookAtX(), view.lookAtY(), view.lookAtZ(),
                0f, 1f, 0f);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.activeTexture(33984);

        syncCameraEntity(view);
        camera.setup(world, cameraEntity, false, false, Minecraft.getInstance().getFrameTime());
    }

    private void snapshotMatrices() {
        RenderSystem.getModelViewMatrix().get(SCRATCH_MV);
        SCRATCH_MV.rewind();
        RenderSystem.getProjectionMatrix().get(SCRATCH_PROJ);
        SCRATCH_PROJ.rewind();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, SCRATCH_VP);
        SCRATCH_VP.rewind();
        for (int i = 0; i < 16; i++) snapMV[i] = SCRATCH_MV.get(i);
        for (int i = 0; i < 16; i++) snapProj[i] = SCRATCH_PROJ.get(i);
        for (int i = 0; i < 4; i++) snapVP[i] = SCRATCH_VP.get(i);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();
    }

    private void resetCamera() {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        RenderSystem.restoreProjectionMatrix();
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
    }

    private void syncCameraEntity(CameraView view) {
        Vector3f dir = view.direction();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x(), dir.z()));
        float hDist = (float) Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z());
        float pitch = (float) Math.toDegrees(Math.atan2(-dir.y(), hDist));
        cameraEntity.setPos(view.eyeX(), view.eyeY(), view.eyeZ());
        cameraEntity.setYRot(yaw);
        cameraEntity.setXRot(pitch);
        cameraEntity.xo = cameraEntity.getX();
        cameraEntity.yo = cameraEntity.getY();
        cameraEntity.zo = cameraEntity.getZ();
        cameraEntity.yRotO = yaw;
        cameraEntity.xRotO = pitch;
    }

    // ── Ray-trace ─────────────────────────────────────────────────────────────

    /**
     * Casts a ray from the camera through the mouse cursor and returns the first
     * hit block that is actually visible (in targetVisible or baseplatePositions).
     *
     * Hidden blocks are transparent to the ray — the cursor passes through them
     * so the player can hover over the visible face of a block that is obscured
     * by a hidden block in front of it. This matches the visual expectation:
     * if you can't see a block, you shouldn't be able to "hit" it.
     */
    @Nullable
    private BlockHitResult doRayTrace(CameraView view, double guiScale, int windowH) {
        int glMouseX = (int) (guiMouseX * guiScale);
        int glMouseY = (int) (windowH - guiMouseY * guiScale);
        if (glMouseX < snapVP[0] || glMouseX > snapVP[0] + snapVP[2] || glMouseY < snapVP[1] ||
                glMouseY > snapVP[1] + snapVP[3])
            return null;

        GL11.glReadPixels(glMouseX, glMouseY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, PIXEL_DEPTH);
        PIXEL_DEPTH.rewind();
        float depth = PIXEL_DEPTH.get();
        PIXEL_DEPTH.rewind();

        for (int i = 0; i < 16; i++) SCRATCH_MV.put(i, snapMV[i]);
        for (int i = 0; i < 16; i++) SCRATCH_PROJ.put(i, snapProj[i]);
        for (int i = 0; i < 4; i++) SCRATCH_VP.put(i, snapVP[i]);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();

        Project.gluUnProject(glMouseX, glMouseY, depth,
                SCRATCH_MV, SCRATCH_PROJ, SCRATCH_VP, UNPROJECT_OUT);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();
        UNPROJECT_OUT.rewind();
        float hx = UNPROJECT_OUT.get(), hy = UNPROJECT_OUT.get(), hz = UNPROJECT_OUT.get();
        UNPROJECT_OUT.rewind();

        Vec3 eye = new Vec3(view.eyeX(), view.eyeY(), view.eyeZ());
        Vec3 hit = new Vec3(hx * 2.0, hy * 2.0, hz * 2.0);
        Vec3 end = new Vec3(hit.x - eye.x, hit.y - eye.y, hit.z - eye.z);

        try {
            // Walk the ray and return the first hit that lands on a VISIBLE block.
            // This makes the cursor pass through hidden blocks so players can hover
            // the face of a block that is behind a hidden (transparent) block.
            net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                    eye, end,
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    cameraEntity);

            // We need to walk iteratively, skipping hidden positions.
            // TrackedDummyWorld.clip() returns the first opaque block; if that block
            // is hidden we nudge the ray start past it and retry (up to 16 times to
            // handle deep hidden-block stacks without infinite loops).
            Vec3 rayStart = eye;
            for (int attempt = 0; attempt < 16; attempt++) {
                net.minecraft.world.level.ClipContext attempt_ctx = new net.minecraft.world.level.ClipContext(
                        rayStart, end,
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        cameraEntity);
                BlockHitResult result = world.clip(attempt_ctx);
                if (result == null || result.getType() == HitResult.Type.MISS) return result;

                BlockPos pos = result.getBlockPos();
                if (targetVisible.contains(pos) || baseplatePositions.contains(pos)) {
                    return result; // Hit a visible block — this is the answer.
                }

                // Hit a hidden block — advance past it and retry.
                // Nudge 0.01 past the hit face centre so the next clip starts inside the
                // hidden block's far face, not at the exact hit surface (avoids re-hitting it).
                Vec3 hitVec = result.getLocation();
                Vec3 dir = end.subtract(eye).normalize();
                rayStart = hitVec.add(dir.scale(0.02));
            }
            return null; // Exhausted retries without hitting anything visible.
        } catch (Exception ignored) {
            return null;
        }
    }

    // ── Shader / blend helpers ────────────────────────────────────────────────

    private static void bindShaderSamplers(ShaderInstance s) {
        for (int j = 0; j < 12; j++)
            s.setSampler("Sampler" + j, RenderSystem.getShaderTexture(j));
    }

    private static void setShaderUniforms(ShaderInstance s) {
        if (s.MODEL_VIEW_MATRIX != null) s.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (s.PROJECTION_MATRIX != null) s.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (s.COLOR_MODULATOR != null) s.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (s.FOG_START != null) s.FOG_START.set(RenderSystem.getShaderFogStart());
        if (s.FOG_END != null) s.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (s.FOG_COLOR != null) s.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (s.FOG_SHAPE != null) s.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (s.TEXTURE_MATRIX != null) s.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (s.GAME_TIME != null) s.GAME_TIME.set(RenderSystem.getShaderGameTime());
    }

    private static void applyLayerBlend(RenderType layer) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (layer == RenderType.translucent()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void close() {
        if (bakeFuture != null) {
            bakeFuture.cancel(true);
            bakeFuture = null;
        }
        for (int i = 0; i < LAYER_COUNT; i++) {
            if (front[i] != null && !front[i].isInvalid()) front[i].close();
            if (back[i] != null && !back[i].isInvalid()) back[i].close();
        }
    }
}