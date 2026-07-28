package net.phoenix.core.growth.tendril;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.phoenix.core.growth.GrowthMultiblockMachine;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TendrilShapeRegistry {

    private record Entry(TendrilShape shape, TendrilWeightCondition weight) {}

    private static final Map<ResourceLocation, Entry> REGISTRY = new LinkedHashMap<>();

    public static void register(TendrilShape shape, TendrilWeightCondition weight) {
        REGISTRY.put(shape.id(), new Entry(shape, weight));
    }

    public static Collection<TendrilShape> getAll() {
        return REGISTRY.values().stream().map(Entry::shape).toList();
    }

    @Nullable
    public static TendrilShape pickWeighted(GrowthMultiblockMachine machine, RandomSource random) {
        List<Entry> entries = new ArrayList<>(REGISTRY.values());
        int[] weights = new int[entries.size()];
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            int w = Math.max(0, entries.get(i).weight().weight(machine, entries.get(i).shape()));
            weights[i] = w;
            total += w;
        }
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < entries.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) return entries.get(i).shape();
        }
        return null;
    }

    private TendrilShapeRegistry() {}
}
