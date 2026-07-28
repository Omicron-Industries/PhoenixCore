package net.phoenix.core.growth;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.ExpandableMultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public final class GrowthPatternHelper {

    private static final IntList EMPTY_BOUNDS = new IntArrayList(new int[] { 0, 0, 0, 0, 0, 0 });
    private static final RelativeDirection[] AXES = { RelativeDirection.BACK, RelativeDirection.UP,
            RelativeDirection.RIGHT };

    private GrowthPatternHelper() {}

    public static IBlockPattern getPattern(MultiblockMachineDefinition definition, PatternPredicate shellPredicate,
                                           IntList maxBounds) {
        return ExpandableMultiblockPatternBuilder.start()
                .boundsProvider(GrowthPatternHelper::boundsFor)
                .constraintProvider(() -> List.of(
                        IntIntPair.of(0, maxBounds.getInt(0)),
                        IntIntPair.of(0, maxBounds.getInt(1)),
                        IntIntPair.of(0, maxBounds.getInt(2)),
                        IntIntPair.of(0, maxBounds.getInt(3)),
                        IntIntPair.of(0, maxBounds.getInt(4)),
                        IntIntPair.of(0, maxBounds.getInt(5))))
                .predicateProvider((pos, bounds) -> pos.equals(BlockPos.ZERO) ?
                        Predicates.controller(Predicates.blocks(definition.get())) :
                        intersections(pos, bounds) >= 1 ? shellPredicate : Predicates.any())
                .build();
    }

    private static IntList boundsFor(Level level, BlockPos.MutableBlockPos pos, Direction front, Direction up) {
        if (level.getBlockEntity(pos) instanceof GrowthMultiblockMachine host) {
            List<GrowthStage> stages = host.getGrowthStages();
            int stage = Math.min(Math.max(host.getGrowthStage(), 0), stages.size() - 1);
            return stages.get(stage).bounds();
        }
        return EMPTY_BOUNDS;
    }

    private static int intersections(BlockPos pos, List<Integer> bounds) {
        int n = 0;
        if (pos.getX() == bounds.get(5) || pos.getX() == -bounds.get(4)) n++;
        if (pos.getY() == bounds.get(0) || pos.getY() == -bounds.get(1)) n++;
        if (pos.getZ() == bounds.get(3) || pos.getZ() == -bounds.get(2)) n++;
        return n;
    }

    private static boolean withinAndOnShell(BlockPos pos, IntList bounds) {
        boolean withinX = pos.getX() <= bounds.getInt(5) && pos.getX() >= -bounds.getInt(4);
        boolean withinY = pos.getY() <= bounds.getInt(0) && pos.getY() >= -bounds.getInt(1);
        boolean withinZ = pos.getZ() <= bounds.getInt(3) && pos.getZ() >= -bounds.getInt(2);
        return withinX && withinY && withinZ && intersections(pos, bounds) >= 1;
    }

    public static List<BlockPos> diffShellPositions(BlockPos center, Direction front, Direction up,
                                                    IntList fromBounds, IntList toBounds) {
        BlockPos.MutableBlockPos negCorner = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posCorner = new BlockPos.MutableBlockPos();
        negCorner.setX(-toBounds.getInt(4));
        posCorner.setX(toBounds.getInt(5));
        negCorner.setY(-toBounds.getInt(1));
        posCorner.setY(toBounds.getInt(0));
        negCorner.setZ(-toBounds.getInt(2));
        posCorner.setZ(toBounds.getInt(3));

        List<BlockPos> result = new ArrayList<>();
        for (BlockPos relative : BlockPos.betweenClosed(negCorner, posCorner)) {
            if (intersections(relative, toBounds) < 1) continue;
            if (withinAndOnShell(relative, fromBounds)) continue;

            result.add(rotateToWorld(relative, front, up).offset(center).immutable());
        }
        return result;
    }

    public static BlockPos rotateToWorld(BlockPos local, Direction front, Direction up) {
        Direction[] absolutes = new Direction[3];
        for (int i = 0; i < 3; i++) absolutes[i] = AXES[i].getRelativeFacing(front, up, false);

        BlockPos.MutableBlockPos abs = new BlockPos.MutableBlockPos();
        abs.set(BlockPos.ZERO).move(absolutes[0], local.getX()).move(absolutes[1], local.getY())
                .move(absolutes[2], local.getZ());
        return abs.immutable();
    }
}
