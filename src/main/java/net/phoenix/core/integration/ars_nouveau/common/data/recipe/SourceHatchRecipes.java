package net.phoenix.core.integration.ars_nouveau.common.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.common.machine.PhoenixMachines;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.VN;
import static com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SOURCE_JAR;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;

@SuppressWarnings("removal")
public class SourceHatchRecipes {

    private static final int[] VA = GTValues.VA;

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        run(provider);
    }

    public static void run(@NotNull Consumer<FinishedRecipe> provider) {
        // Only run for tiers where you actually have machines defined
        for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
            // Safety: Skip if the machines for this tier don't exist in your registry
            if (PhoenixMachines.SOURCE_EXPORT_HATCH[tier] == null ||
                    PhoenixMachines.SOURCE_IMPORT_HATCH[tier] == null) {
                continue;
            }
            processSourceHatches(provider, tier);
        }
    }

    private static void processSourceHatches(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        var sourceStone = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "sourcestone"));

        // Final safety check: if Ars Nouveau item is missing, don't register the recipe
        if (sourceStone == null || sourceStone.toString().contains("air")) return;

        // Export Hatch Recipe
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("source_hatch_output_" + VN[tier].toLowerCase())
                .inputItems(getSmallGearForTier(tier), 4)
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(sourceStone, 8)
                .inputItems(SOURCE_JAR.get()) // Using .get() for registry objects is safer
                .inputItems(getDrumForTier(tier), 2)
                .inputItems(getCircuitForTier(tier), 2)
                .outputItems(PhoenixMachines.SOURCE_EXPORT_HATCH[tier].asStack())
                .duration(100)
                .EUt(VA[tier])
                .save(provider);

        // Import Hatch Recipe
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("source_hatch_input_" + VN[tier].toLowerCase())
                .inputItems(getGearForTier(tier), 2)
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(sourceStone, 4)
                .inputItems(SOURCE_JAR.get())
                .inputItems(getDrumForTier(tier))
                .inputItems(getCircuitForTier(tier), 2)
                .outputItems(PhoenixMachines.SOURCE_IMPORT_HATCH[tier].asStack())
                .duration(100)
                .EUt(VA[tier])
                .save(provider);
    }
}
