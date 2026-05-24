package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

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
            if (!(level.getBlockEntity(msg.pos) instanceof MetaMachineBlockEntity mbe)) return;
            if (!(mbe.getMetaMachine() instanceof ResonantJukeboxMachine jukebox)) return;

            if (msg.soundLoc.length() > 256 || msg.streamUrl.length() > 512) return;

            // 1. Update the values on the server
            jukebox.selectedLibrarySound = msg.soundLoc;
            jukebox.currentStreamUrl = msg.streamUrl;

            // 2. CRITICAL FIXES: Mark dirty and synchronize block updates
            // This marks the BlockEntity as modified so Minecraft marks it for a disk save
            mbe.setChanged();

            // This forces Minecraft to send an S2C block update packet to all tracking clients,
            // which syncs the underlying machine NBT data dynamically.
            var state = level.getBlockState(msg.pos);
            level.sendBlockUpdated(msg.pos, state, state, 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
