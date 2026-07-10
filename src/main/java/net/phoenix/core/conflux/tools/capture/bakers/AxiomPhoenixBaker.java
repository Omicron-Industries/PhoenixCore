package net.phoenix.core.conflux.tools.capture.bakers;

import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.conflux.client.render.RenderContext;
import net.phoenix.core.conflux.client.render.discipline.PhoenixDisciplineRenderer;
import net.phoenix.core.conflux.tools.capture.BakeRenderContext;
import net.phoenix.core.conflux.tools.capture.CaptureBakeable;

/**
 * Bakes the Phoenix discipline background (radial heat gradient + rotating ray arms)
 * into a 16-frame sprite sheet.
 *
 * One animation period for the rays ≈ 2π / 0.09 ≈ 69.8 s.
 * 16 frames covers one full rotation at ~4.36 s per frame.
 */
public final class AxiomPhoenixBaker implements CaptureBakeable {

    private static final int   FRAMES       = 16;
    private static final int   SIZE         = 512;
    private static final float PERIOD       = (float)(2 * Math.PI / 0.09f);
    private static final float DT_PER_FRAME = PERIOD / FRAMES;

    @Override public String id()       { return "phoenix_bg"; }
    @Override public int frameCount()  { return FRAMES; }
    @Override public int frameWidth()  { return SIZE; }
    @Override public int frameHeight() { return SIZE; }

    @Override
    public void renderFrame(GuiGraphics g, int frame, float t, int w, int h) {
        PhoenixDisciplineRenderer renderer = new PhoenixDisciplineRenderer();
        renderer.onActivate(null);

        float elapsed = frame * DT_PER_FRAME;
        RenderContext ctx = BakeRenderContext.of(w, h, elapsed);
        renderer.tick(elapsed, ctx);

        renderer.renderBackground(g, ctx);
    }
}
