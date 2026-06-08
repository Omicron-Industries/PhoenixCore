package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CrystalRoseBlock extends MaterialBlock {

    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public CrystalRoseBlock(BlockBehaviour.Properties properties, TagPrefix tagPrefix, Material material) {
        super(properties.noCollission().instabreak().noOcclusion().sound(SoundType.GRASS), tagPrefix, material);
        if (material == null) return;
    }

    @Override
    public List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        ItemStack stack = new ItemStack(this.asItem());

        if (stack.isEmpty()) {
            var item = ChemicalHelper.get(tagPrefix, material);
            stack = new ItemStack(item.getItem());
        }

        return Collections.singletonList(stack);
    }

    @Override
    public void playerDestroy(@NotNull Level world, @NotNull Player player, @NotNull BlockPos pos,
                              @NotNull BlockState state, @Nullable BlockEntity blockEntity, @NotNull ItemStack tool) {
        if (!player.isCreative() && !world.isClientSide) {
            LootParams.Builder builder = new LootParams.Builder((net.minecraft.server.level.ServerLevel) world)
                    .withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, tool);

            List<ItemStack> drops = this.getDrops(state, builder);
            for (ItemStack drop : drops) {
                popResource(world, pos, drop);
            }
        }

        super.playerDestroy(world, player, pos, state, blockEntity, tool);
    }

    @Override
    public void spawnAfterBreak(@NotNull BlockState state, @NotNull net.minecraft.server.level.ServerLevel world,
                                @NotNull BlockPos pos, @NotNull ItemStack tool, boolean dropXp) {
        super.spawnAfterBreak(state, world, pos, tool, dropXp);
        if (tool.isEmpty()) {
            LootParams.Builder builder = new LootParams.Builder(world)
                    .withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, tool);

            List<ItemStack> drops = this.getDrops(state, builder);
            for (ItemStack drop : drops) {
                popResource(world, pos, drop);
            }
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader world, BlockPos pos) {
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
