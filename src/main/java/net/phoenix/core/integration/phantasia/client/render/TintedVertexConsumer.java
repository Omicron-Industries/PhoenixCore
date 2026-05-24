package net.phoenix.core.integration.phantasia.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Thin {@link VertexConsumer} wrapper that applies a per-block alpha tint.
 *
 * Used by {@link PhantasiaWorldRenderer} to instantly hide or fade blocks
 * without requiring a VBO rebake — we just set {@code alpha} to 0 for blocks
 * that should be invisible, and interpolate toward 1 for blocks fading in.
 *
 * The offset fields replicate LDLib's VertexConsumerWrapper behaviour for
 * fluid chunk offsets (fluids need a sub-chunk origin shift applied to vertices).
 */
public final class TintedVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;

    // Per-fluid-chunk offset (cleared after each block)
    private double offsetX, offsetY, offsetZ;

    // Per-block tint — alpha is the key channel for show/hide transitions
    private float r = 1f, g = 1f, b = 1f, a = 1f;

    public TintedVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    // ── Tint control ──────────────────────────────────────────────────────────

    public void setAlpha(float alpha) {
        this.a = alpha;
    }

    public void setTint(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public void resetTint() {
        r = 1f;
        g = 1f;
        b = 1f;
        a = 1f;
    }

    // ── Fluid offset control ──────────────────────────────────────────────────

    public void addOffset(double ox, double oy, double oz) {
        offsetX += ox;
        offsetY += oy;
        offsetZ += oz;
    }

    public void clearOffset() {
        offsetX = 0;
        offsetY = 0;
        offsetZ = 0;
    }

    // ── VertexConsumer implementation ─────────────────────────────────────────

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return delegate.vertex(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return delegate.color(
                (int) (red * r),
                (int) (green * g),
                (int) (blue * b),
                (int) (alpha * a));
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return delegate.uv(u, v);
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return delegate.overlayCoords(u, v);
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return delegate.uv2(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        delegate.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
