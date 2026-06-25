package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

/**
 * Manual checkmark task — completed by an operator via a server command
 * or by another system calling {@link #complete(Player)}.
 *
 * Useful for "talk to the admin", "attend the event", or "read the lore book" objectives.
 * SNBT shape: { type: "checkmark" }
 *
 * Command to complete: /chronicle task complete <player> <taskId>
 * (wired up in ChronicleCommands)
 */
public class CheckmarkTask extends QuestTask {

    public CheckmarkTask(ResourceLocation taskId, Component description) {
        super(taskId, description);
    }

    /** Called by the server command. */
    public static void complete(Player player, ResourceLocation taskId) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .ifPresent(data -> data.getOrCreateTaskProgress(taskId).putBoolean("completed", true));
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(getTaskId()).getBoolean("completed"))
                .orElse(false);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "checkmark");
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // no fields to restore
    }
}
