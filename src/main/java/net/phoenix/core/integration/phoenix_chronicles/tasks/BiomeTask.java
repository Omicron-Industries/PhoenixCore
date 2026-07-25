package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class BiomeTask extends QuestTask {

    private ResourceLocation biomeId;

    public BiomeTask(ResourceLocation taskId, Component description, ResourceLocation biomeId) {
        super(taskId, description);
        this.biomeId = biomeId;
    }

    public ResourceLocation getBiomeId() {
        return biomeId;
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide || biomeId == null) return;
        if (isCompletedFor(player)) return;

        ResourceLocation current = player.level().getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId.equals(current)) {
            player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                    .ifPresent(data -> data.getOrCreateTaskProgress(getTaskId()).putBoolean("completed", true));
        }
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
        tag.putString("type", "biome");
        tag.putString("biome_id", biomeId != null ? biomeId.toString() : "minecraft:plains");
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("biome_id")) biomeId = new ResourceLocation(nbt.getString("biome_id"));
    }
}
