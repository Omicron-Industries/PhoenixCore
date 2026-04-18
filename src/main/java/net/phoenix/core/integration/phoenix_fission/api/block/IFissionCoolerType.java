package net.phoenix.core.integration.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public interface IFissionCoolerType {

    @NotNull
    String getName();

    int getTier();

    int getCoolerTemperature();

    @NotNull
    String getRequiredCoolantMaterialId();

    @NotNull
    default String getOutputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    @NotNull
    default String getInputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    /**
     * Defines how much coolant is consumed per tick (mB/t).
     */
    int getCoolantUsagePerTick();

    default int getCoolantPerTick() {
        return getCoolantUsagePerTick();
    }

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    Material getMaterial();

    ResourceLocation getTexture();

    Lazy<IFissionCoolerType[]> ALL_COOLER_TEMPERATURES_SORTED = Lazy
            .of(() -> PhoenixAPI.FISSION_COOLERS.keySet().stream()
                    .sorted(Comparator.comparingInt(IFissionCoolerType::getCoolerTemperature))
                    .toArray(IFissionCoolerType[]::new));

    @Nullable
    static IFissionCoolerType getMinRequiredType(int requiredTemperature) {
        return Arrays.stream(ALL_COOLER_TEMPERATURES_SORTED.get())
                .filter(cooler -> cooler.getCoolerTemperature() >= requiredTemperature)
                .findFirst()
                .orElse(null);
    }
}
