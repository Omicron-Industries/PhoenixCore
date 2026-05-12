package net.phoenix.core.api.pattern;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.block.LampBlock;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionCoolerBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;
import net.phoenix.core.integration.phoenix_tesla_network.api.machine.trait.ITeslaBattery;
import net.phoenix.core.integration.phoenix_tesla_network.common.block.TeslaBatteryBlock;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.phoenix.core.integration.vocal_resonance.ISpeakerType;
import net.phoenix.core.integration.vocal_resonance.SoundHatchPartMachine;
import net.phoenix.core.integration.vocal_resonance.SpeakerBlock;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS;
import static net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine.TTB_BATTERY_HEADER;

public class PhoenixPredicates {

    public static TraceabilityPredicate teslaBatteries() {
        return new TraceabilityPredicate(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();

            for (Map.Entry<ITeslaBattery, Supplier<TeslaBatteryBlock>> entry : PhoenixAPI.TESLA_BATTERIES.entrySet()) {
                if (state.is(entry.getValue().get())) {
                    ITeslaBattery battery = entry.getKey();

                    if (battery.getTier() != -1 && battery.getCapacity().compareTo(BigInteger.ZERO) > 0) {
                        String key = TTB_BATTERY_HEADER + battery.getBatteryName();

                        TeslaTowerMachine.BatteryMatchWrapper wrapper = blockWorldState.getMatchContext()
                                .getOrCreate(key, () -> new TeslaTowerMachine.BatteryMatchWrapper(battery));

                        wrapper.increment();
                    }
                    return true;
                }
            }
            return false;
        }, () -> PhoenixAPI.TESLA_BATTERIES.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getTier()))
                .map(entry -> new BlockInfo(entry.getValue().get().defaultBlockState(), null))
                .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("gtceu.multiblock.pattern.error.batteries"));
    }

    @SafeVarargs
    public static TraceabilityPredicate lamps(BlockEntry<LampBlock>... lampEntries) {
        return new TraceabilityPredicate(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();
            for (BlockEntry<LampBlock> entry : lampEntries) {
                if (state.is(entry.get())) return true;
            }
            return false;
        }, () -> Arrays.stream(lampEntries)
                .map(entry -> new BlockInfo(entry.get().defaultBlockState(), null))
                .toArray(BlockInfo[]::new));
    }

    public static TraceabilityPredicate anyLamp() {
        List<BlockEntry<LampBlock>> all = new ArrayList<>();
        all.addAll(LAMPS.values());
        all.addAll(BORDERLESS_LAMPS.values());
        return lamps(all.toArray(BlockEntry[]::new));
    }

    private static final Map<DyeColor, TraceabilityPredicate> LAMPS_BY_COLOR = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            LAMPS_BY_COLOR.put(color, lamps(LAMPS.get(color), BORDERLESS_LAMPS.get(color)));
        }
    }

    public static TraceabilityPredicate lampsByColor(DyeColor color) {
        return LAMPS_BY_COLOR.getOrDefault(color, anyLamp());
    }

    public static TraceabilityPredicate fissionCoolers() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();

            for (Map.Entry<IFissionCoolerType, Supplier<FissionCoolerBlock>> entry : PhoenixAPI.FISSION_COOLERS
                    .entrySet()) {

                if (blockState.is(entry.getValue().get())) {

                    var type = entry.getKey();

                    List<IFissionCoolerType> componentList = blockWorldState
                            .getMatchContext()
                            .getOrPut("CoolerTypes", new ArrayList<>());
                    componentList.add(type);

                    return true;
                }
            }

            return false;
        },
                () -> PhoenixAPI.FISSION_COOLERS.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                        .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                        .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_coolers"));
    }

    public static TraceabilityPredicate fissionBlankets() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IFissionBlanketType, Supplier<FissionBlanketBlock>> entry : PhoenixAPI.FISSION_BLANKETS
                    .entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    List<IFissionBlanketType> componentList = blockWorldState.getMatchContext().getOrPut("BlanketTypes",
                            new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> PhoenixAPI.FISSION_BLANKETS.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                        .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                        .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_blankets"));
    }

    public static TraceabilityPredicate fissionFuelRods() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();

            for (Map.Entry<IFissionFuelRodType, Supplier<FissionFuelRodBlock>> entry : PhoenixAPI.FISSION_FUEL_RODS
                    .entrySet()) {

                if (blockState.is(entry.getValue().get())) {

                    var type = entry.getKey();

                    List<IFissionFuelRodType> componentList = blockWorldState
                            .getMatchContext()
                            .getOrPut("FuelRodTypes", new ArrayList<>());
                    componentList.add(type);

                    return true;
                }
            }

            return false;
        },
                () -> PhoenixAPI.FISSION_FUEL_RODS.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                        .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                        .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_fuel_rods"));
    }

    public static TraceabilityPredicate fissionModerators() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();

            for (Map.Entry<IFissionModeratorType, Supplier<FissionModeratorBlock>> entry : PhoenixAPI.FISSION_MODERATORS
                    .entrySet()) {

                if (blockState.is(entry.getValue().get())) {

                    var type = entry.getKey();

                    List<IFissionModeratorType> componentList = blockWorldState
                            .getMatchContext()
                            .getOrPut("ModeratorTypes", new ArrayList<>());
                    componentList.add(type);

                    return true;
                }
            }

            return false;
        },
                () -> PhoenixAPI.FISSION_MODERATORS.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                        .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                        .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_moderators"));
    }

