package net.phoenix.core.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.vocal_resonance.RadioClientAudio;
import net.phoenix.core.integration.vocal_resonance.client.JukeblockSoundInstance;
import net.phoenix.core.integration.vocal_vibrancy.VibrancyEvents;
import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientSoundHandler {

    private static final Map<BlockPos, SoundInstance> ACTIVE_SOUNDS = new HashMap<>();

    public static void tickActiveStreams(net.minecraft.world.entity.player.Player player) {
        for (var entry : ACTIVE_SOUNDS.entrySet()) {
            if (entry.getValue() instanceof RadioClientAudio radio) {
                radio.updateVolume(player);
            }
        }
    }

    public static void stopSoundAt(BlockPos pos) {
        SoundInstance old = ACTIVE_SOUNDS.remove(pos);
        if (old instanceof RadioClientAudio radio) {

            radio.stopStreaming();
        } else if (old != null) {
            Minecraft.getInstance().getSoundManager().stop(old);
        }
        VibrancyEvents.onSoundStopped();
        VocalVibrancyClient.stopTracking(pos);
    }

    public static void playStream(String url, BlockPos pos, float range, float volume) {
        PhoenixCore.LOGGER.info("VR playStream: url='{}' pos={} range={} volume={}", url, pos, range, volume);
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || url == null || url.isEmpty()) {
            PhoenixCore.LOGGER.warn("VR playStream: early exit — player={} urlBlank={}", player,
                    url == null || url.isEmpty());
            return;
        }

        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        PhoenixCore.LOGGER.info("VR playStream: distance={} range={}", distance, range);
        if (distance > range) {
            PhoenixCore.LOGGER.warn("VR playStream: player out of range, skipping");
            return;
        }

        stopSoundAt(pos);

        PhoenixCore.LOGGER.info("VR playStream: constructing RadioClientAudio");
        RadioClientAudio streamInstance;
        try {
            streamInstance = new RadioClientAudio(url, pos, range, volume);
        } catch (Throwable t) {
            PhoenixCore.LOGGER.error("VR playStream: RadioClientAudio constructor threw", t);
            return;
        }
        VocalVibrancyClient.startTracking(pos);
        mc.getSoundManager().play(streamInstance);
        ACTIVE_SOUNDS.put(pos, streamInstance);
        PhoenixCore.LOGGER.info("VR playStream: RadioClientAudio started for url='{}'", url);
    }

    public static void playSound(BlockPos pos, ResourceLocation soundLoc, float baseVolume, float pitch, float range) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        if (baseVolume <= 0.0f) {
            stopSoundAt(pos);
            return;
        }

        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > range) return;

        float distanceFactor = Math.max(0.0f, Math.min(1.0f, (float) (1.0 - (distance / range))));
        float finalVolume = baseVolume * distanceFactor;
        if (finalVolume <= 0.0f) return;

        stopSoundAt(pos);

        var instance = new JukeblockSoundInstance(soundLoc, pos, baseVolume, pitch, range);

        VocalVibrancyClient.startTracking(pos);
        mc.getSoundManager().play(instance);
        ACTIVE_SOUNDS.put(pos, instance);
    }
}
