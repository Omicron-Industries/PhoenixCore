package net.phoenix.core.client.emi;

import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.runtime.EmiFavorites;
import dev.emi.emi.screen.EmiScreenManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhoenixFavoriteSets {

    public static final String DEFAULT_SET = "default";

    public enum Scope {
        GLOBAL,
        PER_WORLD
    }

    private static final Logger LOGGER = LogManager.getLogger("PhoenixCore/EmiFavorites");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("phoenixcore/emi_favorites.json");

    private static String worldId = null;
    private static Scope scope = Scope.GLOBAL;
    private static String activeRawKey = DEFAULT_SET;
    private static final Map<String, JsonArray> pages = new LinkedHashMap<>();
    private static final Map<String, String> activeByWorld = new LinkedHashMap<>();
    private static boolean loaded = false;

    private PhoenixFavoriteSets() {}

    public static synchronized void loadForWorld(String id) {
        loaded = false;
        worldId = id;
        pages.clear();
        activeByWorld.clear();

        if (Files.exists(FILE)) {
            try (FileReader reader = new FileReader(FILE.toFile())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("scope")) {
                        try {
                            scope = Scope.valueOf(json.get("scope").getAsString());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (json.has("pages")) {
                        for (var entry : json.getAsJsonObject("pages").entrySet()) {
                            pages.put(entry.getKey(), entry.getValue().getAsJsonArray());
                        }
                    }
                    if (json.has("activeByWorld")) {
                        for (var entry : json.getAsJsonObject("activeByWorld").entrySet()) {
                            activeByWorld.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read EMI favorite pages at {}", FILE, e);
            }
        }

        String raw = activeByWorld.get(id);
        if (raw == null || !isVisibleKey(raw)) {
            raw = rawKey(DEFAULT_SET);
        }
        pages.putIfAbsent(raw, new JsonArray());
        activeRawKey = raw;

        EmiFavorites.load(pages.get(activeRawKey));
        loaded = true;
        refreshPanel();
    }

    public static synchronized void unload() {
        if (loaded) {
            snapshotActivePage();
            save();
        }
        loaded = false;
        worldId = null;
    }

    /** Called after every favorite add/remove so the active page stays in sync with EMI's live list. */
    public static synchronized void onFavoritesMutated() {
        if (!loaded) return;
        snapshotActivePage();
        save();
    }

    public static synchronized Scope getScope() {
        return scope;
    }

    public static synchronized void setScope(Scope newScope) {
        if (!loaded || newScope == scope) return;
        snapshotActivePage();
        scope = newScope;

        String raw = activeByWorld.get(worldId);
        if (raw == null || !isVisibleKey(raw)) {
            raw = rawKey(DEFAULT_SET);
        }
        pages.putIfAbsent(raw, new JsonArray());
        activeRawKey = raw;

        EmiFavorites.load(pages.get(activeRawKey));
        save();
        refreshPanel();
    }

    public static synchronized String getActiveSet() {
        return displayName(activeRawKey);
    }

    /** Page names visible under the current scope (every page, for GLOBAL; just this world's, for PER_WORLD). */
    public static synchronized List<String> getSetNames() {
        return pages.keySet().stream()
                .filter(PhoenixFavoriteSets::isVisibleKey)
                .map(PhoenixFavoriteSets::displayName)
                .sorted()
                .toList();
    }

    public static synchronized void switchTo(String name) {
        if (!loaded) return;
        String raw = rawKey(name);
        if (raw.equals(activeRawKey)) return;
        snapshotActivePage();
        activeRawKey = raw;
        pages.putIfAbsent(activeRawKey, new JsonArray());
        EmiFavorites.load(pages.get(activeRawKey));
        save();
        refreshPanel();
    }

    public static synchronized boolean createSet(String name) {
        if (!loaded) return false;
        String raw = rawKey(name);
        if (pages.containsKey(raw)) return false;
        pages.put(raw, new JsonArray());
        switchTo(name);
        return true;
    }

    public static synchronized void cycle() {
        if (!loaded) return;
        List<String> names = getSetNames();
        if (names.size() <= 1) return;
        int index = names.indexOf(displayName(activeRawKey));
        String next = names.get((index + 1) % names.size());
        switchTo(next);
    }

    private static String rawKey(String pageName) {
        return scope == Scope.PER_WORLD ? ("@" + worldId + "@" + pageName) : pageName;
    }

    private static boolean isVisibleKey(String key) {
        return scope == Scope.PER_WORLD ? key.startsWith("@" + worldId + "@") : !key.startsWith("@");
    }

    private static String displayName(String key) {
        if (scope == Scope.PER_WORLD && key.startsWith("@")) {
            int idx = key.indexOf('@', 1);
            if (idx >= 0) return key.substring(idx + 1);
        }
        return key;
    }

    private static void snapshotActivePage() {
        pages.put(activeRawKey, EmiFavorites.save());
    }

    private static void save() {
        if (worldId == null) return;
        try {
            Files.createDirectories(FILE.getParent());

            JsonObject json = new JsonObject();
            json.addProperty("scope", scope.name());

            JsonObject pagesJson = new JsonObject();
            for (var entry : pages.entrySet()) {
                pagesJson.add(entry.getKey(), entry.getValue());
            }
            json.add("pages", pagesJson);

            activeByWorld.put(worldId, activeRawKey);
            JsonObject activeJson = new JsonObject();
            for (var entry : activeByWorld.entrySet()) {
                activeJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("activeByWorld", activeJson);

            try (FileWriter writer = new FileWriter(FILE.toFile())) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save EMI favorite pages", e);
        }
    }

    private static void refreshPanel() {
        EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
    }
}
