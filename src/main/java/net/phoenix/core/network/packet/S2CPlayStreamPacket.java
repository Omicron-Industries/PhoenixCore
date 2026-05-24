package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client: start a radio stream on the client.
 *
 * Like S2CPlaySoundPacket, this class contains ZERO references to Minecraft
 * client classes. RadioClientAudio (which extends AbstractTickableSoundInstance)
 * lives entirely in ClientSoundHandler which is stripped on the dedicated server.
 */
public class S2CPlayStreamPacket {

    private final String url;
    private final BlockPos pos;
    private final int range;

    public S2CPlayStreamPacket(String url, BlockPos pos, int range) {
        this.url = url;
        this.pos = pos;
        this.range = range;
    }

    // Decoder constructor
    public S2CPlayStreamPacket(FriendlyByteBuf buffer) {
        this.url = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.range = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.url);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.range);
    }

    public static void handle(S2CPlayStreamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            // FIXED: Passed msg.range down to match our manual attenuation signatures
            net.phoenix.core.network.client.ClientSoundHandler.playStream(msg.url, msg.pos, (float) msg.range);
        });
        ctx.get().setPacketHandled(true);
    }
}
