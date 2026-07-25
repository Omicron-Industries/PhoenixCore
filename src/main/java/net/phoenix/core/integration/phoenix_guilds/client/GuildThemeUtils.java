package net.phoenix.core.integration.phoenix_guilds.client;

public final class GuildThemeUtils {

    private GuildThemeUtils() {}

    public static int C_BG() {
        return GuildTheme.current().bg.getColor();
    }

    public static int C_PANEL() {
        return GuildTheme.current().panel.getColor();
    }

    public static int C_HEADER() {
        return GuildTheme.current().header.getColor();
    }

    public static int C_BORDER() {
        return GuildTheme.current().border.getColor();
    }

    public static int C_ACCENT() {
        return GuildTheme.current().accent.getColor();
    }

    public static int C_ALLY() {
        return GuildTheme.current().ally.getColor();
    }

    public static int C_TEXT() {
        return GuildTheme.current().text.getColor();
    }

    public static int C_DIM() {
        return GuildTheme.current().dim.getColor();
    }

    public static int C_FAINT() {
        return GuildTheme.current().faint.getColor();
    }

    public static int C_BORDER2() {
        int b = GuildTheme.current().border.getColor();
        int a = (b >> 24) & 0xFF;
        int r = Math.max(0, ((b >> 16) & 0xFF) - 12);
        int g = Math.max(0, ((b >> 8) & 0xFF) - 12);
        int bl = Math.max(0, (b & 0xFF) - 12);
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }

    public static int C_ROW_ALT() {
        return 0x0AFFFFFF;
    }

    public static int C_ONLINE() {
        return 0xFF33EE77;
    }

    public static int C_OFFLINE() {
        return 0xFF444466;
    }

    public static int C_GOLD() {
        return 0xFFFFCC22;
    }

    public static int C_OFFICER() {
        return 0xFF88AAFF;
    }
}
