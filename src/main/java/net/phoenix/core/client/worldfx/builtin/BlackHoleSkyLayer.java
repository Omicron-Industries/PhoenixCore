package net.phoenix.core.client.worldfx.builtin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.SkyRenderContext;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class BlackHoleSkyLayer extends PhoenixSkyLayer {

    private final java.util.function.Supplier<Vec3> worldPosSupplier;
    private final float eventHorizonRadius;
    private final float lensingStrength;
    private final float diskBrightness;

    public BlackHoleSkyLayer(java.util.function.Supplier<Vec3> worldPosSupplier,
                             float eventHorizonRadius,
                             float lensingStrength,
                             float diskBrightness) {
        this.worldPosSupplier = worldPosSupplier;
        this.eventHorizonRadius = eventHorizonRadius;
        this.lensingStrength = lensingStrength;
        this.diskBrightness = diskBrightness;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public void render(SkyRenderContext ctx) {
        ShaderInstance shader = WorldFXShaders.BLACK_HOLE;
        if (shader == null) return;

        int skyTexId = WorldFXManager.captureSkyToTexture();

        float[] screenPos = projectToScreen(worldPosSupplier.get(), ctx);
        if (screenPos == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        float aspectRatio = (float) w / h;

        mc.getMainRenderTarget().bindWrite(false);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        shader.setSampler("InSampler", skyTexId);
        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("BlackHoleScreenPos").set(screenPos[0], screenPos[1]);
        shader.safeGetUniform("EventHorizonRadius").set(eventHorizonRadius);
        shader.safeGetUniform("LensingStrength").set(lensingStrength * intensity);
        shader.safeGetUniform("AccretionDiskBrightness").set(diskBrightness * intensity);
        shader.safeGetUniform("AspectRatio").set(aspectRatio);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 1000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.enableDepthTest();
    }

    private static float[] projectToScreen(Vec3 world, SkyRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = ctx.cameraPos();

        double rx = world.x - cam.x;
        double ry = world.y - cam.y;
        double rz = world.z - cam.z;

        Matrix4f view = new Matrix4f(ctx.poseStack().last().pose());
        Vector4f clip = new Vector4f((float) rx, (float) ry, (float) rz, 1.0f).mul(view).mul(ctx.projectionMatrix());

        if (clip.w <= 0.01f) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        return new float[] { ndcX * 0.5f + 0.5f, ndcY * 0.5f + 0.5f };
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
