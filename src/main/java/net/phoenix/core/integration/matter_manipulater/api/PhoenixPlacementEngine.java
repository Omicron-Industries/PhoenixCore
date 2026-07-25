package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import appeng.api.parts.IPartItem;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.AEBlocks;

import java.util.ArrayList;
import java.util.List;

public class PhoenixPlacementEngine {

    public static void fillPipeArea(Level level, Player player, BlockPos p1, BlockPos p2, ItemStack tool,
                                    PhoenixManipulatorMode mode) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<BlockPos> targets = getTargetPositions(p1, p2, mode);
        ItemStack offhandStack = player.getOffhandItem();
        int actionCount = 0;

        for (BlockPos pos : targets) {
            if (mode == PhoenixManipulatorMode.DISCONNECT) {
                handleRemoval(serverLevel, pos);
                actionCount++;
            } else {
                boolean isAir = level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced();
                boolean isCableBus = level.getBlockEntity(pos) instanceof CableBusBlockEntity;

                if (isAir) {
                    if (offhandStack.getItem() instanceof PipeBlockItem pipeItem) {

                        if (!PhoenixInventoryService.consumePipe(player, offhandStack)) break;
                        level.setBlock(pos, pipeItem.getBlock().defaultBlockState(), 3);
                        actionCount++;
                    } else if (offhandStack.getItem() instanceof IPartItem<?>) {
                        if (placeAE2Cable(level, pos, offhandStack, player)) {
                            actionCount++;
                        }
                    }
                } else if (isCableBus && offhandStack.getItem() instanceof IPartItem<?>) {
                    if (placeAE2Cable(level, pos, offhandStack, player)) {
                        actionCount++;
                    }
                }
            }
        }

        if (mode != PhoenixManipulatorMode.DISCONNECT) {
            for (BlockPos pos : targets) {
                if (level.getBlockEntity(pos) instanceof IPipeNode<?, ?> node) {
                    boolean visuallyUpdated = false;

                    for (Direction side : Direction.values()) {
                        BlockPos neighborPos = pos.relative(side);
                        if (level.getBlockEntity(neighborPos) instanceof IPipeNode<?, ?> neighbor) {
                            node.setConnection(side, true, true);
                            neighbor.setConnection(side.getOpposite(), true, true);
                            visuallyUpdated = true;
                        }
                    }

                    if (visuallyUpdated) {
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                        node.scheduleRenderUpdate();
                    }
                }

                if (level.getBlockEntity(pos) instanceof CableBusBlockEntity bus) {
                    bus.notifyNeighbors();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            }
        }

        player.displayClientMessage(
                Component.literal("§6Phoenix: " + actionCount + " operations complete."), true);
    }

    private static boolean placeAE2Cable(Level level, BlockPos pos, ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof IPartItem<?> partItem)) return false;

        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, AEBlocks.CABLE_BUS.block().defaultBlockState(), 3);
        }

        if (level.getBlockEntity(pos) instanceof CableBusBlockEntity bus) {
            var container = bus.getCableBus();
            if (container.canAddPart(stack, null)) {
                if (!PhoenixInventoryService.consumePipe(player, stack)) return false;
                container.addPart(partItem, null, player);
                bus.saveChanges();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                return true;
            }
        }
        return false;
    }

    private static void handleRemoval(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IPipeNode<?, ?> node) {
            node.getPipeBlock().getWorldPipeNet(level).removeNode(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (level.getBlockState(pos).getBlock() == AEBlocks.CABLE_BUS.block()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static List<BlockPos> getTargetPositions(BlockPos p1, BlockPos p2, PhoenixManipulatorMode mode) {
        List<BlockPos> positions = new ArrayList<>();

        int minX = Math.min(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX());
        int maxY = Math.max(p1.getY(), p2.getY());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        int anchorX = p1.getX();
        int anchorY = p1.getY();
        int anchorZ = p1.getZ();

        int dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;

        switch (mode) {
            case LINE -> {
                if (dx >= dy && dx >= dz) {
                    for (int x = minX; x <= maxX; x++) positions.add(new BlockPos(x, anchorY, anchorZ));
                } else if (dy >= dx && dy >= dz) {
                    for (int y = minY; y <= maxY; y++) positions.add(new BlockPos(anchorX, y, anchorZ));
                } else {
                    for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(anchorX, anchorY, z));
                }
            }
            case WALL -> {
                if (dx <= dy && dx <= dz) {
                    for (int y = minY; y <= maxY; y++)
                        for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(anchorX, y, z));
                } else if (dy <= dx && dy <= dz) {
                    for (int x = minX; x <= maxX; x++)
                        for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(x, anchorY, z));
                } else {
                    for (int x = minX; x <= maxX; x++)
                        for (int y = minY; y <= maxY; y++) positions.add(new BlockPos(x, y, anchorZ));
                }
            }
            default -> {
                for (int x = minX; x <= maxX; x++)
                    for (int y = minY; y <= maxY; y++)
                        for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(x, y, z));
            }
        }
        return positions;
    }
}
