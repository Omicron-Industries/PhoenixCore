package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

/**
 * Task: Triggered when a player completes a specific Minecraft advancement.
 * SNBT shape: { type: "advancement", advancement_id: "minecraft:story/mine_diamond" }
 * Refactored to evaluate player properties natively without saving redundant NBT states.
 */
public class AdvancementTask extends QuestTask {

    // REMOVED final: Must be assignable by the data loader inside deserializeNBT()
    private ResourceLocation advancementId;

    public AdvancementTask(ResourceLocation taskId, Component description, ResourceLocation advancementId) {
        super(taskId, description);
        this.advancementId = advancementId;
    }

    public ResourceLocation getAdvancementId() {
        return advancementId;
    }

    /**
     * Context-driven completion check required by the stateless QuestTask superclass.
     * Inspects vanilla's advancement tracker on the server side live.
     */
    @Override
    public boolean isCompletedFor(Player player) {
        if (advancementId == null) return false;

        // Advancements are strictly tracked server-side on ServerPlayer
        if (player instanceof ServerPlayer serverPlayer) {
            Advancement adv = serverPlayer.getServer().getAdvancements().getAdvancement(advancementId);
            if (adv != null) {
                return serverPlayer.getAdvancements().getOrStartProgress(adv).isDone();
            }
        }
        return false;
    }

    public void onAdvancementEarned(Player player, ResourceLocation earnedId) {
        // No-op: Completely managed polymorphically via isCompletedFor(player) now!
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "advancement");
        tag.putString("advancement_id", advancementId != null ? advancementId.toString() : "minecraft:story/root");
        // REMOVED: tag.putBoolean("completed", this.completed);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("advancement_id")) {
            this.advancementId = new ResourceLocation(nbt.getString("advancement_id"));
        }
    }
}
