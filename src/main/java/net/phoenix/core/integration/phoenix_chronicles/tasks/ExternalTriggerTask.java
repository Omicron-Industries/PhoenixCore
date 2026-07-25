package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

public class ExternalTriggerTask extends QuestTask {

    private String triggerId = "";
    private int required = 1;

    private String kjsTypeId = null;

    public ExternalTriggerTask(ResourceLocation taskId, Component description, String triggerId, int required) {
        super(taskId, description);
        this.triggerId = triggerId;
        this.required = Math.max(1, required);
    }

    public String getTriggerId() {
        return triggerId;
    }

    public int getRequired() {
        return required;
    }

    public void setKjsTypeId(String id) {
        this.kjsTypeId = id;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(getTaskId()).getInt("current") >= required)
                .orElse(false);
    }

    @Override
    public String getProgressString(Player player) {
        int current = player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(getTaskId()).getInt("current"))
                .orElse(0);
        return required == 1 ? (current >= 1 ? "Done" : "Pending") : current + "/" + required;
    }

    public void onExternalEvent(Player player, net.minecraft.nbt.CompoundTag eventData) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            CompoundTag nbt = data.getOrCreateTaskProgress(getTaskId());
            if (nbt.getInt("current") >= required) return;
            int next = Math.min(nbt.getInt("current") + 1, required);
            nbt.putInt("current", next);
        });
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", kjsTypeId != null ? kjsTypeId : "external_trigger");
        tag.putString("trigger_id", triggerId);
        tag.putInt("required", required);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("trigger_id")) this.triggerId = nbt.getString("trigger_id");
        if (nbt.contains("required")) this.required = Math.max(1, nbt.getInt("required"));
    }
}
