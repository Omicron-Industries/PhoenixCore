package net.phoenix.core.integration.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public interface IFissionFuelRodType {

    @NotNull
    String getName();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    default int getNeutronBias() {
        return 0;
    }

    int getBaseHeatProduction();

    @NotNull
    String getFuelKey();

    @NotNull
    String getOutputKey();

    int getDurationTicks();

    int getAmountPerCycle();

    int getTier();

    ResourceLocation getTexture();

    Lazy<IFissionFuelRodType[]> ALL_FUEL_RODS_BY_HEAT = Lazy.of(() -> PhoenixAPI.FISSION_FUEL_RODS.keySet().stream()
            .sorted(Comparator.comparingInt(IFissionFuelRodType::getBaseHeatProduction))
            .toArray(IFissionFuelRodType[]::new));
}
