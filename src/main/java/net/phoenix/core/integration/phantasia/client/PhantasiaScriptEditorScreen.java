package net.phoenix.core.integration.phantasia.client;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.phantasia.*;
import net.phoenix.core.mixin.gtceu.AccessorWorldSceneRenderer;

import org.joml.Vector3f;

import java.util.*;

/**
 * PhantasiaScriptEditorScreen — complete in-world script editor.
 *
 * ── Layout ────────────────────────────────────────────────────────────────────
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ [✥ Camera] [◈ Select] [⚠ Annotate] machine name ● unsaved [💾][✕] │ TOP_BAR
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │ Layer │ │
 * │slider │ Full-screen 3D scene (own SceneWidget) │
 * │ (if │ Blocks highlighted/faded based on current step + mode │
 * │layer) │ Mistake markers floating above marked blocks │
 * ├───────┴──────────────────────────────────────────────────────────────────┤
 * │ [Step 1] [Step 2●] [Step 3] [+] │ Caption: _______ t= ___ ○ Running │ STEP_ROW
 * │ Show: [all][layer][range][pos].. │ 📷 Capture ✕Cam [Hide Y] [HidePos]│
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │ ◄ ──●─────────────●──────●──────────────────────────────────────── ► │ TIMELINE
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * ── Interaction modes ─────────────────────────────────────────────────────────
 *
 * CAMERA — Left-drag orbits. Middle-drag pans. Scroll zooms. (default)
 *
 * SELECT — Click any block to toggle it in the step's position list.
 * Selected blocks glow cyan; others fade to 40% opacity.
 * Show mode is automatically set to "pos".
 * Right-click a selected block to remove it.
 * Ctrl+A selects all visible blocks. Ctrl+D deselects all.
 *
 * ANNOTATE — Left-click any block → inline label + colour picker appears.
 * Right-click an already-marked block → removes its marker.
 * Mistake markers rendered as floating coloured labels in 3D space.
 *
 * ── Step navigation ───────────────────────────────────────────────────────────
 * Click dots on timeline, or press ← → arrow keys.
 * Drag a dot left/right on the timeline to adjust its tick value live.
 * Alt+drag a step card to reorder steps.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaScriptEditorScreen extends Screen {

    // ── Theme ─────────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF080810;
    private static final int C_BAR = 0xEE0A0A14;
    private static final int C_PANEL = 0xDD0C0C1A;
    private static final int C_ACCENT = 0xFF4FC3F7;
    private static final int C_BTN = 0xBB151528;
    private static final int C_BTN_HOV = 0xBB1A2840;
    private static final int C_BTN_ACT = 0xFF0D3050;
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF667788;
    private static final int C_WARN = 0xFFFFB74D;
    private static final int C_GREEN = 0xFF66BB6A;
    private static final int C_RED = 0xFFFF5252;

    // Mistake colour palette (displayed as swatches in ANNOTATE mode)
    private static final int[] MISTAKE_COLORS = {
            0xFFFFB74D, 0xFFFF5252, 0xFF66BB6A, 0xFF4FC3F7, 0xFFCE93D8, 0xFFFFFFFF
    };
    private static final String[] MISTAKE_COLOR_NAMES = {
            "Amber", "Red", "Green", "Cyan", "Purple", "White"
    };

    private static final int TOP_BAR_H = 22;
    private static final int STEP_ROW_H = 50;   // two rows of controls
    private static final int TIMELINE_H = 22;
    private static final int BOTTOM_H = STEP_ROW_H + TIMELINE_H;

    private static final String[] SHOW_MODES = { "all", "layer", "layers", "pos", "parts", "controller", "functional" };
    private static final String[] SHOW_LABELS = { "All", "Layer", "Range", "Pos", "Parts", "Ctrl", "Func" };

    // ── Mode ──────────────────────────────────────────────────────────────────
    private enum Mode {
        CAMERA,
        SELECT,
        ANNOTATE
    }

    private Mode mode = Mode.CAMERA;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final PhantasiaSceneScreen parentScene;
    private final String machineId;
    private PhantasiaScriptData data;
    private boolean dirty = false;
    private int selectedStep = 0;

    // ── Own 3D world ──────────────────────────────────────────────────────────
    private SceneWidget sceneWidget;
    private TrackedDummyWorld editorLevel;
    private PhantasiaLoadedPattern pattern;

    // ── Camera ────────────────────────────────────────────────────────────────
    private float camYaw = -135f;
    private float camPitch = -30f;
    private float camDist = 15f;
    private float camTgtX, camTgtY, camTgtZ;

    // ── SELECT mode ───────────────────────────────────────────────────────────
    private final Set<BlockPos> selectedWorldPos = new LinkedHashSet<>();
    private BlockPos hoveredWorldPos = null;
    // Pulse animation for selected blocks (0..1)
    private float selectPulse = 0f;
    private boolean pulseUp = true;

    // ── ANNOTATE mode ─────────────────────────────────────────────────────────
    private BlockPos pendingAnnotationLocalPos = null;
    private int selectedMistakeColor = 0;  // index into MISTAKE_COLORS
    private int hoveredMistakeIndex = -1;  // for right-click removal

    // ── Layer slider ──────────────────────────────────────────────────────────
    private boolean draggingLayer = false;
    private boolean draggingLayerMax = false;

    // ── Timeline dragging ─────────────────────────────────────────────────────
    private int draggingTimelineDot = -1;   // step index being dragged, or -1
    private boolean dotDragMoved = false;   // true once drag has moved enough to commit
    private double dotDragStartMX = 0;      // mouse X when drag began, for threshold

    // ── Timeline hover ghost (shows + for empty track space) ──────────────────
    private int timelineGhostX = -1;        // screen X of the + ghost, or -1 if none
    private int timelineGhostTick = -1;     // tick value the ghost represents

    // ── Step reordering (Alt+drag) ────────────────────────────────────────────
    private int reorderingStep = -1;
    private int reorderInsertAt = -1;

    // ── Preview playback ──────────────────────────────────────────────────────
    private boolean previewing = false;
    private int previewTick = 0;
    private float previewAccum = 0f;

    // ── Inputs ────────────────────────────────────────────────────────────────
    private EditBox captionBox;
    private EditBox tickBox;
    private EditBox hideLayerBox;
    private EditBox hidePosBox;
    private EditBox annotationLabelBox;
    private EditBox globalMistakeBox;
    private EditBox fakeRecipeBox;

    // ── Button registry ───────────────────────────────────────────────────────
    private record Btn(int x, int y, int w, int h, Runnable action) {

        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Btn> btns = new ArrayList<>();

    // Mouse position cached each frame so sub-render methods can access it without threading mx/my everywhere
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public PhantasiaScriptEditorScreen(PhantasiaSceneScreen parent,
                                       String machineId,
                                       PhantasiaScriptData original) {
        super(Component.literal("Editor"));
        this.parentScene = parent;
        this.machineId = machineId;
        this.data = original.copy();
        ensureOneStep();
    }

    private void ensureOneStep() {
        if (data.getSteps().isEmpty()) {
            PhantasiaScriptData.StepData s = new PhantasiaScriptData.StepData(0, null);
            s.show = "all";
            data.getSteps().add(s);
        }
        selectedStep = Mth.clamp(selectedStep, 0, data.getSteps().size() - 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        if (editorLevel == null) setupEditorWorld();

        int sceneH = this.height - TOP_BAR_H - BOTTOM_H;
        if (sceneWidget == null) {
            sceneWidget = new SceneWidget(0, TOP_BAR_H, this.width, sceneH, editorLevel);
            sceneWidget.setDraggable(false);
            WorldSceneRenderer r = sceneWidget.getRenderer();
            r.useCacheBuffer(true);
            if (r instanceof AccessorWorldSceneRenderer acc) acc.setEndBatchLast(true);
            initCamera();
        } else {
            sceneWidget.setSize(this.width, sceneH);
            // Re-apply camera so widget internal state matches our fields after resize
            applyCamera();
        }

        buildInputWidgets();
        populateInputsFromStep();
        rebuildVisibility();
    }

    private void setupEditorWorld() {
        pattern = parentScene != null ? parentScene.getLoadedPattern() : null;
        if (pattern == null) {
            onClose();
            return;
        }
        editorLevel = new TrackedDummyWorld();
        editorLevel.addBlocks(pattern.blockMap);
    }

    private void initCamera() {
        if (pattern == null) return;
        float midY = pattern.origin.getY() + (pattern.minY + pattern.maxY) * 0.5f + 0.5f;
        BlockPos cp = pattern.controllerWorldPos != null ? pattern.controllerWorldPos : pattern.origin;
        camTgtX = cp.getX() + 0.5f;
        camTgtY = midY;
        camTgtZ = cp.getZ() + 0.5f;
        int machineH = pattern.maxY - pattern.minY + 1;
        camDist = 15f + Math.max(0, machineH - 8) * 1.5f;
        if (!data.getSteps().isEmpty() && data.getSteps().get(0).camera != null) {
            camYaw = data.getSteps().get(0).camera.yaw;
            camPitch = data.getSteps().get(0).camera.pitch;
        }
        applyCamera();
    }

    private void buildInputWidgets() {
        clearWidgets();

        captionBox = addW(new EditBox(font, 0, 0, 200, 12, Component.empty()));
        captionBox.setMaxLength(256);
        captionBox.setHint(Component.literal("Caption for this step..."));
        captionBox.setResponder(v -> {
            step().caption = v.isBlank() ? null : v;
            dirty = true;
        });

        tickBox = addW(new EditBox(font, 0, 0, 40, 12, Component.empty()));
        tickBox.setMaxLength(5);
        tickBox.setFilter(s -> s.matches("\\d*"));
        tickBox.setResponder(v -> {
            try {
                step().tick = Integer.parseInt(v);
                dirty = true;
            } catch (NumberFormatException ignored) {}
        });

        hideLayerBox = addW(new EditBox(font, 0, 0, 30, 12, Component.empty()));
        hideLayerBox.setMaxLength(4);
        hideLayerBox.setFilter(s -> s.matches("-?\\d*"));
        hideLayerBox.setHint(Component.literal("-1"));
        hideLayerBox.setResponder(v -> {
            try {
                step().hideLayer = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                step().hideLayer = -1;
            }
            dirty = true;
            rebuildVisibility();
        });

        hidePosBox = addW(new EditBox(font, 0, 0, 120, 12, Component.empty()));
        hidePosBox.setMaxLength(512);
        hidePosBox.setHint(Component.literal("x,y,z; x,y,z ..."));
        hidePosBox.setResponder(v -> {
            step().hidePositions = parsePosList(v);
            dirty = true;
            rebuildVisibility();
        });

        annotationLabelBox = addW(new EditBox(font, 0, 0, 180, 12, Component.empty()));
        annotationLabelBox.setMaxLength(128);
        annotationLabelBox.setHint(Component.literal("Mistake label (Enter = confirm)"));
        annotationLabelBox.visible = false;
        annotationLabelBox.active = false;

        globalMistakeBox = addW(new EditBox(font, 0, 0, 200, 12, Component.empty()));
        globalMistakeBox.setMaxLength(256);
        globalMistakeBox.setHint(Component.literal("Global mistake note (Enter = add)"));
        globalMistakeBox.visible = false;
        globalMistakeBox.active = false;

        // Fake recipe id — only visible when the step's working flag is true.
        // The recipe resource-location entered here is injected into the controller's
        // RecipeLogic so recipe-dependent renders (fusion plasma, laser colour, etc.)
        // have a real recipe to read.
        fakeRecipeBox = addW(new EditBox(font, 0, 0, 180, 12, Component.empty()));
        fakeRecipeBox.setMaxLength(256);
        fakeRecipeBox.setHint(Component.literal("gtceu:fusion/recipe_name"));
        fakeRecipeBox.setResponder(v -> {
            step().fakeRecipeId = v.isBlank() ? null : v.trim();
            dirty = true;
        });
        fakeRecipeBox.visible = false;
        fakeRecipeBox.active = false;
    }

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T addW(T w) {
        return addRenderableWidget(w);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        // SELECT pulse
        if (mode == Mode.SELECT) {
            selectPulse += pulseUp ? 0.07f : -0.07f;
            if (selectPulse >= 1f) {
                selectPulse = 1f;
                pulseUp = false;
            }
            if (selectPulse <= 0f) {
                selectPulse = 0f;
                pulseUp = true;
            }
        }
        // Preview playback
        if (previewing) {
            previewAccum += 1f;
            while (previewAccum >= 1f) {
                previewAccum -= 1f;
                previewTick++;
            }
            int total = computeTotalTicks();
            if (previewTick >= total) {
                previewTick = 0;
            }
            // Switch to the step that's active at this tick
            for (int i = data.getSteps().size() - 1; i >= 0; i--) {
                if (data.getSteps().get(i).tick <= previewTick) {
                    if (i != selectedStep) {
                        selectedStep = i;
                        rebuildVisibility();
                    }
                    break;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visibility + block highlighting
    // ─────────────────────────────────────────────────────────────────────────

    private void rebuildVisibility() {
        if (sceneWidget == null || pattern == null) return;

        PhantasiaScriptData.StepData s = step();
        Set<BlockPos> visible = new HashSet<>(pattern.baseplatePositions);

        if (mode == Mode.SELECT) {
            // Show every machine block — user needs to see all blocks to click them.
            // Apply a two-tone tint: selected blocks glow, others are dim.
            for (BlockPos wp : pattern.localToWorld.values()) visible.add(wp);
        } else {
            PhantasiaScriptData tmp = new PhantasiaScriptData(machineId);
            tmp.getSteps().add(s);
            PhantasiaScript tmpScript = PhantasiaScript.fromData(tmp);
            PhantasiaScript.Step compiled = tmpScript.getActiveStep(0);
            for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
                if (compiled == null || compiled.filter().test(e.getKey()))
                    visible.add(e.getValue());
            }
        }

        sceneWidget.setRenderedCore(visible, null);
        sceneWidget.getRenderer().needCompileCache();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        btns.clear();
        hideAllInputs();
        lastMouseX = mx;
        lastMouseY = my;

        g.fill(0, 0, this.width, this.height, C_BG);

        // 3D scene
        if (sceneWidget != null) {
            BlockHitResult hit = sceneWidget.getRenderer().getLastTraceResult();
            hoveredWorldPos = (hit != null && hit.getType() == HitResult.Type.BLOCK) ? hit.getBlockPos() : null;
            sceneWidget.drawInBackground(g, mx, my, partial);
        }

        // Mode-specific in-scene overlays (drawn in screen space)
        renderInSceneOverlays(g, mx, my);

        // UI chrome
        renderTopBar(g, mx, my);
        renderLayerSlider(g, mx, my);
        renderStepRow(g, mx, my);
        renderTimeline(g, mx, my);

        // EditBoxes on top
        super.render(g, mx, my, partial);

        // Tooltip
        if (hoveredWorldPos != null && editorLevel != null && mode == Mode.SELECT) {
            try {
                BlockState bs = editorLevel.getBlockState(hoveredWorldPos);
                if (!bs.isAir()) g.renderTooltip(font, bs.getBlock().getName(), mx, my);
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-scene overlays
    // ─────────────────────────────────────────────────────────────────────────

    private void renderInSceneOverlays(GuiGraphics g, int mx, int my) {
        renderMistakeMarkers(g);
        if (mode == Mode.SELECT) renderSelectOverlay(g, mx, my);
        if (mode == Mode.ANNOTATE) renderAnnotateOverlay(g, mx, my);
    }

    /**
     * Renders coloured mistake labels floating above their blocks in screen space.
     * Uses an approximate world→screen projection.
     */
    private void renderMistakeMarkers(GuiGraphics g) {
        if (data.getMistakes().isEmpty() || pattern == null || sceneWidget == null) return;
        hoveredMistakeIndex = -1;

        WorldSceneRenderer r = sceneWidget.getRenderer();
        Vector3f eye = r.getEyePos();
        Vector3f lookat = r.getLookAt();
        if (eye == null || lookat == null) return;

        Vector3f fwd = new Vector3f(lookat).sub(eye).normalize();
        Vector3f rgt = new Vector3f(fwd).cross(new Vector3f(0, 1, 0)).normalize();
        Vector3f upv = new Vector3f(rgt).cross(fwd).normalize();
        float fov = this.height / (2f * (float) Math.tan(Math.toRadians(50)));

        for (int i = 0; i < data.getMistakes().size(); i++) {
            PhantasiaScriptData.MistakeData m = data.getMistakes().get(i);
            BlockPos local = new BlockPos(m.x, m.y, m.z);
            BlockPos world = pattern.localToWorld.get(local);
            if (world == null) continue;

            float wx = world.getX() + 0.5f, wy = world.getY() + 1.4f, wz = world.getZ() + 0.5f;
            Vector3f toP = new Vector3f(wx - eye.x, wy - eye.y, wz - eye.z);
            float depth = toP.dot(fwd);
            if (depth < 0.5f) continue;

            float sx = this.width / 2f + (toP.dot(rgt) / depth) * fov;
            float sy = this.height / 2f - (toP.dot(upv) / depth) * fov;
            int isx = (int) sx, isy = (int) sy;

            if (isy < TOP_BAR_H || isy > this.height - BOTTOM_H) continue;

            int col = m.colorArgb();
            String lbl = m.label;
            int lw = font.width(lbl) + 8;

            // Background pill
            g.fill(isx - lw / 2 - 1, isy - 6, isx + lw / 2 + 1, isy + 8, 0xCC000000);
            g.fill(isx - lw / 2 - 1, isy - 6, isx + lw / 2 + 1, isy - 5, col);
            g.drawCenteredString(font, lbl, isx, isy - 3, col);

            // Small stem down to block
            g.fill(isx - 1, isy + 8, isx + 1, isy + 14, col & 0x88FFFFFF);

            // Track for hover (for right-click removal in ANNOTATE mode)
            if (mode == Mode.ANNOTATE && isOver(lastMouseX, lastMouseY, isx - lw / 2 - 1, isy - 6, lw + 2, 14))
                hoveredMistakeIndex = i;
        }
    }

    private void renderSelectOverlay(GuiGraphics g, int mx, int my) {
        // Instruction banner
        int hy = TOP_BAR_H + 4;
        String hint = selectedWorldPos.isEmpty() ?
                "Left-click blocks to add to step  |  Ctrl+A: select all  |  Ctrl+D: clear" :
                selectedWorldPos.size() + " block" + (selectedWorldPos.size() == 1 ? "" : "s") +
                        " selected  —  Left-click to toggle  |  Right-click to remove";
        drawBanner(g, hint, hy, C_ACCENT);

        // Hovered block action label
        if (hoveredWorldPos != null && pattern != null) {
            BlockPos local = pattern.toLocal(hoveredWorldPos);
            if (local != null && !pattern.baseplatePositions.contains(hoveredWorldPos)) {
                boolean isSel = isInPositionList(local);
                String action = isSel ? "▼ Remove from step" : "▲ Add to step";
                int ay = hy + 20;
                g.drawCenteredString(font, action, this.width / 2, ay, isSel ? C_WARN : C_GREEN);
            }
        }

        // Draw selection highlights using scissor + fill over the scene
        // We draw a translucent cyan border around selected blocks and a dim tint on unselected
        if (pattern != null && sceneWidget != null) {
            WorldSceneRenderer r = sceneWidget.getRenderer();
            Vector3f eye = r.getEyePos();
            Vector3f lookat = r.getLookAt();
            if (eye != null && lookat != null) {
                Vector3f fwd = new Vector3f(lookat).sub(eye).normalize();
                Vector3f rgt = new Vector3f(fwd).cross(new Vector3f(0, 1, 0)).normalize();
                Vector3f upv = new Vector3f(rgt).cross(fwd).normalize();
                float fov = this.height / (2f * (float) Math.tan(Math.toRadians(50)));

                for (BlockPos wp : selectedWorldPos) {
                    // Project block centre + top face corners to screen
                    float[] sc = projectToScreen(wp.getX() + 0.5f, wp.getY() + 1f, wp.getZ() + 0.5f,
                            eye, fwd, rgt, upv, fov);
                    if (sc == null || sc[2] < 0.3f) continue;
                    int isx = (int) sc[0], isy = (int) sc[1];
                    if (isy < TOP_BAR_H || isy > this.height - BOTTOM_H) continue;

                    // Pulsing cyan highlight at the block's screen projection
                    int alpha = (int) (0.5f + selectPulse * 0.5f) * 0xAA;
                    alpha = Mth.clamp(alpha, 0x44, 0xBB);
                    int col = (alpha << 24) | (C_ACCENT & 0x00FFFFFF);
                    g.fill(isx - 3, isy - 3, isx + 3, isy + 3, col);
                    g.fill(isx - 5, isy - 1, isx + 5, isy + 1, col & 0x66FFFFFF);
                    g.fill(isx - 1, isy - 5, isx + 1, isy + 5, col & 0x66FFFFFF);
                }

                // Hovered block indicator when not yet selected
                if (hoveredWorldPos != null && !selectedWorldPos.contains(hoveredWorldPos) &&
                        !pattern.baseplatePositions.contains(hoveredWorldPos)) {
                    float[] sc = projectToScreen(hoveredWorldPos.getX() + 0.5f, hoveredWorldPos.getY() + 1f,
                            hoveredWorldPos.getZ() + 0.5f, eye, fwd, rgt, upv, fov);
                    if (sc != null && sc[2] > 0.3f) {
                        int isx = (int) sc[0], isy = (int) sc[1];
                        g.fill(isx - 4, isy - 4, isx + 4, isy + 4, 0x66FFFFFF);
                    }
                }
            }
        }
    }

    private void renderAnnotateOverlay(GuiGraphics g, int mx, int my) {
        if (pendingAnnotationLocalPos != null) {
            // Colour picker panel above the step row
            int px = this.width / 2 - 160;
            int py = this.height - BOTTOM_H - 52;
            int pw = 320;

            g.fill(px, py, px + pw, py + 50, C_BAR);
            g.fill(px, py, px + pw, py + 1, C_WARN);

            // Label box
            placeBox(annotationLabelBox, px + 6, py + 5, pw - 14, 12);
            annotationLabelBox.visible = true;
            annotationLabelBox.active = true;
            annotationLabelBox.setFocused(true);

            // Colour swatches
            int sx = px + 6, sy = py + 22;
            g.drawString(font, "Colour:", sx, sy + 1, C_DIM, false);
            sx += 44;
            for (int i = 0; i < MISTAKE_COLORS.length; i++) {
                boolean sel = i == selectedMistakeColor;
                boolean hov = isOver(mx, my, sx, sy, 16, 12);
                g.fill(sx, sy, sx + 16, sy + 12, MISTAKE_COLORS[i]);
                if (sel) {
                    g.fill(sx - 1, sy - 1, sx + 17, sy, 0xFFFFFFFF);
                    g.fill(sx - 1, sy + 12, sx + 17, sy + 13, 0xFFFFFFFF);
                }
                int fi = i;
                btns.add(new Btn(sx, sy, 16, 12, () -> selectedMistakeColor = fi));
                if (hov) g.drawString(font, MISTAKE_COLOR_NAMES[i], sx, sy + 14, C_DIM, false);
                sx += 20;
            }

            // Confirm / cancel
            int btnY = py + 36;
            btn(g, mx, my, px + pw - 120, btnY, 54, 12, "✓ Add", C_GREEN, this::confirmAnnotation);
            btn(g, mx, my, px + pw - 62, btnY, 54, 12, "✕ Cancel", C_BTN, this::cancelAnnotation);

            g.drawString(font, "Marking: " + pendingAnnotationLocalPos.toShortString(), px + 6, btnY + 1, C_DIM, false);
            return;
        }

        // Not pending — show instruction + existing mistakes list
        String hint = hoveredMistakeIndex >= 0 ? "Right-click marker to remove  |  Left-click block to add" :
                "Left-click any block to add a mistake marker";
        drawBanner(g, hint, TOP_BAR_H + 4, C_WARN);

        // Global mistakes section in bottom bar
        if (mode == Mode.ANNOTATE) {
            placeBox(globalMistakeBox,
                    this.width - 310, this.height - BOTTOM_H + STEP_ROW_H + 4, 220, 12);
            globalMistakeBox.visible = true;
            globalMistakeBox.active = true;
            int glx = this.width - 310;
            int gly = this.height - BOTTOM_H + STEP_ROW_H + 4;
            g.drawString(font, "Global note:", glx - 74, gly + 2, C_DIM, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top bar
    // ─────────────────────────────────────────────────────────────────────────

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, this.width, TOP_BAR_H, C_BAR);
        g.fill(0, TOP_BAR_H - 1, this.width, TOP_BAR_H, C_ACCENT);

        int x = 6;
        x = modeBtn(g, mx, my, x, Mode.CAMERA, "✥ Camera");
        x = modeBtn(g, mx, my, x, Mode.SELECT, "◈ Select");
        x = modeBtn(g, mx, my, x, Mode.ANNOTATE, "⚠ Annotate");

        // Preview toggle
        x += 6;
        boolean ph = isOver(mx, my, x, 3, 70, TOP_BAR_H - 6);
        g.fill(x, 3, x + 70, TOP_BAR_H - 3, previewing ? C_BTN_ACT : (ph ? C_BTN_HOV : C_BTN));
        if (previewing) g.fill(x, TOP_BAR_H - 3, x + 70, TOP_BAR_H - 2, C_GREEN);
        g.drawString(font, previewing ? "⏹ Stop" : "▶ Preview", x + 5, (TOP_BAR_H - 8) / 2,
                previewing ? C_GREEN : C_DIM, false);
        btns.add(new Btn(x, 3, 70, TOP_BAR_H - 6, this::togglePreview));

        // Machine name (centred)
        String name = machineId.contains(":") ? machineId.split(":")[1].replace('_', ' ') : machineId;
        g.drawCenteredString(font, name, this.width / 2, (TOP_BAR_H - 8) / 2, C_DIM);

        // Right side
        int rx = this.width - 4;
        rx = topBtn(g, mx, my, rx, "✕ Back", C_BTN, this::onClose);
        rx = topBtn(g, mx, my, rx, "💾 Save", C_GREEN, this::save);
        if (dirty) {
            String dot = "● unsaved";
            rx -= font.width(dot) + 10;
            g.drawString(font, dot, rx, (TOP_BAR_H - 8) / 2, C_WARN, false);
        }
    }

    private int modeBtn(GuiGraphics g, int mx, int my, int x, Mode m, String label) {
        int w = font.width(label) + 12;
        boolean act = mode == m;
        boolean hov = isOver(mx, my, x, 3, w, TOP_BAR_H - 6);
        g.fill(x, 3, x + w, TOP_BAR_H - 3, act ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
        if (act) g.fill(x, TOP_BAR_H - 3, x + w, TOP_BAR_H - 2, C_ACCENT);
        g.drawString(font, label, x + 6, (TOP_BAR_H - 8) / 2, act ? C_ACCENT : C_TEXT, false);
        btns.add(new Btn(x, 3, w, TOP_BAR_H - 6, () -> setMode(m)));
        return x + w + 4;
    }

    private int topBtn(GuiGraphics g, int mx, int my, int rx, String label, int color, Runnable action) {
        int w = font.width(label) + 10;
        int x = rx - w, y = 3, h = TOP_BAR_H - 6;
        boolean hov = isOver(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : color);
        if (hov) {
            g.fill(x, y, x + w, y + 1, C_ACCENT);
            g.fill(x, y + h - 1, x + w, y + h, C_ACCENT);
        }
        g.drawString(font, label, x + 5, (TOP_BAR_H - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
        btns.add(new Btn(x, y, w, h, action));
        return x - 4;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer slider
    // ─────────────────────────────────────────────────────────────────────────

    private void renderLayerSlider(GuiGraphics g, int mx, int my) {
        PhantasiaScriptData.StepData s = step();
        if (!"layer".equals(s.show) && !"layers".equals(s.show)) return;
        if (pattern == null) return;

        int sliderX = 10;
        int sceneTop = TOP_BAR_H;
        int sceneBottom = this.height - BOTTOM_H;
        int sliderH = sceneBottom - sceneTop - 24;
        int sliderY = sceneTop + 12;

        // Background track
        g.fill(sliderX + 3, sliderY, sliderX + 7, sliderY + sliderH, 0x44FFFFFF);
        // Label
        g.drawString(font, "Y", sliderX + 1, sliderY - 10, C_DIM, false);

        int range = Math.max(1, pattern.maxY - pattern.minY);

        if ("layer".equals(s.show)) {
            float t = 1f - (float) (s.layer - pattern.minY) / range;
            int thumbY = sliderY + (int) (t * sliderH);
            boolean hov = isOver(mx, my, sliderX - 2, thumbY - 7, 18, 14) || draggingLayer;
            g.fill(sliderX - 2, thumbY - 6, sliderX + 16, thumbY + 6, hov ? C_ACCENT : C_BTN_ACT);
            g.drawString(font, String.valueOf(s.layer), sliderX + 18, thumbY - 4, C_ACCENT, false);
            // Step markers along track
            for (int y = pattern.minY; y <= pattern.maxY; y++) {
                float ft = 1f - (float) (y - pattern.minY) / range;
                int fy = sliderY + (int) (ft * sliderH);
                g.fill(sliderX + 4, fy, sliderX + 6, fy + 1, 0x33FFFFFF);
            }
        } else {
            float tMin = 1f - (float) (s.layerMin - pattern.minY) / range;
            float tMax = 1f - (float) (s.layerMax - pattern.minY) / range;
            int tyMin = sliderY + (int) (tMin * sliderH);
            int tyMax = sliderY + (int) (tMax * sliderH);
            // Range fill
            g.fill(sliderX + 2, tyMax, sliderX + 8, tyMin, 0x664FC3F7);
            // Thumbs
            boolean hovMin = isOver(mx, my, sliderX - 2, tyMin - 6, 18, 12) || (draggingLayer && !draggingLayerMax);
            boolean hovMax = isOver(mx, my, sliderX - 2, tyMax - 6, 18, 12) || (draggingLayer && draggingLayerMax);
            g.fill(sliderX - 2, tyMin - 5, sliderX + 16, tyMin + 5, hovMin ? C_ACCENT : C_BTN_ACT);
            g.fill(sliderX - 2, tyMax - 5, sliderX + 16, tyMax + 5, hovMax ? C_ACCENT : C_BTN_ACT);
            g.drawString(font, s.layerMin + "→" + s.layerMax, sliderX + 18, (tyMin + tyMax) / 2 - 4, C_ACCENT, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step row (two rows of controls)
    // ─────────────────────────────────────────────────────────────────────────

    private void renderStepRow(GuiGraphics g, int mx, int my) {
        int rowY = this.height - BOTTOM_H;
        int row2 = rowY + STEP_ROW_H / 2;

        g.fill(0, rowY, this.width, rowY + STEP_ROW_H, C_BAR);
        g.fill(0, rowY, this.width, rowY + 1, C_ACCENT);

        PhantasiaScriptData.StepData s = step();

        // ── ROW 1: step navigation + caption + tick + working + camera ─────────
        int y1 = rowY + 5;
        int x = 8;

        // Step number label
        String stepLbl = "Step " + (selectedStep + 1) + "/" + data.getSteps().size();
        g.drawString(font, stepLbl, x, y1 + 2, C_ACCENT, false);
        x += font.width(stepLbl) + 6;

        // +/- /Dup buttons
        btn(g, mx, my, x, y1, 14, 14, "+", C_BTN, this::addStep);
        x += 18;
        btn(g, mx, my, x, y1, 14, 14, "−", C_BTN, this::deleteStep);
        x += 18;
        btn(g, mx, my, x, y1, 24, 14, "Dup", C_BTN, this::duplicateStep);
        x += 30;
        // Move buttons
        btn(g, mx, my, x, y1, 14, 14, "◄", C_BTN, () -> moveStep(selectedStep, -1));
        x += 18;
        btn(g, mx, my, x, y1, 14, 14, "►", C_BTN, () -> moveStep(selectedStep, +1));
        x += 22;

        // t= tick
        g.drawString(font, "t=", x, y1 + 2, C_DIM, false);
        x += font.width("t=") + 2;
        placeBox(tickBox, x, y1, 38, 13);
        x += 44;

        // Caption
        g.drawString(font, "Caption:", x, y1 + 2, C_DIM, false);
        x += font.width("Caption:") + 4;
        int capW = Math.min(260, this.width / 2 - x - 10);
        placeBox(captionBox, x, y1, capW, 13);
        x += capW + 10;

        // Working toggle
        boolean wh = isOver(mx, my, x, y1, 82, 14);
        g.fill(x, y1, x + 82, y1 + 14, s.working ? C_BTN_ACT : (wh ? C_BTN_HOV : C_BTN));
        if (s.working) g.fill(x, y1, x + 82, y1 + 1, C_GREEN);
        g.drawString(font, (s.working ? "✓" : "○") + " Running", x + 5, y1 + 3, s.working ? C_GREEN : C_DIM, false);
        btns.add(new Btn(x, y1, 82, 14, () -> {
            s.working = !s.working;
            dirty = true;
        }));
        x += 88;

        // Fake recipe input — only shown when the step is set to Working.
        // Lets recipe-dependent renders (fusion plasma colour, laser arc, etc.)
        // show the correct visual by injecting a real GT recipe into RecipeLogic.
        if (s.working) {
            g.drawString(font, "Recipe:", x, y1 + 3, C_DIM, false);
            x += font.width("Recipe:") + 4;
            placeBox(fakeRecipeBox, x, y1, 180, 13);
            x += 186;
        }

        // Camera capture
        boolean hasCam = s.camera != null;
        boolean cch = isOver(mx, my, x, y1, 110, 14);
        g.fill(x, y1, x + 110, y1 + 14, hasCam ? C_BTN_ACT : (cch ? C_BTN_HOV : C_BTN));
        if (hasCam) g.fill(x, y1, x + 110, y1 + 1, C_ACCENT);
        String camLbl = hasCam ? "📷 Update Cam" : "📷 Capture Cam";
        g.drawString(font, camLbl, x + 5, y1 + 3, hasCam ? C_ACCENT : C_DIM, false);
        btns.add(new Btn(x, y1, 110, 14, this::captureCamera));
        x += 116;
        if (hasCam) {
            boolean rch = isOver(mx, my, x, y1, 48, 14);
            g.fill(x, y1, x + 48, y1 + 14, rch ? C_BTN_HOV : C_BTN);
            g.drawString(font, "✕ Cam", x + 5, y1 + 3, rch ? C_RED : C_DIM, false);
            btns.add(new Btn(x, y1, 48, 14, () -> {
                s.camera = null;
                dirty = true;
            }));
            x += 54;
        }

        // ── ROW 2: show mode + hide controls ──────────────────────────────────
        int y2 = rowY + STEP_ROW_H / 2 + 3;
        x = 8;
        g.drawString(font, "Show:", x, y2 + 2, C_DIM, false);
        x += font.width("Show:") + 4;

        for (int i = 0; i < SHOW_MODES.length; i++) {
            String sm = SHOW_MODES[i];
            String sml = SHOW_LABELS[i];
            int mw = font.width(sml) + 10;
            boolean act = sm.equals(s.show);
            boolean hov = isOver(mx, my, x, y2, mw, 14);
            g.fill(x, y2, x + mw, y2 + 14, act ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
            if (act) g.fill(x, y2, x + mw, y2 + 1, C_ACCENT);
            g.drawString(font, sml, x + 5, y2 + 3, act ? C_ACCENT : C_TEXT, false);
            final String fsm = sm;
            btns.add(new Btn(x, y2, mw, 14, () -> {
                s.show = fsm;
                if ("pos".equals(fsm)) {
                    syncSelectedFromStep();
                    setMode(Mode.SELECT);
                } else if (mode == Mode.SELECT && !"pos".equals(fsm)) setMode(Mode.CAMERA);
                dirty = true;
                rebuildVisibility();
            }));
            x += mw + 3;

            // Show current layer/range value inline next to the active button
            if (act) {
                if ("layer".equals(sm))
                    g.drawString(font, " Y=" + s.layer, x, y2 + 2, C_ACCENT, false);
                else if ("layers".equals(sm))
                    g.drawString(font, " " + s.layerMin + "→" + s.layerMax, x, y2 + 2, C_ACCENT, false);
            }
        }

        // Hide controls (right side of row 2)
        int rx2 = this.width - 8;
        // Hide positions
        int hpW = 130;
        rx2 -= hpW;
        g.drawString(font, "HidePos:", rx2 - 54, y2 + 2, C_DIM, false);
        placeBox(hidePosBox, rx2, y2, hpW, 13);
        rx2 -= 58;

        // Hide layer
        g.drawString(font, "HideY:", rx2 - 40, y2 + 2, C_DIM, false);
        placeBox(hideLayerBox, rx2, y2, 30, 13);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timeline
    // ─────────────────────────────────────────────────────────────────────────

    private void renderTimeline(GuiGraphics g, int mx, int my) {
        int tlY = this.height - TIMELINE_H;
        int margin = 30;
        int trackW = this.width - margin * 2;
        int total = computeTotalTicks();
        int midY = tlY + TIMELINE_H / 2;

        // Panel background + top rule
        g.fill(0, tlY, this.width, this.height, C_PANEL);
        g.fill(0, tlY, this.width, tlY + 1, 0x33FFFFFF);

        // Track groove
        g.fill(margin, midY - 1, margin + trackW, midY + 1, 0xFF1A2C3C);
        // Track end caps
        g.fill(margin - 1, midY - 3, margin, midY + 3, 0xFF3A506A);
        g.fill(margin + trackW, midY - 3, margin + trackW + 1, midY + 3, 0xFF3A506A);

        if (data.getSteps().isEmpty()) return;

        // ── Ghost + marker: hovering empty track space ──────────────────────
        // Compute whether the mouse is over the timeline track but not over any dot.
        timelineGhostX = -1;
        timelineGhostTick = -1;
        boolean mouseOnTrack = isOver(mx, my, margin, tlY, trackW, TIMELINE_H);
        if (mouseOnTrack && draggingTimelineDot < 0) {
            // Check if mouse is near any existing dot — suppress ghost if so.
            boolean nearDot = false;
            for (PhantasiaScriptData.StepData s : data.getSteps()) {
                float t = total > 0 ? (float) s.tick / total : 0f;
                int dotX = margin + (int) (t * trackW);
                if (Math.abs(mx - dotX) < 14) {
                    nearDot = true;
                    break;
                }
            }
            if (!nearDot) {
                timelineGhostX = mx;
                timelineGhostTick = total > 0 ? Math.round((float) (mx - margin) / trackW * total) : 0;
                // Ghost: faint dashed vertical line + "+" label
                g.fill(mx, tlY + 2, mx + 1, tlY + TIMELINE_H - 2, 0x554FC3F7);
                int ghW = font.width("+") + 6;
                g.fill(mx - ghW / 2, midY - 7, mx + ghW / 2, midY + 7, 0x884FC3F7);
                g.drawCenteredString(font, "+", mx, midY - 3, 0xFFFFFFFF);
                // Tick hint below
                String ghLbl = "t=" + timelineGhostTick;
                g.drawCenteredString(font, ghLbl, mx, midY + 8, 0x664FC3F7);
            }
        }

        // Preview playhead
        if (previewing && total > 0) {
            int px = margin + (int) ((float) previewTick / total * trackW);
            g.fill(px - 1, tlY + 2, px + 1, tlY + TIMELINE_H - 2, 0xAAFFFFFF);
        }

        // ── Step dots ────────────────────────────────────────────────────────
        for (int i = 0; i < data.getSteps().size(); i++) {
            PhantasiaScriptData.StepData s = data.getSteps().get(i);
            float t = total > 0 ? (float) s.tick / total : 0f;
            int dotX = margin + (int) (t * trackW);
            boolean sel = i == selectedStep;
            boolean hov = isOver(mx, my, dotX - 9, midY - 9, 18, 18);
            boolean dragging = draggingTimelineDot == i;

            // Outer ring: accent for selected/dragging, dim for others
            int ringCol = sel || dragging ? C_ACCENT : (hov ? 0xFFAADDFF : 0xFF3A506A);
            g.fill(dotX - 7, midY - 7, dotX + 7, midY + 7, ringCol);

            // Inner fill
            int fillCol = dragging ? 0xFFFFFFFF : (sel ? 0xFF1A3C5C : 0xFF0A1520);
            g.fill(dotX - 5, midY - 5, dotX + 5, midY + 5, fillCol);

            // Step number inside dot (tiny, centred)
            String numLbl = String.valueOf(i + 1);
            int numCol = sel || dragging ? C_ACCENT : (hov ? 0xFFCCEEFF : C_DIM);
            g.drawCenteredString(font, numLbl, dotX, midY - 3, numCol);

            // Connecting line between adjacent dots
            if (i + 1 < data.getSteps().size()) {
                PhantasiaScriptData.StepData next = data.getSteps().get(i + 1);
                float nt = total > 0 ? (float) next.tick / total : 0f;
                int nextX = margin + (int) (nt * trackW);
                g.fill(dotX + 7, midY, nextX - 7, midY + 1, 0xFF1E3A52);
            }

            // Tick label below dot (full detail for selected/dragging, just step# otherwise)
            String lbl = (sel || dragging) ? "#" + (i + 1) + "  t=" + s.tick + "  [" + s.show + "]" : "#" + (i + 1);
            int lx = Mth.clamp(dotX - font.width(lbl) / 2, margin, margin + trackW - font.width(lbl));
            g.drawString(font, lbl, lx, midY + 9, sel || dragging ? C_ACCENT : C_DIM, false);

            // Caption snippet above the selected dot
            if (sel && s.caption != null && !s.caption.isBlank()) {
                String cap = trunc(s.caption, 180);
                int cx = Mth.clamp(dotX - font.width(cap) / 2, 4, this.width - font.width(cap) - 4);
                g.drawString(font, cap, cx, midY - 19, C_TEXT, false);
            }

            // Right-click delete hint on hover
            if (hov && !sel && !dragging) {
                g.drawCenteredString(font, "✕", dotX, midY - 16, 0x88FF5252);
            }

            // Register click target (not drag — drag is handled in startTimelineDotDrag)
            final int fi = i;
            btns.add(new Btn(dotX - 9, midY - 9, 18, 18, () -> selectStep(fi)));
        }

        // Tick ruler: small marks every 60 ticks (3 seconds)
        int rulerInterval = 60;
        for (int tick = 0; tick <= total; tick += rulerInterval) {
            int rx = margin + (int) ((float) tick / total * trackW);
            g.fill(rx, midY + 1, rx + 1, midY + 4, 0x33FFFFFF);
        }

        g.drawString(font, "◄ ►", margin - 24, midY - 4, C_DIM, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // 1. Registered button hits
        for (Btn b : btns) if (b.hit(mx, my)) {
            b.action().run();
            return true;
        }

        // 2. Widgets
        if (super.mouseClicked(mx, my, btn)) return true;

        // 3. Layer slider start
        if (startLayerSliderDrag(mx, my)) return true;

        // 4. Timeline interactions (before general scene click so the bar takes priority)
        int tlY = this.height - TIMELINE_H;
        int midY = tlY + TIMELINE_H / 2;
        int margin = 30, trackW = this.width - margin * 2;
        int total = computeTotalTicks();
        boolean onTimeline = isOver(mx, my, 0, tlY, this.width, TIMELINE_H);

        if (onTimeline) {
            // Right-click on a dot → delete that step
            if (btn == 1) {
                for (int i = 0; i < data.getSteps().size(); i++) {
                    PhantasiaScriptData.StepData s = data.getSteps().get(i);
                    float t = total > 0 ? (float) s.tick / total : 0f;
                    int dotX = margin + (int) (t * trackW);
                    if (isOver(mx, my, dotX - 9, midY - 9, 18, 18)) {
                        if (data.getSteps().size() > 1) {
                            data.getSteps().remove(i);
                            selectStep(Math.min(selectedStep, data.getSteps().size() - 1));
                            dirty = true;
                        }
                        return true;
                    }
                }
            }

            // Left-click: dot drag or click-on-empty-track to add a new step
            if (btn == 0) {
                if (startTimelineDotDrag(mx, my)) return true;

                // Click on empty track area → create a step at that tick
                if (isOver(mx, my, margin, tlY, trackW, TIMELINE_H)) {
                    boolean nearDot = false;
                    for (PhantasiaScriptData.StepData s : data.getSteps()) {
                        float t = total > 0 ? (float) s.tick / total : 0f;
                        int dotX = margin + (int) (t * trackW);
                        if (Math.abs(mx - dotX) < 14) {
                            nearDot = true;
                            break;
                        }
                    }
                    if (!nearDot) {
                        int newTick = total > 0 ? Math.round((float) (mx - margin) / trackW * total) : 0;
                        addStepAtTick(newTick);
                        return true;
                    }
                }
            }
            return false;
        }

        // 5. Scene click (only in scene area)
        int sceneBottom = this.height - BOTTOM_H;
        if (my < TOP_BAR_H || my >= sceneBottom) return false;

        return switch (mode) {
            case CAMERA -> false;
            case SELECT -> handleSelectClick(mx, my, btn);
            case ANNOTATE -> handleAnnotateClick(mx, my, btn);
        };
    }

    private boolean startLayerSliderDrag(double mx, double my) {
        PhantasiaScriptData.StepData s = step();
        if (!"layer".equals(s.show) && !"layers".equals(s.show)) return false;
        if (pattern == null) return false;

        int sliderX = 10;
        int sceneTop = TOP_BAR_H, sceneBottom = this.height - BOTTOM_H;
        int sliderH = sceneBottom - sceneTop - 24, sliderY = sceneTop + 12;
        int range = Math.max(1, pattern.maxY - pattern.minY);

        if ("layer".equals(s.show)) {
            float t = 1f - (float) (s.layer - pattern.minY) / range;
            int thumbY = sliderY + (int) (t * sliderH);
            if (isOver(mx, my, sliderX - 2, thumbY - 7, 18, 14)) {
                draggingLayer = true;
                draggingLayerMax = false;
                return true;
            }
        } else {
            float tMin = 1f - (float) (s.layerMin - pattern.minY) / range;
            float tMax = 1f - (float) (s.layerMax - pattern.minY) / range;
            int tyMin = sliderY + (int) (tMin * sliderH);
            int tyMax = sliderY + (int) (tMax * sliderH);
            if (isOver(mx, my, sliderX - 2, tyMin - 6, 18, 12)) {
                draggingLayer = true;
                draggingLayerMax = false;
                return true;
            }
            if (isOver(mx, my, sliderX - 2, tyMax - 6, 18, 12)) {
                draggingLayer = true;
                draggingLayerMax = true;
                return true;
            }
        }
        return false;
    }

    private boolean startTimelineDotDrag(double mx, double my) {
        int tlY = this.height - TIMELINE_H;
        int midY = tlY + TIMELINE_H / 2;
        int margin = 30, trackW = this.width - margin * 2;
        int total = computeTotalTicks();

        // Right-click on a dot → delete that step (with guard: must have >1 step)
        // This is handled in mouseClicked before this is called, so we only handle
        // left-button drag-start here.

        for (int i = 0; i < data.getSteps().size(); i++) {
            PhantasiaScriptData.StepData s = data.getSteps().get(i);
            float t = total > 0 ? (float) s.tick / total : 0f;
            int dotX = margin + (int) (t * trackW);
            if (isOver(mx, my, dotX - 9, midY - 9, 18, 18)) {
                draggingTimelineDot = i;
                dotDragMoved = false;
                dotDragStartMX = mx;
                selectStep(i);
                return true;
            }
        }
        return false;
    }

    private boolean handleSelectClick(double mx, double my, int btn) {
        if (hoveredWorldPos == null || pattern == null) return false;
        if (pattern.baseplatePositions.contains(hoveredWorldPos)) return false;

        BlockPos local = pattern.toLocal(hoveredWorldPos);
        if (local == null) return false;

        PhantasiaScriptData.StepData s = step();
        s.show = "pos";

        if (btn == 0) {
            // Left-click: toggle
            if (isInPositionList(local)) {
                s.positions.removeIf(
                        p -> p.length >= 3 && p[0] == local.getX() && p[1] == local.getY() && p[2] == local.getZ());
                selectedWorldPos.remove(hoveredWorldPos);
            } else {
                s.positions.add(new int[] { local.getX(), local.getY(), local.getZ() });
                selectedWorldPos.add(hoveredWorldPos);
            }
        } else if (btn == 1) {
            // Right-click: remove
            s.positions.removeIf(
                    p -> p.length >= 3 && p[0] == local.getX() && p[1] == local.getY() && p[2] == local.getZ());
            selectedWorldPos.remove(hoveredWorldPos);
        }

        dirty = true;
        rebuildVisibility();
        return true;
    }

    private boolean handleAnnotateClick(double mx, double my, int btn) {
        // Right-click: remove hovered marker
        if (btn == 1 && hoveredMistakeIndex >= 0) {
            data.getMistakes().remove(hoveredMistakeIndex);
            hoveredMistakeIndex = -1;
            dirty = true;
            return true;
        }

        if (btn != 0) return false;

        // Confirm pending if label box is shown
        if (pendingAnnotationLocalPos != null) {
            confirmAnnotation();
            return true;
        }

        if (hoveredWorldPos == null || pattern == null) return false;
        BlockPos local = pattern.toLocal(hoveredWorldPos);
        if (local == null) return false;

        pendingAnnotationLocalPos = local;
        annotationLabelBox.setValue("");
        annotationLabelBox.setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        // Layer slider drag
        if (draggingLayer && pattern != null) {
            int sceneTop = TOP_BAR_H, sceneBottom = this.height - BOTTOM_H;
            int sliderH = sceneBottom - sceneTop - 24, sliderY = sceneTop + 12;
            float t = 1f - Mth.clamp((float) (my - sliderY) / sliderH, 0f, 1f);
            int layer = pattern.minY + Math.round(t * (pattern.maxY - pattern.minY));
            PhantasiaScriptData.StepData s = step();
            if (draggingLayerMax) s.layerMax = Math.max(s.layerMin, layer);
            else if ("layers".equals(s.show)) s.layerMin = Math.min(layer, s.layerMax);
            else s.layer = layer;
            dirty = true;
            rebuildVisibility();
            return true;
        }

        // Timeline dot drag (adjusts tick value live, then auto-sorts on release)
        if (draggingTimelineDot >= 0 && draggingTimelineDot < data.getSteps().size()) {
            int margin = 30, trackW = this.width - margin * 2;
            int total = computeTotalTicks();
            float t = Mth.clamp((float) (mx - margin) / trackW, 0f, 1f);
            int newTick = Math.round(t * total);
            data.getSteps().get(draggingTimelineDot).tick = Math.max(0, newTick);
            if (Math.abs(mx - dotDragStartMX) > 3) dotDragMoved = true;
            populateInputsFromStep();
            dirty = true;
            return true;
        }

        // Camera drag (left-drag in CAMERA mode, or middle-drag in any mode)
        int sceneBottom = this.height - BOTTOM_H;
        if (my < TOP_BAR_H || my >= sceneBottom) return super.mouseDragged(mx, my, btn, dx, dy);

        if (btn == 2 || (btn == 0 && mode == Mode.CAMERA)) {
            WorldSceneRenderer r = sceneWidget.getRenderer();
            if (btn == 2) {
                // Pan
                Vector3f fwd = new Vector3f(r.getLookAt()).sub(r.getEyePos()).normalize();
                Vector3f rgt = new Vector3f(fwd).cross(r.getWorldUp()).normalize();
                Vector3f up = new Vector3f(rgt).cross(fwd).normalize();
                camTgtX += (rgt.x * -(float) dx + up.x * (float) dy) * 0.02f;
                camTgtY += (rgt.y * -(float) dx + up.y * (float) dy) * 0.02f;
                camTgtZ += (rgt.z * -(float) dx + up.z * (float) dy) * 0.02f;
            } else {
                camYaw = (camYaw + (float) dx * 0.5f) % 360f;
                camPitch = Mth.clamp(camPitch + (float) dy * 0.5f, -85f, -5f);
            }
            applyCamera();
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int sceneBottom = this.height - BOTTOM_H;
        if (my >= TOP_BAR_H && my < sceneBottom) {
            camDist = Mth.clamp(camDist * (delta > 0 ? 0.9f : 1.1f), 2f, 150f);
            applyCamera();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (draggingTimelineDot >= 0 && dotDragMoved) {
            // Auto-sort steps by tick so list order always matches timeline order.
            PhantasiaScriptData.StepData dragged = data.getSteps().get(draggingTimelineDot);
            data.getSteps().sort(Comparator.comparingInt(s -> s.tick));
            selectedStep = data.getSteps().indexOf(dragged);
            populateInputsFromStep();
            rebuildVisibility();
        }
        draggingTimelineDot = -1;
        dotDragMoved = false;
        draggingLayer = false;
        draggingLayerMax = false;
        reorderingStep = -1;
        reorderInsertAt = -1;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        super.mouseMoved(mx, my);
        if (sceneWidget != null) sceneWidget.mouseMoved(mx, my);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        // Annotation confirm / cancel
        if (pendingAnnotationLocalPos != null) {
            if (kc == 257) {
                confirmAnnotation();
                return true;
            }     // Enter
            if (kc == 256) {
                cancelAnnotation();
                return true;
            }     // Escape
        }
        // Global mistake add
        if (globalMistakeBox.isFocused() && kc == 257) {
            String v = globalMistakeBox.getValue().trim();
            if (!v.isEmpty()) {
                data.getGlobalMistakes().add(v);
                globalMistakeBox.setValue("");
                dirty = true;
            }
            return true;
        }
        if (kc == 256) {
            onClose();
            return true;
        }                   // Escape
        if (kc == 262) {
            selectStep(Math.min(selectedStep + 1, data.getSteps().size() - 1));
            return true;
        } // →
        if (kc == 263) {
            selectStep(Math.max(selectedStep - 1, 0));
            return true;
        }                          // ←

        // Ctrl+A / Ctrl+D in SELECT mode
        boolean ctrl = (mod & 2) != 0;
        if (mode == Mode.SELECT && ctrl) {
            if (kc == 65) {
                selectAllBlocks();
                return true;
            }  // A
            if (kc == 68) {
                deselectAll();
                return true;
            }  // D
        }

        return super.keyPressed(kc, sc, mod);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera
    // ─────────────────────────────────────────────────────────────────────────

    private void applyCamera() {
        if (sceneWidget == null) return;
        double yr = Math.toRadians(camYaw), pr = Math.toRadians(camPitch);
        float nx = (float) (Math.cos(pr) * Math.sin(yr));
        float ny = (float) Math.sin(pr);
        float nz = (float) (Math.cos(pr) * Math.cos(yr));
        sceneWidget.getRenderer().setCameraLookAt(
                new Vector3f(camTgtX + nx * camDist, camTgtY + ny * camDist, camTgtZ + nz * camDist),
                new Vector3f(camTgtX, camTgtY, camTgtZ),
                new Vector3f(0, 1, 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    private void setMode(Mode m) {
        mode = m;
        if (m == Mode.SELECT) {
            syncSelectedFromStep();
            if (!"pos".equals(step().show)) {
                step().show = "pos";
                dirty = true;
            }
        }
        pendingAnnotationLocalPos = null;
        rebuildVisibility();
    }

    private void togglePreview() {
        previewing = !previewing;
        previewTick = 0;
        previewAccum = 0f;
        if (!previewing) rebuildVisibility();
    }

    private void selectStep(int i) {
        selectedStep = Mth.clamp(i, 0, data.getSteps().size() - 1);
        populateInputsFromStep();
        if (mode == Mode.SELECT) syncSelectedFromStep();
        rebuildVisibility();
    }

    /**
     * Creates a new step at the given tick position and inserts it in sorted order.
     * Inherits the show mode of whichever existing step is active at that tick,
     * so the new step starts looking like its context rather than a blank reset.
     */
    private void addStepAtTick(int tick) {
        // Find the active step at this tick for show-mode inheritance
        String inheritedShow = "all";
        for (PhantasiaScriptData.StepData s : data.getSteps()) {
            if (s.tick <= tick) inheritedShow = s.show;
            else break;
        }
        PhantasiaScriptData.StepData s = new PhantasiaScriptData.StepData(tick, null);
        s.show = inheritedShow;

        // Insert in sorted position
        int insertAt = data.getSteps().size();
        for (int i = 0; i < data.getSteps().size(); i++) {
            if (data.getSteps().get(i).tick > tick) {
                insertAt = i;
                break;
            }
        }
        data.getSteps().add(insertAt, s);
        selectStep(insertAt);
        dirty = true;
    }

    private void addStep() {
        int lastTick = data.getSteps().isEmpty() ? 0 : data.getSteps().get(data.getSteps().size() - 1).tick + 60;
        PhantasiaScriptData.StepData s = new PhantasiaScriptData.StepData(lastTick, null);
        s.show = "all";
        data.getSteps().add(s);
        selectStep(data.getSteps().size() - 1);
        dirty = true;
    }

    private void deleteStep() {
        if (data.getSteps().size() <= 1) return;
        data.getSteps().remove(selectedStep);
        selectStep(Math.min(selectedStep, data.getSteps().size() - 1));
        dirty = true;
    }

    private void duplicateStep() {
        if (selectedStep < 0 || selectedStep >= data.getSteps().size()) return;
        PhantasiaScriptData.StepData copy = data.getSteps().get(selectedStep).copy();
        copy.tick += 60;
        data.getSteps().add(selectedStep + 1, copy);
        selectStep(selectedStep + 1);
        dirty = true;
    }

    private void moveStep(int from, int delta) {
        int to = from + delta;
        if (to < 0 || to >= data.getSteps().size()) return;
        Collections.swap(data.getSteps(), from, to);
        selectedStep = to;
        rebuildVisibility();
        dirty = true;
    }

    private void captureCamera() {
        PhantasiaScriptData.StepData s = step();
        if (s.camera == null) s.camera = new PhantasiaScriptData.CameraData();
        s.camera.yaw = camYaw;
        s.camera.pitch = camPitch;
        dirty = true;
    }

    private void confirmAnnotation() {
        if (pendingAnnotationLocalPos == null) return;
        String label = annotationLabelBox.getValue().trim();
        if (!label.isEmpty()) {
            BlockPos lp = pendingAnnotationLocalPos;
            String colorHex = String.format("%06X", MISTAKE_COLORS[selectedMistakeColor] & 0xFFFFFF);
            // Remove existing marker at this pos if any
            data.getMistakes().removeIf(m -> m.x == lp.getX() && m.y == lp.getY() && m.z == lp.getZ());
            data.getMistakes()
                    .add(new PhantasiaScriptData.MistakeData(lp.getX(), lp.getY(), lp.getZ(), label, colorHex));
            dirty = true;
        }
        pendingAnnotationLocalPos = null;
        annotationLabelBox.setValue("");
    }

    private void cancelAnnotation() {
        pendingAnnotationLocalPos = null;
        annotationLabelBox.setValue("");
    }

    private void selectAllBlocks() {
        if (pattern == null) return;
        PhantasiaScriptData.StepData s = step();
        s.positions.clear();
        selectedWorldPos.clear();
        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            if (pattern.baseplatePositions.contains(e.getValue())) continue;
            s.positions.add(new int[] { e.getKey().getX(), e.getKey().getY(), e.getKey().getZ() });
            selectedWorldPos.add(e.getValue());
        }
        dirty = true;
        rebuildVisibility();
    }

    private void deselectAll() {
        step().positions.clear();
        selectedWorldPos.clear();
        dirty = true;
        rebuildVisibility();
    }

    private void save() {
        PhantasiaScriptLoader.save(machineId, data);
        dirty = false;
        if (parentScene != null) parentScene.reloadScript();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int computeTotalTicks() {
        return data.getSteps().isEmpty() ? 60 : data.getSteps().get(data.getSteps().size() - 1).tick + 60;
    }

    private void syncSelectedFromStep() {
        selectedWorldPos.clear();
        if (pattern == null) return;
        for (int[] xyz : step().positions) {
            if (xyz.length < 3) continue;
            BlockPos world = pattern.toWorld(new BlockPos(xyz[0], xyz[1], xyz[2]));
            if (world != null) selectedWorldPos.add(world);
        }
    }

    private boolean isInPositionList(BlockPos local) {
        for (int[] p : step().positions)
            if (p.length >= 3 && p[0] == local.getX() && p[1] == local.getY() && p[2] == local.getZ()) return true;
        return false;
    }

    private void populateInputsFromStep() {
        if (captionBox == null) return;
        PhantasiaScriptData.StepData s = step();
        captionBox.setValue(s.caption != null ? s.caption : "");
        tickBox.setValue(String.valueOf(s.tick));
        hideLayerBox.setValue(s.hideLayer >= 0 ? String.valueOf(s.hideLayer) : "");
        hidePosBox.setValue(serializePosList(s.hidePositions));
        if (fakeRecipeBox != null)
            fakeRecipeBox.setValue(s.fakeRecipeId != null ? s.fakeRecipeId : "");
    }

    private PhantasiaScriptData.StepData step() {
        if (selectedStep >= 0 && selectedStep < data.getSteps().size())
            return data.getSteps().get(selectedStep);
        return new PhantasiaScriptData.StepData();
    }

    private float[] projectToScreen(float wx, float wy, float wz,
                                    Vector3f eye, Vector3f fwd, Vector3f rgt, Vector3f upv, float fov) {
        Vector3f toP = new Vector3f(wx - eye.x, wy - eye.y, wz - eye.z);
        float depth = toP.dot(fwd);
        if (depth < 0.3f) return null;
        float sx = this.width / 2f + (toP.dot(rgt) / depth) * fov;
        float sy = this.height / 2f - (toP.dot(upv) / depth) * fov;
        return new float[] { sx, sy, depth };
    }

    private void drawBanner(GuiGraphics g, String text, int y, int accentColor) {
        int tw = font.width(text) + 20;
        int tx = (this.width - tw) / 2;
        g.fill(tx, y, tx + tw, y + 16, 0xBB0C0C1A);
        g.fill(tx, y, tx + tw, y + 1, accentColor);
        g.drawString(font, text, tx + 10, y + 4, C_DIM, false);
    }

    private void btn(GuiGraphics g, int mx, int my, int x, int y, int w, int h,
                     String label, int base, Runnable action) {
        boolean hov = isOver(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : base);
        if (hov) {
            g.fill(x, y, x + w, y + 1, C_ACCENT);
            g.fill(x, y + h - 1, x + w, y + h, C_ACCENT);
        }
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
        btns.add(new Btn(x, y, w, h, action));
    }

    private void placeBox(EditBox box, int x, int y, int w, int h) {
        box.setX(x);
        box.setY(y);
        box.setWidth(w);
        box.setHeight(h);
        box.visible = true;
        box.active = true;
    }

    private void hideAllInputs() {
        for (var box : List.of(captionBox, tickBox, hideLayerBox, hidePosBox,
                annotationLabelBox, globalMistakeBox, fakeRecipeBox)) {
            if (box != null) {
                box.visible = false;
                box.active = false;
            }
        }
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) s = s.substring(0, s.length() - 2) + "\u2026";
        return s;
    }

    private static List<int[]> parsePosList(String raw) {
        List<int[]> r = new ArrayList<>();
        if (raw == null || raw.isBlank()) return r;
        for (String e : raw.split(";")) {
            String[] p = e.trim().split(",");
            if (p.length >= 3) try {
                r.add(new int[] { Integer.parseInt(p[0].trim()),
                        Integer.parseInt(p[1].trim()),
                        Integer.parseInt(p[2].trim()) });
            } catch (NumberFormatException ignored) {}
        }
        return r;
    }

    private static String serializePosList(List<int[]> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int[] p : list) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(p[0]).append(',').append(p[1]).append(',').append(p[2]);
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        if (parentScene != null) parentScene.reloadScript();
        Minecraft.getInstance().setScreen(parentScene);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
