package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission;

import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SensorHatchPartMachine extends TieredPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SensorHatchPartMachine.class,
            TieredPartMachine.MANAGED_FIELD_HOLDER);

    protected final ConditionalSubscriptionHandler signalUpdateHandler;

    public SensorHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        this.signalUpdateHandler = new ConditionalSubscriptionHandler(
                this,
                this::updateSignal,
                this::isAttachedToFormedController);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        signalUpdateHandler.updateSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        signalUpdateHandler.unsubscribe();
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        signalUpdateHandler.updateSubscription();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        signalUpdateHandler.updateSubscription();
    }

    @Override
    public boolean canConnectRedstone(@NotNull Direction side) {
        return side == getFrontFacing().getOpposite();
    }

    public abstract int getOutputSignal(@Nullable Direction direction);

    public void updateSignal() {
        if (getLevel() != null && !getLevel().isClientSide) {
            getLevel().updateNeighborsAt(getPos(), getHolder().getSelf().getBlockState().getBlock());
        }
    }

    public @Nullable IMultiController getController() {
        if (getControllers().isEmpty()) return null;
        var controller = getControllers().iterator().next();
        return (controller != null && controller.isFormed()) ? controller : null;
    }

    private boolean isAttachedToFormedController() {
        return getController() != null;
    }
}
