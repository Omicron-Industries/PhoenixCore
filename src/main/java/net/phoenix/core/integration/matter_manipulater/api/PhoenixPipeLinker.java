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

            // 1. Set the internal connection booleans
            // Third param 'false' is crucial to prevent recursive loops
            nodeA.setConnection(side, true, false);
            nodeB.setConnection(side.getOpposite(), true, false);

            // 2. Trigger Minecraft Physics/Redstone updates
            level.neighborChanged(posA, level.getBlockState(posB).getBlock(), posB);
            level.neighborChanged(posB, level.getBlockState(posA).getBlock(), posA);

            // 3. Mark for re-render and saving
            // scheduleRenderUpdate handles the GT-side ModelData refresh
            nodeA.scheduleRenderUpdate();
            nodeB.scheduleRenderUpdate();

            // 4. Force NBT Sync (The "Visual Connection" fix)
            // level.sendBlockUpdated is mandatory when changing connections via code
            level.sendBlockUpdated(posA, level.getBlockState(posA), level.getBlockState(posA), Block.UPDATE_ALL);
            level.sendBlockUpdated(posB, level.getBlockState(posB), level.getBlockState(posB), Block.UPDATE_ALL);

            // Explicitly mark dirty to ensure NBT saves the connection to disk
            nodeA.self().setChanged();
            nodeB.self().setChanged();
        }
    }
}
