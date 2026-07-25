package net.phoenix.core.integration.phoenix_chronicles.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class S2CSyncQuestsPacket {

    private final Map<ResourceLocation, QuestSnapshot> snapshotMap;

    public S2CSyncQuestsPacket(Map<ResourceLocation, QuestNode> serverRegistry) {
        this.snapshotMap = new HashMap<>();
        for (Map.Entry<ResourceLocation, QuestNode> entry : serverRegistry.entrySet()) {
            snapshotMap.put(entry.getKey(), new QuestSnapshot(entry.getValue()));
        }
    }

    public S2CSyncQuestsPacket(FriendlyByteBuf buf) {
        this.snapshotMap = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Component title = buf.readComponent();
            Component description = buf.readComponent();
            String category = buf.readUtf();
            String shapeType = buf.readUtf();
            String iconItemId = buf.readUtf();
            int customX = buf.readInt();
            int customY = buf.readInt();

            String subtitle = buf.readUtf();
            String visibility = buf.readUtf();
            String enableIf = buf.readUtf();
            int taskMinCount = buf.readInt();
            boolean requireAllPrerequisites = buf.readBoolean();

            int childCount = buf.readInt();
            List<ResourceLocation> childIds = new ArrayList<>(childCount);
            for (int c = 0; c < childCount; c++) childIds.add(buf.readResourceLocation());

            int prereqCount = buf.readInt();
            List<ResourceLocation> prereqIds = new ArrayList<>(prereqCount);
            List<Boolean> prereqRequired = new ArrayList<>(prereqCount);
            List<Boolean> prereqForbidden = new ArrayList<>(prereqCount);
            List<Boolean> prereqLink = new ArrayList<>(prereqCount);
            for (int p = 0; p < prereqCount; p++) {
                prereqIds.add(buf.readResourceLocation());
                prereqRequired.add(buf.readBoolean());
                prereqForbidden.add(buf.readBoolean());
                prereqLink.add(buf.readBoolean());
            }
            int optionalPrereqMinCount = buf.readInt();

            int taskCount = buf.readInt();
            List<CompoundTag> tasksNbt = new ArrayList<>(taskCount);
            for (int t = 0; t < taskCount; t++) tasksNbt.add(buf.readNbt());

            snapshotMap.put(id, new QuestSnapshot(
                    id, title, description, category, shapeType, iconItemId,
                    customX, customY, subtitle, visibility, enableIf, taskMinCount, requireAllPrerequisites,
                    childIds, prereqIds, prereqRequired, prereqForbidden, prereqLink,
                    optionalPrereqMinCount, tasksNbt));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(snapshotMap.size());
        for (QuestSnapshot snap : snapshotMap.values()) {
            buf.writeResourceLocation(snap.id);
            buf.writeComponent(snap.title);
            buf.writeComponent(snap.description);
            buf.writeUtf(snap.category);
            buf.writeUtf(snap.shapeType);
            buf.writeUtf(snap.iconItemId);
            buf.writeInt(snap.customX);
            buf.writeInt(snap.customY);
            buf.writeUtf(snap.subtitle);
            buf.writeUtf(snap.visibility);
            buf.writeUtf(snap.enableIf);
            buf.writeInt(snap.taskMinCount);
            buf.writeBoolean(snap.requireAllPrerequisites);

            buf.writeInt(snap.childIds.size());
            for (ResourceLocation cId : snap.childIds) buf.writeResourceLocation(cId);

            buf.writeInt(snap.prereqIds.size());
            for (int pi = 0; pi < snap.prereqIds.size(); pi++) {
                buf.writeResourceLocation(snap.prereqIds.get(pi));
                buf.writeBoolean(pi < snap.prereqRequired.size() && snap.prereqRequired.get(pi));
                buf.writeBoolean(pi < snap.prereqForbidden.size() && snap.prereqForbidden.get(pi));
                buf.writeBoolean(pi < snap.prereqLink.size() && snap.prereqLink.get(pi));
            }
            buf.writeInt(snap.optionalPrereqMinCount);

            buf.writeInt(snap.tasksNbt.size());
            for (CompoundTag tag : snap.tasksNbt) buf.writeNbt(tag);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPayloadProcessor.processQuestTree(snapshotMap)));
        ctx.get().setPacketHandled(true);
    }

    private static class QuestSnapshot {

        final ResourceLocation id;
        final Component title;
        final Component description;
        final String category;
        final String shapeType;
        final String iconItemId;
        final int customX;
        final int customY;

        final String subtitle;
        final String visibility;
        final String enableIf;
        final int taskMinCount;
        final boolean requireAllPrerequisites;
        final List<ResourceLocation> childIds;
        final List<ResourceLocation> prereqIds;

        final List<Boolean> prereqRequired;

        final List<Boolean> prereqForbidden;

        final List<Boolean> prereqLink;
        final int optionalPrereqMinCount;
        final List<CompoundTag> tasksNbt;

        QuestSnapshot(QuestNode node) {
            this.id = node.getId();
            this.title = node.getTitle();
            this.description = node.getDescription();
            this.category = node.getCategory() != null ? node.getCategory() : "MAIN";
            this.shapeType = node.getShapeType() != null ? node.getShapeType() : "SQUARE";
            this.iconItemId = node.getIconItemId();
            this.customX = node.getCustomX();
            this.customY = node.getCustomY();
            this.subtitle = node.getSubtitle() != null ? node.getSubtitle() : "";
            this.visibility = node.getVisibility().name();
            this.enableIf = node.getEnableIf() != null ? node.getEnableIf() : "";
            this.taskMinCount = node.getTaskMinCount();
            this.requireAllPrerequisites = node.getRequireAllPrerequisites();
            this.optionalPrereqMinCount = node.getOptionalPrereqMinCount();

            this.childIds = new ArrayList<>();
            for (QuestNode child : node.getChildren()) childIds.add(child.getId());

            this.prereqIds = new ArrayList<>();
            this.prereqRequired = new ArrayList<>();
            this.prereqForbidden = new ArrayList<>();
            this.prereqLink = new ArrayList<>();
            for (QuestNode req : node.getPrerequisites()) {
                prereqIds.add(req.getId());
                prereqRequired.add(node.isPrereqRequired(req.getId()));
                prereqForbidden.add(node.isPrereqForbidden(req.getId()));
                prereqLink.add(node.isPrereqLink(req.getId()));
            }

            this.tasksNbt = new ArrayList<>();
            for (QuestTask task : node.getTasks()) {
                CompoundTag tag = task.serializeNBT();
                if (!tag.contains("task_id"))
                    tag.putString("task_id", task.getTaskId().toString());
                if (!tag.contains("description"))
                    tag.putString("description", Component.Serializer.toJson(task.getDescription()));
                tag.putBoolean("optional", task.isOptional());
                tasksNbt.add(tag);
            }
        }

        QuestSnapshot(ResourceLocation id, Component title, Component description,
                      String category, String shapeType, String iconItemId,
                      int customX, int customY,
                      String subtitle, String visibility, String enableIf, int taskMinCount,
                      boolean requireAllPrerequisites,
                      List<ResourceLocation> childIds, List<ResourceLocation> prereqIds,
                      List<Boolean> prereqRequired, List<Boolean> prereqForbidden, List<Boolean> prereqLink,
                      int optionalPrereqMinCount,
                      List<CompoundTag> tasksNbt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.shapeType = shapeType;
            this.iconItemId = iconItemId;
            this.customX = customX;
            this.customY = customY;
            this.subtitle = subtitle;
            this.visibility = visibility;
            this.enableIf = enableIf;
            this.taskMinCount = taskMinCount;
            this.requireAllPrerequisites = requireAllPrerequisites;
            this.childIds = childIds;
            this.prereqIds = prereqIds;
            this.prereqRequired = prereqRequired;
            this.prereqForbidden = prereqForbidden;
            this.prereqLink = prereqLink;
            this.optionalPrereqMinCount = optionalPrereqMinCount;
            this.tasksNbt = tasksNbt;
        }
    }

    private static class ClientPayloadProcessor {

        static void processQuestTree(Map<ResourceLocation, QuestSnapshot> snapshots) {
            QuestTreeRegistry.clear();

            for (QuestSnapshot snap : snapshots.values()) {
                QuestNode node = new QuestNode(snap.id, snap.title, snap.description);
                node.setCategory(snap.category);
                node.setShapeType(snap.shapeType);
                node.setCustomX(snap.customX);
                node.setCustomY(snap.customY);
                node.setSubtitle(snap.subtitle);
                node.setTaskMinCount(snap.taskMinCount);
                node.setRequireAllPrerequisites(snap.requireAllPrerequisites);
                node.setOptionalPrereqMinCount(snap.optionalPrereqMinCount);
                try {
                    node.setVisibility(QuestNode.Visibility.valueOf(snap.visibility));
                } catch (Exception ignored) {}
                node.setEnableIf(snap.enableIf.isEmpty() ? null : snap.enableIf);

                if (!snap.iconItemId.isEmpty()) {
                    node.setIconItemById(snap.iconItemId);
                }

                for (CompoundTag tag : snap.tasksNbt) {
                    QuestTask task = deserializeTask(tag);
                    if (task != null) {
                        if (tag.contains("optional")) task.setOptional(tag.getBoolean("optional"));
                        node.addTask(task);
                    }
                }

                QuestTreeRegistry.registerBareQuestNode(node);
            }

            Set<ResourceLocation> hasParent = new HashSet<>();
            for (QuestSnapshot snap : snapshots.values())
                hasParent.addAll(snap.childIds);

            for (QuestSnapshot snap : snapshots.values()) {
                QuestNode node = QuestTreeRegistry.getQuest(snap.id);
                if (node == null) continue;

                for (ResourceLocation childId : snap.childIds) {
                    QuestNode child = QuestTreeRegistry.getQuest(childId);
                    if (child != null) node.addChild(child);
                }

                for (int pi = 0; pi < snap.prereqIds.size(); pi++) {
                    QuestNode req = QuestTreeRegistry.getQuest(snap.prereqIds.get(pi));
                    if (req != null) {
                        node.addPrerequisite(req);
                        boolean forbidden = pi < snap.prereqForbidden.size() && snap.prereqForbidden.get(pi);
                        if (forbidden) {
                            node.setPrereqForbidden(req.getId(), true);
                        } else {
                            boolean required = pi < snap.prereqRequired.size() && snap.prereqRequired.get(pi);
                            node.setPrereqRequired(req.getId(), required);
                        }
                        if (pi < snap.prereqLink.size() && snap.prereqLink.get(pi)) {
                            node.setPrereqLink(req.getId(), true);
                        }
                    }
                }

                if (!hasParent.contains(snap.id)) {
                    QuestTreeRegistry.registerRootChapter(node);
                }
            }

            System.out.println("[Phoenix Chronicles] Client synced " + snapshots.size() + " quest(s) from server.");
        }

        private static QuestTask deserializeTask(CompoundTag tag) {
            if (!tag.contains("type") || !tag.contains("task_id")) return null;
            QuestTask task = net.phoenix.core.integration.phoenix_chronicles.PhoenixTaskRegistry.deserialize(tag);
            if (task == null) {
                System.err.println("[Phoenix Chronicles] Unknown task type in sync packet: '" +
                        tag.getString("type") + "' — skipping.");
            }
            return task;
        }
    }
}
