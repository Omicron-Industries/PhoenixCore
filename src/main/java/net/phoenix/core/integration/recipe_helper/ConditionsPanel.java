package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditions panel — purple/black theme.
 *
 * Shows active conditions as pill tags in a scrollable list.
 * A [+ Add Condition] button opens a modal for picking/configuring a condition.
 */
public class ConditionsPanel extends AbstractWidget {

    // ── Condition types ───────────────────────────────────────────────────────

    public enum ConditionType {

        SOUL_CONDITION("SoulCondition", new String[] { "Threshold (float)" }, new String[] { "0.5" }),
        FLUID_IN_HATCH("FluidInHatch", new String[] { "Fluid Registry ID" }, new String[] { "phoenixcore:fluid" }),
        FUSION_START_EU("fusionStartEU", new String[] { "EU Value" }, new String[] { "40000000" }),
        CLEANROOM("cleanroom", new String[] { "CleanroomType" }, new String[] { "CLEANROOM" }),
        CIRCUIT_META("circuitMeta", new String[] { "Circuit Meta (int)" }, new String[] { "1" }),
        DATA_SOUL_PERM("addData: soulPerm", new String[] { "Value (float)" }, new String[] { "0.01" }),
        DATA_SOUL_TEMP("addData: soulTemp", new String[] { "Value (float)" }, new String[] { "0.5" }),
        DATA_SHIELD_ACT("addData: shieldAct", new String[] { "Value (boolean)" }, new String[] { "true" }),
        DATA_CUSTOM("addData: custom", new String[] { "Key", "Value" }, new String[] { "myKey", "myValue" }),
        STATION_RESEARCH("stationResearch", new String[] { "Research Item Expr", "CWUt" },
                new String[] { "ITEM", "16" }),
        SCANNER_RESEARCH("scannerResearch", new String[] { "Research Item Expr", "Duration", "EUt" },
                new String[] { "ITEM", "2400", "VA[IV]" });

        final String label;
        final String[] fieldLabels;
        final String[] defaults;

        ConditionType(String label, String[] fieldLabels, String[] defaults) {
            this.label = label;
            this.fieldLabels = fieldLabels;
            this.defaults = defaults;
        }
    }

    // ── Active entry ──────────────────────────────────────────────────────────

    public static class ConditionEntry {

        public final ConditionType type;
        public final String[] values;

        public ConditionEntry(ConditionType type, String[] values) {
            this.type = type;
            this.values = values;
        }

        public String toCode() {
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
                case STATION_RESEARCH -> "        .stationResearch(b -> b\n" +
                        "                .researchStack(" + v(0) + ").CWUt(" + v(1) + "))\n";
                case SCANNER_RESEARCH -> "        .scannerResearch(b -> b\n" +
                        "                .researchStack(" + v(0) + ")\n" +
                        "                .duration(" + v(1) + ")\n" +
                        "                .EUt(" + v(2) + "))\n";
            };
        }

        private String v(int i) {
            return (values != null && i < values.length && !values[i].isBlank()) ? values[i].trim() : type.defaults[i];
        }

