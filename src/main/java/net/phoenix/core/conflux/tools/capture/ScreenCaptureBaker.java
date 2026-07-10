package net.phoenix.core.conflux.tools.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * Wraps any {@link Screen} subclass as a single-frame {@link CaptureBakeable}.
 *
 * <p>Usage:
 * <pre>{@code
 * SpriteCaptureRegistry.register(new ScreenCaptureBaker(
 *     "theme_editor_preview",
 *     854, 480,
 *     () -> new PhantasiaThemeEditorScreen(null)
 * ));
 * }</pre>
 *
 * The screen is initialised once at bake dimensions. Mouse is positioned off-screen
 * (-1, -1) so no hover state is active. partialTick is 0.
 */
public final class ScreenCaptureBaker implements CaptureBakeable {

    private final String id;
    private final int width;
    private final int height;
    private final Supplier<Screen> factory;

    public ScreenCaptureBaker(String id, int width, int height, Supplier<Screen> factory) {
        this.id      = id;
        this.width   = width;
        this.height  = height;
        this.factory = factory;
    }

    @Override public String id()         { return id; }
    @Override public int frameCount()    { return 1; }
    @Override public int frameWidth()    { return width; }
    @Override public int frameHeight()   { return height; }

    @Override
    public void renderFrame(GuiGraphics g, int frame, float t, int w, int h) {
        Screen screen = factory.get();
        screen.init(Minecraft.getInstance(), w, h);
        screen.render(g, -1, -1, 0f);
    }
}
