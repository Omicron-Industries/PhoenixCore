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
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Server-to-Client packet that safely transfers server-verified quest configurations
 * and hierarchy maps to the player's client-side registry instance.
 *
 * Wire format per quest node (in order):
 * ResourceLocation id
 * Component title
 * Component description
 * String category
 * String shapeType
 * String iconItemId ("" = no icon)
 * int customX
 * int customY
 * int childCount
 * ResourceLocation childIds[childCount]
 * int prereqCount
 * ResourceLocation prereqIds[prereqCount]
 * int taskCount
 * CompoundTag tasksNbt[taskCount]
 * Each CompoundTag contains:
 * "task_id" : String (ResourceLocation.toString())
 * "description" : String (Component JSON)
 * + all type-specific fields written by QuestTask.serializeNBT()
 *
 * Root detection in Phase 2:
 * A node is a root iff its id does not appear in any snapshot's childIds list.
 * registerBareQuestNode only touches ALL_QUESTS; we call registerRootChapter()
 * for true roots so getRootChapters() is populated and the canvas renders.
 */
public class S2CSyncQuestsPacket {

    private final Map<ResourceLocation, QuestSnapshot> snapshotMap;

    // ── Server-side constructor ───────────────────────────────────────────────

    public S2CSyncQuestsPacket(Map<ResourceLocation, QuestNode> serverRegistry) {
        this.snapshotMap = new HashMap<>();
        for (Map.Entry<ResourceLocation, QuestNode> entry : serverRegistry.entrySet()) {
            snapshotMap.put(entry.getKey(), new QuestSnapshot(entry.getValue()));
        }
    }

    // ── Decode constructor ────────────────────────────────────────────────────

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

            int childCount = buf.readInt();
            List<ResourceLocation> childIds = new ArrayList<>(childCount);
            for (int c = 0; c < childCount; c++) childIds.add(buf.readResourceLocation());

            int prereqCount = buf.readInt();
            List<ResourceLocation> prereqIds = new ArrayList<>(prereqCount);
            for (int p = 0; p < prereqCount; p++) prereqIds.add(buf.readResourceLocation());

            int taskCount = buf.readInt();
            List<CompoundTag> tasksNbt = new ArrayList<>(taskCount);
            for (int t = 0; t < taskCount; t++) tasksNbt.add(buf.readNbt());

