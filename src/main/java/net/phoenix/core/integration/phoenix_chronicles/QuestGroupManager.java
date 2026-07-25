package net.phoenix.core.integration.phoenix_chronicles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuestGroupManager {

    private static final Map<String, QuestGroup> GROUPS = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;

    public static Collection<QuestGroup> getAll() {
        return GROUPS.values();
    }

    public static QuestGroup get(String id) {
        return GROUPS.get(id);
    }

    public static void put(QuestGroup g) {
        GROUPS.put(g.getId(), g);
    }

    public static void remove(String id) {
        GROUPS.remove(id);
    }

    public static void clear() {
        GROUPS.clear();
    }

    public static List<QuestGroup> forCategory(String category) {
        return GROUPS.values().stream()
                .filter(g -> "ALL".equals(category) || category.equalsIgnoreCase(g.getCategory()))
                .collect(Collectors.toList());
    }

    public static String generateId() {
        String candidate;
        do {
            candidate = "group_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (GROUPS.containsKey(candidate));
        return candidate;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void invalidate() {
        loaded = false;
    }

    private static Path groupsFile(Path configDir) {
        return configDir.resolve("groups.json");
    }

    public static void load(Path configDir) {
        if (loaded) return;
        loaded = true;
        GROUPS.clear();

        Path file = groupsFile(configDir);
        if (!Files.exists(file)) return;

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            if (arr == null) return;

            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                String id = getString(obj, "id", null);
                if (id == null || id.isBlank()) continue;
                String label = getString(obj, "label", "Group");
                String category = getString(obj, "category", "ALL");

                QuestGroup g = new QuestGroup(id, label, category);
                g.setColor(parseColor(getString(obj, "color", "#22FFFFFF")));
                g.setBorderColor(parseColor(getString(obj, "borderColor", "#44FFFFFF")));
                g.setX(getInt(obj, "x", 0));
                g.setY(getInt(obj, "y", 0));
                g.setWidth(getInt(obj, "width", 120));
                g.setHeight(getInt(obj, "height", 80));

                GROUPS.put(id, g);
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void save(Path configDir) {
        try {
            Files.createDirectories(configDir);
            JsonArray arr = new JsonArray();

            for (QuestGroup g : GROUPS.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", g.getId());
                obj.addProperty("label", g.getLabel());
                obj.addProperty("category", g.getCategory());
                obj.addProperty("color", formatColor(g.getColor()));
                obj.addProperty("borderColor", formatColor(g.getBorderColor()));
                obj.addProperty("x", g.getX());
                obj.addProperty("y", g.getY());
                obj.addProperty("width", g.getWidth());
                obj.addProperty("height", g.getHeight());
                arr.add(obj);
            }

            Files.writeString(groupsFile(configDir), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String formatColor(int argb) {
        return String.format("#%08X", argb);
    }

    public static int parseColor(String hex) {
        if (hex == null) return 0x22FFFFFF;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (s.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(s, 16));
            } else if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0x22FFFFFF;
    }

    private static String getString(JsonObject obj, String key, String def) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsInt();
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
