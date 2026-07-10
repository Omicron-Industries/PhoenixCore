package net.phoenix.core.client.worldfx;

import net.minecraft.client.shader.ShaderInstance;

/**
 * A full-screen shader pass applied AFTER the entire world (sky + terrain + entities)
 * has been rendered, but before the HUD.
 *
 * <p>The pass reads from the main render target (the world as rendered so far) and
 * writes its output back, replacing the frame.  Multiple effects chain in
 * {@link #priority()} order.
 *
 * <h2>Usage</h2>
 * Override {@link #getShader()} to return your loaded {@link ShaderInstance} and
 * override {@link #uploadUniforms(float)} to push per-frame data (soul level, time,
 * machine position, etc.) into shader uniforms before the draw call.
 *
 * <p>The manager handles framebuffer management and the fullscreen quad draw — you
 * only need to worry about the shader and its uniforms.
 */
public abstract class PhoenixScreenEffect {

    protected float intensity = 1.0f;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void onAdd() {}

    public void onRemove() {}

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    // ── Per-frame ─────────────────────────────────────────────────────────────

    /**
     * Return the loaded {@link ShaderInstance} for this effect.
     * Return null to skip this effect this frame (e.g. while the shader is loading).
     */
    public abstract net.minecraft.client.renderer.ShaderInstance getShader();

    /**
     * Push per-frame uniforms into the shader before the draw call.
     * The "InSampler" sampler uniform is already bound to the captured world framebuffer
     * — don't rebind it here.
     *
     * @param partialTick interpolation factor (0–1) between the last two server ticks
     */
    public abstract void uploadUniforms(float partialTick);

    // ── Metadata ──────────────────────────────────────────────────────────────

    /**
     * Application order.  Lower = applied first.
     * Standard values: 0 color grade, 50 bloom, 100 chromatic aberration.
     */
    public int priority() {
        return 0;
    }
}
