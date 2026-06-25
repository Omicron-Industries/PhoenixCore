package net.phoenix.core.integration.phoenix_guilds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Guild {

    public  static final int MAX_MEMBERS = 30;
    private static final int MAX_LOG     = 25;
    private static final int MAX_WIKI    = 20;

    public record LogEntry(long timestamp, String message) {}

    private final UUID   id;
    private       String name;
    private       UUID   owner;

    private final Set<UUID>           members         = new LinkedHashSet<>();
    private final Map<UUID, GuildRank> memberRanks    = new LinkedHashMap<>();
    private final Set<UUID>           allies          = new LinkedHashSet<>();
    private final Set<UUID>           pendingOutgoing = new LinkedHashSet<>();
    private final Deque<LogEntry>     log             = new ArrayDeque<>();
    private final Map<String, String> wikiPages       = new LinkedHashMap<>(); // title → content (ordered insertion)

    private String           motd         = "";
    private String           description  = "";
    private boolean          friendlyFire = false; // false = PvP between members disabled
    private double           homeX, homeY, homeZ;
    private float            homeYaw, homePitch;
    private ResourceLocation homeDimension = null;

    public Guild(UUID id, String name, UUID owner) {
        this.id    = id;
        this.name  = name;
        this.owner = owner;
        members.add(owner);
        memberRanks.put(owner, GuildRank.OWNER);
    }

    // ── Basic accessors ───────────────────────────────────────────────────────

    public UUID   getId()            { return id; }
    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }
    public UUID   getOwner()         { return owner; }

    public void setOwner(UUID u) {
        if (memberRanks.containsKey(this.owner))
            memberRanks.put(this.owner, GuildRank.OFFICER);
        this.owner = u;
        memberRanks.put(u, GuildRank.OWNER);
    }

    public Set<UUID>           getMembers()         { return members; }
    public Map<UUID, GuildRank> getMemberRanks()    { return memberRanks; }
    public Set<UUID>           getAllies()           { return allies; }
    public Set<UUID>           getPendingOutgoing() { return pendingOutgoing; }
    public Deque<LogEntry>     getLog()             { return log; }

    public String  getMotd()         { return motd; }
    public void    setMotd(String m) { this.motd = m; }

    public String  getDescription()         { return description; }
    public void    setDescription(String d) { this.description = d; }

    public boolean isFriendlyFire()             { return friendlyFire; }
    public void    setFriendlyFire(boolean ff)  { this.friendlyFire = ff; }

    public boolean isHomeSet()       { return homeDimension != null; }
    public double  getHomeX()        { return homeX; }
    public double  getHomeY()        { return homeY; }
    public double  getHomeZ()        { return homeZ; }
    public float   getHomeYaw()      { return homeYaw; }
    public float   getHomePitch()    { return homePitch; }
    public ResourceLocation getHomeDimension() { return homeDimension; }

    public void setHome(ResourceLocation dim, double x, double y, double z, float yaw, float pitch) {
        this.homeDimension = dim;
        this.homeX = x; this.homeY = y; this.homeZ = z;
        this.homeYaw = yaw; this.homePitch = pitch;
    }

    // ── Rank helpers ──────────────────────────────────────────────────────────

    public GuildRank getRank(UUID uuid) {
        return memberRanks.getOrDefault(uuid, GuildRank.MEMBER);
    }

    public boolean hasRank(UUID uuid, GuildRank required) {
        return getRank(uuid).isAtLeast(required);
    }

    // ── Member mutations ──────────────────────────────────────────────────────

    public boolean isMember(UUID uuid)   { return members.contains(uuid); }

    public void addMember(UUID uuid) {
        members.add(uuid);
        memberRanks.putIfAbsent(uuid, GuildRank.MEMBER);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        memberRanks.remove(uuid);
    }

    // ── Ally helpers ──────────────────────────────────────────────────────────

    public boolean isAlly(UUID guildId)              { return allies.contains(guildId); }
    public void    addAlly(UUID guildId)             { allies.add(guildId); }
    public void    removeAlly(UUID guildId)          { allies.remove(guildId); }

    public boolean hasPendingOutgoing(UUID guildId)    { return pendingOutgoing.contains(guildId); }
    public void    addPendingOutgoing(UUID guildId)    { pendingOutgoing.add(guildId); }
    public void    removePendingOutgoing(UUID guildId) { pendingOutgoing.remove(guildId); }

    // ── Wiki ──────────────────────────────────────────────────────────────────

    public Map<String, String> getWikiPages()             { return wikiPages; }
    public int  getWikiPageCount()                        { return wikiPages.size(); }
    public boolean isFull()                               { return members.size() >= MAX_MEMBERS; }
    public boolean wikiFull()                             { return wikiPages.size() >= MAX_WIKI; }

    public boolean setWikiPage(String title, String content) {
        if (!wikiPages.containsKey(title) && wikiFull()) return false;
        wikiPages.put(title, content);
        return true;
    }

    public boolean deleteWikiPage(String title) {
        return wikiPages.remove(title) != null;
    }

    // ── Log ───────────────────────────────────────────────────────────────────

    public void addLog(String message) {
        log.addFirst(new LogEntry(System.currentTimeMillis(), message));
        while (log.size() > MAX_LOG) log.removeLast();
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putUUID("owner", owner);
        tag.putString("motd", motd);
        tag.putString("description", description);
        tag.putBoolean("friendlyFire", friendlyFire);
        if (homeDimension != null) {
            tag.putString("homeDim", homeDimension.toString());
            tag.putDouble("homeX", homeX);
            tag.putDouble("homeY", homeY);
            tag.putDouble("homeZ", homeZ);
            tag.putFloat("homeYaw", homeYaw);
            tag.putFloat("homePitch", homePitch);
        }
        tag.put("members",         uuidSet(members));
        tag.put("allies",          uuidSet(allies));
        tag.put("pendingOutgoing", uuidSet(pendingOutgoing));

        // Ranks
        ListTag rankList = new ListTag();
        for (Map.Entry<UUID, GuildRank> e : memberRanks.entrySet()) {
            CompoundTag r = new CompoundTag();
            r.putUUID("uuid", e.getKey());
            r.putString("rank", e.getValue().name());
            rankList.add(r);
        }
        tag.put("ranks", rankList);

        // Wiki
        ListTag wikiList = new ListTag();
        for (Map.Entry<String, String> e : wikiPages.entrySet()) {
            CompoundTag w = new CompoundTag();
            w.putString("title", e.getKey());
            w.putString("content", e.getValue());
            wikiList.add(w);
        }
        tag.put("wiki", wikiList);

        // Log
        ListTag logList = new ListTag();
        for (LogEntry entry : log) {
            CompoundTag l = new CompoundTag();
            l.putLong("ts", entry.timestamp());
            l.putString("msg", entry.message());
            logList.add(l);
        }
        tag.put("log", logList);

        return tag;
    }

    public static Guild deserialize(CompoundTag tag) {
        UUID id     = tag.getUUID("id");
        String name = tag.getString("name");
        UUID owner  = tag.getUUID("owner");
        Guild g     = new Guild(id, name, owner);
        g.members.clear();
        g.memberRanks.clear();

        g.motd        = tag.getString("motd");
        g.description = tag.getString("description");
        g.friendlyFire= tag.getBoolean("friendlyFire");

        if (tag.contains("homeDim")) {
            g.homeDimension = new ResourceLocation(tag.getString("homeDim"));
            g.homeX = tag.getDouble("homeX"); g.homeY = tag.getDouble("homeY");
            g.homeZ = tag.getDouble("homeZ");
            g.homeYaw = tag.getFloat("homeYaw"); g.homePitch = tag.getFloat("homePitch");
        }

        readUuidSet(tag, "members",         g.members);
        readUuidSet(tag, "allies",          g.allies);
        readUuidSet(tag, "pendingOutgoing", g.pendingOutgoing);

        ListTag rankList = tag.getList("ranks", Tag.TAG_COMPOUND);
        for (int i = 0; i < rankList.size(); i++) {
            CompoundTag r = rankList.getCompound(i);
            try {
                UUID uuid = r.getUUID("uuid");
                GuildRank rank = GuildRank.valueOf(r.getString("rank"));
                g.memberRanks.put(uuid, rank);
            } catch (Exception ignored) {}
        }
        // Ensure all members have a rank entry
        for (UUID m : g.members) g.memberRanks.putIfAbsent(m, GuildRank.MEMBER);
        g.memberRanks.put(owner, GuildRank.OWNER);

        ListTag wikiList = tag.getList("wiki", Tag.TAG_COMPOUND);
        for (int i = 0; i < wikiList.size(); i++) {
            CompoundTag w = wikiList.getCompound(i);
            g.wikiPages.put(w.getString("title"), w.getString("content"));
        }

        ListTag logList = tag.getList("log", Tag.TAG_COMPOUND);
        for (int i = 0; i < logList.size(); i++) {
            CompoundTag l = logList.getCompound(i);
            g.log.addLast(new LogEntry(l.getLong("ts"), l.getString("msg")));
        }

        return g;
    }

    private static ListTag uuidSet(Set<UUID> set) {
        ListTag list = new ListTag();
        for (UUID u : set) list.add(StringTag.valueOf(u.toString()));
        return list;
    }

    private static void readUuidSet(CompoundTag tag, String key, Set<UUID> out) {
        if (!tag.contains(key)) return;
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try { out.add(UUID.fromString(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
    }
}
