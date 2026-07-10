package net.phoenix.core.integration.phoenix_guilds.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_guilds.GuildEvents;
import net.phoenix.core.integration.phoenix_guilds.GuildManager;

import java.util.function.Supplier;

public class C2SGuildActionPacket {

    public enum Action {
        CREATE,
        INVITE,
        REMOVE,
        LEAVE,
        DISBAND,
        PROMOTE,
        DEMOTE,
        TRANSFER,
        SET_MOTD,
        SET_DESC,
        TOGGLE_FF,
        SET_HOME,
        HOME,
        ALLY_REQUEST,
        ALLY_ACCEPT,
        ALLY_DECLINE,
        ALLY_BREAK,
        GUILD_CHAT,
        ALLY_CHAT,
        WIKI_SET,
        WIKI_DELETE
    }

    private final Action action;
    private final String arg;

    public C2SGuildActionPacket(Action action, String arg) {
        this.action = action;
        this.arg = arg;
    }

    public C2SGuildActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.arg = buf.readUtf(600);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(arg, 600);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            GuildManager mgr = GuildManager.get(player.getServer().overworld());
            switch (action) {
                case CREATE -> GuildEvents.handleCreate(player, mgr, arg);
                case INVITE -> GuildEvents.handleInvite(player, mgr, arg);
                case REMOVE -> GuildEvents.handleRemove(player, mgr, arg);
                case LEAVE -> GuildEvents.handleLeave(player, mgr);
                case DISBAND -> GuildEvents.handleDisband(player, mgr);
                case PROMOTE -> GuildEvents.handlePromote(player, mgr, arg);
                case DEMOTE -> GuildEvents.handleDemote(player, mgr, arg);
                case TRANSFER -> GuildEvents.handleTransfer(player, mgr, arg);
                case SET_MOTD -> GuildEvents.handleSetMotd(player, mgr, arg);
                case SET_DESC -> GuildEvents.handleSetDesc(player, mgr, arg);
                case TOGGLE_FF -> GuildEvents.handleToggleFF(player, mgr);
                case SET_HOME -> GuildEvents.handleSetHome(player, mgr);
                case HOME -> GuildEvents.handleHome(player, mgr);
                case ALLY_REQUEST -> GuildEvents.handleAllyRequest(player, mgr, arg);
                case ALLY_ACCEPT -> GuildEvents.handleAllyAccept(player, mgr, arg);
                case ALLY_DECLINE -> GuildEvents.handleAllyDecline(player, mgr, arg);
                case ALLY_BREAK -> GuildEvents.handleAllyBreak(player, mgr, arg);
                case GUILD_CHAT -> GuildEvents.handleGuildChat(player, mgr, arg);
                case ALLY_CHAT -> GuildEvents.handleAllyChat(player, mgr, arg);
                case WIKI_SET -> GuildEvents.handleWikiSet(player, mgr, arg);
                case WIKI_DELETE -> GuildEvents.handleWikiDelete(player, mgr, arg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
