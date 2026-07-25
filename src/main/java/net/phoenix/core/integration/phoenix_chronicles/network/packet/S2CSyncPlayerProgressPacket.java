package net.phoenix.core.integration.phoenix_chronicles.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.client.QuestToastManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncPlayerProgressPacket {

    private final CompoundTag progressNbt;

    public S2CSyncPlayerProgressPacket(PlayerQuestData data) {
        this.progressNbt = data.serializeNBT();
    }

    public S2CSyncPlayerProgressPacket(FriendlyByteBuf buf) {
        this.progressNbt = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(progressNbt);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyOnClient(progressNbt)));
        ctx.get().setPacketHandled(true);
    }

    private static void applyOnClient(CompoundTag nbt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            
            Map<ResourceLocation, QuestState> oldStates = new HashMap<>();
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                oldStates.put(node.getId(), data.getQuestState(node.getId(), QuestState.LOCKED));
            }

            data.deserializeNBT(nbt);

            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                QuestState oldState = oldStates.getOrDefault(node.getId(), QuestState.LOCKED);
                QuestState newState = data.getQuestState(node.getId(), QuestState.LOCKED);
                if (oldState == newState) continue;
                if (newState == QuestState.UNLOCKED) {
                    QuestToastManager.get().push(node, QuestToastManager.ToastType.UNLOCKED);
                } else if (newState == QuestState.COMPLETED) {
                    QuestToastManager.get().push(node, QuestToastManager.ToastType.COMPLETED);
                }
            }
        });
    }
}
