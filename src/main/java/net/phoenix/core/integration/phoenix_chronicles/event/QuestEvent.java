package net.phoenix.core.integration.phoenix_chronicles.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;

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

    @Cancelable
    public static class PlayerTick extends QuestEvent {

        public PlayerTick(Player player, QuestNode node) {
            super(player, node);
        }
    }

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

    @Cancelable
    public static class ExternalEvent extends QuestEvent {

        private final String triggerId;
        private final net.minecraft.nbt.CompoundTag data;

        public ExternalEvent(Player player, QuestNode node, String triggerId, net.minecraft.nbt.CompoundTag data) {
            super(player, node);
            this.triggerId = triggerId;
            this.data = data != null ? data : new net.minecraft.nbt.CompoundTag();
        }

        public String getTriggerId() {
            return triggerId;
        }

        public net.minecraft.nbt.CompoundTag getData() {
            return data;
        }
    }
}
