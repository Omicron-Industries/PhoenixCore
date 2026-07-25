package net.phoenix.core.integration.phoenix_chronicles.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestProgressTracker;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewMachineTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewSceneTask;

import java.util.function.Supplier;

public class C2SPhantasiaTaskCompletePacket {

    private final ResourceLocation questId;
    private final ResourceLocation taskId;

    public C2SPhantasiaTaskCompletePacket(ResourceLocation questId, ResourceLocation taskId) {
        this.questId = questId;
        this.taskId = taskId;
    }

    public C2SPhantasiaTaskCompletePacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
        this.taskId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeResourceLocation(taskId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            QuestNode node = QuestTreeRegistry.getQuest(questId);
            if (node == null) return;

            QuestState state = QuestProgressTracker.getQuestState(player, node);
            if (state == QuestState.COMPLETED || state == QuestState.LOCKED) return;

            for (QuestTask task : node.getTasks()) {
                if (!task.getTaskId().equals(taskId)) continue;

                if (!(task instanceof ViewMachineTask) && !(task instanceof ViewSceneTask)) return;
                if (task.isCompletedFor(player)) return;

                player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS)
                        .ifPresent(data -> data.getOrCreateTaskProgress(taskId).putBoolean("completed", true));

                QuestProgressTracker.checkAndTryComplete(player, node);
                break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
