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
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.widgets.TextWidget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.conflux.pipe.ConfluxPipeBlockEntity;
import net.phoenix.core.conflux.pipe.IConfluxMultiHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfluxResonanceWeb extends WorkableElectricMultiblockMachine {

    private static final int  SCAN_INTERVAL = 100;    
    private static final int  SCAN_RADIUS   = 24;     
    private static final long RATE_PER_PIPE = 8L;     
    private static final long BUFFER_PER    = 500_000L;
    private static final long PUSH_RATE     = 8_192L;
    private static final long EU_PER_TICK   = 2_097_152L; 

    private int totalPipeCount = 0;
    private int distinctTypeCount = 0;
    private boolean webActive = false;

    private final Map<ConfluxDataType, Long> buffer = new EnumMap<>(ConfluxDataType.class);
    private int scanCooldown = 0;

    public ConfluxResonanceWeb(BlockEntityCreationInfo holder) {
        super(holder);
        for (ConfluxDataType type : ConfluxDataType.values()) {
            buffer.put(type, 0L);
        }
        this.subscribeServerTick(() -> this.webTick());
    }

    private void webTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        List<IRecipeHandler<?>> energyCaps = this.getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (energyCaps.isEmpty()) {
            webActive = false;
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
            webActive = false;
            return;
        }
        energyList.removeEnergy(EU_PER_TICK);
        webActive = true;

        if (--scanCooldown <= 0) {
            scanNetwork();
            scanCooldown = SCAN_INTERVAL;
        }

        if (totalPipeCount > 0) {
            float diversityBonus = 1f + 0.25f * distinctTypeCount;
            long rateBase = (long)(totalPipeCount * RATE_PER_PIPE * diversityBonus);

            for (ConfluxDataType type : ConfluxDataType.values()) {
                if (!type.isAvailable()) continue;
                long space = BUFFER_PER - buffer.get(type);
                if (space > 0) {
                    buffer.merge(type, Math.min(rateBase, space), Long::sum);
                }
            }

            pushAllTypes();
        }
    }

    private void scanNetwork() {
        BlockPos center = getBlockPos();
        Map<ConfluxDataType, Integer> typeCounts = new EnumMap<>(ConfluxDataType.class);
        int total = 0;

        for (BlockPos scan : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -8, -SCAN_RADIUS),
                center.offset( SCAN_RADIUS,  8,  SCAN_RADIUS))) {
            BlockEntity be = getLevel().getBlockEntity(scan);
            if (be instanceof ConfluxPipeBlockEntity pipe) {
                total++;
                typeCounts.merge(pipe.getDataType(), 1, Integer::sum);
            }
        }

        totalPipeCount  = total;
        distinctTypeCount = typeCounts.size();
        setChanged();
    }

    private void pushAllTypes() {
        BlockPos pos = getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            var multiCap = be.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, dir.getOpposite());
            if (!multiCap.isPresent()) continue;
            IConfluxMultiHandler handler = multiCap.orElseThrow(IllegalStateException::new);

            for (ConfluxDataType type : ConfluxDataType.values()) {
                long stored = buffer.get(type);
                if (stored <= 0) continue;
                long toSend = Math.min(stored, PUSH_RATE);
                long sent   = handler.insert(type, toSend);
                if (sent > 0) {
                    buffer.merge(type, -sent, Long::sum);
                    setChanged();
                }
            }
        }
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;

        syncManager.syncValue("web_active", new BooleanSyncValue(() -> this.webActive, (v) -> this.webActive = v));
        syncManager.syncValue("total_pipes", new IntSyncValue(() -> this.totalPipeCount, (v) -> this.totalPipeCount = v));
        syncManager.syncValue("distinct_types", new IntSyncValue(() -> this.distinctTypeCount, (v) -> this.distinctTypeCount = v));

        widgets.add(new TextWidget<>(Text.dynamic(() -> this.webActive
                ? Component.literal("§a[SCANNING]§r")
                : Component.literal("§c[OFFLINE — insufficient EU]§r"))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(String.format(
                "Pipes detected: §e%,d§r  |  Types active: §b%d§r/5", this.totalPipeCount, this.distinctTypeCount)))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            if (this.totalPipeCount > 0) {
                float bonus = 1f + 0.25f * this.distinctTypeCount;
                long rateEach = (long)(this.totalPipeCount * RATE_PER_PIPE * bonus);
                return Component.literal(String.format(
                        "Rate / type: §e%,d§r u/t  Diversity bonus: §6+%.0f%%§r", rateEach, (bonus - 1f) * 100));
            } else {
                return Component.literal("§7No Axiom pipes detected in range.§r");
            }
        })));

        widgets.add(new TextWidget<>(Text.of(Component.literal(String.format(
                "§7Scan radius: %d blocks  Interval: %d ticks§r", SCAN_RADIUS, SCAN_INTERVAL)))));
        return widgets;
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        scanCooldown = 0; 
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        webActive = false;
        totalPipeCount = 0;
        distinctTypeCount = 0;
    }

    public int getTotalPipeCount()   { return totalPipeCount; }
    public int getDistinctTypeCount(){ return distinctTypeCount; }
    public boolean isWebActive()     { return webActive; }
}