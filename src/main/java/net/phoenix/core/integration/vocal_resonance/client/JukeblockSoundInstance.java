package net.phoenix.core.integration.vocal_resonance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JukeblockSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private static final Logger LOGGER = LogManager.getLogger("VocalResonance");

    private final float maxRange;
    private final float baseVolume;
    private boolean stopped = false;
    private int debugTick = 0;

    public JukeblockSoundInstance(ResourceLocation soundId, BlockPos pos, float volume, float pitch, float range) {
        super(soundId, SoundSource.RECORDS, RandomSource.create());

        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;

        this.volume = Math.min(1.0f, volume);
        this.pitch = pitch;
        this.maxRange = range;
        this.baseVolume = volume;
        this.looping = false;
        this.delay = 0;

        this.attenuation = Attenuation.NONE;
        LOGGER.info("VR JukeblockSoundInstance created: sound={} pos={} volume={} range={}", soundId, pos, volume,
                range);
    }

    public float getMaxRange() {
        return maxRange;
    }

    @Override
    public void tick() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            stopped = true;
            return;
        }

        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        double dz = player.getZ() - this.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > maxRange) {
            this.volume = 0.0f;
        } else {

            float effectiveBase = Math.min(1.0f, baseVolume);
            this.volume = effectiveBase * (1.0f - (float) (distance / maxRange));
        }
        if (++debugTick % 40 == 0) {
            LOGGER.info("VR JukeblockSoundInstance tick#{}: dist={} maxRange={} vol={}",
                    debugTick, String.format("%.1f", distance), maxRange, String.format("%.3f", this.volume));
        }
    }

    @Override
    public boolean isStopped() {
        if (stopped) return true;
        return Minecraft.getInstance().player == null;
    }
}
