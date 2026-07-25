package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CategoryFlagRegistry {

    private CategoryFlagRegistry() {}

    private static final Map<String, String> categoryExpressions = new ConcurrentHashMap<>();

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

    public static boolean isCategoryEnabled(String category) {
        if (category == null) return true;
        String expr = categoryExpressions.get(category.toUpperCase());
        if (expr == null) return true;
        return PhoenixQuestFlags.evaluate(expr);
    }

    public static void clear() {
        categoryExpressions.clear();
    }
}
