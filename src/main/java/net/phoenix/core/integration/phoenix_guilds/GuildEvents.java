package net.phoenix.core.integration.phoenix_guilds;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_guilds.network.S2CGuildSyncPacket;
import net.phoenix.core.integration.phoenix_guilds.network.S2COpenGuildScreenPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuildEvents {

    // Home cooldown: playerUUID → cooldown expiry epoch ms (not persisted, fine)
    private static final Map<UUID, Long> HOME_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long HOME_COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes

    // ── Forge events ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GuildManager mgr = GuildManager.get(player.getServer().overworld());
        // Show MOTD if set
        mgr.getGuildFor(player.getUUID()).ifPresent(g -> {
            if (!g.getMotd().isBlank())
                player.sendSystemMessage(Component.literal("§6[Guild MOTD] §f" + g.getMotd()));
            g.addLog(player.getName().getString() + " logged in.");
            mgr.setDirty();
        });
        syncToPlayer(player, mgr);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer attacker)) return;
        if (victim.getUUID().equals(attacker.getUUID())) return;

        GuildManager mgr = GuildManager.get(victim.getServer().overworld());
        Optional<Guild> vGuild = mgr.getGuildFor(victim.getUUID());
        Optional<Guild> aGuild = mgr.getGuildFor(attacker.getUUID());

        // Cancel if same guild and friendly fire is off
        if (vGuild.isPresent() && aGuild.isPresent() && vGuild.get().getId().equals(aGuild.get().getId()) &&
                !vGuild.get().isFriendlyFire()) {
            event.setCanceled(true);
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();

        // /guilds
        d.register(Commands.literal("guilds")
                .executes(ctx -> openGui(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("gui").executes(ctx -> openGui(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    handleCreate(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    handleInvite(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            EntityArgument.getPlayer(ctx, "player").getName().getString());
                                    return 1;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    handleRemove(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            EntityArgument.getPlayer(ctx, "player").getName().getString());
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            handleLeave(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("disband")
                        .executes(ctx -> {
                            handleDisband(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    handlePromote(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            EntityArgument.getPlayer(ctx, "player").getName().getString());
                                    return 1;
                                })))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    handleDemote(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            EntityArgument.getPlayer(ctx, "player").getName().getString());
                                    return 1;
                                })))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    handleTransfer(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            EntityArgument.getPlayer(ctx, "player").getName().getString());
                                    return 1;
                                })))
                .then(Commands.literal("motd")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    handleSetMotd(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            StringArgumentType.getString(ctx, "text"));
                                    return 1;
                                })))
                .then(Commands.literal("desc")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    handleSetDesc(ctx.getSource().getPlayerOrException(),
                                            get(ctx.getSource().getPlayerOrException()),
                                            StringArgumentType.getString(ctx, "text"));
                                    return 1;
                                })))
                .then(Commands.literal("friendlyfire")
                        .executes(ctx -> {
                            handleToggleFF(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("sethome")
                        .executes(ctx -> {
                            handleSetHome(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("home")
                        .executes(ctx -> {
                            handleHome(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            cmdInfo(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            cmdList(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("log")
                        .executes(ctx -> {
                            cmdLog(ctx.getSource().getPlayerOrException(), get(ctx.getSource().getPlayerOrException()));
                            return 1;
                        }))
                .then(Commands.literal("wiki")
                        .executes(ctx -> openGui(ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            handleWikiDelete(ctx.getSource().getPlayerOrException(),
                                                    get(ctx.getSource().getPlayerOrException()),
                                                    StringArgumentType.getString(ctx, "title"));
                                            return 1;
                                        }))))
                .then(Commands.literal("ally")
                        .then(Commands.literal("request")
                                .then(Commands.argument("guild", StringArgumentType.word())
                                        .executes(ctx -> {
                                            handleAllyRequest(ctx.getSource().getPlayerOrException(),
                                                    get(ctx.getSource().getPlayerOrException()),
                                                    StringArgumentType.getString(ctx, "guild"));
                                            return 1;
                                        })))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("guild", StringArgumentType.word())
                                        .executes(ctx -> {
                                            handleAllyAccept(ctx.getSource().getPlayerOrException(),
                                                    get(ctx.getSource().getPlayerOrException()),
                                                    StringArgumentType.getString(ctx, "guild"));
                                            return 1;
                                        })))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("guild", StringArgumentType.word())
                                        .executes(ctx -> {
                                            handleAllyDecline(ctx.getSource().getPlayerOrException(),
                                                    get(ctx.getSource().getPlayerOrException()),
                                                    StringArgumentType.getString(ctx, "guild"));
                                            return 1;
                                        })))
                        .then(Commands.literal("break")
                                .then(Commands.argument("guild", StringArgumentType.word())
                                        .executes(ctx -> {
                                            handleAllyBreak(ctx.getSource().getPlayerOrException(),
                                                    get(ctx.getSource().getPlayerOrException()),
                                                    StringArgumentType.getString(ctx, "guild"));
                                            return 1;
                                        })))));

        // /gc — guild chat
        d.register(Commands.literal("gc")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            handleGuildChat(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()),
                                    StringArgumentType.getString(ctx, "message"));
                            return 1;
                        })));

        // /ac — ally chat
        d.register(Commands.literal("ac")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            handleAllyChat(ctx.getSource().getPlayerOrException(),
                                    get(ctx.getSource().getPlayerOrException()),
                                    StringArgumentType.getString(ctx, "message"));
                            return 1;
                        })));
    }

    // ── Sync helpers ──────────────────────────────────────────────────────────

    public static S2CGuildSyncPacket buildPacketFor(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());

        String guildName = null;
        UUID ownerUUID = null;
        String motd = "";
        String description = "";
        boolean ff = false;
        boolean homeSet = false;
        List<S2CGuildSyncPacket.MemberEntry> members = List.of();
        List<S2CGuildSyncPacket.AllyEntry> allies = List.of();
        List<S2CGuildSyncPacket.PendingEntry> pendingOutgoing = List.of();
        List<S2CGuildSyncPacket.PendingEntry> pendingIncoming = List.of();
        List<S2CGuildSyncPacket.LogEntry> logEntries = List.of();
        List<S2CGuildSyncPacket.WikiPage> wikiPageList = List.of();

        if (opt.isPresent()) {
            Guild g = opt.get();
            guildName = g.getName();
            ownerUUID = g.getOwner();
            motd = g.getMotd();
            description = g.getDescription();
            ff = g.isFriendlyFire();
            homeSet = g.isHomeSet();

            List<S2CGuildSyncPacket.MemberEntry> ml = new ArrayList<>();
            for (UUID uuid : g.getMembers()) {
                ServerPlayer online = player.getServer().getPlayerList().getPlayer(uuid);
                String name = online != null ? online.getName().getString() : uuid.toString().substring(0, 8) + "…";
                ml.add(new S2CGuildSyncPacket.MemberEntry(uuid, name, online != null, g.getRank(uuid).name()));
            }
            members = ml;

            List<S2CGuildSyncPacket.AllyEntry> al = new ArrayList<>();
            for (UUID allyId : g.getAllies()) {
                mgr.getGuildById(allyId).ifPresent(ally -> {
                    long online = ally.getMembers().stream()
                            .filter(u -> player.getServer().getPlayerList().getPlayer(u) != null).count();
                    al.add(new S2CGuildSyncPacket.AllyEntry(ally.getName(), ally.getMembers().size(), (int) online));
                });
            }
            allies = al;

            List<S2CGuildSyncPacket.PendingEntry> out = new ArrayList<>();
            for (UUID pid : g.getPendingOutgoing())
                mgr.getGuildById(pid).ifPresent(t -> out.add(new S2CGuildSyncPacket.PendingEntry(t.getName())));
            pendingOutgoing = out;

            List<S2CGuildSyncPacket.PendingEntry> inc = new ArrayList<>();
            for (Guild other : mgr.getAllGuilds())
                if (!other.getId().equals(g.getId()) && other.hasPendingOutgoing(g.getId()))
                    inc.add(new S2CGuildSyncPacket.PendingEntry(other.getName()));
            pendingIncoming = inc;

            List<S2CGuildSyncPacket.LogEntry> ll = new ArrayList<>();
            for (Guild.LogEntry e : g.getLog()) ll.add(new S2CGuildSyncPacket.LogEntry(e.timestamp(), e.message()));
            logEntries = ll;

            List<S2CGuildSyncPacket.WikiPage> wl = new ArrayList<>();
            for (Map.Entry<String, String> e : g.getWikiPages().entrySet())
                wl.add(new S2CGuildSyncPacket.WikiPage(e.getKey(), e.getValue()));
            wikiPageList = wl;
        }

        List<S2CGuildSyncPacket.GuildSummary> all = new ArrayList<>();
        for (Guild g : mgr.getAllGuilds()) {
            long online = g.getMembers().stream().filter(u -> player.getServer().getPlayerList().getPlayer(u) != null)
                    .count();
            all.add(new S2CGuildSyncPacket.GuildSummary(g.getName(), g.getMembers().size(), (int) online,
                    g.getDescription()));
        }

        return new S2CGuildSyncPacket(guildName, ownerUUID, motd, description, ff, homeSet,
                members, allies, pendingOutgoing, pendingIncoming, logEntries, wikiPageList, all);
    }

    public static void syncToPlayer(ServerPlayer player, GuildManager mgr) {
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), buildPacketFor(player, mgr));
    }

    public static void syncToGuild(Guild guild, MinecraftServer server, GuildManager mgr) {
        for (UUID uuid : guild.getMembers()) {
            ServerPlayer m = server.getPlayerList().getPlayer(uuid);
            if (m != null) syncToPlayer(m, mgr);
        }
    }

    public static void broadcastAllGuildList(MinecraftServer server, GuildManager mgr) {
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (!mgr.isInGuild(p.getUUID())) syncToPlayer(p, mgr);
    }

    // ── Member handlers (called by both commands and C2S packet) ──────────────

    public static void handleCreate(ServerPlayer player, GuildManager mgr, String name) {
        if (name.isBlank()) {
            send(player, "§cGuild name cannot be empty.");
            return;
        }
        if (mgr.isInGuild(player.getUUID())) {
            send(player, "§cYou are already in a guild.");
            return;
        }
        if (mgr.getGuildByName(name).isPresent()) {
            send(player, "§cA guild named '§f" + name + "§c' already exists.");
            return;
        }
        Guild g = mgr.createGuild(name, player.getUUID());
        send(player, "§aCreated guild §f" + g.getName() + "§a.");
        syncToPlayer(player, mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleInvite(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can invite players.");
            return;
        }
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            send(player, "§cPlayer '§f" + targetName + "§c' is not online.");
            return;
        }
        if (mgr.isInGuild(target.getUUID())) {
            send(player, "§c" + targetName + " is already in a guild.");
            return;
        }
        if (g.isFull()) {
            send(player, "§cYour guild is full (§f" + Guild.MAX_MEMBERS + "§c members max).");
            return;
        }
        mgr.addMember(g.getId(), target.getUUID());
        g.addLog(player.getName().getString() + " invited " + targetName + ".");
        send(player, "§aAdded §f" + targetName + " §ato §f" + g.getName() + "§a.");
        target.sendSystemMessage(Component.literal(
                "§aYou were added to guild §f" + g.getName() + " §aby §f" + player.getName().getString() + "§a."));
        syncToGuild(g, player.getServer(), mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleRemove(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can remove players.");
            return;
        }
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null || !g.isMember(target.getUUID())) {
            send(player, "§c" + targetName + " is not in your guild or is not online.");
            return;
        }
        if (g.getRank(target.getUUID()).ordinal() >= g.getRank(player.getUUID()).ordinal()) {
            send(player, "§cYou cannot remove someone of equal or higher rank.");
            return;
        }
        mgr.removeMember(g.getId(), target.getUUID());
        g.addLog(player.getName().getString() + " removed " + targetName + ".");
        send(player, "§cRemoved §f" + targetName + " §cfrom §f" + g.getName() + "§c.");
        target.sendSystemMessage(Component.literal("§cYou were removed from guild §f" + g.getName() + "§c."));
        syncToGuild(g, player.getServer(), mgr);
        syncToPlayer(target, mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleLeave(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        String name = g.getName();
        UUID gid = g.getId();
        g.addLog(player.getName().getString() + " left the guild.");
        mgr.removeMember(gid, player.getUUID());
        send(player, "§cLeft guild §f" + name + "§c.");
        syncToPlayer(player, mgr);
        mgr.getGuildById(gid).ifPresent(s -> syncToGuild(s, player.getServer(), mgr));
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleDisband(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.getOwner().equals(player.getUUID())) {
            send(player, "§cOnly the owner can disband the guild.");
            return;
        }
        String name = g.getName();
        List<ServerPlayer> allOnline = onlineMembers(g, player.getServer());
        for (ServerPlayer m : allOnline)
            if (!m.getUUID().equals(player.getUUID()))
                m.sendSystemMessage(Component.literal("§cGuild §f" + name + " §cwas disbanded."));
        mgr.disbandGuild(g.getId());
        send(player, "§cDisbanded guild §f" + name + "§c.");
        for (ServerPlayer m : allOnline) syncToPlayer(m, mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    // ── Rank handlers ─────────────────────────────────────────────────────────

    public static void handlePromote(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null || !g.isMember(target.getUUID())) {
            send(player, "§c" + targetName + " is not in your guild or is not online.");
            return;
        }
        String result = mgr.promotePlayer(g.getId(), player.getUUID(), target.getUUID());
        switch (result) {
            case "ok" -> {
                GuildRank newRank = g.getRank(target.getUUID());
                g.addLog(player.getName().getString() + " promoted " + targetName + " to " + newRank.label() + ".");
                send(player, "§aPromoted §f" + targetName + " §ato §f" + newRank.label() + "§a.");
                target.sendSystemMessage(Component
                        .literal("§aYou were promoted to §f" + newRank.label() + " §ain §f" + g.getName() + "§a."));
                syncToGuild(g, player.getServer(), mgr);
            }
            case "no_permission" -> send(player, "§cOnly the guild owner can promote players.");
            case "already_owner" -> send(player, "§cUse /guilds transfer to make them owner.");
            default -> send(player, "§cCould not promote: " + result);
        }
    }

    public static void handleDemote(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null || !g.isMember(target.getUUID())) {
            send(player, "§c" + targetName + " is not in your guild or is not online.");
            return;
        }
        String result = mgr.demotePlayer(g.getId(), player.getUUID(), target.getUUID());
        switch (result) {
            case "ok" -> {
                g.addLog(player.getName().getString() + " demoted " + targetName + " to Member.");
                send(player, "§7Demoted §f" + targetName + " §7to Member.");
                target.sendSystemMessage(Component.literal("§7You were demoted to Member in §f" + g.getName() + "§7."));
                syncToGuild(g, player.getServer(), mgr);
            }
            case "no_permission" -> send(player, "§cOnly the guild owner can demote players.");
            case "cant_demote_owner" -> send(player, "§cCannot demote the owner.");
            case "already_member" -> send(player, "§c" + targetName + " is already a Member.");
            default -> send(player, "§cCould not demote: " + result);
        }
    }

    public static void handleTransfer(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null || !g.isMember(target.getUUID())) {
            send(player, "§c" + targetName + " is not in your guild or is not online.");
            return;
        }
        String result = mgr.transferOwnership(g.getId(), player.getUUID(), target.getUUID());
        if ("ok".equals(result)) {
            g.addLog(player.getName().getString() + " transferred ownership to " + targetName + ".");
            send(player, "§aTransferred ownership of §f" + g.getName() + " §ato §f" + targetName + "§a.");
            target.sendSystemMessage(Component.literal("§aYou are now the owner of guild §f" + g.getName() + "§a."));
            syncToGuild(g, player.getServer(), mgr);
        } else {
            send(player, "§cOnly the guild owner can transfer ownership.");
        }
    }

    // ── Settings handlers ─────────────────────────────────────────────────────

    public static void handleSetMotd(ServerPlayer player, GuildManager mgr, String text) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!mgr.setMotd(g.getId(), player.getUUID(), text)) {
            send(player, "§cOfficers and above can set the MOTD.");
            return;
        }
        g.addLog(player.getName().getString() + " updated the MOTD.");
        send(player, "§aGuild MOTD updated.");
        syncToGuild(g, player.getServer(), mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleSetDesc(ServerPlayer player, GuildManager mgr, String text) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!mgr.setDescription(g.getId(), player.getUUID(), text)) {
            send(player, "§cOfficers and above can set the description.");
            return;
        }
        send(player, "§aGuild description updated.");
        syncToGuild(g, player.getServer(), mgr);
        broadcastAllGuildList(player.getServer(), mgr);
    }

    public static void handleToggleFF(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!mgr.toggleFriendlyFire(g.getId(), player.getUUID())) {
            send(player, "§cOnly the guild owner can toggle friendly fire.");
            return;
        }
        boolean nowOn = g.isFriendlyFire();
        g.addLog(player.getName().getString() + " turned friendly fire " + (nowOn ? "ON" : "OFF") + ".");
        send(player, "§aFriendly fire is now §f" + (nowOn ? "§aON" : "§cOFF") + "§a.");
        syncToGuild(g, player.getServer(), mgr);
    }

    public static void handleSetHome(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        ResourceLocation dim = player.level().dimension().location();
        boolean ok = mgr.setHome(g.getId(), player.getUUID(), dim,
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        if (!ok) {
            send(player, "§cOfficers and above can set the guild home.");
            return;
        }
        g.addLog(player.getName().getString() + " set guild home.");
        send(player, "§aGuild home set to your current location.");
        syncToGuild(g, player.getServer(), mgr);
    }

    public static void handleHome(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.isHomeSet()) {
            send(player, "§cGuild home has not been set yet.");
            return;
        }
        // Cooldown check
        long now = System.currentTimeMillis();
        Long expires = HOME_COOLDOWNS.get(player.getUUID());
        if (expires != null && now < expires) {
            long secsLeft = (expires - now) / 1000;
            send(player, "§cGuild home is on cooldown for §f" + secsLeft + "s§c.");
            return;
        }
        HOME_COOLDOWNS.put(player.getUUID(), now + HOME_COOLDOWN_MS);
        // Teleport
        ServerLevel targetLevel = player.getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, g.getHomeDimension()));
        if (targetLevel == null) {
            send(player, "§cGuild home dimension is no longer loaded.");
            return;
        }
        if (player.level() == targetLevel) {
            player.connection.teleport(g.getHomeX(), g.getHomeY(), g.getHomeZ(), g.getHomeYaw(), g.getHomePitch());
        } else {
            player.teleportTo(targetLevel, g.getHomeX(), g.getHomeY(), g.getHomeZ(), g.getHomeYaw(), g.getHomePitch());
        }
        send(player, "§aTeleported to §f" + g.getName() + "§a's home.");
    }

    // ── Chat handlers ─────────────────────────────────────────────────────────

    public static void handleGuildChat(ServerPlayer player, GuildManager mgr, String message) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        String formatted = "§2[Guild] §a" + player.getName().getString() + "§7: §f" + message;
        Component msg = Component.literal(formatted);
        for (UUID uuid : g.getMembers()) {
            ServerPlayer m = player.getServer().getPlayerList().getPlayer(uuid);
            if (m != null) m.sendSystemMessage(msg);
        }
    }

    public static void handleAllyChat(ServerPlayer player, GuildManager mgr, String message) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        String formatted = "§3[Ally] §b" + player.getName().getString() + " §7[" + g.getName() + "]§7: §f" + message;
        Component msg = Component.literal(formatted);
        // Send to own guild
        for (UUID uuid : g.getMembers()) {
            ServerPlayer m = player.getServer().getPlayerList().getPlayer(uuid);
            if (m != null) m.sendSystemMessage(msg);
        }
        // Send to allied guilds
        for (UUID allyId : g.getAllies()) {
            mgr.getGuildById(allyId).ifPresent(ally -> {
                for (UUID uuid : ally.getMembers()) {
                    ServerPlayer m = player.getServer().getPlayerList().getPlayer(uuid);
                    if (m != null) m.sendSystemMessage(msg);
                }
            });
        }
    }

    // ── Alliance handlers ─────────────────────────────────────────────────────

    public static void handleAllyRequest(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can send alliance requests.");
            return;
        }
        String result = mgr.sendAllyRequest(g.getId(), targetName);
        switch (result) {
            case "sent" -> {
                send(player, "§aAlliance request sent to §f" + targetName + "§a.");
                mgr.getGuildByName(targetName).ifPresent(target -> {
                    ServerPlayer to = player.getServer().getPlayerList().getPlayer(target.getOwner());
                    if (to != null) to.sendSystemMessage(Component.literal("§6[Guild] §f" + g.getName() +
                            " §6has sent your guild an alliance request. Use §e/guilds ally accept " + g.getName()));
                    syncToGuild(target, player.getServer(), mgr);
                });
                syncToGuild(g, player.getServer(), mgr);
            }
            case "accepted" -> {
                send(player, "§aAlliance formed with §f" + targetName + "§a!");
                mgr.getGuildByName(targetName).ifPresent(t -> syncToGuild(t, player.getServer(), mgr));
                syncToGuild(g, player.getServer(), mgr);
            }
            case "already_ally" -> send(player, "§cAlready allied with §f" + targetName + "§c.");
            case "already_sent" -> send(player, "§cRequest already pending.");
            case "self" -> send(player, "§cYou can't ally with your own guild.");
            case "guild_not_found" -> send(player, "§cGuild '§f" + targetName + "§c' not found.");
        }
    }

    public static void handleAllyAccept(ServerPlayer player, GuildManager mgr, String senderName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can accept alliance requests.");
            return;
        }
        String result = mgr.acceptAllyRequest(g.getId(), senderName);
        switch (result) {
            case "accepted" -> {
                send(player, "§aAlliance formed with §f" + senderName + "§a!");
                mgr.getGuildByName(senderName).ifPresent(s -> {
                    syncToGuild(s, player.getServer(), mgr);
                    ServerPlayer so = player.getServer().getPlayerList().getPlayer(s.getOwner());
                    if (so != null) so.sendSystemMessage(
                            Component.literal("§a[Guild] §f" + g.getName() + " §aaccepted your alliance request!"));
                });
                syncToGuild(g, player.getServer(), mgr);
            }
            case "no_request" -> send(player, "§cNo pending request from §f" + senderName + "§c.");
            case "guild_not_found" -> send(player, "§cGuild '§f" + senderName + "§c' not found.");
        }
    }

    public static void handleAllyDecline(ServerPlayer player, GuildManager mgr, String senderName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can decline alliance requests.");
            return;
        }
        String result = mgr.declineAllyRequest(g.getId(), senderName);
        switch (result) {
            case "declined" -> {
                send(player, "§7Declined request from §f" + senderName + "§7.");
                mgr.getGuildByName(senderName).ifPresent(s -> syncToGuild(s, player.getServer(), mgr));
                syncToGuild(g, player.getServer(), mgr);
            }
            case "no_request" -> send(player, "§cNo pending request from §f" + senderName + "§c.");
            case "guild_not_found" -> send(player, "§cGuild '§f" + senderName + "§c' not found.");
        }
    }

    public static void handleAllyBreak(ServerPlayer player, GuildManager mgr, String targetName) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (!g.hasRank(player.getUUID(), GuildRank.OFFICER)) {
            send(player, "§cOfficers and above can break alliances.");
            return;
        }
        String result = mgr.breakAlliance(g.getId(), targetName);
        switch (result) {
            case "broken" -> {
                send(player, "§cBroke alliance with §f" + targetName + "§c.");
                mgr.getGuildByName(targetName).ifPresent(t -> {
                    syncToGuild(t, player.getServer(), mgr);
                    ServerPlayer to = player.getServer().getPlayerList().getPlayer(t.getOwner());
                    if (to != null) to.sendSystemMessage(
                            Component.literal("§c[Guild] §f" + g.getName() + " §cbroke the alliance."));
                });
                syncToGuild(g, player.getServer(), mgr);
            }
            case "not_ally" -> send(player, "§cNot allied with §f" + targetName + "§c.");
            case "guild_not_found" -> send(player, "§cGuild '§f" + targetName + "§c' not found.");
        }
    }

    // ── Wiki handlers ─────────────────────────────────────────────────────────

    public static void handleWikiSet(ServerPlayer player, GuildManager mgr, String arg) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        int sep = arg.indexOf('');
        if (sep < 0) {
            send(player, "§cInvalid wiki data.");
            return;
        }
        String title = arg.substring(0, sep).trim();
        String content = arg.substring(sep + 1);
        String result = mgr.setWikiPage(opt.get().getId(), player.getUUID(), title, content);
        switch (result) {
            case "ok" -> {
                send(player, "§aWiki page '§f" + title + "§a' saved.");
                opt.get().addLog(player.getName().getString() + " updated wiki page \"" + title + "\".");
                syncToGuild(opt.get(), player.getServer(), mgr);
            }
            case "no_permission" -> send(player, "§cOfficers and above can edit the wiki.");
            case "wiki_full" -> send(player, "§cWiki is full (20 pages max). Delete a page first.");
            case "empty_title" -> send(player, "§cPage title cannot be empty.");
        }
    }

    public static void handleWikiDelete(ServerPlayer player, GuildManager mgr, String title) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        String result = mgr.deleteWikiPage(opt.get().getId(), player.getUUID(), title);
        switch (result) {
            case "ok" -> {
                send(player, "§7Deleted wiki page '§f" + title + "§7'.");
                opt.get().addLog(player.getName().getString() + " deleted wiki page \"" + title + "\".");
                syncToGuild(opt.get(), player.getServer(), mgr);
            }
            case "no_permission" -> send(player, "§cOfficers and above can delete wiki pages.");
            case "not_found" -> send(player, "§cNo wiki page named '§f" + title + "§c'.");
        }
    }

    // ── Text commands ─────────────────────────────────────────────────────────

    private static void cmdInfo(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        StringBuilder sb = new StringBuilder("§6[Guild] §f" + g.getName());
        if (!g.getDescription().isBlank()) sb.append("\n§7").append(g.getDescription());
        if (!g.getMotd().isBlank()) sb.append("\n§6MOTD: §f").append(g.getMotd());
        sb.append("\n§7FF: ").append(g.isFriendlyFire() ? "§aON" : "§cOFF");
        sb.append("  Home: ").append(g.isHomeSet() ? "§aSet" : "§cNot set");
        sb.append("\n§7Members:");
        for (UUID m : g.getMembers()) {
            ServerPlayer on = player.getServer().getPlayerList().getPlayer(m);
            String nm = on != null ? on.getName().getString() : m.toString().substring(0, 8) + "…";
            sb.append("\n  ").append(g.getRank(m).display()).append(" §f").append(nm)
                    .append(on != null ? " §a(online)" : " §8(offline)");
        }
        if (!g.getAllies().isEmpty()) {
            sb.append("\n§7Allies:");
            for (UUID aid : g.getAllies())
                mgr.getGuildById(aid).ifPresent(a -> sb.append("\n  §b⚑ §f").append(a.getName()));
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    private static void cmdList(ServerPlayer player, GuildManager mgr) {
        var all = mgr.getAllGuilds();
        if (all.isEmpty()) {
            send(player, "§7No guilds exist yet.");
            return;
        }
        StringBuilder sb = new StringBuilder("§6[Guilds]");
        for (Guild g : all)
            sb.append("\n  §f").append(g.getName()).append(" §7(").append(g.getMembers().size()).append(" members)");
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    private static void cmdLog(ServerPlayer player, GuildManager mgr) {
        Optional<Guild> opt = mgr.getGuildFor(player.getUUID());
        if (opt.isEmpty()) {
            send(player, "§cYou are not in a guild.");
            return;
        }
        Guild g = opt.get();
        if (g.getLog().isEmpty()) {
            send(player, "§7No events logged yet.");
            return;
        }
        StringBuilder sb = new StringBuilder("§6[Guild Log] §f" + g.getName());
        for (Guild.LogEntry e : g.getLog()) {
            String time = new java.text.SimpleDateFormat("MM/dd HH:mm").format(new java.util.Date(e.timestamp()));
            sb.append("\n§7").append(time).append(" §f").append(e.message());
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int openGui(ServerPlayer player) {
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2COpenGuildScreenPacket());
        return 1;
    }

    private static List<ServerPlayer> onlineMembers(Guild g, MinecraftServer server) {
        return g.getMembers().stream().map(u -> server.getPlayerList().getPlayer(u)).filter(m -> m != null).toList();
    }

    private static GuildManager get(ServerPlayer player) {
        return GuildManager.get(player.getServer().overworld());
    }

    private static void send(ServerPlayer player, String msg) {
        player.sendSystemMessage(Component.literal(msg));
    }
}
