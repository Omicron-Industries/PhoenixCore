package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

public class ExperienceTask extends QuestTask {

    private int requiredLevel;

    public ExperienceTask(ResourceLocation taskId, Component description, int requiredLevel) {
        super(taskId, description);
        this.requiredLevel = requiredLevel;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.experienceLevel >= requiredLevel;
    }

    public void checkPlayerLevel(Player player) {
        
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
        
        this.requiredLevel = nbt.getInt("RequiredLevel");
    }
}
