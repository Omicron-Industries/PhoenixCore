package net.phoenix.core.integration.phoenix_chronicles.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestProgressTracker;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import java.util.function.Supplier;

/**
 * Client → Server packet: player clicked "Claim Rewards" on a quest.
 *
 * Wire format:
 * ResourceLocation questId
 * int choiceIndex (-1 = grant all rewards, ≥0 = chosen reward index)
 *
 * The server validates that:
 * - The quest exists
 * - All tasks are actually complete for this player
 * - Rewards have not already been claimed
 * before calling QuestProgressTracker.grantRewards / grantChosenReward.
 *
 * The client does NOT wait for a response — it closes the screen immediately
 * on click. If the server rejects the claim (e.g. tasks incomplete due to
 * desync) nothing bad happens; the player just won't receive rewards.
 */
public class C2SClaimQuestRewardPacket {

    private final ResourceLocation questId;
    private final int choiceIndex; // -1 = grant all

    // ── Client-side constructor ───────────────────────────────────────────────

    public C2SClaimQuestRewardPacket(ResourceLocation questId, int choiceIndex) {
        this.questId = questId;
        this.choiceIndex = choiceIndex;
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    public C2SClaimQuestRewardPacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
        this.choiceIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeInt(choiceIndex);
    }

    // ── Handle (server thread) ────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            QuestNode node = QuestTreeRegistry.getQuest(questId);
            if (node == null) return;

            // CRITICAL PROTECTION: Check if they've already claimed/completed it
            // (Adjust the method name below to match your QuestProgressTracker API if needed)
            QuestState currentState = QuestProgressTracker.getQuestState(player, node);
            if (currentState == QuestState.COMPLETED) {
                return;
            }

            // Server-authoritative validation: all tasks must actually be done
            boolean allDone = true;
            for (var task : node.getTasks()) {
                if (!task.isCompletedFor(player)) {
                    allDone = false;
                    break;
                }
            }
            if (!allDone) return;

            // Grant and transition state
            if (choiceIndex >= 0) {
                QuestProgressTracker.grantChosenReward(player, node, choiceIndex);
            } else {
                QuestProgressTracker.grantRewards(player, node);
            }
            QuestProgressTracker.changeQuestState(player, node, QuestState.COMPLETED);
        });
        ctx.get().setPacketHandled(true);
    }
}
