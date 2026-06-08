package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

/**
 * Task: Reach a specific experience level threshold.
 * Migrated to evaluate player properties natively without saving redundant runtime states.
 */
public class ExperienceTask extends QuestTask {

    // FIXED: Removed 'final' so the global data loader can hydrate this during disk processing
    private int requiredLevel;

    public ExperienceTask(ResourceLocation taskId, Component description, int requiredLevel) {
        super(taskId, description);
        this.requiredLevel = requiredLevel;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Replaces the old generic isCompleted().
     * Checks the player's level directly in real-time.
     */
    @Override
    public boolean isCompletedFor(Player player) {
        return player.experienceLevel >= requiredLevel;
    }

    /**
     * Retained for backward compatibility with tracking loops,
     * but no longer mutates a shared local object variable.
     */
    public void checkPlayerLevel(Player player) {
        // No-op or can call your progression update trigger check if necessary:
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", "experience");
        nbt.putInt("RequiredLevel", requiredLevel);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // FIXED: Safely hydrate structural layouts when reading data files from disk
        this.requiredLevel = nbt.getInt("RequiredLevel");
    }
}
