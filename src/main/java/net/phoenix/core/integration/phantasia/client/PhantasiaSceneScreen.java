package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phantasia.PhantasiaLoadedPattern;
import net.phoenix.core.integration.phantasia.PhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaScriptData;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;
import net.phoenix.core.integration.phantasia.client.camera.CameraView;
import net.phoenix.core.integration.phantasia.client.camera.LerpType;
import net.phoenix.core.integration.phantasia.client.camera.PhantasiaCamera;
import net.phoenix.core.integration.phantasia.client.render.PhantasiaWorldRenderer;
import net.phoenix.core.integration.phantasia.utils.PhantasiaThemeUtils;
import net.phoenix.core.integration.phantasia.utils.PhantasiaUIUtils;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

import static net.phoenix.core.integration.phantasia.utils.PhantasiaThemeUtils.*;

@OnlyIn(Dist.CLIENT)
public class PhantasiaSceneScreen extends Screen {

    // ─────────────────────────────────────────────────────────────────────────
    // Shared dummy world
    // ─────────────────────────────────────────────────────────────────────────

    public static TrackedDummyWorld SHARED_LEVEL;
    private static int NEXT_REGION = 0;
    private static final int REGION_SIZE = 512;

    public static void invalidateSharedLevel() {
        SHARED_LEVEL = null;
        NEXT_REGION  = 0;
    }

    public static BlockPos getOriginForCurrentPattern() {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof PhantasiaSceneScreen pss && pss.pattern != null)
            return pss.pattern.origin;
        if (mc.screen instanceof PhantasiaFootprintScreen pfs && pfs.getPattern() != null)
            return pfs.getPattern().origin;
        return BlockPos.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout constants
    // ─────────────────────────────────────────────────────────────────────────

    private static final int FULL_PANEL_W      = 168;
    private static final int COLLAPSED_PANEL_W = 18;
    private static final int TIMELINE_H        = 26;
    private static final int CAPTION_STRIP_H   = 22;

    // ─────────────────────────────────────────────────────────────────────────
    // Camera defaults
    // ─────────────────────────────────────────────────────────────────────────

    private static final float CAM_TARGET_Y_BIAS     = 0.0f;
    private static final float CAM_DEFAULT_PITCH     = 5.0f;
    private static final float CAM_DEFAULT_ZOOM      = 40.0f;
    private static final float CAM_ZOOM_IN_FACTOR    = 0.9f;
    private static final float CAM_ZOOM_OUT_FACTOR   = 1.1f;
    private static final float CAM_ZOOM_MIN          = 2.0f;
    private static final float CAM_ZOOM_MAX          = 100.0f;
    private static final float CAM_ORBIT_SENSITIVITY = 0.5f;
    private static final float CAM_PAN_SPEED         = 0.02f;

    // ─────────────────────────────────────────────────────────────────────────
    // Core state
    // ─────────────────────────────────────────────────────────────────────────

    private final Screen parent;
    public final MultiblockMachineDefinition definition;
    private PhantasiaScript script;

    private PhantasiaLoadedPattern pattern;

    /**
     * Our custom renderer. Created once on first init(), survives re-inits
     * (window resize, sub-screen returns) so VBOs are preserved.
     * Explicitly closed in onClose() and on shape changes.
     */
    private PhantasiaWorldRenderer renderer;

    private int                       shapeIndex      = 0;
    private List<MultiblockShapeInfo> availableShapes = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Camera
    // ─────────────────────────────────────────────────────────────────────────

    private PhantasiaCamera camera;
    private boolean         isPanning = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Playback
    // ─────────────────────────────────────────────────────────────────────────

    private boolean              playing        = true;
    private int                  playbackTick   = 0;
    private float                tickAccum      = 0f;
    private float                playbackSpeed  = 1.0f;
    private boolean              scrubbing      = false;
    private PhantasiaScript.Step lastAppliedStep = null;

    // ─────────────────────────────────────────────────────────────────────────
    // View / filter
    // ─────────────────────────────────────────────────────────────────────────

    public enum ViewFilter { ALL, HATCHES_BUSES, ENERGY_IO, BLOCK_ENTITIES, CONTROLLER }

    private ViewFilter viewFilter             = ViewFilter.ALL;
    private boolean    wasPlayingBeforeFilter = false;
    private int        manualLayer            = -1;

    private Set<BlockPos> filteredHatchBus   = null;
    private Set<BlockPos> filteredEnergyIO   = null;
    private Set<BlockPos> filteredHasBE      = null;
    private Set<BlockPos> filteredController = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Build-order mode
    // ─────────────────────────────────────────────────────────────────────────

    private boolean buildOrderMode  = false;
    private int     buildOrderGroup = 0;
    private float   buildPulse      = 0f;
    private boolean buildPulseUp    = true;

    // ─────────────────────────────────────────────────────────────────────────
    // Coil cycling
    // ─────────────────────────────────────────────────────────────────────────

    private int coilIndex = 0;
    private static final List<BlockInfo> COIL_TIERS = List.of(
            new BlockInfo(GTBlocks.COIL_CUPRONICKEL.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_KANTHAL.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_NICHROME.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_RTMALLOY.get().defaultBlockState()));

    // ─────────────────────────────────────────────────────────────────────────
    // Caption
    // ─────────────────────────────────────────────────────────────────────────

