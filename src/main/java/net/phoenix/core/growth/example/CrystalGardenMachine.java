package net.phoenix.core.growth.example;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.growth.GrowthMultiblockMachine;
import net.phoenix.core.growth.GrowthStage;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public class CrystalGardenMachine extends GrowthMultiblockMachine {

    private static final int MAX_STAGE = 4;

    public static final IntList MAX_BOUNDS = IntList.of(
            1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE, 1 + MAX_STAGE);

    private static final List<GrowthStage> STAGES = buildStages();

    private static List<GrowthStage> buildStages() {
        List<GrowthStage> stages = new ArrayList<>();
        for (int stage = 0; stage <= MAX_STAGE; stage++) {
            int r = 1 + stage;
            IntList bounds = IntList.of(r, r, r, r, r, r);
            List<ItemStack> cost = stage == 0 ? List.of() : List.of(new ItemStack(Items.EMERALD, stage * 4));
            stages.add(new GrowthStage(bounds, cost));
        }
        return stages;
    }

    public CrystalGardenMachine(BlockEntityCreationInfo holder) {
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
    protected float getGrowthPerRecipe() {
        return 0.1f;
    }
}
