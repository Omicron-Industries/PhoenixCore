package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class QuestFileLoader {

    private record QuestRecord(
                               ResourceLocation id,
                               String title,
                               String description,
                               String category,
                               String shape,
                               String iconItemId,
                               int posX,
                               int posY,
                               ResourceLocation parentId,
                               QuestNode.RepeatMode repeatMode,
                               int repeatCooldownHours,
                               boolean requireAllPrereqs,
                               List<QuestReward> rewards) {}

    // ── Public entry point ────────────────────────────────────────────────────

    public static void reloadAllQuestsFromDisk() {
        QuestTreeRegistry.clear();

        Path configFolder = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");
        if (!Files.exists(configFolder)) return;

        List<QuestRecord> records = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(configFolder)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".snbt"))
                    .forEach(file -> {
                        QuestRecord rec = parseFile(file);
                        if (rec != null) records.add(rec);
                    });
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to walk config folder: " + e.getMessage());
            return;
        }

        // Phase 1 — construct and bare-register all nodes
        for (QuestRecord rec : records) {
            QuestNode node = new QuestNode(rec.id(),
                    Component.literal(rec.title()), Component.literal(rec.description()));
            node.setCategory(rec.category());
            node.setShapeType(rec.shape());
            node.setCustomX(rec.posX());
            node.setCustomY(rec.posY());
            if (!rec.iconItemId().isEmpty()) node.setIconItemById(rec.iconItemId());
            node.setRepeatMode(rec.repeatMode());
            node.setRepeatCooldownHours(rec.repeatCooldownHours());
            node.setRequireAllPrerequisites(rec.requireAllPrereqs());
            for (QuestReward r : rec.rewards()) node.addReward(r);
            QuestTreeRegistry.registerBareQuestNode(node);
        }

        // Phase 2 — wire parent→child, prerequisites, and roots
        Set<ResourceLocation> hasParent = new HashSet<>();
        for (QuestRecord rec : records) if (rec.parentId() != null) hasParent.add(rec.id());

        for (QuestRecord rec : records) {
            QuestNode node = QuestTreeRegistry.getQuest(rec.id());
            if (node == null) continue;

            if (rec.parentId() != null) {
                QuestNode parent = QuestTreeRegistry.getQuest(rec.parentId());
                if (parent != null) {
                    parent.addChild(node);
                } else {
                    System.err.println("[Phoenix Chronicles] Parent '" + rec.parentId() + "' not found for '" +
                            rec.id() + "' — treating as root.");
                    QuestTreeRegistry.registerRootChapter(node);
                }
            } else {
                QuestTreeRegistry.registerRootChapter(node);
            }
        }

        System.out.println("[Phoenix Chronicles] Loaded " + records.size() + " quest(s) from disk (" +
                QuestTreeRegistry.getRootChapters().size() + " root(s)).");
    }

    // ── File parser ───────────────────────────────────────────────────────────

    private static QuestRecord parseFile(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            CompoundTag tag = TagParser.parseTag(raw);

            String fileName = file.getFileName().toString();
            String idStr = tag.contains("id") && !tag.getString("id").isEmpty() ? tag.getString("id") :
                    fileName.substring(0, fileName.lastIndexOf('.'));
            ResourceLocation id = new ResourceLocation("phoenixcore", idStr.toLowerCase());

            String title = tag.contains("title") ? tag.getString("title") : "Unnamed Quest";
            String desc = tag.contains("description") ? tag.getString("description") : "";
            String category = tag.contains("category") ? tag.getString("category") : "MAIN";
            String shape = tag.contains("shape") ? tag.getString("shape") : "SQUARE";
            String iconItem = tag.contains("icon_item") ? tag.getString("icon_item") : "";
            int posX = tag.contains("positionX") ? tag.getInt("positionX") : 40;
            int posY = tag.contains("positionY") ? tag.getInt("positionY") : 70;

            String parentStr = tag.contains("parent") ? tag.getString("parent") : "none";
            ResourceLocation parentId = (!parentStr.isEmpty() && !parentStr.equals("none")) ?
                    new ResourceLocation("phoenixcore", parentStr.toLowerCase()) : null;

            // Repeat behaviour
            QuestNode.RepeatMode repeatMode = QuestNode.RepeatMode.NONE;
            if (tag.contains("repeat_mode")) {
                try {
                    repeatMode = QuestNode.RepeatMode.valueOf(tag.getString("repeat_mode").toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            int repeatCooldownHours = tag.contains("repeat_cooldown_hours") ? tag.getInt("repeat_cooldown_hours") : 24;

            // Prerequisite gate
            boolean requireAllPrereqs = !tag.contains("require_all_prereqs") || tag.getBoolean("require_all_prereqs");

            // Rewards
            List<QuestReward> rewards = new ArrayList<>();
            if (tag.contains("rewards")) {
                ListTag rewardList = tag.getList("rewards", Tag.TAG_COMPOUND);
                for (int ri = 0; ri < rewardList.size(); ri++) {
                    QuestReward r = QuestReward.deserializeNBT(rewardList.getCompound(ri));
                    if (r != null) rewards.add(r);
                }
            }

            return new QuestRecord(id, title, desc, category.toUpperCase(), shape.toUpperCase(),
                    iconItem, posX, posY, parentId, repeatMode, repeatCooldownHours, requireAllPrereqs, rewards);

        } catch (Exception e) {
            System.err.println(
                    "[Phoenix Chronicles] Failed to parse quest file '" + file.getFileName() + "': " + e.getMessage());
            return null;
        }
    }
}
