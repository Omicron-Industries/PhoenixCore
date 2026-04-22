package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C → S: player clicked "Generate Java" in the Recipe Builder.
 * Server echoes code into chat line-by-line for in-game visibility.
 * Client also copies directly to clipboard (no round-trip needed for that).
 *
 * Register in PhoenixNetwork.init():
 * CHANNEL.registerMessage(id++,
 * PacketRecipeBuilderGenerate.class,
 * PacketRecipeBuilderGenerate::encode,
 * PacketRecipeBuilderGenerate::decode,
 * PacketRecipeBuilderGenerate::handle,
 * Optional.of(NetworkDirection.PLAY_TO_SERVER));
 */
public class PacketRecipeBuilderGenerate {

    private final String code;

    public PacketRecipeBuilderGenerate(String code) {
        this.code = code;
    }

    // ── Encode / Decode (matches the pattern of your other packets) ───────────
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
