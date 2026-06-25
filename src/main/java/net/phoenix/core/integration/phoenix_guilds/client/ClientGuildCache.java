package net.phoenix.core.integration.phoenix_guilds.client;

import net.minecraft.client.Minecraft;
import net.phoenix.core.integration.phoenix_guilds.network.S2CGuildSyncPacket;

import java.util.List;
import java.util.UUID;

/** Client-side mirror of the player's guild state, updated by S2CGuildSyncPacket. */
public final class ClientGuildCache {

    private ClientGuildCache() {}

    public static String  guildName    = null;
    public static UUID    ownerUUID    = null;
    public static String  motd         = "";
    public static String  description  = "";
    public static boolean friendlyFire = false;
    public static boolean homeSet      = false;

    public static List<S2CGuildSyncPacket.MemberEntry>  members         = List.of();
    public static List<S2CGuildSyncPacket.AllyEntry>    allies          = List.of();
    public static List<S2CGuildSyncPacket.PendingEntry> pendingOutgoing = List.of();
    public static List<S2CGuildSyncPacket.PendingEntry> pendingIncoming = List.of();
    public static List<S2CGuildSyncPacket.LogEntry>     logEntries      = List.of();
    public static List<S2CGuildSyncPacket.WikiPage>     wikiPages       = List.of();
    public static List<S2CGuildSyncPacket.GuildSummary> allGuilds       = List.of();

    public static void update(String name, UUID owner, String motdVal, String descVal,
                              boolean ff, boolean hs,
                              List<S2CGuildSyncPacket.MemberEntry>  memberList,
                              List<S2CGuildSyncPacket.AllyEntry>    allyList,
                              List<S2CGuildSyncPacket.PendingEntry> outgoing,
                              List<S2CGuildSyncPacket.PendingEntry> incoming,
                              List<S2CGuildSyncPacket.LogEntry>     log,
                              List<S2CGuildSyncPacket.WikiPage>     wiki,
                              List<S2CGuildSyncPacket.GuildSummary> allList) {
        guildName    = name;    ownerUUID    = owner;
        motd         = motdVal; description  = descVal;
        friendlyFire = ff;      homeSet      = hs;
        members      = memberList; allies      = allyList;
        pendingOutgoing = outgoing; pendingIncoming = incoming;
        logEntries   = log;     wikiPages    = wiki;    allGuilds    = allList;
    }

    public static boolean isInGuild() { return guildName != null; }

    public static boolean isOwner() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ownerUUID != null && ownerUUID.equals(mc.player.getUUID());
    }

    /** Returns the local player's rank string ("OWNER", "OFFICER", "MEMBER") or null if not in guild. */
    public static String myRank() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isInGuild()) return null;
        UUID me = mc.player.getUUID();
        return members.stream().filter(m -> m.uuid().equals(me))
                .map(S2CGuildSyncPacket.MemberEntry::rank).findFirst().orElse("MEMBER");
    }

    public static boolean isAtLeastOfficer() {
        String r = myRank();
        return "OFFICER".equals(r) || "OWNER".equals(r);
    }

    public static void clear() {
        guildName = null; ownerUUID = null; motd = ""; description = "";
        friendlyFire = false; homeSet = false;
        members = List.of(); allies = List.of();
        pendingOutgoing = List.of(); pendingIncoming = List.of();
        logEntries = List.of(); wikiPages = List.of(); allGuilds = List.of();
    }
}
