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

@OnlyIn(Dist.CLIENT)
public class VibrancyEvents {

    public static void onSoundStarted(SoundInstance sound) {
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

        if (!VocalVibrancyClient.hasSensorNear(soundPos, range)) return;

        VocalVibrancyClient.onSoundStarted(soundPos, range);

        int duration = OggMetadataProvider.getExactDurationTicks(
                mc.getResourceManager(), sound.getLocation());

        PhoenixNetwork.CHANNEL.sendToServer(new C2SSoundMetadataPacket(
                soundPos, range, duration, 0f, 0f, 0f, 0));
    }

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

        return sound.getAttenuation() == SoundInstance.Attenuation.NONE ? 64f : 16f;
    }
}
