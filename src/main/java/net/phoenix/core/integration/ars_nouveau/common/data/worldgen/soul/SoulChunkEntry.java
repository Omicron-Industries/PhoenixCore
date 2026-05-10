package net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul;

import net.minecraft.nbt.CompoundTag;

public class SoulChunkEntry {

    public float currentSoul;
    public float maxCapacity;
    public long lastUpdateTime;

    public SoulChunkEntry(float currentSoul, float maxCapacity, long lastUpdateTime) {
        this.currentSoul = currentSoul;
        this.maxCapacity = maxCapacity;
        this.lastUpdateTime = lastUpdateTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("current", currentSoul);
        tag.putFloat("max", maxCapacity);
        tag.putLong("time", lastUpdateTime);
        return tag;
    }

    public static SoulChunkEntry load(CompoundTag tag) {
        return new SoulChunkEntry(
                tag.getFloat("current"),
                tag.getFloat("max"),
                tag.getLong("time"));
    }
}
