package net.phoenix.core.client.worldfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldFXManager {

    private WorldFXManager() {}

    private record ActiveEffect(
                                IWorldFXEmitter emitter,
                                PhoenixSkyLayer skyLayer,
                                PhoenixScreenEffect screenEffect) {}

    private static final Map<BlockPos, ActiveEffect> effects = new LinkedHashMap<>();

    public static void register(BlockPos pos, IWorldFXEmitter emitter) {
        PhoenixSkyLayer sky = emitter.createSkyLayer();
        PhoenixScreenEffect screen = emitter.createScreenEffect();
        if (sky != null) sky.onAdd();
        if (screen != null) screen.onAdd();
        effects.put(pos, new ActiveEffect(emitter, sky, screen));
    }

    public static void unregister(BlockPos pos) {
        ActiveEffect removed = effects.remove(pos);
        if (removed != null) {
            if (removed.skyLayer() != null) removed.skyLayer().onRemove();
            if (removed.screenEffect() != null) removed.screenEffect().onRemove();
        }
    }

    public static void renderSkyLayers(SkyRenderContext ctx) {
        Vec3 camPos = ctx.cameraPos();
        List<PhoenixSkyLayer> sorted = new ArrayList<>();

        for (Map.Entry<BlockPos, ActiveEffect> entry : effects.entrySet()) {
            ActiveEffect ae = entry.getValue();
            if (ae.skyLayer() == null) continue;

            float radius = ae.emitter().getEffectRadius();
            float intensity;
            if (radius < 0) {
                intensity = 1.0f;
            } else {
                double dist = camPos.distanceTo(Vec3.atCenterOf(entry.getKey()));
                if (dist > radius) continue;
                intensity = (float) (1.0 - dist / radius);
            }

            ae.skyLayer().setIntensity(intensity);
            sorted.add(ae.skyLayer());
        }

        sorted.sort(Comparator.comparingInt(PhoenixSkyLayer::priority));
        for (PhoenixSkyLayer layer : sorted) {
            layer.render(ctx);
        }
    }

    public static void applyScreenEffects(float partialTick) {
        Vec3 camPos = getCameraPos();
        if (camPos == null) return;

        List<PhoenixScreenEffect> sorted = new ArrayList<>();

        for (Map.Entry<BlockPos, ActiveEffect> entry : effects.entrySet()) {
            ActiveEffect ae = entry.getValue();
            if (ae.screenEffect() == null) continue;

            float radius = ae.emitter().getEffectRadius();
            float intensity;
            if (radius < 0) {
                intensity = 1.0f;
            } else {
                double dist = camPos.distanceTo(Vec3.atCenterOf(entry.getKey()));
                if (dist > radius) continue;
                intensity = (float) (1.0 - dist / radius);
            }

            ae.screenEffect().setIntensity(intensity);
            sorted.add(ae.screenEffect());
        }

        if (sorted.isEmpty()) return;
        sorted.sort(Comparator.comparingInt(PhoenixScreenEffect::priority));

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;

        ensureWorkTargets(w, h);

        blitToTarget(main, pingTarget);

        RenderTarget src = pingTarget;
        RenderTarget dst = pongTarget;

        for (PhoenixScreenEffect effect : sorted) {
            ShaderInstance shader = effect.getShader();
            if (shader == null) continue;

            dst.bindWrite(true);
            RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);

            RenderSystem.setShader(() -> shader);
            shader.setSampler("InSampler", src.getColorTextureId());
            shader.safeGetUniform("OutSize").set((float) w, (float) h);

            effect.uploadUniforms(partialTick);
            shader.apply();
            drawNdcQuad();
            shader.clear();

            RenderTarget tmp = src;
            src = dst;
            dst = tmp;
        }

        blitToTarget(src, main);
    }

    public static int captureSkyToTexture() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        ensureSkyCapture(main.width, main.height);
        blitToTarget(main, skyCapture);
        return skyCapture.getColorTextureId();
    }

    private static RenderTarget pingTarget;
    private static RenderTarget pongTarget;
    private static RenderTarget skyCapture;

    private static void ensureWorkTargets(int w, int h) {
        if (pingTarget == null || pingTarget.width != w || pingTarget.height != h) {
            if (pingTarget != null) pingTarget.destroyBuffers();
            if (pongTarget != null) pongTarget.destroyBuffers();
            pingTarget = new MainTarget(w, h);
            pongTarget = new MainTarget(w, h);
        }
    }

    private static void ensureSkyCapture(int w, int h) {
        if (skyCapture == null || skyCapture.width != w || skyCapture.height != h) {
            if (skyCapture != null) skyCapture.destroyBuffers();
            skyCapture = new MainTarget(w, h);
        }
    }

    private static void blitToTarget(RenderTarget src, RenderTarget dst) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, src.width, src.height,
                0, 0, dst.width, dst.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void drawNdcQuad() {
        RenderSystem.disableDepthTest();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.enableDepthTest();
    }

    private static Vec3 getCameraPos() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) return null;
        return mc.gameRenderer.getMainCamera().getPosition();
    }
}
