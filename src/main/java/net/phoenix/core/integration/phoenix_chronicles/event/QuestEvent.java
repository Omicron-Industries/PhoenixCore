package net.phoenix.core.integration.phoenix_chronicles.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;

/**
 * Parent event class for all Chronicle quest-system updates.
 * Registered on the main MinecraftForge.EVENT_BUS.
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
}
