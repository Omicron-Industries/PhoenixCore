package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps category names to flag expressions, allowing whole categories of quests
 * to be toggled on/off at world load via the same flag system used by enable_if.
 *
 * ── Config file ───────────────────────────────────────────────────────────────
 *
 *   config/phoenix_chronicles/category_flags.snbt
 *
 *   {
 *     EXPERT_MODE: "kjs:difficulty_expert",
 *     NORMAL_MODE: "!kjs:difficulty_expert",
 *     SKYBLOCK:    "kjs:mode_skyblock"
 *   }
 *
 * Categories not listed are always enabled.
 * Expressions use the same syntax as per-quest enable_if (including ! negation,
 * mod:, kjs:, rule:, config: prefixes).
 *
 * ── Evaluation ────────────────────────────────────────────────────────────────
 *
 * Checked inside QuestNode.isFlagEnabled(), so all existing call-sites
 * (rendering, progress tracking, search) automatically respect category flags.
 * Evaluation is live — PhoenixQuestFlags re-evaluates each call, so
 * dynamic conditions (registerCondition) work here too.
 */
public final class CategoryFlagRegistry {

    private CategoryFlagRegistry() {}

    private static final Map<String, String> categoryExpressions = new ConcurrentHashMap<>();

    /**
     * Loads category_flags.snbt from the given config directory.
     * Called during ServerStartingEvent, after PhoenixQuestFlags.invalidateCaches().
     * Safe to call multiple times — clears previous state each time.
     */
    public static void load(Path configDir) {
        categoryExpressions.clear();
        Path file = configDir.resolve("category_flags.snbt");
        if (!Files.exists(file)) return;

        try {
            String content = Files.readString(file);
            CompoundTag tag = TagParser.parseTag(content);
            for (String key : tag.getAllKeys()) {
                String expr = tag.getString(key).trim();
                if (!expr.isEmpty()) {
                    categoryExpressions.put(key.toUpperCase(), expr);
                }
            }
            if (!categoryExpressions.isEmpty()) {
                System.out.println("[Phoenix Chronicles] Loaded category flags for: " + categoryExpressions.keySet());
            }
        } catch (Exception e) {
            System.err.println("[Phoenix Chronicles] Failed to load category_flags.snbt: " + e.getMessage());
        }
    }

    /**
     * Returns true if the given category is currently enabled.
     * Categories with no entry in the config are always enabled.
     */
    public static boolean isCategoryEnabled(String category) {
        if (category == null) return true;
        String expr = categoryExpressions.get(category.toUpperCase());
        if (expr == null) return true;
        return PhoenixQuestFlags.evaluate(expr);
    }

    /** Clears all loaded category flags (called implicitly by load()). */
    public static void clear() {
        categoryExpressions.clear();
    }
}
