package net.phoenix.core.conflux.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.conflux.research.ResearchNode;
import net.phoenix.core.conflux.research.ResearchTeamHelper;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.conflux.research.WorldResearchData;
import net.phoenix.core.conflux.terminal.ResearchTerminalBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SResearchUnlockPacket {

    private final ResourceLocation nodeId;
    private final BlockPos         terminalPos;

    public C2SResearchUnlockPacket(ResourceLocation nodeId, BlockPos terminalPos) {
        this.nodeId      = nodeId;
        this.terminalPos = terminalPos;
    }

    public static void encode(C2SResearchUnlockPacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.nodeId);
        buf.writeBlockPos(pkt.terminalPos);
    }

    public static C2SResearchUnlockPacket decode(FriendlyByteBuf buf) {
        return new C2SResearchUnlockPacket(buf.readResourceLocation(), buf.readBlockPos());
    }

    public static void handle(C2SResearchUnlockPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            if (!(level.getBlockEntity(pkt.terminalPos) instanceof ResearchTerminalBlockEntity terminal)) return;
            if (player.distanceToSqr(pkt.terminalPos.getX() + 0.5, pkt.terminalPos.getY() + 0.5, pkt.terminalPos.getZ() + 0.5) > 64) return;

            ResearchNode node = ResearchTreeRegistry.INSTANCE.getNode(pkt.nodeId).orElse(null);
            if (node == null) return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            boolean success = WorldResearchData.get(level).tryUnlock(teamId, node, terminal, ResearchTreeRegistry.INSTANCE);
            if (success) {
                ConfluxNetwork.syncResearchToPlayer(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
