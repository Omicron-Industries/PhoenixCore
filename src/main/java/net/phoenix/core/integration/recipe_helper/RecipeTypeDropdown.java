package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Recipe type selector that opens a full-overlay picker screen so it never
 * bleeds into or clips against the parent GUI.
 *
 * Clicking the header opens {@link RecipePickerOverlay}, a lightweight Screen
 * that renders on top of everything. When the user picks a type the overlay
 * calls back into this widget and closes itself.
 */
public class RecipeTypeDropdown extends AbstractWidget {

    // ── Recipe type list ──────────────────────────────────────────────────────

    static final List<String> TYPES = List.of(
            "PHOENIXWARE_FUSION_MK1", "COMB_DECANTING_RECIPES", "HONEY_CHAMBER_RECIPES",
            "APIS_PROGENITOR_RECIPES", "SOURCE_REACTOR_RECIPES", "BIO_ENGINE_RECIPES",
            "SOURCE_IMBUEMENT_RECIPES", "SOURCE_EXTRACTION_RECIPES", "ALCHEMICAL_IMBUER_RECIPES",
            "ASSEMBLER_RECIPES", "ASSEMBLY_LINE_RECIPES", "CIRCUIT_ASSEMBLER_RECIPES",
            "CHEMICAL_RECIPES", "CHEMICAL_BATH_RECIPES", "CENTRIFUGE_RECIPES",
            "ELECTROLYZER_RECIPES", "MIXER_RECIPES", "BLAST_RECIPES", "DISTILLERY_RECIPES",
            "DISTILLATION_RECIPES", "AUTOCLAVE_RECIPES", "FORMING_PRESS_RECIPES",
            "COMPRESSOR_RECIPES", "EXTRACTOR_RECIPES", "MACERATOR_RECIPES",
            "ORE_WASHER_RECIPES", "THERMAL_CENTRIFUGE_RECIPES", "ALLOY_SMELTER_RECIPES",
            "ELECTRIC_FURNACE_RECIPES", "CANNER_RECIPES", "LATHE_RECIPES",
            "BENDER_RECIPES", "FLUID_HEATER_RECIPES", "FLUID_SOLIDIFIER_RECIPES",
            "GAS_COLLECTOR_RECIPES", "WIREMILL_RECIPES", "CUTTER_RECIPES",
            "EXTRUDER_RECIPES", "ELECTROMAGNETIC_SEPARATOR_RECIPES", "SIFTER_RECIPES",
            "LASER_ENGRAVER_RECIPES", "POLARIZER_RECIPES", "FERMENTING_RECIPES",
            "BREWING_RECIPES", "PLASMA_GENERATOR_FUELS", "LARGE_TURBINE_FUELS",
            "COMBUSTION_GENERATOR_FUELS", "GAS_TURBINE_FUELS", "STEAM_TURBINE_FUELS");

    // ── State ─────────────────────────────────────────────────────────────────

    private int selectedIdx = 0;
    private final RecipeBuilderScreen parent;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RecipeTypeDropdown(int x, int y, int w, int h, RecipeBuilderScreen parent) {
        super(x, y, w, h, Component.empty());
        this.parent = parent;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
        boolean hov = isHoveredOrFocused();

        // Header button
        int bg = hov ? 0xFF2A1A3A : 0xFF1A0A2A;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        drawBorder(g, getX(), getY(), getX() + width, getY() + height, 0xFF7A3A9A);

        // Selected type text (truncate if too long)
        String label = TYPES.get(selectedIdx);
        if (parent.getFont().width(label) > width - 14)
            label = parent.getFont().plainSubstrByWidth(label, width - 18) + "…";
        g.drawString(parent.getFont(), label, getX() + 4, getY() + 3, 0xCC88FF, false);

        // Arrow indicator
        g.drawString(parent.getFont(), "▼", getX() + width - 11, getY() + 3, 0x885599, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isActive() || !isHovered()) return false;

        // Open the picker overlay screen on top of the current screen
        Screen current = Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(new RecipePickerOverlay(current, this));
        return true;
    }

