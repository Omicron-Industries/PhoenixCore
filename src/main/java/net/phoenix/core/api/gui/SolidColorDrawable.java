package net.phoenix.core.api.gui;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SolidColorDrawable implements IDrawable {

    private final int argbColor;

    public SolidColorDrawable(int argbColor) {
        this.argbColor = argbColor;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        context.getGraphics().fill(x, y, x + width, y + height, argbColor);
    }
}