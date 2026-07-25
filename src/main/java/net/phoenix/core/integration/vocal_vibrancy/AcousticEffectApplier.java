package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.openal.AL10;

@OnlyIn(Dist.CLIENT)
public class AcousticEffectApplier {

    public static void applyPhysicalProperties(int sourceId, Vec3 soundPos, float efficiency) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (sourceId == 0) return;

        Vec3 playerPos = mc.player.getEyePosition();
        boolean isOccluded = mc.level.clip(new ClipContext(
                playerPos, soundPos,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player))
                .getType() == HitResult.Type.BLOCK;

        float occlusionMuffle = isOccluded ? 0.6f : 1.0f;

        float basePitch = 1.0f + (efficiency - 1.0f) * 0.2f;
        float finalPitch = basePitch * (isOccluded ? 0.95f : 1.0f);

        float masterVolume = mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.BLOCKS);
        float finalGain = masterVolume * occlusionMuffle;

        AL10.alSourcef(sourceId, AL10.AL_PITCH, finalPitch);
        AL10.alSourcef(sourceId, AL10.AL_GAIN, finalGain);
    }
}
