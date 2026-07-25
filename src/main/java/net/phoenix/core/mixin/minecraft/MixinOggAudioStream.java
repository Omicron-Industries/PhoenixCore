package net.phoenix.core.mixin.minecraft;

import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

import com.mojang.blaze3d.audio.OggAudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(OggAudioStream.class)
public abstract class MixinOggAudioStream {

    @Inject(method = "read", at = @At("RETURN"))
    private void phoenix$captureLivePCM(int size, CallbackInfoReturnable<ByteBuffer> cir) {
        if (!VocalVibrancyClient.isAnyTracking()) return;

        ByteBuffer data = cir.getReturnValue();
        if (data == null || !data.hasRemaining()) return;

        try {

            VocalVibrancyClient.onPCMBuffer(data, 44100);
        } catch (Exception e) {
            System.err.println("VocalVibrancy: PCM analysis error: " + e.getMessage());
        }
    }
}
