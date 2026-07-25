package net.phoenix.core.common.machine.multiblock.part.special;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.Direction;
import net.phoenix.core.common.machine.multiblock.Shield.ShieldTypes;
import net.phoenix.core.common.machine.multiblock.ShieldedMachine;
import net.phoenix.core.common.machine.multiblock.part.SensorHatchPartMachine;
import net.phoenix.core.common.machine.multiblock.part.ShieldRenderProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ShieldSensorHatchPartMachine extends SensorHatchPartMachine {

    public ShieldSensorHatchPartMachine(BlockEntityCreationInfo holder) {
        super(holder, GTValues.HV);
    }

    @Override
    public int getOutputSignal(@Nullable Direction direction) {
        if (direction == getFrontFacing().getOpposite()) {
            var controllers = getControllers().stream()
                    .filter(ShieldedMachine.class::isInstance)
                    .map(ShieldedMachine.class::cast)
                    .toList();

            if (controllers.isEmpty()) {
                return 0;
            } else {
                ShieldedMachine controller = controllers.get(0);
                ShieldTypes shield = controller.getShieldType();

                int signal = (int) (15.0 * shield.shieldHealth / 1000.0);

                return signal;
            }
        }
        return 0;
    }

    @Override
    public void updateSignal() {
        super.updateSignal();
        var controllers = getControllers().stream()
                .filter(ShieldedMachine.class::isInstance)
                .map(ShieldedMachine.class::cast)
                .toList();

        if (controllers.isEmpty()) {
            setRenderShieldState(ShieldTypes.INACTIVE);
        } else {
            ShieldedMachine controller = controllers.get(0);
            setRenderShieldState(controller.getShieldType());
        }
    }

    private void setRenderShieldState(ShieldTypes type) {
        var oldRenderState = getRenderState();
        ShieldRenderProperty renderProp = switch (type) {
            case NORMAL -> ShieldRenderProperty.NORMAL;
            case INACTIVE -> ShieldRenderProperty.INACTIVE;
            case DECAYED -> ShieldRenderProperty.DECAYED;
        };
        var newRenderState = oldRenderState.setValue(ShieldRenderProperty.TYPE, renderProp);

        if (!Objects.equals(oldRenderState, newRenderState)) {
            setRenderState(newRenderState);
        }
    }

    @Override
    public void addedToController(@NotNull MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        if (controller instanceof ShieldedMachine shielded) {
            setRenderShieldState(shielded.getShieldType());
        }
    }

    @Override
    public void removedFromController(@NotNull MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        setRenderShieldState(ShieldTypes.INACTIVE);
    }
}
