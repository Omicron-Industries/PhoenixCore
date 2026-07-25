package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine.TierConditions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AetherCrucibleMachine extends TierAwareMultiblockMachine {

    public AetherCrucibleMachine(BlockEntityCreationInfo info) {
        super(info, 2);

        registerTierCondition(1, TierConditions.hasPartOfClass(FluidHatchPartMachine.class));
    }

    public static ModifierFunction recipeModifier(com.gregtechceu.gtceu.api.machine.MetaMachine machine,
                                                  com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (!(machine instanceof AetherCrucibleMachine aetherMachine)) {
            return ModifierFunction.IDENTITY;
        }

        return switch (aetherMachine.getFormationTier()) {
            case 2 -> ModifierFunction.builder()
                    .durationMultiplier(0.70)
                    .parallels(2)
                    .build();
            case 1 -> ModifierFunction.builder()
                    .durationMultiplier(0.85)
                    .build();
            default -> ModifierFunction.IDENTITY;
        };
    }

    public static class AetherCatalystHatch
                                            extends
                                            com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
                                            implements
                                            net.phoenix.core.common.machine.multiblock.api.IMultiblockTierProvider {

        public AetherCatalystHatch(BlockEntityCreationInfo info, int tier) {
            super(info);
        }

        @Override
        public int getFormationTier() {
            return 2;
        }
    }
}
