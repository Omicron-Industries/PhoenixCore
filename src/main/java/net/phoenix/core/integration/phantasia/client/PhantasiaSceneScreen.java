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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import net.phoenix.core.integration.phantasia.*;
import net.phoenix.core.integration.phantasia.utils.PhantasiaThemeUtils;
import net.phoenix.core.integration.phantasia.utils.PhantasiaUIUtils;
import net.phoenix.core.mixin.gtceu.AccessorWorldSceneRenderer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

import static net.phoenix.core.integration.phantasia.utils.PhantasiaThemeUtils.*;

@OnlyIn(Dist.CLIENT)
public class PhantasiaSceneScreen extends Screen {

    public static TrackedDummyWorld SHARED_LEVEL;
    private static int NEXT_REGION = 0;
    private static final int REGION_SIZE = 512;

    private PanelLayout cachedLayout = null;
    private String cachedTimerString = "";
    private int lastCachedTick = -1;
    boolean hasCoils = false;

    public static void invalidateSharedLevel() { SHARED_LEVEL = null; NEXT_REGION = 0; }

    private static final int FULL_PANEL_W      = 168;
    private static final int COLLAPSED_PANEL_W = 18;
    private static final int TIMELINE_H        = 26;
    // FIX (F6): solid caption strip replaces floating text
    private static final int CAPTION_STRIP_H   = 22;

    private final Screen parent;
    private final MultiblockMachineDefinition definition;
    private final PhantasiaScript script;

    private PhantasiaLoadedPattern pattern;
    private SceneWidget sceneWidget;
    private int lastSceneW = -1, lastSceneH = -1;

    private boolean isPanning = false;
    private float cameraTargetX = 0f;
    private float cameraTargetY = 0f;
    private float cameraTargetZ = 0f;

    // FIX (B2): single source-of-truth for zoom distance
    private float currentZoomDist = 15.0f;

    private boolean visibilityDirty = true;
    private int     lastVisibleHash = Integer.MIN_VALUE;

    private boolean playing       = true;
    private int     playbackTick  = 0;
    private float   tickAccum     = 0f;
    private float   playbackSpeed = 1.0f;
    private boolean scrubbing     = false;
    private PhantasiaScript.Step lastAppliedStep = null;

    private int manualLayer = -1;

    private final java.util.List<PhantasiaUIUtils.ButtonAction> activeButtons = new java.util.ArrayList<>();

