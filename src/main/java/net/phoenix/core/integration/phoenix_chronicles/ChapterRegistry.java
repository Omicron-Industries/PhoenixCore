package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChapterRegistry {

    private static final Map<ResourceLocation, ChapterDefinition> CHAPTERS = new LinkedHashMap<>();

    public static void register(ChapterDefinition chapter) {
        if (chapter != null) {
            CHAPTERS.put(chapter.getId(), chapter);
        }
    }

    @Nullable
    public static ChapterDefinition get(ResourceLocation id) {
        return CHAPTERS.get(id);
    }

    public static List<ChapterDefinition> getAllChapters() {
        return Collections.unmodifiableList(new ArrayList<>(CHAPTERS.values()));
    }

    public static void clear() {
        CHAPTERS.clear();
    }
}
