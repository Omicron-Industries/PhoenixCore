package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class CraftItemTask extends QuestTask {

    private ResourceLocation itemId;
    private int requiredCount;

    public CraftItemTask(ResourceLocation taskId, Component description, ResourceLocation itemId, int requiredCount) {
        super(taskId, description);
        this.itemId = itemId;
        this.requiredCount = requiredCount;
    }

    public ResourceLocation getItemId() {
        return itemId;
    }

    @Override
    public ResourceLocation getDisplayItemId() {
        return itemId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).map(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
            return nbt.getBoolean("completed");
        }).orElse(false);
    }

    public void onItemCrafted(Player player, ResourceLocation craftedItemId, int amount) {
        if (this.itemId == null || this.requiredCount <= 0) return;

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
            if (nbt.getBoolean("completed")) return;

            if (craftedItemId.equals(itemId)) {
                int currentCount = nbt.getInt("current");
                currentCount = Math.min(currentCount + amount, requiredCount);

                nbt.putInt("current", currentCount);
                if (currentCount >= requiredCount) {
                    nbt.putBoolean("completed", true);
                }
            }
        });
    }

    @Override
    public String getProgressString(Player player) {
        int currentCount = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getInt("current")).orElse(0);
        return currentCount + "/" + requiredCount;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "craft_item");
        tag.putString("item_id", itemId != null ? itemId.toString() : "minecraft:air");
        tag.putInt("required", requiredCount);
        
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        
        if (nbt.contains("item_id")) {
            this.itemId = new ResourceLocation(nbt.getString("item_id"));
        }
        this.requiredCount = nbt.getInt("required");
    }
}
