package net.phoenix.core.client.worldfx;

import net.minecraft.client.renderer.ShaderInstance;

import lombok.Setter;

@Setter
public abstract class PhoenixScreenEffect {

    protected float intensity = 1.0f;

    public void onAdd() {}

    public void onRemove() {}

    public abstract net.minecraft.client.renderer.ShaderInstance getShader();

    public abstract void uploadUniforms(float partialTick);

    public int priority() {
        return 0;
    }
}
