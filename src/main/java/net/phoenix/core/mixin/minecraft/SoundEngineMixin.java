package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.phoenix.core.integration.vocal_vibrancy.VibrancyEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private void phoenix$onSoundPlay(SoundInstance sound, CallbackInfo ci) {
        if (sound != null) {
            VibrancyEvents.onSoundStarted(sound);
        }
    }

    // NOTE: No stop() hook here. Stopping the old instance triggers the SoundEngine
    // internally and we don't need to intercept it — VocalVibrancyClient.onSoundStopped()
    // is called directly from ClientSoundHandler.stopSoundAt() instead, which avoids
    // both the method descriptor ambiguity and the re-entrant clear-then-play race.
}
