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
import java.util.*;
import java.util.stream.Stream;

public class QuestFileLoader {

    public static final List<String> LOAD_ERRORS = new ArrayList<>();

    private record QuestRecord(
                               ResourceLocation id,
                               String title,
                               String description,
                               String subtitle,
                               String category,
                               String shape,
                               String iconItemId,
                               int posX,
                               int posY,
                               QuestNode.Visibility visibility,
                               int taskMinCount,
                               ResourceLocation parentId,
                               QuestNode.RepeatMode repeatMode,
                               int repeatCooldownHours,
                               boolean requireAllPrereqs,
                               List<QuestReward> rewards,
                               List<QuestTask> tasks,
                               net.minecraft.nbt.ListTag emergencyItems,
                               Map<String, Boolean> prereqRequired,
                               int optionalPrereqMinCount,
                               String enableIf,
                               Set<String> prereqForbidden,
                               Set<String> prereqLink,
                               boolean hideDepLine,
                               boolean disabledBlocksChildren,
                               boolean shared,
                               List<TutorialStep> tutorialSteps) {}

    public static void loadAdditiveFromDisk(Path configDir) {
        if (!Files.exists(configDir)) return;
        LOAD_ERRORS.clear();

        List<QuestRecord> records = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(configDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".snbt"))
                    .forEach(file -> {
                        QuestRecord rec = parseFile(file);
                        if (rec != null && QuestTreeRegistry.getQuest(rec.id()) == null)
                            records.add(rec);
                    });
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to walk config folder: " + e.getMessage());
            return;
        }

        if (records.isEmpty()) return;
        wireAndRegister(records);
        System.out.println("[Phoenix Chronicles] Loaded " + records.size() +
                " editor quest(s) from config dir.");
    }

    public static void reloadAllQuestsFromDisk() {
        LOAD_ERRORS.clear();
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

        wireAndRegister(records);
        System.out.println("[Phoenix Chronicles] Loaded " + records.size() + " quest(s) from disk (" +
                QuestTreeRegistry.getRootChapters().size() + " root(s)).");
    }

    private static void wireAndRegister(List<QuestRecord> records) {
        for (QuestRecord rec : records) {
            QuestNode node = new QuestNode(rec.id(),
                    Component.literal(rec.title()), Component.literal(rec.description()));
            node.setCategory(rec.category());
            node.setShapeType(rec.shape());
            node.setCustomX(rec.posX());
            node.setCustomY(rec.posY());
            node.setSubtitle(rec.subtitle());
            node.setVisibility(rec.visibility());
            node.setEnableIf(rec.enableIf());
            node.setTaskMinCount(rec.taskMinCount());
            node.setHideDepLine(rec.hideDepLine());
            node.setDisabledBlocksChildren(rec.disabledBlocksChildren());
            node.setShared(rec.shared());
            if (!rec.iconItemId().isEmpty()) node.setIconItemById(rec.iconItemId());
            node.setRepeatMode(rec.repeatMode());
            node.setRepeatCooldownHours(rec.repeatCooldownHours());
            node.setRequireAllPrerequisites(rec.requireAllPrereqs());
            node.setOptionalPrereqMinCount(rec.optionalPrereqMinCount());
            for (QuestReward r : rec.rewards()) node.addReward(r);
            for (QuestTask t : rec.tasks()) node.addTask(t);
            if (rec.emergencyItems() != null) node.deserializeEmergencyItems(rec.emergencyItems());
            for (TutorialStep step : rec.tutorialSteps()) node.addTutorialStep(step);
            QuestTreeRegistry.registerBareQuestNode(node);
        }

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

            for (String pid : rec.prereqRequired().keySet()) {
                if (QuestTreeRegistry.getQuest(new ResourceLocation("phoenixcore", pid)) == null)
                    LOAD_ERRORS.add("Quest '" + rec.id().getPath() + "': prereq '" + pid + "' not found.");
            }
            for (String pid : rec.prereqForbidden()) {
                if (QuestTreeRegistry.getQuest(new ResourceLocation("phoenixcore", pid)) == null)
                    LOAD_ERRORS.add("Quest '" + rec.id().getPath() + "': forbidden prereq '" + pid + "' not found.");
            }

            for (Map.Entry<String, Boolean> e : rec.prereqRequired().entrySet()) {
                QuestNode prereq = QuestTreeRegistry.getQuest(new ResourceLocation("phoenixcore", e.getKey()));
                if (prereq != null) {
                    node.addPrerequisite(prereq);
                    node.setPrereqRequired(prereq.getId(), e.getValue());
                    if (rec.prereqLink().contains(e.getKey())) node.setPrereqLink(prereq.getId(), true);
                }
            }
            for (String pid : rec.prereqForbidden()) {
                QuestNode prereq = QuestTreeRegistry.getQuest(new ResourceLocation("phoenixcore", pid));
                if (prereq != null) {
                    node.addPrerequisite(prereq);
                    node.setPrereqForbidden(prereq.getId(), true);
                    if (rec.prereqLink().contains(pid)) node.setPrereqLink(prereq.getId(), true);
                }
            }
        }

        Map<ResourceLocation, String> taskIdToQuest = new LinkedHashMap<>();
        for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
            for (QuestTask task : node.getTasks()) {
                ResourceLocation tid = task.getTaskId();
                if (taskIdToQuest.containsKey(tid)) {
                    LOAD_ERRORS.add("Duplicate task_id '" + tid + "' in quest '" + node.getId().getPath() +
                            "' (also in '" + taskIdToQuest.get(tid) + "').");
                } else {
                    taskIdToQuest.put(tid, node.getId().getPath());
                }
            }
        }
    }

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

            QuestNode.RepeatMode repeatMode = QuestNode.RepeatMode.NONE;
            if (tag.contains("repeat_mode")) {
                try {
                    repeatMode = QuestNode.RepeatMode.valueOf(tag.getString("repeat_mode").toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            int repeatCooldownHours = tag.contains("repeat_cooldown_hours") ? tag.getInt("repeat_cooldown_hours") : 24;

            boolean requireAllPrereqs = !tag.contains("require_all_prereqs") || tag.getBoolean("require_all_prereqs");

            String subtitle = tag.contains("subtitle") ? tag.getString("subtitle") : "";
            QuestNode.Visibility visibility = QuestNode.Visibility.NORMAL;
            if (tag.contains("visibility")) {
                try {
                    visibility = QuestNode.Visibility.valueOf(tag.getString("visibility").toUpperCase());
                } catch (Exception ignored) {}
            }
            int taskMinCount = tag.contains("task_min_count") ? tag.getInt("task_min_count") : 0;

            List<QuestReward> rewards = new ArrayList<>();
            if (tag.contains("rewards")) {
                ListTag rewardList = tag.getList("rewards", Tag.TAG_COMPOUND);
                for (int ri = 0; ri < rewardList.size(); ri++) {
                    QuestReward r = QuestReward.deserializeNBT(rewardList.getCompound(ri));
                    if (r != null) rewards.add(r);
                }
            }

            List<QuestTask> tasks = new ArrayList<>();
            if (tag.contains("tasks")) {
                ListTag taskList = tag.getList("tasks", Tag.TAG_COMPOUND);
                for (int ti = 0; ti < taskList.size(); ti++) {
                    QuestTask t = deserializeTask(taskList.getCompound(ti));
                    if (t != null) tasks.add(t);
                }
            }

            net.minecraft.nbt.ListTag emergencyTag = tag.contains("emergency_items") ?
                    tag.getList("emergency_items", Tag.TAG_COMPOUND) : null;

            Map<String, Boolean> prereqRequired = new LinkedHashMap<>();
            Set<String> prereqForbidden = new java.util.LinkedHashSet<>();
            Set<String> prereqLink = new java.util.LinkedHashSet<>();
            if (tag.contains("prerequisites")) {
                ListTag pList = tag.getList("prerequisites", Tag.TAG_COMPOUND);
                for (int pi = 0; pi < pList.size(); pi++) {
                    CompoundTag pTag = pList.getCompound(pi);
                    String pid = pTag.contains("id") ? pTag.getString("id") : "";
                    if (pid.isEmpty()) continue;
                    if (pTag.contains("forbidden") && pTag.getBoolean("forbidden")) {
                        prereqForbidden.add(pid);
                    } else {
                        boolean req = !pTag.contains("required") || pTag.getBoolean("required");
                        prereqRequired.put(pid, req);
                    }
                    if (pTag.contains("link") && pTag.getBoolean("link")) prereqLink.add(pid);
                }
            }
            int optionalPrereqMinCount = tag.contains("optional_prereq_min_count") ?
                    tag.getInt("optional_prereq_min_count") : 0;

            String enableIf = tag.contains("enable_if") ? tag.getString("enable_if") : null;
            boolean hideDepLine = tag.contains("hide_dep_line") && tag.getBoolean("hide_dep_line");
            boolean disabledBlocksChildren = tag.contains("disabled_blocks_children") &&
                    tag.getBoolean("disabled_blocks_children");
            boolean shared = tag.contains("shared") && tag.getBoolean("shared");

            List<TutorialStep> tutorialSteps = new ArrayList<>();
            if (tag.contains("tutorial_steps")) {
                ListTag stepList = tag.getList("tutorial_steps", Tag.TAG_COMPOUND);
                for (int si = 0; si < stepList.size(); si++) {
                    CompoundTag st = stepList.getCompound(si);
                    String stepText = st.contains("text") ? st.getString("text") : "";
                    String highlight = st.contains("highlight") ? st.getString("highlight") : TutorialStep.HL_NONE;
                    if (!stepText.isBlank()) tutorialSteps.add(new TutorialStep(stepText, highlight));
                }
            }

            return new QuestRecord(id, title, desc, subtitle, category.toUpperCase(), shape.toUpperCase(),
                    iconItem, posX, posY, visibility, taskMinCount, parentId,
                    repeatMode, repeatCooldownHours, requireAllPrereqs, rewards, tasks, emergencyTag,
                    prereqRequired, optionalPrereqMinCount, enableIf, prereqForbidden, prereqLink, hideDepLine,
                    disabledBlocksChildren, shared, tutorialSteps);

        } catch (Exception e) {
            String msg = "Failed to parse '" + file.getFileName() + "': " + e.getMessage();
            LOAD_ERRORS.add(msg);
            System.err.println("[Phoenix Chronicles] " + msg);
            return null;
        }
    }

    private static QuestTask deserializeTask(CompoundTag tag) {
        if (!tag.contains("type") || !tag.contains("task_id")) return null;
        boolean optional = tag.contains("optional") && tag.getBoolean("optional");

        QuestTask task = PhoenixTaskRegistry.deserialize(tag);
        if (task != null) {
            task.setOptional(optional);
        }
        return task;
    }
}
