package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.common.data.bees.BeeRecipeData;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.common.data.materials.PhoenixOres;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;

import java.util.function.Consumer;

import static net.phoenix.core.common.data.materials.PhoenixFissionMaterials.CRYO_GRAPHITE_BINDING_SOLUTION;

public class CrystalRoseAssemblerGenerator {

    public static void generateCrystalRoseRecipes(Consumer<FinishedRecipe> provider) {
        if (CRYO_GRAPHITE_BINDING_SOLUTION == null || CRYO_GRAPHITE_BINDING_SOLUTION == GTMaterials.NULL) return;

        FluidStack crystalRoseFluid = CRYO_GRAPHITE_BINDING_SOLUTION.getFluid(144);
        if (crystalRoseFluid.isEmpty()) return;

        BeeRecipeData.ALL_BEE_CONFIGS.forEach((id, config) -> {

            Material material = getMaterial(id);

            if (material == null || material == GTMaterials.NULL) return;

            ItemStack inputStack = ChemicalHelper.get(TagPrefix.dust, material, 4);

            if (inputStack.isEmpty()) {
                inputStack = ChemicalHelper.get(TagPrefix.gem, material, 4);
            }

            if (inputStack.isEmpty()) return;

            ItemStack roseStack = ChemicalHelper.get(PhoenixMaterialFlags.crystal_rose, material, 1);
            if (roseStack.isEmpty()) return;

            GTRecipeBuilder builder = GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(
                    "phoenixcore:crystal_rose_" + material.getName())
                    .EUt(GTValues.V[GTValues.IV])
                    .duration(200)
                    .inputItems(inputStack)
                    .inputFluids(crystalRoseFluid)
                    .outputItems(roseStack);

            builder.save(provider);
        });
    }

    private static Material getMaterial(String id) {
        if (id == null || id.isEmpty()) return GTMaterials.NULL;

        switch (id) {
            case "fluorite" -> {
                return PhoenixOres.FLUORITE;
            }
            case "voidglass_shard" -> {
                return PhoenixOres.VOIDGLASS_SHARD;
            }
            case "ignisium" -> {
                return PhoenixOres.IGNISIUM;
            }
            case "crystallized_fluxstone" -> {
                return PhoenixOres.CRYSTALLIZED_FLUXSTONE;
            }
            case "fluix" -> {
                return PhoenixProgressionMaterials.FLUIX;
            }
            case "resonant_ender" -> {
                return PhoenixProgressionMaterials.RESONANT_ENDER;
            }
            case "sponge" -> {
                return PhoenixProgressionMaterials.SPONGE;
            }
            case "slime" -> {
                return PhoenixProgressionMaterials.SLIME;
            }
            case "magma" -> {
                return PhoenixProgressionMaterials.MAGMA;
            }
            case "source_gem" -> {
                return PhoenixProgressionMaterials.SOURCE_GEM;
            }
            case "zombie" -> {
                return PhoenixProgressionMaterials.ZOMBIE;
            }
            case "withered" -> {
                return PhoenixProgressionMaterials.WITHERED;
            }
            case "ghostly" -> {
                return PhoenixProgressionMaterials.GHOSTLY;
            }
            case "silky" -> {
                return PhoenixProgressionMaterials.SILKY;
            }
            case "prismarine" -> {
                return PhoenixProgressionMaterials.PRISMARINE;
            }
            case "titanium" -> {
                return GTMaterials.Titanium;
            }
        }

        String normalizedId = id.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        Material mat = GTRegistries.MATERIALS.get(normalizedId);

        if (mat == null || mat == GTMaterials.NULL) {
            mat = GTRegistries.MATERIALS.get(id.toLowerCase());
        }

        return mat != null ? mat : GTMaterials.NULL;
    }

    public static void linkCrystalRoseFlags() {
        BeeRecipeData.ALL_BEE_CONFIGS.forEach((id, config) -> {
            Material material = getMaterial(id);
            if (material != null && material != GTMaterials.NULL) {
                material.addFlags(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE);
            }
        });
    }
}
