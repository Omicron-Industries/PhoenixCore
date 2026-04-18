package net.phoenix.core.integration.ars_nouveau.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Random;

public class SourceHatchScreen extends AbstractContainerScreen<SourceHatchMenu> {

    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");

    private int[] noise;
    private int noiseW = 256;
    private int noiseH = 256;

    public SourceHatchScreen(SourceHatchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 0;
        this.imageHeight = 0;
    }

    @Override
    protected void init() {
        super.init();

        int bw = 140, bh = 20;
        int bx = (this.width - bw) / 2;
        int by = this.height - 28;

        addRenderableWidget(Button.builder(Component.literal("Exit"), b -> onClose())
                .bounds(bx, by, bw, bh)
                .build());

        generateNoise();
    }

    private void generateNoise() {
        noise = new int[noiseW * noiseH];
        Random r = new Random(1337L);
        for (int i = 0; i < noise.length; i++) {
            int v = r.nextInt(256);
            noise[i] = v;
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);

        int cur = menu.getCurrent();
        int max = Math.max(1, menu.getMax());
        int rate = menu.getRate();
        int pct = (int) ((cur * 100L) / max);

        int cx = this.width / 2;
        int top = 28;

        gg.drawCenteredString(this.font, "Source Hatch", cx, top, 0xE6E6FF);
        gg.drawCenteredString(this.font, "Source: " + cur + " / " + max + " (" + pct + "%)", cx, top + 14, 0xFFFFFF);
        gg.drawCenteredString(this.font, "Rate: " + rate + "/s", cx, top + 28, 0xC8C8FF);

        drawOrbGauge(gg, cx, this.height / 2 + 10, 46, (float) cur / (float) max);
    }

    @Override
    public void renderBackground(GuiGraphics gg) {
        gg.fill(0, 0, this.width, this.height, 0xFF060712);

        float t = (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0) +
                Minecraft.getInstance().getFrameTime();
        int ox = (int) (t * 2) & 255;
        int oy = (int) (t * 1) & 255;

        int step = 6;
        for (int y = 0; y < this.height; y += step) {
            for (int x = 0; x < this.width; x += step) {
                int nx = (x + ox) & 255;
                int ny = (y + oy) & 255;
                int v = noise[nx + ny * noiseW];

                int a = (v * 70) / 255;
                int col = (a << 24) | 0x6A3DFF;
                gg.fill(x, y, x + step, y + step, col);
            }
        }

        RenderSystem.enableBlend();
        gg.blit(VIGNETTE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();
    }

    private void drawOrbGauge(GuiGraphics gg, int cx, int cy, int r, float fill) {
        fill = Math.max(0f, Math.min(1f, fill));

        gg.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy + r + 2, 0x66000000);

        gg.fill(cx - r, cy - r, cx + r, cy + r, 0xAA0C0D22);

        int h = (int) (2 * r * fill);
        int y0 = cy + r - h;
        gg.fill(cx - r, y0, cx + r, cy + r, 0xAA6A3DFF);

        gg.fill(cx - r, cy - r, cx - r + 6, cy + r, 0x22000000);
        gg.fill(cx - r, cy - r, cx + r, cy - r + 6, 0x22000000);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {}
}
