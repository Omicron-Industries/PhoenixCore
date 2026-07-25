package net.phoenix.core.conflux.producer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.pipe.IConfluxDataHandler;
import net.phoenix.core.conflux.pipe.IConfluxMultiHandler;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class ConfluxProducerMachine extends TieredEnergyMachine {

    private static final long BASE_RATE = 16L;
    private static final int FUEL_TICKS = 200;
    private static final long BUFFER = 1024L;
    private static final long PUSH_RATE = 128L;

    protected final AxiomStorageTrait storageTrait;

    @SaveField
    @SyncToClient
    protected final NotifiableItemStackHandler fuelSlot;

    @Getter
    private final ConfluxDataType dataType;
    private final IConfluxDataHandler dataHandler;

    @Nullable
    private TickableSubscription tickSub;

    public ConfluxProducerMachine(BlockEntityCreationInfo holder, int tier, ConfluxDataType dataType) {
        super(holder, tier, NotifiableEnergyContainer.receiverContainer(
                GTValues.V[tier] * 64L,
                GTValues.V[tier],
                1L));

        this.dataType = dataType;

        AxiomStorageTrait localStorage = new AxiomStorageTrait();
        this.attachPersistentTrait("axiom_storage", localStorage);
        this.storageTrait = localStorage;

        NotifiableItemStackHandler localFuel = new NotifiableItemStackHandler(1, IO.IN, IO.NONE);
        this.attachTrait(localFuel);
        this.fuelSlot = localFuel;

        this.dataHandler = this.buildDataHandler();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tickSub = subscribeServerTick(tickSub, this::tick);
    }

    protected void tick() {
        if (getLevel() == null || isRemote()) return;

        long euPerTick = 8L * (1L << (tier - 1));
        if (energyContainer.getEnergyStored() < euPerTick) {
            storageTrait.setActive(false);
            return;
        }
        energyContainer.removeEnergy(euPerTick);

        if (storageTrait.getFuelTicks() <= 0) {
            consumeFuel();
        }

        if (storageTrait.getFuelTicks() > 0) {
            long rate = BASE_RATE * tier;
            long space = BUFFER - storageTrait.getStored();
            if (space > 0) {
                storageTrait.addStored(Math.min(rate, space));
            }
            storageTrait.decrementFuelTick();
            storageTrait.setActive(true);
        } else {
            storageTrait.setActive(false);
        }

        if (storageTrait.getStored() > 0) {
            pushOutput();
        }
    }

    protected void consumeFuel() {
        var stack = fuelSlot.getStackInSlot(0);
        if (stack.isEmpty()) return;
        fuelSlot.extractItem(0, 1, false);
        storageTrait.setFuelTicks(FUEL_TICKS);
    }

    private void pushOutput() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(storageTrait.getStored(), PUSH_RATE);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            if (be instanceof IConfluxDataHandler singleHandler && singleHandler.getDataType() == dataType) {
                long sent = singleHandler.insert(budget);
                if (sent > 0) {
                    storageTrait.addStored(-sent);
                    budget -= sent;
                }
                continue;
            }

            if (be instanceof IConfluxMultiHandler multiHandler) {
                long sent = multiHandler.insert(dataType, budget);
                if (sent > 0) {
                    storageTrait.addStored(-sent);
                    budget -= sent;
                }
            }
        }
    }

    private IConfluxDataHandler buildDataHandler() {
        return new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return dataType;
            }

            @Override
            public long insert(long amount) {
                return 0;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, storageTrait.getStored());
                if (given > 0) storageTrait.addStored(-given);
                return given;
            }

            @Override
            public long getStored() {
                return storageTrait.getStored();
            }

            @Override
            public long getCapacity() {
                return BUFFER;
            }
        };
    }

    public IConfluxDataHandler getAxiomDataHandler() {
        return this.dataHandler;
    }

    public long getStored() {
        return storageTrait.getStored();
    }

    public boolean isActive() {
        return storageTrait.isActive();
    }

    protected static class AxiomStorageTrait extends MachineTrait {

        private static final MachineTraitType<AxiomStorageTrait> TRAIT_TYPE = new MachineTraitType<>(
                AxiomStorageTrait.class, true);

        @SaveField
        @SyncToClient
        private long stored = 0L;
        @SaveField
        @SyncToClient
        private int fuelTicks = 0;
        @SaveField
        @SyncToClient
        private boolean active = false;

        public AxiomStorageTrait() {
            super();
        }

        @Override
        public MachineTraitType<?> getTraitType() {
            return TRAIT_TYPE;
        }

        public long getStored() {
            return stored;
        }

        public int getFuelTicks() {
            return fuelTicks;
        }

        public boolean isActive() {
            return active;
        }

        public void addStored(long delta) {
            stored += delta;
            getSyncDataHolder().markClientSyncFieldDirty("stored");
            markAsChanged();
        }

        public void setFuelTicks(int ticks) {
            fuelTicks = ticks;
            getSyncDataHolder().markClientSyncFieldDirty("fuelTicks");
            markAsChanged();
        }

        public void decrementFuelTick() {
            fuelTicks--;
            getSyncDataHolder().markClientSyncFieldDirty("fuelTicks");
        }

        public void setActive(boolean next) {
            if (active == next) return;
            active = next;
            getSyncDataHolder().markClientSyncFieldDirty("active");
            markAsChanged();
        }
    }
}
