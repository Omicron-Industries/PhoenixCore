package net.phoenix.core.conflux.tools.capture.bakers;

import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.conflux.client.render.RenderContext;
import net.phoenix.core.conflux.client.render.discipline.SealedDisciplineRenderer;
import net.phoenix.core.conflux.tools.capture.BakeRenderContext;
import net.phoenix.core.conflux.tools.capture.CaptureBakeable;

public final class AxiomSealedBaker implements CaptureBakeable {

    private static final int SIZE = 512;

    @Override public String id()       { return "sealed_bg"; }
    @Override public int frameCount()  { return 1; }
    @Override public int frameWidth()  { return SIZE; }
    @Override public int frameHeight() { return SIZE; }

    @Override
    public void renderFrame(GuiGraphics g, int frame, float t, int w, int h) {
        SealedDisciplineRenderer renderer = new SealedDisciplineRenderer("sealed");
        renderer.onActivate(null);

        RenderContext ctx = BakeRenderContext.of(w, h, 0f);
        renderer.renderBackground(g, ctx);
    }
}
