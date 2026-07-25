package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PhoenixTerrainNoise {

    private PhoenixTerrainNoise() {}

    public static TerrainSampler heightmap(long seed, double baseY, double amplitude, double frequency, int octaves) {
        SimplexNoise noise = makeNoise(seed);
        return (x, y, z) -> {
            double height = baseY + amplitude * fbm(noise, x, 0, z, octaves, frequency);
            return height - y;
        };
    }

    public static TerrainSampler volumetric(long seed, double baseY, double amplitude, double xzFreq, double yFreq,
                                            int octaves) {
        SimplexNoise noise = makeNoise(seed);
        return (x, y, z) -> {
            double d = baseY + amplitude * fbm3D(noise, x, y, z, octaves, xzFreq, yFreq);
            return d - y;
        };
    }

    public static TerrainSampler caves(long seed, double frequency, double threshold) {
        SimplexNoise noise1 = makeNoise(seed);
        SimplexNoise noise2 = makeNoise(seed ^ 0xDEADBEEFL);
        return (x, y, z) -> {
            double n1 = noise1.getValue(x * frequency, y * frequency, z * frequency);
            double n2 = noise2.getValue(x * frequency + 100, y * frequency * 0.5, z * frequency + 100);

            double tube = Math.abs(n1) + Math.abs(n2);
            return tube - threshold;
        };
    }

    public static TerrainSampler withCaves(TerrainSampler terrain, TerrainSampler caves) {
        return (x, y, z) -> {
            double t = terrain.sample(x, y, z);
            double c = caves.sample(x, y, z);

            if (t > 0 && c < 0) {
                return c;
            }
            return t;
        };
    }

    public static TerrainSampler vein(long seed, double scale, double threshold) {
        SimplexNoise n1 = makeNoise(seed);
        SimplexNoise n2 = makeNoise(seed ^ 0xCAFEBABEL);
        return (x, y, z) -> {
            double a = Math.abs(n1.getValue(x * scale, y * scale * 0.5, z * scale));
            double b = Math.abs(n2.getValue(x * scale + 31.7, y * scale * 0.5 + 17.3, z * scale - 41.2));
            return (a + b) * 0.5 - threshold;
        };
    }

    private static SimplexNoise makeNoise(long seed) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
        return new SimplexNoise(random);
    }

    private static double fbm(SimplexNoise noise, double x, double y, double z, int octaves, double freq) {
        double value = 0;
        double amplitude = 1.0;
        double maxAmp = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise.getValue(x * freq, y * freq, z * freq) * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            freq *= 2.0;
        }
        return value / maxAmp;
    }

    private static double fbm3D(SimplexNoise noise, double x, double y, double z, int octaves, double xzFreq,
                                double yFreq) {
        double value = 0;
        double amplitude = 1.0;
        double maxAmp = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise.getValue(x * xzFreq, y * yFreq, z * xzFreq) * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            xzFreq *= 2.0;
            yFreq *= 2.0;
        }
        return value / maxAmp;
    }
}
