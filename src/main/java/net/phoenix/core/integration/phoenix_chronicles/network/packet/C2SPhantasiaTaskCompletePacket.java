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

/**
 * Client → Server: a Phantasia viewer just closed and a ViewMachineTask or ViewSceneTask
 * should be marked complete for the sending player.
 *
 * Wire format:
 * ResourceLocation questId — quest that owns this task
 * ResourceLocation taskId — the specific task to mark done
 *
 * The server re-validates that the task is actually a ViewMachineTask / ViewSceneTask
 * before writing to the capability, so a malicious client can't abuse this to complete
 * arbitrary tasks.
 *
 * TO HOOK UP: Register this packet in PhoenixNetwork (alongside C2SClaimQuestRewardPacket):
 * CHANNEL.registerMessage(nextId++, C2SPhantasiaTaskCompletePacket.class,
 * C2SPhantasiaTaskCompletePacket::encode,
 * C2SPhantasiaTaskCompletePacket::new,
 * C2SPhantasiaTaskCompletePacket::handle);
 */
public class C2SPhantasiaTaskCompletePacket {

    private final ResourceLocation questId;
    private final ResourceLocation taskId;

    // ── Client-side constructor ───────────────────────────────────────────────

    public C2SPhantasiaTaskCompletePacket(ResourceLocation questId, ResourceLocation taskId) {
        this.questId = questId;
        this.taskId = taskId;
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    public C2SPhantasiaTaskCompletePacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
        this.taskId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeResourceLocation(taskId);
    }

    // ── Handle (server thread) ────────────────────────────────────────────────

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

                // Whitelist: only ViewMachineTask and ViewSceneTask may be completed this way
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
