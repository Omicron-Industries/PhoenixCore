package net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class SoulBalance {

    private static final List<TagKey<Biome>> MAGICAL_BIOME_TAGS = List.of(
            TagKey.create(Registries.BIOME, new ResourceLocation("forge", "is_magical")),
            TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", "is_forest")),
            TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", "is_jungle")));

    public record SoulProfile(float minSoul, float maxSoul, float regenPerTick) {}

    private static final Map<ResourceLocation, SoulProfile> REGISTRY = new HashMap<>();

    // This is your catch-all for biomes like Plains, Forests, etc.
    private static final SoulProfile DEFAULT = new SoulProfile(0.8f, 1.2f, 0.001f);
    private static final SoulProfile END_NETHER = new SoulProfile(0.1f, 0.2f, 0.001f);

    // This is a "Smart Fallback" for modded biomes that have magical tags
    // but aren't manually registered by you yet.
    private static final SoulProfile MAGICAL_FALLBACK = new SoulProfile(1.0f, 2.0f, 0.003f);

    static {
        register("ars_nouveau:archwood_forrest", 2.0f, 4.0f, 0.008f);
    }

    private static void register(String id, float min, float max, float regen) {
        REGISTRY.put(new ResourceLocation(id), new SoulProfile(min, max, regen));
    }

    /**
     * Gets the profile for a biome.
     * Priority: Manual Registry -> Biome Tag Check -> Default
     */
    public static SoulProfile get(Holder<Biome> biomeHolder, Level level) {
        // 1. Check Manual Registry first
        ResourceLocation id = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id != null && REGISTRY.containsKey(id)) {
            return REGISTRY.get(id);
        }

        // 2. Check dimension
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.NETHER || dim == Level.END) return END_NETHER;

        // 3. Check magical biome tags
        boolean isMagical = MAGICAL_BIOME_TAGS.stream().anyMatch(biomeHolder::is);
        if (isMagical) {
            return MAGICAL_FALLBACK;
        }

        return DEFAULT;
    }
}
