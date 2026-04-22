package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Conditions editor screen.
 *
 * Pressing "+ Add Condition" pushes {@link ConditionFormOverlay} (a separate Screen)
 * so the form never bleeds into the pill list. Choosing the type inside that form
 * pushes {@link ConditionTypePickerOverlay}, keeping all dropdowns out of the main panel.
 *
 * Research conditions (STATION_RESEARCH, SCANNER_RESEARCH) expose an 18×18 item slot
 * for the research stack that is compatible with EMI drag-and-drop.
 */
public class ConditionsScreen extends Screen {

    // ── Condition types ───────────────────────────────────────────────────────

    public enum ConditionType {

        SOUL_CONDITION("SoulCondition", new String[] { "Threshold (float)" }, new String[] { "0.5" }),
        FLUID_IN_HATCH("FluidInHatch", new String[] { "Fluid Registry ID" }, new String[] { "phoenixcore:fluid" }),
        FUSION_START_EU("fusionStartEU", new String[] { "EU Value (long)" }, new String[] { "40000000" }),
        CLEANROOM("cleanroom", new String[] { "CleanroomType" }, new String[] { "CLEANROOM" }),
        CIRCUIT_META("circuitMeta", new String[] { "Circuit Meta (int)" }, new String[] { "1" }),
        DATA_SOUL_PERM("addData: soulPerm", new String[] { "Value (float)" }, new String[] { "0.01" }),
        DATA_SOUL_TEMP("addData: soulTemp", new String[] { "Value (float)" }, new String[] { "0.5" }),
        DATA_SHIELD_ACT("addData: shieldAct", new String[] { "Value (boolean)" }, new String[] { "true" }),
        DATA_CUSTOM("addData: custom", new String[] { "Key (String)", "Value (any)" },
                new String[] { "myKey", "myValue" }),
        STATION_RESEARCH("stationResearch", new String[] { "Research Item (drop here)", "CWUt" },
                new String[] { "", "16" }),
        SCANNER_RESEARCH("scannerResearch", new String[] { "Research Item (drop here)", "Duration", "EUt" },
                new String[] { "", "2400", "VA[IV]" });

        public final String label;
        public final String[] fieldLabels;
        public final String[] defaults;

        /** True when field {@code i} should be an EMI item-slot rather than a text box. */
        public boolean isItemSlot(int i) {
            return (this == STATION_RESEARCH || this == SCANNER_RESEARCH) && i == 0;
        }

