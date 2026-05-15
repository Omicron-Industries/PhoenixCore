package net.phoenix.core.integration.phantasia.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class PhantasiaThemeUtils {
    // --- THEME COLORS ---

    public static final int C_PROG = 0xFF4FC3F7; // Bright Blue for progress bars
    public static final int C_WARN = 0xFFFFB74D;
    public static final int C_HILIGHT = 0xFFFFEB3B; // A bright yellow for highlighting mistakes or important blocks
    public static final int C_BG = 0xFF080810;
    public static final int C_PANEL = 0xEE0C0C1A;
    public static final int C_ACCENT = 0xFF4FC3F7;
    public static final int C_BTN = 0xBB151528;
    public static final int C_BTN_HOV = 0xBB1A2840;
    public static final int C_BTN_ACT = 0xBB0D3050;
    public static final int C_TEXT = 0xFFDDDDDD;
    public static final int C_DIM = 0xFF667788;
    public static final int C_TL_BG = 0xFF0F1820;

    public static void drawThemedBtn(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hov,
                                     int baseColor) {
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : baseColor);
        if (hov) {
            g.fill(x, y, x + w, y + 1, C_ACCENT);
            g.fill(x, y + h - 1, x + w, y + h, C_ACCENT);
        }
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
    }

    public static void drawIconBtn(GuiGraphics g, Font font, int x, int y, int w, int h, String icon, String label,
                                   boolean hov, int baseColor) {
        drawThemedBtn(g, font, x, y, w, h, "", hov, baseColor);
        g.drawString(font, icon, x + 6, y + (h - 8) / 2, C_ACCENT, false);
        g.drawString(font, label, x + 20, y + (h - 8) / 2, hov ? C_ACCENT : C_TEXT, false);
    }
}
