package net.phoenix.core.integration.ars_nouveau.common.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SourceHatchTracker {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> HATCHES = new ConcurrentHashMap<>();

    public static void add(ResourceKey<Level> dim, BlockPos pos) {
        HATCHES.computeIfAbsent(dim, d -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(pos.immutable());
    }

    public static void remove(ResourceKey<Level> dim, BlockPos pos) {
        Set<BlockPos> set = HATCHES.get(dim);
        if (set != null && set.remove(pos)) {

        }
    }

    public static Set<BlockPos> get(ResourceKey<Level> dim) {
        return HATCHES.getOrDefault(dim, Collections.emptySet());
    }
}
