package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

import net.phoenix.core.common.machine.multiblock.unique.BlazingCleanroom;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum BlazingFilterType implements IFilterType {

    FILTER_CASING_BLAZING("blazing_filter_casing", 1, BlazingCleanroom.BLAZING_CLEANROOM);

    private final String name;
    private final int tier;
    @Getter
    private final CleanroomType cleanroomType;

    BlazingFilterType(String name, int tier, CleanroomType cleanroomType) {
        this.name = name;
        this.tier = tier;
        this.cleanroomType = cleanroomType;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return this.name;
    }

    @NotNull
    @Override
    public String toString() {
        return getSerializedName();
    }
}
