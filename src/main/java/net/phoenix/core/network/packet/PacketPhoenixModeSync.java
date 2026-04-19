package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixManipulatorMode;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

import java.util.function.Supplier;

public class PacketPhoenixModeSync {

    private final int modeOrdinal;

    public PacketPhoenixModeSync(int modeOrdinal) {
        this.modeOrdinal = modeOrdinal;
    }

    public static void encode(PacketPhoenixModeSync msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modeOrdinal);
    }

    public static PacketPhoenixModeSync decode(FriendlyByteBuf buf) {
        return new PacketPhoenixModeSync(buf.readInt());
    }

    public static void handle(PacketPhoenixModeSync msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof PhoenixManipulatorItem tool) {
                    tool.setMode(stack, PhoenixManipulatorMode.values()[msg.modeOrdinal], player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
