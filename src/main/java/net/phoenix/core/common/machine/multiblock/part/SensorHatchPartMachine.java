package net.phoenix.core.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.NotNull;

public class SensorHatchPartMachine extends TieredPartMachine {

    public SensorHatchPartMachine(BlockEntityCreationInfo holder, int tier) {
        super(holder, tier);
    }

    @Override
    public boolean canConnectRedstone(@NotNull Direction side) {
        return side == getFrontFacing();
    }

    @Override
    public void removedFromController(@NotNull MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        this.updateSignal();
    }

    @Override
    public void addedToController(@NotNull MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        this.updateSignal();
    }

    @Override
    public net.minecraft.world.InteractionResult onUse(com.gregtechceu.gtceu.utils.ExtendedUseOnContext context) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    public void updateSignal() {
        if (getLevel() != null) {
            getLevel().updateNeighborsAt(getBlockPos(), this.getBlockState().getBlock());
        }
    }
}
