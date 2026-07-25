package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FluidSlotPanel extends AbstractWidget {

    public static class FluidEntry {

        public String fluidExpr = null;
        public int amount = 1000;
    }

    private static final int SLOT_W = 18;
    private static final int SLOT_H = 18;
    private static final int GAP = 2;
    private static final int LABEL_H = 10;

    private final int slotCount;
    @Getter
    private final List<FluidEntry> entries;
    private final RecipeBuilderScreen parent;

    public FluidSlotPanel(int x, int y, int slotCount, String label, RecipeBuilderScreen parent) {
        super(x, y, slotCount * SLOT_W + (slotCount - 1) * GAP, LABEL_H + SLOT_H, Component.literal(label));
        this.slotCount = slotCount;
        this.parent = parent;
        this.entries = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) entries.add(new FluidEntry());
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mx, int my, float dt) {
        if (!this.visible) return;

        g.drawString(parent.getFont(), getMessage(), getX(), getY(), 0xAA88FF, false);

        for (int i = 0; i < slotCount; i++) {
            int sx = getX() + i * (SLOT_W + GAP);
            int sy = getY() + LABEL_H;
            FluidEntry e = entries.get(i);

            int bgCol = e.fluidExpr != null ? hashColor(e.fluidExpr) : 0xFF180820;
            g.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, bgCol);
            renderBorder(g, sx, sy, sx + SLOT_W, sy + SLOT_H, 0xFF5C2E7A);

            if (isOver(mx, my, sx, sy, SLOT_W, SLOT_H))
                g.fill(sx + 1, sy + 1, sx + SLOT_W - 1, sy + SLOT_H - 1, 0x33CC88FF);

            if (e.fluidExpr != null) {
                String amt = RecipeBuilderScreen.formatAmount(e.amount);
                g.drawString(parent.getFont(), amt, sx + 2, sy + 6, 0xEEDDFF, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!this.visible) return false;
        int i = colAt(mx, my);
        if (i >= 0 && btn == 1) {
            FluidEntry e = entries.get(i);
            if (e.fluidExpr != null) {
                parent.openEditor(mx, my, e, e.amount);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!this.visible) return false;
        int i = colAt(mx, my);
        if (i >= 0) {
            FluidEntry e = entries.get(i);
            if (e.fluidExpr != null) {
                int step = Screen.hasShiftDown() ? 10 : 100;
                e.amount = Math.max(1, e.amount + (int) (delta * step));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    public boolean acceptFluid(String fluidId, int amount, double mx, double my) {
        int target = colAt(mx, my);
        if (target < 0) {
            for (int i = 0; i < slotCount; i++) {
                if (entries.get(i).fluidExpr == null) {
                    target = i;
                    break;
                }
            }
        }
        if (target < 0) return false;
        entries.get(target).fluidExpr = fluidId;
        entries.get(target).amount = amount;
        return true;
    }

    private int colAt(double mx, double my) {
        for (int i = 0; i < slotCount; i++) {
            int sx = getX() + i * (SLOT_W + GAP);
            int sy = getY() + LABEL_H;
            if (isOver(mx, my, sx, sy, SLOT_W, SLOT_H)) return i;
        }
        return -1;
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }

    private int hashColor(String s) {
        int h = s.hashCode();
        int r = 80 + (h & 0x5F);
        int g = 20 + ((h >> 7) & 0x3F);
        int b = 150 + ((h >> 14) & 0x6F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void setVisible(boolean v) {
        this.visible = v;
        this.active = v;
    }

    public void clear() {
        for (FluidEntry e : entries) {
            e.fluidExpr = null;
            e.amount = 1000;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput o) {
        defaultButtonNarrationText(o);
    }
}
