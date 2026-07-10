package net.phoenix.core.conflux.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;


import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.widgets.TextWidget;

import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.conflux.pipe.IConfluxDataHandler;
import net.phoenix.core.conflux.research.PlayerResearchCapability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfluxHarmonicLens extends WorkableElectricMultiblockMachine {

    private static final double BASE_EFFICIENCY = 0.70;
    private static final double MAX_BONUS       = 0.30;
    private static final double BONUS_PER_NODE  = 0.005 / 50.0;
    private static final long   INPUT_BUFFER    = 200_000L;
    private static final long   OUTPUT_BUFFER   = 200_000L;
    private static final long   EU_PER_TICK     = 524_288L;
    private static final long   THROUGHPUT      = 2_048L;
    private static final int    RESEARCH_SCAN_INTERVAL = 100;

    // Annotations removed - managed by new sync/save systems
    private ConfluxDataType inputType  = ConfluxDataType.MATERIAL;
    private ConfluxDataType outputType = ConfluxDataType.COMPUTATIONAL;
    private long inputBuffer  = 0L;
    private long outputBuffer = 0L;
    private boolean lensActive = false;
    private double currentEfficiency = BASE_EFFICIENCY;

    private LazyOptional<IConfluxDataHandler> inputCap = LazyOptional.empty();
    private int researchScanCooldown = 0;

    public ConfluxHarmonicLens(BlockEntityCreationInfo holder) {
        super(holder);
        rebuildInputCap();
        this.subscribeServerTick(() -> this.lensTick());
    }

    private void lensTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        // FIXED: Using getCapabilitiesFlat directly on MetaMachine context
        List<IRecipeHandler<?>> energyCaps = this.getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (energyCaps.isEmpty()) {
            lensActive = false;
            return;
        }

        List<com.gregtechceu.gtceu.api.capability.IEnergyContainer> containers = new ArrayList<>();
        for (IRecipeHandler<?> handler : energyCaps) {
            if (handler instanceof com.gregtechceu.gtceu.api.capability.IEnergyContainer container) {
                containers.add(container);
            }
        }

        EnergyContainerList energyList = new EnergyContainerList(containers);
        if (energyList.getEnergyStored() < EU_PER_TICK) {
            lensActive = false;
            return;
        }
        energyList.removeEnergy(EU_PER_TICK);
        lensActive = true;

        if (--researchScanCooldown <= 0) {
            currentEfficiency = BASE_EFFICIENCY + computeResearchBonus();
            researchScanCooldown = RESEARCH_SCAN_INTERVAL;
        }

        long canConvert = Math.min(inputBuffer, THROUGHPUT);
        if (canConvert > 0) {
            long converted = (long)(canConvert * currentEfficiency);
            inputBuffer -= canConvert;
            long space = OUTPUT_BUFFER - outputBuffer;
            outputBuffer += Math.min(converted, space);
            setChanged();
        }

        if (outputBuffer > 0) pushOutput();
    }

    private double computeResearchBonus() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return 0;
        double bonus = 0;
        AABB range = new AABB(getBlockPos()).inflate(16);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, range)) {
            var data = PlayerResearchCapability.get(player);
            if (data != null) bonus += data.getUnlockedCount() * BONUS_PER_NODE;
        }
        return Math.min(bonus, MAX_BONUS);
    }

    private void pushOutput() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(outputBuffer, THROUGHPUT);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            var single = be.getCapability(ConfluxDataCapability.DATA, dir.getOpposite());
            if (single.isPresent()) {
                IConfluxDataHandler h = single.orElseThrow(IllegalStateException::new);
                if (h.getDataType() == outputType) {
                    long sent = h.insert(budget);
                    outputBuffer -= sent; budget -= sent;
                }
                continue;
            }

            var multi = be.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, dir.getOpposite());
            if (multi.isPresent()) {
                long sent = multi.orElseThrow(IllegalStateException::new).insert(outputType, budget);
                outputBuffer -= sent; budget -= sent;
            }
        }
        if (budget < THROUGHPUT) setChanged();
    }

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // Fetch the stack using the active hand context
        ItemStack heldStack = player.getItemInHand(context.getHand());

        // Clean, dynamic check that supports ALL GregTech screwdrivers
        if (com.gregtechceu.gtceu.api.item.tool.GTToolType.SCREWDRIVER.is(heldStack)) {
            if (isRemote()) return InteractionResult.SUCCESS;

            ConfluxDataType[] types = ConfluxDataType.values();
            if (player.isCrouching()) {
                int next = inputType.ordinal();
                do { next = (next + 1) % types.length; } while (types[next] == outputType);
                inputType = types[next];
                inputCap.invalidate();
                rebuildInputCap();
                player.sendSystemMessage(Component.literal("Input type: ").append(inputType.displayComponent()));
            } else {
                int next = outputType.ordinal();
                do { next = (next + 1) % types.length; } while (types[next] == inputType);
                outputType = types[next];
                player.sendSystemMessage(Component.literal("Output type: ").append(outputType.displayComponent()));
            }
            setChanged();
            return InteractionResult.SUCCESS;
        }
        return super.onUseWithItem(context);
    }

    private void rebuildInputCap() {
        inputCap = LazyOptional.of(() -> new IConfluxDataHandler() {
            @Override public ConfluxDataType getDataType() { return inputType; }
            @Override public long insert(long amount) {
                long space = INPUT_BUFFER - inputBuffer;
                long taken = Math.min(amount, space);
                inputBuffer += taken;
                if (taken > 0) setChanged();
                return taken;
            }
            @Override public long extract(long amount) { return 0; }
            @Override public long getStored()   { return inputBuffer; }
            @Override public long getCapacity() { return INPUT_BUFFER; }
        });
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ConfluxDataCapability.DATA) return inputCap.cast();
        return super.getCapability(cap, side);
    }

    // FIXED: Formatted explicitly via MUI2 PanelSyncManager registries
    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;

        // Map SyncHandlers to the window session
        syncManager.syncValue("lens_active", new BooleanSyncValue(() -> this.lensActive, (v) -> this.lensActive = v));
        syncManager.syncValue("input_buffer", new LongSyncValue(() -> this.inputBuffer, (v) -> this.inputBuffer = v));
        syncManager.syncValue("output_buffer", new LongSyncValue(() -> this.outputBuffer, (v) -> this.outputBuffer = v));
        syncManager.syncValue("efficiency", new DoubleSyncValue(() -> this.currentEfficiency, (v) -> this.currentEfficiency = v));

        widgets.add(new TextWidget<>(Text.dynamic(() -> this.lensActive
                ? Component.literal("§a[TRANSMUTING]§r")
                : Component.literal("§c[OFFLINE — insufficient EU]§r"))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Input:  ")
                .append(inputType.displayComponent())
                .append(Component.literal(String.format(" (§e%,d§r / §7%,d§r)", this.inputBuffer, INPUT_BUFFER))))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Output: ")
                .append(outputType.displayComponent())
                .append(Component.literal(String.format(" (§e%,d§r / §7%,d§r)", this.outputBuffer, OUTPUT_BUFFER))))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            double bonusPct = (this.currentEfficiency - BASE_EFFICIENCY) * 100;
            return Component.literal(String.format(
                    "Efficiency: §e%.1f%%§r  (base §770%%§r + §b%.1f%%§r research bonus)",
                    this.currentEfficiency * 100, bonusPct));
        })));

        widgets.add(new TextWidget<>(Text.of(Component.literal("§7Screwdriver to cycle types. Research nearby to improve efficiency.§r"))));
        return widgets;
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        lensActive = false;
        inputCap.invalidate();
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        rebuildInputCap();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        inputCap.invalidate();
    }

    public ConfluxDataType getInputType()      { return inputType; }
    public ConfluxDataType getOutputType()     { return outputType; }
    public double getCurrentEfficiency()     { return currentEfficiency; }
    public boolean isLensActive()            { return lensActive; }
}