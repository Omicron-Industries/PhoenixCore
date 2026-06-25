package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.S2CSyncPlayerProgressPacket;
import net.phoenix.core.network.PhoenixNetwork;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestProgressTracker {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                // Flag-disabled quests are fully inert — skip all processing
                if (node.isFlagDisabled()) continue;
                // DISABLED visibility quests are shown but cannot be completed
                if (node.getVisibility() == QuestNode.Visibility.DISABLED) continue;

                QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);

                // Reset a completed repeatable quest if its cooldown has elapsed
                if (state == QuestState.COMPLETED && node.isRepeatable()) {
                    if (canRepeatNow(node, data)) {
                        resetForRepeat(player, node, data);
                        state = data.getQuestState(node.getId(), QuestState.LOCKED);
                    }
                }

                if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;

                if (!MinecraftForge.EVENT_BUS.post(new QuestEvent.PlayerTick(player, node))) {
                    // Let polling tasks update their state (biome, structure, etc.)
                    for (QuestTask task : node.getTasks()) {
                        if (!task.isCompletedFor(player)) task.onTick(player);
                    }
                    checkAndTryComplete(player, node);
                }
            }
        });
    }

    // ── Completion check ──────────────────────────────────────────────────────

    public static void checkAndTryComplete(Player player, QuestNode node) {
        // Flag-disabled or visibility-DISABLED quests can never be completed
        if (node.isFlagDisabled() || node.getVisibility() == QuestNode.Visibility.DISABLED) return;
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
            if (state == QuestState.COMPLETED) return;

            java.util.List<QuestTask> tasks = node.getTasks();
            int minCount = node.getTaskMinCount();

            boolean complete;
            if (minCount > 0) {
                // X-of-N mode: count any completed task (optional or not)
                int done = 0;
                for (QuestTask task : tasks) if (task.isCompletedFor(player)) done++;
                complete = done >= minCount;
            } else {
                // Default: all non-optional tasks must be done
                complete = true;
                for (QuestTask task : tasks) {
                    if (!task.isOptional() && !task.isCompletedFor(player)) {
                        complete = false;
                        break;
                    }
                }
            }

            if (complete && (state == QuestState.UNLOCKED || state == QuestState.ACTIVE)) {
                changeQuestState(player, node, QuestState.COMPLETED);
            }
        });
    }

    // ── State mutation ────────────────────────────────────────────────────────

    public static void changeQuestState(Player player, QuestNode node, QuestState newState) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            QuestState oldState = data.getQuestState(node.getId(), QuestState.LOCKED);
            if (oldState == newState) return;

            data.setQuestState(node.getId(), newState);
            MinecraftForge.EVENT_BUS.post(new QuestEvent.StateChanged(player, node, oldState, newState));

            if (newState == QuestState.COMPLETED) {
                data.recordCompletion(node.getId());
                player.sendSystemMessage(Component.literal("§aChronicle: §f" + node.getTitle().getString()));
                processChildCascades(player, node);
                if (node.isShared() && player instanceof net.minecraft.server.level.ServerPlayer sp)
                    propagateSharedCompletion(sp, node);
            }

            sendProgressSync(player);
        });
    }

    // ── Shared quest team cascade ─────────────────────────────────────────────

    private static void propagateSharedCompletion(net.minecraft.server.level.ServerPlayer source, QuestNode node) {
        // Try Phoenix Guilds first (lightweight built-in teams)
        net.phoenix.core.integration.phoenix_guilds.GuildManager guildMgr =
                net.phoenix.core.integration.phoenix_guilds.GuildManager.get(source.getServer().overworld());
        var pTeam = guildMgr.getGuildFor(source.getUUID());
        if (pTeam.isPresent()) {
            for (java.util.UUID memberUUID : pTeam.get().getMembers()) {
                if (memberUUID.equals(source.getUUID())) continue;
                net.minecraft.server.level.ServerPlayer member =
                        source.getServer().getPlayerList().getPlayer(memberUUID);
                if (member == null) continue;
                member.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                    if (data.getQuestState(node.getId(), QuestState.LOCKED) != QuestState.COMPLETED)
                        changeQuestState(member, node, QuestState.COMPLETED);
                });
            }
            return;
        }

        // Try FTB Teams next (party/server teams only, not the per-player default team)
        if (dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().isManagerLoaded()) {
            var opt = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager()
                    .getTeamForPlayerID(source.getUUID());
            if (opt.isPresent()) {
                var team = opt.get();
                if (team.isPartyTeam() || team.isServerTeam()) {
                    for (net.minecraft.server.level.ServerPlayer member : team.getOnlineMembers()) {
                        if (member.getUUID().equals(source.getUUID())) continue;
                        member.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                            if (data.getQuestState(node.getId(), QuestState.LOCKED) != QuestState.COMPLETED)
                                changeQuestState(member, node, QuestState.COMPLETED);
                        });
                    }
                    return;
                }
            }
        }

        // Fallback: Minecraft scoreboard teams
        net.minecraft.world.scores.Team team = source.getTeam();
        if (team == null) return;
        net.minecraft.server.MinecraftServer server = source.getServer();
        if (server == null) return;
        for (String memberName : team.getPlayers()) {
            net.minecraft.server.level.ServerPlayer member = server.getPlayerList().getPlayerByName(memberName);
            if (member == null || member.getUUID().equals(source.getUUID())) continue;
            member.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                if (data.getQuestState(node.getId(), QuestState.LOCKED) != QuestState.COMPLETED)
                    changeQuestState(member, node, QuestState.COMPLETED);
            });
        }
    }

    // ── Prerequisite-aware cascade ────────────────────────────────────────────

    private static void processChildCascades(Player player, QuestNode completedNode) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode child : completedNode.getChildren()) {
                if (data.getQuestState(child.getId(), QuestState.LOCKED) != QuestState.LOCKED) continue;

                if (prereqsSatisfied(child, data)) {
                    changeQuestState(player, child, QuestState.UNLOCKED);

                    // Auto-complete if the player already satisfied the child quest's tasks
                    if (!child.getTasks().isEmpty()) {
                        checkAndTryComplete(player, child);
                    }
                }
            }
        });
    }

    /**
     * Checks whether a quest's prerequisites are satisfied.
     *
     * If the node has per-prereq required flags set:
     * - All REQUIRED prereqs must be COMPLETED
     * - At least {@code optionalPrereqMinCount} OPTIONAL prereqs must be COMPLETED
     * (0 = all optional must be done; -1 = none needed)
     *
     * Otherwise falls back to the legacy gate:
     * requireAllPrerequisites=true → every prereq must be COMPLETED (AND)
     * requireAllPrerequisites=false → at least one prereq must be COMPLETED (OR)
     *
     * DISABLED quests are skipped — they don't gate their children.
     */
    public static boolean prereqsSatisfied(QuestNode node, PlayerQuestData data) {
        List<QuestNode> prereqs = node.getPrerequisites();
        if (prereqs.isEmpty()) return true;

        // Check FORBIDDEN prereqs first — any completed forbidden prereq blocks unlock
        for (QuestNode p : prereqs) {
            if (node.isPrereqForbidden(p.getId())) {
                if (data.getQuestState(p.getId(), QuestState.LOCKED) == QuestState.COMPLETED) return false;
            }
        }

        // Filter out flag-disabled, forbidden, and non-blocking DISABLED prereqs from the positive gate.
        // A DISABLED prereq with disabledBlocksChildren=true is kept in the gate intentionally.
        List<QuestNode> active = new java.util.ArrayList<>();
        for (QuestNode p : prereqs) {
            if (p.isFlagDisabled()) continue;
            if (node.isPrereqForbidden(p.getId())) continue;
            if (p.getVisibility() == QuestNode.Visibility.DISABLED && !p.isDisabledBlocksChildren()) continue;
            active.add(p);
        }
        if (active.isEmpty()) return true;

        // Per-prereq flag mode
        if (node.hasPerPrereqFlags()) {
            List<QuestNode> required = new java.util.ArrayList<>();
            List<QuestNode> optional = new java.util.ArrayList<>();
            for (QuestNode p : active) {
                if (node.isPrereqRequired(p.getId())) required.add(p);
                else optional.add(p);
            }
            // All required must be done
            for (QuestNode p : required) {
                if (data.getQuestState(p.getId(), QuestState.LOCKED) != QuestState.COMPLETED) return false;
            }
            // Optional pool: check minCount
            if (!optional.isEmpty()) {
                int minCount = node.getOptionalPrereqMinCount();
                if (minCount < 0) return true; // -1 = none needed
                int doneOptional = 0;
                for (QuestNode p : optional) {
                    if (data.getQuestState(p.getId(), QuestState.LOCKED) == QuestState.COMPLETED) doneOptional++;
                }
                int need = (minCount == 0) ? optional.size() : minCount;
                return doneOptional >= need;
            }
            return true;
        }

        // Legacy AND/OR gate
        if (node.getRequireAllPrerequisites()) {
            for (QuestNode prereq : active) {
                if (data.getQuestState(prereq.getId(), QuestState.LOCKED) != QuestState.COMPLETED) return false;
            }
            return true;
        } else {
            for (QuestNode prereq : active) {
                if (data.getQuestState(prereq.getId(), QuestState.LOCKED) == QuestState.COMPLETED) return true;
            }
            return false;
        }
    }

    // ── Repeat helpers ────────────────────────────────────────────────────────

    public static boolean canRepeatNow(QuestNode node, PlayerQuestData data) {
        long last = data.getLastCompletedTime(node.getId());
        if (last == 0) return true; // never completed — always ok

        return switch (node.getRepeatMode()) {
            case NONE -> false;
            case INFINITE -> true;
            case DAILY -> !isSameDay(last, System.currentTimeMillis());
            case COOLDOWN -> System.currentTimeMillis() - last >=
                    TimeUnit.HOURS.toMillis(node.getRepeatCooldownHours());
        };
    }

    private static boolean isSameDay(long epochA, long epochB) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate a = Instant.ofEpochMilli(epochA).atZone(zone).toLocalDate();
        LocalDate b = Instant.ofEpochMilli(epochB).atZone(zone).toLocalDate();
        return a.equals(b);
    }

    private static void resetForRepeat(Player player, QuestNode node, PlayerQuestData data) {
        // Wipe all task progress so accumulator tasks (kill, craft, stat) start fresh
        for (QuestTask task : node.getTasks()) {
            data.clearTaskProgress(task.getTaskId());
        }
        // Allow claiming rewards again
        data.clearClaimedRewards(node.getId());
        data.clearChosenRewardIndex(node.getId());
        // Transition to UNLOCKED via the normal path (fires events + sends sync)
        changeQuestState(player, node, QuestState.UNLOCKED);
    }

    // ── Reward granting ───────────────────────────────────────────────────────

    /**
     * Grants all rewards for a completed quest. Safe to call from the claim button (client
     * should send a packet that triggers this server-side). Marks the rewards as claimed so
     * repeated calls are no-ops.
     */
    public static void grantRewards(ServerPlayer player, QuestNode node) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            if (data.hasClaimedRewards(node.getId())) return;
            if (MinecraftForge.EVENT_BUS.post(
                    new net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent.RewardClaimed(player, node)))
                return; // cancelled — mod vetoed the reward grant
            for (QuestReward reward : node.getRewards()) {
                reward.grant(player);
            }
            data.markRewardsClaimed(node.getId());
            sendProgressSync(player);
        });
    }

    public static QuestState getQuestState(Player player, QuestNode node) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getQuestState(node.getId(), QuestState.LOCKED))
                .orElse(QuestState.LOCKED);
    }

    // ── Client sync ───────────────────────────────────────────────────────────

    public static void sendProgressSync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                    .ifPresent(data -> PhoenixNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new S2CSyncPlayerProgressPacket(data)));
        }
    }

    /**
     * Grants a single chosen reward (for choice-group quests).
     */
    public static void grantChosenReward(ServerPlayer player, QuestNode node, int choiceIndex) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            if (data.hasClaimedRewards(node.getId())) return;
            if (choiceIndex < 0 || choiceIndex >= node.getRewards().size()) return;
            node.getRewards().get(choiceIndex).grant(player);
            data.setChosenRewardIndex(node.getId(), choiceIndex);
            data.markRewardsClaimed(node.getId());
            sendProgressSync(player);
        });
    }
}
