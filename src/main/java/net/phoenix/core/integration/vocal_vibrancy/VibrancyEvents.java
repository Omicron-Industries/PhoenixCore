package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CSoundMetadataPacket;

@OnlyIn(Dist.CLIENT)
public class VibrancyEvents {

    /**
     * Called by SoundEngineMixin whenever a sound begins playing.
     *
     * Critical path: this fires for EVERY sound (footsteps, UI clicks, ambient…).
     * We must exit fast for anything not near a tracked machine.
     */
    public static void onSoundStarted(SoundInstance sound) {
        Minecraft mc = Minecraft.getInstance();

        // Guard 1: don't run on the main menu or during loading screens.
        if (mc.getConnection() == null || mc.level == null) return;

        // Guard 2: only process sounds near a machine we're actively tracking.
        // This prevents sending a C2S packet for every footstep in the world.
        BlockPos pos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
        if (!VocalVibrancyClient.isTracking(pos)) return;

        // Duration lookup — only done for relevant sounds now, so the OGG file
        // read is not a per-sound-event performance hit.
        int duration = OggMetadataProvider.getExactDurationTicks(
                mc.getResourceManager(),
                sound.getLocation());

        // Initial bass is always 1.0 — the live FFT in MixinOggAudioStream will
        // update it in real time via LiveAcousticTracker.
        float initialBass = 1.0f;

        PhoenixNetwork.CHANNEL.sendToServer(
                new S2CSoundMetadataPacket(pos, duration, initialBass));
    }
}
