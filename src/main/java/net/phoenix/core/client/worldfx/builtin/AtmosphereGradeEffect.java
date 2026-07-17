package net.phoenix.core.client.worldfx.builtin;

import brachy.modularui.utils.FloatSupplier;
import net.minecraft.client.renderer.ShaderInstance;

import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.WorldFXShaders;



/**
 * Full-screen colour grading driven by a live float parameter (e.g. "soul level").
 *
 * <p>Replaces the old {@code PhoenixShaders}/{@code soul_vision.json} approach.
 * This runs as a proper screen effect after all world rendering, not through
 * the {@code GameRenderer.loadEffect()} single-slot system.
 *
 * <h2>What the shader does</h2>
 * <ul>
 *   <li><b>Saturation</b> — blends from greyscale (0) to full colour (1).</li>
 *   <li><b>Tint</b> — additively mixes a colour over the screen (soul glow, etc.).</li>
 *   <li><b>Vignette</b> — darkens edges; strength 0 = none, 1 = heavy.</li>
 *   <li><b>Brightness</b> — uniform brightness scale (1 = neutral).</li>
 * </ul>
 *
 * <h2>Usage — soul level example</h2>
 * <pre>{@code
 * float soulLevel = 0.7f; // 0 = depleted, 1 = full
 * return new AtmosphereGradeEffect(
 *     () -> 0.3f + soulLevel * 0.7f,   // saturation: greyscale at 0 soul, vivid at 1
 *     () -> soulLevel,                  // tint strength
 *     new float[]{0.4f, 0.0f, 0.8f},   // tint colour: purple soul glow
 *     () -> 0.3f,                       // vignette always 0.3
 *     () -> 0.8f + soulLevel * 0.2f    // slightly darker at low soul
 * );
 * }</pre>
 */
public class AtmosphereGradeEffect extends PhoenixScreenEffect {

    private final FloatSupplier saturation;
    private final FloatSupplier tintStrength;
    private final float[] tintColor;
    private final FloatSupplier vignetteStrength;
    private final FloatSupplier brightness;

    public AtmosphereGradeEffect(FloatSupplier saturation,
                                 FloatSupplier tintStrength,
                                 float[] tintColor,
                                 FloatSupplier vignetteStrength,
                                 FloatSupplier brightness) {
        this.saturation = saturation;
        this.tintStrength = tintStrength;
        this.tintColor = tintColor;
        this.vignetteStrength = vignetteStrength;
        this.brightness = brightness;
    }

    /** Convenience: just a saturation effect with no tint, no vignette. */
    public static AtmosphereGradeEffect saturationOnly(FloatSupplier saturation) {
        return new AtmosphereGradeEffect(saturation, () -> 0f,
                new float[]{0f, 0f, 0f}, () -> 0f, () -> 1f);
    }

    @Override
    public ShaderInstance getShader() {
        return WorldFXShaders.ATMOSPHERE_GRADE;
    }

    @Override
    public void uploadUniforms(float partialTick) {
        ShaderInstance s = WorldFXShaders.ATMOSPHERE_GRADE;
        if (s == null) return;
        s.safeGetUniform("Saturation").set(saturation.getAsFloat() * intensity);
        s.safeGetUniform("TintStrength").set(tintStrength.getAsFloat() * intensity);
        s.safeGetUniform("TintColor").set(tintColor[0], tintColor[1], tintColor[2]);
        s.safeGetUniform("VignetteStrength").set(vignetteStrength.getAsFloat() * intensity);
        s.safeGetUniform("Brightness").set(brightness.getAsFloat());
    }

    @Override
    public int priority() {
        return 0; // apply first so other effects (bloom, etc.) layer on top
    }
}
