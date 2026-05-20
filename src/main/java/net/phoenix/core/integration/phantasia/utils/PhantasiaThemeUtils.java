package net.phoenix.core.integration.phantasia.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * PhantasiaThemeUtils
 *
 * Drop-in replacement for the old hardcoded color constants.
 * All color fields now delegate to {@link PhantasiaTheme#current()} so
 * switching themes in the config takes effect immediately with no restart.
 *
 * ── Usage ────────────────────────────────────────────────────────────────────
 * Replace all direct field reads (e.g. {@code C_ACCENT}) with static imports
 * from this class — the pattern is identical to before. The static properties
 * read the active theme live, so callers don't need to change.
 *
 * For rainbow-animated colors (accent, prog, hilight), this class automatically
 * passes the current game time when the RAINBOW theme is active — callers never
 * need to handle that themselves.
 *
 * ── Button rendering ─────────────────────────────────────────────────────────
 * {@link #drawThemedBtn} and {@link #drawIconBtn} branch on the active theme so
 * the MINECRAFT theme gets its boxy vanilla-slot look automatically.
 */
public class PhantasiaThemeUtils {

    // ── Color accessors ───────────────────────────────────────────────────────
    // These match the old static final field names exactly so import-static
    // sites in PhantasiaSceneScreen etc. compile without changes.

    public static int C_BG() {
        return PhantasiaTheme.current().C_BG;
    }

    public static int C_PANEL() {
        return PhantasiaTheme.current().C_PANEL;
    }

    public static int C_BTN() {
        return PhantasiaTheme.current().C_BTN;
    }

    public static int C_BTN_HOV() {
        return PhantasiaTheme.current().C_BTN_HOV;
    }

    public static int C_BTN_ACT() {
        return PhantasiaTheme.current().C_BTN_ACT;
    }

    public static int C_TEXT() {
        return PhantasiaTheme.current().C_TEXT;
    }

    public static int C_DIM() {
        return PhantasiaTheme.current().C_DIM;
    }

    public static int C_TL_BG() {
        return PhantasiaTheme.current().C_TL_BG;
    }

    public static int C_WARN() {
        return PhantasiaTheme.current().C_WARN;
    }

    // Dynamic colors — route through the theme's time-aware getters.
    public static int C_ACCENT() {
        return PhantasiaTheme.current().getAccent(gameTime());
    }

    public static int C_PROG() {
        return PhantasiaTheme.current().getProg(gameTime());
    }

    public static int C_HILIGHT() {
        return PhantasiaTheme.current().getHilight(gameTime());
    }

    private static long gameTime() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    // ── Button rendering ──────────────────────────────────────────────────────

    /**
     * Draws a themed button. Appearance varies by active theme:
     * <ul>
     * <li>COBALT / RAINBOW / AMETHYST — dark fill, accent top/bottom lines on hover.</li>
     * <li>MINECRAFT — boxy vanilla slot style: 3-D border, no glow lines.</li>
     * </ul>
     */
    public static void drawThemedBtn(GuiGraphics g, Font font,
                                     int x, int y, int w, int h,
                                     String label, boolean hov, int baseColor) {
        PhantasiaTheme t = PhantasiaTheme.current();
        if (t == PhantasiaTheme.MINECRAFT) {
            drawMinecraftBtn(g, font, x, y, w, h, label, hov, baseColor);
        } else {
            drawModernBtn(g, font, x, y, w, h, label, hov, baseColor, t);
        }
    }

    /** Icon button — icon on the left, label on the right. */
    public static void drawIconBtn(GuiGraphics g, Font font,
                                   int x, int y, int w, int h,
                                   String icon, String label,
                                   boolean hov, int baseColor) {
        drawThemedBtn(g, font, x, y, w, h, "", hov, baseColor);
        int midY = y + (h - 8) / 2;
        g.drawString(font, icon, x + 6, midY, C_ACCENT(), false);
        g.drawString(font, label, x + 20, midY, hov ? C_ACCENT() : C_TEXT(), false);
    }

    // ── Modern button (Cobalt / Rainbow / Amethyst) ───────────────────────────

    private static void drawModernBtn(GuiGraphics g, Font font,
                                      int x, int y, int w, int h,
                                      String label, boolean hov,
                                      int baseColor, PhantasiaTheme t) {
        g.fill(x, y, x + w, y + h, hov ? t.C_BTN_HOV : baseColor);
        if (hov) {
            int accent = t.getAccent(gameTime());
            // Thin accent lines on top and bottom edges only — clean, not garish.
            g.fill(x, y, x + w, y + 1, accent);
            g.fill(x, y + h - 1, x + w, y + h, accent);
        }
        int textColor = hov ? t.getAccent(gameTime()) : t.C_TEXT;
        g.drawString(font, label,
                x + (w - font.width(label)) / 2,
                y + (h - 8) / 2,
                textColor, false);
    }

    // ── Minecraft button ──────────────────────────────────────────────────────
    // Replicates the classic vanilla GUI button look:
    // - Outer 1px dark border
    // - Inner top-left highlight (lighter)
    // - Inner bottom-right shadow (darker)
    // - Fill in the middle

    private static final int MC_BORDER = 0xFF000000;
    private static final int MC_SHADOW = 0xFF373737; // dark face
    private static final int MC_HIGHLIGHT = 0xFFAAAAAA; // light bevel
    private static final int MC_FILL = 0xFF8B8B8B; // stone grey mid

    private static void drawMinecraftBtn(GuiGraphics g, Font font,
                                         int x, int y, int w, int h,
                                         String label, boolean hov,
                                         int baseColor) {
        // Outer border
        g.fill(x, y, x + w, y + h, MC_BORDER);

        int fill = hov ? 0xFF9A9A9A   // slightly lighter when hovered — no glow, just lift
                : baseColor == PhantasiaTheme.MINECRAFT.C_BTN_ACT ? baseColor  // active state uses its own color
                        : MC_FILL;

        // Inner fill
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);

        // Top and left highlight (raised edge)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, MC_HIGHLIGHT); // top
        g.fill(x + 1, y + 1, x + 2, y + h - 1, MC_HIGHLIGHT); // left

        // Bottom and right shadow (lowered edge)
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, MC_SHADOW); // bottom
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, MC_SHADOW); // right

        // Label — drop-shadow like vanilla (draw dark 1px offset, then white on top)
        int lx = x + (w - font.width(label)) / 2;
        int ly = y + (h - 8) / 2;
        if (!label.isEmpty()) {
            g.drawString(font, label, lx + 1, ly + 1, 0xFF383838, false); // shadow
            g.drawString(font, label, lx, ly, hov ? 0xFFFFFFA0 : 0xFFFFFFFF, false);
        }
    }
}
