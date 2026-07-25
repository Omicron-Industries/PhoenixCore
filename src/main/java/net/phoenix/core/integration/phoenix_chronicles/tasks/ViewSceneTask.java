package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class ViewSceneTask extends QuestTask {

    private String sceneId;
    private float minSeconds;

    public ViewSceneTask(ResourceLocation taskId, Component description, String sceneId, float minSeconds) {
        super(taskId, description);
        this.sceneId = sceneId;
        this.minSeconds = minSeconds;
    }

    public String getSceneId() {
        return sceneId;
    }

    public float getMinSeconds() {
        return minSeconds;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(getTaskId()).getBoolean("completed"))
                .orElse(false);
    }

    public void markCompletedClient(Player player) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .ifPresent(data -> data.getOrCreateTaskProgress(getTaskId()).putBoolean("completed", true));
    }

    @Override
    public String getProgressString(Player player) {
        return isCompletedFor(player) ? "Viewed" : (minSeconds > 0 ? "View for " + (int) minSeconds + "s" : "View");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "view_scene");
        tag.putString("scene_id", sceneId != null ? sceneId : "");
        tag.putFloat("min_seconds", minSeconds);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.sceneId = nbt.getString("scene_id");
        this.minSeconds = nbt.contains("min_seconds") ? nbt.getFloat("min_seconds") : 5.0f;
    }
}
