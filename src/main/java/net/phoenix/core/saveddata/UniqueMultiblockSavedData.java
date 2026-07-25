package net.phoenix.core.saveddata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UniqueMultiblockSavedData extends SavedData {

    private static final String DATA_NAME = "phoenixcore_unique_multiblock_data";
    private static final String UNIQUE_MULTI_MAPPING = "uniqueMultiMapping";
    private static final String UNIQUE_MULTI_OWNER = "ownerUUID";
    private static final String UNIQUE_MULTI_DATA = "uniqueMultiData";

    private final Map<UUID, UniqueMultiblockData> ownerMap = new HashMap<>();

    public static UniqueMultiblockSavedData getOrCreate(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                UniqueMultiblockSavedData::new,
                UniqueMultiblockSavedData::new,
                DATA_NAME);
    }

    private UniqueMultiblockSavedData() {}

    private UniqueMultiblockSavedData(CompoundTag nbt) {
        ListTag list = nbt.getList(UNIQUE_MULTI_MAPPING, Tag.TAG_COMPOUND);

        for (Tag tag : list) {
            if (!(tag instanceof CompoundTag ownerTag)) continue;

            UUID owner = UUID.fromString(ownerTag.getString(UNIQUE_MULTI_OWNER));
            ListTag dataList = ownerTag.getList(UNIQUE_MULTI_DATA, Tag.TAG_COMPOUND);

            ownerMap.put(owner, UniqueMultiblockData.fromTag(dataList));
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();

        for (var entry : ownerMap.entrySet()) {
            UUID owner = entry.getKey();
            UniqueMultiblockData data = entry.getValue();

            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putString(UNIQUE_MULTI_OWNER, owner.toString());
            ownerTag.put(UNIQUE_MULTI_DATA, data.toTag());

            list.add(ownerTag);
        }

        tag.put(UNIQUE_MULTI_MAPPING, list);
        return tag;
    }

    private UniqueMultiblockData getDataFor(UUID owner) {
        return ownerMap.computeIfAbsent(owner, uuid -> new UniqueMultiblockData());
    }

    public boolean hasData(UUID owner, String multiblockType) {
        return getDataFor(owner).hasData(multiblockType);
    }

    public boolean isUnique(UUID owner, String multiblockType, String dimension, BlockPos pos) {
        return getDataFor(owner).isUnique(multiblockType, dimension, pos);
    }

    public void addMultiblock(UUID owner, String multiblockType, String dimension, BlockPos pos) {
        getDataFor(owner).addMultiblock(multiblockType, dimension, pos);
        setDirty();
    }

    public void removeMultiblock(UUID owner, String multiblockType, String dimension, BlockPos pos) {
        getDataFor(owner).removeMultiblock(multiblockType, dimension, pos);
        setDirty();
    }

    public UniqueMultiblockData.UniqueMultiblockEntry getEntry(UUID owner, String multiblockType) {
        return getDataFor(owner).getEntry(multiblockType);
    }
}
