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

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class VerdantBloomMachine extends GrowthMultiblockMachine {

    private static final int MAX_STAGE = 4;
    public static final IntList MAX_BOUNDS = IntList.of(
            1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE);

    private static final List<GrowthStage> STAGES = buildStages();

    private static List<GrowthStage> buildStages() {
        List<GrowthStage> stages = new java.util.ArrayList<>();
        for (int stage = 0; stage <= MAX_STAGE; stage++) {
            int r = 1 + stage;
            IntList bounds = IntList.of(r, r, r, r, r, r);
            List<ItemStack> cost = stage == 0 ? List.of() :
                    List.of(new ItemStack(net.minecraft.world.item.Items.EMERALD, stage * 4));
            stages.add(new GrowthStage(bounds, cost));
        }
        return stages;
    }

    public VerdantBloomMachine(BlockEntityCreationInfo holder) {
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
        IntList bounds = getGrowthStages().get(getGrowthStage()).bounds();
        int up = bounds.getInt(0);
        int left = bounds.getInt(2);
        int right = bounds.getInt(3);
        int front = bounds.getInt(4);
        int back = bounds.getInt(5);

        return List.of(
                new BlockPos(back + 1, up, 0),
                new BlockPos(-(front + 1), up, 0),
                new BlockPos(0, up, right + 1),
                new BlockPos(0, up, -(left + 1)));
    }

    @Override
    protected int getMaxTendrils() {
        return 4;
    }

    @Override
    protected int getMaxTendrilLength() {
        return 6;
    }

    private static final TendrilShape REACHING_SHOOT = TendrilShape.builder(PhoenixCore.id("verdant_reaching_shoot"))
            .repeat(5, b -> b.drift(1, 0))
            .build();

    private static final TendrilShape CURLING_FROND = TendrilShape.generated(
            PhoenixCore.id("verdant_curling_frond"), 8, i -> {
                double angle = i * 0.7;
                int x = (int) Math.round(Math.cos(angle) * 1.2);
                int z = (int) Math.round(Math.sin(angle) * 1.2);
                return new BlockPos(x, i, z);
            });

    private static boolean registered = false;

    public static void registerTendrilShapes() {
        if (registered) return;
        registered = true;

        TendrilShapeRegistry.register(REACHING_SHOOT, (machine, shape) -> 10);

        TendrilShapeRegistry.register(CURLING_FROND,
                (machine, shape) -> machine.getGrowthStage() >= MAX_STAGE ? 12 : 0);
    }
}