        ConditionType(String label, String[] fieldLabels, String[] defaults) {
            this.label = label;
            this.fieldLabels = fieldLabels;
            this.defaults = defaults;
        }
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    public record ConditionEntry(ConditionType type, String[] values) {

        public String toJavaCode() {
            return switch (type) {
                case SOUL_CONDITION -> "        .addCondition(new SoulCondition(false, " + v(0) + "f))\n";
                case FLUID_IN_HATCH -> "        .addCondition(FluidInHatchCondition.of(\"" + v(0) + "\"))\n";
                case FUSION_START_EU -> "        .fusionStartEU(" + v(0) + ")\n";
                case CLEANROOM -> "        .cleanroom(CleanroomType." + v(0) + ")\n";
                case CIRCUIT_META -> "        .circuitMeta(" + v(0) + ")\n";
                case DATA_SOUL_PERM -> "        .addData(\"soul_growth_perm\", " + v(0) + "f)\n";
                case DATA_SOUL_TEMP -> "        .addData(\"soul_growth_temp\", " + v(0) + "f)\n";
                case DATA_SHIELD_ACT -> "        .addData(\"shield_activation\", " + v(0) + ")\n";
                case DATA_CUSTOM -> "        .addData(\"" + v(0) + "\", " + v(1) + ")\n";
                case STATION_RESEARCH -> "        .stationResearch(b -> b\n                .researchStack(" + v(0) +
                        ").CWUt(" + v(1) + "))\n";
                case SCANNER_RESEARCH -> "        .scannerResearch(b -> b\n                .researchStack(" + v(0) +
                        ")\n                .duration(" + v(1) + ")\n                .EUt(" + v(2) + "))\n";
            };
        }

        public String toKjsCode() {
            return "    // Condition: " + type.label + "(" + String.join(", ", values) + ")\n";
        }

        public String summary() {
            return (values.length > 0 && !values[0].isBlank()) ? type.label + ": " + values[0] : type.label;
        }

        private String v(int i) {
            return (i < values.length && !values[i].isBlank()) ? values[i].trim() : type.defaults[i];
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 220;
    private static final int PILL_H = 14;
    private static final int PILL_GAP = 3;
    private static final int SCROLL_W = 6;
    private static final int HDR_H = 22;

    // ── State ─────────────────────────────────────────────────────────────────

    private final Screen parent;
    private final List<ConditionEntry> conditions;
    private int pillScroll = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConditionsScreen(Screen parent, List<ConditionEntry> conditions) {
        super(Component.empty());
        this.parent = parent;
        this.conditions = conditions;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        int px = panelX(), py = panelY();

        addRenderableWidget(Button.builder(
                Component.literal("+ Add Condition"),
                b -> Minecraft.getInstance().setScreen(new ConditionFormOverlay(this, conditions)))
                .bounds(px + 2, py + 4, 120, 13).build());

        addRenderableWidget(Button.builder(
                Component.literal("← Back"),
                b -> onClose()).bounds(px + PANEL_W - 54, py + 4, 52, 13).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int px = panelX(), py = panelY();

        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF0D000F);
        drawBorder(g, px, py, px + PANEL_W, py + PANEL_H, 0xFF7A3A9A);
        g.fill(px, py, px + PANEL_W, py + HDR_H, 0xFF15002A);
        g.fill(px, py + HDR_H - 1, px + PANEL_W, py + HDR_H, 0xFF5C2E7A);
        g.drawString(font, "§5Conditions", px + 128, py + 7, 0xFFFFFF, false);

        int listX = px + 2, listW = PANEL_W - SCROLL_W - 4;
        g.enableScissor(listX, py + HDR_H + 1, listX + listW, py + PANEL_H - 1);
        renderPills(g, mx, my, listX, py + HDR_H, listW);
        g.disableScissor();
        renderScrollbar(g, px, py);

        super.render(g, mx, my, pt);
    }

    private void renderPills(GuiGraphics g, int mx, int my, int lx, int ly, int lw) {
        int ry = ly + 4 - pillScroll;
        for (ConditionEntry c : conditions) {
            boolean hov = mx >= lx + 2 && mx < lx + lw && my >= ry && my < ry + PILL_H;
            g.fill(lx + 2, ry, lx + lw, ry + PILL_H, hov ? 0xFF1E0A30 : 0xFF130018);
            drawBorder(g, lx + 2, ry, lx + lw, ry + PILL_H, 0xFF5C2E7A);
            g.drawString(font, c.summary(), lx + 6, ry + 3, 0xCC88FF, false);
            int rx = lx + lw - 13;
            boolean hx = mx >= rx && mx < rx + 11 && my >= ry + 1 && my < ry + PILL_H - 1;
            g.fill(rx, ry + 1, rx + 11, ry + PILL_H - 1, hx ? 0xFF6E1040 : 0xFF3A1020);
            g.drawString(font, "x", rx + 2, ry + 3, 0xBB4466, false);
            ry += PILL_H + PILL_GAP;
        }
        if (conditions.isEmpty())
            g.drawString(font, "No conditions yet.  Click + Add Condition.", lx + 6, ly + 8, 0x443355, false);
    }

    private void renderScrollbar(GuiGraphics g, int px, int py) {
        int total = conditions.size() * (PILL_H + PILL_GAP);
        int view = PANEL_H - HDR_H - 4;
        if (total <= view) return;
        int tx = px + PANEL_W - SCROLL_W - 1, ty0 = py + HDR_H + 1, ty1 = py + PANEL_H - 1;
        g.fill(tx, ty0, tx + SCROLL_W, ty1, 0xFF0A000A);
        int th = Math.max(8, (int) ((ty1 - ty0) * (float) view / total));
        int ty = ty0 + (int) ((ty1 - ty0 - th) * (float) pillScroll / Math.max(1, total - view));
        g.fill(tx + 1, ty, tx + SCROLL_W - 1, ty + th, 0xFF5C2E7A);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (handlePillClick(mx, my)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    private boolean handlePillClick(double mx, double my) {
        int lx = panelX() + 2, lw = PANEL_W - SCROLL_W - 4;
        int ry = panelY() + HDR_H + 4 - pillScroll;
        for (int i = 0; i < conditions.size(); i++) {
            if (my >= ry && my < ry + PILL_H) {
                int rx = lx + lw - 13;
                if (mx >= rx && mx < rx + 11) {
                    conditions.remove(i);
                    return true;
                }
            }
            ry += PILL_H + PILL_GAP;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int total = conditions.size() * (PILL_H + PILL_GAP);
        int view = PANEL_H - HDR_H - 4;
        pillScroll = clamp(pillScroll - (int) (delta * (PILL_H + PILL_GAP)), Math.max(0, total - view));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }

    private int clamp(int v, int hi) {
        return Math.max(0, Math.min(hi, v));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ConditionFormOverlay — "Add Condition" form as a separate Screen
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Renders the add-condition form on top of the underlying ConditionsScreen.
     * Each text field has a dark background rectangle.
     * Research item fields are 18×18 item slots (EMI/cursor drag-and-drop).
     * Clicking "Choose Type ▼" pushes ConditionTypePickerOverlay.
     */
    public static class ConditionFormOverlay extends Screen {

        private static final int FORM_W = 284;
        private static final int FORM_PAD = 8;
        private static final int LABEL_H = 9;
        private static final int BOX_H = 14;   // height of each input row
        private static final int ROW_GAP = 4;    // gap between rows
        private static final int HDR_H = 16;
        private static final int TYPE_BTN_H = 14;

        private final Screen parent;
        private final List<ConditionEntry> conditions;

        ConditionType formType = ConditionType.values()[0];
        private final EditBox[] formFields = new EditBox[3];
        final String[] slotKeys = new String[3];
        final ItemStack[] slotStacks = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

        private Button confirmBtn, cancelBtn, chooseTypeBtn;
        private int formX, formY, formH;

        public ConditionFormOverlay(Screen parent, List<ConditionEntry> conditions) {
            super(Component.empty());
            this.parent = parent;
            this.conditions = conditions;
        }

        @Override
        protected void init() {
            super.init();
            formX = (this.width - FORM_W) / 2;
            formH = calcFormH();
            formY = (this.height - formH) / 2;

            chooseTypeBtn = addRenderableWidget(Button.builder(
                    Component.literal(formType.label + " ▼"),
                    b -> Minecraft.getInstance().setScreen(new ConditionTypePickerOverlay(this)))
                    .bounds(formX + FORM_PAD, formY + HDR_H + 4, FORM_W - FORM_PAD * 2, TYPE_BTN_H).build());

            for (int i = 0; i < 3; i++) {
                EditBox box = new EditBox(font,
                        formX + FORM_PAD + 1, 0, // Y set in rebuildForm
                        FORM_W - FORM_PAD * 2 - 2, BOX_H - 2,
                        Component.empty());
                box.setMaxLength(256);
                box.setBordered(false); // we draw our own border
                formFields[i] = addRenderableWidget(box);
            }

            confirmBtn = addRenderableWidget(Button.builder(
                    Component.literal("Add"), b -> confirmForm()).bounds(formX + FORM_PAD, 0, 60, 13).build());

            cancelBtn = addRenderableWidget(Button.builder(
                    Component.literal("Cancel"), b -> close()).bounds(formX + FORM_W - FORM_PAD - 60, 0, 60, 13)
                    .build());

            rebuildForm();
        }

        // ── Called back by type-picker ─────────────────────────────────────────

        void applyType(ConditionType t) {
            formType = t;
            Arrays.fill(slotKeys, null);
            Arrays.fill(slotStacks, ItemStack.EMPTY);
            chooseTypeBtn.setMessage(Component.literal(formType.label + " ▼"));
            rebuildForm();
        }

        // ── Layout ────────────────────────────────────────────────────────────

        private int calcFormH() {
            // header + gap + type-btn + gap + per-field rows + gap + confirm row + bottom pad
            return HDR_H + 4 + TYPE_BTN_H + FORM_PAD + formType.fieldLabels.length * (LABEL_H + BOX_H + ROW_GAP) +
                    FORM_PAD + 13 + FORM_PAD;
        }

        /** Top-left Y of field i's background box. */
        private int fieldBoxY(int i) {
            return formY + HDR_H + 4 + TYPE_BTN_H + FORM_PAD + i * (LABEL_H + BOX_H + ROW_GAP) + LABEL_H;
        }

        private void rebuildForm() {
            formH = calcFormH();
            formY = (this.height - formH) / 2;
            chooseTypeBtn.setPosition(formX + FORM_PAD, formY + HDR_H + 4);

            int fields = formType.fieldLabels.length;
            for (int i = 0; i < 3; i++) {
                boolean active = i < fields && !formType.isItemSlot(i);
                formFields[i].visible = active;
                formFields[i].active = active;
                if (active) {
                    formFields[i].setY(fieldBoxY(i) + 1);
                    formFields[i].setValue(formType.defaults[i]);
                    formFields[i].setHint(Component.literal(formType.fieldLabels[i]));
                }
            }
            if (fields > 0 && !formType.isItemSlot(0)) formFields[0].setFocused(true);

            int btnY = formY + formH - FORM_PAD - 13;
            confirmBtn.setPosition(formX + FORM_PAD, btnY);
            cancelBtn.setPosition(formX + FORM_W - FORM_PAD - 60, btnY);
        }

        // ── Confirm ───────────────────────────────────────────────────────────

        private void confirmForm() {
            int n = formType.fieldLabels.length;
            String[] vals = new String[n];
            for (int i = 0; i < n; i++) {
                if (formType.isItemSlot(i)) {
                    vals[i] = (slotKeys[i] != null && !slotKeys[i].isBlank()) ? slotKeys[i] : formType.defaults[i];
                } else {
                    String v = formFields[i].getValue().trim();
                    vals[i] = v.isEmpty() ? formType.defaults[i] : v;
                }
            }
            conditions.add(new ConditionEntry(formType, vals));
            close();
        }

        private void close() {
            Minecraft.getInstance().setScreen(parent);
        }

        // ── Render ────────────────────────────────────────────────────────────

        @Override
        public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
            // Render parent underneath so the pill list stays visible
            parent.render(g, -1, -1, pt);
            // Dim
            g.fill(0, 0, this.width, this.height, 0x88000000);

            // Form panel
            g.fill(formX, formY, formX + FORM_W, formY + formH, 0xFF0D000F);
            drawBorder(g, formX, formY, formX + FORM_W, formY + formH, 0xFF7A3A9A);
            g.fill(formX, formY, formX + FORM_W, formY + HDR_H, 0xFF1A003A);
            g.fill(formX, formY + HDR_H - 1, formX + FORM_W, formY + HDR_H, 0xFF5C2E7A);
            g.drawString(font, "§dAdd Condition", formX + FORM_PAD, formY + 4, 0xFFFFFF, false);

            // Per-field rendering
            int fields = formType.fieldLabels.length;
            for (int i = 0; i < fields; i++) {
                int labelY = fieldBoxY(i) - LABEL_H;
                int boxY = fieldBoxY(i);
                int bx = formX + FORM_PAD;
                int bw = FORM_W - FORM_PAD * 2;

                g.drawString(font, formType.fieldLabels[i], bx, labelY, 0x886688, false);

                if (formType.isItemSlot(i)) {
                    // 18×18 item slot
                    boolean hov = mx >= bx && mx < bx + 18 && my >= boxY && my < boxY + 18;
                    g.fill(bx, boxY, bx + 18, boxY + 18, 0xFF1A0A2A);
                    drawBorder(g, bx, boxY, bx + 18, boxY + 18, 0xFF7A3A9A);
                    if (hov) g.fill(bx + 1, boxY + 1, bx + 17, boxY + 17, 0x22FFFFFF);
                    if (!slotStacks[i].isEmpty()) {
                        g.renderFakeItem(slotStacks[i], bx + 1, boxY + 1);
                    } else {
                        g.drawString(font, "§8?", bx + 5, boxY + 5, 0x664466, false);
                    }
                    if (hov && slotKeys[i] != null)
                        g.renderTooltip(font, Component.literal(slotKeys[i]), mx, my);
                } else {
                    // Text box with background
                    g.fill(bx, boxY, bx + bw, boxY + BOX_H, 0xFF090012);
                    drawBorder(g, bx, boxY, bx + bw, boxY + BOX_H, 0xFF4A2060);
                }
            }

            super.render(g, mx, my, pt);
        }

        // ── Input ─────────────────────────────────────────────────────────────

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            int fields = formType.fieldLabels.length;
            for (int i = 0; i < fields; i++) {
                if (!formType.isItemSlot(i)) continue;
                int bx = formX + FORM_PAD, by = fieldBoxY(i);
                if (mx >= bx && mx < bx + 18 && my >= by && my < by + 18 && btn == 1) {
                    slotKeys[i] = null;
                    slotStacks[i] = ItemStack.EMPTY;
                    return true;
                }
            }
            // Click outside → close
            if (mx < formX || mx > formX + FORM_W || my < formY || my > formY + formH) {
                close();
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int btn) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack carried = mc.player != null ? mc.player.containerMenu.getCarried() : ItemStack.EMPTY;
            if (!carried.isEmpty()) {
                int fields = formType.fieldLabels.length;
                for (int i = 0; i < fields; i++) {
                    if (!formType.isItemSlot(i)) continue;
                    int bx = formX + FORM_PAD, by = fieldBoxY(i);
                    if (mx >= bx && mx < bx + 18 && my >= by && my < by + 18) {
                        slotStacks[i] = carried.copy();
                        slotKeys[i] = ForgeRegistries.ITEMS.getKey(carried.getItem()).toString();
                        return true;
                    }
                }
            }
            return super.mouseReleased(mx, my, btn);
        }

        @Override
        public boolean keyPressed(int key, int scan, int mod) {
            if (key == 256) {
                close();
                return true;
            }
            if (key == 257) {
                confirmForm();
                return true;
            }
            return super.keyPressed(key, scan, mod);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
            g.fill(x0, y0, x1, y0 + 1, col);
            g.fill(x0, y1 - 1, x1, y1, col);
            g.fill(x0, y0, x0 + 1, y1, col);
            g.fill(x1 - 1, y0, x1, y1, col);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ConditionTypePickerOverlay — scrollable / searchable type list
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ConditionTypePickerOverlay extends Screen {

        private static final int PANEL_W = 240;
        private static final int PANEL_H = 200;
        private static final int ROW_H = 14;
        private static final int HEADER_H = 28; // title (14) + search (12) + gap
        private static final int SCROLL_W = 6;

        private final ConditionFormOverlay formScreen;
        private int scrollOffset = 0;
        private String searchQuery = "";
        private ConditionType[] filtered;

        public ConditionTypePickerOverlay(ConditionFormOverlay formScreen) {
            super(Component.empty());
            this.formScreen = formScreen;
            this.filtered = ConditionType.values();
        }

        @Override
        protected void init() {
            super.init();
            applyFilter();
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
            // Render the form screen underneath
            formScreen.render(g, -1, -1, pt);
            g.fill(0, 0, this.width, this.height, 0x66000000);

            int px = (this.width - PANEL_W) / 2;
            int py = (this.height - PANEL_H) / 2;

            g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF0D000F);
            drawBorder(g, px, py, px + PANEL_W, py + PANEL_H, 0xFF7A3A9A);
            g.fill(px, py, px + PANEL_W, py + 14, 0xFF1A003A);
            g.fill(px, py + 13, px + PANEL_W, py + 14, 0xFF5C2E7A);
            g.drawString(font, "§5Select Condition Type", px + 4, py + 3, 0xFFFFFF, false);

            // Search box
            int sY = py + 15;
            g.fill(px + 4, sY, px + PANEL_W - 4, sY + 12, 0xFF120018);
            drawBorder(g, px + 4, sY, px + PANEL_W - 4, sY + 12, 0xFF5C2E7A);
            String disp = searchQuery.isEmpty() ? "§7Search…" :
                    searchQuery + (System.currentTimeMillis() % 1000 < 500 ? "§7|" : "");
            g.drawString(font, disp, px + 7, sY + 2, 0xDDCCFF, false);

            // List
            int listY = py + HEADER_H;
            int listH = PANEL_H - HEADER_H - 12;
            int listX = px + 2;
            int listW = PANEL_W - SCROLL_W - 4;
            int vis = listH / ROW_H;

            g.enableScissor(listX, listY, listX + listW, listY + listH);
            for (int i = 0; i < vis; i++) {
                int idx = i + scrollOffset;
                if (idx >= filtered.length) break;
                ConditionType t = filtered[idx];
                int ry = listY + i * ROW_H;
                boolean hov = mx >= listX && mx < listX + listW && my >= ry && my < ry + ROW_H;
                boolean isCur = t == formScreen.formType;
                if (isCur) g.fill(listX, ry, listX + listW, ry + ROW_H, 0xFF2A0A3A);
                else if (hov) g.fill(listX, ry, listX + listW, ry + ROW_H, 0xFF1A0A28);
                int col = isCur ? 0xCC88FF : (hov ? 0xDDBBFF : 0x998899);
                g.drawString(font, t.label, listX + 4, ry + 3, col, false);
            }
            g.disableScissor();

            // Scrollbar
            if (filtered.length > vis) {
                int tx = px + PANEL_W - SCROLL_W - 2;
                g.fill(tx, listY, tx + SCROLL_W, listY + listH, 0xFF0D000D);
                int th = Math.max(8, (int) ((float) listH * vis / filtered.length));
                int ty = listY + (int) ((float) (listH - th) * scrollOffset / Math.max(1, filtered.length - vis));
                g.fill(tx + 1, ty, tx + SCROLL_W - 1, ty + th, 0xFF5C2E7A);
            }

            g.drawString(font, "§8[Esc] back", px + 4, py + PANEL_H - 10, 0x444444, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            int px = (this.width - PANEL_W) / 2, py = (this.height - PANEL_H) / 2;
            int listY = py + HEADER_H, listH = PANEL_H - HEADER_H - 12;
            int listX = px + 2, listW = PANEL_W - SCROLL_W - 4;
            int vis = listH / ROW_H;

            if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
                int row = (int) ((my - listY) / ROW_H) + scrollOffset;
                if (row < filtered.length) {
                    formScreen.applyType(filtered[row]);
                    close();
                    return true;
                }
            }
            if (mx < px || mx > px + PANEL_W || my < py || my > py + PANEL_H) {
                close();
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            int vis = (PANEL_H - HEADER_H - 12) / ROW_H;
            scrollOffset = Math.max(0, Math.min(Math.max(0, filtered.length - vis), scrollOffset - (int) delta));
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scan, int mod) {
            if (key == 256) {
                close();
                return true;
            }
            if (key == 259 && !searchQuery.isEmpty()) {
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

        private void applyFilter() {
            scrollOffset = 0;
            ConditionType[] all = ConditionType.values();
            if (searchQuery.isEmpty()) {
                filtered = all;
                return;
            }
            String q = searchQuery.toLowerCase();
            filtered = Arrays.stream(all)
                    .filter(t -> t.label.toLowerCase().contains(q) || t.name().toLowerCase().contains(q))
                    .toArray(ConditionType[]::new);
        }

        private void close() {
            Minecraft.getInstance().setScreen(formScreen);
        }

        private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
            g.fill(x0, y0, x1, y0 + 1, col);
            g.fill(x0, y1 - 1, x1, y1, col);
            g.fill(x0, y0, x0 + 1, y1, col);
            g.fill(x1 - 1, y0, x1, y1, col);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
