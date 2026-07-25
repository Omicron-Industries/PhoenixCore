package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

import org.jetbrains.annotations.NotNull;

public class BlazingCleanroom extends CleanroomType {

    public static final CleanroomType BLAZING_CLEANROOM = new CleanroomType(
            "blazing_cleanroom",
            2,
            "gtceu.recipe.cleanroom_blazing.display_name");

    public BlazingCleanroom(@NotNull String name, int tier, @NotNull String translationKey) {
        super(name, tier, translationKey);
    }
}
