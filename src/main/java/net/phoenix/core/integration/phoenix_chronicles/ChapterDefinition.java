package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ChapterDefinition {

    private final ResourceLocation id;
    private final String displayName;
    private final String category;
    private final List<ChapterNodeEntry> nodes;

    public ChapterDefinition(ResourceLocation id, String displayName, String category, List<ChapterNodeEntry> nodes) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.nodes = nodes;
    }

    public ResourceLocation getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public List<ChapterNodeEntry> getNodes() {
        return nodes;
    }

    public ChapterNodeEntry getEntryFor(ResourceLocation questId) {
        for (ChapterNodeEntry entry : nodes) {
            if (entry.questId().equals(questId)) return entry;
        }
        return null;
    }

    public record ChapterNodeEntry(
                                   ResourceLocation questId,
                                   String shape,
                                   int x,
                                   int y,
                                   boolean visible,
                                   List<ResourceLocation> dependsOn) {

        public boolean hasPrerequisites() {
            return dependsOn != null && !dependsOn.isEmpty();
        }
    }
}
