package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.pipenet.IPipeNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class PhoenixPipeLinker {

    public static void linkNodes(Level level, BlockPos posA, IPipeNode<?, ?> nodeA, Direction side) {
        BlockPos posB = posA.relative(side);

        if (level.getBlockEntity(posB) instanceof IPipeNode<?, ?> nodeB) {

            nodeA.setConnection(side, true, false);
            nodeB.setConnection(side.getOpposite(), true, false);

            level.neighborChanged(posA, level.getBlockState(posB).getBlock(), posB);
            level.neighborChanged(posB, level.getBlockState(posA).getBlock(), posA);

            nodeA.scheduleRenderUpdate();
            nodeB.scheduleRenderUpdate();

            level.sendBlockUpdated(posA, level.getBlockState(posA), level.getBlockState(posA), Block.UPDATE_ALL);
            level.sendBlockUpdated(posB, level.getBlockState(posB), level.getBlockState(posB), Block.UPDATE_ALL);

            nodeA.self().setChanged();
            nodeB.self().setChanged();
        }
    }
}
