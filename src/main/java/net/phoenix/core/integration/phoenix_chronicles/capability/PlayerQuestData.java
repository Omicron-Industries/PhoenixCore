package net.phoenix.core.integration.phoenix_chronicles.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerQuestData {

    // Quest state per quest id
    private final Map<ResourceLocation, QuestState> questStates = new HashMap<>();
    // Task-specific progress blobs
    private final Map<ResourceLocation, CompoundTag> taskProgress = new HashMap<>();
    // Last-completed epoch-millis per quest (for cooldown / daily repeat logic)
    private final Map<ResourceLocation, Long> lastCompleted = new HashMap<>();
    // Rewards the player has already claimed (prevents double-granting on reconnect)
    private final Set<ResourceLocation> claimedRewards = new HashSet<>();
    // Which reward index was chosen in a choice group, keyed by quest id
    private final Map<ResourceLocation, Integer> chosenRewardIndex = new HashMap<>();
    // The single pinned quest shown on the HUD (null = nothing pinned)
    private ResourceLocation pinnedQuestId = null;

    // ── Quest state ───────────────────────────────────────────────────────────

    public QuestState getQuestState(ResourceLocation questId, QuestState defaultState) {
        return questStates.getOrDefault(questId, defaultState);
    }

    public void setQuestState(ResourceLocation questId, QuestState state) {
        questStates.put(questId, state);
    }

    // ── Task progress ─────────────────────────────────────────────────────────

    public CompoundTag getOrCreateTaskProgress(ResourceLocation taskId) {
        return taskProgress.computeIfAbsent(taskId, id -> new CompoundTag());
    }

    // ── Repeat / cooldown ─────────────────────────────────────────────────────

    public long getLastCompletedTime(ResourceLocation questId) {
        return lastCompleted.getOrDefault(questId, 0L);
    }

    public void recordCompletion(ResourceLocation questId) {
        lastCompleted.put(questId, System.currentTimeMillis());
    }

    // ── Rewards ───────────────────────────────────────────────────────────────

    public boolean hasClaimedRewards(ResourceLocation questId) {
        return claimedRewards.contains(questId);
    }

    public void markRewardsClaimed(ResourceLocation questId) {
        claimedRewards.add(questId);
    }

    public void clearClaimedRewards(ResourceLocation questId) {
        claimedRewards.remove(questId);
    }

    public int getChosenRewardIndex(ResourceLocation questId) {
        return chosenRewardIndex.getOrDefault(questId, -1);
    }

    public void setChosenRewardIndex(ResourceLocation questId, int index) {
        chosenRewardIndex.put(questId, index);
    }

    public void clearChosenRewardIndex(ResourceLocation questId) {
        chosenRewardIndex.remove(questId);
    }

    /** Wipes all accumulated task progress for a single task (used on repeat reset). */
    public void clearTaskProgress(ResourceLocation taskId) {
        taskProgress.remove(taskId);
    }

    // ── Pinned quest ──────────────────────────────────────────────────────────

    public ResourceLocation getPinnedQuestId() {
        return pinnedQuestId;
    }

    public void setPinnedQuestId(ResourceLocation id) {
        this.pinnedQuestId = id;
    }

    public void clearPin() {
        this.pinnedQuestId = null;
    }

    public boolean isPinned(ResourceLocation questId) {
        return pinnedQuestId != null && pinnedQuestId.equals(questId);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();

        // Quest states
        ListTag questsList = new ListTag();
        questStates.forEach((id, state) -> {
            CompoundTag e = new CompoundTag();
            e.putString("id", id.toString());
            e.putString("state", state.name());
            questsList.add(e);
        });
        root.put("Quests", questsList);

        // Task progress
        ListTag tasksList = new ListTag();
        taskProgress.forEach((id, tag) -> {
            CompoundTag e = new CompoundTag();
            e.putString("id", id.toString());
            e.put("progress", tag);
            tasksList.add(e);
        });
        root.put("Tasks", tasksList);

        // Last completed timestamps
        ListTag completedList = new ListTag();
        lastCompleted.forEach((id, time) -> {
            CompoundTag e = new CompoundTag();
            e.putString("id", id.toString());
            e.putLong("time", time);
            completedList.add(e);
        });
        root.put("LastCompleted", completedList);

        // Claimed rewards
        ListTag claimedList = new ListTag();
        for (ResourceLocation id : claimedRewards) {
            CompoundTag e = new CompoundTag();
            e.putString("id", id.toString());
            claimedList.add(e);
        }
        root.put("ClaimedRewards", claimedList);

        // Chosen reward indices
        ListTag chosenList = new ListTag();
        chosenRewardIndex.forEach((id, idx) -> {
            CompoundTag e = new CompoundTag();
            e.putString("id", id.toString());
            e.putInt("index", idx);
            chosenList.add(e);
        });
        root.put("ChosenRewards", chosenList);

        // Pinned quest
        if (pinnedQuestId != null) root.putString("PinnedQuest", pinnedQuestId.toString());

        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        questStates.clear();
        taskProgress.clear();
        lastCompleted.clear();
        claimedRewards.clear();
        chosenRewardIndex.clear();
        pinnedQuestId = null;

        for (int i = 0; i < root.getList("Quests", Tag.TAG_COMPOUND).size(); i++) {
            CompoundTag e = root.getList("Quests", Tag.TAG_COMPOUND).getCompound(i);
            try {
                questStates.put(new ResourceLocation(e.getString("id")),
                        QuestState.valueOf(e.getString("state")));
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < root.getList("Tasks", Tag.TAG_COMPOUND).size(); i++) {
            CompoundTag e = root.getList("Tasks", Tag.TAG_COMPOUND).getCompound(i);
            taskProgress.put(new ResourceLocation(e.getString("id")), e.getCompound("progress"));
        }

        for (int i = 0; i < root.getList("LastCompleted", Tag.TAG_COMPOUND).size(); i++) {
            CompoundTag e = root.getList("LastCompleted", Tag.TAG_COMPOUND).getCompound(i);
            lastCompleted.put(new ResourceLocation(e.getString("id")), e.getLong("time"));
        }

        for (int i = 0; i < root.getList("ClaimedRewards", Tag.TAG_COMPOUND).size(); i++) {
            CompoundTag e = root.getList("ClaimedRewards", Tag.TAG_COMPOUND).getCompound(i);
            claimedRewards.add(new ResourceLocation(e.getString("id")));
        }

        for (int i = 0; i < root.getList("ChosenRewards", Tag.TAG_COMPOUND).size(); i++) {
            CompoundTag e = root.getList("ChosenRewards", Tag.TAG_COMPOUND).getCompound(i);
            chosenRewardIndex.put(new ResourceLocation(e.getString("id")), e.getInt("index"));
        }

        if (root.contains("PinnedQuest")) {
            try {
                pinnedQuestId = new ResourceLocation(root.getString("PinnedQuest"));
            } catch (Exception ignored) {}
        }
    }
}
