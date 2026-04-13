package net.phoenix.core.client.renderer.gui;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.common.machine.multiblock.part.special.SourceHatchPartMachine;

import com.mojang.blaze3d.systems.RenderSystem;

import javax.annotation.Nonnull;

public class SourceHatchFancyUIWidget extends FancyMachineUIWidget {

    private final SourceHatchPartMachine hatch;

    private static final int BG_COLOR_A = 0xFF0a050f;
    private static final int BG_COLOR_B = 0xFF050208;

    private static final int PURPLE_MIST = 0x8F00FF;
    private static final int BUTTON_BG = 0xB0100518;
    private static final int PURPLE_ACCENT = 0xFF8F00FF;

    public SourceHatchFancyUIWidget(IFancyUIProvider mainPage, int width, int height) {
        super(mainPage, width, height);
        this.hatch = (SourceHatchPartMachine) mainPage;

        setBackground((IGuiTexture) null);
    }

    @Override
    public void initWidget() {
        super.initWidget();

        addExitButton();
    }

    private void addExitButton() {
        int bw = 40, bh = 14;
        int bx = getPosition().x + getSize().width - 46;
        int by = getPosition().y + 6;

        IGuiTexture bg = new GuiTextureGroup(
                new ColorRectTexture(BUTTON_BG),
                new ColorBorderTexture(1, PURPLE_ACCENT));

        addWidget(new ButtonWidget(bx, by, bw, bh, bg, cd -> closeUIClientSide()));

        var font = Minecraft.getInstance().font;
        int tx = bx + (bw - font.width("Exit")) / 2;
        int ty = by + (bh - font.lineHeight) / 2;

        addWidget(new LabelWidget(tx, ty, "Exit"));
    }

    @OnlyIn(Dist.CLIENT)
    private void closeUIClientSide() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = getPosition().x;
        int y = getPosition().y;
        int w = getSize().width;
        int h = getSize().height;

        DrawerHelper.drawGradientRect(graphics, x, y, w, h, BG_COLOR_A, BG_COLOR_B, false);
        drawGridPattern(graphics, x, y, w, h);
        drawMist(graphics, x, y, w, h);

        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        drawOverlayHudForHomePageOnly(graphics);

        int accent = getAccentColor();
        DrawerHelper.drawBorder(graphics, x, y, w, h, (0xAA << 24) | (accent & 0xFFFFFF), 1);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawOverlayHudForHomePageOnly(GuiGraphics graphics) {
        if (currentPage != currentHomePage) return;
        if (pageContainer == null || pageContainer.widgets.isEmpty()) return;

        var page = pageContainer.widgets.get(0);

        int pageX = page.getPosition().x;
        int pageY = page.getPosition().y;
        int pageW = page.getSize().width;
        int pageH = page.getSize().height;

        int expand = 2;

        graphics.enableScissor(
                pageX - expand,
                pageY - expand,
                pageX + pageW + expand,
                pageY + pageH + expand);

        int padX = 8;
        int padY = 6;
        drawOverlayHUD(graphics, pageX + padX, pageY + padY, pageW - padX * 2);

        graphics.disableScissor();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawOverlayHUD(GuiGraphics graphics, int x, int y, int usableWidth) {
        var font = Minecraft.getInstance().font;

        int cur = hatch.getSourceContainer().getSource();
        int max = Math.max(1, hatch.getSourceContainer().getMaxSource());
        int rate = hatch.getSourceContainer().getTransferRate();
        int pct = (int) ((cur * 100L) / max);

        int accent = getAccentColor();
        int value = 0xFFE6E6FF;

        graphics.drawString(font, "SOURCE HATCH", x, y, accent, false);
        graphics.drawString(font, "MODE: " + hatch.getIo().name(), x, y + 20, value, false);
        graphics.drawString(font, "SOURCE: " + cur + " / " + max + " (" + pct + "%)", x, y + 34, value, false);
        graphics.drawString(font, "RATE: " + rate + "/s", x, y + 48, value, false);

        int lineY = y + 64;
        int lineW = Math.max(10, usableWidth);
        graphics.fill(x, lineY, x + lineW, lineY + 1, (0x66 << 24) | (accent & 0xFFFFFF));
    }

    @OnlyIn(Dist.CLIENT)
    private void drawMist(GuiGraphics graphics, int x, int y, int w, int h) {
        long t = System.currentTimeMillis();
        int ox = (int) ((t / 50) & 255);
        int oy = (int) ((t / 70) & 255);
        int step = 4;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int py = 0; py < h; py += step) {
            for (int px = 0; px < w; px += step) {
                int nx = (px + ox) & 255;
                int ny = (py + oy) & 255;
                int v = ((nx * 734287 + ny * 912271) ^ (nx * 31 + ny * 17)) & 255;

                int alpha = (v * 35) / 255;
                int col = (alpha << 24) | (PURPLE_MIST & 0xFFFFFF);

                graphics.fill(x + px, y + py, x + px + step, y + py + step, col);
            }
        }

        RenderSystem.disableBlend();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawGridPattern(GuiGraphics graphics, int x, int y, int w, int h) {
        int gridColor = 0x0AFFFFFF;
        int spacing = 16;

        for (int gx = x + spacing; gx < x + w; gx += spacing) {
            graphics.fill(gx, y, gx + 1, y + h, gridColor);
        }
        for (int gy = y + spacing; gy < y + h; gy += spacing) {
            graphics.fill(x, gy, x + w, gy + 1, gridColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private int getAccentColor() {
        return switch (hatch.getIo()) {
            case IN -> 0xFF8F00FF;
            case OUT -> 0xFFD466FF;
            case BOTH -> 0xFFBC66FF;
            default -> 0xFF8080A0;
        };
    }
}
