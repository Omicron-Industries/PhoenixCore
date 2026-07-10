package net.phoenix.core.integration.phoenix_guilds.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GuildTheme {

    // ── ThemeColor ────────────────────────────────────────────────────────────

    public static class ThemeColor {

        public String hex;
        private transient Integer cached = null;

        public ThemeColor() {
            this.hex = "FFFFFFFF";
        }

        public ThemeColor(String hex) {
            this.hex = hex;
        }

        public int getColor() {
            if (hex == null) return 0xFFFFFFFF;
            String clean = hex.trim().toUpperCase(Locale.ROOT);
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (cached == null) {
                try {
                    cached = (int) Long.parseUnsignedLong(clean, 16);
                } catch (Exception e) {
                    cached = 0xFFFFFFFF;
                }
            }
            return cached;
        }

        public String getHex() {
            return hex;
        }

        public void set(String h) {
            this.hex = h;
            this.cached = null;
        }
    }

    // ── Fields (structural/stylistic colors only) ─────────────────────────────

    public ThemeColor bg, panel, header, border, accent, ally, text, dim, faint;

    // ── Registry ──────────────────────────────────────────────────────────────

    public static final Map<String, GuildTheme> REGISTRY = new LinkedHashMap<>();
    private static GuildTheme active = null;
    private static String activeName = "DARK";

    private static final Set<String> BUILTINS = Set.of("DARK", "OCEAN", "CRIMSON", "PHANTOM", "EMERALD", "EMBER");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .create();
    private static final Path THEMES_FILE = Paths.get("config", "phoenix_guilds_themes.json");

    // ── Constructors ──────────────────────────────────────────────────────────

    public GuildTheme() {}

    public GuildTheme(String bg, String panel, String header, String border,
                      String accent, String ally, String text, String dim, String faint) {
        this.bg = new ThemeColor(bg);
        this.panel = new ThemeColor(panel);
        this.header = new ThemeColor(header);
        this.border = new ThemeColor(border);
        this.accent = new ThemeColor(accent);
        this.ally = new ThemeColor(ally);
        this.text = new ThemeColor(text);
        this.dim = new ThemeColor(dim);
        this.faint = new ThemeColor(faint);
    }

    public GuildTheme copy() {
        return new GuildTheme(bg.hex, panel.hex, header.hex, border.hex,
                accent.hex, ally.hex, text.hex, dim.hex, faint.hex);
    }

    // ── Static API ────────────────────────────────────────────────────────────

    public static GuildTheme current() {
        if (REGISTRY.isEmpty()) loadThemes();
        return active != null ? active : REGISTRY.get("DARK");
    }

    public static String getActiveName() {
        if (REGISTRY.isEmpty()) loadThemes();
        return activeName;
    }

    public static boolean isBuiltin(String name) {
        return BUILTINS.contains(name.toUpperCase(Locale.ROOT));
    }

    public static void setCurrent(String name) {
        GuildTheme t = REGISTRY.get(name);
        if (t == null) t = REGISTRY.get(name.toUpperCase(Locale.ROOT));
        if (t != null) {
            active = t;
            activeName = name;
        }
    }

    public static void saveCustomTheme(String name, GuildTheme theme) {
        REGISTRY.put(name, theme);
        saveAll();
    }

    public static boolean deleteCustom(String name) {
        if (isBuiltin(name)) return false;
        REGISTRY.remove(name);
        if (name.equals(activeName)) {
            activeName = "DARK";
            active = REGISTRY.get("DARK");
        }
        saveAll();
        return true;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static class ThemeSave {

        String active = "DARK";
        Map<String, GuildTheme> custom = new LinkedHashMap<>();
    }

    public static void saveAll() {
        try {
            Files.createDirectories(THEMES_FILE.getParent());
            ThemeSave save = new ThemeSave();
            save.active = activeName;
            for (Map.Entry<String, GuildTheme> e : REGISTRY.entrySet())
                if (!isBuiltin(e.getKey())) save.custom.put(e.getKey(), e.getValue());
            Files.writeString(THEMES_FILE, GSON.toJson(save));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadThemes() {
        REGISTRY.clear();

        // bg panel header border accent ally text dim faint
        REGISTRY.put("DARK", new GuildTheme("BE050510", "FF0C0C1A", "FF060614", "FF202048", "FF4422AA", "FF2277BB",
                "FFDDDDFF", "FF7777AA", "FF383858"));
        REGISTRY.put("OCEAN", new GuildTheme("BE020810", "FF091520", "FF040C18", "FF1A2E44", "FF1177CC", "FF22AADD",
                "FFCCE8FF", "FF6688AA", "FF263A52"));
        REGISTRY.put("CRIMSON", new GuildTheme("BE100508", "FF1A0C0E", "FF0E0608", "FF3A1820", "FFAA1133", "FFCC3355",
                "FFFFE0E4", "FF9A6070", "FF503038"));
        REGISTRY.put("PHANTOM", new GuildTheme("BE0A0510", "FF140E1C", "FF0A0812", "FF2E2040", "FF8833CC", "FF6644AA",
                "FFD8CCF0", "FF7060A0", "FF3C2C5C"));
        REGISTRY.put("EMERALD", new GuildTheme("BE041008", "FF0C1A10", "FF060E08", "FF183828", "FF22AA55", "FF33CC77",
                "FFCCFFDD", "FF60AA78", "FF243C2E"));
        REGISTRY.put("EMBER", new GuildTheme("BE100806", "FF1C140A", "FF100A04", "FF382410", "FFCC6600", "FFFF9922",
                "FFF0E0CC", "FFA0785A", "FF604830"));

        String loadedActive = "DARK";
        try {
            if (Files.exists(THEMES_FILE)) {
                String json = Files.readString(THEMES_FILE);
                Type type = new TypeToken<ThemeSave>() {}.getType();
                ThemeSave save = GSON.fromJson(json, type);
                if (save != null) {
                    if (save.custom != null) REGISTRY.putAll(save.custom);
                    if (save.active != null) loadedActive = save.active;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        activeName = loadedActive;
        active = REGISTRY.getOrDefault(activeName, REGISTRY.get("DARK"));
    }
}
