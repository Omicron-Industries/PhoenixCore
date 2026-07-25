package net.phoenix.core.integration.phoenix_guilds;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GuildAPI {

    private GuildAPI() {}

    public static boolean isInGuild(UUID playerUUID) {
        GuildManager mgr = manager();
        return mgr != null && mgr.isInGuild(playerUUID);
    }

    public static Optional<String> getGuildName(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(Guild::getName);
    }

    public static Optional<UUID> getGuildId(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(Guild::getId);
    }

    public static Set<UUID> getGuildMembers(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Collections.emptySet();
        return mgr.getGuildFor(playerUUID)
                .map(g -> Collections.unmodifiableSet(g.getMembers()))
                .orElse(Collections.emptySet());
    }

    public static List<ServerPlayer> getOnlineGuildMembers(UUID playerUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Collections.emptyList();
        GuildManager mgr = GuildManager.get(server.overworld());
        return mgr.getGuildFor(playerUUID)
                .map(g -> g.getMembers().stream()
                        .map(server.getPlayerList()::getPlayer)
                        .filter(p -> p != null)
                        .toList())
                .orElse(Collections.emptyList());
    }

    public static boolean areGuildmates(UUID a, UUID b) {
        if (a.equals(b)) return false;
        GuildManager mgr = manager();
        if (mgr == null) return false;
        Optional<Guild> ga = mgr.getGuildFor(a);
        Optional<Guild> gb = mgr.getGuildFor(b);
        return ga.isPresent() && gb.isPresent() && ga.get().getId().equals(gb.get().getId());
    }

    public static boolean areAllied(UUID a, UUID b) {
        GuildManager mgr = manager();
        if (mgr == null) return false;
        Optional<Guild> ga = mgr.getGuildFor(a);
        Optional<Guild> gb = mgr.getGuildFor(b);
        if (ga.isEmpty() || gb.isEmpty()) return false;
        if (ga.get().getId().equals(gb.get().getId())) return false;
        return ga.get().isAlly(gb.get().getId());
    }

    public static boolean areFriendly(UUID a, UUID b) {
        return areGuildmates(a, b) || areAllied(a, b);
    }

    public static Optional<GuildRank> getGuildRank(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(g -> g.getRank(playerUUID));
    }

    public static boolean hasRank(UUID playerUUID, GuildRank required) {
        return getGuildRank(playerUUID).map(r -> r.isAtLeast(required)).orElse(false);
    }

    public static boolean isOwner(UUID playerUUID) {
        return hasRank(playerUUID, GuildRank.OWNER);
    }

    public static boolean isOfficerOrAbove(UUID playerUUID) {
        return hasRank(playerUUID, GuildRank.OFFICER);
    }

    public static Optional<String> getGuildNameById(UUID guildId) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildById(guildId).map(Guild::getName);
    }

    public static Set<UUID> getGuildMembersById(UUID guildId) {
        GuildManager mgr = manager();
        if (mgr == null) return Collections.emptySet();
        return mgr.getGuildById(guildId)
                .map(g -> Collections.unmodifiableSet(g.getMembers()))
                .orElse(Collections.emptySet());
    }

    public static UUID getGuildIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;
        GuildManager mgr = manager();
        if (mgr == null) return playerUUID;
        return mgr.getGuildFor(playerUUID)
                .map(Guild::getId)
                .orElse(playerUUID);
    }

    public static boolean isPlayerInGuildOrIs(UUID playerUUID, UUID token) {
        if (playerUUID == null || token == null) return false;
        if (playerUUID.equals(token)) return true;
        GuildManager mgr = manager();
        if (mgr == null) return false;
        return mgr.getGuildById(token)
                .map(g -> g.isMember(playerUUID))
                .orElse(false);
    }

    public static String getDisplayName(UUID guildOrPlayerToken) {
        if (guildOrPlayerToken == null) return "Unknown";
        GuildManager mgr = manager();
        if (mgr == null) return "Player: " + guildOrPlayerToken.toString().substring(0, 8);
        return mgr.getGuildById(guildOrPlayerToken)
                .map(Guild::getName)
                .orElse("Player: " + guildOrPlayerToken.toString().substring(0, 8));
    }

    private static GuildManager manager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return GuildManager.get(server.overworld());
    }
}
