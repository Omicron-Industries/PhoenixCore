package net.phoenix.core.common.worldgen;

@FunctionalInterface
public interface TerrainSampler {

    double sample(int x, int y, int z);
}
