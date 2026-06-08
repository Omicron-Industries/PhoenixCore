package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Base abstract class for all Chronicle Quest Objectives.
 * Migrated to be stateless to ensure full multi-player capability isolation.
 */
public abstract class QuestTask {

    private final ResourceLocation taskId;
    private final Component description;

    // REMOVED: protected boolean completed = false;
    // Tasks are singletons loaded from server files; they must not hold local state variables.

    public QuestTask(ResourceLocation taskId, Component description) {
        this.taskId = taskId;
        this.description = description;
    }

    public ResourceLocation getTaskId() {
        return taskId;
    }

    public Component getDescription() {
        return description;
    }

    /**
     * Context-driven completion check.
     * Every custom task must evaluate this against a specific player's metrics or capability data.
     */
    public abstract boolean isCompletedFor(Player player);

    // REMOVED: public boolean isCompleted()
    // REMOVED: public void setCompleted(boolean completed)

    /**
     * Serializes static structural task data (e.g., Target item ID, Required quantity) to NBT.
     */
    public abstract CompoundTag serializeNBT();

    /**
     * Deserializes structural settings back into the task configuration.
     */
    public abstract void deserializeNBT(CompoundTag nbt);
}
