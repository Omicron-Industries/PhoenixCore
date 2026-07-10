package net.phoenix.core.conflux.tools.capture.bakers;

import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.conflux.client.render.RenderContext;
import net.phoenix.core.conflux.client.render.discipline.VoidDisciplineRenderer;
import net.phoenix.core.conflux.tools.capture.BakeRenderContext;
import net.phoenix.core.conflux.tools.capture.CaptureBakeable;

/**
 * Bakes the Void discipline background (star field + rotating accretion disk +
 * dust lanes + braid rings) into a 32-frame sprite sheet.
 *
 * One animation period for the disk ≈ 2π / 0.19 ≈ 33.1 s.
 * 32 frames covers one full rotation at ~1.03 s per frame.
 */
public final class AxiomVoidBaker implements CaptureBakeable {

    private static final int   FRAMES       = 32;
    private static final int   SIZE         = 512;
    private static final float PERIOD       = (float)(2 * Math.PI / 0.19f); // one full ring rotation
    private static final float DT_PER_FRAME = PERIOD / FRAMES;

    @Override public String id()       { return "void_bg"; }
    @Override public int frameCount()  { return FRAMES; }
    @Override public int frameWidth()  { return SIZE; }
    @Override public int frameHeight() { return SIZE; }

    @Override
    public void renderFrame(GuiGraphics g, int frame, float t, int w, int h) {
        VoidDisciplineRenderer renderer = new VoidDisciplineRenderer();
        renderer.onActivate(null);

        // Advance the renderer to the right animation phase for this frame
        float elapsed = frame * DT_PER_FRAME;
        RenderContext ctx = BakeRenderContext.of(w, h, elapsed);
        renderer.tick(elapsed, ctx);

        renderer.renderBackground(g, ctx);
    }
}
