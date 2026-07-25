package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen.formatAmount;

public class SlotPanel extends AbstractWidget {

    public static class SlotEntry {

        public ItemStack stack = ItemStack.EMPTY;
        public int count = 1;
        public boolean notConsumable = false;
    }

    private static final int S = 18;
    private static final int GA = 2;

    private final int slotCount;
    @Getter
    private final List<SlotEntry> entries;
    private final RecipeBuilderScreen parent;

    public SlotPanel(int x, int y, int slotCount, String label, RecipeBuilderScreen parent) {
        super(x, y, slotCount * S + (slotCount - 1) * GA, S + 10, Component.literal(label));
        this.slotCount = slotCount;
        this.parent = parent;
        this.entries = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) entries.add(new SlotEntry());
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mx, int my, float dt) {
        if (!this.visible) return;

        guiGraphics.drawString(parent.getFont(), getMessage(), getX(), getY(), 0x888888, false);

        for (int i = 0; i < slotCount; i++) {
            int sx = getX() + i * (S + GA);
            int sy = getY() + 10;

            guiGraphics.fill(sx, sy, sx + S, sy + S, 0xFF2C2C2C);
            renderBorder(guiGraphics, sx, sy, sx + S, sy + S, 0xFF505050);

            SlotEntry e = entries.get(i);

            if (e.notConsumable && !e.stack.isEmpty())
                guiGraphics.fill(sx + 1, sy + 1, sx + S - 1, sy + S - 1, 0x55FF8800);

            if (!e.stack.isEmpty()) {
                guiGraphics.renderFakeItem(e.stack, sx + 1, sy + 1);
                if (e.count > 1) {

                    guiGraphics.renderItemDecorations(parent.getFont(), e.stack, sx + 1, sy + 1, formatAmount(e.count));
                }
            }

            if (isOver(mx, my, sx, sy, S, S))
                guiGraphics.fill(sx + 1, sy + 1, sx + S - 1, sy + S - 1, 0x33FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!this.visible) return false;

        int i = slotAt(mx, my);
        if (i < 0) return false;

        SlotEntry e = entries.get(i);

        if (btn == 1) {
            if (!e.stack.isEmpty()) {

                parent.openEditor(mx, my, e, e.count);
                return true;
            }
        }

        if (btn == 2 && !e.stack.isEmpty()) {
            e.notConsumable = !e.notConsumable;
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!this.visible) return false;

        int i = slotAt(mx, my);
        if (i < 0) return false;
        SlotEntry e = entries.get(i);
        if (!e.stack.isEmpty()) {
            e.count = Math.max(1, e.count + (int) delta);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (!this.visible) return false;

        int i = slotAt(mx, my);
        if (i < 0) return false;

        Minecraft mc = Minecraft.getInstance();
        ItemStack carried = mc.player != null ? mc.player.containerMenu.getCarried() : ItemStack.EMPTY;

        if (!carried.isEmpty()) {
            SlotEntry e = entries.get(i);
            e.stack = carried.copy();
            e.count = carried.getCount();
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    public boolean acceptStack(ItemStack stack, double mx, double my) {
        int i = slotAt(mx, my);
        if (i < 0) {
            for (int j = 0; j < slotCount; j++) {
                if (entries.get(j).stack.isEmpty()) {
                    i = j;
                    break;
                }
            }
        }
        if (i < 0) return false;
        SlotEntry e = entries.get(i);
        e.stack = stack.copy();
        e.count = Math.max(1, stack.getCount());
        return true;
    }

    public ItemStack getStackUnderMouse(double mx, double my) {
        if (!this.visible) return ItemStack.EMPTY;

        int i = slotAt(mx, my);
        if (i >= 0 && i < entries.size()) {
            return entries.get(i).stack;
        }
        return ItemStack.EMPTY;
    }

    private int slotAt(double mx, double my) {
        for (int i = 0; i < slotCount; i++) {
            int sx = getX() + i * (S + GA);
            int sy = getY() + 10;
            if (isOver(mx, my, sx, sy, S, S)) return i;
        }
        return -1;
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderBorder(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int col) {
        guiGraphics.fill(x0, y0, x1, y0 + 1, col);
        guiGraphics.fill(x0, y1 - 1, x1, y1, col);
        guiGraphics.fill(x0, y0, x0 + 1, y1, col);
        guiGraphics.fill(x1 - 1, y0, x1, y1, col);
    }

    public void clear() {
        for (SlotEntry e : entries) {
            e.stack = ItemStack.EMPTY;
            e.count = 1;
            e.notConsumable = false;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput o) {
        defaultButtonNarrationText(o);
    }
}
