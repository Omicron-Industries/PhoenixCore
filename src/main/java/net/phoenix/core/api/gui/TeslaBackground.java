package net.phoenix.core.api.gui;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Custom IDrawable background for the Tesla Binder UI. Electric blue/cyan counterpart to
 * SourceHatchBackground's purple-mist style, but swaps the soft animated mist for jagged
 * animated lightning arcs, and pulses the border brightness instead of using a flat color.
 *
 * Technique: same as SourceHatchBackground (gradient -> grid -> animated overlay -> border, all
 * via context.getGraphics().fill(...)). The arc segments are drawn as thin rotated-looking
 * rectangles approximated with multiple short fill(...) calls along each zigzag segment, since no
 * confirmed line-drawing primitive exists on the graphics object (only fill, confirmed via
 * SourceHatchBackground) -- only axis-aligned rect fills are confirmed available, so each "bolt
 * segment" is built from a short run of small filled squares stepping from point A to point B
 * rather than a true stroked line. This keeps every draw call within the one confirmed primitive.
 */
@OnlyIn(Dist.CLIENT)
public class TeslaBackground implements IDrawable {

    private static final int BG_COLOR_A = 0xFF050912;
    private static final int BG_COLOR_B = 0xFF000005;
    private static final int GRID_COLOR = 0x0A40E0FF;
    private static final int ARC_COLOR = 0x9000D4FF;

    /** Base ARGB border color (alpha gets pulsed on top of this). */
    private final int borderColor;

    public TeslaBackground(int borderColor) {
        this.borderColor = borderColor;
    }

    /** Convenience: fixed electric-cyan border. */
    public TeslaBackground() {
        this(0x0090F0FF);
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        var graphics = context.getGraphics();

        // Gradient background (top -> bottom), deep blue-black.
        graphics.fillGradient(x, y, x + width, y + height, BG_COLOR_A, BG_COLOR_B);

        // Grid lines, cyan-tinted instead of white.
        int spacing = 16;
        for (int gx = x + spacing; gx < x + width; gx += spacing) {
            graphics.fill(gx, y, gx + 1, y + height, GRID_COLOR);
        }
        for (int gy = y + spacing; gy < y + height; gy += spacing) {
            graphics.fill(x, gy, x + width, gy + 1, GRID_COLOR);
        }

        // Animated lightning arcs, replacing the mist.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawArcs(context, x, y, width, height);
        RenderSystem.disableBlend();

        // Border, with a slow alpha pulse layered on top of the base color.
        long t = System.currentTimeMillis();
        float pulsePhase = (float) ((t % 1400) / 1400.0);
        float pulse = 0.55f + 0.45f * (float) Math.sin(pulsePhase * 2 * Math.PI);
        int baseAlpha = (borderColor >>> 24) & 0xFF;
        int pulsedAlpha = Math.max(0, Math.min(255, (int) (baseAlpha * pulse)));
        int pulsedBorder = (pulsedAlpha << 24) | (borderColor & 0x00FFFFFF);

        graphics.fill(x, y, x + width, y + 1, pulsedBorder);
        graphics.fill(x, y + height - 1, x + width, y + height, pulsedBorder);
        graphics.fill(x, y, x + 1, y + height, pulsedBorder);
        graphics.fill(x + width - 1, y, x + width, y + height, pulsedBorder);
    }

    /**
     * Draws a handful of jagged "lightning bolt" zigzags that jump to new random paths every
     * ~120ms, giving a flickering arc effect. Each bolt is a short chain of points; each segment
     * between consecutive points is approximated with stepped small square fills (see class doc
     * for why -- no confirmed line-stroke primitive).
     */
    private void drawArcs(GuiContext context, int x, int y, int width, int height) {
        long t = System.currentTimeMillis();
        long seed = t / 120L; // new arc layout roughly every 120ms
        Random rng = new Random(seed);

        int boltCount = 2 + rng.nextInt(2); // 2-3 bolts visible at once
        for (int b = 0; b < boltCount; b++) {
            drawSingleArc(context, rng, x, y, width, height);
        }
    }

    private void drawSingleArc(GuiContext context, Random rng,
                               int x, int y, int width, int height) {
        int segments = 5 + rng.nextInt(3);

        // Pick a start point on one edge and walk roughly toward the opposite side.
        boolean startTop = rng.nextBoolean();
        int startX = x + rng.nextInt(Math.max(1, width));
        int startY = startTop ? y : y + height;
        int endY = startTop ? y + height : y;

        int prevX = startX;
        int prevY = startY;

        for (int i = 1; i <= segments; i++) {
            float progress = (float) i / segments;
            int targetY = startY + (int) ((endY - startY) * progress);
            int jitter = 10 - (int) (6 * progress); // tighter jitter as it nears the end
            int targetX = prevX + rng.nextInt(jitter * 2 + 1) - jitter;
            targetX = Math.max(x, Math.min(x + width, targetX));

            stepFillLine(context, prevX, prevY, targetX, targetY);

            prevX = targetX;
            prevY = targetY;
        }
    }

    /**
     * Approximates a thin line from (x1,y1) to (x2,y2) using small 2x2 fills stepped along the
     * path. Crude but stays within the one confirmed fill(...) primitive.
     */
    private void stepFillLine(GuiContext context, int x1, int y1, int x2, int y2) {
        var graphics = context.getGraphics();
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)) / 2);

        for (int s = 0; s <= steps; s++) {
            float p = (float) s / steps;
            int px = x1 + (int) (dx * p);
            int py = y1 + (int) (dy * p);
            graphics.fill(px, py, px + 2, py + 2, ARC_COLOR);
        }
    }
}
