package net.phoenix.core.conflux.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;

public final class CanvasMask {

    private CanvasMask() {}

    public static void begin() {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        GL11.glColorMask(false, false, false, false);
    }

    public static void writeMask(float[] poly) {
        if (poly.length < 6) return;

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
        buf.vertex(poly[0], poly[1], 0).endVertex(); 
        tess.end();
    }

    public static void enableTest() {
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0x00); 
    }

    public static void end() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        
        GL11.glColorMask(true, true, true, true);
    }

    public static float[] phoenixEdge(int x0, int y0, int x1, int y1) {
        
        int steps = 32;
        float[] pts = new float[(steps + 1) * 4];
        int pi = 0;
        
        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            float x = x0 + (x1 - x0) * t;
            float seed = MotionClock.hash((long)(i * 17L + 3));
            float tear = seed > 0.7f ? -(8f + seed * 18f) : -(seed * 4f);
            pts[pi++] = x;
            pts[pi++] = y0 + tear;
        }
        
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
