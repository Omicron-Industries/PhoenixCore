package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Runtime representation of one chapter definition file.
 * Loaded from config/phoenix_chronicles/chapters/<id>.yml
 *
 * A chapter defines:
 * - which quests appear in its canvas view
 * - where each quest node sits (position)
 * - what shape each node uses
 * - which other nodes are prerequisites WITHIN this chapter
 * - per-chapter visibility flag
 *
 * The same quest (QuestNode) can appear in multiple chapters at
 * different positions, with different shapes or visibility, because
 * all of that data lives here rather than on the QuestNode itself.
 */
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

    /**
     * Finds the entry for a specific quest within this chapter, or null if
     * this chapter doesn't include that quest.
     */
    public ChapterNodeEntry getEntryFor(ResourceLocation questId) {
        for (ChapterNodeEntry entry : nodes) {
            if (entry.questId().equals(questId)) return entry;
        }
        return null;
    }

    // -------------------------------------------------------------------------

    /**
     * Per-quest layout data scoped to a single chapter.
     *
     * @param questId   The quest this entry describes (matches a quests/<id>.md file)
     * @param shape     Visual shape: SQUARE, CIRCLE, or GEAR
     * @param x         Canvas X position within this chapter's layout
     * @param y         Canvas Y position within this chapter's layout
     * @param visible   Whether to show this node in this chapter's view at all
     * @param dependsOn Quest IDs that must be COMPLETED before this one unlocks,
     *                  scoped to this chapter (does not affect other chapters)
     */
    public record ChapterNodeEntry(
                                   ResourceLocation questId,
                                   String shape,
                                   int x,
                                   int y,
                                   boolean visible,
                                   List<ResourceLocation> dependsOn) {

        /** Convenience: does this entry declare any prerequisites? */
        public boolean hasPrerequisites() {
            return dependsOn != null && !dependsOn.isEmpty();
        }
    }
}
