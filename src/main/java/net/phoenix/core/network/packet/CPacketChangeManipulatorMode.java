package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixManipulatorMode;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

import java.util.function.Supplier;

public class CPacketChangeManipulatorMode {

    private final int modeOrdinal;

    public CPacketChangeManipulatorMode(int modeOrdinal) {
        this.modeOrdinal = modeOrdinal;
    }

    public CPacketChangeManipulatorMode(FriendlyByteBuf buf) {
        this.modeOrdinal = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(modeOrdinal);
    }

    public static void handle(CPacketChangeManipulatorMode msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof PhoenixManipulatorItem tool) {
                tool.setMode(stack, PhoenixManipulatorMode.values()[msg.modeOrdinal], player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
