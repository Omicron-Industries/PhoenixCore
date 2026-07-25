package net.phoenix.core.integration.phoenix_chronicles.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import lombok.Getter;

public class PhoenixQuestScriptRewardEvent extends PlayerEvent {

    private final ServerPlayer player;
    private final String eventId;
    
    @Getter
    private final CompoundTag data;

    public PhoenixQuestScriptRewardEvent(ServerPlayer player, String eventId, CompoundTag data) {
        super(player);
        this.player = player;
        this.eventId = eventId;
        this.data = data != null ? data : new CompoundTag();
    }

    public ServerPlayer getServerPlayer() {
        return player;
    }

    public String getEventId() {
        return eventId;
    }
}
