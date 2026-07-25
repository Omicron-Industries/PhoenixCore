package net.phoenix.core.saveddata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UniqueMultiblockData {

    @Getter
    public static class UniqueMultiblockId {

        private final String multiblockType;

        public UniqueMultiblockId(String multiblockType) {
            this.multiblockType = multiblockType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UniqueMultiblockId that)) return false;
            return Objects.equals(multiblockType, that.multiblockType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(multiblockType);
        }
    }

    @Getter
    public static class UniqueMultiblockEntry {

        private final String dimension;
        private final BlockPos pos;

        public UniqueMultiblockEntry(String dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }

    private static final String MULTIBLOCK_TYPE = "multiblockType";
    private static final String MULTIBLOCK_DIMENSION = "dimension";
    private static final String MULTIBLOCK_POS = "pos";

    private final Map<UniqueMultiblockId, UniqueMultiblockEntry> data;

    public UniqueMultiblockData() {
        this.data = new HashMap<>();
    }

    public static UniqueMultiblockData fromTag(ListTag tag) {
        UniqueMultiblockData result = new UniqueMultiblockData();

        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entryTag = tag.getCompound(i);

            String type = entryTag.getString(MULTIBLOCK_TYPE);
            String dimension = entryTag.getString(MULTIBLOCK_DIMENSION);
            BlockPos pos = BlockPos.of(entryTag.getLong(MULTIBLOCK_POS));

            result.data.put(
                    new UniqueMultiblockId(type),
                    new UniqueMultiblockEntry(dimension, pos));
        }

        return result;
    }

    public ListTag toTag() {
        ListTag list = new ListTag();

        for (var entry : data.entrySet()) {
            UniqueMultiblockId id = entry.getKey();
            UniqueMultiblockEntry value = entry.getValue();

            if (id == null || value == null) continue;

            CompoundTag tag = new CompoundTag();
            tag.putString(MULTIBLOCK_TYPE, id.getMultiblockType());
            tag.putString(MULTIBLOCK_DIMENSION, value.getDimension());
            tag.putLong(MULTIBLOCK_POS, value.getPos().asLong());

            list.add(tag);
        }

        return list;
    }

    public boolean hasData(String multiblockType) {
        return data.containsKey(new UniqueMultiblockId(multiblockType));
    }

    public boolean isUnique(String multiblockType, String dimension, BlockPos pos) {
        UniqueMultiblockId key = new UniqueMultiblockId(multiblockType);

        if (!data.containsKey(key)) return true;

        UniqueMultiblockEntry entry = data.get(key);
        return entry.getDimension().equals(dimension) && entry.getPos().equals(pos);
    }

    public void addMultiblock(String multiblockType, String dimension, BlockPos pos) {
        data.put(
                new UniqueMultiblockId(multiblockType),
                new UniqueMultiblockEntry(dimension, pos));
    }

    public void removeMultiblock(String multiblockType, String dimension, BlockPos pos) {
        UniqueMultiblockId key = new UniqueMultiblockId(multiblockType);

        if (!data.containsKey(key)) return;

        UniqueMultiblockEntry entry = data.get(key);

        if (entry.getDimension().equals(dimension) && entry.getPos().equals(pos)) {
            data.remove(key);
        }
    }

    public UniqueMultiblockEntry getEntry(String multiblockType) {
        return data.get(new UniqueMultiblockId(multiblockType));
    }
}
