package net.phoenix.core.mixin.minecraft;

import net.minecraft.world.phys.Vec3;
import net.phoenix.core.integration.vocal_vibrancy.AcousticEffectApplier;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {

    @Shadow
    @Final
    private int source;

    @Unique
    private Vec3 lastPos = Vec3.ZERO;

    @Inject(method = "setSelfPosition", at = @At("HEAD"))
    private void phoenix$capturePos(Vec3 pos, CallbackInfo ci) {
        this.lastPos = pos;
    }

    @Inject(method = "play", at = @At("HEAD"))
    private void phoenix$applyVocalVibrancy(CallbackInfo ci) {
        AcousticEffectApplier.applyPhysicalProperties(this.source, this.lastPos, 1.0f);
    }

    @Inject(method = "updateStream", at = @At("HEAD"))
    private void phoenix$liveUpdateEffects(CallbackInfo ci) {
        AcousticEffectApplier.applyPhysicalProperties(this.source, this.lastPos, 1.0f);
    }
}
