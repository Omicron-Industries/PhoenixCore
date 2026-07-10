package net.phoenix.core.conflux.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;

/**
 * Writes a non-rectangular silhouette into the GL stencil buffer so the
 * canvas is masked to an organic / torn / fractured shape rather than a
 * scissors rectangle.
 *
 * Usage (inside a DisciplineRenderer.renderBackground call, BEFORE drawing
 * any content):
 * <pre>
 *   CanvasMask.begin();
 *   CanvasMask.writeMask(maskVertices);   // fills stencil = 1 inside shape
 *   CanvasMask.enableTest();              // subsequent draws clip to stencil
 *   // ... draw canvas content ...
 *   CanvasMask.end();                     // restore normal GL state
 * </pre>
 *
 * Edge profiles are pre-computed as int[] of alternating x,y pairs in canvas
 * space (top edge clockwise, then bottom edge counter-clockwise). The stencil
 * polygon is drawn as a triangle fan from the canvas center.
 */
public final class CanvasMask {

    private CanvasMask() {}

    /** Begin stencil write pass. Call before drawing the mask polygon. */
    public static void begin() {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        // Write 1 to stencil wherever we draw the mask polygon
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        // Don't write to color buffer while writing stencil
        GL11.glColorMask(false, false, false, false);
    }

    /**
     * Draw the mask polygon. {@code poly} is a flat float[] of {x0,y0, x1,y1, ...}
     * in screen coordinates, representing the convex (or nearly convex)
     * visible area. Drawn as a triangle fan from the poly's centroid.
     */
    public static void writeMask(float[] poly) {
        if (poly.length < 6) return;

        // Centroid
        float cx = 0, cy = 0;
        int n = poly.length / 2;
        for (int i = 0; i < poly.length; i += 2) { cx += poly[i]; cy += poly[i+1]; }
        cx /= n; cy /= n;

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        buf.vertex(cx, cy, 0).endVertex();
        for (int i = 0; i < poly.length; i += 2) {
            buf.vertex(poly[i], poly[i + 1], 0).endVertex();
        }
        buf.vertex(poly[0], poly[1], 0).endVertex(); // close fan
        tess.end();
    }

    /**
     * Switch from stencil-write to stencil-test mode.
     * After this, only pixels where stencil == 1 will be rendered.
     */
    public static void enableTest() {
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0x00); // don't modify stencil during draw
    }

    /** Restore normal GL state. Call after all masked drawing is done. */
    public static void end() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        // Restore color mask in case it was left dirty
        GL11.glColorMask(true, true, true, true);
    }

    // ── Pre-built edge profiles ───────────────────────────────────────────────

    /**
     * Charred/torn edge for Phoenix — jagged top and bottom, heavier on the
     * top edge where "heat rises." Returns polygon in screen space.
     */
    public static float[] phoenixEdge(int x0, int y0, int x1, int y1) {
        // Top edge: jagged teeth pointing upward (charred tears)
        int steps = 32;
        float[] pts = new float[(steps + 1) * 4];
        int pi = 0;
        // Top edge left→right
        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float seed = MotionClock.hash((long)(i * 17L + 3));
            float tear = seed > 0.7f ? -(8f + seed * 18f) : -(seed * 4f);
            pts[pi++] = x;
            pts[pi++] = y0 + tear;
        }
        // Bottom edge right→left (mostly straight with minor char)
        for (int i = steps; i >= 0; i--) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float seed = MotionClock.hash((long)(i * 31L + 7));
            float tear = seed > 0.8f ? (4f + seed * 8f) : (seed * 2f);
            pts[pi++] = x;
            pts[pi++] = y1 + tear;
        }
        return pts;
    }

    /**
     * Fracture-crack edge for Void — straight segments interrupted by
     * angular jags as if the frame itself is cracked.
     */
    public static float[] voidEdge(int x0, int y0, int x1, int y1) {
        int steps = 28;
        float[] pts = new float[(steps + 1) * 4];
        int pi = 0;
        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float seed = MotionClock.hash((long)(i * 23L + 11));
            float crack = seed > 0.85f ? (seed - 0.85f) * 80f * (seed > 0.92f ? -1 : 1) : 0f;
            pts[pi++] = x + crack * 0.3f;
            pts[pi++] = y0 + crack;
        }
        for (int i = steps; i >= 0; i--) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float seed = MotionClock.hash((long)(i * 41L + 19));
            float crack = seed > 0.88f ? (seed - 0.88f) * 60f * (seed > 0.94f ? -1 : 1) : 0f;
            pts[pi++] = x + crack * 0.2f;
            pts[pi++] = y1 - crack;
        }
        return pts;
    }

    /**
     * Growth-boundary edge for Sculk — organic bumps, like mycelium
     * growing over the edge of the frame.
     */
    public static float[] sculkEdge(int x0, int y0, int x1, int y1) {
        int steps = 40;
        float[] pts = new float[(steps + 1) * 4];
        int pi = 0;
        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float s1 = MotionClock.hash((long)(i * 13L));
            float s2 = MotionClock.hash((long)(i * 29L + 5));
            float bump = (float)(Math.sin(t * Math.PI * 7 + s1) * s2 * 10f);
            pts[pi++] = x + bump * 0.4f;
            pts[pi++] = y0 + bump;
        }
        for (int i = steps; i >= 0; i--) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float s1 = MotionClock.hash((long)(i * 17L + 3));
            float bump = (float)(Math.sin(t * Math.PI * 6 + s1) * 6f);
            pts[pi++] = x;
            pts[pi++] = y1 - bump;
        }
        return pts;
    }
}
