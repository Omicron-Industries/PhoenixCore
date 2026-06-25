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
 * Client → Server: player wants to start tracking (activate) or stop tracking a quest.
 * Server validates the transition before applying it.
 *
 * Wire format:
 * ResourceLocation questId
 * boolean activate (true = UNLOCKED→ACTIVE, false = ACTIVE→UNLOCKED)
 */
public class C2SSetQuestStatePacket {

    private final ResourceLocation questId;
    private final boolean activate;

    public C2SSetQuestStatePacket(ResourceLocation questId, boolean activate) {
        this.questId = questId;
        this.activate = activate;
    }

    public C2SSetQuestStatePacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
        this.activate = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeBoolean(activate);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            QuestNode node = QuestTreeRegistry.getQuest(questId);
            if (node == null) return;

            QuestState current = QuestProgressTracker.getQuestState(player, node);
            if (activate && current == QuestState.UNLOCKED) {
                QuestProgressTracker.changeQuestState(player, node, QuestState.ACTIVE);
            } else if (!activate && current == QuestState.ACTIVE) {
                QuestProgressTracker.changeQuestState(player, node, QuestState.UNLOCKED);
            }
            // changeQuestState sends S2CSyncPlayerProgressPacket automatically
        });
        ctx.get().setPacketHandled(true);
    }
}
