package net.phoenix.core.common.worldgen;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

public final class PhoenixTerrainPresets {

    private PhoenixTerrainPresets() {}

    public static TerrainProfile plains(long seed) {
        return TerrainProfile.builder("Plains")
                .seed(seed)
                .baseY(64).amplitude(12).frequency(0.003).octaves(4)
                .caves(false).volumetric(false)
                .build();
    }

    public static TerrainProfile rollingHills(long seed) {
        return TerrainProfile.builder("Rolling Hills")
                .seed(seed)
                .baseY(68).amplitude(40).frequency(0.005).octaves(5)
                .caves(true).volumetric(false)
                .build();
    }

    public static TerrainProfile mountains(long seed) {
        return TerrainProfile.builder("Mountains")
                .seed(seed)
                .baseY(80).amplitude(160).frequency(0.004).octaves(6)
                .caves(true).volumetric(false)
                .build();
    }

    public static TerrainProfile alienMesas(long seed) {
        return TerrainProfile.builder("Alien Mesas")
                .seed(seed)
                .baseY(90).amplitude(70).frequency(0.006).octaves(4)
                .caves(false).volumetric(true)
                .build();
    }

    public static TerrainProfile floatingIslands(long seed) {
        return TerrainProfile.builder("Floating Islands")
                .seed(seed)
                .baseY(100).amplitude(120).frequency(0.008).octaves(5)
                .caves(false).volumetric(true)
                .seaLevel(0)
                .build();
    }

    public static TerrainProfile deathValley(long seed) {
        return TerrainProfile.builder("Death Valley")
                .seed(seed)
                .baseY(30).amplitude(25).frequency(0.003).octaves(4)
                .caves(false).volumetric(false)
                .seaLevel(28)
                .build();
    }

    public static TerrainProfile cavernous(long seed) {
        return TerrainProfile.builder("Cavernous")
                .seed(seed)
                .baseY(64).amplitude(60).frequency(0.006).octaves(5)
                .caves(true).volumetric(false)
                .build();
    }

    public static List<Map.Entry<String, LongFunction<TerrainProfile>>> all() {
        return List.of(
                entry("Plains", PhoenixTerrainPresets::plains),
                entry("Rolling Hills", PhoenixTerrainPresets::rollingHills),
                entry("Mountains", PhoenixTerrainPresets::mountains),
                entry("Alien Mesas", PhoenixTerrainPresets::alienMesas),
                entry("Floating Islands", PhoenixTerrainPresets::floatingIslands),
                entry("Death Valley", PhoenixTerrainPresets::deathValley),
                entry("Cavernous", PhoenixTerrainPresets::cavernous));
    }

    private static Map.Entry<String, LongFunction<TerrainProfile>> entry(String name, LongFunction<TerrainProfile> fn) {
        return new AbstractMap.SimpleImmutableEntry<>(name, fn);
    }
}
