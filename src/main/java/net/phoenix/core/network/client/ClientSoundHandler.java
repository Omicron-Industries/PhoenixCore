package net.phoenix.core.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSoundHandler {

    /**
     * Handles live URL streaming initialization on the client side.
     */
    public static void playStream(String url, BlockPos pos, float range) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || url == null || url.isEmpty()) return;

        // 1. Initial distance check before spinning up network buffers
        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > range) return;

        // 2. Instantiate your custom streaming tickable sound wrapper.
        // Make sure your RadioClientAudio class has a constructor that accepts
        // the stream url, its physical world block pos, and its maximum speaker range!
        // Inside its tick() loop, it can use the exact same math to update its volume.
        //
        // Example instantiation (adjust class name/constructor signatures to match yours):
        // RadioClientAudio streamInstance = new RadioClientAudio(url, pos, range);

        // 3. Play the stream using the SoundManager
        // mc.getSoundManager().play(streamInstance);

        System.out
                .println("VocalResonance Client: Starting stream from " + url + " at " + pos + " with range " + range);
    }

    public static void playSound(BlockPos pos, ResourceLocation soundLoc, float baseVolume, float pitch, float range) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // 1. Compute exact distance from player to center of the Jukeblock
        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 2. Hard absolute cutoff check
        if (distance > range) return;

        // 3. Mathematical linear volume drop-off: Max volume at center, 0.0f at exact boundary limit
        float distanceFactor = (float) (1.0 - (distance / range));
        distanceFactor = Math.max(0.0f, Math.min(1.0f, distanceFactor)); // Clamp check

        float finalVolume = baseVolume * distanceFactor;
        if (finalVolume <= 0.0f) return;

        // 4. Safe Sound Event resolution
        Holder<SoundEvent> holder;
        if (BuiltInRegistries.SOUND_EVENT.containsKey(soundLoc)) {
            holder = BuiltInRegistries.SOUND_EVENT.getHolderOrThrow(
                    ResourceKey.create(Registries.SOUND_EVENT, soundLoc));
        } else {
            holder = Holder.direct(SoundEvent.createVariableRangeEvent(soundLoc));
        }

        // 5. Build an inline anonymous override instance to completely bypass hidden constructors
        SimpleSoundInstance customSoundInstance = new SimpleSoundInstance(
                holder.value().getLocation(),
                SoundSource.RECORDS,
                finalVolume,
                pitch,
                RandomSource.create(),
                false,
                0,
                // Instead of using a giant constructor payload, we use the basic public
                // convenience constructor and override the physical properties inline:
                SoundInstance.Attenuation.NONE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                false) {

            // Force our custom manual attenuation profiles over the engine defaults
            @Override
            public SoundInstance.Attenuation getAttenuation() {
                return SoundInstance.Attenuation.NONE;
            }
        };

        // 6. Play the sound instance natively
        mc.getSoundManager().play(customSoundInstance);
    }
}
