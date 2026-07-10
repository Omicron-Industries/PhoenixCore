package net.phoenix.core.client.worldfx.builtin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.SkyRenderContext;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Renders a black hole into the sky using screen-space gravitational lensing.
 *
 * <h2>What the shader does</h2>
 * <ol>
 *   <li>Captures the vanilla sky (stars, nebula, etc.) to a texture before distorting it.</li>
 *   <li>For each screen pixel, computes its angular distance from the black hole's projected
 *       position and applies a {@code 1/r²} bending deflection to the UV coordinates.</li>
 *   <li>Pixels inside the event horizon are pure black.</li>
 *   <li>A bright photon ring glows at ~1.5× the horizon radius.</li>
 *   <li>An animated accretion disk with Doppler brightening wraps the equatorial plane.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In your machine's createSkyLayer():
 * return new BlackHoleSkyLayer(
 *     () -> Vec3.atCenterOf(getBlockPos()),  // world position of the black hole
 *     0.05f,   // apparent screen radius of event horizon (0-1 of screen height)
 *     0.015f,  // lensing strength — higher = more extreme bending
 *     1.5f     // accretion disk brightness multiplier
 * );
 * }</pre>
 */
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
        return 100; // render last so it distorts everything behind it
    }

    @Override
    public void render(SkyRenderContext ctx) {
        ShaderInstance shader = WorldFXShaders.BLACK_HOLE;
        if (shader == null) return;

        // Capture the sky (vanilla + lower-priority layers) before we distort it
        int skyTexId = WorldFXManager.captureSkyToTexture();

        // Project the black hole's world position to screen UV [0,1]
        float[] screenPos = projectToScreen(worldPosSupplier.get(), ctx);
        if (screenPos == null) return; // behind camera

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        float aspectRatio = (float) w / h;

        // Write back to the main target
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
        shader.safeGetUniform("Time").set((float)(System.currentTimeMillis() % 1000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.enableDepthTest();
    }

    /** Projects a world position to screen UV [0,1] using the current matrices. Returns null if behind camera. */
    private static float[] projectToScreen(Vec3 world, SkyRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = ctx.cameraPos();

        // Relative to camera
        double rx = world.x - cam.x;
        double ry = world.y - cam.y;
        double rz = world.z - cam.z;

        // View matrix (modelview from pose stack)
        Matrix4f view = new Matrix4f(ctx.poseStack().last().pose());
        Vector4f clip = new Vector4f((float) rx, (float) ry, (float) rz, 1.0f).mul(view).mul(ctx.projectionMatrix());

        if (clip.w <= 0.01f) return null; // behind camera

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        // NDC [-1,1] → screen UV [0,1]
        return new float[]{ ndcX * 0.5f + 0.5f, ndcY * 0.5f + 0.5f };
    }

    private static void drawNdcQuad() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex( 1, -1, 0).endVertex();
        bb.vertex( 1,  1, 0).endVertex();
        bb.vertex(-1,  1, 0).endVertex();
        Tesselator.getInstance().end();
    }
}
