package net.phoenix.core.common.worldgen;

public record TerrainProfile(
                             String name,
                             long seed,
                             int minY,
                             int maxY,
                             int seaLevel,
                             double baseY,
                             double amplitude,
                             double frequency,
                             int octaves,
                             boolean caves,
                             boolean volumetric,
                             TerrainSampler sampler) {

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private long seed = 0L;
        private int minY = -64;
        private int maxY = 320;
        private int seaLevel = 63;
        private double baseY = 64;
        private double amplitude = 80;
        private double frequency = 0.004;
        private int octaves = 5;
        private boolean caves = true;
        private boolean volumetric = false;

        public Builder(String name) {
            this.name = name;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder minY(int y) {
            this.minY = y;
            return this;
        }

        public Builder maxY(int y) {
            this.maxY = y;
            return this;
        }

        public Builder seaLevel(int y) {
            this.seaLevel = y;
            return this;
        }

        public Builder baseY(double y) {
            this.baseY = y;
            return this;
        }

        public Builder amplitude(double amp) {
            this.amplitude = amp;
            return this;
        }

        public Builder frequency(double freq) {
            this.frequency = freq;
            return this;
        }

        public Builder octaves(int n) {
            this.octaves = n;
            return this;
        }

        public Builder caves(boolean b) {
            this.caves = b;
            return this;
        }

        public Builder volumetric(boolean b) {
            this.volumetric = b;
            return this;
        }

        public TerrainProfile build() {
            TerrainSampler terrain = volumetric ?
                    PhoenixTerrainNoise.volumetric(seed, baseY, amplitude, frequency, frequency * 2, octaves) :
                    PhoenixTerrainNoise.heightmap(seed, baseY, amplitude, frequency, octaves);

            if (caves) {
                TerrainSampler caveNoise = PhoenixTerrainNoise.caves(seed + 1, frequency * 4, 0.05);
                terrain = PhoenixTerrainNoise.withCaves(terrain, caveNoise);
            }

            return new TerrainProfile(name, seed, minY, maxY, seaLevel, baseY, amplitude, frequency, octaves, caves,
                    volumetric, terrain);
        }
    }
}
