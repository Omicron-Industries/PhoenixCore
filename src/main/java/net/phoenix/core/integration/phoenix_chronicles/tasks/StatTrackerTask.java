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

/**
 * Task: Reach a cumulative total in a native vanilla statistic.
 * Supports dynamic baselines for repeatable or progressive milestone cycles.
 * SNBT shape: { type: "stat", stat_id: "minecraft:jump", target_value: 1000, consume: true }
 */
public class StatTrackerTask extends QuestTask {

    private ResourceLocation statId;
    private int targetValue;
    private boolean consume; // NEW: Uniform toggle rule (controls progressive/repeatable baseline snapshots)

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

    /**
     * Context-driven completion check required by the stateless QuestTask superclass.
     * Evaluates the vanilla stats layer live against the requested player context, subtracting any active baselines.
     */
    @Override
    public boolean isCompletedFor(Player player) {
        if (statId == null || targetValue <= 0) return false;

        // Stats are strictly handled on the logical server side via ServerPlayer
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraft.stats.StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statId);
            if (type != null) {
                // Grab the raw global value tracked natively by vanilla
                int rawStat = serverPlayer.getStats().getValue(Stats.CUSTOM.get(statId));

                // Subtract any saved baseline offset stored within this player's capability instance
                int baselineOffset = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                        .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getInt("baseline")).orElse(0);

                int relativeStatProgress = Math.max(0, rawStat - baselineOffset);
                return relativeStatProgress >= targetValue;
            }
        }
        return false;
    }

    /**
     * Call this when claiming rewards.
     * If 'consume' is true, it snapshots the current stats value as a baseline offset for repeatable/bounty capability
     * updates.
     */
    public void tryConsume(Player player) {
        if (!consume) return; // Keep standard milestone tracking active if consumption rules are disabled

        if (player instanceof ServerPlayer serverPlayer && statId != null) {
            net.minecraft.stats.StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statId);
            if (type != null) {
                int currentRawValue = serverPlayer.getStats().getValue(Stats.CUSTOM.get(statId));

                // Lock down the baseline snapshot inside the isolated player capability data context
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
        tag.putBoolean("consume", consume); // Save parameter config
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("stat_id")) {
            this.statId = new ResourceLocation(nbt.getString("stat_id"));
        }
        this.targetValue = nbt.getInt("target");
        this.consume = nbt.getBoolean("consume"); // Load parameter config
    }
}