            snapshotMap.put(id, new QuestSnapshot(
                    id, title, description, category, shapeType, iconItemId,
                    customX, customY, childIds, prereqIds, tasksNbt));
        }
    }

    // ── Encode ────────────────────────────────────────────────────────────────

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

            buf.writeInt(snap.childIds.size());
            for (ResourceLocation cId : snap.childIds) buf.writeResourceLocation(cId);

            buf.writeInt(snap.prereqIds.size());
            for (ResourceLocation pId : snap.prereqIds) buf.writeResourceLocation(pId);

            buf.writeInt(snap.tasksNbt.size());
            for (CompoundTag tag : snap.tasksNbt) buf.writeNbt(tag);
        }
    }

    // ── Handle ────────────────────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPayloadProcessor.processQuestTree(snapshotMap)));
        ctx.get().setPacketHandled(true);
    }

    // ── Snapshot ──────────────────────────────────────────────────────────────

    private static class QuestSnapshot {

        final ResourceLocation id;
        final Component title;
        final Component description;
        final String category;
        final String shapeType;
        final String iconItemId;
        final int customX;
        final int customY;
        final List<ResourceLocation> childIds;
        final List<ResourceLocation> prereqIds;
        final List<CompoundTag> tasksNbt;

        /** Server-side: capture everything from a live QuestNode. */
        QuestSnapshot(QuestNode node) {
            this.id = node.getId();
            this.title = node.getTitle();
            this.description = node.getDescription();
            this.category = node.getCategory() != null ? node.getCategory() : "MAIN";
            this.shapeType = node.getShapeType() != null ? node.getShapeType() : "SQUARE";
            this.iconItemId = node.getIconItemId(); // "" if no icon
            this.customX = node.getCustomX();
            this.customY = node.getCustomY();

            this.childIds = new ArrayList<>();
            for (QuestNode child : node.getChildren())
                childIds.add(child.getId());

            this.prereqIds = new ArrayList<>();
            for (QuestNode req : node.getPrerequisites())
                prereqIds.add(req.getId());

            // Build each task tag from serializeNBT(), then inject identity keys
            // (task_id + description) so the client can reconstruct the instance
            // without a separate factory registry.
            this.tasksNbt = new ArrayList<>();
            for (QuestTask task : node.getTasks()) {
                CompoundTag tag = task.serializeNBT();
                if (!tag.contains("task_id"))
                    tag.putString("task_id", task.getTaskId().toString());
                if (!tag.contains("description"))
                    tag.putString("description", Component.Serializer.toJson(task.getDescription()));
                tasksNbt.add(tag);
            }
        }

        /** Client-side: populated from decoded wire data. */
        QuestSnapshot(ResourceLocation id, Component title, Component description,
                      String category, String shapeType, String iconItemId,
                      int customX, int customY,
                      List<ResourceLocation> childIds, List<ResourceLocation> prereqIds,
                      List<CompoundTag> tasksNbt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.shapeType = shapeType;
            this.iconItemId = iconItemId;
            this.customX = customX;
            this.customY = customY;
            this.childIds = childIds;
            this.prereqIds = prereqIds;
            this.tasksNbt = tasksNbt;
        }
    }

    // ── Client-side processor ─────────────────────────────────────────────────

    /**
     * Isolated in a static inner class so the server JVM never loads client-only
     * class references when scanning the outer packet class's method signatures.
     */
    private static class ClientPayloadProcessor {

        static void processQuestTree(Map<ResourceLocation, QuestSnapshot> snapshots) {
            QuestTreeRegistry.clear();

            // ── Phase 1: Reconstruct every node and register it barefoot ─────
            for (QuestSnapshot snap : snapshots.values()) {
                QuestNode node = new QuestNode(snap.id, snap.title, snap.description);
                node.setCategory(snap.category);
                node.setShapeType(snap.shapeType);
                node.setCustomX(snap.customX);
                node.setCustomY(snap.customY);

                if (!snap.iconItemId.isEmpty()) {
                    node.setIconItemById(snap.iconItemId);
                }

                for (CompoundTag tag : snap.tasksNbt) {
                    QuestTask task = deserializeTask(tag);
                    if (task != null) node.addTask(task);
                }

                // Bare registration — only ALL_QUESTS for now; ROOT_NODES populated in Phase 2
                QuestTreeRegistry.registerBareQuestNode(node);
            }

            // ── Phase 2: Wire parent→child, prerequisites, and roots ─────────
            // Any node whose id appears in another snapshot's childIds is not a root
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

                for (ResourceLocation reqId : snap.prereqIds) {
                    QuestNode req = QuestTreeRegistry.getQuest(reqId);
                    if (req != null) node.addPrerequisite(req);
                }

                // Register as root so the canvas can find it via getRootChapters()
                if (!hasParent.contains(snap.id)) {
                    QuestTreeRegistry.registerRootChapter(node);
                }
            }

            System.out.println("[Phoenix Chronicles] Client synced " + snapshots.size() + " quest(s) from server.");
        }

        /**
         * Reconstructs a QuestTask from its full serialized CompoundTag.
         *
         * We create a minimal stub instance (with placeholder field values) and
         * then call deserializeNBT() to overwrite them with the real data from
         * the tag. This avoids a generic factory interface and keeps each task
         * class's own deserializeNBT as the single source of truth.
         */
        private static QuestTask deserializeTask(CompoundTag tag) {
            if (!tag.contains("type") || !tag.contains("task_id")) return null;

            ResourceLocation taskId;
            try {
                taskId = new ResourceLocation(tag.getString("task_id"));
            } catch (Exception e) {
                return null;
            }

            Component desc;
            try {
                desc = Component.Serializer.fromJson(tag.getString("description"));
                if (desc == null) throw new IllegalArgumentException("null component");
            } catch (Exception e) {
                desc = Component.literal(tag.getString("type"));
            }

            // Stubs use the cheapest valid placeholder; deserializeNBT overwrites everything
            QuestTask task = switch (tag.getString("type")) {
                case "kill_entity" -> new KillEntityTask(taskId, desc,
                        new ResourceLocation("minecraft", "pig"), 1, false);
                case "item_check" -> new ItemRequirementTask(taskId, desc,
                        net.minecraft.world.item.Items.DIRT, 1, false);
                case "craft_item" -> new CraftItemTask(taskId, desc,
                        new ResourceLocation("minecraft", "dirt"), 1);
                case "experience" -> new ExperienceTask(taskId, desc, 1);
                case "location_terminal" -> new LocationOrTerminalTask(taskId, desc,
                        new ResourceLocation("minecraft", "air"), false);
                case "advancement" -> new AdvancementTask(taskId, desc,
                        new ResourceLocation("minecraft", "story/root"));
                case "block_interact" -> new BlockInteractTask(taskId, desc,
                        net.minecraft.world.level.block.Blocks.STONE, "PLACE");
                case "fluid_check" -> new FluidRequirementTask(taskId, desc,
                        new ResourceLocation("minecraft", "water"), 1000, false);
                case "stat" -> new StatTrackerTask(taskId, desc,
                        new ResourceLocation("minecraft", "jump"), 1, false);
                case "dimension" -> new DimensionTask(taskId, desc,
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                new ResourceLocation("minecraft", "overworld")));
                default -> {
                    System.err.println("[Phoenix Chronicles] Unknown task type in sync packet: '" +
                            tag.getString("type") + "' — skipping.");
                    yield null;
                }
            };

            if (task != null) {
                try {
                    task.deserializeNBT(tag);
                } catch (Exception e) {
                    System.err.println("[Phoenix Chronicles] deserializeNBT failed for task '" + taskId + "' (type=" +
                            tag.getString("type") + "): " + e.getMessage());
                }
            }

            return task;
        }
    }
}
