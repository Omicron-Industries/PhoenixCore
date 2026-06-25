package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.vocal_resonance.RadioClientAudio;
import net.phoenix.core.integration.vocal_resonance.client.JukeblockSoundInstance;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SSoundMetadataPacket;

/**
 * Hooks called by {@link net.phoenix.core.mixin.minecraft.SoundEngineMixin}.
 *
 * Only routes jukebox-owned sounds (JukeblockSoundInstance / RadioClientAudio)
 * to the FFT pipeline. Vanilla sounds (footsteps, UI, ambient) are ignored
 * completely so they can't pollute the analyzer or cause per-sound file reads.
 *
 * Sound-stop cleanup is NOT done here via a mixin hook — it's called directly
 * from ClientSoundHandler.stopSoundAt() to avoid a race where the old instance's
 * stop fires BEFORE the new instance's play has a chance to re-register.
 */
@OnlyIn(Dist.CLIENT)
public class VibrancyEvents {

    public static void onSoundStarted(SoundInstance sound) {
        // Only care about our own jukebox sound types — ignore everything else.
        // This prevents file reads and FFT setup for footsteps, UI, ambient, etc.
        if (!(sound instanceof JukeblockSoundInstance) && !(sound instanceof RadioClientAudio)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.level == null) return;

        BlockPos soundPos = new BlockPos(
                (int) Math.floor(sound.getX()),
                (int) Math.floor(sound.getY()),
                (int) Math.floor(sound.getZ()));

        float range = estimateRange(sound);

        // Fast exit: no sensor is registered near this sound's origin
        if (!VocalVibrancyClient.hasSensorNear(soundPos, range)) return;

        VocalVibrancyClient.onSoundStarted(soundPos, range);

        // Read OGG duration — only safe here because we already know this is a
        // JukeblockSoundInstance or RadioClientAudio, both of which are OGG-backed.
        int duration = OggMetadataProvider.getExactDurationTicks(
                mc.getResourceManager(), sound.getLocation());

        PhoenixNetwork.CHANNEL.sendToServer(new C2SSoundMetadataPacket(
                soundPos, range, duration, 0f, 0f, 0f, 0));
    }

    /**
     * Called directly from {@link net.phoenix.core.network.client.ClientSoundHandler#stopSoundAt}
     * rather than via a mixin stop hook, so cleanup happens at the right moment in the
     * stop → play sequence without a descriptor-matching risk on SoundEngine.
     */
    public static void onSoundStopped() {
        VocalVibrancyClient.onSoundStopped();
    }

    private static float estimateRange(SoundInstance sound) {
        if (sound instanceof JukeblockSoundInstance jbs) {
            return jbs.getMaxRange();
        }
        if (sound instanceof RadioClientAudio rca) {
            return rca.getMaxRange();
        }
        // Fallback for anything else that somehow gets here
        return sound.getAttenuation() == SoundInstance.Attenuation.NONE ? 64f : 16f;
    }
}