    // ── Selection callback (called by RecipePickerOverlay) ────────────────────

    void setSelectedIdx(int idx) {
        if (idx >= 0 && idx < TYPES.size()) selectedIdx = idx;
    }

    int getSelectedIdx() {
        return selectedIdx;
    }

    // ── Public accessors ──────────────────────────────────────────────────────

    public String getSelected() {
        return TYPES.get(selectedIdx);
    }

    public String getSelectedCallPrefix() {
        String t = getSelected();
        boolean isPhoenix = switch (t) {
            case "PHOENIXWARE_FUSION_MK1", "COMB_DECANTING_RECIPES", "HONEY_CHAMBER_RECIPES", "APIS_PROGENITOR_RECIPES", "SOURCE_REACTOR_RECIPES", "BIO_ENGINE_RECIPES", "SOURCE_IMBUEMENT_RECIPES", "SOURCE_EXTRACTION_RECIPES", "ALCHEMICAL_IMBUER_RECIPES" -> true;
            default -> false;
        };
        return isPhoenix ? "PhoenixRecipeTypes." + t : t;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) {
        defaultButtonNarrationText(o);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Inner class: the full-screen overlay picker
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * A lightweight Screen that draws on top of the parent GUI.
     * It renders a scrollable, searchable list of all recipe types.
     * Pressing Escape or clicking outside the panel returns to the parent.
     */
    public static class RecipePickerOverlay extends Screen {

        private static final int PANEL_W  = 260;
        private static final int PANEL_H  = 220;
        private static final int ROW_H    = 14;  // taller rows — less smushed
        private static final int HEADER_H = 28;  // title (14) + search (12) + 2px gap
        private static final int LIST_PAD = 2;   // vertical padding inside list area
        private static final int SCROLL_W = 6;

        private final Screen parent;
        private final RecipeTypeDropdown owner;

        private int scrollOffset = 0;
        private String searchQuery = "";
        private List<String> filtered;

        // Simple inline search box (no EditBox widget — just capture charTyped)
        private boolean searchFocused = true;

        public RecipePickerOverlay(Screen parent, RecipeTypeDropdown owner) {
            super(Component.empty());
            this.parent = parent;
            this.owner = owner;
            this.filtered = TYPES; // start unfiltered
        }

        // ── Init ──────────────────────────────────────────────────────────────

        @Override
        protected void init() {
            super.init();
            applyFilter();
            // Scroll to show currently selected type
            int selInFiltered = filtered.indexOf(TYPES.get(owner.getSelectedIdx()));
            if (selInFiltered >= 0) scrollOffset = Math.max(0, selInFiltered - visibleRows() / 2);
        }

        // ── Render ────────────────────────────────────────────────────────────

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            // Darken the background
            g.fill(0, 0, this.width, this.height, 0xAA000000);

            int px = (this.width - PANEL_W) / 2;
            int py = (this.height - PANEL_H) / 2;

            // Panel BG
            g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF0D000F);
            drawBorder(g, px, py, px + PANEL_W, py + PANEL_H, 0xFF7A3A9A);

            // Title bar
            g.fill(px, py, px + PANEL_W, py + 14, 0xFF1A0A2A);
            g.fill(px, py + 13, px + PANEL_W, py + 14, 0xFF5C2E7A);
            g.drawString(font, "§5Select Recipe Type", px + 5, py + 3, 0xFFFFFF, false);

            // Search box
            int searchY = py + 15;
            g.fill(px + 4, searchY, px + PANEL_W - 4, searchY + 12, 0xFF120018);
            drawBorder(g, px + 4, searchY, px + PANEL_W - 4, searchY + 12, 0xFF5C2E7A);
            String displaySearch = searchQuery.isEmpty() ? "§7Search…" :
                    searchQuery + (System.currentTimeMillis() % 1000 < 500 ? "§7|" : "");
            g.drawString(font, displaySearch, px + 7, searchY + 2, 0xDDCCFF, false);

            // List area — extra bottom room for the ESC hint
            int listY = py + HEADER_H + LIST_PAD;
            int listH = PANEL_H - HEADER_H - LIST_PAD - 14;
            int listX = px + 2;
            int listW = PANEL_W - SCROLL_W - 4;

            g.enableScissor(listX, listY, listX + listW, listY + listH);

            int vis = visibleRows();
            for (int i = 0; i < vis; i++) {
                int idx = i + scrollOffset;
                if (idx >= filtered.size()) break;
                String type = filtered.get(idx);
                int ry = listY + i * ROW_H;
                boolean sel = type.equals(TYPES.get(owner.getSelectedIdx()));
                boolean hov = mx >= listX && mx < listX + listW && my >= ry && my < ry + ROW_H;

                if (sel) g.fill(listX, ry, listX + listW, ry + ROW_H, 0xFF2A0A3A);
                else if (hov) g.fill(listX, ry, listX + listW, ry + ROW_H, 0xFF1A0A28);

                int textCol = sel ? 0xCC88FF : (hov ? 0xDDBBFF : 0x998899);
                g.drawString(font, type, listX + 3, ry + 3, textCol, false);
            }

            g.disableScissor();

            // Scrollbar
            renderScrollbar(g, px + PANEL_W - SCROLL_W - 2, listY, listH);

            // ESC hint
            g.drawString(font, "§8[Esc] close", px + 4, py + PANEL_H - 10, 0x444444, false);
        }

