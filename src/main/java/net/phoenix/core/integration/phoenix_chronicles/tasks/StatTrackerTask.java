package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class StatTrackerTask extends QuestTask {

    private ResourceLocation statId;
    private int targetValue;
    private boolean consume; 

    public StatTrackerTask(ResourceLocation taskId, Component description, ResourceLocation statId, int targetValue,
                           boolean consume) {
        super(taskId, description);
        this.statId = statId;
        this.targetValue = targetValue;
        this.consume = consume;
    }

    public ResourceLocation getStatId() {
        return statId;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public boolean shouldConsume() {
        return consume;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        if (statId == null || targetValue <= 0) return false;

        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraft.stats.StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statId);
            if (type != null) {
                
                int rawStat = serverPlayer.getStats().getValue(Stats.CUSTOM.get(statId));

                int baselineOffset = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                        .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getInt("baseline")).orElse(0);

                int relativeStatProgress = Math.max(0, rawStat - baselineOffset);
                return relativeStatProgress >= targetValue;
            }
        }
        return false;
    }

    public void tryConsume(Player player) {
        if (!consume) return; 

        if (player instanceof ServerPlayer serverPlayer && statId != null) {
            net.minecraft.stats.StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statId);
            if (type != null) {
                int currentRawValue = serverPlayer.getStats().getValue(Stats.CUSTOM.get(statId));

                player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                    CompoundTag nbt = data.getOrCreateTaskProgress(this.getTaskId());
                    nbt.putInt("baseline", currentRawValue);
                });
            }
        }
    }

    @Override
    public String getProgressString(Player player) {
        if (statId == null || !(player instanceof ServerPlayer serverPlayer)) return "0/" + targetValue;

        int rawStat = serverPlayer.getStats().getValue(Stats.CUSTOM.get(statId));
        int baselineOffset = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getInt("baseline")).orElse(0);

        int currentProgress = Math.min(Math.max(0, rawStat - baselineOffset), targetValue);
        return currentProgress + "/" + targetValue;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "stat");
        tag.putString("stat_id", statId != null ? statId.toString() : "minecraft:jump");
        tag.putInt("target", targetValue);
        tag.putBoolean("consume", consume); 
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("stat_id")) {
            this.statId = new ResourceLocation(nbt.getString("stat_id"));
        }
        this.targetValue = nbt.getInt("target");
        this.consume = nbt.getBoolean("consume"); 
    }
}
