package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ExternalTriggerTask;

import org.jetbrains.annotations.Nullable;

public final class QuestAPI {

    private QuestAPI() {}

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

                    if (MinecraftForge.EVENT_BUS.post(new QuestEvent.ExternalEvent(player, node, triggerId, data)))
                        continue;

                    ext.onExternalEvent(player, data);
                }

                QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

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

    public static QuestState getState(Player player, String questId) {
        try {
            QuestNode node = QuestTreeRegistry.getQuest(new net.minecraft.resources.ResourceLocation(questId));
            if (node == null) return QuestState.LOCKED;
            return QuestProgressTracker.getQuestState(player, node);
        } catch (Exception e) {
            return QuestState.LOCKED;
        }
    }

    public static boolean isCompleted(Player player, String questId) {
        return getState(player, questId) == QuestState.COMPLETED;
    }
}
