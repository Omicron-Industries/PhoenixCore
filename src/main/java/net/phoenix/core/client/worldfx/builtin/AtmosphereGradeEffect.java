package net.phoenix.core.client.worldfx.builtin;

import net.minecraft.client.renderer.ShaderInstance;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import brachy.modularui.utils.FloatSupplier;

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

    public static AtmosphereGradeEffect saturationOnly(FloatSupplier saturation) {
        return new AtmosphereGradeEffect(saturation, () -> 0f,
                new float[] { 0f, 0f, 0f }, () -> 0f, () -> 1f);
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
        return 0;
    }
}
