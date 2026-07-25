package net.phoenix.core.integration.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public interface IFissionBlanketType {

    @NotNull
    String getName();

    int getTier();

    default int getRequiredFuelTier() {
        return getTier(); 
    }

    int getDurationTicks();

    int getAmountPerCycle();

    @NotNull
    String getInputKey();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    public record BlanketOutput(String key, int weight, int instability) {}

    List<BlanketOutput> getOutputs();

    @NotNull
    ResourceLocation getTexture();

    Lazy<IFissionBlanketType[]> ALL_BLANKETS_BY_TIER = Lazy.of(() -> PhoenixAPI.FISSION_BLANKETS.keySet().stream()
            .sorted(Comparator.comparingInt(IFissionBlanketType::getTier))
            .toArray(IFissionBlanketType[]::new));
}
