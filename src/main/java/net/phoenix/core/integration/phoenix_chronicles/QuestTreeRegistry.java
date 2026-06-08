package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuestTreeRegistry {

    private static final Map<ResourceLocation, QuestNode> ALL_QUESTS = new HashMap<>();
    private static final Map<ResourceLocation, QuestNode> ROOT_NODES = new HashMap<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a root node and recursively registers all of its descendants.
     * Called by {@link ChronicleDataLoader} after assembling the full tree.
     */
    public static void registerRootChapter(QuestNode root) {
        if (root == null) return;
        ROOT_NODES.put(root.getId(), root);
        registerRecursively(root);
    }

    private static void registerRecursively(QuestNode node) {
        if (node == null) return;
        ALL_QUESTS.put(node.getId(), node);
        for (QuestNode child : node.getChildren()) {
            registerRecursively(child);
        }
    }

    /**
     * Registers a single node without touching the root map.
     * Used by {@link QuestFileLoader} / content loaders for bare nodes
     * whose tree position is not yet known.
     */
    public static void registerBareQuestNode(QuestNode node) {
        if (node != null) ALL_QUESTS.put(node.getId(), node);
    }

    /**
     * Live-injects a dynamically created node (e.g. from the creator screen)
     * into both maps and wires up the parent link if one is provided.
     */
    public static void injectDynamicQuestNode(QuestNode node, @Nullable ResourceLocation parentId) {
        if (node == null) return;
        ALL_QUESTS.put(node.getId(), node);

        if (parentId != null) {
            QuestNode parent = ALL_QUESTS.get(parentId);
            if (parent != null) {
                // addChild guards against duplicates internally
                parent.addChild(node);
                ROOT_NODES.remove(node.getId());
            }
        } else {
            ROOT_NODES.put(node.getId(), node);
        }
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    @Nullable
    public static QuestNode getQuest(ResourceLocation id) {
        return ALL_QUESTS.get(id);
    }

    /** All root nodes (nodes with no parent), in insertion order. */
    public static Map<ResourceLocation, QuestNode> getRootChapters() {
        return Map.copyOf(ROOT_NODES);
    }

    public static Map<ResourceLocation, QuestNode> getAllQuests() {
        return ALL_QUESTS;
    }

    /** Removes a quest from both maps and unlinks it from any parent's children list. */
    public static void removeQuest(ResourceLocation id) {
        QuestNode removed = ALL_QUESTS.remove(id);
        ROOT_NODES.remove(id);
        if (removed == null) return;
        // Unlink from any parent that references this node as a child
        for (QuestNode n : ALL_QUESTS.values()) {
            // QuestNode.getChildren() is unmodifiable; access via reflection-free helper
            n.removeChild(removed);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static void clear() {
        ALL_QUESTS.clear();
        ROOT_NODES.clear();
    }
}
