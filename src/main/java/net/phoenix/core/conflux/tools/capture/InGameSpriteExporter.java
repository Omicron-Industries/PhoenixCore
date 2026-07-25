package net.phoenix.core.conflux.tools.capture;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class InGameSpriteExporter {

    private InGameSpriteExporter() {}

    public static void exportAll(Path outputDir) {
        int count = 0;
        for (CaptureBakeable b : SpriteCaptureRegistry.all()) {
            try {
                exportInternal(b, outputDir);
                count++;
            } catch (Exception e) {
                sendOverlay("§cFailed [" + b.id() + "]: " + e.getMessage());
            }
        }
        sendOverlay("§aExported " + count + " sprite(s) to " + outputDir);
    }

    public static void export(CaptureBakeable b, Path outputDir) {
        try {
            exportInternal(b, outputDir);
            sendOverlay("§aExported: " + b.id() + ".png");
        } catch (Exception e) {
            sendOverlay("§cExport failed [" + b.id() + "]: " + e.getMessage());
        }
    }

    public static Path defaultOutputDir() {
        return Paths.get("src/main/resources/assets/phoenixcore/textures/gui/axiom");
    }

    private static void exportInternal(CaptureBakeable b, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        int fw = b.frameWidth(), fh = b.frameHeight(), fc = b.frameCount();
        NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, fw * fc, fh, false);
        clearNativeImage(sheet);

        for (int f = 0; f < fc; f++) {
            float t = fc > 1 ? (float) f / fc : 0f;
            NativeImage frame = renderToImage(b, f, t);
            copyFrameIntoSheet(frame, sheet, f, fw, fh);
            frame.close();
        }

        Path out = outputDir.resolve(b.id() + ".png");
        sheet.writeToFile(out);
        sheet.close();
    }

    static NativeImage renderToImage(CaptureBakeable b, int frame, float t) {
        Minecraft mc = Minecraft.getInstance();
        int w = b.frameWidth(), h = b.frameHeight();

        TextureTarget fbo = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        fbo.setClearColor(0f, 0f, 0f, 0f);
        fbo.clear(Minecraft.ON_OSX);
        fbo.bindWrite(true);

        RenderSystem.setProjectionMatrix(
                new Matrix4f().ortho(0f, w, h, 0f, -1000f, 1000f),
                VertexSorting.ORTHOGRAPHIC_Z
        );
        var mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        GuiGraphics gg = new GuiGraphics(mc, buf);
        b.renderFrame(gg, frame, t, w, h);
        gg.flush();

        mv.popPose();
        RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(true);

        RenderSystem.bindTexture(fbo.getColorTextureId());
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        img.downloadTexture(0, false);
        img.flipY(); 
        fbo.destroyBuffers();

        return img;
    }

    private static void copyFrameIntoSheet(NativeImage src, NativeImage dst, int frameIdx, int fw, int fh) {
        int xOff = frameIdx * fw;
        for (int y = 0; y < fh; y++)
            for (int x = 0; x < fw; x++)
                dst.setPixelRGBA(xOff + x, y, src.getPixelRGBA(x, y));
    }

    private static void clearNativeImage(NativeImage img) {
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                img.setPixelRGBA(x, y, 0);
    }

    private static void sendOverlay(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null)
            mc.gui.setOverlayMessage(Component.literal(msg), false);
    }
}
