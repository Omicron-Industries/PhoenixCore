package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine.TierConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Example multiblock demonstrating {@link TierAwareMultiblockMachine}.
 *
 * <h2>Formation tiers</h2>
 * <ul>
 *   <li><b>Tier 0 — Standard</b>: normally formed, no special hatches. Base speed and efficiency.</li>
 *   <li><b>Tier 1 — Aetheric</b>: a standard fluid input hatch is detected ({@code FluidHatchPartMachine}).
 *       This shows detecting a GTM built-in part via predicate.</li>
 *   <li><b>Tier 2 — Resonant</b>: an {@link AetherCatalystHatch} is present.
 *       This shows the {@link net.phoenix.core.common.machine.multiblock.api.IMultiblockTierProvider}
 *       interface on a custom part.</li>
 * </ul>
 *
 * Each tier has a different controller model (see blockstate JSON) and a different
 * recipe speed/efficiency multiplier so there's a meaningful gameplay reason to upgrade.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AetherCrucibleMachine extends TierAwareMultiblockMachine {

    public AetherCrucibleMachine(BlockEntityCreationInfo info) {
        // Passes info up, and sets maxTier to 2
        super(info, 2);

        // TIER 1 registration stays the same
        registerTierCondition(1, TierConditions.hasPartOfClass(FluidHatchPartMachine.class));
    }

    // ── Recipe modifier ───────────────────────────────────────────────────────

    /**
     * Returns a recipe modifier that scales speed and efficiency based on the active
     * formation tier. Register this via {@code .recipeModifiers(AetherCrucibleMachine::recipeModifier)}
     * in the machine definition.
     */
    /**
     * Returns a recipe modifier that scales speed and efficiency based on the active
     * formation tier. Register this via {@code .recipeModifiers(AetherCrucibleMachine::recipeModifier)}
     * in the machine definition.
     */
    public static ModifierFunction recipeModifier(com.gregtechceu.gtceu.api.machine.MetaMachine machine, com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        // 1. Safe cast to your custom machine type
        if (!(machine instanceof AetherCrucibleMachine aetherMachine)) {
            return ModifierFunction.IDENTITY;
        }

        // 2. Scale your parameters based on the active formation tier
        return switch (aetherMachine.getFormationTier()) {
            case 2 -> // Resonant — fastest + 2× parallel
                    ModifierFunction.builder()
                            .durationMultiplier(0.70)
                            .parallels(2)
                            .build();
            case 1 -> // Aetheric — slightly faster
                    ModifierFunction.builder()
                            .durationMultiplier(0.85)
                            .build();
            default -> // Standard — baseline, no bonus (identity makes no changes)
                    ModifierFunction.IDENTITY;
        };
    }

    // ── Custom hatch inner class ──────────────────────────────────────────────
    // Kept here for the example; in practice you'd put this in its own file under
    // common/machine/multiblock/part/.

    /**
     * Aether Catalyst Hatch — signals tier 2 to the controller.
     *
     * <p>In the real registration this would be a separate file with its own
     * {@code MachineDefinition} in {@code PhoenixMachines}.
     */
    public static class AetherCatalystHatch
            extends com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
            implements net.phoenix.core.common.machine.multiblock.api.IMultiblockTierProvider {

        public AetherCatalystHatch(BlockEntityCreationInfo info, int tier) {
            super(info);
        }

        @Override
        public int getFormationTier() {
            // Returning 2 tells TierAwareMultiblockMachine to switch to tier 2
            // when this hatch is present. No controller code required.
            return 2;
        }
    }
}
