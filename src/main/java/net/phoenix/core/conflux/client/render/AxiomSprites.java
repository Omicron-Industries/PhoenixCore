package net.phoenix.core.conflux.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;

/**
 * Blitter for pre-baked Axiom sprite sheets.
 *
 * Each sheet is a horizontal strip: frame 0 at x=0, frame 1 at x=S, etc.
 * All frames are S×S (512×512) pixels.
 *
 * Usage pattern:
 *   // Background — pick frame by time, blit scaled to canvas:
 *   int frame = (int)(time * FPS) % FRAMES;
 *   AxiomSprites.blitFrame(g, VOID_DISK, frame, 32, 0, 0, canvasW, canvasH, alpha);
 *
 *   // Static sheet (1 frame):
 *   AxiomSprites.blitFrame(g, VOID_STARS, 0, 1, 0, 0, canvasW, canvasH, alpha);
 */
public final class AxiomSprites {

    public static final int S = 512; // frame size

    // Sheet descriptors
    public static final Sheet VOID_STARS   = new Sheet("void_stars",   1);
    public static final Sheet VOID_DISK    = new Sheet("void_disk",   32);
    public static final Sheet PHOENIX_BG   = new Sheet("phoenix_bg",  16);
    public static final Sheet SCULK_VEINS  = new Sheet("sculk_veins",  1);
    public static final Sheet SEALED_DOC   = new Sheet("sealed_doc",   1);

    private AxiomSprites() {}

    public record Sheet(String name, int frames) {
        public ResourceLocation location() {
            return new ResourceLocation(PhoenixCore.MOD_ID,
                "textures/gui/axiom/" + name + ".png");
        }
    }

    /**
     * Blit one frame of a sprite sheet, stretched to fill (destX,destY,destW,destH).
     *
     * @param sheet     which sprite sheet
     * @param frame     frame index [0, sheet.frames)
     * @param destX     destination X in current pose space
     * @param destY     destination Y in current pose space
     * @param destW     destination width
     * @param destH     destination height
     * @param alpha     [0,1] overall opacity multiplier
     */
    public static void blitFrame(GuiGraphics g, Sheet sheet, int frame,
                                  int destX, int destY, int destW, int destH,
                                  float alpha) {
        frame = Math.max(0, Math.min(sheet.frames-1, frame));
        ResourceLocation loc = sheet.location();

        // UV coordinates within the texture atlas (each frame is 1/FRAMES wide)
        float totalW = sheet.frames * S;
        float u0 = (frame * S) / totalW;
        float u1 = ((frame + 1) * S) / totalW;
        float v0 = 0f, v1 = 1f;

        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // blit(rl, destX, destY, destW, destH, u, v, srcW, srcH, texW, texH)
        int texW = sheet.frames() * S;
        g.blit(loc,
               destX, destY, destW, destH,
               u0 * texW, 0f, S, S,
               texW, S);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /**
     * Blit a frame centered and rotated (for spinning disks, ray backgrounds).
     *
     * @param angle rotation in radians, applied around (cx, cy)
     * @param scale uniform scale
     */
    public static void blitFrameRotated(GuiGraphics g, Sheet sheet, int frame,
                                         int cx, int cy, float scale, float angle,
                                         float alpha) {
        int half = (int)(S * scale / 2);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(new org.joml.Quaternionf().rotationZ(angle));
        g.pose().translate(-half, -half, 0);
        blitFrame(g, sheet, frame, 0, 0, half*2, half*2, alpha);
        g.pose().popPose();
    }

    /**
     * Convenience: pick frame from a continuous time value.
     * fps = how many frames per second of animation.
     */
    public static int timeToFrame(float time, float fps, int totalFrames) {
        return (int)(time * fps) % totalFrames;
    }
}
