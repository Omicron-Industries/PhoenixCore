package net.phoenix.core.conflux.tools.capture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Implemented by anything that wants to pre-bake itself into a PNG sprite sheet.
 *
 * The exporter calls {@link #renderFrame} once per frame on the render thread
 * with an FBO bound. Frames are stitched left-to-right into a single sheet:
 * {@code width = frameWidth * frameCount, height = frameHeight}.
 */
public interface CaptureBakeable {

    /** Output filename without extension (e.g. "void_disk"). */
    String id();

    /** Number of animation frames. Use 1 for static sprites. */
    int frameCount();

    /** Pixel width of a single frame. */
    int frameWidth();

    /** Pixel height of a single frame. */
    int frameHeight();

    /**
     * Render one frame into {@code g}. Called on the render thread.
     *
     * @param g     GuiGraphics backed by the capture FBO
     * @param frame 0-indexed frame number
     * @param t     normalized time in [0, 1) across all frames
     * @param w     canvas width  (== frameWidth())
     * @param h     canvas height (== frameHeight())
     */
    void renderFrame(GuiGraphics g, int frame, float t, int w, int h);
}
