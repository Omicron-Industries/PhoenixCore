package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class LocationOrTerminalTask extends QuestTask {

    private ResourceLocation targetTerminalId;
    private boolean consume; 

    public LocationOrTerminalTask(ResourceLocation taskId, Component description, ResourceLocation targetTerminalId,
                                  boolean consume) {
        super(taskId, description);
        this.targetTerminalId = targetTerminalId;
        this.consume = consume;
    }

    public ResourceLocation getTargetTerminalId() {
        return targetTerminalId;
    }

    public boolean shouldConsume() {
        return consume;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getBoolean("completed")).orElse(false);
    }

    public void checkTerminalInteraction(Player player, ResourceLocation interactedTerminal) {
        if (this.targetTerminalId != null && this.targetTerminalId.equals(interactedTerminal)) {
            player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                CompoundTag taskNbt = data.getOrCreateTaskProgress(this.getTaskId());
                if (!taskNbt.getBoolean("completed")) {
                    taskNbt.putBoolean("completed", true);
                }
            });
        }
    }

    public void tryConsume(Player player) {
        if (!consume) return; 

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
            nbt.putBoolean("completed", false);
        });
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", "location_terminal");
        nbt.putString("TargetTerminal",
                this.targetTerminalId != null ? this.targetTerminalId.toString() : "minecraft:air");
        nbt.putBoolean("consume", consume); 
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("TargetTerminal")) {
            this.targetTerminalId = new ResourceLocation(nbt.getString("TargetTerminal"));
        }
        this.consume = nbt.getBoolean("consume"); 
    }
}
