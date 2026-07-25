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

        saveStubCategories(base);

        System.out.println("[Phoenix Chronicles] Saved " + saved + " quest(s) to disk.");
    }

    public static void saveNode(Path base, QuestNode node,
                                net.minecraft.resources.ResourceLocation parentId)
                                                                                   throws IOException {
        String id = node.getId().getPath();
        String title = node.getTitle().getString();
        String desc = node.getDescription().getString();
        String category = node.getCategory() != null ? node.getCategory() : "MAIN";
        String shape = node.getShapeType() != null ? node.getShapeType() : "SQUARE";
        String iconItem = node.getIconItemId();
        String parent = parentId != null ? parentId.getPath() : "none";

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

        if (!node.getSubtitle().isEmpty()) tag.putString("subtitle", node.getSubtitle());
        tag.putString("visibility", node.getVisibility().name());
        if (node.getEnableIf() != null) tag.putString("enable_if", node.getEnableIf());
        if (node.getTaskMinCount() > 0) tag.putInt("task_min_count", node.getTaskMinCount());
        if (node.isHideDepLine()) tag.putBoolean("hide_dep_line", true);
        if (node.isDisabledBlocksChildren()) tag.putBoolean("disabled_blocks_children", true);
        if (node.isShared()) tag.putBoolean("shared", true);

        tag.putString("repeat_mode", node.getRepeatMode().name());
        tag.putInt("repeat_cooldown_hours", node.getRepeatCooldownHours());

        tag.putBoolean("require_all_prereqs", node.getRequireAllPrerequisites());
        if (!node.getPrerequisites().isEmpty()) {
            net.minecraft.nbt.ListTag prereqList = new net.minecraft.nbt.ListTag();
            for (QuestNode p : node.getPrerequisites()) {
                CompoundTag pTag = new CompoundTag();
                pTag.putString("id", p.getId().getPath());
                if (node.isPrereqForbidden(p.getId())) {
                    pTag.putBoolean("forbidden", true);
                } else {
                    pTag.putBoolean("required", node.isPrereqRequired(p.getId()));
                }
                if (node.isPrereqLink(p.getId())) pTag.putBoolean("link", true);
                prereqList.add(pTag);
            }
            tag.put("prerequisites", prereqList);
        }
        if (node.getOptionalPrereqMinCount() != 0)
            tag.putInt("optional_prereq_min_count", node.getOptionalPrereqMinCount());

        if (!node.getTasks().isEmpty()) {
            net.minecraft.nbt.ListTag taskList = new net.minecraft.nbt.ListTag();
            for (QuestTask t : node.getTasks()) {
                CompoundTag tTag = t.serializeNBT();
                tTag.putString("task_id", t.getTaskId().toString());
                tTag.putString("description",
                        net.minecraft.network.chat.Component.Serializer.toJson(t.getDescription()));
                tTag.putBoolean("optional", t.isOptional());
                taskList.add(tTag);
            }
            tag.put("tasks", taskList);
        }

        if (!node.getRewards().isEmpty()) {
            net.minecraft.nbt.ListTag rewardList = new net.minecraft.nbt.ListTag();
            for (QuestReward r : node.getRewards()) rewardList.add(r.serializeNBT());
            tag.put("rewards", rewardList);
        }

        if (!node.getEmergencyItems().isEmpty()) {
            tag.put("emergency_items", node.serializeEmergencyItems());
        }

        Path snbtPath = base.resolve(id + ".snbt");
        Files.createDirectories(snbtPath.getParent());
        Files.writeString(snbtPath, tag.toString(), StandardCharsets.UTF_8);

        Path mdPath = base.resolve(id + ".md");

        if (!Files.exists(mdPath)) {
            Files.writeString(mdPath,
                    "# " + title + "\n\n" + (desc.isEmpty() ? "" : desc + "\n"),
                    StandardCharsets.UTF_8);
        }
    }

    private static void saveStubCategories(Path base) {
        try {

            Set<String> questCats = new HashSet<>();
            questCats.add("ALL");
            for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
                if (n.getCategory() != null) questCats.add(n.getCategory());
            }

            Path catFile = base.resolve("categories.txt");
            java.util.List<String> stubs = new java.util.ArrayList<>();
            if (Files.exists(catFile)) {
                for (String line : Files.readAllLines(catFile, StandardCharsets.UTF_8)) {
                    String c = line.trim().toUpperCase();
                    if (!c.isEmpty() && !questCats.contains(c)) stubs.add(c);
                }
            }

            Files.writeString(catFile, String.join("\n", stubs), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to save categories.txt: " + e.getMessage());
        }
    }
}
