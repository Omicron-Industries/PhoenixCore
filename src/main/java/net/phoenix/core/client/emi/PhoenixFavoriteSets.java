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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final List<String> pageOrder = new ArrayList<>();
    private static boolean loaded = false;

    private PhoenixFavoriteSets() {}

    public static synchronized void loadForWorld(String id) {
        loaded = false;
        worldId = id;
        pages.clear();
        activeByWorld.clear();
        pageOrder.clear();

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
                    if (json.has("pageOrder")) {
                        for (var el : json.getAsJsonArray("pageOrder")) {
                            pageOrder.add(el.getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read EMI favorite pages at {}", FILE, e);
            }
        }

        activeRawKey = resolveActiveKey();
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
        activeRawKey = resolveActiveKey();
        EmiFavorites.load(pages.get(activeRawKey));
        save();
        refreshPanel();
    }

    private static String resolveActiveKey() {
        String raw = activeByWorld.get(worldId);
        if (raw != null && isVisibleKey(raw) && pages.containsKey(raw)) {
            return raw;
        }

        String defaultKey = rawKey(DEFAULT_SET);
        if (!pages.containsKey(defaultKey)) {
            String otherScopeDefaultKey = scope == Scope.PER_WORLD ? DEFAULT_SET : ("@" + worldId + "@" + DEFAULT_SET);
            JsonArray migrated = pages.get(otherScopeDefaultKey);
            pages.put(defaultKey, migrated != null ? migrated.deepCopy() : new JsonArray());
        }
        return defaultKey;
    }

    public static synchronized String getActiveSet() {
        return displayName(activeRawKey);
    }

    public static synchronized List<String> getSetNames() {
        List<String> visible = pages.keySet().stream().filter(PhoenixFavoriteSets::isVisibleKey).toList();

        List<String> ordered = new ArrayList<>();
        for (String raw : pageOrder) {
            if (visible.contains(raw)) ordered.add(raw);
        }
        List<String> remaining = new ArrayList<>(visible);
        remaining.removeAll(ordered);
        remaining.sort(String::compareTo);
        ordered.addAll(remaining);

        return ordered.stream().map(PhoenixFavoriteSets::displayName).toList();
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

    public static synchronized boolean deleteSet(String name) {
        if (!loaded) return false;
        String raw = rawKey(name);
        if (!pages.containsKey(raw)) return false;

        List<String> visible = getSetNames();
        if (visible.size() <= 1) return false;

        boolean wasActive = raw.equals(activeRawKey);
        pages.remove(raw);
        pageOrder.remove(raw);
        activeByWorld.entrySet().removeIf(e -> e.getValue().equals(raw));

        if (wasActive) {
            List<String> remaining = getSetNames();
            String next = remaining.contains(DEFAULT_SET) ? DEFAULT_SET : remaining.get(0);
            activeRawKey = rawKey(next);
            pages.putIfAbsent(activeRawKey, new JsonArray());
            EmiFavorites.load(pages.get(activeRawKey));
        }

        save();
        refreshPanel();
        return true;
    }

    public static synchronized boolean renameSet(String oldName, String newName) {
        if (!loaded || oldName.equals(newName) || newName.isBlank()) return false;
        String oldRaw = rawKey(oldName);
        String newRaw = rawKey(newName);
        if (!pages.containsKey(oldRaw) || pages.containsKey(newRaw)) return false;

        pages.put(newRaw, pages.remove(oldRaw));

        int idx = pageOrder.indexOf(oldRaw);
        if (idx >= 0) pageOrder.set(idx, newRaw);

        if (oldRaw.equals(activeRawKey)) activeRawKey = newRaw;
        for (var entry : activeByWorld.entrySet()) {
            if (entry.getValue().equals(oldRaw)) entry.setValue(newRaw);
        }

        save();
        refreshPanel();
        return true;
    }

    public static synchronized void moveUp(String name) {
        reorder(name, -1);
    }

    public static synchronized void moveDown(String name) {
        reorder(name, 1);
    }

    private static void reorder(String name, int delta) {
        if (!loaded) return;
        List<String> visible = getSetNames();
        int idx = visible.indexOf(name);
        int target = idx + delta;
        if (idx < 0 || target < 0 || target >= visible.size()) return;

        for (String display : visible) {
            String raw = rawKey(display);
            if (!pageOrder.contains(raw)) pageOrder.add(raw);
        }

        int ia = pageOrder.indexOf(rawKey(visible.get(idx)));
        int ib = pageOrder.indexOf(rawKey(visible.get(target)));
        if (ia < 0 || ib < 0) return;
        Collections.swap(pageOrder, ia, ib);

        save();
        refreshPanel();
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

            JsonArray orderJson = new JsonArray();
            for (String raw : pageOrder) orderJson.add(raw);
            json.add("pageOrder", orderJson);

            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to save EMI favorite pages", e);
        }
    }

    private static void refreshPanel() {
        try {
            EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
        } catch (Exception ignored) {}
    }
}
