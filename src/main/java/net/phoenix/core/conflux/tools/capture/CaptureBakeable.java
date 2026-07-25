package net.phoenix.core.conflux.tools.capture;

import net.minecraft.client.gui.GuiGraphics;

public interface CaptureBakeable {

    String id();

    int frameCount();

    int frameWidth();

    int frameHeight();

    void renderFrame(GuiGraphics g, int frame, float t, int w, int h);
}
