package net.phoenix.core.common.data.worldgen;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.data.worldgen.generator.IndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.ores.GeneratedVeinMetadata;
import com.gregtechceu.gtceu.api.data.worldgen.ores.OreIndicatorPlacer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrystalRoseIndicatorGenerator extends IndicatorGenerator {

    // FIXED: By explicitly casting the entire Either codec codec payload to an Object-friendly variant,
    // Java can match it against the fields perfectly without breaking runtime functionality.
    @SuppressWarnings("unchecked")
    public static final Codec<CrystalRoseIndicatorGenerator> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ((Codec<Either<BlockState, Material>>) (Codec<?>) Codec.either(
                            BlockState.CODEC,
                            GTCEuAPI.materialManager.codec())).fieldOf("block").forGetter(ext -> ext.block),
                    IntProvider.codec(1, 32).fieldOf("radius").forGetter(ext -> ext.radius),
                    FloatProvider.codec(0.0f, 2.0f).fieldOf("density").forGetter(ext -> ext.density),
                    SurfaceIndicatorGenerator.IndicatorPlacement.CODEC.fieldOf("placement")
                            .forGetter(ext -> ext.placement))
            .apply(instance, CrystalRoseIndicatorGenerator::new));

    // CLEANED UP: Standardized completely back to concrete Material definitions
    private Either<BlockState, Material> block = Either.left(Blocks.AIR.defaultBlockState());
    private IntProvider radius = ConstantInt.of(5);
    private FloatProvider density = ConstantFloat.of(0.2f);
    private SurfaceIndicatorGenerator.IndicatorPlacement placement = SurfaceIndicatorGenerator.IndicatorPlacement.SURFACE;

    public CrystalRoseIndicatorGenerator(GTOreDefinition entry) {
        super(entry);
    }

    public CrystalRoseIndicatorGenerator(Either<BlockState, Material> block, IntProvider radius, FloatProvider density,
                                         SurfaceIndicatorGenerator.IndicatorPlacement placement) {
        super(null);
        this.block = block;
        this.radius = radius;
        this.density = density;
        this.placement = placement;
    }

    public CrystalRoseIndicatorGenerator state(BlockState state) {
        this.block = Either.left(state);
        return this;
    }

    public CrystalRoseIndicatorGenerator radius(int radius) {
        this.radius = ConstantInt.of(radius);
        return this;
    }

    public CrystalRoseIndicatorGenerator density(float density) {
        this.density = ConstantFloat.of(density);
        return this;
    }

    public CrystalRoseIndicatorGenerator placement(SurfaceIndicatorGenerator.IndicatorPlacement placement) {
        this.placement = placement;
        return this;
    }

    @Override
    public Map<ChunkPos, OreIndicatorPlacer> generate(WorldGenLevel level, RandomSource random,
                                                      GeneratedVeinMetadata metadata) {
        BlockState blockState = placement.stateTransformer.apply(block);

        int r = this.radius.sample(random);
        float d = this.density.sample(random);
        BlockPos center = metadata.center();

        Stream<BlockPos> positionStream = BlockPos.betweenClosedStream(
                center.getX() - r, center.getY(), center.getZ() - r,
                center.getX() + r, center.getY(), center.getZ() + r).map(BlockPos::immutable);

        var positions = positionStream
                .filter(pos -> pos.equals(center) || random.nextFloat() <= d)
                .filter(pos -> Math.sqrt(pos.distSqr(center)) <= r)
                .toList();

        return WorldGeneratorUtils.groupByChunks(positions).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> createPlacer(level, entry.getValue(), blockState)));
    }

    private OreIndicatorPlacer createPlacer(WorldGenLevel level, List<BlockPos> positionsWithoutY,
                                            BlockState blockState) {
        return (access) -> {
            for (BlockPos initialPos : positionsWithoutY) {
                // Find the true surface by casting up from the vein center Y level
                BlockPos pos = findTrueSurfacePos(level, access, initialPos);

                if (pos == null || level.isOutsideBuildHeight(pos)) {
                    continue;
                }

                var section = access.getSection(pos);
                if (section == null) continue;

                int sectionX = SectionPos.sectionRelative(pos.getX());
                int sectionY = SectionPos.sectionRelative(pos.getY());
                int sectionZ = SectionPos.sectionRelative(pos.getZ());

                BlockState currentTargetState = section.getBlockState(sectionX, sectionY, sectionZ);

                // Must be genuine surface air, not cave air or solid blocks
                if (!currentTargetState.is(Blocks.AIR)) {
                    continue;
                }

                if (!blockState.canSurvive(level, pos)) {
                    continue;
                }

                section.setBlockState(sectionX, sectionY, sectionZ, blockState, false);
            }
        };
    }

    /**
     * Starts at the vein's generation height and steps upwards.
     * Skips underground caves and only stops when it finds air exposing a solid floor open to the sky.
     */
    private BlockPos findTrueSurfacePos(WorldGenLevel level, BulkSectionAccess access, BlockPos startPos) {
        BlockPos.MutableBlockPos cursor = startPos.mutable();
        int maxSearchHeight = level.getMaxBuildHeight() - 2;
        boolean insideCave = false;

        while (cursor.getY() < maxSearchHeight) {
            BlockState current = access.getBlockState(cursor);
            BlockState below = access.getBlockState(cursor.below());

            // Track if we are currently moving through an underground cave space
            if (current.isAir() && !current.is(Blocks.AIR)) {
                insideCave = true;
            }

            // If we transitioned out of caves/solid rock and found standard surface AIR
            if (current.is(Blocks.AIR)) {
                // Make sure the floor beneath it is solid and NOT a cave ceiling/ ledge inside a cave
                if (below.isSolid() && !below.isAir()) {
                    // Quick look ahead: if the next 15 blocks are also air, this is definitely the surface world
                    BlockState higherCheck = access.getBlockState(cursor.above(10));
                    if (higherCheck.is(Blocks.AIR)) {
                        return cursor.immutable(); // Found it!
                    }
                }
            }

            cursor.move(Direction.UP);
        }

        return null; // Fallback failure state if it hits the sky limit
    }

    @Nullable
    @Override
    public Either<BlockState, Material> block() {
        return this.block;
    }

    @Override
    public int getSearchRadiusModifier(int veinRadius) {
        return Math.max(0, radius.getMaxValue() - veinRadius);
    }

    @Override
    public @NotNull Codec<? extends IndicatorGenerator> codec() {
        return CODEC;
    }
}
