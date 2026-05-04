package net.phoenix.core.integration.phantasia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.TickContainerAccess;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class FakeChunk extends ChunkAccess {

    private final PhantasiaFakeLevel fakeLevel;

    public FakeChunk(PhantasiaFakeLevel fakeLevel, ChunkPos pos) {
        // 1. We create the array FIRST so we can reference it after the super() call
        this(fakeLevel, pos, new LevelChunkSection[fakeLevel.getSectionsCount()]);
    }

    // Private helper constructor to handle the array reference
    private FakeChunk(PhantasiaFakeLevel fakeLevel, ChunkPos pos, LevelChunkSection[] sectionsArray) {
        super(
                pos,
                UpgradeData.EMPTY,
                fakeLevel,
                fakeLevel.registryAccess().registryOrThrow(Registries.BIOME),
                0L,
                sectionsArray, // Pass it to the super
                null);

        this.fakeLevel = fakeLevel;
        var biomeRegistry = fakeLevel.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> defaultBiome = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);

        for (int i = 0; i < sectionsArray.length; i++) {
            // Biome Palette
            PalettedContainer<Holder<Biome>> biomeContainer = new PalettedContainer<>(
                    biomeRegistry.asHolderIdMap(),
                    defaultBiome,
                    PalettedContainer.Strategy.SECTION_BIOMES);

            // Block Palette (Required in many versions to prevent crashes)
            // This initializes the section as full of Air.
            PalettedContainer<BlockState> blockContainer = new PalettedContainer<>(
                    Block.BLOCK_STATE_REGISTRY,
                    Blocks.AIR.defaultBlockState(),
                    PalettedContainer.Strategy.SECTION_STATES);

            // Initialize the section with BOTH containers
            sectionsArray[i] = new LevelChunkSection(
                    blockContainer, // Pass block container first usually
                    biomeContainer);
        }

        // 3. Initialize Heightmaps
        for (Heightmap.Types type : Heightmap.Types.values()) {
            this.heightmaps.put(type, new Heightmap(this, type));
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        // Redirect to the level's map so CTM finds neighbors
        return fakeLevel.getBlockState(pos);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        // Redirect so ModelData can be fetched from neighbors
        return fakeLevel.getBlockEntity(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    // --- Minimal implementations for abstract methods ---

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return fakeLevel.setBlock(pos, state, 3) ? state : null;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        fakeLevel.setBlockEntity(blockEntity);
    }

    @Override
    public void addEntity(net.minecraft.world.entity.Entity entity) {
        // No entities in fake level
    }

    @Override
    public void setHeightmap(Heightmap.Types type, long[] data) {
        // No heightmaps needed
    }

    private final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);

    @Override
    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return heightmaps.computeIfAbsent(type, (t) -> new Heightmap(this, t));
    }

    @Override
    public int getHighestFilledSectionIndex() {
        return 0;
    }

    @Override
    public Set<BlockPos> getBlockEntitiesPos() {
        return fakeLevel.getBlockEntities().keySet();
    }

    @Override
    public LevelChunkSection[] getSections() {
        // Return the array that was initialized in the constructor
        return super.getSections();
    }

    @Override
    public void setUnsaved(boolean unsaved) {
        // Always unsaved
    }

    @Override
    public boolean isUnsaved() {
        return true;
    }

    @Override
    public ChunkStatus getStatus() {
        return ChunkStatus.FULL; // Ensure this is FULL
    }

    // In 1.20.1, the method to check if a chunk is fully loaded is usually:

    @Override
    public void removeBlockEntity(BlockPos blockPos) {}

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos) {
        return null;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return null;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return null;
    }

    @Override
    public TicksToSave getTicksForSerialization() {
        return null;
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(int x, int y, int z) {
        // Note the return type change to Holder<Biome>
        return fakeLevel.getBiome(new BlockPos(x << 2, y << 2, z << 2));
    }

    @Override
    public int getLightEmission(BlockPos pos) {
        return 0;
    }
}
