package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CategoryConfig {

    public enum BgStyle {
        DOT_GRID,
        GRID_LINES,
        HEX_GRID,
        DIAGONAL_LINES,
        SOLID,
        CUSTOM
    }

    private BgStyle style = BgStyle.DOT_GRID;

    private int color = 0;

    private String texture = "";

    public BgStyle getStyle() {
        return style;
    }

    public int getColor() {
        return color;
    }

    public String getTexture() {
        return texture;
    }

    public void setStyle(BgStyle s) {
        this.style = s != null ? s : BgStyle.DOT_GRID;
    }

    public void setColor(int c) {
        this.color = c;
    }

    public void setTexture(String t) {
        this.texture = t != null ? t : "";
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("style", style.name());
        if (color != 0) o.addProperty("color", String.format("#%06X", color & 0x00FFFFFF));
        if (!texture.isEmpty()) o.addProperty("texture", texture);
        return o;
    }

    public static CategoryConfig fromJson(JsonObject o) {
        CategoryConfig cfg = new CategoryConfig();
        if (o.has("style")) {
            try {
                cfg.style = BgStyle.valueOf(o.get("style").getAsString().toUpperCase());
            } catch (Exception ignored) {}
        }
        if (o.has("color")) {
            try {
                cfg.color = (int) Long.parseLong(o.get("color").getAsString().replace("#", ""), 16);
            } catch (Exception ignored) {}
        }
        if (o.has("texture")) cfg.texture = o.get("texture").getAsString();
        return cfg;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CategoryConfig> CACHE = new HashMap<>();
    private static boolean loaded = false;

    public static CategoryConfig get(String category) {
        if (!loaded) load();
        return CACHE.getOrDefault(category, new CategoryConfig());
    }

    public static void put(String category, CategoryConfig cfg) {
        if (!loaded) load();
        CACHE.put(category, cfg);
    }

    public static void invalidate() {
        loaded = false;
        CACHE.clear();
    }

    public static void load() {
        loaded = true;
        CACHE.clear();
        Path p = configPath();
        if (!Files.exists(p)) return;
        try {
            String raw = Files.readString(p, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(raw, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getValue().isJsonObject())
                    CACHE.put(e.getKey().toUpperCase(), fromJson(e.getValue().getAsJsonObject()));
            }
        } catch (Exception e) {
            System.err.println("[Phoenix Chronicles] Failed to load categories.json: " + e.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, CategoryConfig> e : CACHE.entrySet())
            root.add(e.getKey(), e.getValue().toJson());
        try {
            Path p = configPath();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to save categories.json: " + e.getMessage());
        }
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles").resolve("categories.json");
    }
}
