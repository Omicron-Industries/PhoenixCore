package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

public class AdvancementTask extends QuestTask {

    private ResourceLocation advancementId;

    public AdvancementTask(ResourceLocation taskId, Component description, ResourceLocation advancementId) {
        super(taskId, description);
        this.advancementId = advancementId;
    }

    public ResourceLocation getAdvancementId() {
        return advancementId;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        if (advancementId == null) return false;

        if (player instanceof ServerPlayer serverPlayer) {
            Advancement adv = serverPlayer.getServer().getAdvancements().getAdvancement(advancementId);
            if (adv != null) {
                return serverPlayer.getAdvancements().getOrStartProgress(adv).isDone();
            }
        }
        return false;
    }

    public void onAdvancementEarned(Player player, ResourceLocation earnedId) {}

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "advancement");
        tag.putString("advancement_id", advancementId != null ? advancementId.toString() : "minecraft:story/root");

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("advancement_id")) {
            this.advancementId = new ResourceLocation(nbt.getString("advancement_id"));
        }
    }
}
