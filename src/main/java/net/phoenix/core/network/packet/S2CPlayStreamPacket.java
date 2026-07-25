package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class S2CPlayStreamPacket {

    private static final Logger LOGGER = LogManager.getLogger("VocalResonance");

    private final String url;
    private final BlockPos pos;
    private final int range;
    private final float volume;

    public S2CPlayStreamPacket(String url, BlockPos pos, int range, float volume) {
        this.url = url;
        this.pos = pos;
        this.range = range;
        this.volume = volume;
    }

    public S2CPlayStreamPacket(FriendlyByteBuf buffer) {
        this.url = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.range = buffer.readInt();
        this.volume = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.url);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.range);
        buffer.writeFloat(this.volume);
    }

    public static void handle(S2CPlayStreamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        LOGGER.info("VR S2CPlayStreamPacket received: url='{}' pos={} range={} volume={} dist={}",
                msg.url, msg.pos, msg.range, msg.volume, FMLEnvironment.dist);
        ctx.get().enqueueWork(() -> {
            LOGGER.info("VR S2CPlayStreamPacket enqueueWork executing, dist={}", FMLEnvironment.dist);
            try {
                if (!FMLEnvironment.dist.isClient()) {
                    LOGGER.warn("VR S2CPlayStreamPacket: dist is not CLIENT, skipping");
                    return;
                }
                net.phoenix.core.network.client.ClientSoundHandler.playStream(
                        msg.url, msg.pos, (float) msg.range, msg.volume);
            } catch (Throwable t) {
                LOGGER.error("VR S2CPlayStreamPacket: exception in enqueueWork", t);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
