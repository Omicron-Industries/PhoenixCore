package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class AmountEditor extends AbstractWidget {

    private final EditBox input;
    private final Button saveBtn;
    private final Button deleteBtn;
    private Object target;
    private Runnable onComplete;

    public AmountEditor(int x, int y, Font font) {
        super(x, y, 80, 40, Component.empty());
        this.input = new EditBox(font, x + 5, y + 5, 70, 12, Component.empty());
        this.saveBtn = Button.builder(Component.literal("Save"), b -> close(false))
                .bounds(x + 5, y + 22, 34, 14).build();
        this.deleteBtn = Button.builder(Component.literal("Del"), b -> close(true))
                .bounds(x + 41, y + 22, 34, 14).build();
        this.visible = false;
    }

    @Override
    public boolean charTyped(char code, int mod) {
        if (!visible) return false;

        if (Character.isDigit(code)) {
            return this.input.charTyped(code, mod);
        }
        return false;
    }

    public void open(int mx, int my, Object target, int currentAmt, Runnable onComplete) {
        this.setX(mx);
        this.setY(my);

        this.input.setPosition(mx + 5, my + 5);
        this.saveBtn.setPosition(mx + 5, my + 22);
        this.deleteBtn.setPosition(mx + 41, my + 22);

        this.target = target;
        this.onComplete = onComplete;
        this.input.setValue(String.valueOf(currentAmt));
        this.visible = true;
        this.input.setFocused(true);
        this.input.setHighlightPos(0);
    }

    private void close(boolean delete) {
        if (delete) {
            if (target instanceof FluidSlotPanel.FluidEntry e) e.fluidExpr = null;

            if (target instanceof SlotPanel.SlotEntry e) e.stack = net.minecraft.world.item.ItemStack.EMPTY;
        } else {
            int amt = 1;
            try {
                amt = Integer.parseInt(input.getValue());
            } catch (Exception ignored) {}
            if (target instanceof FluidSlotPanel.FluidEntry e) e.amount = amt;

            if (target instanceof SlotPanel.SlotEntry e) e.count = Math.max(1, amt);
        }
        this.visible = false;
        if (onComplete != null) onComplete.run();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible) return false;
        if (input.mouseClicked(mx, my, btn)) return true;
        if (saveBtn.mouseClicked(mx, my, btn)) return true;
        if (deleteBtn.mouseClicked(mx, my, btn)) return true;

        if (!isMouseOver(mx, my)) {
            this.visible = false;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (!visible) return false;

        if (key == 257) {
            close(false);
            return true;
        }
        if (key == 256) {
            this.visible = false;
            return true;
        }

        return this.input.keyPressed(key, scan, mod);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        if (!visible) return;
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
        renderBorder(g, getX(), getY(), getX() + width, getY() + height, 0xFF555555);
        input.render(g, mx, my, pt);
        saveBtn.render(g, mx, my, pt);
        deleteBtn.render(g, mx, my, pt);
    }

    private void renderBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) {}
}
