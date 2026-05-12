package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import net.phoenix.core.integration.vocal_resonance.RadioClientAudio;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public class S2CPlayStreamPacket {
    private final String url;
    private final BlockPos pos;
    private final int range;

    public S2CPlayStreamPacket(String url, BlockPos pos, int range) {
        this.url = url;
        this.pos = pos;
        this.range = range;
    }

    public S2CPlayStreamPacket(FriendlyByteBuf buffer) {
        this.url = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.range = buffer.readInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.url);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.range);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            distributeToClient(url, pos, range);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void distributeToClient(String url, BlockPos pos, int range) {
        // 1. Stop the previous stream if it exists
        if (RadioClientAudio.currentInstance != null) {
            RadioClientAudio.currentInstance.stopStreaming();
        }

        // 2. If the URL is empty, we just wanted to stop the music
        if (url == null || url.isEmpty()) return;

        // 3. Start the new stream and track it
        RadioClientAudio sound = new RadioClientAudio(url, pos);
        RadioClientAudio.currentInstance = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}