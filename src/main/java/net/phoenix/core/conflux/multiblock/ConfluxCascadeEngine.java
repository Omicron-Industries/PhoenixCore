package net.phoenix.core.conflux.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.conflux.pipe.IConfluxDataHandler;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfluxCascadeEngine extends WorkableElectricMultiblockMachine {

    private static final int FUEL_TICKS = 400;
    private static final long BASE_RATE = 200L;
    private static final float MAX_MULT = 10f;
    private static final long EU_PER_TICK = 524_288L;
    private static final long BUFFER = 100_000L;
    private static final long PUSH_RATE = 4_096L;
    private static final float MOMENTUM_GAIN = 1f / 500f;
    private static final float MOMENTUM_DECAY = 0.05f;

    @SaveField
    private float momentum = 0f;

    @SaveField
    private int fuelTicks = 0;

    @SaveField
    private long dataBuffer = 0L;

    @SaveField
    @SyncToClient
    private ConfluxDataType outputType = ConfluxDataType.MATERIAL;

    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private boolean engineActive = false;

    @SaveField
    private final NotifiableItemStackHandler fuelSlot;

    private final LazyOptional<IConfluxDataHandler> outputCap;

    public ConfluxCascadeEngine(BlockEntityCreationInfo holder) {
        super(holder);
        this.fuelSlot = new NotifiableItemStackHandler(1, IO.IN);
        this.outputCap = LazyOptional.of(this::makeOutputHandler);
        subscribeServerTick(this::engineTick);
    }

    private void engineTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        var energy = getEnergyContainer();
        if (energy == null || energy.getEnergyStored() < EU_PER_TICK) {
            stall();
            return;
        }
        energy.removeEnergy(EU_PER_TICK);

        if (fuelTicks <= 0) {
            var stack = fuelSlot.getStackInSlot(0);
            if (!stack.isEmpty()) {
                fuelSlot.extractItem(0, 1, false);
                fuelTicks = FUEL_TICKS;
                setChanged();
            }
        }

        if (fuelTicks <= 0) {
            stall();
            return;
        }
        fuelTicks--;

        momentum = Math.min(1f, momentum + MOMENTUM_GAIN);
        engineActive = true;
        setChanged();

        float multiplier = 1f + (MAX_MULT - 1f) * momentum;
        long produced = (long) (BASE_RATE * multiplier);
        long space = BUFFER - dataBuffer;
        if (space > 0) dataBuffer += Math.min(produced, space);

        if (dataBuffer > 0) pushToNetwork();
    }

    private void stall() {
        if (momentum > 0) {
            momentum = Math.max(0f, momentum - MOMENTUM_DECAY);
            setChanged();
        }
        if (engineActive) {
            engineActive = false;
            setChanged();
        }
        if (dataBuffer > 0) pushToNetwork();
    }

    private void pushToNetwork() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(dataBuffer, PUSH_RATE);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            var singleCap = be.getCapability(ConfluxDataCapability.DATA, dir.getOpposite());
            if (singleCap.isPresent()) {
                var h = singleCap.orElseThrow(IllegalStateException::new);
                if (h.getDataType() == outputType) {
                    long sent = h.insert(budget);
                    dataBuffer -= sent;
                    budget -= sent;
                }
                continue;
            }

            var multiCap = be.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, dir.getOpposite());
            if (multiCap.isPresent()) {
                long sent = multiCap.orElseThrow(IllegalStateException::new).insert(outputType, budget);
                dataBuffer -= sent;
                budget -= sent;
            }
        }
        if (budget < PUSH_RATE) setChanged();
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext ctx) {
        var player = ctx.getPlayer();
        if (player != null && player.isCrouching() && !isRemote()) {
            ConfluxDataType[] types = ConfluxDataType.values();
            outputType = types[(outputType.ordinal() + 1) % types.length];
            player.sendSystemMessage(Component.literal("Output type: ")
                    .append(outputType.displayComponent()));
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(ctx);
    }

    private IConfluxDataHandler makeOutputHandler() {
        return new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return outputType;
            }

            @Override
            public long insert(long amount) {
                return 0;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, dataBuffer);
                dataBuffer -= given;
                return given;
            }

            @Override
            public long getStored() {
                return dataBuffer;
            }

            @Override
            public long getCapacity() {
                return BUFFER;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ConfluxDataCapability.DATA) return outputCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(String.format(
                "Momentum: §e%.1f%%§r  %s", momentum * 100f,
                engineActive ? "§a[RUNNING]§r" : "§c[STALLED]§r")))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            float mult = 1f + (MAX_MULT - 1f) * momentum;
            long rate = (long) (BASE_RATE * mult);
            return Component.literal(String.format("Output: §b%,d§r u/t × §6%.1f§rx  [%s§r]",
                    rate, mult, outputType.color + outputType.displayName));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(String.format(
                "Buffer: §e%,d§r / §7%,d§r  Fuel: §e%d§r ticks", dataBuffer, BUFFER, fuelTicks)))));
        if (momentum < 1f) {
            widgets.add(new TextWidget<>(Text.dynamic(() -> {
                int ticksToMax = (int) ((1f - momentum) / MOMENTUM_GAIN);
                return Component.literal("§7Full momentum in: " + ticksToMax + " ticks")
                        .withStyle(ChatFormatting.DARK_GRAY);
            })));
        }
        return widgets;
    }

    @Override
    public void onUnload() {
        super.onUnload();
        outputCap.invalidate();
    }

    public ConfluxDataType getOutputType() {
        return outputType;
    }

    public float getMomentum() {
        return momentum;
    }

    public boolean isEngineActive() {
        return engineActive;
    }
}
