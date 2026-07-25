package net.phoenix.core.conflux.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;

import com.mojang.blaze3d.systems.RenderSystem;

public final class AxiomSprites {

    public static final int S = 512;

    public static final Sheet VOID_STARS = new Sheet("void_stars", 1);
    public static final Sheet VOID_DISK = new Sheet("void_disk", 32);
    public static final Sheet PHOENIX_BG = new Sheet("phoenix_bg", 16);
    public static final Sheet SCULK_VEINS = new Sheet("sculk_veins", 1);
    public static final Sheet SEALED_DOC = new Sheet("sealed_doc", 1);

    private AxiomSprites() {}

    public record Sheet(String name, int frames) {

        public ResourceLocation location() {
            return new ResourceLocation(PhoenixCore.MOD_ID,
                    "textures/gui/axiom/" + name + ".png");
        }
    }

    public static void blitFrame(GuiGraphics g, Sheet sheet, int frame,
                                 int destX, int destY, int destW, int destH,
                                 float alpha) {
        frame = Math.max(0, Math.min(sheet.frames - 1, frame));
        ResourceLocation loc = sheet.location();

        float totalW = sheet.frames * S;
        float u0 = (frame * S) / totalW;
        float u1 = ((frame + 1) * S) / totalW;
        float v0 = 0f, v1 = 1f;

        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int texW = sheet.frames() * S;
        g.blit(loc,
                destX, destY, destW, destH,
                u0 * texW, 0f, S, S,
                texW, S);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void blitFrameRotated(GuiGraphics g, Sheet sheet, int frame,
                                        int cx, int cy, float scale, float angle,
                                        float alpha) {
        int half = (int) (S * scale / 2);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(new org.joml.Quaternionf().rotationZ(angle));
        g.pose().translate(-half, -half, 0);
        blitFrame(g, sheet, frame, 0, 0, half * 2, half * 2, alpha);
        g.pose().popPose();
    }

    public static int timeToFrame(float time, float fps, int totalFrames) {
        return (int) (time * fps) % totalFrames;
    }
}
