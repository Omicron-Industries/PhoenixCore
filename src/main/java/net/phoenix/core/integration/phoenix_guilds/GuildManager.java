package net.phoenix.core.integration.phoenix_guilds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GuildManager extends SavedData {

    private static final String SAVE_KEY = "phoenix_guilds";

    private final Map<UUID, Guild> guilds      = new LinkedHashMap<>();
    private final Map<UUID, UUID>  memberIndex = new LinkedHashMap<>();

    // ── SavedData factory ─────────────────────────────────────────────────────

    public static GuildManager get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                GuildManager::load, GuildManager::new, SAVE_KEY);
    }

    private static GuildManager load(CompoundTag tag) {
        GuildManager mgr = new GuildManager();
        ListTag list = tag.getList("guilds", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Guild g = Guild.deserialize(list.getCompound(i));
            mgr.guilds.put(g.getId(), g);
            for (UUID m : g.getMembers()) mgr.memberIndex.put(m, g.getId());
        }
        return mgr;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Guild g : guilds.values()) list.add(g.serialize());
        tag.put("guilds", list);
        return tag;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public Optional<Guild> getGuildFor(UUID playerUUID) {
        UUID gid = memberIndex.get(playerUUID);
        return gid == null ? Optional.empty() : Optional.ofNullable(guilds.get(gid));
    }

    public Optional<Guild> getGuildById(UUID guildId) {
        return Optional.ofNullable(guilds.get(guildId));
    }

    public Optional<Guild> getGuildByName(String name) {
        return guilds.values().stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst();
    }

    public Collection<Guild> getAllGuilds()          { return guilds.values(); }
    public boolean isInGuild(UUID playerUUID)        { return memberIndex.containsKey(playerUUID); }

    // ── Member mutations ──────────────────────────────────────────────────────

    public Guild createGuild(String name, UUID ownerUUID) {
        Guild g = new Guild(UUID.randomUUID(), name, ownerUUID);
        guilds.put(g.getId(), g);
        memberIndex.put(ownerUUID, g.getId());
        g.addLog(ownerUUID + " founded the guild.");
        setDirty();
        return g;
    }

    public boolean addMember(UUID guildId, UUID playerUUID) {
        Guild g = guilds.get(guildId);
        if (g == null || memberIndex.containsKey(playerUUID) || g.isFull()) return false;
        g.addMember(playerUUID);
        memberIndex.put(playerUUID, guildId);
        setDirty();
        return true;
    }

    public boolean removeMember(UUID guildId, UUID playerUUID) {
        Guild g = guilds.get(guildId);
        if (g == null || !g.isMember(playerUUID)) return false;
        g.removeMember(playerUUID);
        memberIndex.remove(playerUUID);
        if (playerUUID.equals(g.getOwner())) {
            Optional<UUID> next = g.getMembers().stream().findFirst();
            if (next.isPresent()) {
                g.setOwner(next.get());
            } else {
                disbandGuild(guildId);
                return true;
            }
        }
        setDirty();
        return true;
    }

    public void disbandGuild(UUID guildId) {
        Guild g = guilds.remove(guildId);
        if (g == null) return;
        g.getMembers().forEach(memberIndex::remove);
        for (Guild other : guilds.values()) {
            other.removeAlly(guildId);
            other.removePendingOutgoing(guildId);
        }
        setDirty();
    }

    // ── Rank mutations ────────────────────────────────────────────────────────

    /** Returns error key or "ok". */
    public String promotePlayer(UUID guildId, UUID promoterUUID, UUID targetUUID) {
        Guild g = guilds.get(guildId);
        if (g == null) return "not_in_guild";
        if (!g.hasRank(promoterUUID, GuildRank.OWNER)) return "no_permission";
        if (!g.isMember(targetUUID)) return "not_member";
        GuildRank current = g.getRank(targetUUID);
        if (current == GuildRank.OWNER) return "already_owner";
        if (current == GuildRank.OFFICER) {
            // Promote to owner = transfer
            g.setOwner(targetUUID);
        } else {
            g.getMemberRanks().put(targetUUID, GuildRank.OFFICER);
        }
        setDirty();
        return "ok";
    }

    public String demotePlayer(UUID guildId, UUID demoterUUID, UUID targetUUID) {
        Guild g = guilds.get(guildId);
        if (g == null) return "not_in_guild";
        if (!g.hasRank(demoterUUID, GuildRank.OWNER)) return "no_permission";
        if (!g.isMember(targetUUID)) return "not_member";
        GuildRank current = g.getRank(targetUUID);
        if (current == GuildRank.OWNER) return "cant_demote_owner";
        if (current == GuildRank.MEMBER) return "already_member";
        g.getMemberRanks().put(targetUUID, GuildRank.MEMBER);
        setDirty();
        return "ok";
    }

    public String transferOwnership(UUID guildId, UUID ownerUUID, UUID targetUUID) {
        Guild g = guilds.get(guildId);
        if (g == null) return "not_in_guild";
        if (!g.getOwner().equals(ownerUUID)) return "no_permission";
        if (!g.isMember(targetUUID)) return "not_member";
        g.setOwner(targetUUID);
        setDirty();
        return "ok";
    }

    // ── Guild settings mutations ───────────────────────────────────────────────

    public boolean setMotd(UUID guildId, UUID playerUUID, String motd) {
        Guild g = guilds.get(guildId);
        if (g == null || !g.hasRank(playerUUID, GuildRank.OFFICER)) return false;
        g.setMotd(motd);
        setDirty();
        return true;
    }

    public boolean setDescription(UUID guildId, UUID playerUUID, String desc) {
        Guild g = guilds.get(guildId);
        if (g == null || !g.hasRank(playerUUID, GuildRank.OFFICER)) return false;
        g.setDescription(desc);
        setDirty();
        return true;
    }

    public boolean toggleFriendlyFire(UUID guildId, UUID ownerUUID) {
        Guild g = guilds.get(guildId);
        if (g == null || !g.getOwner().equals(ownerUUID)) return false;
        g.setFriendlyFire(!g.isFriendlyFire());
        setDirty();
        return true;
    }

    public boolean setHome(UUID guildId, UUID playerUUID,
                           net.minecraft.resources.ResourceLocation dim,
                           double x, double y, double z, float yaw, float pitch) {
        Guild g = guilds.get(guildId);
        if (g == null || !g.hasRank(playerUUID, GuildRank.OFFICER)) return false;
        g.setHome(dim, x, y, z, yaw, pitch);
        setDirty();
        return true;
    }

    // ── Wiki mutations ────────────────────────────────────────────────────────

    public String setWikiPage(UUID guildId, UUID playerUUID, String title, String content) {
        Guild g = guilds.get(guildId);
        if (g == null) return "not_in_guild";
        if (!g.hasRank(playerUUID, GuildRank.OFFICER)) return "no_permission";
        if (title.isBlank()) return "empty_title";
        if (!g.setWikiPage(title, content)) return "wiki_full";
        setDirty();
        return "ok";
    }

    public String deleteWikiPage(UUID guildId, UUID playerUUID, String title) {
        Guild g = guilds.get(guildId);
        if (g == null) return "not_in_guild";
        if (!g.hasRank(playerUUID, GuildRank.OFFICER)) return "no_permission";
        if (!g.deleteWikiPage(title)) return "not_found";
        setDirty();
        return "ok";
    }

    // ── Alliance mutations ────────────────────────────────────────────────────

    public String sendAllyRequest(UUID requesterGuildId, String targetName) {
        Guild requester = guilds.get(requesterGuildId);
        if (requester == null) return "not_in_guild";
        Optional<Guild> targetOpt = getGuildByName(targetName);
        if (targetOpt.isEmpty()) return "guild_not_found";
        Guild target = targetOpt.get();
        if (target.getId().equals(requesterGuildId)) return "self";
        if (requester.isAlly(target.getId())) return "already_ally";
        if (requester.hasPendingOutgoing(target.getId())) return "already_sent";
        if (target.hasPendingOutgoing(requesterGuildId)) {
            target.removePendingOutgoing(requesterGuildId);
            requester.addAlly(target.getId());
            target.addAlly(requesterGuildId);
            requester.addLog("Alliance formed with " + target.getName() + ".");
            target.addLog("Alliance formed with " + requester.getName() + ".");
            setDirty();
            return "accepted";
        }
        requester.addPendingOutgoing(target.getId());
        setDirty();
        return "sent";
    }

    public String acceptAllyRequest(UUID accepterGuildId, String senderName) {
        Guild accepter = guilds.get(accepterGuildId);
        if (accepter == null) return "not_in_guild";
        Optional<Guild> senderOpt = getGuildByName(senderName);
        if (senderOpt.isEmpty()) return "guild_not_found";
        Guild sender = senderOpt.get();
        if (!sender.hasPendingOutgoing(accepterGuildId)) return "no_request";
        sender.removePendingOutgoing(accepterGuildId);
        accepter.addAlly(sender.getId());
        sender.addAlly(accepterGuildId);
        accepter.addLog("Alliance formed with " + sender.getName() + ".");
        sender.addLog("Alliance formed with " + accepter.getName() + ".");
        setDirty();
        return "accepted";
    }

    public String declineAllyRequest(UUID declinerGuildId, String senderName) {
        Guild decliner = guilds.get(declinerGuildId);
        if (decliner == null) return "not_in_guild";
        Optional<Guild> senderOpt = getGuildByName(senderName);
        if (senderOpt.isEmpty()) return "guild_not_found";
        Guild sender = senderOpt.get();
        if (!sender.hasPendingOutgoing(declinerGuildId)) return "no_request";
        sender.removePendingOutgoing(declinerGuildId);
        setDirty();
        return "declined";
    }

    public String breakAlliance(UUID breakerGuildId, String targetName) {
        Guild breaker = guilds.get(breakerGuildId);
        if (breaker == null) return "not_in_guild";
        Optional<Guild> targetOpt = getGuildByName(targetName);
        if (targetOpt.isEmpty()) return "guild_not_found";
        Guild target = targetOpt.get();
        if (!breaker.isAlly(target.getId())) return "not_ally";
        breaker.removeAlly(target.getId());
        target.removeAlly(breakerGuildId);
        breaker.removePendingOutgoing(target.getId());
        target.removePendingOutgoing(breakerGuildId);
        breaker.addLog("Alliance with " + target.getName() + " was broken.");
        target.addLog("Alliance with " + breaker.getName() + " was broken.");
        setDirty();
        return "broken";
    }
}
