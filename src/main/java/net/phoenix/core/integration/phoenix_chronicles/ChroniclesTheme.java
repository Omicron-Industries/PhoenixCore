package net.phoenix.core.integration.phoenix_chronicles;

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

public class ChroniclesTheme {

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

        public void set(String newHex) {
            this.hex = newHex;
            this.cached = null;
        }
    }

    public ThemeColor bg, panel, header, border, accent, text, textDim, textFaint, done, activeColor, locked;

    public static final Map<String, ChroniclesTheme> REGISTRY = new LinkedHashMap<>();
    private static ChroniclesTheme active = null;
    private static String activeName = "DARK";

    private static final Set<String> BUILTINS = Set.of("DARK", "LIGHT", "CRIMSON", "OCEAN", "PHANTOM", "EMBER");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .create();
    private static final Path THEMES_FILE = Paths.get("config", "phoenix_chronicles_themes.json");

    public ChroniclesTheme() {}

    public ChroniclesTheme(String bg, String panel, String header, String border, String accent,
                           String text, String textDim, String textFaint, String done, String activeCol,
                           String locked) {
        this.bg = new ThemeColor(bg);
        this.panel = new ThemeColor(panel);
        this.header = new ThemeColor(header);
        this.border = new ThemeColor(border);
        this.accent = new ThemeColor(accent);
        this.text = new ThemeColor(text);
        this.textDim = new ThemeColor(textDim);
        this.textFaint = new ThemeColor(textFaint);
        this.done = new ThemeColor(done);
        this.activeColor = new ThemeColor(activeCol);
        this.locked = new ThemeColor(locked);
    }

    public ChroniclesTheme copy() {
        return new ChroniclesTheme(
                bg.hex, panel.hex, header.hex, border.hex, accent.hex,
                text.hex, textDim.hex, textFaint.hex, done.hex, activeColor.hex, locked.hex);
    }

    public static ChroniclesTheme current() {
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
        ChroniclesTheme t = REGISTRY.get(name);
        if (t == null) t = REGISTRY.get(name.toUpperCase(Locale.ROOT));
        if (t != null) {
            active = t;
            activeName = name;
        }
    }

    public static void saveCustomTheme(String name, ChroniclesTheme theme) {
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

    private static class ThemeSave {

        String active = "DARK";
        Map<String, ChroniclesTheme> custom = new LinkedHashMap<>();
    }

    public static void saveAll() {
        try {
            Files.createDirectories(THEMES_FILE.getParent());
            ThemeSave save = new ThemeSave();
            save.active = activeName;
            for (Map.Entry<String, ChroniclesTheme> e : REGISTRY.entrySet()) {
                if (!isBuiltin(e.getKey())) save.custom.put(e.getKey(), e.getValue());
            }
            Files.writeString(THEMES_FILE, GSON.toJson(save));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadThemes() {
        REGISTRY.clear();

        REGISTRY.put("DARK", new ChroniclesTheme(
                "FF0B0B0F", "FF14141A", "FF0C0C10", "FF353548", "FF00AA55",
                "FFD8D8E4", "FF7A7A8A", "FF404050", "FF44CC88", "FFFFBB33", "FF606070"));

        REGISTRY.put("LIGHT", new ChroniclesTheme(
                "FFF0F0F4", "FFE4E4EC", "FFD8D8E0", "FFA0A0B0", "FF0088CC",
                "FF1A1A2A", "FF555565", "FF888898", "FF22AA55", "FFCC6600", "FF808090"));

        REGISTRY.put("CRIMSON", new ChroniclesTheme(
                "FF0F0808", "FF1A0C0C", "FF0D0606", "FF3A1818", "FFCC2233",
                "FFE4D0D0", "FF8A6A6A", "FF503838", "FF44CC66", "FFFFAA33", "FF705555"));

        REGISTRY.put("OCEAN", new ChroniclesTheme(
                "FF080C12", "FF0E1520", "FF080C14", "FF1E2A3C", "FF0099CC",
                "FFCCE0F0", "FF6080A0", "FF304060", "FF33CC88", "FFFFBB33", "FF506888"));

        REGISTRY.put("PHANTOM", new ChroniclesTheme(
                "FF0A080F", "FF130E1C", "FF0A0810", "FF2E2040", "FF8833CC",
                "FFD8CCF0", "FF7060A0", "FF3C2C5C", "FF44BB88", "FFFFAA44", "FF604880"));

        REGISTRY.put("EMBER", new ChroniclesTheme(
                "FF100A06", "FF1C120A", "FF100A04", "FF382210", "FFCC6600",
                "FFF0E0CC", "FFA0785A", "FF604830", "FF44CC77", "FFFFCC22", "FF806040"));

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

    @Deprecated
    public static void saveCustom() {
        saveAll();
    }
}
