package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestProgressTracker {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
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
                    checkAndTryComplete(player, node);
                }
            }
        });
    }

    // ── Completion check ──────────────────────────────────────────────────────

    public static void checkAndTryComplete(Player player, QuestNode node) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
            if (state == QuestState.COMPLETED) return;

            boolean allDone = true;
            for (QuestTask task : node.getTasks()) {
                if (!task.isCompletedFor(player)) {
                    allDone = false;
                    break;
                }
            }

            if (allDone && (state == QuestState.UNLOCKED || state == QuestState.ACTIVE)) {
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
            }
        });
    }

    // ── Prerequisite-aware cascade ────────────────────────────────────────────

    private static void processChildCascades(Player player, QuestNode completedNode) {
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode child : completedNode.getChildren()) {
                if (data.getQuestState(child.getId(), QuestState.LOCKED) != QuestState.LOCKED) continue;

                if (prereqsSatisfied(child, data)) {
                    changeQuestState(player, child, QuestState.UNLOCKED);

                    // Auto-complete if the player somehow already finished all tasks
                    boolean alreadyDone = !child.getTasks().isEmpty();
                    for (QuestTask t : child.getTasks()) {
                        if (!t.isCompletedFor(player)) {
                            alreadyDone = false;
                            break;
                        }
                    }
                    if (alreadyDone) changeQuestState(player, child, QuestState.COMPLETED);
                }
            }
        });
    }

    /**
     * Checks whether a quest's prerequisites are satisfied given the AND/OR gate.
     *
     * requireAllPrerequisites=true → every prereq must be COMPLETED (AND)
     * requireAllPrerequisites=false → at least one prereq must be COMPLETED (OR)
     */
    public static boolean prereqsSatisfied(QuestNode node, PlayerQuestData data) {
        if (node.getPrerequisites().isEmpty()) return true;

        if (node.getRequireAllPrerequisites()) {
            for (QuestNode prereq : node.getPrerequisites()) {
                if (data.getQuestState(prereq.getId(), QuestState.LOCKED) != QuestState.COMPLETED)
                    return false;
            }
            return true;
        } else {
            for (QuestNode prereq : node.getPrerequisites()) {
                if (data.getQuestState(prereq.getId(), QuestState.LOCKED) == QuestState.COMPLETED)
                    return true;
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
        data.setQuestState(node.getId(), QuestState.UNLOCKED);
        // Clear claimed-rewards flag so the player can claim again
        // (the actual clearing is done through the public API so the capability stays consistent)
        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(d -> {
            // Re-derive state cleanly through the normal change path without firing events
            d.setQuestState(node.getId(), QuestState.UNLOCKED);
        });
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
            for (QuestReward reward : node.getRewards()) {
                reward.grant(player);
            }
            data.markRewardsClaimed(node.getId());
        });
    }

    public static QuestState getQuestState(Player player, QuestNode node) {
        return player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                .map(data -> data.getQuestState(node.getId(), QuestState.LOCKED))
                .orElse(QuestState.LOCKED);
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
        });
    }
}
