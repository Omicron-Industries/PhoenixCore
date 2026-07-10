package net.phoenix.core.conflux.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.mixin.accessor.PostChainAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Manages the per-discipline post-process shader active while the Axiom
 * research terminal is open.
 */
public final class AxiomShaderManager {

    private static @Nullable ResourceLocation activeLocation = null;
    private static float elapsed = 0f;

    private AxiomShaderManager() {}

    public static void activate(@Nullable ResourceLocation location) {
        if (location == null) { deactivate(); return; }
        if (location.equals(activeLocation)) return;

        Minecraft mc = Minecraft.getInstance();
        try {
            mc.gameRenderer.loadEffect(location);
            activeLocation = location;
            elapsed = 0f;
        } catch (Exception e) {
            activeLocation = null;
        }
    }

    /** Advance Time uniform. */
    public static void tick(float dt) {
        if (activeLocation == null) return;
        elapsed += dt;
        pushUniform1f("Time", elapsed);
    }

    /**
     * Push per-frame live data into shader uniforms.
     *
     * @param screenW      screen pixel width
     * @param screenH      screen pixel height
     * @param cursorX      cursor screen pixel x
     * @param cursorY      cursor screen pixel y
     * @param nodeScreenXY flat array [x0,y0, x1,y1, ...] in screen pixels, up to 8 nodes
     * @param nodeCount    how many nodes are in nodeScreenXY
     * @param nodeStrength per-node heat/intensity [0,1], length = nodeCount (Phoenix only)
     * @param rippleOriginXY flat array [x0,y0, ...] of unlock ripple centres, screen pixels (Sculk only)
     * @param rippleAge    age [0,1] per ripple (Sculk only)
     * @param rippleCount  active ripple count (Sculk only)
     */
    public static void pushFrameData(
            float screenW, float screenH,
            float cursorX, float cursorY,
            float[] nodeScreenXY, int nodeCount,
            float[] nodeStrength,
            float[] rippleOriginXY, float[] rippleAge, int rippleCount
    ) {
        if (activeLocation == null) return;
        Minecraft mc = Minecraft.getInstance();
        PostChain effect = mc.gameRenderer.currentEffect();
        if (effect == null) return;

        List<PostPass> passes;
        try {
            passes = ((PostChainAccessor) effect).getPasses();
        } catch (Exception e) {
            return;
        }

        float invW = screenW > 0 ? 1f / screenW : 0f;
        float invH = screenH > 0 ? 1f / screenH : 0f;

        // Convert screen-pixel node positions → UV [0,1]
        int safeCount = Math.min(nodeCount, 8);
        float[] nodeUV = new float[16];
        for (int i = 0; i < safeCount; i++) {
            nodeUV[i * 2]     = nodeScreenXY[i * 2]     * invW;
            nodeUV[i * 2 + 1] = nodeScreenXY[i * 2 + 1] * invH;
        }

        // Convert ripple origins → UV
        int safeRipples = Math.min(rippleCount, 4);
        float[] rippleUV = new float[8];
        for (int i = 0; i < safeRipples; i++) {
            rippleUV[i * 2]     = rippleOriginXY[i * 2]     * invW;
            rippleUV[i * 2 + 1] = rippleOriginXY[i * 2 + 1] * invH;
        }

        float cursorU = cursorX * invW;
        float cursorV = cursorY * invH;

        for (PostPass pass : passes) {
            var eff = pass.getEffect();
            try {
                // Universal
                var ssSampler = eff.getUniform("ScreenSize");
                if (ssSampler != null) ssSampler.set(screenW, screenH);

                var cursor = eff.getUniform("CursorUV");
                if (cursor != null) cursor.set(cursorU, cursorV);

                // Phoenix-specific
                var heatCount = eff.getUniform("HeatCount");
                if (heatCount != null) heatCount.set(safeCount);

                var heatSrc = eff.getUniform("HeatSources");
                if (heatSrc != null) heatSrc.set(nodeUV);

                var heatStr = eff.getUniform("HeatStrength");
                if (heatStr != null) {
                    float[] str = new float[8];
                    for (int i = 0; i < safeCount; i++) str[i] = (nodeStrength != null && i < nodeStrength.length) ? nodeStrength[i] : 1f;
                    heatStr.set(str);
                }

                // Void / Sculk shared
                var nodeCountU = eff.getUniform("NodeCount");
                if (nodeCountU != null) nodeCountU.set(safeCount);

                var nodePos = eff.getUniform("NodePositions");
                if (nodePos != null) nodePos.set(nodeUV);

                // Sculk-specific
                var ripCnt = eff.getUniform("RippleCount");
                if (ripCnt != null) ripCnt.set(safeRipples);

                var ripOrig = eff.getUniform("RippleOrigins");
                if (ripOrig != null) ripOrig.set(rippleUV);

                var ripA = eff.getUniform("RippleAge");
                if (ripA != null) {
                    float[] ages = new float[4];
                    for (int i = 0; i < safeRipples; i++) ages[i] = rippleAge != null && i < rippleAge.length ? rippleAge[i] : 0f;
                    ripA.set(ages);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void applyPostChain() {}

    public static void deactivate() {
        if (activeLocation == null) return;
        try {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
        } catch (Exception ignored) {}
        activeLocation = null;
    }

    public static boolean isActive() { return activeLocation != null; }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static void pushUniform1f(String name, float value) {
        Minecraft mc = Minecraft.getInstance();
        PostChain effect = mc.gameRenderer.currentEffect();
        if (effect == null) return;
        try {
            for (PostPass pass : ((PostChainAccessor) effect).getPasses()) {
                var u = pass.getEffect().getUniform(name);
                if (u != null) u.set(value);
            }
        } catch (Exception ignored) {}
    }
}
