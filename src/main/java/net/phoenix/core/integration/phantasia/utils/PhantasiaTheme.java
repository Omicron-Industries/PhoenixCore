package net.phoenix.core.integration.phantasia.utils;

import net.minecraft.util.Mth;
import net.phoenix.core.configs.PhoenixConfigs;

/**
 * PhantasiaTheme
 *
 * Defines every visual preset for the Phantasia UI.
 * Each theme carries a complete set of color constants so no theme ever
 * falls back to another's values.
 *
 * ── Switching ────────────────────────────────────────────────────────────────
 * Read {@link #current()} every frame — it resolves the active theme from the
 * config live, so changes take effect immediately with no restart.
 *
 * ── Rainbow ──────────────────────────────────────────────────────────────────
 * The RAINBOW theme marks {@code C_ACCENT}, {@code C_PROG}, and {@code C_HILIGHT}
 * as dynamic. Call {@link #getAccent(long)} / {@link #getProg(long)} /
 * {@link #getHilight(long)} instead of reading the field directly when
 * {@link #isRainbow()} is true. All other callers just use the constants.
 * {@link PhantasiaThemeUtils} handles this transparently.
 */
public enum PhantasiaTheme {

    // ── Cobalt (default) ──────────────────────────────────────────────────────
    COBALT(
            /* bg */ 0xFF080810,
            /* panel */ 0xEE0C0C1A,
            /* accent */ 0xFF4FC3F7,
            /* btn */ 0xBB151528,
            /* btnHov */ 0xBB1A2840,
            /* btnAct */ 0xBB0D3050,
            /* text */ 0xFFDDDDDD,
            /* dim */ 0xFF667788,
            /* tlBg */ 0xFF0F1820,
            /* prog */ 0xFF4FC3F7,
            /* warn */ 0xFFFFB74D,
            /* hilight */ 0xFFFFEB3B,
            /* rainbow */ false),

    // ── Rainbow ───────────────────────────────────────────────────────────────
    // Accent / prog / hilight are computed dynamically; the stored int is a
    // reasonable static fallback but callers should use getAccent(time) etc.
    RAINBOW(
            /* bg */ 0xFF090909,
            /* panel */ 0xEE111111,
            /* accent */ 0xFFFF4444,   // fallback only
            /* btn */ 0xBB1A1A1A,
            /* btnHov */ 0xBB2A2A2A,
            /* btnAct */ 0xBB202040,
            /* text */ 0xFFEEEEEE,
            /* dim */ 0xFF666666,
            /* tlBg */ 0xFF101010,
            /* prog */ 0xFFFF4444,   // fallback only
            /* warn */ 0xFFFFB74D,
            /* hilight */ 0xFFFFFF44,   // fallback only
            /* rainbow */ true),

    // ── Amethyst ──────────────────────────────────────────────────────────────
    AMETHYST(
            /* bg */ 0xFF08060F,
            /* panel */ 0xEE100818,
            /* accent */ 0xFFB39DDB,   // soft lavender
            /* btn */ 0xBB1A0A2E,
            /* btnHov */ 0xBB2A1048,
            /* btnAct */ 0xBB3D1A6E,
            /* text */ 0xFFE8D5FF,
            /* dim */ 0xFF7755AA,
            /* tlBg */ 0xFF0C0514,
            /* prog */ 0xFFFFD700,   // gold progress bar
            /* warn */ 0xFFFF9800,
            /* hilight */ 0xFFFFD700,
            /* rainbow */ false),

    // ── Minecraft ─────────────────────────────────────────────────────────────
    // Boxy, stone-grey panels. No glowing accent lines on hover —
    // vanilla buttons just get brighter. Gold accent, green progress (XP bar feel).
    MINECRAFT(
            /* bg */ 0xFF373737,   // stone grey
            /* panel */ 0xFF2D2D2D,   // slightly darker
            /* accent */ 0xFFFFAA00,   // MC gold
            /* btn */ 0xFF5A5A5A,   // vanilla mid-grey button
            /* btnHov */ 0xFF6E6E6E,   // lighter on hover, no glow
            /* btnAct */ 0xFF4A4A8A,   // pressed: faint blue like vanilla selected slot
            /* text */ 0xFFFFFFFF,
            /* dim */ 0xFFAAAAAA,
            /* tlBg */ 0xFF1E1E1E,
            /* prog */ 0xFF55FF55,   // MC XP green
            /* warn */ 0xFFFF5555,   // MC red
            /* hilight */ 0xFFFFFF55,   // MC yellow
            /* rainbow */ false);

