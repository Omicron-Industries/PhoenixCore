package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

/**
 * A cosmetic/instructional task — displays a text tip or image URL.
 * The player manually acknowledges it by clicking "Mark as read" in the task screen.
 * Useful for tutorial steps, lore blurbs, and warnings in quest chains.
 *
 * SNBT shape: { type: "info", body: "Some tip text here." }
 */
public class InfoTask extends QuestTask {

    private String body;

    public InfoTask(ResourceLocation taskId, Component description, String body) {
        super(taskId, description);
        this.body = body != null ? body : "";
    }

    public String getBody() {
        return body;
    }

    /** Call from the task screen's "Mark as read" button. */
    public static void acknowledge(Player player, ResourceLocation taskId) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .ifPresent(data -> data.getOrCreateTaskProgress(taskId).putBoolean("acknowledged", true));
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(d -> d.getOrCreateTaskProgress(getTaskId()).getBoolean("acknowledged"))
                .orElse(false);
    }

    @Override
    public String getProgressString(Player player) {
        return isCompletedFor(player) ? "Read" : "Unread";
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "info");
        tag.putString("body", body);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("body")) body = nbt.getString("body");
    }
}
