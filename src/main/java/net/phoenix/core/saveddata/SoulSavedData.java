package net.phoenix.core.saveddata;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul.SoulBalance;
import net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul.SoulChunkEntry;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SoulSavedData extends SavedData {

    private final Map<ChunkPos, SoulChunkEntry> soulMap = new HashMap<>();
    private final ServerLevel level;

    public SoulSavedData(ServerLevel level) {
        this.level = level;
    }

    public static SoulSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> new SoulSavedData(level, tag),
                () -> new SoulSavedData(level),
                "ars_soul_data");
    }

    public SoulSavedData(ServerLevel level, CompoundTag nbt) {
        this.level = level;
        ListTag list = nbt.getList("SoulData", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            soulMap.put(new ChunkPos(entryTag.getLong("pos")), SoulChunkEntry.load(entryTag));
        }
    }

    public void modifySoul(ChunkPos pos, float amount, boolean isPermanent) {
        getMultiplier(pos);

        SoulChunkEntry entry = soulMap.get(pos);
        if (entry != null) {
            if (isPermanent) {

                entry.maxCapacity += amount;
                entry.currentSoul += amount;
            } else {

                entry.currentSoul = Math.min(entry.maxCapacity, entry.currentSoul + amount);
            }
            this.setDirty();
        }
    }

    private float calculateBiomeBase(ChunkPos pos) {
        var biomeHolder = level.getBiome(pos.getMiddleBlockPosition(level.getSeaLevel()));
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biomeHolder.value());

        float base = 1.0f;
        if (biomeId != null) {
            String path = biomeId.getPath();
            if (path.contains("forest") || path.contains("grove")) base = 1.5f;
            else if (path.contains("magical") || path.contains("old_growth")) base = 2.5f;
            else if (path.contains("wasteland") || path.contains("desert")) base = 0.5f;
            else if (path.contains("dead") || path.contains("corruption")) base = 0.1f;
        }

        float noise = ((Math.abs(pos.x * 31 + pos.z * 17) % 100) / 333.0f) - 0.15f;

        return Math.max(0.05f, base + noise);
    }

    private void applyNaturalRegen(SoulChunkEntry entry, long currentTime) {
        if (entry.currentSoul < entry.maxCapacity) {
            long timePassed = currentTime - entry.lastUpdateTime;

            if (timePassed >= 1200) {
                float regenAmount = (timePassed / 1200f) * 0.01f;
                entry.currentSoul = Math.min(entry.maxCapacity, entry.currentSoul + regenAmount);
                entry.lastUpdateTime = currentTime;
                this.setDirty();
            }
        } else {

            entry.lastUpdateTime = currentTime;
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        soulMap.forEach((pos, entry) -> {
            CompoundTag chunkTag = entry.save();
            chunkTag.putLong("pos", pos.toLong());
            list.add(chunkTag);
        });
        tag.put("SoulData", list);
        return tag;
    }

    public static SoulSavedData load(CompoundTag tag, ServerLevel level) {
        SoulSavedData data = new SoulSavedData(level);

        ListTag list = tag.getList("SoulData", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag chunkTag = list.getCompound(i);
            data.soulMap.put(new ChunkPos(chunkTag.getLong("pos")), SoulChunkEntry.load(chunkTag));
        }
        return data;
    }

    public float getMultiplier(ChunkPos pos) {
        if (soulMap.containsKey(pos)) {
            SoulChunkEntry entry = soulMap.get(pos);

            applyNaturalRegen(entry, level.getGameTime());

            return entry.currentSoul;
        }

        float baseSoul = calculateBiomeBase(pos);
        soulMap.put(pos, new SoulChunkEntry(baseSoul, baseSoul, level.getGameTime()));
        this.setDirty();
        return baseSoul;
    }

    private void tickRegen(ChunkPos pos, SoulChunkEntry entry) {
        long ticksPassed = level.getGameTime() - entry.lastUpdateTime;
        if (ticksPassed <= 0) return;

        Holder<Biome> biomeHolder = level.getBiome(pos.getMiddleBlockPosition(64));
        SoulBalance.SoulProfile profile = SoulBalance.get(biomeHolder, level);

        float updatedSoul = entry.currentSoul + (ticksPassed * profile.regenPerTick());
        entry.currentSoul = Math.min(entry.maxCapacity, updatedSoul);

        entry.lastUpdateTime = level.getGameTime();
        setDirty();
    }

    public Map<ChunkPos, SoulChunkEntry> getSoulMap() {
        return soulMap;
    }
}
