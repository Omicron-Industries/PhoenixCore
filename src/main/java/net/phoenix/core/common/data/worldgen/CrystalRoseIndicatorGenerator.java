package net.phoenix.core.common.data.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class CrystalRoseIndicatorGenerator extends SurfaceIndicatorGenerator {

    public CrystalRoseIndicatorGenerator(GTOreDefinition entry) {
        super(entry);
        this.placement(IndicatorPlacement.SURFACE);
    }

    public static BlockPos resolveMagicSurface(WorldGenLevel level, BulkSectionAccess access, BlockPos pos) {
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
        BlockPos surfacePos = pos.atY(y);

        BlockState stateAtSurface = access.getBlockState(surfacePos);

        if (stateAtSurface.is(Blocks.SNOW)) {
            return surfacePos;
        }

        if (stateAtSurface.is(Blocks.WATER)) {
            return surfacePos.above();
        }

        if (stateAtSurface.isAir()) {
            return surfacePos;
        }

        return surfacePos.above();
    }
}
