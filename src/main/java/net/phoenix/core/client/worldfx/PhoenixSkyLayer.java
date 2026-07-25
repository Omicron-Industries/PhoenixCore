package net.phoenix.core.client.worldfx;

public abstract class PhoenixSkyLayer {

    protected float intensity = 1.0f;

    public void onAdd() {}

    public void onRemove() {}

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public abstract void render(SkyRenderContext ctx);

    public int priority() {
        return 50;
    }
}
