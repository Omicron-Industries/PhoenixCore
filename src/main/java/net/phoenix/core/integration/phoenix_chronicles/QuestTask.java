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
    private Component description;
    private boolean optional = false;

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

    public void setDescription(Component d) {
        this.description = d;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean v) {
        this.optional = v;
    }

    /**
     * Called once per server tick for each active (non-completed) quest the player has.
     * Override in tasks that need to poll world state (biome, structure, etc.).
     * The default is a no-op; keep overrides cheap.
     */
    public void onTick(Player player) {}

    /**
     * Context-driven completion check.
     * Every custom task must evaluate this against a specific player's metrics or capability data.
     */
    public abstract boolean isCompletedFor(Player player);

    /**
     * Optional numeric progress label shown in the HUD and detail screen (e.g. "7/10").
     * Returns null when there is no meaningful partial progress to display (e.g. one-shot tasks).
     */
    public String getProgressString(Player player) {
        return null;
    }

    /** Optional item icon shown next to this task in the detail screen. Return null to show none. */
    public ResourceLocation getDisplayItemId() {
        return null;
    }

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
