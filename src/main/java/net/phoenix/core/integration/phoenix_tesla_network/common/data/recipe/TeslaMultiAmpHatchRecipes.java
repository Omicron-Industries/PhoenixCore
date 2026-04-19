package net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;

public class TeslaMultiAmpHatchRecipes {

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        final long[] V = GTValues.V;
        for (int tier = LV; tier <= OpV; tier++) {

            processMultiAmpAssemblerHatch(provider, tier, V);
        }
    }

    private static void processMultiAmpAssemblerHatch(Consumer<FinishedRecipe> provider, int tier, long[] V) {
        if (getQuadWireForTier(tier).isEmpty()) return;

        if (PhoenixTeslaMachines.TESLA_INPUT_4A[tier] != null && PhoenixTeslaMachines.TESLA_INPUT_2A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_4a_" + VN[tier].toLowerCase())
                    .inputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                    .inputItems(getQuadWireForTier(tier), 2)
                    .inputItems(getPlateForTier(tier), 2)
                    .outputItems(PhoenixTeslaMachines.TESLA_INPUT_4A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }

        if (PhoenixTeslaMachines.TESLA_OUTPUT_4A[tier] != null && PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_4a_" + VN[tier].toLowerCase())
                    .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                    .inputItems(getQuadWireForTier(tier), 2)
                    .inputItems(getPlateForTier(tier), 2)
                    .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_4A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }

        if (PhoenixTeslaMachines.TESLA_INPUT_16A[tier] != null && PhoenixTeslaMachines.TESLA_INPUT_4A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_16a_" + VN[tier].toLowerCase())
                    .inputItems(GTMachines.TRANSFORMER[tier])
                    .inputItems(PhoenixTeslaMachines.TESLA_INPUT_4A[tier])
                    .inputItems(getOctalWireForTier(tier), 2)
                    .inputItems(getPlateForTier(tier), 4)
                    .outputItems(PhoenixTeslaMachines.TESLA_INPUT_16A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }

        if (PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier] != null && PhoenixTeslaMachines.TESLA_OUTPUT_4A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_16a_" + VN[tier].toLowerCase())
                    .inputItems(GTMachines.TRANSFORMER[tier])
                    .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_4A[tier])
                    .inputItems(getOctalWireForTier(tier), 2)
                    .inputItems(getPlateForTier(tier), 4)
                    .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }

        if (PhoenixTeslaMachines.TESLA_INPUT_64A[tier] != null && PhoenixTeslaMachines.TESLA_INPUT_16A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_64a_" + VN[tier].toLowerCase())
                    .inputItems(GTMachines.HI_AMP_TRANSFORMER_2A[tier], 1)
                    .inputItems(PhoenixTeslaMachines.TESLA_INPUT_16A[tier])
                    .inputItems(getPlateForTier(tier), 8)
                    .outputItems(PhoenixTeslaMachines.TESLA_INPUT_64A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }

        if (PhoenixTeslaMachines.TESLA_OUTPUT_64A[tier] != null &&
                PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier] != null) {
            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_64a_" + VN[tier].toLowerCase())
                    .inputItems(GTMachines.HI_AMP_TRANSFORMER_2A[tier], 1)
                    .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier])
                    .inputItems(getPlateForTier(tier), 8)
                    .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_64A[tier])
                    .EUt(V[tier]).duration(100).save(provider);
        }
    }
}
