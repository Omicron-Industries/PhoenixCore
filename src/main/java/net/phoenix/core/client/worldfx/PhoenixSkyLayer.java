package net.phoenix.core.client.worldfx;

/**
 * A visual layer rendered into the sky after vanilla sky (sun/moon/stars).
 *
 * <h2>Ordering</h2>
 * Layers are drawn in ascending {@link #priority()} order.  Lower priority = further
 * "back" in the sky.  Nebulae should be low (e.g. 0), the black hole higher (e.g. 100)
 * so it distorts the nebula behind it.
 *
 * <h2>Intensity</h2>
 * {@link #setIntensity} is called by the manager each frame with a 0→1 value derived
 * from the player's distance to the emitter.  Fade in/out logic lives here so each
 * layer can decide how to blend — don't apply the fade in the manager.
 *
 * <h2>Lifecycle</h2>
 * {@link #onAdd} is called when the layer becomes active (player enters radius or
 * machine loads for global effects). {@link #onRemove} fires on the reverse.
 * These are good places to pre-allocate GL resources.
 */
public abstract class PhoenixSkyLayer {

    protected float intensity = 1.0f;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Called when this layer is first registered / becomes active. */
    public void onAdd() {}

    /** Called when this layer is removed or becomes inactive. Free GL resources here. */
    public void onRemove() {}

    // ── Per-frame ─────────────────────────────────────────────────────────────

    /**
     * Updated every frame before {@link #render}.
     * @param intensity 0 = not active, 1 = fully active, values between = fade zone
     */
    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    /**
     * Draw this layer into the sky.
     *
     * <p>GL state at entry: depth writes OFF, no depth test, alpha blending enabled.
     * The pose stack is in camera space.  You are responsible for your own GL state
     * cleanup — don't leave the blending mode changed.
     *
     * <p>If your layer needs to capture and distort the current framebuffer (e.g. the
     * black hole), do that here via {@link WorldFXManager#captureSkyToTexture}.
     */
    public abstract void render(SkyRenderContext ctx);

    // ── Metadata ──────────────────────────────────────────────────────────────

    /**
     * Draw order.  Lower = drawn first (further back).
     * Standard values: 0 nebula, 50 custom stars, 100 black hole / distortion.
     */
    public int priority() {
        return 50;
    }
}
