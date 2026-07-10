package net.phoenix.core.api.gui;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A minimal rectangular border IDrawable: four thin edge fills, same technique as
 * SourceHatchBackground's border (it draws its border with four graphics.fill(...) calls along
 * the edges rather than a stroke/outline primitive).
 */
@OnlyIn(Dist.CLIENT)
public class BorderDrawable implements IDrawable {

    private final int argbColor;
    private final int thickness;

    public BorderDrawable(int argbColor, int thickness) {
        this.argbColor = argbColor;
        this.thickness = thickness;
    }

    public BorderDrawable(int argbColor) {
        this(argbColor, 1);
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        var graphics = context.getGraphics();
        graphics.fill(x, y, x + width, y + thickness, argbColor);
        graphics.fill(x, y + height - thickness, x + width, y + height, argbColor);
        graphics.fill(x, y, x + thickness, y + height, argbColor);
        graphics.fill(x + width - thickness, y, x + width, y + height, argbColor);
    }
}