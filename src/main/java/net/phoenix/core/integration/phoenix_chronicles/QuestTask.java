package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public abstract class QuestTask {

    private final ResourceLocation taskId;
    private Component description;
    private boolean optional = false;

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

    public void onTick(Player player) {}

    public abstract boolean isCompletedFor(Player player);

    public String getProgressString(Player player) {
        return null;
    }

    public ResourceLocation getDisplayItemId() {
        return null;
    }

    public abstract CompoundTag serializeNBT();

    public abstract void deserializeNBT(CompoundTag nbt);
}
