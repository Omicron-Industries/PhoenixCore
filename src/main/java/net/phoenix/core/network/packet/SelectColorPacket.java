package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

import java.util.function.Supplier;

public class SelectColorPacket {

    private final InteractionHand hand;
    private final int selectedIndex;

    public SelectColorPacket(InteractionHand hand, int selectedIndex) {
        this.hand = hand;
        this.selectedIndex = selectedIndex;
    }

    // Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(selectedIndex);
    }

    // Decoder (Static factory)
    public static SelectColorPacket decode(FriendlyByteBuf buf) {
        return new SelectColorPacket(buf.readEnum(InteractionHand.class), buf.readVarInt());
    }

    // Logic Handler
    public static void handle(SelectColorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(msg.hand);
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                DyeColor[] colors = DyeColor.values();
                DyeColor selectedColor = null;

                // If index is -1, selectedColor stays null (Solvent mode)
                if (msg.selectedIndex >= 0 && msg.selectedIndex < colors.length) {
                    selectedColor = colors[msg.selectedIndex];
                }

                ChameleonSprayCanBehaviour.setColor(stack, selectedColor);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}