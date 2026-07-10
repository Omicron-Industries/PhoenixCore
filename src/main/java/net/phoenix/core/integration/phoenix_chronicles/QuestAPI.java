package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ExternalTriggerTask;

import org.jetbrains.annotations.Nullable;

/**
 * Public inbound API for the Phoenix Chronicles quest system.
 *
 * This is the single entry point for external code (other mods, KubeJS scripts)
 * to push information into the quest system. Call these methods server-side.
 *
 * ── KubeJS server_scripts usage ───────────────────────────────────────────────
 * 
 * <pre>
 * const QuestAPI = Java.loadClass('net.phoenix.core.integration.phoenix_chronicles.QuestAPI')
 *
 * ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDeathEvent', event => {
 *   const killer = event.source.entity
 *   if (killer && event.entity.type.registryName.equals('minecraft:ender_dragon')) {
 *     QuestAPI.fireExternalEvent(killer, 'mypack:killed_dragon', null)
 *   }
 * })
 * </pre>
 *
 * ── Java mod usage ────────────────────────────────────────────────────────────
 * 
 * <pre>
 * // In your Forge event handler:
 * QuestAPI.fireExternalEvent(serverPlayer, "mymod:sun_eaten", null);
 *
 * // With data:
 * CompoundTag data = new CompoundTag();
 * data.putString("dimension", player.level().dimension().location().toString());
 * QuestAPI.fireExternalEvent(serverPlayer, "mymod:dimension_visit", data);
 * </pre>
 *
 * @see ExternalTriggerTask
 * @see QuestEvent.ExternalEvent
 */
public final class QuestAPI {

    private QuestAPI() {}

    /**
     * Signals that a custom external event occurred for a player.
     *
     * <p>
     * The quest system will check all active quests the player has for any
     * {@link ExternalTriggerTask} with a matching {@code trigger_id} and advance
     * their progress. A {@link QuestEvent.ExternalEvent} is also fired on the
     * Forge event bus so other mods can observe or cancel the signal.
     *
     * @param player    The player the event applies to. Must be server-side.
     * @param triggerId The event identifier — matches the {@code trigger_id} field
     *                  in {@code ExternalTriggerTask} SNBT. Use namespaced IDs
     *                  to avoid conflicts (e.g. {@code "mymod:sun_eaten"}).
     * @param data      Optional extra data. Passed to {@link QuestEvent.ExternalEvent}
     *                  and available to Forge event subscribers. May be {@code null}.
     */
    public static void fireExternalEvent(Player player, String triggerId, @Nullable CompoundTag data) {
        if (player == null || triggerId == null || triggerId.isBlank()) return;
        if (player.level().isClientSide()) return;

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(questData -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                if (node.isFlagDisabled()) continue;
                if (node.getVisibility() == QuestNode.Visibility.DISABLED) continue;

                QuestState state = questData.getQuestState(node.getId(), QuestState.LOCKED);
                if (state != QuestState.ACTIVE && state != QuestState.UNLOCKED) continue;

                for (QuestTask task : node.getTasks()) {
                    if (!(task instanceof ExternalTriggerTask ext)) continue;
                    if (!triggerId.equals(ext.getTriggerId())) continue;
                    if (ext.isCompletedFor(player)) continue;

                    // Fire the per-node ExternalEvent — cancellable so subscribers can veto per-quest
                    if (MinecraftForge.EVENT_BUS.post(new QuestEvent.ExternalEvent(player, node, triggerId, data)))
                        continue;

                    ext.onExternalEvent(player, data);
                }

                // Recheck completion after potentially updating task progress
                QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

    /**
     * Manually set a quest to COMPLETED for a player, bypassing task requirements.
     * Useful for admin commands or integration tests.
     *
     * @param player  The player.
     * @param questId The quest ID (e.g. {@code "phoenixcore:intro_quest"}).
     * @return {@code true} if the quest was found and transitioned to COMPLETED.
     */
    public static boolean forceComplete(Player player, String questId) {
        try {
            QuestNode node = QuestTreeRegistry.getQuest(new net.minecraft.resources.ResourceLocation(questId));
            if (node == null) return false;
            QuestProgressTracker.changeQuestState(player, node, QuestState.COMPLETED);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the current state of a quest for a player.
     *
     * @param player  The player.
     * @param questId The quest ID.
     * @return The {@link QuestState}, or {@link QuestState#LOCKED} if not found.
     */
    public static QuestState getState(Player player, String questId) {
        try {
            QuestNode node = QuestTreeRegistry.getQuest(new net.minecraft.resources.ResourceLocation(questId));
            if (node == null) return QuestState.LOCKED;
            return QuestProgressTracker.getQuestState(player, node);
        } catch (Exception e) {
            return QuestState.LOCKED;
        }
    }

    /**
     * Returns {@code true} if the player has completed the given quest.
     * Convenience wrapper around {@link #getState}.
     */
    public static boolean isCompleted(Player player, String questId) {
        return getState(player, questId) == QuestState.COMPLETED;
    }
}
