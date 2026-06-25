package net.phoenix.core.integration.phoenix_guilds;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Stable public API for the Phoenix Guilds system.
 *
 * Safe to call from any server-side code — returns empty/false gracefully if
 * the server is not running or the player is not in a guild.
 *
 * Do NOT call from client-side code; use ClientGuildCache for that.
 */
public final class GuildAPI {

    private GuildAPI() {}

    // ── Guild membership ──────────────────────────────────────────────────────

    /** Returns true if the player is in any guild. */
    public static boolean isInGuild(UUID playerUUID) {
        GuildManager mgr = manager();
        return mgr != null && mgr.isInGuild(playerUUID);
    }

    /** Returns the name of the guild the player belongs to, or empty. */
    public static Optional<String> getGuildName(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(Guild::getName);
    }

    /** Returns the internal UUID of the guild the player belongs to, or empty. */
    public static Optional<UUID> getGuildId(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(Guild::getId);
    }

    /** Returns the UUID set of all members in the player's guild, or an empty set. */
    public static Set<UUID> getGuildMembers(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Collections.emptySet();
        return mgr.getGuildFor(playerUUID)
                .map(g -> Collections.unmodifiableSet(g.getMembers()))
                .orElse(Collections.emptySet());
    }

    /** Returns all currently online members of the player's guild. */
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

    // ── Relationship checks ───────────────────────────────────────────────────

    /** Returns true if both players are in the same guild. */
    public static boolean areGuildmates(UUID a, UUID b) {
        if (a.equals(b)) return false;
        GuildManager mgr = manager();
        if (mgr == null) return false;
        Optional<Guild> ga = mgr.getGuildFor(a);
        Optional<Guild> gb = mgr.getGuildFor(b);
        return ga.isPresent() && gb.isPresent() && ga.get().getId().equals(gb.get().getId());
    }

    /**
     * Returns true if the two players are in guilds that have an active alliance.
     * Returns false if either player is not in a guild, or if they are guildmates.
     */
    public static boolean areAllied(UUID a, UUID b) {
        GuildManager mgr = manager();
        if (mgr == null) return false;
        Optional<Guild> ga = mgr.getGuildFor(a);
        Optional<Guild> gb = mgr.getGuildFor(b);
        if (ga.isEmpty() || gb.isEmpty()) return false;
        if (ga.get().getId().equals(gb.get().getId())) return false;
        return ga.get().isAlly(gb.get().getId());
    }

    /**
     * Returns true if the two players are friendly — either guildmates or allied.
     * Useful for damage cancellation or friendly-fire checks in external mods.
     */
    public static boolean areFriendly(UUID a, UUID b) {
        return areGuildmates(a, b) || areAllied(a, b);
    }

    // ── Rank ─────────────────────────────────────────────────────────────────

    /** Returns the player's rank in their guild, or empty if not in one. */
    public static Optional<GuildRank> getGuildRank(UUID playerUUID) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildFor(playerUUID).map(g -> g.getRank(playerUUID));
    }

    /** Returns true if the player is at least the given rank in their guild. */
    public static boolean hasRank(UUID playerUUID, GuildRank required) {
        return getGuildRank(playerUUID).map(r -> r.isAtLeast(required)).orElse(false);
    }

    /** Returns true if the player is the owner of their guild. */
    public static boolean isOwner(UUID playerUUID) {
        return hasRank(playerUUID, GuildRank.OWNER);
    }

    /** Returns true if the player is at least an officer in their guild. */
    public static boolean isOfficerOrAbove(UUID playerUUID) {
        return hasRank(playerUUID, GuildRank.OFFICER);
    }

    // ── Guild lookup ──────────────────────────────────────────────────────────

    /** Looks up a guild by name (case-insensitive). Returns empty if not found. */
    public static Optional<String> getGuildNameById(UUID guildId) {
        GuildManager mgr = manager();
        if (mgr == null) return Optional.empty();
        return mgr.getGuildById(guildId).map(Guild::getName);
    }

    /** Returns the member UUIDs of a specific guild by its ID. */
    public static Set<UUID> getGuildMembersById(UUID guildId) {
        GuildManager mgr = manager();
        if (mgr == null) return Collections.emptySet();
        return mgr.getGuildById(guildId)
                .map(g -> Collections.unmodifiableSet(g.getMembers()))
                .orElse(Collections.emptySet());
    }

    // ── Ownership-token helpers (multiblock / machine use) ────────────────────

    /**
     * Returns the guild UUID if the player is in a guild, otherwise the player's own UUID.
     *
     * Store this as the "owner token" on placement. Use {@link #isPlayerInGuildOrIs} for
     * access checks. Mirrors {@code TeamUtils.getTeamIdOrPlayerFallback}.
     */
    public static UUID getGuildIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;
        GuildManager mgr = manager();
        if (mgr == null) return playerUUID;
        return mgr.getGuildFor(playerUUID)
                .map(Guild::getId)
                .orElse(playerUUID);
    }

    /**
     * Returns true if the player is a member of the guild identified by {@code token},
     * or if {@code token} is the player's own UUID (solo/unguilded owner).
     *
     * Use this for every machine access check after storing the token on placement.
     */
    public static boolean isPlayerInGuildOrIs(UUID playerUUID, UUID token) {
        if (playerUUID == null || token == null) return false;
        if (playerUUID.equals(token)) return true;
        GuildManager mgr = manager();
        if (mgr == null) return false;
        return mgr.getGuildById(token)
                .map(g -> g.isMember(playerUUID))
                .orElse(false);
    }

    /**
     * Returns a human-readable name for an ownership token — either the guild name,
     * or a short player-UUID fallback. Safe to call with either a guild UUID or player UUID.
     *
     * Mirrors {@code TeamUtils.getTeamName}.
     */
    public static String getDisplayName(UUID guildOrPlayerToken) {
        if (guildOrPlayerToken == null) return "Unknown";
        GuildManager mgr = manager();
        if (mgr == null) return "Player: " + guildOrPlayerToken.toString().substring(0, 8);
        return mgr.getGuildById(guildOrPlayerToken)
                .map(Guild::getName)
                .orElse("Player: " + guildOrPlayerToken.toString().substring(0, 8));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static GuildManager manager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return GuildManager.get(server.overworld());
    }
}
