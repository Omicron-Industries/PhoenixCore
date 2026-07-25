package net.phoenix.core.integration.phoenix_guilds.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_guilds.client.ClientGuildCache;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CGuildSyncPacket {

    public record MemberEntry(UUID uuid, String name, boolean isOnline, String rank) {}

    public record GuildSummary(String name, int memberCount, int onlineCount, String description) {}

    public record AllyEntry(String name, int memberCount, int onlineCount) {}

    public record PendingEntry(String guildName) {}

    public record LogEntry(long timestamp, String message) {}

    public record WikiPage(String title, String content) {}

    private final String guildName;
    private final UUID ownerUUID;
    private final String motd;
    private final String description;
    private final boolean friendlyFire;
    private final boolean homeSet;
    private final List<MemberEntry> members;
    private final List<AllyEntry> allies;
    private final List<PendingEntry> pendingOutgoing;
    private final List<PendingEntry> pendingIncoming;
    private final List<LogEntry> logEntries;
    private final List<WikiPage> wikiPages;
    private final List<GuildSummary> allGuilds;

    public S2CGuildSyncPacket(String guildName, UUID ownerUUID, String motd, String description,
                              boolean friendlyFire, boolean homeSet,
                              List<MemberEntry> members, List<AllyEntry> allies,
                              List<PendingEntry> pendingOutgoing, List<PendingEntry> pendingIncoming,
                              List<LogEntry> logEntries, List<WikiPage> wikiPages, List<GuildSummary> allGuilds) {
        this.guildName = guildName;
        this.ownerUUID = ownerUUID;
        this.motd = motd;
        this.description = description;
        this.friendlyFire = friendlyFire;
        this.homeSet = homeSet;
        this.members = members;
        this.allies = allies;
        this.pendingOutgoing = pendingOutgoing;
        this.pendingIncoming = pendingIncoming;
        this.logEntries = logEntries;
        this.wikiPages = wikiPages;
        this.allGuilds = allGuilds;
    }

    public S2CGuildSyncPacket(FriendlyByteBuf buf) {
        boolean inGuild = buf.readBoolean();
        if (inGuild) {
            this.guildName = buf.readUtf(64);
            this.ownerUUID = buf.readUUID();
            this.motd = buf.readUtf(128);
            this.description = buf.readUtf(256);
            this.friendlyFire = buf.readBoolean();
            this.homeSet = buf.readBoolean();
            this.members = readList(buf,
                    b -> new MemberEntry(b.readUUID(), b.readUtf(32), b.readBoolean(), b.readUtf(16)));
            this.allies = readList(buf, b -> new AllyEntry(b.readUtf(64), b.readVarInt(), b.readVarInt()));
            this.pendingOutgoing = readList(buf, b -> new PendingEntry(b.readUtf(64)));
            this.pendingIncoming = readList(buf, b -> new PendingEntry(b.readUtf(64)));
            this.logEntries = readList(buf, b -> new LogEntry(b.readLong(), b.readUtf(256)));
            this.wikiPages = readList(buf, b -> new WikiPage(b.readUtf(64), b.readUtf(512)));
        } else {
            this.guildName = null;
            this.ownerUUID = null;
            this.motd = "";
            this.description = "";
            this.friendlyFire = false;
            this.homeSet = false;
            this.members = List.of();
            this.allies = List.of();
            this.pendingOutgoing = List.of();
            this.pendingIncoming = List.of();
            this.logEntries = List.of();
            this.wikiPages = List.of();
        }
        this.allGuilds = readList(buf,
                b -> new GuildSummary(b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readUtf(256)));
    }

    public void encode(FriendlyByteBuf buf) {
        boolean inGuild = guildName != null;
        buf.writeBoolean(inGuild);
        if (inGuild) {
            buf.writeUtf(guildName, 64);
            buf.writeUUID(ownerUUID);
            buf.writeUtf(motd, 128);
            buf.writeUtf(description, 256);
            buf.writeBoolean(friendlyFire);
            buf.writeBoolean(homeSet);
            writeList(buf, members, (b, m) -> {
                b.writeUUID(m.uuid());
                b.writeUtf(m.name(), 32);
                b.writeBoolean(m.isOnline());
                b.writeUtf(m.rank(), 16);
            });
            writeList(buf, allies, (b, a) -> {
                b.writeUtf(a.name(), 64);
                b.writeVarInt(a.memberCount());
                b.writeVarInt(a.onlineCount());
            });
            writeList(buf, pendingOutgoing, (b, p) -> b.writeUtf(p.guildName(), 64));
            writeList(buf, pendingIncoming, (b, p) -> b.writeUtf(p.guildName(), 64));
            writeList(buf, logEntries, (b, l) -> {
                b.writeLong(l.timestamp());
                b.writeUtf(l.message(), 256);
            });
            writeList(buf, wikiPages, (b, w) -> {
                b.writeUtf(w.title(), 64);
                b.writeUtf(w.content(), 512);
            });
        }
        writeList(buf, allGuilds, (b, g) -> {
            b.writeUtf(g.name(), 64);
            b.writeVarInt(g.memberCount());
            b.writeVarInt(g.onlineCount());
            b.writeUtf(g.description(), 256);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyOnClient(this)));
        ctx.get().setPacketHandled(true);
    }

    private static void applyOnClient(S2CGuildSyncPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientGuildCache.update(pkt.guildName, pkt.ownerUUID, pkt.motd, pkt.description,
                pkt.friendlyFire, pkt.homeSet,
                pkt.members, pkt.allies, pkt.pendingOutgoing, pkt.pendingIncoming,
                pkt.logEntries, pkt.wikiPages, pkt.allGuilds);
        if (mc.screen instanceof net.phoenix.core.integration.phoenix_guilds.client.GuildScreen gs)
            gs.onDataRefreshed();
    }

    @FunctionalInterface
    private interface ElemReader<T> {

        T read(FriendlyByteBuf b);
    }

    @FunctionalInterface
    private interface ElemWriter<T> {

        void write(FriendlyByteBuf b, T t);
    }

    private static <T> List<T> readList(FriendlyByteBuf buf, ElemReader<T> r) {
        int n = buf.readVarInt();
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(r.read(buf));
        return out;
    }

    private static <T> void writeList(FriendlyByteBuf buf, List<T> list, ElemWriter<T> w) {
        buf.writeVarInt(list.size());
        for (T t : list) w.write(buf, t);
    }
}
