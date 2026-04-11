package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

import org.jetbrains.annotations.NotNull;

public class BlazingCleanroom extends CleanroomType {

    public static final CleanroomType BLAZING_CLEANROOM = new CleanroomType(
            "blazing_cleanroom",
            "gtceu.recipe.cleanroom_blazing.display_name");

    /**
     * Constructs a new CleanroomType instance.
     * 
     * @param name           The unique name of the cleanroom type.
     * @param translationKey The translation key for the cleanroom's display name.
     */
    public BlazingCleanroom(@NotNull String name, @NotNull String translationKey) {
        super(name, translationKey);
    }
}
