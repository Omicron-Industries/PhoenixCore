package net.phoenix.core.client.worldfx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

/**
 * Implement on any machine/block-entity that should alter how the world looks around it.
 *
 * <h2>Sky layers</h2>
 * Rendered into the sky sphere after vanilla sky (sun, moon, stars).
 * Use for: nebulae, custom star fields, the black hole geometry + lensing.
 * Sorted by {@link PhoenixSkyLayer#priority()} ascending (lower = further back).
 *
 * <h2>Screen effects</h2>
 * Full-screen shader passes applied after the entire world (including sky) has rendered.
 * Use for: color grading, bloom, desaturation, chromatic aberration.
 *
 * <p>Both slots are optional — return null for whichever you don't need.
 *
 * <h2>Registration</h2>
 * Call {@link WorldFXManager#register} from {@code onLoad} and
 * {@link WorldFXManager#unregister} from {@code onUnload} (client-side only).
 */
public interface IWorldFXEmitter {

    /**
     * Returns the sky layer this emitter contributes, or null.
     * Called once on registration; the returned instance is reused.
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    PhoenixSkyLayer createSkyLayer();

    /**
     * Returns the screen effect this emitter contributes, or null.
     * Called once on registration; the returned instance is reused.
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    PhoenixScreenEffect createScreenEffect();

    /**
     * How far away the player must be (in blocks) for the effect to be active.
     * Return {@code -1} to make the effect always active while the machine is loaded
     * (use this for sky-scale effects like black holes).
     */
    @OnlyIn(Dist.CLIENT)
    float getEffectRadius();
}
