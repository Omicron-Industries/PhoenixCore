package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;

import java.util.function.Supplier;

/**
 * Client → Server: real-time FFT data for sounds playing near one or more sensors.
 *
 * The packet now targets a {@code soundPos} (where the sound originated) rather than
 * a specific machine pos. {@link WorldAcousticSensor#onSoundData} fans the data out to
 * every registered sensor within range, so any machine — not just the jukebox — can
 * react to ambient world audio.
 *
 * Also carries {@code mid} and {@code treble} bands so {@link
 * net.phoenix.core.integration.vocal_resonance.ingredient.SoundIngredient} can match
 * on "high sounds" as well as bass.
 */
public class C2SSoundMetadataPacket {

    private final BlockPos soundPos;    // where the sound originated in the world
    private final float soundRange;     // how far the sound propagates (blocks)
    private final int durationTicks;    // -1 = live update only, don't reset countdown
    private final float bass;
    private final float mid;
    private final float treble;
    private final int bpm;

    public C2SSoundMetadataPacket(BlockPos soundPos, float soundRange,
                                  int durationTicks, float bass, float mid, float treble, int bpm) {
        this.soundPos = soundPos;
        this.soundRange = soundRange;
        this.durationTicks = durationTicks;
        this.bass = bass;
        this.mid = mid;
        this.treble = treble;
        this.bpm = bpm;
    }

    public C2SSoundMetadataPacket(FriendlyByteBuf buf) {
        this.soundPos = buf.readBlockPos();
        this.soundRange = buf.readFloat();
        this.durationTicks = buf.readInt();
        this.bass = buf.readFloat();
        this.mid = buf.readFloat();
        this.treble = buf.readFloat();
        this.bpm = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.soundPos);
        buf.writeFloat(this.soundRange);
        buf.writeInt(this.durationTicks);
        buf.writeFloat(this.bass);
        buf.writeFloat(this.mid);
        buf.writeFloat(this.treble);
        buf.writeInt(this.bpm);
    }

    public static void handle(C2SSoundMetadataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            // Clamp all values to prevent client exploits
            float safeBass = Math.max(0f, Math.min(msg.bass, 10f));
            float safeMid = Math.max(0f, Math.min(msg.mid, 10f));
            float safeTreble = Math.max(0f, Math.min(msg.treble, 10f));
            int safeBPM = Math.max(0, Math.min(msg.bpm, 300));
            float safeRange = Math.max(0f, Math.min(msg.soundRange, 512f));

            WorldAcousticSensor.onSoundData(
                    msg.soundPos, safeRange, msg.durationTicks,
                    safeBass, safeMid, safeTreble, safeBPM);
        });
        ctx.get().setPacketHandled(true);
    }
}
