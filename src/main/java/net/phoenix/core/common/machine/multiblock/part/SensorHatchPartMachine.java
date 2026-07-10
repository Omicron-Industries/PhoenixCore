package net.phoenix.core.common.machine.multiblock.part;


import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;

public class SensorHatchPartMachine extends TieredPartMachine {

    public SensorHatchPartMachine(BlockEntityCreationInfo holder, int tier) {
        super(holder, tier);
    }

    @Override
    public boolean canConnectRedstone(@NotNull Direction side) {
        return side == getFrontFacing();
    }

    // Fix: Adjusted parameter layout to match 1-argument signature from 8.0.0 controller
    @Override
    public void removedFromController(@NotNull MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        this.updateSignal();
    }

    // Fix: Adjusted parameter layout to match 2-argument signature from 8.0.0 controller
    @Override
    public void addedToController(@NotNull MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        this.updateSignal();
    }

    @Override
    public net.minecraft.world.InteractionResult onUse(com.gregtechceu.gtceu.utils.ExtendedUseOnContext context) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    // Fix: Removed .getHolder().getSelf() proxy chain chain
    public void updateSignal() {
        if (getLevel() != null) {
            getLevel().updateNeighborsAt(getBlockPos(), this.getBlockState().getBlock());
        }
    }
}