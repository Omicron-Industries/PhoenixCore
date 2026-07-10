package net.phoenix.core.integration.phoenix_chronicles.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;

/**
 * Parent event class for all Chronicle quest-system updates.
 * Registered on the main MinecraftForge.EVENT_BUS.
 *
 * ── Outbound (quest system → world) ──────────────────────────────────────────
 * QuestEvent.StateChanged — quest state transition (LOCKED→UNLOCKED→COMPLETED)
 * QuestEvent.RewardClaimed — player just claimed rewards for a completed quest
 * QuestEvent.PlayerTick — (cancelable) suppress default task evaluation for one quest/player
 *
 * ── Inbound (world → quest system) ───────────────────────────────────────────
 * Use {@link net.phoenix.core.integration.phoenix_chronicles.QuestAPI#fireExternalEvent}
 * to signal that a custom event occurred; any ExternalTriggerTask listening for that
 * trigger_id will advance its progress.
 */
public class QuestEvent extends Event {

    private final Player player;
    private final QuestNode node;

    public QuestEvent(Player player, QuestNode node) {
        this.player = player;
        this.node = node;
    }

    public Player getPlayer() {
        return player;
    }

    public QuestNode getNode() {
        return node;
    }

    /**
     * Fired during the ServerPlayer background tick.
     * Cancel this event to prevent standard task evaluation for this node.
     */
    @Cancelable
    public static class PlayerTick extends QuestEvent {

        public PlayerTick(Player player, QuestNode node) {
            super(player, node);
        }
    }

    /**
     * Fired immediately AFTER a quest has changed state in the player capability.
     * Perfect for syncing packets, giving rewards, logging, or sound cues.
     */
    public static class StateChanged extends QuestEvent {

        private final QuestState oldState;
        private final QuestState newState;

        public StateChanged(Player player, QuestNode node, QuestState oldState, QuestState newState) {
            super(player, node);
            this.oldState = oldState;
            this.newState = newState;
        }

        public QuestState getOldState() {
            return oldState;
        }

        public QuestState getNewState() {
            return newState;
        }
    }

    /**
     * Fired after all rewards for a completed quest have been granted to the player.
     * Cancelable — cancel to prevent the reward grant (e.g. inventory full guard).
     *
     * <p>
     * Example (KubeJS server_scripts):
     * 
     * <pre>{@code
     * ForgeEvents.onEvent('net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent$RewardClaimed',
     *   event => {
     *     event.player.tell('You claimed rewards for: ' + event.node.title.string)
     *   })
     * }</pre>
     */
    @Cancelable
    public static class RewardClaimed extends QuestEvent {

        private final ServerPlayer serverPlayer;

        public RewardClaimed(ServerPlayer player, QuestNode node) {
            super(player, node);
            this.serverPlayer = player;
        }

        public ServerPlayer getServerPlayer() {
            return serverPlayer;
        }
    }

    /**
     * Fired when an external event is signalled via
     * {@link net.phoenix.core.integration.phoenix_chronicles.QuestAPI#fireExternalEvent}.
     * Mods can subscribe to inspect or cancel the signal before the quest system processes it.
     *
     * <p>
     * Cancelable — cancel to suppress this external event from reaching any tasks.
     */
    @Cancelable
    public static class ExternalEvent extends QuestEvent {

        private final String triggerId;
        private final net.minecraft.nbt.CompoundTag data;

        public ExternalEvent(Player player, QuestNode node, String triggerId, net.minecraft.nbt.CompoundTag data) {
            super(player, node);
            this.triggerId = triggerId;
            this.data = data != null ? data : new net.minecraft.nbt.CompoundTag();
        }

        /** The trigger ID string passed to {@code QuestAPI.fireExternalEvent()}. */
        public String getTriggerId() {
            return triggerId;
        }

        /** Arbitrary data passed alongside the event. Empty tag if none provided. */
        public net.minecraft.nbt.CompoundTag getData() {
            return data;
        }
    }
}
