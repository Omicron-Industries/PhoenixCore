package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

/**
 * Task: Travel to a specific dimension.
 * SNBT shape: { type: "dimension", target: "minecraft:the_nether" }
 * Migrated to player-capability data layers to safely isolate multi-player progress states.
 */
public class DimensionTask extends QuestTask {

    // REMOVED final: Must be assignable by the data loader inside deserializeNBT()
    private ResourceKey<Level> targetDimension;

    public DimensionTask(ResourceLocation taskId, Component description, ResourceKey<Level> targetDimension) {
        super(taskId, description);
        this.targetDimension = targetDimension;
    }

    public ResourceKey<Level> getTargetDimension() {
        return targetDimension;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(this.getTaskId()).getBoolean("completed")).orElse(false);
    }

    /** Call this from the PlayerEvent.PlayerChangedDimensionEvent hook */
    public void onChangedDimension(Player player, ResourceKey<Level> dimension) {
        if (targetDimension == null) return;

        if (dimension.equals(targetDimension)) {
            player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                CompoundTag taskNbt = data.getOrCreateTaskProgress(this.getTaskId());
                if (!taskNbt.getBoolean("completed")) {
                    taskNbt.putBoolean("completed", true);
                }
            });
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "dimension");
        tag.putString("target",
                targetDimension != null ? targetDimension.location().toString() : "minecraft:overworld");
        // REMOVED: tag.putBoolean("completed", this.completed);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("target")) {
            this.targetDimension = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(nbt.getString("target")));
        }
    }
}