    private float  captionAlpha    = 0f;
    private String captionCurrent  = null;
    private String captionOutgoing = null;
    private float  captionOutAlpha = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    private final List<PhantasiaUIUtils.ButtonAction> activeButtons = new ArrayList<>();
    private boolean  sidePanelCollapsed = false;
    private BlockPos hoveredPos         = null;

    public boolean showMistakes      = false;
    public int     selectedTierIndex = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public PhantasiaSceneScreen(MultiblockMachineDefinition definition, Screen parent) {
        super(Component.literal(definition.getLangValue()));
        this.parent     = parent;
        this.definition = definition;
        this.script     = PhantasiaScripts.get(definition);
    }

    public void reloadScript() {
        this.script          = PhantasiaScripts.get(definition);
        this.lastAppliedStep = null;
        applyVisibility();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // init()
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        if (SHARED_LEVEL == null) {
            if (Minecraft.getInstance().level == null) { onClose(); return; }
            SHARED_LEVEL = new TrackedDummyWorld();
        }

        availableShapes = definition.getMatchingShapes();
        if (pattern == null && !availableShapes.isEmpty()) {
            if (shapeIndex >= availableShapes.size()) shapeIndex = 0;
            pattern = loadPattern(availableShapes.get(shapeIndex));
        }

        // ── Renderer ──────────────────────────────────────────────────────────
        // Created once; re-inits (resize, sub-screen returns) reuse it so baked
        // VBOs survive the re-init without a redundant rebake.
        if (renderer == null) {
            renderer = new PhantasiaWorldRenderer(SHARED_LEVEL);
            if (pattern != null) renderer.setBaseplatePositions(pattern.baseplatePositions);
        }

        // ── Camera ────────────────────────────────────────────────────────────
        if (camera == null) {
            camera = buildFreshCamera();
        } else if (camera.hasSavedSnapshot()) {
            // Returning from a sub-screen — restore the exact view the player had.
            camera.restore();
        } else if (!camera.isPlayerOwned()) {
            // System re-init with no snapshot and no player input — recalculate
            // default (e.g. window was resized; script step-0 camera still applies).
            resetCameraToDefault(LerpType.SNAP, 0);
        }
        // playerOwned + no snapshot = player moved the camera before resize; keep it.

        if (pattern != null) applyVisibility();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera helpers
    // ─────────────────────────────────────────────────────────────────────────

    private PhantasiaCamera buildFreshCamera() {
        float[] yp     = resolveStartingYawPitch();
        float   zoom   = resolveStartingZoom();
        float[] target = resolveTarget();
        PhantasiaCamera cam = new PhantasiaCamera(yp[0], yp[1], zoom,
                target[0], target[1], target[2]);
        if (pattern != null) cam.setFloorY(pattern.origin.getY() + 0.5f);
        return cam;
    }

    private void resetCameraToDefault(LerpType type, int ticks) {
        float[] yp     = resolveStartingYawPitch();
        float   zoom   = resolveStartingZoom();
        float[] target = resolveTarget();
        camera.setTarget(target[0], target[1], target[2]);
        camera.hardReset(yp[0], yp[1], zoom, target[0], target[1], target[2], type, ticks);
        if (pattern != null) camera.setFloorY(pattern.origin.getY() + 0.5f);
    }

    private float[] resolveStartingYawPitch() {
        // 1. Explicit startCamera on the script (highest priority).
        if (script != null) {
            var sc = script.getStartCamera();
            if (sc != null) {
                float yaw   = sc.hasYaw()   ? sc.getYaw()   : getFacingYaw();
                float pitch = sc.hasPitch() ? sc.getPitch() : CAM_DEFAULT_PITCH;
                return new float[]{ yaw, pitch };
            }
        }
        // 2. Step-0 animation camera (kept for backwards compatibility).
        if (script != null && !script.getSteps().isEmpty()) {
            PhantasiaScript.Step s0 = script.getSteps().get(0);
            if (s0.hasCamera()) return new float[]{ s0.yaw(), s0.pitch() };
        }
        // 3. Auto: face from the controller's facing direction.
        return new float[]{ getFacingYaw(), CAM_DEFAULT_PITCH };
    }

    private float resolveStartingZoom() {
        // 1. Explicit startCamera zoom.
        if (script != null) {
            var sc = script.getStartCamera();
            if (sc != null && sc.hasZoom()) return sc.getZoom();
        }
        // 2. Step-0 animation camera zoom.
        if (script != null && !script.getSteps().isEmpty()) {
            PhantasiaScript.Step s0 = script.getSteps().get(0);
            if (s0.hasCamera() && s0.zoom() > 0) return s0.zoom();
        }
        // 3. Auto from bounding box: use the largest dimension across all three axes
        //    so both tall narrow machines and wide flat ones frame comfortably.
        if (pattern == null || SHARED_LEVEL == null) return CAM_DEFAULT_ZOOM;
        org.joml.Vector3f size = SHARED_LEVEL.getSize();
        float maxDim = Math.max(size.x, Math.max(size.y, size.z));
        return Math.max(CAM_DEFAULT_ZOOM, maxDim * 3.0f);
    }

    private float[] resolveTarget() {
        if (pattern == null) return new float[]{ 0, 0, 0 };

        // Use world-space bounding box centre so asymmetric and offset machines
        // are framed correctly, rather than always pointing at the controller.
        float cx, cy, cz;
        if (SHARED_LEVEL != null) {
            // minPos/maxPos are populated by TrackedDummyWorld.addBlock() and span
            // the entire rendered set. We want the machine body centre, not the
            // baseplate centre, so use the pattern's local Y range for vertical.
            org.joml.Vector3f min = SHARED_LEVEL.getMinPos();
            org.joml.Vector3f max = SHARED_LEVEL.getMaxPos();
            cx = (min.x + max.x) * 0.5f;
            cz = (min.z + max.z) * 0.5f;
            // Y: use the local pattern range (avoids the baseplate pulling the
            // target too low on tall machines).
            cy = pattern.origin.getY()
                    + (pattern.minY + pattern.maxY) * 0.5f + CAM_TARGET_Y_BIAS;
        } else {
            // Fallback: controller position.
            BlockPos cp = pattern.controllerWorldPos != null
                    ? pattern.controllerWorldPos : pattern.origin;
            cx = cp.getX() + 0.5f;
            cy = pattern.origin.getY()
                    + (pattern.minY + pattern.maxY) * 0.5f + CAM_TARGET_Y_BIAS;
            cz = cp.getZ() + 0.5f;
        }

        // Apply any manual target offset from startCamera.
        if (script != null) {
            var sc = script.getStartCamera();
            if (sc != null && sc.hasTargetOffset()) {
                cx += sc.getTargetOffsetX();
                cy += sc.getTargetOffsetY();
                cz += sc.getTargetOffsetZ();
            }
        }

        return new float[]{ cx, cy, cz };
    }

    private float getFacingYaw() {
        if (pattern == null || pattern.controllerWorldPos == null || SHARED_LEVEL == null)
            return -135f;
        try {
            BlockState ctrl = SHARED_LEVEL.getBlockState(pattern.controllerWorldPos);
            if (ctrl.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return switch (ctrl.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    case NORTH -> 180f;
                    case SOUTH ->   0f;
                    case WEST  -> 270f;
                    case EAST  ->  90f;
                    default    -> -135f;
                };
            }
        } catch (Exception ignored) {}
        return -135f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern loading
    // ─────────────────────────────────────────────────────────────────────────

    private PhantasiaLoadedPattern loadPattern(MultiblockShapeInfo shape) {
        int regionIndex = NEXT_REGION++;
        BlockPos origin = new BlockPos(regionIndex * REGION_SIZE, 50, 0);

        Map<BlockPos, BlockInfo> blockMap     = new HashMap<>();
        Map<BlockPos, BlockPos>  localToWorld = new HashMap<>();
        Set<BlockPos>            baseplatePos = new HashSet<>();
        Set<BlockPos>            bePos        = new HashSet<>();
        BlockPos                 controllerWP = null;
        MultiblockControllerMachine controller = null;

        BlockInfo floor  = BlockInfo.fromBlockState(Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        BlockInfo[][][] raw   = shape.getBlocks();
        int sxLen = raw.length;
        int szLen = sxLen > 0 && raw[0].length > 0 ? raw[0][0].length : 0;
        int padX  = Math.max(2, sxLen / 2 + 1);
        int padZ  = Math.max(2, szLen / 2 + 1);

        for (int bx = -padX; bx <= sxLen + padX; bx++)
            for (int bz = -padZ; bz <= szLen + padZ; bz++) {
                BlockPos wp = origin.offset(bx, -1, bz);
                blockMap.put(wp, floor);
                baseplatePos.add(wp);
            }

        for (int x = 0; x < raw.length; x++)
            for (int y = 0; y < raw[x].length; y++)
                for (int z = 0; z < raw[x][y].length; z++) {
                    BlockInfo info = raw[x][y][z];
                    if (info == null) continue;
                    BlockPos lp = new BlockPos(x, y, z);
                    BlockPos wp = origin.offset(x, y, z);
                    try {
                        var be = info.getBlockEntity(wp);
                        if (be instanceof MetaMachineBlockEntity mbe) {
                            mbe.setLevel(SHARED_LEVEL);
                            var machine = mbe.getMetaMachine();
                            if (machine instanceof MultiblockControllerMachine ctrl && controllerWP == null) {
                                controller   = ctrl;
                                controllerWP = wp;
                            }
                            bePos.add(wp);
                        }
                    } catch (Exception ignored) {}
                    blockMap.put(wp, info);
                    localToWorld.put(lp, wp);
                }

        SHARED_LEVEL.addBlocks(blockMap);

        if (controller != null) {
            try {
                var holder = controller.getHolder();
                if (holder instanceof net.minecraft.world.level.block.entity.BlockEntity vanillaBe)
                    SHARED_LEVEL.setInnerBlockEntity(vanillaBe);
                BlockPattern pat = controller.getPattern();
                if (pat != null && pat.checkPatternAt(controller.getMultiblockState(), true))
                    controller.onStructureFormed();
            } catch (Exception ignored) {}
        }

        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos lp : localToWorld.keySet()) {
            minY = Math.min(minY, lp.getY());
            maxY = Math.max(maxY, lp.getY());
        }
        if (minY > maxY) { minY = 0; maxY = 0; }

        return new PhantasiaLoadedPattern(blockMap, localToWorld, baseplatePos,
                controllerWP, bePos, origin, minY, maxY, controller, script);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visibility
    // ─────────────────────────────────────────────────────────────────────────

    public void applyVisibility() {
        if (renderer == null || pattern == null || SHARED_LEVEL == null) return;
        PhantasiaScript.Step step = script.getActiveStep(playbackTick);

        Set<BlockPos> next = new HashSet<>();
        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            if (isBlockVisible(e.getKey(), e.getValue(), step))
                next.add(e.getValue());
        }
        // Baseplate positions are managed inside the renderer via setBaseplatePositions.
        // We pass only machine-block world positions here so alpha transitions don't
        // affect the floor.
        renderer.setVisible(next);
    }

    private boolean isBlockVisible(BlockPos local, BlockPos world, PhantasiaScript.Step step) {
        if (viewFilter != ViewFilter.ALL) {
            Set<BlockPos> fs = getFilterSet(viewFilter);
            return fs == null || fs.contains(world);
        }
        if (manualLayer >= 0) return local.getY() == manualLayer;
        if (buildOrderMode) {
            int g = pattern.getGroupIndex(local);
            return g != -1 && g <= buildOrderGroup;
        }
        if (step != null) return step.filter().test(local);
        return true;
    }

    public void applyViewFilter(ViewFilter vf) {
        if (viewFilter == vf) return;
        viewFilter = vf;
        applyVisibility();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter sets (lazy build)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the available shapes represent genuinely different machine sizes
     * (different XZ footprint), as opposed to coil-tier variations of the same structure.
     *
     * Strategy: compare the total block count of each shape. Coil-tier variants have
     * identical block counts (same structure, different materials). Size variants have
     * different block counts. This is data-driven and requires no machine-type checks.
     */
    private boolean computeHasRealSizeVariants() {
        if (availableShapes == null || availableShapes.size() <= 1) return false;
        int firstCount = countBlocks(availableShapes.get(0));
        for (int i = 1; i < availableShapes.size(); i++) {
            if (countBlocks(availableShapes.get(i)) != firstCount) return true;
        }
        return false;
    }

    private static int countBlocks(MultiblockShapeInfo shape) {
        int count = 0;
        for (BlockInfo[][] layer : shape.getBlocks())
            for (BlockInfo[] row : layer)
                for (BlockInfo b : row)
                    if (b != null && b.getBlockState() != null && !b.getBlockState().isAir())
                        count++;
        return count;
    }

    private void buildFilterSets() {
        if (pattern == null || SHARED_LEVEL == null) return;
        filteredHatchBus   = new HashSet<>();
        filteredEnergyIO   = new HashSet<>();
        filteredHasBE      = pattern.blockEntityWorldPos;
        filteredController = pattern.controllerWorldPos != null
                ? Set.of(pattern.controllerWorldPos) : Set.of();

        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            BlockPos wp = e.getValue();
            if (wp.equals(pattern.controllerWorldPos)) continue;
            BlockState state = SHARED_LEVEL.getBlockState(wp);
            if (!(state.getBlock() instanceof MetaMachineBlock)) continue;
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (rl == null) continue;
            String p = rl.getPath();
            if (p.contains("hatch") || p.contains("bus")
                    || p.contains("muffler") || p.contains("maintenance"))
                filteredHatchBus.add(wp);
            if (p.contains("energy") || p.contains("dynamo")
                    || p.contains("laser") || p.contains("power"))
                filteredEnergyIO.add(wp);
        }
    }

    private Set<BlockPos> getFilterSet(ViewFilter vf) {
        if (filteredHatchBus == null) buildFilterSets();
        return switch (vf) {
            case HATCHES_BUSES  -> filteredHatchBus;
            case ENERGY_IO      -> filteredEnergyIO;
            case BLOCK_ENTITIES -> filteredHasBE;
            case CONTROLLER     -> filteredController;
            default             -> null;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // tick()
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Advance camera lerp one game-tick.
        if (camera != null) camera.tick();

        // Caption fades
        if (captionCurrent != null && captionAlpha < 1f)
            captionAlpha = Math.min(1f, captionAlpha + 0.1f);
        if (captionOutgoing != null) {
            captionOutAlpha -= 0.1f;
            if (captionOutAlpha <= 0f) { captionOutgoing = null; captionOutAlpha = 0f; }
        }

        // Build-order pulse
        if (buildOrderMode) {
            buildPulse += buildPulseUp ? 0.05f : -0.05f;
            if (buildPulse >= 1f) { buildPulse = 1f; buildPulseUp = false; }
            if (buildPulse <= 0f) { buildPulse = 0f; buildPulseUp = true;  }
        }

        if (!playing || scrubbing || buildOrderMode || script == null
                || viewFilter != ViewFilter.ALL) return;

        int prevTick = playbackTick;
        tickAccum += playbackSpeed;
        while (tickAccum >= 1f) { tickAccum -= 1f; playbackTick++; }
        if (playbackTick >= script.getTotalTicks()) {
            playbackTick = (int) script.getTotalTicks();
            playing = false;
        }

        PhantasiaScript.Step step = script.getActiveStep(playbackTick);

        if (step != null && step.forceCoil() != -1 && step.forceCoil() != coilIndex) {
            coilIndex = step.forceCoil();
            updateCoilType();
        }

        if (playbackTick != prevTick && step != lastAppliedStep) {
            lastAppliedStep = step;

            // Script camera — PhantasiaCamera enforces locked/unlocked ownership.
            if (step != null && step.hasCamera() && camera != null) {
                float zoom = step.zoom() > 0 ? step.zoom() : camera.getZoom();
                camera.scriptDrive(step.yaw(), step.pitch(), zoom,
                        step.lerpType(), step.lerpTicks());
            }

            // Script shape change
            if (step != null && step.forceShape() != -1
                    && step.forceShape() != shapeIndex
                    && availableShapes != null
                    && step.forceShape() < availableShapes.size()) {

                shapeIndex = step.forceShape();
                // Hard-reset camera so the script reclaims authority for the new shape.
                if (camera != null)
                    camera.hardReset(getFacingYaw(), CAM_DEFAULT_PITCH, CAM_DEFAULT_ZOOM,
                            0, 0, 0);
                // Close old renderer before allocating the new one.
                if (renderer != null) { renderer.close(); renderer = null; }
                pattern = null;
                init();
                return; // init() already calls applyVisibility
            }

            applyVisibility();
            updateMachineState(step);
            updateCaptionForStep(step);
        }
    }

    private void updateCaptionForStep(PhantasiaScript.Step step) {
        String next = step != null ? step.caption() : null;
        if (!Objects.equals(next, captionCurrent)) {
            captionOutgoing = captionCurrent;
            captionOutAlpha = captionAlpha;
            captionCurrent  = next;
            captionAlpha    = 0f;
        }
    }

    private void updateMachineState(PhantasiaScript.Step step) {
        if (pattern == null || pattern.controller == null) return;
        boolean working = step != null && step.working()
                && playbackTick < script.getTotalTicks();
        if (pattern.controller instanceof WorkableMultiblockMachine w) {
            RecipeLogic logic = w.getRecipeLogic();
            if ((logic.getStatus() == RecipeLogic.Status.WORKING) != working)
                logic.setStatus(working
                        ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
        }
        var rs = pattern.controller.getRenderState();
        var ap = com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_ACTIVE;
        if (rs.hasProperty(ap) && rs.getValue(ap) != working)
            pattern.controller.setRenderState(rs.setValue(ap, working));
    }

    private void updateCoilType() {
        if (pattern == null || pattern.blockMap == null) return;
        BlockInfo newCoil = COIL_TIERS.get(coilIndex);
        for (Map.Entry<BlockPos, BlockInfo> e : pattern.blockMap.entrySet()) {
            if (e.getValue().getBlockState().getBlock()
                    instanceof com.gregtechceu.gtceu.common.block.CoilBlock) {
                e.setValue(newCoil);
                if (SHARED_LEVEL != null)
                    SHARED_LEVEL.setBlock(e.getKey(), newCoil.getBlockState(), 3);
            }
        }
        // invalidate() forces a rebake even though the POSITIONS haven't changed —
        // only the block states at those positions have changed.
        if (renderer != null) renderer.invalidate();
        applyVisibility();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // render()
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        activeButtons.clear();

        int pw = getCurrentPanelWidth();
        int sw = this.width  - pw;
        int sh = this.height - TIMELINE_H - CAPTION_STRIP_H;

        // Background fill
        g.fill(0, 0, this.width, this.height, C_BG);

        // ── 3-D scene ─────────────────────────────────────────────────────────
        if (renderer != null && camera != null) {
            CameraView view = camera.getView(partial);
            renderer.setMousePos(mx, my);
            // Viewport starts below the caption strip
            renderer.render(view, 0, CAPTION_STRIP_H, sw, sh);
            BlockHitResult hit = renderer.getLastHitResult();
            hoveredPos = (hit != null && hit.getType() == HitResult.Type.BLOCK)
                    ? hit.getBlockPos() : null;
        }

        // ── GUI overlays (on top of 3-D) ──────────────────────────────────────
        renderCaption(g);
        if (buildOrderMode && pattern != null) renderBuildPulseBanner(g);
        if (showMistakes && script != null && script.hasCommonMistakes())
            renderMistakesOverlay(g);

        renderTimeline(g, mx, my);
        renderSidePanel(g, mx, my);
        regBtn(g, mx, my, 10, 10, 50, 18, "Back", this::onClose);

        super.render(g, mx, my, partial);

        // Hovered-block name in panel gutter
        int px = this.width - pw;
        if (hoveredPos != null && SHARED_LEVEL != null) {
            try {
                BlockState st = SHARED_LEVEL.getBlockState(hoveredPos);
                if (!st.isAir()) {
                    g.drawString(font, trunc(st.getBlock().getName().getString(), pw - 20),
                            px + 10, this.height - 20, C_DIM, false);
                    if (mx < px) g.renderTooltip(font, st.getBlock().getName(), mx, my);
                }
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timeline
    // ─────────────────────────────────────────────────────────────────────────

    private void renderTimeline(GuiGraphics g, int mx, int my) {
        int px   = this.width - getCurrentPanelWidth();
        int barY = this.height - TIMELINE_H;

        g.fill(0, barY, px, this.height, C_TL_BG);
        g.fill(0, barY, px, barY + 1, C_ACCENT);

        int x = 6;
        regBtn(g, mx, my, x, barY + 4, 18, 17, playing ? "⏸" : "▶", () -> {
            if (!playing && playbackTick >= script.getTotalTicks()) {
                // At end — restart from the beginning
                playbackTick    = 0;
                tickAccum       = 0f;
                lastAppliedStep = null;
                applyVisibility();
            }
            playing = !playing;
        });
        x += 22;
        regBtn(g, mx, my, x, barY + 4, 18, 17,
                camera != null && camera.isLocked() ? "🔒" : "🔓",
                () -> { if (camera != null) camera.toggleLocked(); });
        x += 22;
        String spd = playbackSpeed == 0.5f ? "½x" : playbackSpeed == 2f ? "2x" : "1x";
        regBtn(g, mx, my, x, barY + 4, 24, 17, spd, () ->
                playbackSpeed = playbackSpeed == 1f ? 2f : playbackSpeed == 2f ? 0.5f : 1f);

        int tx = 80, tw = px - tx - 65, midY = barY + TIMELINE_H / 2;
        g.fill(tx, midY - 1, tx + tw, midY + 1, 0xFF1A2C3C);

        float total = script.getTotalTicks();
        for (PhantasiaScript.Step s : script.getSteps()) {
            int mx2 = tx + (int)(tw * s.tickOffset() / total);
            g.fill(mx2 - 1, midY - 4, mx2 + 1, midY + 4, 0xAAFFFFFF);
        }
        float prog = total > 0 ? playbackTick / total : 0f;
        g.fill(tx, midY - 1, tx + (int)(tw * prog), midY + 1, C_PROG);
        g.fill(tx + (int)(tw * prog) - 2, midY - 4,
               tx + (int)(tw * prog) + 2, midY + 4, C_ACCENT);
        g.drawString(font, formatTicks(playbackTick), tx + tw + 8, barY + 9, C_DIM, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caption strip
    // ─────────────────────────────────────────────────────────────────────────

    private void renderCaption(GuiGraphics g) {
        if (captionCurrent == null && captionOutgoing == null) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        int sw     = this.width - getCurrentPanelWidth();
        int stripY = this.height - TIMELINE_H - CAPTION_STRIP_H;
        g.fill(0, stripY, sw, stripY + CAPTION_STRIP_H, 0xDD08080F);
        g.fill(0, stripY, sw, stripY + 1, 0xFF4FC3F7);
        int ty = stripY + (CAPTION_STRIP_H - 8) / 2;

        if (captionOutgoing != null && captionOutAlpha > 0.05f) {
            int col = ((int)(captionOutAlpha * 160) << 24) | 0xBBBBBB;
            g.drawCenteredString(font, trunc(captionOutgoing, sw - 20), sw / 2, ty, col);
        }
        if (captionCurrent != null && captionAlpha > 0.05f) {
            int col = ((int)(captionAlpha * 255) << 24) | 0xDDDDDD;
            g.drawCenteredString(font, trunc(captionCurrent, sw - 20), sw / 2, ty, col);
        }
        g.pose().popPose();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build-order banner
    // ─────────────────────────────────────────────────────────────────────────

    private void renderBuildPulseBanner(GuiGraphics g) {
        if (buildOrderGroup >= pattern.buildOrder.size()) return;
        int sceneW = this.width - getCurrentPanelWidth();
        int alpha  = (int)(buildPulse * 0xBB);
        int col    = (alpha << 24) | (C_HILIGHT & 0x00FFFFFF);
        int by     = TIMELINE_H;
        g.fill(0, by, sceneW, by + 18, ((alpha / 3) << 24) | 0x1A1400);
        g.fill(0, by + 17, sceneW, by + 18, col);
        List<BlockPos> grp = pattern.buildOrder.get(buildOrderGroup);
        g.drawCenteredString(font,
                "Next: Layer Y=" + grp.get(0).getY() + " — " + grp.size() + " block(s)",
                sceneW / 2, by + 5, col);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mistakes overlay
    // ─────────────────────────────────────────────────────────────────────────

    private void renderMistakesOverlay(GuiGraphics g) {
        List<PhantasiaScript.LocalWarning> local  = script.getCommonMistakes();
        List<String>                       global = script.getGlobalMistakes();
        int x = 8, y = TIMELINE_H + 26;
        int ph = (local.size() + global.size()) * 12 + 10;
        g.fill(x - 2, y - 2, x + 240, y + ph, 0xCC06060E);
        g.fill(x - 2, y - 2, x + 240, y - 1,  0xFFFF5252);
        for (var w : local) {
            g.drawString(font, "⚠ " + w.label(), x, y, w.color(), false);
            BlockPos lp = w.localPos();
            g.drawString(font,
                    " [" + lp.getX() + "," + lp.getY() + "," + lp.getZ() + "]",
                    x + font.width("⚠ " + w.label()), y, C_DIM, false);
            y += 12;
        }
        for (String m : global) {
            g.drawString(font, "• " + m, x, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Side panel
    // ─────────────────────────────────────────────────────────────────────────

    private void renderSidePanel(GuiGraphics g, int mx, int my) {
        int pw = getCurrentPanelWidth();
        int px = this.width - pw;
        activeButtons.removeIf(b -> b.x() >= px);

        g.fill(px, 0, this.width, this.height, C_PANEL);
        g.fill(px, 0, px + 1, this.height, C_ACCENT);

        int y = 10;
        g.drawString(font, trunc(definition.getLangValue(), pw - 20),
                px + 10, y, C_ACCENT, false);
        y += 20;
        if (sidePanelCollapsed) return;

        // Determine what kind of shape variation this multi has.
        // If shapes differ in their XZ block footprint → genuinely different sizes.
        // If shapes have the same footprint but different blocks → coil-tier variation.
        boolean hasCoilBlocks = pattern != null && pattern.blockMap.values().stream()
                .anyMatch(i -> i.getBlockState().getBlock()
                        instanceof com.gregtechceu.gtceu.common.block.CoilBlock);
        boolean hasRealSizeVariants = computeHasRealSizeVariants();
        boolean isCoilTierMachine   = hasCoilBlocks && !hasRealSizeVariants;

        if (isCoilTierMachine) {
            String cn = COIL_TIERS.get(coilIndex).getBlockState().getBlock()
                    .getName().getString();
            regBtn(g, mx, my, px + 10, y, pw - 20, 16, "Coil: " + cn, () -> {
                coilIndex = (coilIndex + 1) % COIL_TIERS.size();
                updateCoilType();
            });
            y += 20;
        }

        if (hasRealSizeVariants) {
            regBtn(g, mx, my, px + 10, y, pw - 20, 16,
                    "Structure Size: " + (shapeIndex + 1), () -> {
                        if (camera != null) camera.save();
                        shapeIndex = (shapeIndex + 1) % availableShapes.size();
                        if (renderer != null) { renderer.close(); renderer = null; }
                        pattern = null;
                        init();
                    });
            y += 20;
        }
        y += 5;

        int bW = 20, lW = pw - 60, lX = px + 30;
        if (!buildOrderMode) {
            g.drawString(font, "Manual Layer:", px + 10, y, C_DIM, false);
            y += 12;
            regBtn(g, mx, my, px + 10,           y, bW, 16, "<", () -> nudgeLayer(-1));
            regBtn(g, mx, my, lX,                y, lW, 16,
                    manualLayer < 0 ? "All" : "Layer " + manualLayer,
                    () -> { manualLayer = -1; applyVisibility(); });
            regBtn(g, mx, my, px + pw - 10 - bW, y, bW, 16, ">", () -> nudgeLayer(1));
            y += 25;
        } else {
            g.drawString(font, "Build Step:", px + 10, y, C_DIM, false);
            y += 12;
            regBtn(g, mx, my, px + 10,           y, bW, 16, "<", () -> buildOrderStep(-1));
            regBtn(g, mx, my, lX,                y, lW, 16,
                    "Group " + (buildOrderGroup + 1), () -> {});
            regBtn(g, mx, my, px + pw - 10 - bW, y, bW, 16, ">", () -> buildOrderStep(1));
            y += 25;
        }

        g.drawString(font, "Show:", px + 10, y, C_DIM, false);
        y += 12;
        ViewFilter[] vfs = ViewFilter.values();
        int fw = (pw - 25) / 2;
        for (int i = 0; i < vfs.length; i++) {
            final ViewFilter vf = vfs[i];
            int bx = (i % 2 == 0) ? px + 10 : px + 15 + fw;
            regBtn(g, mx, my, bx, y, fw, 14, vf.name(), viewFilter == vf,
                    () -> toggleViewFilter(vf));
            if (i % 2 != 0 || i == vfs.length - 1) y += 17;
        }

        y += 8;
        if (script != null && script.hasCommonMistakes()) {
            regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "⚠", "Common Mistakes",
                    showMistakes, () -> showMistakes = !showMistakes);
            y += 20;
        }
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🧱", "Build Mode",
                buildOrderMode, () -> { buildOrderMode = !buildOrderMode; applyVisibility(); });
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🗺", "Footprint",
                false, this::openFootprintScreen);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "⊕", "Center Camera",
                false, this::centerCamera);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🔍", "Block List",
                false, this::openBlockFilterScreen);
        y += 20;

        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getAbilities().instabuild) {
            regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "✏", "Edit Script",
                    false, this::openScriptEditor);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (PhantasiaUIUtils.ButtonAction b : activeButtons) {
            if (b.hit(mx, my)) { b.action().run(); return true; }
        }

        int px  = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;

        // Timeline scrub click
        if (btn == 0 && my >= tlY && mx < px && !buildOrderMode) {
            int tx = 80, tw = px - tx - 65;
            if (mx >= tx && mx <= tx + tw) {
                playing   = false;
                scrubbing = true;
                scrubTo((float)(mx - tx) / tw);
                return true;
            }
        }

        // Scene-area interactions
        if (mx < px && my > CAPTION_STRIP_H && my < tlY) {
            if (btn == 1 && hoveredPos != null && SHARED_LEVEL != null) {
                try {
                    if (!SHARED_LEVEL.getBlockState(hoveredPos).isAir()) {
                        if (camera != null) camera.save();
                        Minecraft.getInstance().setScreen(
                                new PhantasiaBlockInspectScreen(hoveredPos, pattern, this));
                        return true;
                    }
                } catch (Exception ignored) {}
            }
            if (btn == 2 && camera != null && !camera.isLocked()) {
                isPanning = true; return true;
            }
            if (btn == 0) return true; // consume so vanilla doesn't do anything
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int px  = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;
        if (mx >= px || my >= tlY || camera == null || camera.isLocked())
            return super.mouseDragged(mx, my, btn, dx, dy);

        if (btn == 2 && isPanning) {
            Vector3f right = new Vector3f(), up = new Vector3f();
            camera.getRightAndUp(right, up);
            float s = CAM_PAN_SPEED;
            camera.pan(
                    (right.x * (float)-dx + up.x * (float)dy) * s,
                    (right.y * (float)-dx + up.y * (float)dy) * s,
                    (right.z * (float)-dx + up.z * (float)dy) * s);
            return true;
        }

        if (btn == 0 || btn == 1) {
            camera.orbit((float)dx * CAM_ORBIT_SENSITIVITY,
                         (float)dy * CAM_ORBIT_SENSITIVITY);
            return true;
        }

        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= this.width - getCurrentPanelWidth()) return false;
        if (camera == null || camera.isLocked()) return false;
        camera.zoom(delta > 0 ? CAM_ZOOM_IN_FACTOR : CAM_ZOOM_OUT_FACTOR,
                CAM_ZOOM_MIN, CAM_ZOOM_MAX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 2 || btn == 0) isPanning = false;
        if (scrubbing) {
            int px = this.width - getCurrentPanelWidth();
            scrubTo(Mth.clamp((float)(mx - 80) / (px - 80 - 65), 0f, 1f));
            scrubbing = false;
            applyVisibility();
        }
        return super.mouseReleased(mx, my, btn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleViewFilter(ViewFilter vf) {
        if (viewFilter == vf) {
            viewFilter = ViewFilter.ALL;
            if (wasPlayingBeforeFilter) playing = true;
            wasPlayingBeforeFilter = false;
        } else {
            if (viewFilter == ViewFilter.ALL) {
                wasPlayingBeforeFilter = playing;
                playing = false;
            }
            viewFilter = vf;
        }
        applyVisibility();
    }

    private void centerCamera() {
        if (camera == null || pattern == null) return;
        resetCameraToDefault(LerpType.EASE_OUT, 12);
    }

    private void nudgeLayer(int delta) {
        if (pattern == null) return;
        manualLayer = manualLayer < 0
                ? (delta < 0 ? pattern.maxY : pattern.minY)
                : Mth.clamp(manualLayer + delta, pattern.minY, pattern.maxY);
        applyVisibility();
    }

    private void buildOrderStep(int delta) {
        if (pattern == null) return;
        buildOrderGroup = Mth.clamp(buildOrderGroup + delta, 0,
                pattern.buildOrder.size() - 1);
        applyVisibility();
    }

    private void scrubTo(float t) {
        scrubbing    = true;
        playing      = false;
        playbackTick = (int)(Mth.clamp(t, 0f, 1f) * script.getTotalTicks());
        PhantasiaScript.Step step = script.getActiveStep(playbackTick);
        if (step != lastAppliedStep) {
            lastAppliedStep = step;
            updateCaptionForStep(step);
            applyVisibility();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-screen navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void openScriptEditor() {
        if (camera != null) camera.save();
        String machineId = definition.getId().toString();
        PhantasiaScriptData current = script.getSourceData();
        if (current == null) current = PhantasiaScriptData.defaultFor(machineId);
        Minecraft.getInstance().setScreen(
                new PhantasiaScriptEditorScreen(this, machineId, current));
    }

    private void openFootprintScreen() {
        if (pattern == null) return;
        if (camera != null) camera.save();
        Minecraft.getInstance().setScreen(
                new PhantasiaFootprintScreen(pattern, this, script));
    }

    private void openBlockFilterScreen() {
        if (pattern == null) return;
        if (camera != null) camera.save();
        Minecraft.getInstance().setScreen(
                new PhantasiaBlockFilterScreen(pattern, script, viewFilter, this));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors (used by sub-screens and script editor)
    // ─────────────────────────────────────────────────────────────────────────

    public float getRotationYaw()   { return camera != null ? camera.getYaw()   : 0f; }
    public float getRotationPitch() { return camera != null ? camera.getPitch() : 0f; }
    public PhantasiaLoadedPattern getLoadedPattern() { return pattern; }

    // ─────────────────────────────────────────────────────────────────────────
    // Button helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void regBtn(GuiGraphics g, int mx, int my,
                        int x, int y, int w, int h, String label, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov, C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private void regBtn(GuiGraphics g, int mx, int my,
                        int x, int y, int w, int h,
                        String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov,
                active ? C_BTN_ACT : C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private void regIconBtn(GuiGraphics g, int mx, int my,
                            int x, int y, int w, int h,
                            String icon, String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawIconBtn(g, font, x, y, w, h, icon, label, hov,
                active ? C_BTN_ACT : C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc
    // ─────────────────────────────────────────────────────────────────────────

    private int getCurrentPanelWidth() {
        return sidePanelCollapsed ? COLLAPSED_PANEL_W : FULL_PANEL_W;
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2)
            s = s.substring(0, s.length() - 2) + "\u2026";
        return s;
    }

    private static String formatTicks(int t) {
        return String.format("%d.%02ds", t / 20, (t % 20) * 5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        if (renderer != null) { renderer.close(); renderer = null; }
        invalidateSharedLevel();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
