package net.phoenix.core.client.worldfx.builtin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.SkyRenderContext;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public class NebulaSkyLayer extends PhoenixSkyLayer {

    private final float[] primaryColor;
    private final float[] secondaryColor;
    private final float density;
    private final float scale;
    private final float seed;

    public NebulaSkyLayer(float[] primaryColor, float[] secondaryColor,
                          float density, float scale, float seed) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.density = density;
        this.scale = scale;
        this.seed = seed;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void render(SkyRenderContext ctx) {
        ShaderInstance shader = WorldFXShaders.NEBULA;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(ctx.poseStack().last().pose()).invert();
        Matrix4f invProj = new Matrix4f(ctx.projectionMatrix()).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("PrimaryColor").set(primaryColor[0], primaryColor[1], primaryColor[2]);
        shader.safeGetUniform("SecondaryColor").set(secondaryColor[0], secondaryColor[1], secondaryColor[2]);
        shader.safeGetUniform("Density").set(density * intensity);
        shader.safeGetUniform("Scale").set(scale);
        shader.safeGetUniform("Seed").set(seed);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 10000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void drawNdcQuad() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
    }
}
