package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Writes the full quest registry back to disk.
 *
 * Called:
 * - When the player leaves the world (LoggingOut event)
 * - When Minecraft stops (ClientStopping event)
 * - Explicitly after edits in QuestCreatorScreen and TaskRewardEditorScreen
 *
 * Format per node: one .snbt + one .md (human-readable companion).
 * The .snbt is the source of truth; the .md is for author readability only.
 */
public class QuestFileSaver {

    public static void saveAllQuestsToDisk() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gameDirectory == null) return;

        Path base = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");

        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Determine each node's parent id by scanning the child lists
        java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> childToParent = new java.util.HashMap<>();
        for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
            for (QuestNode child : node.getChildren()) {
                childToParent.put(child.getId(), node.getId());
            }
        }

        int saved = 0;
        for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
            try {
                saveNode(base, node, childToParent.get(node.getId()));
                saved++;
            } catch (IOException e) {
                System.err
                        .println("[Phoenix Chronicles] Failed to save quest '" + node.getId() + "': " + e.getMessage());
            }
        }

        // Persist stub categories (categories with no quests)
        saveStubCategories(base);

        System.out.println("[Phoenix Chronicles] Saved " + saved + " quest(s) to disk.");
    }

    // ── Single node ───────────────────────────────────────────────────────────

    public static void saveNode(Path base, QuestNode node,
                                net.minecraft.resources.ResourceLocation parentId)
                                                                                   throws IOException {
        String id = node.getId().getPath();
        String title = node.getTitle().getString();
        String desc = node.getDescription().getString();
        String category = node.getCategory() != null ? node.getCategory() : "MAIN";
        String shape = node.getShapeType() != null ? node.getShapeType() : "SQUARE";
        String iconItem = node.getIconItemId();        // "" if none
        String parent = parentId != null ? parentId.getPath() : "none";

        // ── .snbt ─────────────────────────────────────────────────────────────
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("title", title);
        tag.putString("description", desc);
        tag.putString("category", category);
        tag.putString("shape", shape);
        tag.putString("parent", parent);
        tag.putInt("positionX", node.getCustomX());
        tag.putInt("positionY", node.getCustomY());
        if (!iconItem.isEmpty()) tag.putString("icon_item", iconItem);

        // Repeat behaviour
        tag.putString("repeat_mode", node.getRepeatMode().name());
        tag.putInt("repeat_cooldown_hours", node.getRepeatCooldownHours());

        // Prerequisite gate
        tag.putBoolean("require_all_prereqs", node.getRequireAllPrerequisites());

        // Rewards
        if (!node.getRewards().isEmpty()) {
            net.minecraft.nbt.ListTag rewardList = new net.minecraft.nbt.ListTag();
            for (QuestReward r : node.getRewards()) rewardList.add(r.serializeNBT());
            tag.put("rewards", rewardList);
        }

        Path snbtPath = base.resolve(id + ".snbt");
        Files.createDirectories(snbtPath.getParent());
        Files.writeString(snbtPath, tag.toString(), StandardCharsets.UTF_8);

        // ── .md ───────────────────────────────────────────────────────────────
        Path mdPath = base.resolve(id + ".md");
        // Only write .md if it doesn't already exist (preserve author edits)
        if (!Files.exists(mdPath)) {
            Files.writeString(mdPath,
                    "# " + title + "\n\n" + (desc.isEmpty() ? "" : desc + "\n"),
                    StandardCharsets.UTF_8);
        }
    }

    // ── Stub categories ───────────────────────────────────────────────────────

    private static void saveStubCategories(Path base) {
        try {
            // Collect categories that come from quests
            Set<String> questCats = new HashSet<>();
            questCats.add("ALL");
            for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
                if (n.getCategory() != null) questCats.add(n.getCategory());
            }

            // Read the existing categories.txt to find any stubs
            Path catFile = base.resolve("categories.txt");
            java.util.List<String> stubs = new java.util.ArrayList<>();
            if (Files.exists(catFile)) {
                for (String line : Files.readAllLines(catFile, StandardCharsets.UTF_8)) {
                    String c = line.trim().toUpperCase();
                    if (!c.isEmpty() && !questCats.contains(c)) stubs.add(c);
                }
            }

            // Persist (overwrite with pruned list)
            Files.writeString(catFile, String.join("\n", stubs), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to save categories.txt: " + e.getMessage());
        }
    }
}
