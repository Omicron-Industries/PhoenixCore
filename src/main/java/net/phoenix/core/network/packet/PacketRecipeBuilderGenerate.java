package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRecipeBuilderGenerate {

    private final String code;

    public PacketRecipeBuilderGenerate(String code) {
        this.code = code;
    }

    public static void encode(PacketRecipeBuilderGenerate msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.code, 32_768);
    }

    public static PacketRecipeBuilderGenerate decode(FriendlyByteBuf buf) {
        return new PacketRecipeBuilderGenerate(buf.readUtf(32_768));
    }

    public static void handle(PacketRecipeBuilderGenerate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            player.sendSystemMessage(Component.literal("§6=== Generated Recipe Code ==="));
            for (String line : msg.code.split("\n")) {
                player.sendSystemMessage(Component.literal("§f" + line));
            }
            player.sendSystemMessage(Component.literal("§6=============================="));
        });
        ctx.get().setPacketHandled(true);
    }
}
