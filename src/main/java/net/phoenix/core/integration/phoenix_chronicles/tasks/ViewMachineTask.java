package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

/**
 * Task: View a machine build guide in Phantasia for at least {@code minSeconds} seconds.
 *
 * Completion is driven client-side by PhantasiaEvents.ViewerClose (see PhantasiaCompat).
 * The client event listener marks progress in the player capability then sends
 * C2SPhantasiaTaskCompletePacket to the server so it persists across sessions.
 *
 * SNBT shape:
 * { type: "view_machine",
 * machine_id: "gtceu:electric_blast_furnace",
 * min_seconds: 3.0,
 * description: "Study the Electric Blast Furnace" }
 *
 * Omitting min_seconds defaults to 3.0 (enough to confirm intent without punishing quick readers).
 * Set to 0.0 to complete the instant the viewer is opened.
 */
public class ViewMachineTask extends QuestTask {

    private String machineId;   // e.g. "gtceu:electric_blast_furnace"
    private float minSeconds;

    public ViewMachineTask(ResourceLocation taskId, Component description, String machineId, float minSeconds) {
        super(taskId, description);
        this.machineId = machineId;
        this.minSeconds = minSeconds;
    }

    public String getMachineId() {
        return machineId;
    }

    public float getMinSeconds() {
        return minSeconds;
    }

    // ── Completion ────────────────────────────────────────────────────────────

    @Override
    public boolean isCompletedFor(Player player) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getOrCreateTaskProgress(getTaskId()).getBoolean("completed"))
                .orElse(false);
    }

    /**
     * Called from PhantasiaCompat's ViewerClose subscriber on the CLIENT.
     * Marks the task done in the local capability so isCompletedFor() returns true
     * immediately — the companion C2SPhantasiaTaskCompletePacket handles the server side.
     */
    public void markCompletedClient(Player player) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .ifPresent(data -> data.getOrCreateTaskProgress(getTaskId()).putBoolean("completed", true));
    }

    @Override
    public String getProgressString(Player player) {
        return isCompletedFor(player) ? "Viewed" : (minSeconds > 0 ? "View for " + (int) minSeconds + "s" : "View");
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "view_machine");
        tag.putString("machine_id", machineId != null ? machineId : "");
        tag.putFloat("min_seconds", minSeconds);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.machineId = nbt.getString("machine_id");
        this.minSeconds = nbt.contains("min_seconds") ? nbt.getFloat("min_seconds") : 3.0f;
    }
}
