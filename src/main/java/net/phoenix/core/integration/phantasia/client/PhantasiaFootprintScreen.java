package net.phoenix.core.integration.phantasia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.phantasia.PhantasiaLoadedPattern;
import net.phoenix.core.integration.phantasia.PhantasiaScript;

import lombok.Getter;

import java.util.*;

/**
 * PhantasiaFootprintScreen
 *
 * 2D top-down grid view of the multiblock at a given Y layer.
 * Purely 2D — does not touch the SceneWidget camera.
 *
 * FIX (B5): onClose() calls applyVisibility() on the parent PhantasiaSceneScreen
 * so any filter state set while here is immediately reflected on return.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaFootprintScreen extends Screen {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF080810;
    private static final int C_PANEL = 0xEE0C0C1A;
    private static final int C_ACCENT = 0xFF4FC3F7;
    private static final int C_BTN = 0xBB151528;
    private static final int C_BTN_HOV = 0xBB1A2840;
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF667788;
    private static final int C_WARN = 0xFFFFB74D;
    private static final int C_GRID_LINE = 0xFF1E2D3C;
    private static final int C_CONTROLLER = 0xFF4FC3F7;
    private static final int C_BE = 0xFFFFB74D;
    private static final int C_NORMAL = 0xFF3A506A;
    private static final int C_HOVER = 0xAAFFFFFF;

    private static final int PANEL_W = 164;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    @Getter
    private final PhantasiaLoadedPattern pattern;
    private final PhantasiaScript script;

    /** All Y values that have at least one block, sorted ascending. */
    private final List<Integer> layers;
    private int layerIndex = 0;

    // Grid geometry — computed in recalcGrid(), purely local coords
    private int cellSize;
    private int gridPixelX;
    private int gridPixelY;
    private int localMinX, localMinZ;

    // Interaction
    private BlockPos hoveredLocal = null;
    private BlockPos inspectedLocal = null;

    private boolean showHeatmap = false;

    // Layer block counts — computed lazily per layer
    private final Map<Integer, Map<String, Integer>> layerBlockCounts = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public PhantasiaFootprintScreen(PhantasiaLoadedPattern pattern, Screen parent, PhantasiaScript script) {
        super(Component.literal("Footprint"));
        this.parent = parent;
        this.pattern = pattern;
        this.script = script;

        Set<Integer> ys = new TreeSet<>();
        for (BlockPos lp : pattern.localToWorld.keySet()) ys.add(lp.getY());
        this.layers = new ArrayList<>(ys);
    }

    @Override
    protected void init() {
        super.init();
        recalcGrid();
    }

    private int currentLayerY() {
        return layers.isEmpty() ? 0 : layers.get(layerIndex);
    }

    // ── Grid geometry ─────────────────────────────────────────────────────────

    private void recalcGrid() {
        if (pattern.localToWorld.isEmpty()) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos lp : pattern.localToWorld.keySet()) {
            minX = Math.min(minX, lp.getX());
            maxX = Math.max(maxX, lp.getX());
            minZ = Math.min(minZ, lp.getZ());
            maxZ = Math.max(maxZ, lp.getZ());
        }
        localMinX = minX;
        localMinZ = minZ;

        int spanX = maxX - minX + 1;
        int spanZ = maxZ - minZ + 1;
        int availW = this.width - PANEL_W - 16;
        int availH = this.height - 50;

        cellSize = Math.max(6, Math.min(40,
                Math.min(availW / Math.max(1, spanX),
                        availH / Math.max(1, spanZ))));

        int gridW = spanX * cellSize;
        int gridH = spanZ * cellSize;
        gridPixelX = 8 + (availW - gridW) / 2;
        gridPixelY = 32 + (availH - gridH) / 2;
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private BlockPos screenToLocal(int sx, int sy) {
        if (cellSize <= 0) return null;
        int localX = localMinX + Math.floorDiv(sx - gridPixelX, cellSize);
        int localZ = localMinZ + Math.floorDiv(sy - gridPixelY, cellSize);
        BlockPos candidate = new BlockPos(localX, currentLayerY(), localZ);
        return pattern.localToWorld.containsKey(candidate) ? candidate : null;
    }

    private int localXToScreen(int lx) {
        return gridPixelX + (lx - localMinX) * cellSize;
    }

    private int localZToScreen(int lz) {
        return gridPixelY + (lz - localMinZ) * cellSize;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, C_BG);

        g.fill(0, 0, this.width, 23, 0xCC0A0A14);
        g.fill(0, 22, this.width, 23, C_ACCENT);
        String title = "Footprint  —  Layer Y = " + currentLayerY() + "  (" + (layerIndex + 1) + " / " + layers.size() +
                ")";
        g.drawCenteredString(font, title, (this.width - PANEL_W) / 2, 7, C_ACCENT);

        hoveredLocal = screenToLocal(mx, my);

        renderGrid(g, mx, my);
        renderSidePanel(g, mx, my);
        renderLegend(g);
    }

    private void renderGrid(GuiGraphics g, int mx, int my) {
        int layerY = currentLayerY();
        Set<Integer> drawnX = new HashSet<>(), drawnZ = new HashSet<>();

        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            BlockPos lp = e.getKey();
            if (lp.getY() != layerY) continue;

            BlockPos wp = e.getValue();
            int sx = localXToScreen(lp.getX());
            int sz = localZToScreen(lp.getZ());

            // Cell color — base then heatmap override
            int color = cellColor(wp, lp);
            g.fill(sx + 1, sz + 1, sx + cellSize - 1, sz + cellSize - 1, color);

            // Grid lines
            g.fill(sx, sz, sx + cellSize, sz + 1, C_GRID_LINE);
            g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_GRID_LINE);
            g.fill(sx, sz, sx + 1, sz + cellSize, C_GRID_LINE);
            g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_GRID_LINE);

            // Hover border
            if (lp.equals(hoveredLocal)) {
                g.fill(sx, sz, sx + cellSize, sz + 1, C_HOVER);
                g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_HOVER);
                g.fill(sx, sz, sx + 1, sz + cellSize, C_HOVER);
                g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_HOVER);
            }

            // Inspect border
            if (lp.equals(inspectedLocal)) {
                g.fill(sx, sz, sx + cellSize, sz + 1, C_ACCENT);
                g.fill(sx, sz + cellSize - 1, sx + cellSize, sz + cellSize, C_ACCENT);
                g.fill(sx, sz, sx + 1, sz + cellSize, C_ACCENT);
                g.fill(sx + cellSize - 1, sz, sx + cellSize, sz + cellSize, C_ACCENT);
            }

            // Abbreviation labels
            if (cellSize >= 10 && PhantasiaSceneScreen.SHARED_LEVEL != null) {
                try {
                    BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                    if (!bs.isAir()) {
                        String name = bs.getBlock().getName().getString();
                        String disp = cellSize < 16 ? name.substring(0, 1).toUpperCase() :
                                cellSize < 32 ? abbreviate(name).substring(0, Math.min(1, abbreviate(name).length())) :
                                        abbreviate(name);
                        if (font.width(disp) > cellSize - 2) disp = disp.substring(0, 1);
                        g.drawString(font, disp,
                                sx + (cellSize - font.width(disp)) / 2,
                                sz + (cellSize - 8) / 2, 0xFFFFFFFF, false);
                    }
                } catch (Exception ignored) {}
            }

            // Axis labels
            if (cellSize >= 12) {
                if (drawnX.add(lp.getX()))
                    g.drawString(font, String.valueOf(lp.getX()), sx + 1, 25, C_DIM, false);
                if (drawnZ.add(lp.getZ()))
                    g.drawString(font, String.valueOf(lp.getZ()), 1, sz + (cellSize - 8) / 2, C_DIM, false);
            }
        }

        // Tooltip
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

    private int cellColor(BlockPos worldPos, BlockPos localPos) {
        if (showHeatmap && script != null) {
            for (PhantasiaScript.HeatmapTier tier : script.getHeatmapTiers()) {
                if (tier.matcher().test(localPos)) return tier.color();
            }
            return 0xFF222222;
        }
        if (worldPos.equals(pattern.controllerWorldPos)) return C_CONTROLLER;
        if (pattern.hasBlockEntity(worldPos)) return C_BE;
        return C_NORMAL;
    }

    private void renderSidePanel(GuiGraphics g, int mx, int my) {
        int px = this.width - PANEL_W;
        g.fill(px, 0, this.width, this.height, C_PANEL);
        g.fill(px, 0, px + 2, this.height, C_ACCENT);

        int y = 28;
        int hw = (PANEL_W - 18) / 2;

        // Layer navigation
        g.drawString(font, "Layer (Y):", px + 10, y, C_DIM, false);
        y += 11;
        drawBtn(g, mx, my, px + 8, y, hw, 15, "\u25BC Prev", isOver(mx, my, px + 8, y, hw, 15), C_BTN);
        drawBtn(g, mx, my, px + 10 + hw, y, hw, 15, "Next \u25B2", isOver(mx, my, px + 10 + hw, y, hw, 15), C_BTN);
        y += 19;

        // Layer pills
        g.drawString(font, "Jump to:", px + 10, y, C_DIM, false);
        y += 11;
        int pillX = px + 8;
        for (int i = 0; i < layers.size(); i++) {
            if (pillX + 28 > this.width - 6) {
                pillX = px + 8;
                y += 15;
            }
            boolean active = i == layerIndex;
            boolean hov = isOver(mx, my, pillX, y, 28, 13);
            g.fill(pillX, y, pillX + 28, y + 13, active ? C_ACCENT : (hov ? C_BTN_HOV : C_BTN));
            String lbl = String.valueOf(layers.get(i));
            g.drawString(font, lbl, pillX + (28 - font.width(lbl)) / 2, y + 3, active ? C_BG : C_TEXT, false);
            pillX += 30;
        }
        y += 18;

        g.fill(px + 6, y, this.width - 4, y + 1, 0x33FFFFFF);
        y += 8;

        // Heatmap toggle
        drawBtn(g, mx, my, px + 8, y, PANEL_W - 16, 15,
                "Heatmap: " + (showHeatmap ? "ON" : "OFF"),
                isOver(mx, my, px + 8, y, PANEL_W - 16, 15),
                showHeatmap ? C_ACCENT : C_BTN);
        y += 20;

        // Inspect panel
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

        // Layer block counts
        int layerY = currentLayerY();
        Map<String, Integer> counts = layerBlockCounts.computeIfAbsent(layerY, this::computeLayerCounts);
        g.drawString(font, "Layer blocks:", px + 10, y, C_DIM, false);
        y += 11;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (y >= this.height - 35) {
                g.drawString(font, "...", px + 10, y, C_DIM, false);
                break;
            }
            g.drawString(font, e.getValue() + "\u00D7 " + trunc(e.getKey(), PANEL_W - 30), px + 10, y, C_TEXT, false);
            y += 10;
        }

        // Back button — pinned to bottom
        drawBtn(g, mx, my, px + 8, this.height - 24, PANEL_W - 16, 18, "\u2190 Back",
                isOver(mx, my, px + 8, this.height - 24, PANEL_W - 16, 18), C_BTN);
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

    private Map<String, Integer> computeLayerCounts(int layerY) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (PhantasiaSceneScreen.SHARED_LEVEL == null) return counts;
        for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
            if (e.getKey().getY() != layerY) continue;
            try {
                BlockState bs = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(e.getValue());
                if (!bs.isAir()) counts.merge(bs.getBlock().getName().getString(), 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : sorted) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = this.width - PANEL_W;
        int hw = (PANEL_W - 18) / 2;
        int y = 28;

        // Layer nav buttons
        y += 11;
        if (isOver((int) mx, (int) my, px + 8, y, hw, 15)) {
            cycleLayer(-1);
            return true;
        }
        if (isOver((int) mx, (int) my, px + 10 + hw, y, hw, 15)) {
            cycleLayer(+1);
            return true;
        }
        y += 19;

        // Layer pills
        y += 11;
        int pillX = px + 8;
        for (int i = 0; i < layers.size(); i++) {
            if (pillX + 28 > this.width - 6) {
                pillX = px + 8;
                y += 15;
            }
            if (isOver((int) mx, (int) my, pillX, y, 28, 13)) {
                layerIndex = i;
                inspectedLocal = null;
                return true;
            }
            pillX += 30;
        }
        y += 18;
        y += 8; // separator

        // Heatmap toggle
        if (isOver((int) mx, (int) my, px + 8, y, PANEL_W - 16, 15)) {
            showHeatmap = !showHeatmap;
            return true;
        }
        y += 20;

        // Inspect / Clear button
        if (inspectedLocal != null) {
            y += 11;
            y += 10;
            y += 10;
            BlockPos wp = pattern.toWorld(inspectedLocal);
            if (wp != null) {
                if (pattern.hasBlockEntity(wp)) y += 10;
                if (wp.equals(pattern.controllerWorldPos)) y += 10;
            }
            y += 3;
            if (isOver((int) mx, (int) my, px + 8, y, PANEL_W - 16, 13)) {
                inspectedLocal = null;
                return true;
            }
            y += 18;
            y += 8;
        }

        // Back button
        if (isOver((int) mx, (int) my, px + 8, this.height - 24, PANEL_W - 16, 18)) {
            onClose();
            return true;
        }

        // Grid click
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
        }
        if (kc == 264) {
            cycleLayer(-1);
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    private void cycleLayer(int delta) {
        if (layers.isEmpty()) return;
        layerIndex = Math.max(0, Math.min(layers.size() - 1, layerIndex + delta));
        inspectedLocal = null;
    }

    /**
     * FIX (B5): call applyVisibility() on the parent PhantasiaSceneScreen so the scene
     * reflects the current filter / view state immediately on return, without needing
     * an extra click.
     */
    @Override
    public void onClose() {
        if (parent instanceof PhantasiaSceneScreen pss) pss.applyVisibility();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h,
                         String label, boolean hov, int color) {
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
