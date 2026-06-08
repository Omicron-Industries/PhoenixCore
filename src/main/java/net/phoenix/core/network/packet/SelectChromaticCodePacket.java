package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

import java.util.function.Supplier;

public class SelectChromaticCodePacket {

    private final InteractionHand hand;
    private final char code;

    public SelectChromaticCodePacket(InteractionHand hand, char code) {
        this.hand = hand;
        this.code = code;
    }

    public static void encode(SelectChromaticCodePacket msg, FriendlyByteBuf buffer) {
        buffer.writeEnum(msg.hand);
        buffer.writeChar(msg.code);
    }

    public static SelectChromaticCodePacket decode(FriendlyByteBuf buffer) {
        return new SelectChromaticCodePacket(buffer.readEnum(InteractionHand.class), buffer.readChar());
    }

    public static void handle(SelectChromaticCodePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(msg.hand);
                if (stack.getItem() instanceof ChameleonSprayCanItem) {
                    ChameleonSprayCanBehaviour.setChromaticCode(stack, msg.code);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
