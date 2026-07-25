package net.phoenix.core.integration.ars_nouveau.client.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
public class SourceHatchBackground implements IDrawable {

    private static final int BG_COLOR_A = 0xFF0a050f;
    private static final int BG_COLOR_B = 0xFF050208;
    private static final int PURPLE_MIST = 0x8F00FF;
    private static final int GRID_COLOR = 0x0AFFFFFF;

    private final int borderColor;

    public SourceHatchBackground(int borderColor) {
        this.borderColor = borderColor;
    }

    public SourceHatchBackground() {
        this(0xAABB66FF);
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        var graphics = context.getGraphics();

        graphics.fillGradient(x, y, x + width, y + height, BG_COLOR_A, BG_COLOR_B);

        int spacing = 16;
        for (int gx = x + spacing; gx < x + width; gx += spacing)
            graphics.fill(gx, y, gx + 1, y + height, GRID_COLOR);
        for (int gy = y + spacing; gy < y + height; gy += spacing)
            graphics.fill(x, gy, x + width, gy + 1, GRID_COLOR);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        long t = System.currentTimeMillis();
        int ox = (int) ((t / 50) & 255);
        int oy = (int) ((t / 70) & 255);
        int step = 4;
        for (int py = 0; py < height; py += step) {
            for (int px = 0; px < width; px += step) {
                int nx = (px + ox) & 255;
                int ny = (py + oy) & 255;
                int v = ((nx * 734287 + ny * 912271) ^ (nx * 31 + ny * 17)) & 255;
                int alpha = (v * 35) / 255;
                int col = (alpha << 24) | (PURPLE_MIST & 0xFFFFFF);
                graphics.fill(x + px, y + py, x + px + step, y + py + step, col);
            }
        }
        RenderSystem.disableBlend();

        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