        public String summary() {
            return (values != null && values.length > 0 && !values[0].isBlank()) ? type.label + ": " + values[0] :
                    type.label;
        }
    }

    // ── Layout constants ──────────────────────────────────────────────────────

    private static final int HEADER_H = 18;
    private static final int PILL_H = 13;
    private static final int PILL_GAP = 3;
    private static final int SCROLL_W = 5;

    // Modal
    private static final int MODAL_W = 240;
    private static final int MODAL_PAD = 8;
    private static final int MODAL_ROW = 20;

    // Type dropdown inside modal
    private static final int TYPE_ROWS = 6;
    private static final int TYPE_ROW_H = 11;

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<ConditionEntry> conditions = new ArrayList<>();
    private int scrollY = 0;

    private boolean modalOpen = false;
    private int modalX, modalY, modalH;
    private ConditionType modalSel = ConditionType.values()[0];
    private boolean typeDropOpen = false;
    private int typeDropScroll = 0;

    private final EditBox[] modalFields = new EditBox[3];
    private Button addBtn;
    private Button confirmBtn;
    private Button cancelBtn;

    private final Font font;
    private final RecipeBuilderScreen parent;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConditionsPanel(int x, int y, int w, int h, Font font, RecipeBuilderScreen parent) {
        super(x, y, w, h, Component.literal("Conditions"));
        this.font = font;
        this.parent = parent;

        addBtn = Button.builder(Component.literal("+ Add Condition"), b -> openModal()).bounds(x + 2, y + 2, 120, 13)
                .build();
        confirmBtn = Button.builder(Component.literal("Add"), b -> confirmModal()).bounds(0, 0, 50, 13).build();
        cancelBtn = Button.builder(Component.literal("Cancel"), b -> closeModal()).bounds(0, 0, 50, 13).build();

        for (int i = 0; i < 3; i++) {
            modalFields[i] = new EditBox(font, 0, 0, 160, 11, Component.empty());
            modalFields[i].visible = false;
        }
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public void setVisible(boolean v) {
        this.visible = v;
        addBtn.visible = v;
        if (!v) closeModal();
    }

    // ── Modal lifecycle ───────────────────────────────────────────────────────

    private void openModal() {
        modalSel = ConditionType.values()[0];
        typeDropOpen = false;
        typeDropScroll = 0;
        rebuildFields();

        modalX = getX() + (width - MODAL_W) / 2;
        modalY = getY() + 20;
        modalH = calcModalH();

        confirmBtn.setPosition(modalX + MODAL_PAD, modalY + modalH - 16);
        cancelBtn.setPosition(modalX + MODAL_W - 58 - MODAL_PAD, modalY + modalH - 16);
        modalOpen = true;
    }

    private void closeModal() {
        modalOpen = false;
        typeDropOpen = false;
        for (EditBox f : modalFields) f.visible = false;
    }

    private void confirmModal() {
        String[] vals = new String[modalSel.fieldLabels.length];
        for (int i = 0; i < vals.length; i++) vals[i] = modalFields[i].getValue();
        conditions.add(new ConditionEntry(modalSel, vals));
        closeModal();
    }

    private void selectType(ConditionType t) {
        modalSel = t;
        typeDropOpen = false;
        rebuildFields();
        modalH = calcModalH();
        confirmBtn.setPosition(modalX + MODAL_PAD, modalY + modalH - 16);
        cancelBtn.setPosition(modalX + MODAL_W - 58 - MODAL_PAD, modalY + modalH - 16);
    }

    private void rebuildFields() {
        for (int i = 0; i < 3; i++) {
            boolean active = i < modalSel.fieldLabels.length;
            modalFields[i].setValue(active ? modalSel.defaults[i] : "");
            modalFields[i].setHint(active ? Component.literal(modalSel.fieldLabels[i]) : Component.empty());
            modalFields[i].visible = active;
            modalFields[i].setFocused(false);
        }
        if (modalSel.fieldLabels.length > 0) modalFields[0].setFocused(true);
    }

    private int calcModalH() {
        return MODAL_PAD + 14          // type selector row
                + MODAL_PAD + modalSel.fieldLabels.length * (MODAL_ROW + 2) + MODAL_PAD + 16 + MODAL_PAD; // confirm row
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
        if (!visible) return;

        // Panel
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xBB080008);
        drawBorder(g, getX(), getY(), getX() + width, getY() + height, 0xFF3A1A5A);

        // Header
        g.fill(getX(), getY(), getX() + width, getY() + HEADER_H, 0xBB130020);
        g.fill(getX(), getY() + HEADER_H - 1, getX() + width, getY() + HEADER_H, 0xFF2E1A3A);
        g.drawString(font, "§dConditions", getX() + 128, getY() + 5, 0xFFFFFF, false);

        addBtn.setPosition(getX() + 2, getY() + 2);
        addBtn.render(g, mx, my, dt);

        // Pills
        g.enableScissor(getX() + 1, getY() + HEADER_H + 1, getX() + width - SCROLL_W - 1, getY() + height - 1);
        renderPills(g, mx, my);
        g.disableScissor();

        renderScrollbar(g);

        if (modalOpen) renderModal(g, mx, my, dt);
    }

    private void renderPills(GuiGraphics g, int mx, int my) {
        int py = getY() + HEADER_H + 4 - scrollY;
        int pillW = width - SCROLL_W - 6;

        for (int i = 0; i < conditions.size(); i++) {
            boolean hov = mx >= getX() + 2 && mx < getX() + 2 + pillW && my >= py && my < py + PILL_H;
            g.fill(getX() + 2, py, getX() + 2 + pillW, py + PILL_H, hov ? 0xFF1E0A30 : 0xFF130018);
            drawBorder(g, getX() + 2, py, getX() + 2 + pillW, py + PILL_H, 0xFF5C2E7A);
            g.drawString(font, conditions.get(i).summary(), getX() + 6, py + 2, 0xCC88FF, false);

            int rx = getX() + 2 + pillW - 13;
            boolean hx = mx >= rx && mx < rx + 11 && my >= py + 1 && my < py + PILL_H - 1;
            g.fill(rx, py + 1, rx + 11, py + PILL_H - 1, hx ? 0xFF6E1040 : 0xFF3A1020);
            g.drawString(font, "×", rx + 2, py + 2, 0xBB4466, false);

            py += PILL_H + PILL_GAP;
        }

        if (conditions.isEmpty())
            g.drawString(font, "No conditions — click + Add Condition", getX() + 6, getY() + HEADER_H + 6, 0x332244,
                    false);
    }

    private void renderScrollbar(GuiGraphics g) {
        int total = conditions.size() * (PILL_H + PILL_GAP);
        int view = height - HEADER_H - 4;
        if (total <= view) return;

        int tx = getX() + width - SCROLL_W - 1;
        int ty0 = getY() + HEADER_H + 1, ty1 = getY() + height - 1;
        g.fill(tx, ty0, tx + SCROLL_W, ty1, 0xFF0A000A);

        int thumbH = Math.max(8, (int) ((ty1 - ty0) * (float) view / total));
        int thumbY = ty0 + (int) ((ty1 - ty0 - thumbH) * (float) scrollY / Math.max(1, total - view));
        g.fill(tx + 1, thumbY, tx + SCROLL_W - 1, thumbY + thumbH, 0xFF5C2E7A);
    }

    private void renderModal(GuiGraphics g, int mx, int my, float dt) {
        // Dim
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xAA000000);

        // Modal bg
        g.fill(modalX, modalY, modalX + MODAL_W, modalY + modalH, 0xFF0D000F);
        drawBorder(g, modalX, modalY, modalX + MODAL_W, modalY + modalH, 0xFF7A3A9A);

        // Title strip
        g.fill(modalX, modalY, modalX + MODAL_W, modalY + 14, 0xFF1A003A);
        g.fill(modalX, modalY + 13, modalX + MODAL_W, modalY + 14, 0xFF5C2E7A);
        g.drawString(font, "§5Add Condition", modalX + MODAL_PAD, modalY + 3, 0xFFFFFF, false);

        int cy = modalY + 15;

        // Type selector header
        boolean typeHov = mx >= modalX + MODAL_PAD && mx < modalX + MODAL_W - MODAL_PAD && my >= cy && my < cy + 13;
        g.fill(modalX + MODAL_PAD, cy, modalX + MODAL_W - MODAL_PAD, cy + 13,
                typeDropOpen ? 0xFF200040 : (typeHov ? 0xFF1A003A : 0xFF130028));
        drawBorder(g, modalX + MODAL_PAD, cy, modalX + MODAL_W - MODAL_PAD, cy + 13, 0xFF5C2E7A);
        g.drawString(font, modalSel.label, modalX + MODAL_PAD + 3, cy + 2, 0xCC88FF, false);
        g.drawString(font, typeDropOpen ? "▲" : "▼", modalX + MODAL_W - MODAL_PAD - 10, cy + 2, 0x774499, false);
        cy += 14;

        // Dropdown list
        if (typeDropOpen) {
            ConditionType[] all = ConditionType.values();
            int dropH = Math.min(TYPE_ROWS, all.length) * TYPE_ROW_H;
            g.fill(modalX + MODAL_PAD, cy, modalX + MODAL_W - MODAL_PAD, cy + dropH, 0xFF0A000A);
            drawBorder(g, modalX + MODAL_PAD, cy, modalX + MODAL_W - MODAL_PAD, cy + dropH, 0xFF5C2E7A);

            for (int i = 0; i < TYPE_ROWS && (i + typeDropScroll) < all.length; i++) {
                ConditionType t = all[i + typeDropScroll];
                int ry = cy + i * TYPE_ROW_H;
                boolean rh = mx >= modalX + MODAL_PAD && mx < modalX + MODAL_W - MODAL_PAD && my >= ry &&
                        my < ry + TYPE_ROW_H;
                if (t == modalSel)
                    g.fill(modalX + MODAL_PAD + 1, ry, modalX + MODAL_W - MODAL_PAD - 1, ry + TYPE_ROW_H, 0xFF200050);
                else if (rh)
                    g.fill(modalX + MODAL_PAD + 1, ry, modalX + MODAL_W - MODAL_PAD - 1, ry + TYPE_ROW_H, 0xFF150030);
                g.drawString(font, t.label, modalX + MODAL_PAD + 4, ry + 1, t == modalSel ? 0xCC88FF : 0x998899, false);
            }
            cy += dropH;
        }

        // Fields
        if (!typeDropOpen) {
            cy += MODAL_PAD;
            for (int i = 0; i < modalSel.fieldLabels.length; i++) {
                g.drawString(font, modalSel.fieldLabels[i], modalX + MODAL_PAD, cy, 0x886688, false);
                cy += 10;
                modalFields[i].setPosition(modalX + MODAL_PAD, cy);
                modalFields[i].setWidth(MODAL_W - MODAL_PAD * 2);
                modalFields[i].render(g, mx, my, dt);
                cy += 13;
            }
        }

        confirmBtn.render(g, mx, my, dt);
        cancelBtn.render(g, mx, my, dt);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible) return false;
        if (modalOpen) return handleModalClick(mx, my, btn);
        if (addBtn.mouseClicked(mx, my, btn)) return true;
        return handlePillClick(mx, my);
    }

    private boolean handleModalClick(double mx, double my, int btn) {
        int cy = modalY + 15;

        // Type selector toggle
        if (mx >= modalX + MODAL_PAD && mx < modalX + MODAL_W - MODAL_PAD && my >= cy && my < cy + 13) {
            typeDropOpen = !typeDropOpen;
            return true;
        }
        cy += 14;

        if (typeDropOpen) {
            ConditionType[] all = ConditionType.values();
            int dropH = Math.min(TYPE_ROWS, all.length) * TYPE_ROW_H;
            if (mx >= modalX + MODAL_PAD && mx < modalX + MODAL_W - MODAL_PAD && my >= cy && my < cy + dropH) {
                int row = (int) ((my - cy) / TYPE_ROW_H) + typeDropScroll;
                if (row < all.length) selectType(all[row]);
                return true;
            }
            typeDropOpen = false;
            return true;
        }

        for (int i = 0; i < modalSel.fieldLabels.length; i++)
            if (modalFields[i].mouseClicked(mx, my, btn)) return true;
        if (confirmBtn.mouseClicked(mx, my, btn)) return true;
        if (cancelBtn.mouseClicked(mx, my, btn)) return true;

        if (mx < modalX || mx > modalX + MODAL_W || my < modalY || my > modalY + modalH) closeModal();
        return true;
    }

    private boolean handlePillClick(double mx, double my) {
        int py = getY() + HEADER_H + 4 - scrollY;
        int pillW = width - SCROLL_W - 6;
        for (int i = 0; i < conditions.size(); i++) {
            if (my >= py && my < py + PILL_H) {
                int rx = getX() + 2 + pillW - 13;
                if (mx >= rx && mx < rx + 11) {
                    conditions.remove(i);
                    return true;
                }
            }
            py += PILL_H + PILL_GAP;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!visible) return false;
        if (modalOpen && typeDropOpen) {
            typeDropScroll = clamp(typeDropScroll - (int) delta, 0,
                    Math.max(0, ConditionType.values().length - TYPE_ROWS));
            return true;
        }
        if (!isMouseOver(mx, my)) return false;
        int total = conditions.size() * (PILL_H + PILL_GAP);
        int view = height - HEADER_H - 4;
        scrollY = clamp(scrollY - (int) (delta * (PILL_H + PILL_GAP)), 0, Math.max(0, total - view));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (!visible || !modalOpen) return false;
        if (key == 256) {
            closeModal();
            return true;
        }
        if (key == 257) {
            confirmModal();
            return true;
        }
        if (key == 259 && !typeDropOpen) {
            for (int i = 0; i < modalSel.fieldLabels.length; i++)
                if (modalFields[i].isFocused() && modalFields[i].keyPressed(key, scan, mod)) return true;
        }
        for (int i = 0; i < modalSel.fieldLabels.length; i++)
            if (modalFields[i].keyPressed(key, scan, mod)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (!visible || !modalOpen) return false;
        for (int i = 0; i < modalSel.fieldLabels.length; i++)
            if (modalFields[i].isFocused() && modalFields[i].charTyped(c, mod)) return true;
        return false;
    }

    // ── Code generation ───────────────────────────────────────────────────────

    public String buildConditionLines() {
        StringBuilder sb = new StringBuilder();
        for (ConditionEntry e : conditions) sb.append(e.toCode());
        return sb.toString();
    }

    public void clear() {
        conditions.clear();
        scrollY = 0;
        closeModal();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
    protected void updateWidgetNarration(NarrationElementOutput o) {
        defaultButtonNarrationText(o);
    }
}
