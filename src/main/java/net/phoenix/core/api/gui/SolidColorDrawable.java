package net.phoenix.core.api.gui;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A minimal solid-color {@link IDrawable}. No built-in equivalent was found in brachy.modularui
 * (no ColorRectTexture/FlatColorDrawable or similar exists in the IDrawable interface itself or
 * anywhere else confirmed in this codebase), so this recreates the minimum needed: fill a
 * rectangle with a single ARGB color.
 *
 * Uses context.getGraphics().fill(...) -- confirmed via SourceHatchBackground, which calls the
 * same accessor for its gradient/grid/mist rendering. This resolves the earlier TODO: GuiContext
 * does expose graphics access, just via getGraphics() rather than a GuiGraphics-named accessor on
 * ModularGuiContext (which doesn't declare it itself -- it's on the GuiContext superclass).
 */
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