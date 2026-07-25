package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuestTreeRegistry {

    private static final Map<ResourceLocation, QuestNode> ALL_QUESTS = new HashMap<>();
    private static final Map<ResourceLocation, QuestNode> ROOT_NODES = new HashMap<>();

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

    public static void registerBareQuestNode(QuestNode node) {
        if (node != null) ALL_QUESTS.put(node.getId(), node);
    }

    public static void injectDynamicQuestNode(QuestNode node, @Nullable ResourceLocation parentId) {
        if (node == null) return;
        ALL_QUESTS.put(node.getId(), node);

        if (parentId != null) {
            QuestNode parent = ALL_QUESTS.get(parentId);
            if (parent != null) {
                
                parent.addChild(node);
                ROOT_NODES.remove(node.getId());
            }
        } else {
            ROOT_NODES.put(node.getId(), node);
        }
    }

    @Nullable
    public static QuestNode getQuest(ResourceLocation id) {
        return ALL_QUESTS.get(id);
    }

    public static Map<ResourceLocation, QuestNode> getRootChapters() {
        return Map.copyOf(ROOT_NODES);
    }

    public static Map<ResourceLocation, QuestNode> getAllQuests() {
        return ALL_QUESTS;
    }

    public static void removeQuest(ResourceLocation id) {
        QuestNode removed = ALL_QUESTS.remove(id);
        ROOT_NODES.remove(id);
        if (removed == null) return;
        
        for (QuestNode n : ALL_QUESTS.values()) {
            
            n.removeChild(removed);
        }
    }

    public static void clear() {
        ALL_QUESTS.clear();
        ROOT_NODES.clear();
    }
}
