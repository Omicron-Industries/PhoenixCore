package net.phoenix.core.integration.phantasia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;

import java.util.*;

/**
 * PhantasiaFootprintScreen
 *
 * 2D top-down grid view of the multiblock at a given Y layer.
 * This is a fully self-contained screen — it does NOT touch the SceneWidget
 * camera at all, which was causing the "camera moves weirdly" bug.
 *
 * Fixed issues:
 * - Uses local coordinates (0-based) for grid drawing, not world coordinates.
 * - gridOriginX/Y is computed so the grid is centred in the non-panel area.
 * - Screen-to-local conversion uses floorDiv on local coords, not world coords.
 * - Layer selector shows ALL layers with blocks, not just min/max.
 * - Does not call any renderer methods — purely 2D GuiGraphics drawing.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaFootprintScreen extends Screen {

    private final PhantasiaScript script;
    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF080810;
    private static final int C_PANEL = 0xEE0C0C1A;
    private static final int C_ACCENT = 0xFF4FC3F7;
    private static final int C_BTN = 0xBB151528;
    private static final int C_BTN_HOV = 0xBB1A2840;
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF667788;
    private static final int C_WARN = 0xFFFFB74D;
    private static final int C_GREEN = 0xFF66BB6A;
    private static final int C_GRID_LINE = 0xFF1E2D3C;
    private static final int C_CONTROLLER = 0xFF4FC3F7; // cyan
    private static final int C_BE = 0xFFFFB74D; // amber
    private static final int C_NORMAL = 0xFF3A506A; // slate
    private static final int C_HOVER = 0xAAFFFFFF;

    private static final int PANEL_W = 164;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    // Inside PhantasiaFootprintScreen class
    @Getter
    private final PhantasiaLoadedPattern pattern;

    // All Y values that actually have blocks, sorted ascending
    private final List<Integer> layers;
    private int layerIndex; // index into layers list

    // Grid geometry — computed in recalcGrid(), purely in local coords
    private int cellSize;
    private int gridPixelX; // screen X of local X=0 cell left edge
    private int gridPixelY; // screen Y of local Z=0 cell top edge
    private int localMinX, localMinZ; // lowest local coords present on any layer

    // Interaction
    private BlockPos hoveredLocal = null; // local pos (Y = current layer)
    private BlockPos inspectedLocal = null; // local pos of right-clicked cell

    // Layer block counts (for side panel) — computed once per layer change
    private final Map<Integer, Map<String, Integer>> layerBlockCounts = new HashMap<>();

    // ──────────────────────────────────────────────────────────────────────────

    public PhantasiaFootprintScreen(PhantasiaLoadedPattern pattern, Screen parent, PhantasiaScript script) {
        super(Component.literal("Footprint"));
        this.parent = parent;
        this.pattern = pattern;
        this.script = script;

        // Collect unique Y values that have at least one machine block
        Set<Integer> ys = new TreeSet<>();
        for (BlockPos lp : pattern.localToWorld.keySet()) ys.add(lp.getY());
        this.layers = new ArrayList<>(ys);
        this.layerIndex = 0; // start at bottom
    }

    @Override
    protected void init() {
        super.init();
        recalcGrid();
    }

    private int currentLayerY() {
        return layers.isEmpty() ? 0 : layers.get(layerIndex);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Grid geometry — purely local coords, no world pos involved
    // ──────────────────────────────────────────────────────────────────────────

    private void recalcGrid() {
        if (pattern.localToWorld.isEmpty()) return;

        // Find bounding box of ALL local positions across ALL layers
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos lp : pattern.localToWorld.keySet()) {
            if (lp.getX() < minX) minX = lp.getX();
            if (lp.getX() > maxX) maxX = lp.getX();
            if (lp.getZ() < minZ) minZ = lp.getZ();
            if (lp.getZ() > maxZ) maxZ = lp.getZ();
        }
        localMinX = minX;
        localMinZ = minZ;

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;

        // Available area: full screen minus right panel, title bar, footer
        int availW = this.width - PANEL_W - 16;
        int availH = this.height - 50;

        // Cell size: fit the grid in available area, clamp to sensible range
        cellSize = Math.max(6, Math.min(40,
                Math.min(availW / Math.max(1, spanX),
                        availH / Math.max(1, spanZ))));

        int gridW = spanX * cellSize;
        int gridH = spanZ * cellSize;

        // Centre the grid in the available area
        // gridPixelX is the screen X of the LEFT edge of the local-minX column
        gridPixelX = 8 + (availW - gridW) / 2;
        gridPixelY = 32 + (availH - gridH) / 2;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Screen → local coord conversion
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns the local BlockPos at screen coords (sx, sy) for currentLayerY(), or null. */
    private BlockPos screenToLocal(int sx, int sy) {
        if (cellSize <= 0) return null;
        // Convert screen → local grid indices
        int localX = localMinX + Math.floorDiv(sx - gridPixelX, cellSize);
        int localZ = localMinZ + Math.floorDiv(sy - gridPixelY, cellSize);
        BlockPos candidate = new BlockPos(localX, currentLayerY(), localZ);
        return pattern.localToWorld.containsKey(candidate) ? candidate : null;
    }

    /** Returns the screen X (left edge) of local X index. */
    private int localXToScreen(int localX) {
        return gridPixelX + (localX - localMinX) * cellSize;
    }

    /** Returns the screen Y (top edge) of local Z index. */
    private int localZToScreen(int localZ) {
        return gridPixelY + (localZ - localMinZ) * cellSize;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rendering
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, C_BG);

        // Title bar
        g.fill(0, 0, this.width, 23, 0xCC0A0A14);
        g.fill(0, 22, this.width, 23, C_ACCENT);
        String title = "Footprint  —  Layer Y = " + currentLayerY() + "  (" + (layerIndex + 1) + " / " + layers.size() +
                ")";
        g.drawCenteredString(font, title, (this.width - PANEL_W) / 2, 7, C_ACCENT);

        // Update hovered cell
        hoveredLocal = screenToLocal(mx, my);

        renderGrid(g, mx, my);
        renderSidePanel(g, mx, my);
        renderLegend(g);
    }

    private void renderGrid(GuiGraphics g, int mx, int my) {
        int layerY = currentLayerY();
        Set<Integer> drawnX = new HashSet<>(), drawnZ = new HashSet<>();

        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            BlockPos lp = e.getKey(); // Local coordinate (0, 0, 0)
            if (lp.getY() != layerY) continue;

            BlockPos wp = e.getValue(); // World coordinate
            int sx = localXToScreen(lp.getX());
            int sz = localZToScreen(lp.getZ());

            // --- 1. COLOR CALCULATION (Heatmap Support) ---
            int color = cellColor(wp);

            // SECOND: Check heatmap (This must happen AFTER base color so it can override it)
            if (showHeatmap && script != null) {
                for (PhantasiaScript.HeatmapTier tier : script.getHeatmapTiers()) {
                    if (tier.matcher().test(lp)) {
                        color = tier.color(); // This replaces the gray with Red
                        break;
                    }
                }
            }

            // THIRD: Fill the cell
            g.fill(sx + 1, sz + 1, sx + cellSize - 1, sz + cellSize - 1, color);

            // --- 3. DRAW BORDERS & HIGHLIGHTS ---
            // Basic Grid Lines
            g.fill(sx, sz, sx + cellSize, sz + 1, C_GRID_LINE);
            g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_GRID_LINE);
            g.fill(sx, sz, sx + 1, sz + cellSize, C_GRID_LINE);
            g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_GRID_LINE);

            // Hover highlight (Light Blue)
            if (lp.equals(hoveredLocal)) {
                g.fill(sx, sz, sx + cellSize, sz + 1, C_HOVER);
                g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_HOVER);
                g.fill(sx, sz, sx + 1, sz + cellSize, C_HOVER);
                g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_HOVER);
            }

            // Inspect highlight (Yellow/Accent)
            if (lp.equals(inspectedLocal)) {
                g.fill(sx, sz, sx + cellSize, sz + 1, C_ACCENT);
                g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_ACCENT);
                g.fill(sx, sz, sx + 1, sz + cellSize, C_ACCENT);
                g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_ACCENT);
            }

            // --- 4. ABBREVIATION LABELS (Tiered by Cell Size) ---
            if (cellSize >= 10 && PhantasiaSceneScreen.SHARED_LEVEL != null) {
                try {
                    BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                    if (!bs.isAir()) {
                        String name = bs.getBlock().getName().getString();
                        String displayStr;

                        if (cellSize < 16) {
                            // Very small: 1 letter
                            displayStr = name.substring(0, 1).toUpperCase();
                        } else if (cellSize < 32) {
                            // Medium: 1-2 letters (Triggers acronyms)
                            displayStr = abbreviate(name);
                            if (displayStr.length() > 2) displayStr = displayStr.substring(0, 1);
                        } else {
                            // Large: Full acronyms
                            displayStr = abbreviate(name);
                        }

                        // Final fit check
                        if (font.width(displayStr) > cellSize - 2) {
                            displayStr = displayStr.substring(0, 1);
                        }

                        g.drawString(font, displayStr,
                                sx + (cellSize - font.width(displayStr)) / 2,
                                sz + (cellSize - 8) / 2, 0xFFFFFFFF, false);
                    }
                } catch (Exception ignored) {}
            }

            // --- 5. AXIS LABELS ---
            if (cellSize >= 12) {
                if (!drawnX.contains(lp.getX())) {
                    g.drawString(font, String.valueOf(lp.getX()), sx + 1, 25, C_DIM, false);
                    drawnX.add(lp.getX());
                }
                if (!drawnZ.contains(lp.getZ())) {
                    g.drawString(font, String.valueOf(lp.getZ()), 1, sz + (cellSize - 8) / 2, C_DIM, false);
                    drawnZ.add(lp.getZ());
                }
            }
        }

        // --- 6. TOOLTIP (Drawn last to stay on top) ---
        if (hoveredLocal != null) {
            BlockPos wp = pattern.toWorld(hoveredLocal);
            if (wp != null && PhantasiaSceneScreen.SHARED_LEVEL != null) {
                try {
                    BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                    if (!bs.isAir()) g.renderTooltip(font, bs.getBlock().getName(), mx, my);
                } catch (Exception ignored) {}
            }
        }
    }

    private boolean showHeatmap = false;

    private int cellColor(BlockPos worldPos) {
        BlockPos localPos = pattern.toLocal(worldPos);

        // If heatmap is ON, override standard colors with tier colors
        if (showHeatmap && localPos != null) {
            for (PhantasiaScript.HeatmapTier tier : pattern.getScript().getHeatmapTiers()) {
                if (tier.matcher().test(localPos)) {
                    return tier.color();
                }
            }
            return 0xFF222222; // Dim out blocks not in a heatmap tier
        }

        // Standard colors
        if (worldPos.equals(pattern.controllerWorldPos)) return C_CONTROLLER;
        if (pattern.hasBlockEntity(worldPos)) return C_BE;
        return C_NORMAL;
    }

    private void renderSidePanel(GuiGraphics g, int mx, int my) {
        int px = this.width - PANEL_W;
        g.fill(px, 0, this.width, this.height, C_PANEL);
        g.fill(px, 0, px + 2, this.height, C_ACCENT);

        int y = 28; // The only Y coordinate we will use
        int hw = (PANEL_W - 18) / 2;

        // 1. Layer navigation buttons
        g.drawString(font, "Layer (Y):", px + 10, y, C_DIM, false);
        y += 11;
        drawBtn(g, mx, my, px + 8, y, hw, 15, "\u25BC Prev", isOver(mx, my, px + 8, y, hw, 15), C_BTN);
        drawBtn(g, mx, my, px + 10 + hw, y, hw, 15, "Next \u25B2", isOver(mx, my, px + 10 + hw, y, hw, 15), C_BTN);
        y += 19;

        // 2. Jump To Pills
        g.drawString(font, "Jump to:", px + 10, y, C_DIM, false);
        y += 11;
        int pillX = px + 8;
        // We start the pills at the current 'y'.
        // If they wrap, they will increment the SAME 'y' variable.
        for (int i = 0; i < layers.size(); i++) {
            if (pillX + 28 > this.width - 6) {
                pillX = px + 8;
                y += 15; // Push the rolling Y down for the next row
            }
            boolean active = i == layerIndex;
            boolean hov = isOver(mx, my, pillX, y, 28, 13);

            g.fill(pillX, y, pillX + 28, y + 13, active ? C_ACCENT : (hov ? C_BTN_HOV : C_BTN));
            String label = String.valueOf(layers.get(i));
            g.drawString(font, label, pillX + (28 - font.width(label)) / 2, y + 3, active ? C_BG : C_TEXT, false);

            pillX += 30;
        }
        y += 18; // Move past the last row of pills
        g.fill(px + 6, y, this.width - 4, y + 1, 0x33FFFFFF);
        y += 8;

        // 3. Heatmap Toggle
        // Now positioned relative to the rolling Y
        drawBtn(g, mx, my, px + 8, y, PANEL_W - 16, 15, "Heatmap: " + (showHeatmap ? "ON" : "OFF"),
                isOver(mx, my, px + 8, y, PANEL_W - 16, 15), showHeatmap ? C_ACCENT : C_BTN);
        y += 20;

        // 4. Inspect panel
        if (inspectedLocal != null) {
            BlockPos wp = pattern.toWorld(inspectedLocal);
            if (wp != null && PhantasiaSceneScreen.SHARED_LEVEL != null) {
                try {
                    BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                    if (!bs.isAir()) {
                        g.drawString(font, "Inspecting:", px + 10, y, C_ACCENT, false);
                        y += 11;
                        g.drawString(font, trunc(bs.getBlock().getName().getString(), PANEL_W - 18), px + 10, y, C_TEXT,
                                false);
                        y += 10;
                        g.drawString(font, "X=" + inspectedLocal.getX() + " Z=" + inspectedLocal.getZ(), px + 10, y,
                                C_DIM, false);
                        y += 10;
                        if (pattern.hasBlockEntity(wp)) {
                            g.drawString(font, "\u26A1 Block Entity", px + 10, y, C_WARN, false);
                            y += 10;
                        }
                        if (wp.equals(pattern.controllerWorldPos)) {
                            g.drawString(font, "\u2605 Controller", px + 10, y, C_ACCENT, false);
                            y += 10;
                        }
                        y += 3;
                        boolean ch = isOver(mx, my, px + 8, y, PANEL_W - 16, 13);
                        drawBtn(g, mx, my, px + 8, y, PANEL_W - 16, 13, "Clear", ch, C_BTN);
                        y += 18;
                    }
                } catch (Exception ignored) {}
            }
            g.fill(px + 6, y, this.width - 4, y + 1, 0x33FFFFFF);
            y += 8;
        }

        // 5. Layer block counts
        int layerY = currentLayerY();
        Map<String, Integer> counts = layerBlockCounts.computeIfAbsent(layerY, this::computeLayerCounts);
        g.drawString(font, "Layer blocks:", px + 10, y, C_DIM, false);
        y += 11;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (y >= this.height - 35) { // Leave room for Back button
                g.drawString(font, "...", px + 10, y, C_DIM, false);
                break;
            }
            g.drawString(font, e.getValue() + "\u00D7 " + trunc(e.getKey(), PANEL_W - 30), px + 10, y, C_TEXT, false);
            y += 10;
        }

        // 6. Back button (Fixed to bottom)
        drawBtn(g, mx, my, px + 8, this.height - 24, PANEL_W - 16, 18, "\u2190 Back",
                isOver(mx, my, px + 8, this.height - 24, PANEL_W - 16, 18), C_BTN);
    }

    private Map<String, Integer> computeLayerCounts(int layerY) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (PhantasiaSceneScreen.SHARED_LEVEL == null) return counts;
        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            if (e.getKey().getY() != layerY) continue;
            try {
                BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(e.getValue());
                if (bs.isAir()) continue;
                counts.merge(bs.getBlock().getName().getString(), 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        // Sort by count desc
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : sorted) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private void renderLegend(GuiGraphics g) {
        int ly = this.height - 13, x = 8;
        g.fill(0, ly - 3, this.width - PANEL_W, this.height, 0xBB060610);
        x = legendDot(g, x, ly, C_CONTROLLER, "Controller");
        x = legendDot(g, x, ly, C_BE, "Block Entity");
        legendDot(g, x, ly, C_NORMAL, "Block");
    }

    private int legendDot(GuiGraphics g, int x, int y, int color, String label) {
        g.fill(x, y, x + 8, y + 8, color);
        g.drawString(font, label, x + 10, y, C_DIM, false);
        return x + 12 + font.width(label) + 6;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Input
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = this.width - PANEL_W;
        int hw = (PANEL_W - 18) / 2;

        // Start 'y' at 28, exactly like renderSidePanel
        int y = 28;

        // 1. Layer navigation buttons
        y += 11; // Skip the "Layer (Y):" text height
        if (isOver((int) mx, (int) my, px + 8, y, hw, 15)) {
            cycleLayer(-1);
            return true;
        }
        if (isOver((int) mx, (int) my, px + 10 + hw, y, hw, 15)) {
            cycleLayer(+1);
            return true;
        }
        y += 19; // Skip past buttons

        // 2. Layer Pills
        y += 11; // Skip "Jump to:" text
        int pillX = px + 8;
        for (int i = 0; i < layers.size(); i++) {
            if (pillX + 28 > this.width - 6) {
                pillX = px + 8;
                y += 15; // Push Y down for the next row
            }
            if (isOver((int) mx, (int) my, pillX, y, 28, 13)) {
                layerIndex = i;
                inspectedLocal = null;
                return true;
            }
            pillX += 30;
        }
        y += 18; // Move past the pills
        y += 8;  // Skip the separator line

        // 3. Heatmap Toggle
        if (isOver((int) mx, (int) my, px + 8, y, PANEL_W - 16, 15)) {
            this.showHeatmap = !this.showHeatmap;
            return true;
        }
        y += 20; // Move past Heatmap button

        // 4. Inspect / Clear Button
        if (inspectedLocal != null) {
            // We have to simulate the text drawing to find where the Clear button ended up
            y += 11; // "Inspecting:"
            y += 10; // Name
            y += 10; // Coords
            BlockPos wp = pattern.toWorld(inspectedLocal);
            if (wp != null) {
                if (pattern.hasBlockEntity(wp)) y += 10;
                if (wp.equals(pattern.controllerWorldPos)) y += 10;
            }
            y += 3;

            // Hit-test for the "Clear" button
            if (isOver((int) mx, (int) my, px + 8, y, PANEL_W - 16, 13)) {
                inspectedLocal = null;
                return true;
            }
            y += 18;
            y += 8; // Separator
        }

        // 5. Back Button (Fixed to bottom, no rolling Y needed)
        if (isOver((int) mx, (int) my, px + 8, this.height - 24, PANEL_W - 16, 18)) {
            onClose();
            return true;
        }

        // 6. Grid Interaction (Left of the panel)
        if ((int) mx < px) {
            BlockPos lp = screenToLocal((int) mx, (int) my);
            if (lp != null) {
                if (btn == 0) inspectedLocal = lp;
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < this.width - PANEL_W) {
            cycleLayer(delta > 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == 256) {
            onClose();
            return true;
        }
        if (kc == 265) {
            cycleLayer(+1);
            return true;
        } // UP arrow
        if (kc == 264) {
            cycleLayer(-1);
            return true;
        } // DOWN arrow
        return super.keyPressed(kc, sc, mod);
    }

    private void cycleLayer(int delta) {
        if (layers.isEmpty()) return;
        layerIndex = Math.max(0, Math.min(layers.size() - 1, layerIndex + delta));
        inspectedLocal = null; // clear inspect when changing layer
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean hov,
                         int color) {
        // Use the passed color for the background, or a darker version if not hovered
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : color);

        if (hov) {
            g.fill(x, y, x + w, y + 1, C_ACCENT);
            g.fill(x, y + h - 1, x + w, y + h, C_ACCENT);
        }

        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) s = s.substring(0, s.length() - 2) + "\u2026";
        return s;
    }

    private String abbreviate(String name) {
        String[] words = name.split("[\\s_]+");
        if (words.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String w : words) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)));
            return sb.toString();
        }
        return name.length() > 4 ? name.substring(0, 4) : name;
    }
}
