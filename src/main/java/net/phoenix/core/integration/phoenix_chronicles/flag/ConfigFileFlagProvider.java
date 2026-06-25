package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * Reads flag values from config files in the Minecraft config directory.
 *
 * Supported formats: .toml, .json, .properties
 * Cache TTL: 15 seconds (re-reads automatically; no restart needed for value changes).
 *
 * ── Quest SNBT ───────────────────────────────────────────────────────────────
 *
 * enable_if: "config:packmode.toml#mode=expert"
 * enable_if: "config:mypack/settings.json#features.expert_quests"
 * enable_if: "config:packmode.properties#pack.mode!=easy"
 *
 * ── Expression format ────────────────────────────────────────────────────────
 *
 * config:FILENAME#KEY[OPERATOR VALUE]
 *
 * FILENAME — relative to the Minecraft config directory.
 * Subdirectories are allowed: "mypack/settings.json"
 * KEY — dot-separated path within the file:
 * TOML/JSON: "section.subsection.key"
 * Properties: "my.property.key"
 * OPERATOR — one of = != > >= < <= (omit for a truthy existence check)
 * VALUE — literal string or number to compare against
 *
 * ── TOML example (packmode.toml) ─────────────────────────────────────────────
 *
 * [general]
 * mode = "expert"
 *
 * → enable_if: "config:packmode.toml#general.mode=expert"
 *
 * ── JSON example (mypack/settings.json) ──────────────────────────────────────
 *
 * { "features": { "expert_quests": true, "tier": 3 } }
 *
 * → enable_if: "config:mypack/settings.json#features.expert_quests"
 * → enable_if: "config:mypack/settings.json#features.tier>=2"
 *
 * ── Properties example (packmode.properties) ─────────────────────────────────
 *
 * pack.mode=expert
 * pack.tier=3
 *
 * → enable_if: "config:packmode.properties#pack.mode=expert"
 */
public class ConfigFileFlagProvider implements QuestFlagProvider {

    private static final long CACHE_TTL_MS = 15_000;

    private record CachedFile(Map<String, String> flat, long loadedAt) {}

    private final Map<String, CachedFile> cache = new ConcurrentHashMap<>();

    @Override
    public String prefix() {
        return "config";
    }

    @Override
    public boolean evaluate(String expression, @Nullable MinecraftServer server) {
        int hash = expression.indexOf('#');
        if (hash < 0) {
            System.err.println("[Phoenix Chronicles] config flag missing '#': " + expression);
            return false;
        }
        String filename = expression.substring(0, hash).trim();
        String rest = expression.substring(hash + 1).trim();

        FlagExpression expr = FlagExpression.parse(rest);
        Map<String, String> flat = loadFlat(filename, server);
        return expr.test(flat.get(expr.key));
    }

    // ── Config loading ────────────────────────────────────────────────────────

    private Map<String, String> loadFlat(String filename, @Nullable MinecraftServer server) {
        CachedFile cached = cache.get(filename);
        if (cached != null && System.currentTimeMillis() - cached.loadedAt() < CACHE_TTL_MS) {
            return cached.flat();
        }

        Path configDir = server != null ? server.getServerDirectory().toPath().resolve("config") :
                FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve(filename);

        Map<String, String> flat = readFlat(file);
        cache.put(filename, new CachedFile(flat, System.currentTimeMillis()));
        return flat;
    }

    /** Forces all cached files to re-read on the next access (call on world load). */
    public void invalidateCache() {
        cache.clear();
    }

    private Map<String, String> readFlat(Path file) {
        if (!Files.exists(file)) return Map.of();
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".json")) return flattenJson(file);
            if (name.endsWith(".toml")) return flattenToml(file);
            if (name.endsWith(".properties")) return flattenProperties(file);
        } catch (Exception e) {
            System.err.println(
                    "[Phoenix Chronicles] config flag: failed to read " + file.getFileName() + ": " + e.getMessage());
        }
        return Map.of();
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private Map<String, String> flattenJson(Path file) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (root.isJsonObject()) flattenJsonObject(root.getAsJsonObject(), "", out);
        }
        return out;
    }

    private void flattenJsonObject(JsonObject obj, String prefix, Map<String, String> out) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonObject()) {
                flattenJsonObject(v.getAsJsonObject(), key, out);
            } else if (v.isJsonArray()) {
                out.put(key, v.toString()); // arrays stored as-is
            } else {
                String raw = v.getAsString();
                out.put(key, raw);
            }
        }
    }

    // ── TOML ─────────────────────────────────────────────────────────────────

    /**
     * Minimal TOML reader — handles key=value pairs and [section] headers.
     * Does not support arrays of tables, multi-line strings, or inline tables,
     * which are uncommon in simple pack-config files. Use JSON for complex data.
     */
    private Map<String, String> flattenToml(Path file) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        String section = "";
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Section header [section] or [section.subsection]
            if (line.startsWith("[") && line.endsWith("]") && !line.startsWith("[[")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int eq = line.indexOf('=');
            if (eq > 0) {
                String k = line.substring(0, eq).trim();
                String v = stripTomlValue(line.substring(eq + 1).trim());
                String fullKey = section.isEmpty() ? k : section + "." + k;
                out.put(fullKey, v);
            }
        }
        return out;
    }

    private String stripTomlValue(String raw) {
        // Strip inline comment
        int comment = raw.indexOf('#');
        if (comment > 0) raw = raw.substring(0, comment).trim();
        // Strip surrounding quotes
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    // ── Properties ────────────────────────────────────────────────────────────

    private Map<String, String> flattenProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(r);
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            out.put(key, props.getProperty(key));
        }
        return out;
    }
}
