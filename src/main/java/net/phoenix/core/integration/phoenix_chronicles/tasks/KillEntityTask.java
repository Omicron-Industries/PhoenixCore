package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class KillEntityTask extends QuestTask {

    private ResourceLocation entityId;
    private int requiredCount;
    private boolean consume;

    public KillEntityTask(ResourceLocation taskId, Component description, ResourceLocation entityId, int requiredCount,
                          boolean consume) {
        super(taskId, description);
        this.entityId = entityId;
        this.requiredCount = requiredCount;
        this.consume = consume;
    }

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public boolean shouldConsume() {
        return consume;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getBoolean("completed")).orElse(false);
    }

    public void onEntityKilled(Player player, ResourceLocation killedEntityId) {
        if (this.entityId == null || this.requiredCount <= 0) return;

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
            if (nbt.getBoolean("completed")) return;

            if (killedEntityId.equals(entityId)) {
                int current = nbt.getInt("current");
                current = Math.min(current + 1, requiredCount);
                nbt.putInt("current", current);

                if (current >= requiredCount) {
                    nbt.putBoolean("completed", true);
                }
            }
        });
    }

    public void tryConsume(Player player) {
        if (!consume) return;

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
            nbt.putInt("current", 0);
            nbt.putBoolean("completed", false);
        });
    }

    @Override
    public String getProgressString(Player player) {
        int current = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getInt("current"))
                .orElse(0);
        return current + "/" + requiredCount;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "kill_entity");
        tag.putString("entity_id", entityId != null ? entityId.toString() : "minecraft:pig");
        tag.putInt("required", requiredCount);
        tag.putBoolean("consume", consume);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("entity_id")) {
            this.entityId = new ResourceLocation(nbt.getString("entity_id"));
        }
        this.requiredCount = nbt.getInt("required");
        this.consume = nbt.getBoolean("consume");
    }
}
