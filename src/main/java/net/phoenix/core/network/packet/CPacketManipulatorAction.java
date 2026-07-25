package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixPlacementEngine;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

import java.util.function.Supplier;

public class CPacketManipulatorAction {

    private final BlockPos start;
    private final BlockPos end;

    public CPacketManipulatorAction(BlockPos start, BlockPos end) {
        this.start = start;
        this.end = end;
    }

    public CPacketManipulatorAction(FriendlyByteBuf buf) {
        this.start = buf.readBlockPos();
        this.end = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(start);
        buf.writeBlockPos(end);
    }

    public static void handle(CPacketManipulatorAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof PhoenixManipulatorItem tool) {

                PhoenixPlacementEngine.fillPipeArea(
                        player.level(),
                        player,
                        msg.start,
                        msg.end,
                        stack,
                        tool.getMode(stack));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
