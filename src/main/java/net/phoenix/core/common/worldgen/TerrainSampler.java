package net.phoenix.core.common.worldgen;

@FunctionalInterface
public interface TerrainSampler {
    /** Returns density at (x,y,z). Positive = solid, negative = air. */
    double sample(int x, int y, int z);
}
