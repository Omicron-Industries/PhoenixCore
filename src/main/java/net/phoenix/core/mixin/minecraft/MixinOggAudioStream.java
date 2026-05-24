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
        ByteBuffer data = cir.getReturnValue();
        if (data == null || !data.hasRemaining()) return;

        // Duplicate gives us an independent position/limit pair safely
        ByteBuffer copy = data.duplicate();
        copy.rewind();

        if (copy.remaining() > 0) {
            // Defensive Safety: Catch any analysis parsing exceptions so audio processing
            // issues inside VocalVibrancy do not crash or mute the client sound system.
            try {
                VocalVibrancyClient.getLiveAnalyzer().processBuffer(copy, 44100);
            } catch (Exception e) {
                // Log silently or handle gracefully to ensure music still plays
                System.err.println("VocalVibrancy: Failed to analyze live PCM buffer slice");
            }
        }
    }
}
