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

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
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
import net.phoenix.core.integration.phantasia.utils.PhantasiaThemeUtils;
import net.phoenix.core.integration.phantasia.utils.PhantasiaUIUtils;
import net.phoenix.core.mixin.gtceu.AccessorWorldSceneRenderer;

import lombok.Getter;
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
        NEXT_REGION = 0;
    }

    public static BlockPos getOriginForCurrentPattern() {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof PhantasiaSceneScreen pss && pss.pattern != null) return pss.pattern.origin;
        if (mc.screen instanceof PhantasiaFootprintScreen pfs && pfs.getPattern() != null)
            return pfs.getPattern().origin;
        return BlockPos.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────────────────────────────────

    private static final int FULL_PANEL_W = 168;
    private static final int COLLAPSED_PANEL_W = 18;
    private static final int TIMELINE_H = 26;
    /** FIX (F6): solid caption strip replaces the old floating text overlay. */
    private static final int CAPTION_STRIP_H = 22;

    // ─────────────────────────────────────────────────────────────────────────
    // Core state
    // ─────────────────────────────────────────────────────────────────────────

    private final Screen parent;
    public final MultiblockMachineDefinition definition;
    // Non-final so the editor can hot-swap the compiled script after saving JSON
    private PhantasiaScript script;

    private PhantasiaLoadedPattern pattern;
    private SceneWidget sceneWidget;
    private int shapeIndex = 0;
    private List<MultiblockShapeInfo> availableShapes = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Camera
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FIX (B2): single source-of-truth for zoom distance.
     * Never re-read from the renderer after a scroll — the renderer defers its
     * camera write by one frame, so a re-read returns the stale pre-scroll value.
     */
    // ─────────────────────────────────────────────────────────────────────────
    // Camera defaults — change these to adjust the fallback starting position
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * How far above the machine's vertical midpoint the look-at target sits.
     * 0 = aim exactly at midpoint. Positive = look higher up the machine.
     */
    private static final float CAM_TARGET_Y_BIAS = 0.0f;

    /**
     * Default pitch (vertical tilt) when no script camera is declared.
     * Negative = looking downward. Range: -85 (steep) to -5 (nearly horizontal).
     * -30 gives a comfortable isometric-ish view from slightly above.
     */
    private static final float CAM_DEFAULT_PITCH = 5.0f;

    /**
     * Default zoom distance when opening the screen fresh.
     * Larger machines may need a higher value; scripts can override per-step.
     */
    private static final float CAM_DEFAULT_ZOOM = 40.0f;

    /**
     * Zoom multiplier per scroll tick. 0.9 = 10% closer per tick (feel free to tune).
     */
    private static final float CAM_ZOOM_IN_FACTOR = 0.9f;
    private static final float CAM_ZOOM_OUT_FACTOR = 1.1f;

    /** Hard zoom limits in world units. */
    private static final float CAM_ZOOM_MIN = 2.0f;
    private static final float CAM_ZOOM_MAX = 100.0f;

    /** Orbit sensitivity: degrees rotated per pixel dragged. */
    private static final float CAM_ORBIT_SENSITIVITY = 0.5f;

    /** Pan speed: world units moved per pixel dragged (middle-click pan). */
    private static final float CAM_PAN_SPEED = 0.02f;

    // ─────────────────────────────────────────────────────────────────────────
    // Camera runtime state
    // ─────────────────────────────────────────────────────────────────────────

    private float currentZoomDist = CAM_DEFAULT_ZOOM;
    // Camera accessors for the editor's "Capture Camera" feature
    @Getter
    private float rotationYaw = -135.0f;   // overwritten in resolveStartingCamera()
    @Getter
    private float rotationPitch = CAM_DEFAULT_PITCH;
    private float cameraTargetX, cameraTargetY, cameraTargetZ;
    private boolean isCameraLocked = true;

    /**
     * True once the player has dragged or scrolled.
     * While true, system-driven re-inits preserve the player's exact position
     * instead of snapping back to the script/facing default.
     * Cleared only by centerCamera().
     */
    private boolean playerHasMovedCamera = false;

    /**
     * True when the last camera move was made by the system (script step, init),
     * NOT by the player. On the player's very first drag after a system move we
     * sync our yaw/pitch fields FROM the renderer before adding the drag delta,
     * so there is no position jump.
     */
    private boolean cameraMovedBySystem = false;

    private boolean isPanning = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Playback
    // ─────────────────────────────────────────────────────────────────────────

    private boolean playing = true;
    private int playbackTick = 0;
    private float tickAccum = 0f;
    private float playbackSpeed = 1.0f;
    private boolean scrubbing = false;
    private PhantasiaScript.Step lastAppliedStep = null;

    // ─────────────────────────────────────────────────────────────────────────
    // View / filter
    // ─────────────────────────────────────────────────────────────────────────

    public enum ViewFilter {
        ALL,
        HATCHES_BUSES,
        ENERGY_IO,
        BLOCK_ENTITIES,
        CONTROLLER
    }

    private ViewFilter viewFilter = ViewFilter.ALL;
    /**
     * FIX (B5): remember whether the script was playing before a filter was activated,
     * so we can restore playback when the filter is cleared.
     */
    private boolean wasPlayingBeforeFilter = false;

    private int manualLayer = -1;

    // Filter sets built lazily from the loaded pattern
    private Set<BlockPos> filteredHatchBus = null;
    private Set<BlockPos> filteredEnergyIO = null;
    private Set<BlockPos> filteredHasBE = null;
    private Set<BlockPos> filteredController = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Build-order mode
    // ─────────────────────────────────────────────────────────────────────────

    private boolean buildOrderMode = false;
    private int buildOrderGroup = 0;
    private float buildPulse = 0f;
    private boolean buildPulseUp = true;

    // ─────────────────────────────────────────────────────────────────────────
    // GPU baking
    // ─────────────────────────────────────────────────────────────────────────

    private final Set<BlockPos> currentlyBakedPositions = new HashSet<>();

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

    private float captionAlpha = 0f;
    private String captionCurrent = null;
    private String captionOutgoing = null;
    private float captionOutAlpha = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    private final List<PhantasiaUIUtils.ButtonAction> activeButtons = new ArrayList<>();
    private boolean sidePanelCollapsed = false;
    private BlockPos hoveredPos = null;

    /** Exposed so PhantasiaBlockFilterScreen can toggle it. */
    public boolean showMistakes = false;
    public int selectedTierIndex = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public PhantasiaSceneScreen(MultiblockMachineDefinition definition, Screen parent) {
        super(Component.literal(definition.getLangValue()));
        this.parent = parent;
        this.definition = definition;
        this.script = PhantasiaScripts.get(definition);
    }

    /** Called by PhantasiaScriptEditorScreen after saving to hot-swap the compiled script. */
    public void reloadScript() {
        this.script = PhantasiaScripts.get(definition);
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
            if (Minecraft.getInstance().level == null) {
                onClose();
                return;
            }
            SHARED_LEVEL = new TrackedDummyWorld();
        }

        availableShapes = definition.getMatchingShapes();
        if (pattern == null && !availableShapes.isEmpty()) {
            if (shapeIndex >= availableShapes.size()) shapeIndex = 0;
            pattern = loadPattern(availableShapes.get(shapeIndex));
        }

        int sw = this.width - getCurrentPanelWidth();
        int sh = this.height - TIMELINE_H - CAPTION_STRIP_H;

        if (sceneWidget == null) {
            sceneWidget = new SceneWidget(0, 0, sw, sh, SHARED_LEVEL);
            sceneWidget.setDraggable(true);
            WorldSceneRenderer r = sceneWidget.getRenderer();
            r.useCacheBuffer(true);
            if (r instanceof AccessorWorldSceneRenderer acc) acc.setEndBatchLast(true);
        } else {
            sceneWidget.setSize(sw, sh);
        }

        if (pattern != null) {
            if (!playerHasMovedCamera) {
                // Fresh open or player hasn't touched the camera yet.
                // Recalculate the look-at target and resolve starting angles.
                BlockPos cp = pattern.controllerWorldPos != null ? pattern.controllerWorldPos : pattern.origin;
                float midY = pattern.origin.getY() + (pattern.minY + pattern.maxY) * 0.5f + CAM_TARGET_Y_BIAS;
                cameraTargetX = cp.getX() + 0.5f;
                cameraTargetY = midY;
                cameraTargetZ = cp.getZ() + 0.5f;
                resolveStartingCamera();

                // Tell the widget about our starting angles and flag a sync so the
                // first player drag reads from the renderer instead of our stale fields.
                sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);
                cameraMovedBySystem = true;
            } else {
                // Player has already moved the camera — preserve every field exactly.
                // Re-sync the widget's internal orbit state so its drag handler starts
                // from the right angle (setSize() above may have reset it).
                // Do NOT set cameraMovedBySystem — our fields are authoritative and we
                // don't want syncFieldsFromRenderer() to overwrite them on the next drag.
                sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);
                // cameraMovedBySystem intentionally left as-is (false when player was last to move)
            }

            updateCameraTarget(currentZoomDist);
            currentlyBakedPositions.clear();
            applyVisibility();
        }
    }

    /**
     * FIX (B4): derive the ideal starting yaw from the controller block's HORIZONTAL_FACING
     * property so the camera always approaches from in front of the machine.
     */
    private float getFacingYaw() {
        if (pattern == null || pattern.controllerWorldPos == null || SHARED_LEVEL == null) return -135.0f;
        try {
            BlockState ctrl = SHARED_LEVEL.getBlockState(pattern.controllerWorldPos);
            if (ctrl.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return switch (ctrl.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    // If facing North, camera should be at the North looking South (180)
                    case NORTH -> 180.0f;
                    // If facing South, camera should be at the South looking North (0)
                    case SOUTH -> 0.0f;
                    // If facing West, camera should be at the West looking East (270)
                    case WEST -> 270.0f;
                    // If facing East, camera should be at the East looking West (90)
                    case EAST -> 90.0f;
                    default -> -135.0f;
                };
            }
        } catch (Exception ignored) {}
        return -135.0f;
    }

    /**
     * Sets rotationYaw, rotationPitch and currentZoomDist for a "fresh open":
     * 1. Script step-0 declared camera — script author chose the ideal angle.
     * 2. Controller HORIZONTAL_FACING — face the machine front, isometric pitch.
     * 3. Isometric fallback — safe default when facing is unavailable.
     *
     * Zoom is auto-sized to the machine's height so small and large machines both
     * fit comfortably in frame. Only called when playerHasMovedCamera is false.
     */
    private void resolveStartingCamera() {
        // Auto-size zoom: taller machines need more distance.
        // Base distance of CAM_DEFAULT_ZOOM covers ~8 blocks; scale linearly above that.
        if (pattern != null) {
            int machineH = pattern.maxY - pattern.minY + 1;
            currentZoomDist = CAM_DEFAULT_ZOOM + Math.max(0, machineH - 8) * 1.5f;
        } else {
            currentZoomDist = CAM_DEFAULT_ZOOM;
        }

        // 1. Script step-0 declared camera
        if (!script.getSteps().isEmpty()) {
            PhantasiaScript.Step first = script.getSteps().get(0);
            if (first.hasCamera()) {
                rotationYaw = first.yaw();
                rotationPitch = first.pitch();
                return;
            }
        }

        // 2. Controller facing yaw + named default pitch
        rotationYaw = getFacingYaw();
        rotationPitch = CAM_DEFAULT_PITCH;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern loading
    // ─────────────────────────────────────────────────────────────────────────

    private PhantasiaLoadedPattern loadPattern(MultiblockShapeInfo shape) {
        int regionIndex = NEXT_REGION++;
        BlockPos origin = new BlockPos(regionIndex * REGION_SIZE, 50, 0);

        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        Map<BlockPos, BlockPos> localToWorld = new HashMap<>();
        Set<BlockPos> baseplatePos = new HashSet<>();
        Set<BlockPos> bePos = new HashSet<>();
        BlockPos controllerWP = null;
        MultiblockControllerMachine controller = null;

        BlockInfo floor = BlockInfo.fromBlockState(Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        BlockInfo[][][] raw = shape.getBlocks();
        int sxLen = raw.length;
        int szLen = sxLen > 0 && raw[0].length > 0 ? raw[0][0].length : 0;
        int padX = Math.max(2, sxLen / 2 + 1), padZ = Math.max(2, szLen / 2 + 1);
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
                                controller = ctrl;
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
        if (minY > maxY) {
            minY = 0;
            maxY = 0;
        }

        return new PhantasiaLoadedPattern(blockMap, localToWorld, baseplatePos,
                controllerWP, bePos, origin, minY, maxY, controller, script);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FIX (B2): writes dist into {@code currentZoomDist} immediately so subsequent
     * drag-rotation calls read the correct value without consulting the renderer.
     *
     * Also clamps the computed eye Y so it never goes below the baseplate surface
     * (baseplateY + 0.5). This prevents large machines from pushing the camera
     * underground when auto-zoom makes the distance large enough that even a shallow
     * pitch results in the eye being below the floor.
     */
    private void updateCameraTarget(float dist) {
        if (sceneWidget == null) return;
        this.currentZoomDist = dist;

        double yr = Math.toRadians(rotationYaw);
        double pr = Math.toRadians(rotationPitch);
        float nx = (float) (Math.cos(pr) * Math.sin(yr));
        float ny = (float) Math.sin(pr);
        float nz = (float) (Math.cos(pr) * Math.cos(yr));

        float eyeX = cameraTargetX + nx * dist;
        float eyeY = cameraTargetY + ny * dist;
        float eyeZ = cameraTargetZ + nz * dist;

        // Clamp: eye must always be above the baseplate surface.
        // Baseplate blocks sit at origin.Y - 1, so the top face is at origin.Y.
        // We add a small margin (0.5) so the camera never clips into the floor.
        if (pattern != null) {
            float baseplateTopY = pattern.origin.getY() + 0.5f;
            eyeY = Math.max(eyeY, baseplateTopY);
        }

        sceneWidget.getRenderer().setCameraLookAt(
                new Vector3f(eyeX, eyeY, eyeZ),
                new Vector3f(cameraTargetX, cameraTargetY, cameraTargetZ),
                new Vector3f(0, 1, 0));
    }

    /**
     * Reads the renderer's current eye direction back into rotationYaw, rotationPitch,
     * and currentZoomDist. Called once on the player's first drag/scroll after any
     * system-driven camera move so there is no position jump.
     *
     * Does NOT overwrite cameraTargetX/Y/Z — those are set by init() and centerCamera()
     * and are always authoritative. Overwriting them here would break the look-at point
     * when the widget's own drag handler drifts it slightly.
     */
    private void syncFieldsFromRenderer() {
        if (sceneWidget == null) return;
        WorldSceneRenderer r = sceneWidget.getRenderer();
        Vector3f eye = r.getEyePos();
        Vector3f target = r.getLookAt();
        if (eye == null || target == null) return;

        float dx = eye.x - target.x;
        float dy = eye.y - target.y;
        float dz = eye.z - target.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.001f) return;

        rotationPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(dy / dist, -1f, 1f)));
        rotationYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        currentZoomDist = dist;
        // NOTE: cameraTargetX/Y/Z deliberately NOT updated here — they remain
        // as set by init() / centerCamera() which are the authoritative look-at values.
    }

    /** Recomputes and uploads the visible block set to the GPU — call after any state change. */
    public void applyVisibility() {
        if (sceneWidget == null || pattern == null) return;
        PhantasiaScript.Step step = script.getActiveStep(playbackTick);
        Set<BlockPos> next = new HashSet<>(pattern.baseplatePositions);
        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet())
            if (isBlockVisible(e.getKey(), e.getValue(), step)) next.add(e.getValue());

        if (!next.equals(currentlyBakedPositions)) {
            currentlyBakedPositions.clear();
            currentlyBakedPositions.addAll(next);
            sceneWidget.setRenderedCore(currentlyBakedPositions, null);
            sceneWidget.getRenderer().needCompileCache();
        }
    }

    /**
     * FIX (B5): a non-ALL filter has TOTAL authority — the script filter is skipped.
     * Ensures clicking a filter always shows what the label says, regardless of playback state.
     */
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

    /**
     * Called by {@link PhantasiaBlockFilterScreen#onClose()} so the filter change is
     * applied immediately on returning to this screen (FIX B5/B6).
     */
    public void applyViewFilter(ViewFilter vf) {
        if (viewFilter == vf) return;
        viewFilter = vf;
        applyVisibility();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter sets (lazy build)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildFilterSets() {
        if (pattern == null || SHARED_LEVEL == null) return;
        filteredHatchBus = new HashSet<>();
        filteredEnergyIO = new HashSet<>();
        filteredHasBE = pattern.blockEntityWorldPos;
        // FIX: CONTROLLER filter now has a proper set
        filteredController = pattern.controllerWorldPos != null ? Set.of(pattern.controllerWorldPos) : Set.of();

        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            BlockPos wp = e.getValue();
            if (wp.equals(pattern.controllerWorldPos)) continue;
            BlockState state = SHARED_LEVEL.getBlockState(wp);
            if (!(state.getBlock() instanceof MetaMachineBlock)) continue;
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (rl == null) continue;
            String p = rl.getPath();
            if (p.contains("hatch") || p.contains("bus") || p.contains("muffler") || p.contains("maintenance"))
                filteredHatchBus.add(wp);
            if (p.contains("energy") || p.contains("dynamo") || p.contains("laser") || p.contains("power"))
                filteredEnergyIO.add(wp);
        }
    }

    private Set<BlockPos> getFilterSet(ViewFilter vf) {
        if (filteredHatchBus == null) buildFilterSets();
        return switch (vf) {
            case HATCHES_BUSES -> filteredHatchBus;
            case ENERGY_IO -> filteredEnergyIO;
            case BLOCK_ENTITIES -> filteredHasBE;
            case CONTROLLER -> filteredController;
            default -> null;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // tick()
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Caption fades
        if (captionCurrent != null && captionAlpha < 1f) captionAlpha = Math.min(1f, captionAlpha + 0.1f);
        if (captionOutgoing != null) {
            captionOutAlpha -= 0.1f;
            if (captionOutAlpha <= 0f) {
                captionOutgoing = null;
                captionOutAlpha = 0f;
            }
        }

        // Build-order pulse
        if (buildOrderMode) {
            buildPulse += buildPulseUp ? 0.05f : -0.05f;
            if (buildPulse >= 1f) {
                buildPulse = 1f;
                buildPulseUp = false;
            }
            if (buildPulse <= 0f) {
                buildPulse = 0f;
                buildPulseUp = true;
            }
        }

        // skip playback advance while a filter is active
        if (!playing || scrubbing || buildOrderMode || script == null || viewFilter != ViewFilter.ALL) return;

        int prevTick = playbackTick;
        tickAccum += playbackSpeed;
        while (tickAccum >= 1f) {
            tickAccum -= 1f;
            playbackTick++;
        }
        if (playbackTick >= script.getTotalTicks()) {
            playbackTick = (int) script.getTotalTicks();
            playing = false;
        }

        PhantasiaScript.Step step = script.getActiveStep(playbackTick);

        // Per-tick transitions
        if (step != null) {
            if (step.useCam() && isCameraLocked) {
                rotationYaw = step.yaw();
                rotationPitch = step.pitch();
                if (sceneWidget != null) {
                    // Keep the Widget's internal orbit logic in sync with the script values
                    sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);

                    // FIX: Flag that the system moved the camera.
                    // This ensures the NEXT player drag starts from the script's position.
                    cameraMovedBySystem = true;

                    // Refresh the actual renderer view
                    updateCameraTarget(currentZoomDist);
                }
            }
            if (step.forceCoil() != -1 && step.forceCoil() != coilIndex) {
                coilIndex = step.forceCoil();
                updateCoilType();
            }
        }

        // Heavy operations only on step boundary
        if (playbackTick != prevTick && step != lastAppliedStep) {
            lastAppliedStep = step;

            if (step != null && step.forceShape() != -1 && step.forceShape() != shapeIndex && availableShapes != null &&
                    step.forceShape() < availableShapes.size()) {

                // SAVE current state before init() resets variables to defaults
                float savedYaw = rotationYaw;
                float savedPitch = rotationPitch;
                float savedDist = currentZoomDist;
                float savedTX = cameraTargetX;
                float savedTY = cameraTargetY;
                float savedTZ = cameraTargetZ;
                boolean hadMoved = playerHasMovedCamera;

                shapeIndex = step.forceShape();
                pattern = null;
                init(); // Note: your new init() handles the cameraMovedBySystem flag

                if (hadMoved) {
                    // RESTORE exact position and target so it doesn't "shunt" to the center
                    rotationYaw = savedYaw;
                    rotationPitch = savedPitch;
                    currentZoomDist = savedDist;
                    cameraTargetX = savedTX;
                    cameraTargetY = savedTY;
                    cameraTargetZ = savedTZ;

                    if (sceneWidget != null) {
                        sceneWidget.setCameraYawAndPitch(savedPitch, savedYaw);
                        cameraMovedBySystem = true;
                        updateCameraTarget(savedDist);
                    }
                }
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
            captionCurrent = next;
            captionAlpha = 0f;
        }
    }

    private void updateMachineState(PhantasiaScript.Step step) {
        if (pattern == null || pattern.controller == null) return;
        boolean working = step != null && step.working() && playbackTick < script.getTotalTicks();
        if (pattern.controller instanceof WorkableMultiblockMachine w) {
            RecipeLogic logic = w.getRecipeLogic();
            if ((logic.getStatus() == RecipeLogic.Status.WORKING) != working)
                logic.setStatus(working ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
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
            if (e.getValue().getBlockState().getBlock() instanceof com.gregtechceu.gtceu.common.block.CoilBlock) {
                e.setValue(newCoil);
                if (SHARED_LEVEL != null)
                    SHARED_LEVEL.setBlock(e.getKey().offset(pattern.origin), newCoil.getBlockState(), 3);
            }
        }
        if (sceneWidget != null) {
            currentlyBakedPositions.clear();
            sceneWidget.getRenderer().needCompileCache();
            applyVisibility();
            if (!playerHasMovedCamera) updateCameraTarget(currentZoomDist);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        activeButtons.clear();

        g.fill(0, 0, this.width, this.height, C_BG);
        if (sceneWidget != null) sceneWidget.drawInBackground(g, mx, my, partial);

        renderCaption(g);
        if (buildOrderMode && pattern != null) renderBuildPulseBanner(g);
        if (showMistakes && script != null && script.hasCommonMistakes()) renderMistakesOverlay(g);

        renderTimeline(g, mx, my);
        renderSidePanel(g, mx, my);

        regBtn(g, mx, my, 10, 10, 50, 18, "Back", this::onClose);

        super.render(g, mx, my, partial);

        int pw = getCurrentPanelWidth();
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

    private void renderTimeline(GuiGraphics g, int mx, int my) {
        int px = this.width - getCurrentPanelWidth();
        int barY = this.height - TIMELINE_H;

        g.fill(0, barY, px, this.height, C_TL_BG);
        g.fill(0, barY, px, barY + 1, C_ACCENT);

        int x = 6;
        regBtn(g, mx, my, x, barY + 4, 18, 17, playing ? "⏸" : "▶", () -> playing = !playing);
        x += 22;
        regBtn(g, mx, my, x, barY + 4, 18, 17, isCameraLocked ? "🔒" : "🔓",
                () -> isCameraLocked = !isCameraLocked);
        x += 22;
        String spd = playbackSpeed == 0.5f ? "½x" : playbackSpeed == 2f ? "2x" : "1x";
        regBtn(g, mx, my, x, barY + 4, 24, 17, spd,
                () -> playbackSpeed = playbackSpeed == 1f ? 2f : playbackSpeed == 2f ? 0.5f : 1f);

        int tx = 80, tw = px - tx - 65, midY = barY + TIMELINE_H / 2;
        g.fill(tx, midY - 1, tx + tw, midY + 1, 0xFF1A2C3C);

        float total = script.getTotalTicks();
        for (PhantasiaScript.Step s : script.getSteps()) {
            int mx2 = tx + (int) (tw * s.tickOffset() / total);
            g.fill(mx2 - 1, midY - 4, mx2 + 1, midY + 4, 0xAAFFFFFF);
        }
        float prog = total > 0 ? playbackTick / total : 0f;
        g.fill(tx, midY - 1, tx + (int) (tw * prog), midY + 1, C_PROG);
        g.fill(tx + (int) (tw * prog) - 2, midY - 4, tx + (int) (tw * prog) + 2, midY + 4, C_ACCENT);
        g.drawString(font, formatTicks(playbackTick), tx + tw + 8, barY + 9, C_DIM, false);
    }

    /**
     * FIX (F6): Solid themed strip directly above the timeline.
     * Outgoing caption fades out while incoming fades in.
     */
    private void renderCaption(GuiGraphics g) {
        if (captionCurrent == null && captionOutgoing == null) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        int sw = this.width - getCurrentPanelWidth();
        int stripY = this.height - TIMELINE_H - CAPTION_STRIP_H;
        g.fill(0, stripY, sw, stripY + CAPTION_STRIP_H, 0xDD08080F);
        g.fill(0, stripY, sw, stripY + 1, 0xFF4FC3F7);
        int ty = stripY + (CAPTION_STRIP_H - 8) / 2;

        if (captionOutgoing != null && captionOutAlpha > 0.05f) {
            int col = ((int) (captionOutAlpha * 160) << 24) | 0xBBBBBB;
            g.drawCenteredString(font, trunc(captionOutgoing, sw - 20), sw / 2, ty, col);
        }
        if (captionCurrent != null && captionAlpha > 0.05f) {
            int col = ((int) (captionAlpha * 255) << 24) | 0xDDDDDD;
            g.drawCenteredString(font, trunc(captionCurrent, sw - 20), sw / 2, ty, col);
        }
        g.pose().popPose();
    }

    private void renderBuildPulseBanner(GuiGraphics g) {
        if (buildOrderGroup >= pattern.buildOrder.size()) return;
        int sceneW = this.width - getCurrentPanelWidth();
        int alpha = (int) (buildPulse * 0xBB);
        int col = (alpha << 24) | (C_HILIGHT & 0x00FFFFFF);
        int by = TIMELINE_H;
        g.fill(0, by, sceneW, by + 18, ((alpha / 3) << 24) | 0x1A1400);
        g.fill(0, by + 17, sceneW, by + 18, col);
        List<BlockPos> next = pattern.buildOrder.get(buildOrderGroup);
        g.drawCenteredString(font, "Next: Layer Y=" + next.get(0).getY() + " — " + next.size() + " block(s)",
                sceneW / 2, by + 5, col);
    }

    private void renderMistakesOverlay(GuiGraphics g) {
        List<PhantasiaScript.LocalWarning> local = script.getCommonMistakes();
        List<String> global = script.getGlobalMistakes();
        int x = 8, y = TIMELINE_H + 26;
        int ph = (local.size() + global.size()) * 12 + 10;
        g.fill(x - 2, y - 2, x + 240, y + ph, 0xCC06060E);
        g.fill(x - 2, y - 2, x + 240, y - 1, 0xFFFF5252);
        for (var w : local) {
            g.drawString(font, "⚠ " + w.label(), x, y, w.color(), false);
            BlockPos lp = w.localPos();
            g.drawString(font, " [" + lp.getX() + "," + lp.getY() + "," + lp.getZ() + "]",
                    x + font.width("⚠ " + w.label()), y, C_DIM, false);
            y += 12;
        }
        for (String m : global) {
            g.drawString(font, "• " + m, x, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }

    private void renderSidePanel(GuiGraphics g, int mx, int my) {
        int pw = getCurrentPanelWidth();
        int px = this.width - pw;
        activeButtons.removeIf(b -> b.x() >= px);

        g.fill(px, 0, this.width, this.height, C_PANEL);
        g.fill(px, 0, px + 1, this.height, C_ACCENT);

        int y = 10;
        g.drawString(font, trunc(definition.getLangValue(), pw - 20), px + 10, y, C_ACCENT, false);
        y += 20;
        if (sidePanelCollapsed) return;

        // Coil selector
        boolean hasCoils = pattern != null && pattern.blockMap.values().stream()
                .anyMatch(i -> i.getBlockState().getBlock() instanceof com.gregtechceu.gtceu.common.block.CoilBlock);
        if (hasCoils) {
            String cn = COIL_TIERS.get(coilIndex).getBlockState().getBlock().getName().getString();
            regBtn(g, mx, my, px + 10, y, pw - 20, 16, "Coil: " + cn,
                    () -> {
                        coilIndex = (coilIndex + 1) % COIL_TIERS.size();
                        updateCoilType();
                    });
            y += 20;
        }

        // Shape selector
        if (availableShapes != null && availableShapes.size() > 1) {
            regBtn(g, mx, my, px + 10, y, pw - 20, 16, "Structure Size: " + (shapeIndex + 1), () -> {
                shapeIndex = (shapeIndex + 1) % availableShapes.size();
                float savedYaw = rotationYaw;
                float savedPitch = rotationPitch;
                float savedDist = currentZoomDist;
                boolean hadMoved = playerHasMovedCamera;
                pattern = null;
                init();
                if (hadMoved) {
                    rotationYaw = savedYaw;
                    rotationPitch = savedPitch;
                    currentZoomDist = savedDist;
                    if (sceneWidget != null) sceneWidget.setCameraYawAndPitch(savedPitch, savedYaw);
                    updateCameraTarget(savedDist);
                }
            });
            y += 20;
        }
        y += 5;

        // Layer / build-order nav
        int bW = 20, lW = pw - 60, lX = px + 30;
        if (!buildOrderMode) {
            g.drawString(font, "Manual Layer:", px + 10, y, C_DIM, false);
            y += 12;
            regBtn(g, mx, my, px + 10, y, bW, 16, "<", () -> nudgeLayer(-1));
            regBtn(g, mx, my, lX, y, lW, 16, manualLayer < 0 ? "All" : "Layer " + manualLayer,
                    () -> {
                        manualLayer = -1;
                        applyVisibility();
                    });
            regBtn(g, mx, my, px + pw - 10 - bW, y, bW, 16, ">", () -> nudgeLayer(1));
            y += 25;
        } else {
            g.drawString(font, "Build Step:", px + 10, y, C_DIM, false);
            y += 12;
            regBtn(g, mx, my, px + 10, y, bW, 16, "<", () -> buildOrderStep(-1));
            regBtn(g, mx, my, lX, y, lW, 16, "Group " + (buildOrderGroup + 1), () -> {});
            regBtn(g, mx, my, px + pw - 10 - bW, y, bW, 16, ">", () -> buildOrderStep(1));
            y += 25;
        }

        // Filter grid — FIX (B6): every button registered independently, no overlap possible
        g.drawString(font, "Show:", px + 10, y, C_DIM, false);
        y += 12;
        ViewFilter[] vfs = ViewFilter.values();
        int fw = (pw - 25) / 2;
        for (int i = 0; i < vfs.length; i++) {
            final ViewFilter vf = vfs[i];
            int bx = (i % 2 == 0) ? px + 10 : px + 15 + fw;
            regBtn(g, mx, my, bx, y, fw, 14, vf.name(), viewFilter == vf, () -> toggleViewFilter(vf));
            if (i % 2 != 0 || i == vfs.length - 1) y += 17;
        }

        y += 8;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🧱", "Build Mode", buildOrderMode,
                () -> {
                    buildOrderMode = !buildOrderMode;
                    applyVisibility();
                    if (sceneWidget != null) {
                        sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);
                        // FIX: Prevent snap when clicking back into the scene after toggling build mode
                        cameraMovedBySystem = true;
                    }
                });
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🗺", "Footprint", false, this::openFootprintScreen);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "⊕", "Center Camera", false, this::centerCamera);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🔍", "Block List", false, this::openBlockFilterScreen);
        y += 20;

        // Edit Script — only shown in creative mode so casual players don't accidentally change scripts
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getAbilities().instabuild) {
            regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "✏", "Edit Script", false, this::openScriptEditor);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // 1. Check UI buttons first
        for (PhantasiaUIUtils.ButtonAction b : activeButtons) {
            if (b.hit(mx, my)) {
                b.action().run();
                return true;
            }
        }

        int px = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;

        // 2. Timeline scrubbing (Left Click only)
        if (btn == 0 && my >= tlY && mx < px && !buildOrderMode) {
            int tx = 80, tw = px - tx - 65;
            if (mx >= tx && mx <= tx + tw) {
                playing = false;
                scrubbing = true;
                scrubTo((float) (mx - tx) / tw);
                return true;
            }
        }

        // 3. Scene Interactions (only if clicking inside the scene area)
        if (mx < px && my < tlY) {
            // RIGHT CLICK: Open Block Inspect Screen
            if (btn == 1 && hoveredPos != null && SHARED_LEVEL != null) {
                try {
                    if (!SHARED_LEVEL.getBlockState(hoveredPos).isAir()) {
                        Minecraft.getInstance().setScreen(
                                new PhantasiaBlockInspectScreen(hoveredPos, pattern, this));
                        return true;
                    }
                } catch (Exception ignored) {}
            }

            // MIDDLE CLICK: Start Panning
            if (btn == 2 && !isCameraLocked) {
                isPanning = true;
                return true;
            }

            // LEFT CLICK: Let the SceneWidget handle standard dragging/selection
            if (sceneWidget != null) {
                return sceneWidget.mouseClicked(mx, my, btn);
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int px = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;
        if (mx >= px || my >= tlY || isCameraLocked) return super.mouseDragged(mx, my, btn, dx, dy);

        if (sceneWidget != null) {
            WorldSceneRenderer r = sceneWidget.getRenderer();
            Vector3f fwd = new Vector3f(r.getLookAt()).sub(r.getEyePos()).normalize();
            Vector3f rgt = new Vector3f(fwd).cross(r.getWorldUp()).normalize();
            Vector3f up = new Vector3f(rgt).cross(fwd).normalize();

            if (btn == 2) {
                if (cameraMovedBySystem) {
                    syncFieldsFromRenderer();
                    cameraMovedBySystem = false;
                }
                float ps = CAM_PAN_SPEED;
                cameraTargetX += (rgt.x * -dx + up.x * dy) * ps;
                cameraTargetY += (rgt.y * -dx + up.y * dy) * ps;
                cameraTargetZ += (rgt.z * -dx + up.z * dy) * ps;
                playerHasMovedCamera = true;
                updateCameraTarget(currentZoomDist);
                return true;
            }
            if (btn == 0 || btn == 1) {
                // Sync our fields from the renderer's current state before applying
                // the first drag delta after a system-driven camera move (script step,
                // init). Without this the camera jumps to our stale field values.
                if (cameraMovedBySystem) {
                    syncFieldsFromRenderer();
                    cameraMovedBySystem = false;
                }

                float s = CAM_ORBIT_SENSITIVITY;
                rotationYaw = (rotationYaw + (float) dx * s) % 360f;
                rotationPitch = Mth.clamp(rotationPitch + (float) dy * s, -85f, -5f);
                playerHasMovedCamera = true;
                sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);
                updateCameraTarget(currentZoomDist);
                return true;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.width - getCurrentPanelWidth() <= mx || isCameraLocked) return false;
        if (sceneWidget != null) {
            if (cameraMovedBySystem) {
                syncFieldsFromRenderer();
                cameraMovedBySystem = false;
            }
            currentZoomDist = Mth.clamp(currentZoomDist * (delta > 0 ? CAM_ZOOM_IN_FACTOR : CAM_ZOOM_OUT_FACTOR),
                    CAM_ZOOM_MIN, CAM_ZOOM_MAX);
            playerHasMovedCamera = true;
            updateCameraTarget(currentZoomDist);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 2 || btn == 0) isPanning = false;
        if (scrubbing) {
            int px = this.width - getCurrentPanelWidth();
            scrubTo(Mth.clamp((float) (mx - 80) / (px - 80 - 65), 0f, 1f));
            scrubbing = false;
            applyVisibility();
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (sceneWidget == null) return;
        sceneWidget.mouseMoved(mx, my);
        if (mx < this.width - getCurrentPanelWidth()) {
            BlockHitResult hit = sceneWidget.getRenderer().getLastTraceResult();
            hoveredPos = (hit != null && hit.getType() == HitResult.Type.BLOCK) ? hit.getBlockPos() : null;
        } else {
            hoveredPos = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FIX (B5 + B6): atomically pause the script when a filter is activated and
     * restore playback when returning to ALL. Always calls applyVisibility() immediately.
     */
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

    /**
     * Resets the camera to the script's declared opening position (or the facing-aware
     * isometric default). Clears playerHasMovedCamera so that subsequent system-driven
     * updates (coil/shape swaps) can apply the script camera cleanly again.
     */
    private void centerCamera() {
        if (sceneWidget == null || pattern == null || isCameraLocked) return;

        playerHasMovedCamera = false; // Resetting this allows init() to use defaults again

        // Re-calculate the "Specific Place"
        float midY = pattern.origin.getY() + (pattern.minY + pattern.maxY) * 0.5f + CAM_TARGET_Y_BIAS;
        BlockPos cp = pattern.controllerWorldPos != null ? pattern.controllerWorldPos : pattern.origin;

        cameraTargetX = cp.getX() + 0.5f;
        cameraTargetY = midY;
        cameraTargetZ = cp.getZ() + 0.5f;

        resolveStartingCamera();
        sceneWidget.setCameraYawAndPitch(rotationPitch, rotationYaw);

        cameraMovedBySystem = true; // CRITICAL: Syncs the mouse anchor to this new target
        updateCameraTarget(currentZoomDist);
    }

    private void nudgeLayer(int delta) {
        if (pattern == null) return;
        manualLayer = manualLayer < 0 ? (delta < 0 ? pattern.maxY : pattern.minY) :
                Mth.clamp(manualLayer + delta, pattern.minY, pattern.maxY);
        applyVisibility();
    }

    private void buildOrderStep(int delta) {
        if (pattern == null) return;
        buildOrderGroup = Mth.clamp(buildOrderGroup + delta, 0, pattern.buildOrder.size() - 1);
        applyVisibility();
    }

    private void scrubTo(float t) {
        scrubbing = true;
        playing = false;
        playbackTick = (int) (Mth.clamp(t, 0f, 1f) * script.getTotalTicks());
        PhantasiaScript.Step step = script.getActiveStep(playbackTick);
        if (step != lastAppliedStep) {
            lastAppliedStep = step;
            updateCaptionForStep(step);
            applyVisibility();
        }
    }

    // Add this to PhantasiaSceneScreen.java
    public PhantasiaLoadedPattern getLoadedPattern() {
        return this.pattern;
    }

    private void openScriptEditor() {
        String machineId = definition.getId().toString();
        PhantasiaScriptData current = script.getSourceData();
        if (current == null) current = PhantasiaScriptData.defaultFor(machineId);
        Minecraft.getInstance().setScreen(
                new PhantasiaScriptEditorScreen(this, machineId, current));
    }

    private void openFootprintScreen() {
        if (pattern != null)
            Minecraft.getInstance().setScreen(new PhantasiaFootprintScreen(pattern, this, script));
    }

    private void openBlockFilterScreen() {
        if (pattern != null)
            Minecraft.getInstance().setScreen(new PhantasiaBlockFilterScreen(pattern, script, viewFilter, this));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void regBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov, C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private void regBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h,
                        String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov, active ? C_BTN_ACT : C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private void regIconBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h,
                            String icon, String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawIconBtn(g, font, x, y, w, h, icon, label, hov, active ? C_BTN_ACT : C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int getCurrentPanelWidth() {
        return sidePanelCollapsed ? COLLAPSED_PANEL_W : FULL_PANEL_W;
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) s = s.substring(0, s.length() - 2) + "\u2026";
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
        invalidateSharedLevel();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
