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
}
