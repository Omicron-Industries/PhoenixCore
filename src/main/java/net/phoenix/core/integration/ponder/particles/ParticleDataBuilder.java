package net.phoenix.core.integration.ponder.particles;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

public abstract class ParticleDataBuilder<O extends ParticleDataBuilder<O, PO>, PO extends ParticleOptions> {

    protected static final Random RANDOM = new Random();

    final List<ParticleTransformation> transformations = new ArrayList<>();
    int density = 1;
    @Nullable
    Float gravity = null;
    @Nullable
    Boolean physics = null;
    @Nullable
    Boolean collision = null;
    @Nullable
    Integer color = null; // Changed from Color object to primitive int (Hex)
    @Nullable
    Float roll = null;
    @Nullable
    Float friction = null;
    @Nullable
    Float scale = null;
    @Nullable
    Integer lifetime = null;

    public O density(int density) {
        this.density = density;
        return getSelf();
    }

    /**
     * Sets the color using a Hex integer (e.g., 0xFF0000 for Red)
     */
    public O color(int color) {
        this.color = color;
        return getSelf();
    }

    public O scale(float scale) {
        this.scale = scale;
        return getSelf();
    }

    public O lifetime(int lifetime) {
        this.lifetime = lifetime;
        return getSelf();
    }

    public O motion(Vec3 motion) {
        return transformMotion((partialTicks, m) -> motion);
    }

    public O speed(Vec3 speed) {
        return transformMotion((partialTick, motion) -> new Vec3(
                RANDOM.nextGaussian() * speed.x,
                RANDOM.nextGaussian() * speed.y,
                RANDOM.nextGaussian() * speed.z));
    }

    public O withinBlockSpace() {
        return transformPosition((partialTicks, position) -> new Vec3(
                Math.floor(position.x) + RANDOM.nextFloat(),
                Math.floor(position.y) + RANDOM.nextFloat(),
                Math.floor(position.z) + RANDOM.nextFloat()));
    }

    public O area(Vec3 area) {
        return transformPosition((partialTicks, position) -> new Vec3(
                position.x + (RANDOM.nextFloat() * (area.x - position.x)),
                position.y + (RANDOM.nextFloat() * (area.y - position.y)),
                position.z + (RANDOM.nextFloat() * (area.z - position.z))));
    }

    // Helper to convert hex int to JOML Vector3f
    protected Vector3f hexToVector(int hex) {
        return new Vector3f(
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f);
    }

    public O transform(ParticleTransformation transformer) {
        transformations.add(transformer);
        return getSelf();
    }

    public O transformPosition(ParticleTransformation.Simple transformer) {
        return transform(ParticleTransformation.onlyPosition(transformer));
    }

    public O transformMotion(ParticleTransformation.Simple transformer) {
        return transform(ParticleTransformation.onlyMotion(transformer));
    }

    abstract PO createOptions();

    @SuppressWarnings("unchecked")
    protected O getSelf() {
        return (O) this;
    }

    /**
     * Standard Dust Particle Builder (Vanilla compatible)
     */
    public static class DustParticleDataBuilder
                                                extends
                                                ParticleDataBuilder<DustParticleDataBuilder, DustParticleOptionsBase> {

        final int fromColor;
        @Nullable
        final Integer toColor;

        public DustParticleDataBuilder(int fromColor, @Nullable Integer toColor) {
            this.fromColor = fromColor;
            this.toColor = toColor;
        }

        @Override
        DustParticleOptionsBase createOptions() {
            float s = scale == null ? 1.0f : scale;
            Vector3f fC = hexToVector(fromColor);

            if (toColor == null) {
                return new DustParticleOptions(fC, s);
            }

            return new DustColorTransitionOptions(fC, hexToVector(toColor), s);
        }
    }
}
