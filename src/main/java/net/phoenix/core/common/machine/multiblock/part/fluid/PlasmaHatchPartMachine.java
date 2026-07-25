package net.phoenix.core.common.machine.multiblock.part.fluid;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PlasmaHatchPartMachine extends FluidHatchPartMachine {

    public PlasmaHatchPartMachine(BlockEntityCreationInfo holder, int tier, IO io, int initialCapacity, int slots) {
        super(holder, tier, io, initialCapacity, slots);
    }

    @NotNull
    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots) {
        return super.createTank(initialCapacity, slots)
                .setFilter(fluidStack -> fluidStack.getFluid().is(CustomTags.PLASMA_FLUIDS));
    }
}