    private record ButtonRegion(int x, int y, int w, int h, Runnable action) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private void regBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov, PhantasiaThemeUtils.C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private void regBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        int color = active ? PhantasiaThemeUtils.C_BTN_ACT : PhantasiaThemeUtils.C_BTN;
        PhantasiaThemeUtils.drawThemedBtn(g, font, x, y, w, h, label, hov, color);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    private record ButtonAction(int x, int y, int w, int h, Runnable action) {
        public boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private boolean buildOrderMode  = false;
    private int     buildOrderGroup = 0;
    private float   buildPulse      = 0f;
    private boolean buildPulseUp    = true;

    public boolean showMistakes = false;

    public int selectedTierIndex = -1;

    private BlockPos hoveredPos = null;

    private float  captionAlpha    = 0f;
    private String captionCurrent  = null;
    private String captionOutgoing = null;
    private float  captionOutAlpha = 0f;

    private ViewFilter viewFilter = ViewFilter.ALL;

    // FIX (B5): pause script when a filter is active
    private boolean wasPlayingBeforeFilter = false;

    private boolean sidePanelCollapsed = false;

    private final Set<BlockPos> currentlyVisiblePositions = new HashSet<>();
    private Set<BlockPos> lastUploadedPositions = null;
    private Set<BlockPos> fullMachinePos = null;

    public PhantasiaSceneScreen(MultiblockMachineDefinition definition, Screen parent) {
        super(Component.literal(definition.getLangValue()));
        this.parent     = parent;
        this.definition = definition;
        this.script     = PhantasiaScripts.get(definition);
    }

    private final Set<BlockPos> currentlyBakedPositions = new HashSet<>();

    // Starting angles – will be overridden in init() by machine facing direction (FIX B4)
    private float rotationYaw   = -135.0f;
    private float rotationPitch = -35.0f;

    @Override
    protected void init() {
        super.init();
        if (SHARED_LEVEL == null) {
            if (Minecraft.getInstance().level == null) { onClose(); return; }
            SHARED_LEVEL = new TrackedDummyWorld();
        }

        this.availableShapes = definition.getMatchingShapes();

        if (pattern == null && !availableShapes.isEmpty()) {
            if (shapeIndex >= availableShapes.size()) shapeIndex = 0;
            pattern = loadPattern(availableShapes.get(shapeIndex));
        }

        int sw = this.width - getCurrentPanelWidth();
        int sh = this.height - TIMELINE_H - CAPTION_STRIP_H;

        if (sceneWidget == null) {
            sceneWidget = new SceneWidget(0, 0, sw, sh, SHARED_LEVEL);
            sceneWidget.setDraggable(true);
            WorldSceneRenderer renderer = sceneWidget.getRenderer();
            renderer.useCacheBuffer(true);
            if (renderer instanceof AccessorWorldSceneRenderer accessor) {
                accessor.setEndBatchLast(true);
            }
        } else {
            sceneWidget.setSize(sw, sh);
        }

        if (pattern != null) {
            BlockPos cp = pattern.controllerWorldPos != null ? pattern.controllerWorldPos : pattern.origin;

            // Capture zoom to prevent snapping during size/coil swaps (FIX B2)
            float dist = currentZoomDist;

            this.cameraTargetX = cp.getX() + 0.5f;
            this.cameraTargetY = cp.getY() + 0.5f;
            this.cameraTargetZ = cp.getZ() + 0.5f;

            if (isCameraLocked) {
                updateCameraTarget(dist);
            } else {
                // FIX (B4): derive yaw from the controller's facing direction
                float facingYaw = getFacingYaw();
                this.rotationYaw   = facingYaw;
                this.rotationPitch = -35.0f;
                currentZoomDist    = 15.0f;
                if (sceneWidget != null) sceneWidget.setCameraYawAndPitch(this.rotationPitch, this.rotationYaw);
                updateCameraTarget(currentZoomDist);
            }

            this.currentlyBakedPositions.clear();
            applyVisibility();
        }
    }

    /**
     * FIX (B4): Reads the controller's HORIZONTAL_FACING property and converts it to a camera yaw
     * that positions the camera in front of the machine's face.
     */
    private float getFacingYaw() {
        if (pattern == null || pattern.controllerWorldPos == null || SHARED_LEVEL == null) return -135.0f;
        try {
            BlockState ctrl = SHARED_LEVEL.getBlockState(pattern.controllerWorldPos);
            if (ctrl.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Direction front = ctrl.getValue(BlockStateProperties.HORIZONTAL_FACING);
                // Camera approaches from the same side as the front face so we look inward
                return switch (front) {
                    case NORTH -> 0.0f;
                    case SOUTH -> 180.0f;
                    case WEST  -> 90.0f;
                    case EAST  -> 270.0f;
                    default    -> -135.0f;
                };
            }
        } catch (Exception ignored) {}
        return -135.0f;
    }

    private int coilIndex = 0;
    private static final List<BlockInfo> COIL_TIERS = List.of(
            new BlockInfo(GTBlocks.COIL_CUPRONICKEL.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_KANTHAL.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_NICHROME.get().defaultBlockState()),
            new BlockInfo(GTBlocks.COIL_RTMALLOY.get().defaultBlockState())
    );

    private void updateCoilType() {
        if (pattern == null || pattern.blockMap == null) return;
        BlockInfo newCoil = COIL_TIERS.get(coilIndex);
        for (Map.Entry<BlockPos, BlockInfo> entry : pattern.blockMap.entrySet()) {
            if (entry.getValue().getBlockState().getBlock() instanceof com.gregtechceu.gtceu.common.block.CoilBlock) {
                entry.setValue(newCoil);
                if (SHARED_LEVEL != null) {
                    BlockPos worldPos = entry.getKey().offset(pattern.origin);
                    SHARED_LEVEL.setBlock(worldPos, newCoil.getBlockState(), 3);
                }
            }
        }
        if (sceneWidget != null) {
            WorldSceneRenderer renderer = sceneWidget.getRenderer();
            this.currentlyBakedPositions.clear();
            renderer.needCompileCache();
            applyVisibility();
            if (!isCameraLocked) {
                updateCameraTarget(currentZoomDist);
            }
        }
    }

    private void updateCameraTarget() {
        updateCameraTarget(currentZoomDist);
    }

    private void updateCameraTarget(float dist) {
        if (sceneWidget == null) return;
        // FIX (B2): also keep currentZoomDist in sync
        this.currentZoomDist = dist;
        WorldSceneRenderer renderer = sceneWidget.getRenderer();

        double yawRad   = Math.toRadians(this.rotationYaw);
        double pitchRad = Math.toRadians(this.rotationPitch);

        float nx = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        float ny = (float)  Math.sin(pitchRad);
        float nz = (float) (Math.cos(pitchRad) * Math.cos(yawRad));

        renderer.setCameraLookAt(
                new Vector3f(cameraTargetX + nx * dist, cameraTargetY + ny * dist, cameraTargetZ + nz * dist),
                new Vector3f(cameraTargetX, cameraTargetY, cameraTargetZ),
                new Vector3f(0, 1, 0)
        );
    }

    private PhantasiaLoadedPattern loadPattern(MultiblockShapeInfo shape) {
        int regionIndex = NEXT_REGION++;
        BlockPos origin = new BlockPos(regionIndex * REGION_SIZE, 50, 0);

        Map<BlockPos, BlockInfo> blockMap     = new HashMap<>();
        Map<BlockPos, BlockPos>  localToWorld = new HashMap<>();
        Set<BlockPos>            baseplatePos = new HashSet<>();
        Set<BlockPos>            bePos        = new HashSet<>();
        BlockPos                 controllerWP = null;
        MultiblockControllerMachine controller = null;

        BlockInfo floorInfo = BlockInfo.fromBlockState(Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        BlockInfo[][][] raw = shape.getBlocks();
        int sxLen = raw.length;
        int szLen = (sxLen > 0 && raw[0].length > 0) ? raw[0][0].length : 0;
        int padX = Math.max(2, sxLen / 2 + 1), padZ = Math.max(2, szLen / 2 + 1);
        for (int bx = -padX; bx <= sxLen + padX; bx++)
            for (int bz = -padZ; bz <= szLen + padZ; bz++) {
                BlockPos wp = origin.offset(bx, -1, bz);
                blockMap.put(wp, floorInfo);
                baseplatePos.add(wp);
            }

        for (int x = 0; x < raw.length; x++) {
            for (int y = 0; y < raw[x].length; y++) {
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
                                controller = ctrl; controllerWP = wp;
                            }
                            bePos.add(wp);
                        }
                    } catch (Exception ignored) {}
                    blockMap.put(wp, info);
                    localToWorld.put(lp, wp);
                }
            }
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
            minY = Math.min(minY, lp.getY()); maxY = Math.max(maxY, lp.getY());
        }
        if (minY > maxY) { minY = 0; maxY = 0; }

        return new PhantasiaLoadedPattern(blockMap, localToWorld, baseplatePos,
                controllerWP, bePos, origin, minY, maxY, controller, this.script);
    }

    private void applyVisibility() {
        if (sceneWidget == null || pattern == null) return;

        PhantasiaScript.Step step = script.getActiveStep(playbackTick);
        Set<BlockPos> nextVisible = new HashSet<>();

        nextVisible.addAll(pattern.baseplatePositions);
        for (Map.Entry<BlockPos, BlockPos> entry : pattern.localToWorld.entrySet()) {
            if (isBlockVisibleInternal(entry.getKey(), entry.getValue(), step)) {
                nextVisible.add(entry.getValue());
            }
        }

        if (!nextVisible.equals(currentlyBakedPositions)) {
            currentlyBakedPositions.clear();
            currentlyBakedPositions.addAll(nextVisible);
            sceneWidget.setRenderedCore(currentlyBakedPositions, null);
            sceneWidget.getRenderer().needCompileCache();
        }
    }

    /**
     * FIX (B5): When a view filter is active it takes FULL control — the script filter is skipped.
     * This means clicking a filter button always shows exactly what the filter describes,
     * regardless of what the current script step wants to show.
     */
    private boolean isBlockVisibleInternal(BlockPos localPos, BlockPos worldPos, PhantasiaScript.Step step) {
        // 1. View Filters — when active, these have total authority; skip script entirely
        if (viewFilter != ViewFilter.ALL) {
            Set<BlockPos> filterSet = getFilterSet(viewFilter);
            // null filterSet means "show all" for that filter mode (shouldn't happen, but safe)
            return filterSet == null || filterSet.contains(worldPos);
        }

        // 2. Manual Layer Mode
        if (manualLayer >= 0) return localPos.getY() == manualLayer;

        // 3. Build Order Mode
        if (buildOrderMode) {
            int group = pattern.getGroupIndex(localPos);
            return group != -1 && group <= buildOrderGroup;
        }

        // 4. Script Filter (only when no override is active)
        if (step != null) return step.filter().test(localPos);

        // 5. Default: show everything
        return true;
    }

    private Set<BlockPos> lastBakedPositions = new HashSet<>();

    public static BlockPos getOriginForCurrentPattern() {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof PhantasiaSceneScreen pss && pss.pattern != null) return pss.pattern.origin;
        if (mc.screen instanceof PhantasiaFootprintScreen pfs && pfs.getPattern() != null) return pfs.getPattern().origin;
        return BlockPos.ZERO;
    }

    private int computeHash(List<BlockPos> list) {
        int h = 0;
        for (BlockPos p : list) h += p.hashCode();
        return h ^ (list.size() * 0x9e3779b9);
    }

    enum ViewFilter { ALL, HATCHES_BUSES, ENERGY_IO, BLOCK_ENTITIES, CONTROLLER }

    private Set<BlockPos> filteredHatchBus  = null;
    private Set<BlockPos> filteredEnergyIO  = null;
    private Set<BlockPos> filteredHasBE     = null;
    private Set<BlockPos> filteredController = null;

    private void buildFilterSets() {
        if (pattern == null || SHARED_LEVEL == null) return;
        filteredHatchBus   = new HashSet<>();
        filteredEnergyIO   = new HashSet<>();
        filteredHasBE      = pattern.blockEntityWorldPos;
        // FIX: wire up CONTROLLER filter set
        filteredController = pattern.controllerWorldPos != null
                ? Set.of(pattern.controllerWorldPos)
                : Set.of();

        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            BlockPos wp = e.getValue();
            if (wp.equals(pattern.controllerWorldPos)) continue;
            BlockState state = SHARED_LEVEL.getBlockState(wp);
            if (!(state.getBlock() instanceof MetaMachineBlock)) continue;

            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (rl == null) continue;
            String path = rl.getPath();

            if (path.contains("hatch") || path.contains("bus")
                    || path.contains("muffler") || path.contains("maintenance"))
                filteredHatchBus.add(wp);

            if (path.contains("energy") || path.contains("dynamo")
                    || path.contains("laser") || path.contains("power"))
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

    private final Map<BlockPos, Float> blockAlphas = new HashMap<>();
    private static final float FADE_SPEED = 0.15f;

    @Override
    public void tick() {
        super.tick();

        // 1. Caption fades
        if (captionCurrent != null  && captionAlpha    < 1.0f) captionAlpha    += 0.1f;
        if (captionOutgoing != null) {
            captionOutAlpha -= 0.1f;
            if (captionOutAlpha <= 0f) captionOutgoing = null;
        }

        // 2. Build-order pulse
        if (buildOrderMode) {
            buildPulse += buildPulseUp ? 0.05f : -0.05f;
            if (buildPulse >= 1f) { buildPulse = 1f; buildPulseUp = false; }
            if (buildPulse <= 0f) { buildPulse = 0f; buildPulseUp = true;  }
        }

        // 3. Playback — skip if a filter is overriding the script (FIX B5)
        if (playing && !scrubbing && !buildOrderMode && script != null && viewFilter == ViewFilter.ALL) {
            int oldTick = playbackTick;
            tickAccum += playbackSpeed;
            while (tickAccum >= 1f) { tickAccum -= 1f; playbackTick++; }

            if (playbackTick >= script.getTotalTicks()) {
                playbackTick = (int) script.getTotalTicks();
                playing = false;
            }

            PhantasiaScript.Step currentStep = script.getActiveStep(playbackTick);

            if (currentStep != null) {
                if (currentStep.useCam() && isCameraLocked) {
                    this.rotationYaw   = currentStep.yaw();
                    this.rotationPitch = currentStep.pitch();
                    if (sceneWidget != null) {
                        sceneWidget.setCameraYawAndPitch(this.rotationPitch, this.rotationYaw);
                        updateCameraTarget(currentZoomDist);
                    }
                }

                if (currentStep.forceCoil() != -1 && currentStep.forceCoil() != this.coilIndex) {
                    this.coilIndex = currentStep.forceCoil();
                    updateCoilType();
                    if (!isCameraLocked) updateCameraTarget(currentZoomDist);
                }
            }

            if (playbackTick != oldTick) {
                if (currentStep != lastAppliedStep) {
                    lastAppliedStep = currentStep;

                    if (currentStep != null && currentStep.forceShape() != -1
                            && currentStep.forceShape() != this.shapeIndex) {
                        if (availableShapes != null && currentStep.forceShape() < availableShapes.size()) {
                            this.shapeIndex = currentStep.forceShape();
                            this.pattern = null;

                            float yaw   = this.rotationYaw;
                            float pitch = this.rotationPitch;
                            float d     = currentZoomDist;
                            this.init();

                            if (!isCameraLocked) {
                                this.rotationYaw   = yaw;
                                this.rotationPitch = pitch;
                                currentZoomDist    = d;
                                if (sceneWidget != null) sceneWidget.setCameraYawAndPitch(pitch, yaw);
                                updateCameraTarget(d);
                            }
                        }
                    }

                    applyVisibility();
                    updateMachineState(currentStep);
                    updateCaptionForStep(currentStep);
                }
            }
        }
    }

    private void updateCaptionForStep(PhantasiaScript.Step step) {
        String nextCaption = (step != null) ? step.caption() : null;
        if (!Objects.equals(nextCaption, captionCurrent)) {
            captionOutgoing = captionCurrent;
            captionOutAlpha = captionAlpha;
            captionCurrent  = nextCaption;
            captionAlpha    = 0f;
        }
    }

    private void updateMachineState(PhantasiaScript.Step step) {
        if (pattern == null || pattern.controller == null) return;
        var controller = pattern.controller;
        boolean isWorking = (step != null) && step.working() && (playbackTick < script.getTotalTicks());

        if (controller instanceof WorkableMultiblockMachine workable) {
            var logic = workable.getRecipeLogic();
            if ((logic.getStatus() == RecipeLogic.Status.WORKING) != isWorking) {
                logic.setStatus(isWorking ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
            }
        }

        var renderState = controller.getRenderState();
        var activeProp  = com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_ACTIVE;
        if (renderState.hasProperty(activeProp)) {
            if (renderState.getValue(activeProp) != isWorking) {
                controller.setRenderState(renderState.setValue(activeProp, isWorking));
            }
        }
    }

    private int shapeIndex = 0;
    private List<MultiblockShapeInfo> availableShapes = new ArrayList<>();

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        // 1. Reset hitboxes every frame
        activeButtons.clear();

        // 2. Background and 3D scene
        g.fill(0, 0, this.width, this.height, PhantasiaThemeUtils.C_BG);
        if (sceneWidget != null) {
            sceneWidget.drawInBackground(g, mx, my, partial);
        }

        // 3. Overlays
        renderCaption(g);
        if (buildOrderMode && pattern != null) renderBuildPulseBanner(g);
        if (showMistakes && script != null && script.hasCommonMistakes()) renderMistakesOverlay(g);

        // 4. UI panels
        renderTimeline(g, mx, my);
        renderSidePanel(g, mx, my);

        // 5. Global navigation
        regBtn(g, mx, my, 10, 10, 50, 18, "Back", this::onClose);

        // 6. Base Minecraft rendering
        super.render(g, mx, my, partial);

        // 7. Block hover info
        int pw = getCurrentPanelWidth();
        int px = this.width - pw;

        if (hoveredPos != null && SHARED_LEVEL != null) {
            try {
                BlockState st = SHARED_LEVEL.getBlockState(hoveredPos);
                if (!st.isAir()) {
                    String name = st.getBlock().getName().getString();
                    g.drawString(font, PhantasiaUIUtils.truncate(font, name, pw - 20),
                            px + 10, this.height - 20, PhantasiaThemeUtils.C_DIM, false);
                    if (mx < px) g.renderTooltip(font, st.getBlock().getName(), mx, my);
                }
            } catch (Exception ignored) {}
        }
    }

    private void renderBuildPulseBanner(GuiGraphics g) {
        if (buildOrderGroup >= pattern.buildOrder.size()) return;
        int sceneW = this.width - getCurrentPanelWidth();
        int alpha  = (int)(buildPulse * 0xBB);
        int col    = (alpha << 24) | (C_HILIGHT & 0x00FFFFFF);
        int bannerY = TIMELINE_H;
        g.fill(0, bannerY, sceneW, bannerY + 18, ((alpha / 3) << 24) | 0x1A1400);
        g.fill(0, bannerY + 17, sceneW, bannerY + 18, col);
        List<BlockPos> nextGroup = pattern.buildOrder.get(buildOrderGroup);
        String hint = "Next: Layer Y=" + nextGroup.get(0).getY() + " — " + nextGroup.size() + " block(s)";
        g.drawCenteredString(font, hint, sceneW / 2, bannerY + 5, col);
    }

    private void renderMistakesOverlay(GuiGraphics g) {
        List<PhantasiaScript.LocalWarning> localList  = script.getCommonMistakes();
        List<String>                       globalList = script.getGlobalMistakes();

        int x = 8, y = TIMELINE_H + 26;
        int panelH = (localList.size() + globalList.size()) * 12 + 10;

        g.fill(x - 2, y - 2, x + 240, y + panelH, 0xCC06060E);
        g.fill(x - 2, y - 2, x + 240, y - 1, 0xFFFF5252);

        for (PhantasiaScript.LocalWarning w : localList) {
            g.drawString(font, "⚠ " + w.label(), x, y, w.color(), false);
            BlockPos lp = w.localPos();
            String posStr = " [" + lp.getX() + "," + lp.getY() + "," + lp.getZ() + "]";
            g.drawString(font, posStr, x + font.width("⚠ " + w.label()), y, C_DIM, false);
            y += 12;
        }
        for (String msg : globalList) {
            g.drawString(font, "• " + msg, x, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }

    private int lastRenderedTick = -1;
    private String cachedTimeStr = "";
    private boolean isCameraLocked = true;

    private void renderTimeline(GuiGraphics g, int mx, int my) {
        int px   = this.width - getCurrentPanelWidth();
        int barY = this.height - TIMELINE_H;

        g.fill(0, barY, px, this.height, PhantasiaThemeUtils.C_TL_BG);
        g.fill(0, barY, px, barY + 1, PhantasiaThemeUtils.C_ACCENT);

        int curX = 6;
        regBtn(g, mx, my, curX, barY + 4, 18, 17, playing ? "⏸" : "▶", () -> playing = !playing);
        curX += 22;

        regBtn(g, mx, my, curX, barY + 4, 18, 17, isCameraLocked ? "🔒" : "🔓", () -> isCameraLocked = !isCameraLocked);
        curX += 22;

        String spdLabel = playbackSpeed == 0.5f ? "½x" : playbackSpeed == 2f ? "2x" : "1x";
        regBtn(g, mx, my, curX, barY + 4, 24, 17, spdLabel, () -> {
            playbackSpeed = (playbackSpeed == 1.0f) ? 2.0f : (playbackSpeed == 2.0f) ? 0.5f : 1.0f;
        });

        int tx = 80;
        int tw = px - tx - 65;
        int midY = barY + (TIMELINE_H / 2);

        g.fill(tx, midY - 1, tx + tw, midY + 1, 0xFF1A2C3C);

        float total = script.getTotalTicks();

        for (PhantasiaScript.Step s : script.getSteps()) {
            int markerX = tx + (int)(tw * (float) s.tickOffset() / total);
            g.fill(markerX - 1, midY - 4, markerX + 1, midY + 4, 0xAAFFFFFF);
        }

        float prog = total > 0 ? (float) playbackTick / total : 0f;
        g.fill(tx, midY - 1, tx + (int)(tw * prog), midY + 1, C_PROG);
        g.fill(tx + (int)(tw * prog) - 2, midY - 4, tx + (int)(tw * prog) + 2, midY + 4, PhantasiaThemeUtils.C_ACCENT);

        g.drawString(font, formatTicks(playbackTick), tx + tw + 8, barY + 9, PhantasiaThemeUtils.C_DIM, false);
    }

    /**
     * FIX (F6): Solid themed strip directly above the timeline bar, replacing the old floating text.
     */
    private void renderCaption(GuiGraphics g) {
        if (captionCurrent == null && captionOutgoing == null) return;

        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        int sw     = this.width - getCurrentPanelWidth();
        int stripY = this.height - TIMELINE_H - CAPTION_STRIP_H;

        // Solid background strip with accent top border
        g.fill(0, stripY, sw, stripY + CAPTION_STRIP_H, 0xDD08080F);
        g.fill(0, stripY, sw, stripY + 1, 0xFF4FC3F7);

        int textY = stripY + (CAPTION_STRIP_H - 8) / 2;

        // Outgoing caption fades out
        if (captionOutgoing != null && captionOutAlpha > 0.05f) {
            int col = ((int)(captionOutAlpha * 160) << 24) | 0xBBBBBB;
            g.drawCenteredString(font, trunc(captionOutgoing, sw - 20), sw / 2, textY, col);
        }
        // Current caption fades in
        if (captionCurrent != null && captionAlpha > 0.05f) {
            int col = ((int)(captionAlpha * 255) << 24) | 0xDDDDDD;
            g.drawCenteredString(font, trunc(captionCurrent, sw - 20), sw / 2, textY, col);
        }

        g.pose().popPose();
    }

    private record PanelLayout(
            int filterRowY,
            int filterRows,
            int layerLabelY,
            int layerDownY,
            int buildOrderBtnY,
            int mistakesBtnY,
            int footprintBtnY,
            int centerBtnY,
            int collapseButtonY
    ) {}

    private PanelLayout lastLayout = null;

    private int getCurrentPanelWidth() {
        return sidePanelCollapsed ? COLLAPSED_PANEL_W : FULL_PANEL_W;
    }

    private PanelLayout computeLayout() {
        int pw = 150;
        int currentY = 40;

        int filterRowY  = currentY;
        int filterRows  = (PhantasiaSceneScreen.ViewFilter.values().length + 1) / 2;
        currentY += (filterRows * 22) + 10;

        int layerLabelY     = currentY;
        int layerDownY      = currentY + 15;
        currentY += 40;

        int buildOrderBtnY  = currentY;
        currentY += 22;

        int centerBtnY      = currentY;
        currentY += 22;

        int mistakesBtnY = script.hasCommonMistakes() ? currentY : -1;
        if (mistakesBtnY != -1) currentY += 22;

        int footprintBtnY   = currentY;
        currentY += 22;

        int collapseButtonY = this.height - 30;

        return new PanelLayout(filterRowY, filterRows, layerLabelY, layerDownY,
                buildOrderBtnY, mistakesBtnY, footprintBtnY, centerBtnY, collapseButtonY);
    }

    private void renderSidePanel(GuiGraphics g, int mx, int my) {
        int pw = getCurrentPanelWidth();
        int px = this.width - pw;

        // Clear only buttons inside the side panel area
        activeButtons.removeIf(btn -> btn.x() >= px);

        g.fill(px, 0, this.width, this.height, PhantasiaThemeUtils.C_PANEL);
        g.fill(px, 0, px + 1, this.height, PhantasiaThemeUtils.C_ACCENT);

        int y = 10;
        g.drawString(font, trunc(definition.getLangValue(), pw - 20), px + 10, y, PhantasiaThemeUtils.C_ACCENT, false);
        y += 20;

        if (sidePanelCollapsed) return;

        // Coil selection
        boolean patternHasCoils = pattern != null && pattern.blockMap.values().stream()
                .anyMatch(info -> info.getBlockState().getBlock() instanceof com.gregtechceu.gtceu.common.block.CoilBlock);
        if (patternHasCoils) {
            String cName = COIL_TIERS.get(coilIndex).getBlockState().getBlock().getName().getString();
            regBtn(g, mx, my, px + 10, y, pw - 20, 16, "Coil: " + cName, () -> {
                this.coilIndex = (this.coilIndex + 1) % COIL_TIERS.size();
                updateCoilType();
            });
            y += 20;
        }

        // Structure size
        if (availableShapes != null && availableShapes.size() > 1) {
            regBtn(g, mx, my, px + 10, y, pw - 20, 16, "Structure Size: " + (shapeIndex + 1), () -> {
                this.shapeIndex = (this.shapeIndex + 1) % availableShapes.size();
                this.pattern = null;
                float yaw   = this.rotationYaw;
                float pitch = this.rotationPitch;
                float d     = currentZoomDist;
                this.init();
                if (isCameraLocked) {
                    this.rotationYaw   = yaw;
                    this.rotationPitch = pitch;
                    updateCameraTarget(d);
                }
            });
            y += 20;
        }

        y += 5;

        // Layer navigation
        int btnW   = 20;
        int labelW = pw - 60;
        int labelX = px + 30;

        if (!buildOrderMode) {
            g.drawString(font, "Manual Layer:", px + 10, y, PhantasiaThemeUtils.C_DIM, false);
            y += 12;

            regBtn(g, mx, my, px + 10, y, btnW, 16, "<", () -> nudgeLayer(-1));
            String layerText = (manualLayer == -1) ? "All" : "Layer " + manualLayer;
            regBtn(g, mx, my, labelX, y, labelW, 16, layerText, () -> {
                manualLayer = -1;
                applyVisibility();
            });
            regBtn(g, mx, my, px + pw - 10 - btnW, y, btnW, 16, ">", () -> nudgeLayer(1));
            y += 25;
        } else {
            g.drawString(font, "Build Step:", px + 10, y, PhantasiaThemeUtils.C_DIM, false);
            y += 12;
            regBtn(g, mx, my, px + 10, y, btnW, 16, "<", () -> buildOrderStep(-1));
            String stepText = "Group " + (buildOrderGroup + 1);
            regBtn(g, mx, my, labelX, y, labelW, 16, stepText, () -> {});
            regBtn(g, mx, my, px + pw - 10 - btnW, y, btnW, 16, ">", () -> buildOrderStep(1));
            y += 25;
        }

        // Filter grid
        g.drawString(font, "Show:", px + 10, y, PhantasiaThemeUtils.C_DIM, false);
        y += 12;
        ViewFilter[] vfs = ViewFilter.values();
        int bw = (pw - 25) / 2;
        for (int i = 0; i < vfs.length; i++) {
            final ViewFilter currentFilter = vfs[i];
            int bx = (i % 2 == 0) ? px + 10 : px + 15 + bw;
            regBtn(g, mx, my, bx, y, bw, 14, currentFilter.name(), viewFilter == currentFilter, () -> {
                toggleViewFilter(currentFilter);
            });
            if (i % 2 != 0 || i == vfs.length - 1) y += 17;
        }

        // Icon buttons
        y += 8;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🧱", "Build Mode", buildOrderMode, () -> {
            buildOrderMode = !buildOrderMode; applyVisibility();
        });
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🗺", "Footprint", false, this::openFootprintScreen);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "⊕", "Center Camera", false, this::centerCamera);
        y += 20;
        regIconBtn(g, mx, my, px + 10, y, pw - 20, 16, "🔍", "Block List", false, this::openBlockFilterScreen);
    }

    private void regIconBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h,
                            String icon, String label, boolean active, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        PhantasiaThemeUtils.drawIconBtn(g, font, x, y, w, h, icon, label, hov,
                active ? PhantasiaThemeUtils.C_BTN_ACT : PhantasiaThemeUtils.C_BTN);
        activeButtons.add(new PhantasiaUIUtils.ButtonAction(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // 1. Check all registered buttons first
        for (PhantasiaUIUtils.ButtonAction button : activeButtons) {
            if (button.hit(mx, my)) {
                button.action().run();
                return true;
            }
        }

        int px  = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;

        // 2. Timeline scrubbing
        if (my >= tlY && mx < px && !buildOrderMode) {
            int tx = 80;
            int tw = px - tx - 65;
            if (mx >= tx && mx <= tx + tw) {
                this.playing  = false;
                this.scrubbing = true;
                scrubTo((float)(mx - tx) / tw);
                applyVisibility();
                return true;
            }
        }

        // 3. World interaction
        if (mx < px && my < tlY) {
            if (!this.isCameraLocked) {
                if (btn == 2) { this.isPanning = true; return true; }

                if (btn == 1 && hoveredPos != null && SHARED_LEVEL != null) {
                    try {
                        BlockState bs = SHARED_LEVEL.getBlockState(hoveredPos);
                        if (!bs.isAir()) {
                            Minecraft.getInstance().setScreen(
                                    new PhantasiaBlockInspectScreen(hoveredPos, pattern, this));
                            return true;
                        }
                    } catch (Exception ignored) {}
                }

                if (sceneWidget != null) return sceneWidget.mouseClicked(mx, my, btn);
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int px  = this.width - getCurrentPanelWidth();
        int tlY = this.height - TIMELINE_H;

        if (mx >= px || my >= tlY || this.isCameraLocked) {
            return super.mouseDragged(mx, my, btn, dx, dy);
        }

        if (sceneWidget != null) {
            WorldSceneRenderer renderer = sceneWidget.getRenderer();
            org.joml.Vector3f forward  = new org.joml.Vector3f(renderer.getLookAt()).sub(renderer.getEyePos()).normalize();
            org.joml.Vector3f right    = new org.joml.Vector3f(forward).cross(renderer.getWorldUp()).normalize();
            org.joml.Vector3f actualUp = new org.joml.Vector3f(right).cross(forward).normalize();

            // Middle-click pan
            if (btn == 2) {
                float panSpeed = 0.02f;
                this.cameraTargetX += (right.x * -dx + actualUp.x * dy) * panSpeed;
                this.cameraTargetY += (right.y * -dx + actualUp.y * dy) * panSpeed;
                this.cameraTargetZ += (right.z * -dx + actualUp.z * dy) * panSpeed;
                updateCameraTarget();
                return true;
            }

            // Left/right drag = orbit
            if (btn == 0 || btn == 1) {
                float sensitivity = 0.5f;
                this.rotationYaw   = (this.rotationYaw + (float) dx * sensitivity) % 360f;
                // FIX (B1): upper pitch clamp tightened to −5 so the eye never goes below the baseplate
                this.rotationPitch = net.minecraft.util.Mth.clamp(
                        this.rotationPitch + (float) dy * sensitivity, -85f, -5f);
                sceneWidget.setCameraYawAndPitch(this.rotationPitch, this.rotationYaw);
                // FIX (B2): use stored zoom distance, not re-read from renderer
                updateCameraTarget(currentZoomDist);
                return true;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int px = this.width - getCurrentPanelWidth();
        if (mx >= px || isCameraLocked) return false;

        if (sceneWidget != null) {
            // FIX (B2): update the stored field; never re-read from the renderer
            float zoomFactor   = delta > 0 ? 0.9f : 1.1f;
            currentZoomDist    = net.minecraft.util.Mth.clamp(currentZoomDist * zoomFactor, 2.0f, 100.0f);
            updateCameraTarget(currentZoomDist);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 2 || btn == 0) this.isPanning = false;

        if (scrubbing) {
            int px = this.width - getCurrentPanelWidth();
            int tx = 80;
            int tw = px - tx - 65;
            float progress = net.minecraft.util.Mth.clamp((float)(mx - tx) / tw, 0f, 1f);
            scrubTo(progress);
            this.scrubbing = false;
            applyVisibility();
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (sceneWidget == null) return;
        sceneWidget.mouseMoved(mx, my);

        int px = this.width - getCurrentPanelWidth();
        if (mx < px) {
            WorldSceneRenderer renderer = sceneWidget.getRenderer();
            if (renderer != null) {
                BlockHitResult hit = renderer.getLastTraceResult();
                hoveredPos = (hit != null && hit.getType() == HitResult.Type.BLOCK) ? hit.getBlockPos() : null;
            }
        } else {
            hoveredPos = null;
        }
    }

    /**
     * FIX (B5): Toggle a view filter. When a filter is activated, the script is paused so
     * the static filtered view is not fought by ongoing playback. Restoring ALL resumes playback.
     */
    private void toggleViewFilter(ViewFilter vf) {
        if (viewFilter == vf) {
            // Deactivate — return to ALL and restore playback
            viewFilter = ViewFilter.ALL;
            if (wasPlayingBeforeFilter) playing = true;
            wasPlayingBeforeFilter = false;
        } else {
            // Activate — pause the script if it was playing
            if (viewFilter == ViewFilter.ALL) {
                wasPlayingBeforeFilter = playing;
                playing = false;
            }
            viewFilter = vf;
        }
        applyVisibility();
    }

    private void nudgeLayer(int delta) {
        if (pattern == null) return;
        if (manualLayer < 0) {
            manualLayer = delta < 0 ? pattern.maxY : pattern.minY;
        } else {
            manualLayer = Math.max(pattern.minY, Math.min(pattern.maxY, manualLayer + delta));
        }
        applyVisibility();
    }

    private void scrubTo(float t) {
        scrubbing    = true;
        playing      = false;
        playbackTick = (int)(Math.max(0f, Math.min(1f, t)) * script.getTotalTicks());

        PhantasiaScript.Step step = script.getActiveStep(playbackTick);
        if (step != lastAppliedStep) {
            lastAppliedStep = step;
            String nc = step != null ? step.caption() : null;
            if (!Objects.equals(captionCurrent, nc)) {
                captionOutgoing = captionCurrent;
                captionOutAlpha = captionAlpha;
                captionCurrent  = nc;
                captionAlpha    = 0f;
            }
            applyVisibility();
        }
    }

    private void buildOrderStep(int delta) {
        if (pattern == null) return;
        buildOrderGroup = Math.max(0, Math.min(pattern.buildOrder.size() - 1, buildOrderGroup + delta));
        applyVisibility();
    }

    /**
     * FIX (B3): Restores the camera to the script's opening position when defined,
     * otherwise falls back to the facing-aware isometric default.
     */
    private void centerCamera() {
        if (sceneWidget == null || pattern == null || this.isCameraLocked) return;

        // Prefer the first step's declared camera angles
        PhantasiaScript.Step first = script.getSteps().isEmpty() ? null : script.getSteps().get(0);
        if (first != null && first.hasCamera()) {
            this.rotationYaw   = first.yaw();
            this.rotationPitch = first.pitch();
        } else {
            // Fall back to the facing-aware default (same as init)
            this.rotationYaw   = getFacingYaw();
            this.rotationPitch = -35.0f;
        }

        BlockPos cp = pattern.controllerWorldPos != null ? pattern.controllerWorldPos : pattern.origin;
        this.cameraTargetX = cp.getX() + 0.5f;
        this.cameraTargetY = cp.getY() + 0.5f;
        this.cameraTargetZ = cp.getZ() + 0.5f;

        sceneWidget.setCameraYawAndPitch(this.rotationPitch, this.rotationYaw);
        currentZoomDist = 15.0f;
        updateCameraTarget(currentZoomDist);
    }

    private void openFootprintScreen() {
        if (pattern != null)
            Minecraft.getInstance().setScreen(new PhantasiaFootprintScreen(this.pattern, this, this.script));
    }

    private void openBlockFilterScreen() {
        if (pattern != null)
            Minecraft.getInstance().setScreen(
                    new PhantasiaBlockFilterScreen(pattern, script, viewFilter, this));
    }

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean hov) {
        drawBtn(g, mx, my, x, y, w, h, label, hov, C_BTN);
    }
    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean hov, int base) {
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : base);
        if (hov) { g.fill(x, y, x + w, y + 1, C_ACCENT); g.fill(x, y + h - 1, x + w, y + h, C_ACCENT); }
        String lbl = trunc(label, w - 4);
        g.drawString(font, lbl, x + (w - font.width(lbl)) / 2, y + (h - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) s = s.substring(0, s.length() - 2) + "\u2026";
        return s;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getPropName(Property<?> p, Comparable<?> v) {
        try { return ((Property<T>) p).getName((T) v); } catch (Exception e) { return v.toString(); }
    }

    private static String formatTicks(int t) { return String.format("%d.%02ds", t / 20, (t % 20) * 5); }

    @Override
    public void onClose() {
        invalidateSharedLevel();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}