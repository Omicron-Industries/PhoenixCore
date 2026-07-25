package net.phoenix.core.common.data.worldgen;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.data.worldgen.generator.IndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.ores.GeneratedVeinMetadata;
import com.gregtechceu.gtceu.api.data.worldgen.ores.OreIndicatorPlacer;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrystalRoseIndicatorGenerator extends IndicatorGenerator {

    public static final Codec<CrystalRoseIndicatorGenerator> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ((Codec<Either<BlockState, Material>>) Codec.either(
                            BlockState.CODEC,
                            GTRegistries.MATERIALS.codec())).fieldOf("block").forGetter(ext -> ext.block),
                    IntProvider.codec(1, 32).fieldOf("radius").forGetter(ext -> ext.radius),
                    FloatProvider.codec(0.0f, 2.0f).fieldOf("density").forGetter(ext -> ext.density),
                    SurfaceIndicatorGenerator.IndicatorPlacement.CODEC.fieldOf("placement")
                            .forGetter(ext -> ext.placement))
            .apply(instance, CrystalRoseIndicatorGenerator::new));

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
    public @NotNull Map<ChunkPos, OreIndicatorPlacer> generate(@NotNull WorldGenLevel level, RandomSource random,
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
        return (access, random) -> {
            for (BlockPos initialPos : positionsWithoutY) {

                BlockPos pos = findTrueSurfacePos(level, initialPos);

                if (pos == null || level.isOutsideBuildHeight(pos)) {
                    continue;
                }

                var section = access.getSection(pos);
                if (section == null) continue;

                int sectionX = SectionPos.sectionRelative(pos.getX());
                int sectionY = SectionPos.sectionRelative(pos.getY());
                int sectionZ = SectionPos.sectionRelative(pos.getZ());

                BlockState currentTargetState = section.getBlockState(sectionX, sectionY, sectionZ);

                if (!currentTargetState.isAir() || currentTargetState.is(Blocks.CAVE_AIR)) {
                    continue;
                }

                if (!blockState.canSurvive(level, pos)) {
                    continue;
                }

                section.setBlockState(sectionX, sectionY, sectionZ, blockState, false);
            }
        };
    }

    private BlockPos findDryLandAt(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        int minHeight = level.getMinBuildHeight();

        while (cursor.getY() > minHeight) {
            BlockState state = level.getBlockState(cursor);

            if (!state.getFluidState().isEmpty()) {
                return null;
            }

            if (!state.isAir() && !state.is(Blocks.CAVE_AIR)) {
                return cursor.above().immutable();
            }

            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private BlockPos findTrueSurfacePos(WorldGenLevel level, BlockPos startPos) {
        BlockPos exactPos = findDryLandAt(level, startPos.getX(), startPos.getZ());
        if (exactPos != null) {
            return exactPos;
        }

        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockPos shorePos = findDryLandAt(level, startPos.getX() + dx, startPos.getZ() + dz);
                        if (shorePos != null) {
                            return shorePos;
                        }
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        try {

            net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<com.mojang.serialization.Codec<? extends com.gregtechceu.gtceu.api.data.worldgen.generator.IndicatorGenerator>>> registryKey = net.minecraft.resources.ResourceKey
                    .createRegistryKey(new net.minecraft.resources.ResourceLocation("gtceu", "indicator_generator"));

            net.minecraft.core.Registry<com.mojang.serialization.Codec<? extends com.gregtechceu.gtceu.api.data.worldgen.generator.IndicatorGenerator>> registry = (net.minecraft.core.Registry<com.mojang.serialization.Codec<? extends com.gregtechceu.gtceu.api.data.worldgen.generator.IndicatorGenerator>>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY
                    .get(registryKey.location());

            if (registry != null) {

                net.minecraft.core.Registry.register(
                        registry,
                        new net.minecraft.resources.ResourceLocation("phoenix", "crystal_rose_indicator"),
                        CODEC);
                com.gregtechceu.gtceu.GTCEu.LOGGER
                        .info("[Phoenix Core] Successfully registered CrystalRoseIndicatorGenerator codec.");
            } else {
                com.gregtechceu.gtceu.GTCEu.LOGGER
                        .error("[Phoenix Core] Could not find 'gtceu:indicator_generator' registry wrapper!");
            }
        } catch (Exception e) {
            com.gregtechceu.gtceu.GTCEu.LOGGER
                    .error("[Phoenix Core] Critical error during Indicator Generator codec registration!", e);
        }
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
