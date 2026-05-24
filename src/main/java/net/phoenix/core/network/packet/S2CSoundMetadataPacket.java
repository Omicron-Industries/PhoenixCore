package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;

import java.util.function.Supplier;

public class S2CSoundMetadataPacket {

    private final BlockPos pos;
    private final int durationTicks;
    private final float bassIntensity;

    public S2CSoundMetadataPacket(BlockPos pos, int durationTicks, float bassIntensity) {
        this.pos = pos;
        this.durationTicks = durationTicks;
        this.bassIntensity = bassIntensity;
    }

    public S2CSoundMetadataPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.durationTicks = buf.readInt();
        this.bassIntensity = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.durationTicks);
        buf.writeFloat(this.bassIntensity);
    }

    public static void handle(S2CSoundMetadataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            // Guard: getSender() can be null if the connection dropped mid-packet.
            if (player == null) return;

            // Clamp bass to a sane range — clients shouldn't be able to send
            // wildly large values that inflate EU consumption on the machine.
            float safeBass = Math.max(0.0f, Math.min(msg.bassIntensity, 10.0f));

            var level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof MetaMachineBlockEntity mbe)) return;
            if (!(mbe.getMetaMachine() instanceof ResonantJukeboxMachine jukebox)) return;

            jukebox.syncAcousticData(msg.durationTicks, safeBass);
        });
        ctx.get().setPacketHandled(true);
    }
}
