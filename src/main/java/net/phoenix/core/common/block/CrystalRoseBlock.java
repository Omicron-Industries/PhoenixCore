package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrystalRoseBlock extends MaterialBlock {

    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public CrystalRoseBlock(BlockBehaviour.Properties properties, TagPrefix tagPrefix, Material material) {
        super(properties.noCollission().instabreak().noOcclusion().sound(SoundType.GRASS), tagPrefix, material);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockState floorState = world.getBlockState(floorPos);

        if (floorState.is(net.minecraft.world.level.block.Blocks.WATER)) {
            return true;
        }

        if (floorState.isFaceSturdy(world, floorPos, net.minecraft.core.Direction.UP)) {
            return true;
        }

        return false;
    }
}