        private void renderScrollbar(GuiGraphics g, int x, int y, int h) {
            if (filtered.size() <= visibleRows()) return;
            g.fill(x, y, x + SCROLL_W, y + h, 0xFF0D000D);
            float ratio = (float) visibleRows() / filtered.size();
            int thumbH = Math.max(8, (int) (h * ratio));
            int thumbY = y +
                    (int) ((h - thumbH) * ((float) scrollOffset / Math.max(1, filtered.size() - visibleRows())));
            g.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + thumbH, 0xFF5C2E7A);
        }

        // ── Input ─────────────────────────────────────────────────────────────

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            int px = (this.width - PANEL_W) / 2;
            int py = (this.height - PANEL_H) / 2;
            int listY = py + HEADER_H + LIST_PAD;
            int listH = PANEL_H - HEADER_H - LIST_PAD - 14;
            int listX = px + 2;
            int listW = PANEL_W - SCROLL_W - 4;

            // Click in list → select
            if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
                int row = (int) ((my - listY) / ROW_H) + scrollOffset;
                if (row < filtered.size()) {
                    String chosen = filtered.get(row);
                    owner.setSelectedIdx(TYPES.indexOf(chosen));
                    close();
                    return true;
                }
            }

            // Click outside panel → close
            if (mx < px || mx > px + PANEL_W || my < py || my > py + PANEL_H) {
                close();
                return true;
            }

            return super.mouseClicked(mx, my, btn);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            int max = Math.max(0, filtered.size() - visibleRows());
            scrollOffset = clamp(scrollOffset - (int) delta, 0, max);
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scan, int mod) {
            if (key == 256) {
                close();
                return true;
            } // ESC
            if (key == 259 && !searchQuery.isEmpty()) { // Backspace
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                applyFilter();
                return true;
            }
            return super.keyPressed(key, scan, mod);
        }

        @Override
        public boolean charTyped(char c, int mod) {
            searchQuery += c;
            applyFilter();
            return true;
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private void applyFilter() {
            scrollOffset = 0;
            if (searchQuery.isEmpty()) {
                filtered = TYPES;
            } else {
                String q = searchQuery.toLowerCase();
                filtered = TYPES.stream().filter(t -> t.toLowerCase().contains(q)).toList();
            }
        }

        private int visibleRows() {
            return (PANEL_H - HEADER_H - LIST_PAD - 14) / ROW_H;
        }

        private void close() {
            Minecraft.getInstance().setScreen(parent);
        }

        private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
            g.fill(x0, y0, x1, y0 + 1, col);
            g.fill(x0, y1 - 1, x1, y1, col);
            g.fill(x0, y0, x0 + 1, y1, col);
            g.fill(x1 - 1, y0, x1, y1, col);
        }

        private int clamp(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
