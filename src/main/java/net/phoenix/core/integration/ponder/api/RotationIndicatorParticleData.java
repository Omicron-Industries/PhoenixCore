package net.phoenix.core.integration.ponder.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

// Replace 'PARTICLE_TYPE_REGISTRY_OBJECT' with your registered particle type
public record RotationIndicatorParticleData(
                                            int color,
                                            float rotationSpeed,
                                            float radius1,
                                            float radius2,
                                            int lifetime,
                                            char axis)
        implements ParticleOptions {

    @Override
    public ParticleType<?> getType() {
        // You must link this to your ParticleType registry entry
        return null;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(color);
        buffer.writeFloat(rotationSpeed);
        buffer.writeFloat(radius1);
        buffer.writeFloat(radius2);
        buffer.writeInt(lifetime);
        buffer.writeChar(axis);
    }

    @Override
    public String writeToString() {
        return String.format("rotation_indicator %d %f %f %f %d %c", color, rotationSpeed, radius1, radius2, lifetime,
                axis);
    }
}
