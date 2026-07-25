package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;

import java.util.function.Supplier;

public class C2SSelectSoundPacket {

    private final BlockPos pos;
    private final String soundLoc;
    private final String streamUrl;

    public C2SSelectSoundPacket(BlockPos pos, String soundLoc, String streamUrl) {
        this.pos = pos;
        this.soundLoc = soundLoc;
        this.streamUrl = streamUrl;
    }

    public C2SSelectSoundPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.soundLoc = buf.readUtf();
        this.streamUrl = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.soundLoc);
        buf.writeUtf(this.streamUrl);
    }

    public static void handle(C2SSelectSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            var level = player.level();

            if (!(level.getBlockEntity(msg.pos) instanceof ResonantJukeboxMachine jukebox)) return;

            if (msg.soundLoc.length() > 256 || msg.streamUrl.length() > 512) return;

            if (!jukebox.selectedLibrarySound.equals(msg.soundLoc) || !jukebox.currentStreamUrl.equals(msg.streamUrl)) {
                jukebox.resetAcousticData();
            }

            jukebox.selectedLibrarySound = msg.soundLoc;
            jukebox.currentStreamUrl = msg.streamUrl;

            jukebox.markAsChanged();

            var state = level.getBlockState(msg.pos);
            level.sendBlockUpdated(msg.pos, state, state, 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
