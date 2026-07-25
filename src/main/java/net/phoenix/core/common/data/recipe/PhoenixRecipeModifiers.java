package net.phoenix.core.common.data.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PhoenixRecipeModifiers {

    public static final RecipeModifier HEAT_DRAWN = PhoenixRecipeModifiers::heatDrawnSpeedBoost;
    private static final int plasmaToConsume = 100;

    private static boolean isFluidHeatedPlasma(FluidStack fluid) {
        return fluid.getFluid().equals(GTMaterials.Argon.getFluid(FluidStorageKeys.PLASMA));
    }

    public static @NotNull ModifierFunction heatDrawnSpeedBoost(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine workableMachine)) return ModifierFunction.IDENTITY;

        List<Fluid> recipeFluids = new ArrayList();

        for (var content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            if (content.content() instanceof FluidIngredient fluidIngredient) {
                for (var stack : fluidIngredient.getStacks()) {
                    recipeFluids.add(stack.getFluid());
                }
            }
        }

        var fluidHandlers = workableMachine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP);
        for (var fluidHandler : fluidHandlers) {

            if (!(fluidHandler instanceof NotifiableFluidTank fluidTank)) continue;

            for (int i = 0; i < fluidTank.getTanks(); i++) {
                var fluidStack = fluidTank.getFluidInTank(i);
                if (recipeFluids.contains(fluidStack.getFluid())) continue;
                if (isFluidHeatedPlasma(fluidStack)) {
                    FluidStack drained = fluidTank.drainInternal(plasmaToConsume, IFluidHandler.FluidAction.SIMULATE);
                    if (drained.getAmount() == plasmaToConsume) {
                        return (newRecipe) -> {
                            GTRecipe modifiedRecipe = ModifierFunction.builder()
                                    .durationMultiplier(0.8)
                                    .eutMultiplier(0.8)
                                    .build().apply(newRecipe.copy());
                            var inputFluids = modifiedRecipe.inputs.getOrDefault(FluidRecipeCapability.CAP,
                                    new ArrayList<>());
                            inputFluids.add(new Content(FluidIngredient.of(drained), ChanceLogic.getMaxChancedValue(),
                                    ChanceLogic.getMaxChancedValue()));
                            modifiedRecipe.inputs.put(FluidRecipeCapability.CAP, inputFluids);
                            return modifiedRecipe;
                        };
                    }
                }
            }
        }
        return ModifierFunction.IDENTITY;
    }
}
