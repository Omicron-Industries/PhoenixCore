package net.phoenix.core.growth.example;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.growth.GrowthMultiblockMachine;
import net.phoenix.core.growth.GrowthStage;
import net.phoenix.core.growth.tendril.TendrilShape;
import net.phoenix.core.growth.tendril.TendrilShapeRegistry;
import net.phoenix.core.growth.tendril.TendrilWeightCondition;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class BloomvineCoreMachine extends GrowthMultiblockMachine {

    private static final int MAX_MATURITY = 5;
    private static final IntList CORE_BOUNDS = IntList.of(1, 1, 1, 1, 1, 1);
    private static final List<GrowthStage> STAGES = buildMaturityTicks();

    private static final List<BlockPos> ANCHORS = List.of(
            new BlockPos(2, 2, 0), new BlockPos(-2, 2, 0),
            new BlockPos(0, 2, 2), new BlockPos(0, 2, -2));

    private static List<GrowthStage> buildMaturityTicks() {
        return java.util.stream.IntStream.rangeClosed(0, MAX_MATURITY)
                .mapToObj(i -> new GrowthStage(CORE_BOUNDS, List.<ItemStack>of()))
                .toList();
    }

    public BloomvineCoreMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    @Override
    protected List<GrowthStage> getGrowthStages() {
        return STAGES;
    }

    @Override
    protected BlockState getShellBlockState() {
        return PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get().defaultBlockState();
    }

    @Override
    protected BlockState getTendrilBlockState() {
        return PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get().defaultBlockState();
    }

    @Override
    protected List<BlockPos> getTendrilAnchors() {
        return ANCHORS;
    }

    @Override
    protected int getMaxTendrils() {
        return 4;
    }

    @Override
    protected int getMaxTendrilLength() {
        return 8;
    }

    private static final TendrilShape SPIKE = TendrilShape.builder(PhoenixCore.id("bloomvine_spike"))
            .repeat(6, b -> b.up(1))
            .build();

    private static final TendrilShape CREEPER_VINE = TendrilShape
            .builder(PhoenixCore.id("bloomvine_creeper_vine"))
            .drift(1, 0).drift(1, 0).drift(1, 0)
            .drift(0, 1).drift(1, 1).drift(0, 1).drift(1, 1)
            .build();

    private static final TendrilShape SPIRAL_FROND = TendrilShape.generated(
            PhoenixCore.id("bloomvine_spiral_frond"), 10, i -> {
                double angle = i * 0.9;
                int x = (int) Math.round(Math.cos(angle) * 1.5);
                int z = (int) Math.round(Math.sin(angle) * 1.5);
                return new BlockPos(x, i, z);
            });

    private static boolean registered = false;

    public static void registerTendrilShapes() {
        if (registered) return;
        registered = true;

        TendrilShapeRegistry.register(SPIKE, TendrilWeightCondition.constant(10));

        TendrilShapeRegistry.register(CREEPER_VINE,
                (machine, shape) -> 4 + machine.getGrowthStage() * 3);

        TendrilShapeRegistry.register(SPIRAL_FROND,
                (machine, shape) -> machine.getGrowthStage() >= MAX_MATURITY ? 14 : 0);
    }
}
