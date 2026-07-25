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

public class C2SClaimQuestRewardPacket {

    private final ResourceLocation questId;
    private final int choiceIndex; 

    public C2SClaimQuestRewardPacket(ResourceLocation questId, int choiceIndex) {
        this.questId = questId;
        this.choiceIndex = choiceIndex;
    }

    public C2SClaimQuestRewardPacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
        this.choiceIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeInt(choiceIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            QuestNode node = QuestTreeRegistry.getQuest(questId);
            if (node == null) return;

            QuestState currentState = QuestProgressTracker.getQuestState(player, node);
            if (currentState == QuestState.COMPLETED) {
                return;
            }

            boolean allDone = true;
            for (var task : node.getTasks()) {
                if (!task.isCompletedFor(player)) {
                    allDone = false;
                    break;
                }
            }
            if (!allDone) return;

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
