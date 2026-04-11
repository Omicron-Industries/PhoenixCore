package net.phoenix.core.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;

import org.jetbrains.annotations.NotNull;

public class TeslaSparkParticle extends TextureSheetParticle {

    private final float noiseSeed;

    public TeslaSparkParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.noiseSeed = level.random.nextFloat() * 1234.5f;

        this.xd = (level.random.nextFloat() - 0.5) * 0.2;
        this.yd = 0.1;
        this.zd = (level.random.nextFloat() - 0.5) * 0.2;

        this.lifetime = 10 + level.random.nextInt(10);
        this.hasPhysics = false;

        // Start with Electric Purple
        this.rCol = 160 / 255f;
        this.gCol = 32 / 255f;
        this.bCol = 240 / 255f;
    }

    @Override
    public void tick() {
        super.tick();

        this.xd += Math.sin(this.age * 0.5 + noiseSeed) * 0.02;
        this.zd += Math.cos(this.age * 0.5 + noiseSeed) * 0.02;

        float ageProgress = (float) this.age / (float) this.lifetime;

        this.gCol = 0.12f + (ageProgress * 0.6f);
        this.bCol = 0.94f - (ageProgress * 0.2f);
        this.alpha = 1.0f - ageProgress;
    }

    // This is the proper way to make particles glow in the dark
    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // This is a magic number for "Full Bright" (Block 15, Sky 15)
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
