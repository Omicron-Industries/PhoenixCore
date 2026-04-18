package net.phoenix.core.integration.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public interface IFissionModeratorType {

    @NotNull
    String getName();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    int getEUBoost();

    int getFuelDiscount();

    default double getHeatMultiplier() {
        return getTier() * 0.5;
    }

    default int getParallelBonus() {
        return getTier();
    }

    Material getMaterial();

    int getTier();

    ResourceLocation getTexture();

    Lazy<IFissionModeratorType[]> ALL_FISSION_MODERATORS_SORTED = Lazy
            .of(() -> PhoenixAPI.FISSION_MODERATORS.keySet().stream()
                    .sorted(Comparator.comparingInt(IFissionModeratorType::getFuelDiscount))
                    .toArray(IFissionModeratorType[]::new));

    @Nullable
    static IFissionModeratorType getMinRequiredType(int requiredTemperature) {
        return Arrays.stream(ALL_FISSION_MODERATORS_SORTED.get())
                .filter(moderatorType -> moderatorType.getFuelDiscount() >= requiredTemperature)
                .findFirst().orElse(null);
    }
}
