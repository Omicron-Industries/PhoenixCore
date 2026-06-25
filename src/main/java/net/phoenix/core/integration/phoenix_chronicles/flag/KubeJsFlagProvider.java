package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Reads flags exported from KubeJS startup scripts.
 *
 * Because KubeJS runs as part of game startup, KubeJS scripts can write a JSON
 * file before any quests are evaluated. This provider reads that file.
 *
 * ── KubeJS startup script (startup_scripts/phoenix_flags.js) ─────────────────
 *
 * const flags = {
 * expert_mode: Platform.isLoaded("ftbquests"),
 * has_create: Platform.isLoaded("create"),
 * has_tech: Platform.isLoaded("create") || Platform.isLoaded("mekanism"),
 * pack_tier: 3,
 * pack_mode: "expert"
 * }
 * const file = new java.io.File("config/phoenix_chronicles/kjs_flags.json")
 * file.getParentFile().mkdirs()
 * file.text = JSON.stringify(flags, null, 2)
 *
 * ── Quest SNBT ───────────────────────────────────────────────────────────────
 *
 * enable_if: "kjs:expert_mode"
 * enable_if: "kjs:pack_tier>=2"
 * enable_if: "kjs:pack_mode=expert"
 * enable_if: "kjs:has_create,kjs:has_tech"
 *
 * ── File location ─────────────────────────────────────────────────────────────
 *
 * config/phoenix_chronicles/kjs_flags.json
 *
 * Flat JSON object only — nested objects are supported using dot-path keys.
 * Arrays are stored as their JSON string representation.
 *
 * ── Cache ─────────────────────────────────────────────────────────────────────
 *
 * Re-read every 10 seconds so that changes made during a dev session
 * (e.g. hot-reloading KubeJS) take effect without a full restart.
 * Call {@link #invalidate()} on world load to flush immediately.
 */
public class KubeJsFlagProvider implements QuestFlagProvider {

    private static final String FLAGS_FILE = "kjs_flags.json";
    private static final long CACHE_TTL_MS = 10_000;

    private volatile Map<String, String> cachedFlags = null;
    private volatile long loadedAt = 0;

    @Override
    public String prefix() {
        return "kjs";
    }

    @Override
    public boolean evaluate(String expression, @Nullable MinecraftServer server) {
        FlagExpression expr = FlagExpression.parse(expression);
        return expr.test(getFlags(server).get(expr.key));
    }

    public void invalidate() {
        cachedFlags = null;
    }

    // ── File loading ──────────────────────────────────────────────────────────

    private Map<String, String> getFlags(@Nullable MinecraftServer server) {
        if (cachedFlags != null && System.currentTimeMillis() - loadedAt < CACHE_TTL_MS) {
            return cachedFlags;
        }

        Path configDir = server != null ? server.getServerDirectory().toPath().resolve("config") :
                FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve("phoenix_chronicles").resolve(FLAGS_FILE);

        Map<String, String> flags = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(r);
                if (root.isJsonObject()) flatten(root.getAsJsonObject(), "", flags);
            } catch (Exception e) {
                System.err.println("[Phoenix Chronicles] kjs flag: failed to read kjs_flags.json: " + e.getMessage());
            }
        }

        cachedFlags = flags;
        loadedAt = System.currentTimeMillis();
        return flags;
    }

    private void flatten(JsonObject obj, String prefix, Map<String, String> out) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonObject()) flatten(v.getAsJsonObject(), key, out);
            else if (v.isJsonPrimitive()) out.put(key, v.getAsString());
            else out.put(key, v.toString());
        }
    }
}