// Inside PhoenixPredicates.java


    public static TraceabilityPredicate soundHatches() {
        return new TraceabilityPredicate(
                state -> {
                    var level = state.getWorld();
                    var pos = state.getPos();
                    if (level == null || pos == null) return false;

                    var blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof IMachineBlockEntity machineEntity &&
                            machineEntity.getMetaMachine() instanceof SoundHatchPartMachine machinePart) {

                        var context = state.getMatchContext();
                        List<SoundHatchPartMachine> hatches = context.getOrCreate("SoundHatches", ArrayList::new);
                        if (!hatches.contains(machinePart)) {
                            hatches.add(machinePart);
                        }
                        return true;
                    }
                    return false;
                },
                // FIX: Tell the multiblock which blocks are valid candidates
                () -> {
                    List<BlockInfo> candidates = new ArrayList<>();
                    // Add all tiers of your 3 hatch types to the candidate list
                    for (var machine : PhoenixMachines.DISC_HATCH) {
                        if (machine != null) {
                            candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
                        }
                    }
                    for (var machine : PhoenixMachines.LIBRARY_HATCH) {
                        if (machine != null) {
                            candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
                        }
                    }
                    for (var machine : PhoenixMachines.STREAM_HATCH) {
                        if (machine != null) {
                            candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
                        }
                    }
                    return candidates.toArray(new BlockInfo[0]);
                }
        );
    }

    public static TraceabilityPredicate tieredSpeakers() {
        return new TraceabilityPredicate(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();
            if (state.getBlock() instanceof SpeakerBlock speakerBlock) {
                ISpeakerType type = speakerBlock.getSpeakerType();

                // Increment range bonus
                blockWorldState.getMatchContext().increment("TotalSpeakerRange", type.getRangeBonus());

                // Resonance logic (increment works with integers)
                int resonanceInt = (int)(type.getResonanceAmplifier() * 100);
                blockWorldState.getMatchContext().increment("ResonancePower", resonanceInt);

                return true;
            }
            return false;
        },
                // CANDIDATES FIX: Provide all registered speaker blocks
                () -> net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_LV.get() != null ?
                        new BlockInfo[]{
                                BlockInfo.fromBlockState(net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_LV.get().defaultBlockState()),
                                BlockInfo.fromBlockState(net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_MV.get().defaultBlockState()),
                                BlockInfo.fromBlockState(net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_HV.get().defaultBlockState())
                        } : new BlockInfo[0]
        );
    }
}
