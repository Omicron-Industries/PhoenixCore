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

    /**
     * @param sourceId   The OpenAL source handle from Channel.source.
     * @param soundPos   World position of the sound emitter.
     * @param efficiency Machine efficiency multiplier (1.0 = normal).
     */
    public static void applyPhysicalProperties(int sourceId, Vec3 soundPos, float efficiency) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Sanity check: sourceId 0 means the AL source wasn't allocated yet.
        if (sourceId == 0) return;

        // --- 1. Dynamic Occlusion (Wall Muffling) ---
        Vec3 playerPos = mc.player.getEyePosition();
        boolean isOccluded = mc.level.clip(new ClipContext(
                playerPos, soundPos,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player))
                .getType() == HitResult.Type.BLOCK;

        // --- 2. Calculate Modifiers ---
        float occlusionMuffle = isOccluded ? 0.6f : 1.0f;

        // Efficiency > 1.0 produces a subtle "overclocking whine" pitch shift.
        float basePitch = 1.0f + (efficiency - 1.0f) * 0.2f;
        float finalPitch = basePitch * (isOccluded ? 0.95f : 1.0f);

        float masterVolume = mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.BLOCKS);
        float finalGain = masterVolume * occlusionMuffle;

        // --- 3. Apply to OpenAL ---
        // Use the named AL10 constants instead of magic numbers.
        // AL10.AL_PITCH == 0x1003 (4099) and AL10.AL_GAIN == 0x100A (4106).
        // Using the named constants is safer across different LWJGL versions.
        AL10.alSourcef(sourceId, AL10.AL_PITCH, finalPitch);
        AL10.alSourcef(sourceId, AL10.AL_GAIN, finalGain);
    }
}
