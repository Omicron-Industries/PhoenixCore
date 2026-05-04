package net.phoenix.core.integration.phantasia;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class FakeChunkSource extends ChunkSource {

    private final PhantasiaFakeLevel fakeLevel;
    private final Map<ChunkPos, FakeChunk> chunks = new HashMap<>();

    public FakeChunkSource(PhantasiaFakeLevel fakeLevel) {
        this.fakeLevel = fakeLevel;
    }

    @Override
    public @Nullable ChunkAccess getChunk(int x, int z, @NotNull ChunkStatus requiredStatus, boolean create) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        return chunks.computeIfAbsent(chunkPos, k -> new FakeChunk(fakeLevel, chunkPos));
    }

    @Override
    public void tick(@NotNull BooleanSupplier booleanSupplier, boolean b) {
        // No-op for fake chunk source
    }

    @Override
    public @NotNull String gatherStats() {
        return "FakeChunkSource";
    }

    @Override
    public int getLoadedChunksCount() {
        return chunks.size();
    }

    @Override
    public @NotNull LevelLightEngine getLightEngine() {
        return fakeLevel.getLightEngine();
    }

    @Override
    public @NotNull BlockGetter getLevel() {
        return fakeLevel;
    }
}
