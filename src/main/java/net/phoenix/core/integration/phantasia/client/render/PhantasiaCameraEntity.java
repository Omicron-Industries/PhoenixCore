package net.phoenix.core.integration.phantasia.client.render;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Minimal entity stub required by {@link net.minecraft.client.Camera#setup}.
 *
 * {@code Camera.setup(level, entity, thirdPerson, inverseView, partial)} only uses
 * the entity for three things:
 * 1. {@code entity.getX/Y/Z()} — position (we set via {@code setPos()})
 * 2. {@code entity.getYRot() / getXRot()} — rotation (we set via setters)
 * 3. {@code entity.getEyeHeight()} — added to Y for the eye position
 *
 * Extending {@link Entity} directly (not {@code Player} or {@code LivingEntity})
 * avoids all server-connection requirements and ability checks that would fire
 * when constructing a Player without a running integrated server.
 */
public final class PhantasiaCameraEntity extends Entity {

    public PhantasiaCameraEntity(Level world) {
        super(EntityType.PIG, world); // type is never used; PIG is a safe non-null stand-in
    }

    // Eye height is zero because we position the entity at the exact eye location
    // (syncCameraEntity subtracts getEyeHeight() before calling setPos).
    // For 1.20.2 through 1.20.4
    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose,
                              net.minecraft.world.entity.EntityDimensions dimensions) {
        return 0f;
    }

    // ── Required abstract method stubs ────────────────────────────────────────

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag tag) {}
}
