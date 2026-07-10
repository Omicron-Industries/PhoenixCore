package net.phoenix.core.common.machine.multiblock.api;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

import lombok.experimental.UtilityClass;

/**
 * Shared model-property definitions for Phoenix multiblock machines.
 *
 * <p>{@link #FORMATION_TIER} is the main property used by {@link TierAwareMultiblockMachine}.
 * It drives model variant selection so the controller block can display a different texture
 * for each tier of formation.
 *
 * <p>Register it in your machine builder with:
 * <pre>{@code
 * REGISTRATE.multiblock("my_machine", MyMachine::new)
 *     .modelProperty(PhoenixMultiblockProperties.FORMATION_TIER, 0)
 *     // ... other builder calls
 *     .build();
 * }</pre>
 *
 * Then add variants to your blockstate JSON:
 * <pre>{@code
 * {
 *   "variants": {
 *     "formation_tier=0": { "model": "phoenixcore:block/my_machine_formed" },
 *     "formation_tier=1": { "model": "phoenixcore:block/my_machine_enhanced" }
 *   }
 * }
 * }</pre>
 *
 * Tiers 2–7 are available without any code changes if you need them later.
 */
@UtilityClass
public class PhoenixMultiblockProperties {

    /**
     * Integer property that drives the controller's active formation tier.
     * <ul>
     *   <li>0 — normally formed (no tier-provider parts present)</li>
     *   <li>1+ — enhanced; controlled by {@link IMultiblockTierProvider#getFormationTier()}</li>
     * </ul>
     * Range 0–7 gives 8 distinct model variants without any infrastructure changes.
     */
    public static final IntegerProperty FORMATION_TIER = IntegerProperty.create("formation_tier", 0, 7);
}
