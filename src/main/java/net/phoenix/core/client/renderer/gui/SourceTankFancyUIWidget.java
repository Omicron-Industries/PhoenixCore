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
import net.phoenix.core.common.machine.multiblock.unique.SourceMultiblockTankMachine;

import com.mojang.blaze3d.systems.RenderSystem;

import javax.annotation.Nonnull;

public class SourceTankFancyUIWidget extends FancyMachineUIWidget {

    private final SourceMultiblockTankMachine tank;

    private static final int BG_COLOR_A = 0xFF0a050f;
    private static final int BG_COLOR_B = 0xFF050208;

    private static final int PURPLE_MIST = 0x8F00FF;
    private static final int BUTTON_BG = 0xB0100518;
    private static final int PURPLE_ACCENT = 0xFF8F00FF;
    // Deep dark purple for the "empty" track
    private static final int BAR_EMPTY = 0xFF120520;

    // A bright Cyan for low levels
    private static final int BAR_LOW = 0xFF00E5FF;

    // A soft, bright Lavender/Light Purple for mid levels
    private static final int BAR_MID = 0xFFD466FF;

    // A vibrant, glowing Purple for full capacity
    private static final int BAR_FULL = 0xFFBD00FF;

    public SourceTankFancyUIWidget(IFancyUIProvider mainPage, int width, int height) {
        super(mainPage, width, height);
        this.tank = (SourceMultiblockTankMachine) mainPage;
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

        DrawerHelper.drawBorder(graphics, x, y, w, h, (0xAA << 24) | (PURPLE_ACCENT & 0xFFFFFF), 1);
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
                pageX - expand, pageY - expand,
                pageX + pageW + expand, pageY + pageH + expand);

        drawOverlayHUD(graphics, pageX + 8, pageY + 6, pageW - 16);

        graphics.disableScissor();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawOverlayHUD(GuiGraphics graphics, int x, int y, int usableWidth) {
        var font = Minecraft.getInstance().font;

        // Get current and max values
        int cur = tank.getSourceTank().getSource();
        int max = Math.max(1, tank.getSourceTank().getMaxSource());
        int pct = (int) ((cur * 100L) / max);

        // Vertical spacing
        int labelY = y + 18;
        int barY = y + 32;
        int barH = 5; // Slightly thinner bar for a sleeker look

        // Title
        graphics.drawString(font, "SOURCE TANK", x, y, PURPLE_ACCENT, false);

        // The compact format: "Capacity 500/1k - 50%"
        // Using your fmt() method to keep the strings short
        String sourceLabel = String.format("Capacity %s/%s - %d%%", fmt(cur), fmt(max), pct);
        graphics.drawString(font, sourceLabel, x, labelY, 0xFFE6E6FF, false);

        // --- Progress Bar ---

        // Background
        graphics.fill(x, barY, x + usableWidth, barY + barH, BAR_EMPTY);

        // Color logic (Cyan -> Lavender -> Purple)
        int barColor = pct < 30 ? BAR_LOW : pct < 75 ? BAR_MID : BAR_FULL;

        // Fill
        int fillW = (int) ((cur * (long) usableWidth) / max);
        if (fillW > 0) {
            graphics.fill(x, barY, x + fillW, barY + barH, barColor);
        }

        // Subtle Cyan glow border (0x33 alpha is very faint/classy)
        DrawerHelper.drawBorder(graphics, x, barY, usableWidth, barH, (0x33 << 24) | 0x00FFFF, 1);

        // Divider Line
        int lineY = barY + barH + 10;
        graphics.fill(x, lineY, x + usableWidth, lineY + 1, (0x44 << 24) | (PURPLE_ACCENT & 0xFFFFFF));
    }

    private static String fmt(long v) {
        if (v < 1_000) return String.valueOf(v);
        if (v < 10_000) return String.format("%.1fk", v / 1_000.0);
        if (v < 1_000_000) return (v / 1_000) + "k";
        if (v < 10_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v < 1_000_000_000) return (v / 1_000_000) + "M";
        return String.format("%.1fB", v / 1_000_000_000.0);
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
                graphics.fill(x + px, y + py, x + px + step, y + py + step,
                        (alpha << 24) | (PURPLE_MIST & 0xFFFFFF));
            }
        }

        RenderSystem.disableBlend();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawGridPattern(GuiGraphics graphics, int x, int y, int w, int h) {
        int gridColor = 0x0AFFFFFF;
        int spacing = 16;

        for (int gx = x + spacing; gx < x + w; gx += spacing)
            graphics.fill(gx, y, gx + 1, y + h, gridColor);
        for (int gy = y + spacing; gy < y + h; gy += spacing)
            graphics.fill(x, gy, x + w, gy + 1, gridColor);
    }
}