    // ── Fields ────────────────────────────────────────────────────────────────

    public final int C_BG;
    public final int C_PANEL;
    public final int C_ACCENT;
    public final int C_BTN;
    public final int C_BTN_HOV;
    public final int C_BTN_ACT;
    public final int C_TEXT;
    public final int C_DIM;
    public final int C_TL_BG;
    public final int C_PROG;
    public final int C_WARN;
    public final int C_HILIGHT;
    private final boolean rainbow;

    PhantasiaTheme(int bg, int panel, int accent, int btn, int btnHov, int btnAct,
                   int text, int dim, int tlBg, int prog, int warn, int hilight,
                   boolean rainbow) {
        this.C_BG = bg;
        this.C_PANEL = panel;
        this.C_ACCENT = accent;
        this.C_BTN = btn;
        this.C_BTN_HOV = btnHov;
        this.C_BTN_ACT = btnAct;
        this.C_TEXT = text;
        this.C_DIM = dim;
        this.C_TL_BG = tlBg;
        this.C_PROG = prog;
        this.C_WARN = warn;
        this.C_HILIGHT = hilight;
        this.rainbow = rainbow;
    }

    // ── Current theme resolution ──────────────────────────────────────────────

    private static PhantasiaTheme cachedTheme = COBALT;
    private static String lastKnownConfigString = null;

    /**
     * Returns the active theme by reading the config live.
     * Optimized to cache the enum parsing, preventing GC allocations every frame.
     */
    public static PhantasiaTheme current() {
        String activeStr = PhoenixConfigs.INSTANCE.phantasiaUI.theme;

        // Only parse if the configuration string actually changes (usually never mid-frame)
        if (activeStr != lastKnownConfigString) {
            // Use standard .equals() check if reference equality fails
            if (!activeStr.equals(lastKnownConfigString)) {
                lastKnownConfigString = activeStr;
                cachedTheme = fromString(activeStr); // Safely utilizes your existing helper
            }
        }

        return cachedTheme;
    }

    // ── Rainbow dynamic color API ─────────────────────────────────────────────

    public boolean isRainbow() {
        return rainbow;
    }

    /**
     * Accent color for the current game time.
     * For non-rainbow themes this is just {@link #C_ACCENT}.
     *
     * @param gameTime {@code Minecraft.getInstance().level.getGameTime()} or similar.
     */
    public int getAccent(long gameTime) {
        return rainbow ? hueToArgb(gameTime, 0f, 1f) : C_ACCENT;
    }

    /** Progress-bar color for the current game time. */
    public int getProg(long gameTime) {
        return rainbow ? hueToArgb(gameTime, 0.1f, 0.9f) : C_PROG;
    }

    /** Highlight color for the current game time. */
    public int getHilight(long gameTime) {
        return rainbow ? hueToArgb(gameTime, 0.25f, 0.85f) : C_HILIGHT;
    }

    /**
     * Computes an ARGB color from the current game time.
     *
     * @param gameTime   tick counter (advances 20/s)
     * @param saturation HSB saturation [0, 1]
     * @param brightness HSB brightness [0, 1]
     */
    private static int hueToArgb(long gameTime, float saturation, float brightness) {
        // Full hue cycle every 10 seconds (200 ticks).
        float hue = (gameTime % 200) / 200f;
        // Standard HSB → RGB conversion (no java.awt dependency).
        float h = hue * 6f;
        int i = (int) h;
        float f = h - i;
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> {
                r = brightness;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = brightness;
                b = p;
            }
            case 2 -> {
                r = p;
                g = brightness;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = brightness;
            }
            case 4 -> {
                r = t;
                g = p;
                b = brightness;
            }
            default -> {
                r = brightness;
                g = p;
                b = q;
            }
        }
        int ri = Mth.clamp((int) (r * 255), 0, 255);
        int gi = Mth.clamp((int) (g * 255), 0, 255);
        int bi = Mth.clamp((int) (b * 255), 0, 255);
        return (0xFF << 24) | (ri << 16) | (gi << 8) | bi;
    }

    // ── Parse helper (used by config) ─────────────────────────────────────────

    /** Case-insensitive parse; returns COBALT if the string is unrecognised. */
    public static PhantasiaTheme fromString(String s) {
        if (s == null) return COBALT;
        try {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COBALT;
        }
    }
}
