package net.phoenix.core.integration.phoenix_chronicles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.S2CSyncPlayerProgressPacket;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.S2CSyncQuestsPacket;
import net.phoenix.core.integration.phoenix_chronicles.tasks.BlockInteractTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.CraftItemTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.DimensionTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.KillEntityTask;
import net.phoenix.core.network.PhoenixNetwork;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChronicleEvents {

    public static MinecraftServer getCachedServer() {
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ChronicleDataLoader());
    }

    // Handles the initial server start — apply() runs before the server exists on integrated servers,
    // so getCachedServer() returns null there. ServerStartingEvent fires after datapacks are applied
    // and the server instance is guaranteed available.
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        java.nio.file.Path configDir = event.getServer().getServerDirectory().toPath()
                .resolve("config").resolve("phoenix_chronicles");
        PhoenixTaskRegistry.registerBuiltins();
        KubeJsTaskTypeLoader.load(configDir); // register KubeJS-defined task types after builtins
        PhoenixQuestFlags.invalidateCaches(); // flush file-backed flag caches before quest load
        CategoryFlagRegistry.load(configDir);
        QuestFileLoader.loadAdditiveFromDisk(configDir);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem());
        if (itemId == null) return;
        int amount = event.getCrafting().getCount();

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;

                for (QuestTask task : node.getTasks()) {
                    if (task instanceof CraftItemTask craftTask) {
                        // FIXED: Passing player instance context safely down to mutable capability layers
                        craftTask.onItemCrafted(player, itemId, amount);
                    }
                }
                // Check if this crafting event satisfied the remaining conditions for the quest
                QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;

            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (entityId == null) return;

            player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                    QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                    if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;

                    for (QuestTask task : node.getTasks()) {
                        if (task instanceof KillEntityTask killTask) {
                            // FIXED: Added player context to comply with refactored stateless parameters
                            killTask.onEntityKilled(player, entityId);
                        }
                    }
                    QuestProgressTracker.checkAndTryComplete(player, node);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        Block placed = event.getPlacedBlock().getBlock();
        handleBlockEvent(player, placed, "PLACE");
    }

    @SubscribeEvent
    public static void onBlockRightClicked(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Block clicked = event.getLevel().getBlockState(event.getPos()).getBlock();

        // Populate the per-player energy cache for EnergyStorageTask (BLOCK source).
        // Runs on both sides: client side so the quest UI can read cached values immediately,
        // server side so server-tick progress checks also work.
        net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.onBlockRightClicked(
                player, event.getLevel(), event.getPos());

        if (player.level().isClientSide) return;
        handleBlockEvent(player, clicked, "RIGHT_CLICK");
    }

    private static void handleBlockEvent(Player player, Block block, String action) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;
                boolean changed = false;
                for (QuestTask task : node.getTasks()) {
                    if (task instanceof BlockInteractTask blockTask) {
                        blockTask.onBlockEvent(player, block, action);
                        changed = true;
                    }
                }
                if (changed) QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        var dimension = event.getTo();
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;
                boolean changed = false;
                for (QuestTask task : node.getTasks()) {
                    if (task instanceof DimensionTask dimTask) {
                        dimTask.onChangedDimension(player, dimension);
                        changed = true;
                    }
                }
                if (changed) QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var questArg = com.mojang.brigadier.arguments.StringArgumentType.string();

        // ── Player-accessible subcommands (no permission required) ────────────
        dispatcher.register(Commands.literal("chronicles")

                // /chronicles status <quest>  — check your own quest state
                .then(Commands.literal("status")
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) {
                                        ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
                                        return 0;
                                    }
                                    String qStr = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "quest");
                                    net.minecraft.resources.ResourceLocation questId;
                                    try { questId = new net.minecraft.resources.ResourceLocation(qStr); }
                                    catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Invalid quest id: " + qStr)); return 0; }
                                    QuestNode node = QuestTreeRegistry.getQuest(questId);
                                    if (node == null) { ctx.getSource().sendFailure(Component.literal("Quest not found: " + qStr)); return 0; }
                                    QuestState state = sp.getCapability(
                                            net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider.PLAYER_QUESTS)
                                            .map(d -> d.getQuestState(questId, QuestState.LOCKED))
                                            .orElse(QuestState.LOCKED);
                                    String stateLabel = switch (state) {
                                        case COMPLETED -> "§aCompleted";
                                        case ACTIVE    -> "§eActive";
                                        case UNLOCKED  -> "§bAvailable";
                                        default        -> "§7Locked";
                                    };
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Quest \"" + node.getTitle().getString() + "\": " + stateLabel), false);
                                    return 1;
                                })))

                // /chronicles emergency <quest>  — get emergency items for an active quest
                .then(Commands.literal("emergency")
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) {
                                        ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
                                        return 0;
                                    }
                                    String qStr = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "quest");
                                    net.minecraft.resources.ResourceLocation questId;
                                    try { questId = new net.minecraft.resources.ResourceLocation(qStr); }
                                    catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Invalid quest id: " + qStr)); return 0; }
                                    QuestNode node = QuestTreeRegistry.getQuest(questId);
                                    if (node == null) { ctx.getSource().sendFailure(Component.literal("Quest not found: " + qStr)); return 0; }
                                    QuestState state = sp.getCapability(
                                            net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider.PLAYER_QUESTS)
                                            .map(d -> d.getQuestState(questId, QuestState.LOCKED))
                                            .orElse(QuestState.LOCKED);
                                    if (state != QuestState.ACTIVE) {
                                        ctx.getSource().sendFailure(Component.literal("Emergency items are only available while the quest is active."));
                                        return 0;
                                    }
                                    List<ItemStack> items = node.getEmergencyItems();
                                    if (items.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("This quest has no emergency items configured."));
                                        return 0;
                                    }
                                    for (ItemStack stack : items) {
                                        if (!sp.addItem(stack.copy())) sp.drop(stack.copy(), false);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aGave " + items.size() + " emergency item(s)."), false);
                                    return 1;
                                })))

                // ── Op-only subcommands (permission level 2) ──────────────────

                // /chronicles complete <quest> [<player>]
                .then(Commands.literal("complete")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> devSetState(ctx, QuestState.COMPLETED, null))
                                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(ctx -> devSetState(ctx, QuestState.COMPLETED,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"))))))

                // /chronicles unlock <quest> [<player>]
                .then(Commands.literal("unlock")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> devSetState(ctx, QuestState.UNLOCKED, null))
                                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(ctx -> devSetState(ctx, QuestState.UNLOCKED,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"))))))

                // /chronicles reset <quest> [<player>]
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> devSetState(ctx, QuestState.LOCKED, null))
                                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(ctx -> devSetState(ctx, QuestState.LOCKED,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"))))))

                // /chronicles active <quest> [<player>]
                .then(Commands.literal("active")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("quest", questArg)
                                .executes(ctx -> devSetState(ctx, QuestState.ACTIVE, null))
                                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(ctx -> devSetState(ctx, QuestState.ACTIVE,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"))))))

                // /chronicles validate  — reports load errors + common issues
                .then(Commands.literal("validate")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            List<String> errors = QuestFileLoader.LOAD_ERRORS;
                            // Also check for quests with no tasks (will auto-complete on unlock)
                            List<String> noTask = new ArrayList<>();
                            for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
                                if (n.getTasks().isEmpty()) noTask.add(n.getId().getPath());
                            }
                            int total = errors.size() + noTask.size();
                            if (total == 0) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§a✔ No issues found. " +
                                        QuestTreeRegistry.getAllQuests().size() + " quests loaded cleanly."), false);
                                return 1;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§e⚠ " + total + " issue(s) found:"), false);
                            for (String err : errors)
                                ctx.getSource().sendSuccess(() -> Component.literal("§c✗ " + err), false);
                            for (String id : noTask)
                                ctx.getSource().sendSuccess(() -> Component.literal("§7◦ '" + id + "' has no tasks — will auto-complete on unlock."), false);
                            return 1;
                        })));
    }

    private static int devSetState(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx,
                                   QuestState target,
                                   @Nullable net.minecraft.server.level.ServerPlayer explicitPlayer) {
        net.minecraft.server.level.ServerPlayer sp = explicitPlayer;
        if (sp == null) {
            if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer self)) {
                ctx.getSource().sendFailure(Component.literal("Must specify a player when running from console."));
                return 0;
            }
            sp = self;
        }
        String questArg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "quest");
        net.minecraft.resources.ResourceLocation questId;
        try {
            questId = new net.minecraft.resources.ResourceLocation(questArg);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Invalid quest ID: " + questArg));
            return 0;
        }
        QuestNode node = QuestTreeRegistry.getQuest(questId);
        if (node == null) {
            ctx.getSource().sendFailure(Component.literal("Quest not found: " + questArg));
            return 0;
        }
        final net.minecraft.server.level.ServerPlayer fsp = sp;
        fsp.getCapability(
                net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider.PLAYER_QUESTS)
                .ifPresent(data -> {
                    data.setQuestState(questId, target);
                    // Sync updated progress back to the client
                    PhoenixNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> fsp),
                            new net.phoenix.core.integration.phoenix_chronicles.network.packet.S2CSyncPlayerProgressPacket(
                                    data));
                });
        String label = target == QuestState.COMPLETED ? "§acompleted" :
                target == QuestState.ACTIVE ? "§estarted" :
                        target == QuestState.UNLOCKED ? "§bunlocked" : "§7reset";
        String name = fsp.getName().getString();
        ctx.getSource().sendSuccess(
                () -> Component.literal("Quest " + label + "§r for " + name + ": " + questArg), true);
        return 1;
    }

    /**
     * Copies quest progress to the new player entity on death/respawn.
     * Without this, every death wipes all progress because Forge creates a fresh entity.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(oldData ->
            event.getEntity().getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(newData ->
                newData.deserializeNBT(oldData.serializeNBT())
            )
        );
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.clearBlockCache(
                event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Map<ResourceLocation, QuestNode> serverQuests = QuestTreeRegistry.getAllQuests();
            PhoenixNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new S2CSyncQuestsPacket(serverQuests));
            // Send player progress so the client HUD and screens have live data
            serverPlayer.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                    .ifPresent(data -> PhoenixNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new S2CSyncPlayerProgressPacket(data)));
        }
    }
}
