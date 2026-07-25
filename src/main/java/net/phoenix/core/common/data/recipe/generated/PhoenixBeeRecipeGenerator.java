package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.common.data.bees.BeeRecipeData;
import net.phoenix.core.common.data.materials.*;
import net.phoenix.core.common.data.recipe.records.ApisProgenitorConfig;
import net.phoenix.core.common.data.recipe.records.FullBeeConfig;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.HV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static net.phoenix.core.common.data.PhoenixRecipeTypes.*;
import static net.phoenix.core.common.data.bees.BeeRecipeData.MOD_ID;

@SuppressWarnings({ "unused", "removal" })
public class PhoenixBeeRecipeGenerator {

    private static ItemStack safeStack(String id, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    public static void loadBeeRecipes(Consumer<FinishedRecipe> provider) {
        ItemStack honeyCombBase = safeStack("phoenixcore:honey_comb_base", 1);

        if (honeyCombBase.isEmpty()) {
            PhoenixAPI.LOGGER.error("BEE CRITICAL: honey_comb_base not found in registry! Skipping all bee recipes.");
            return;
        }

        generateSimulatedColonyRecipes(provider, honeyCombBase);
        loadBeeCombProductionRecipes(provider, honeyCombBase);
        generateApisProgenitorRecipes(provider);
        generateLumberBeeRecipes(provider, honeyCombBase);
        generateQuarryBeeRecipes(provider, honeyCombBase);
        generateSpecialtyBeeRecipes(provider, honeyCombBase);
    }

    private static TagPrefix tierPrefix(int tier) {
        return switch (tier) {
            case 1 -> PhoenixMaterialFlags.tier_one_bee;
            case 2 -> PhoenixMaterialFlags.tier_two_bee;
            case 3 -> PhoenixMaterialFlags.tier_three_bee;
            default -> PhoenixMaterialFlags.tier_one_bee;
        };
    }

    private static Material getMaterial(String id) {
        Material mat = switch (id) {
            case "fluorite" -> PhoenixOres.FLUORITE;
            case "voidglass_shard" -> PhoenixOres.VOIDGLASS_SHARD;
            case "ignisium" -> PhoenixOres.IGNISIUM;
            case "crystallized_fluxstone" -> PhoenixOres.CRYSTALLIZED_FLUXSTONE;
            case "fluix" -> PhoenixProgressionMaterials.FLUIX;
            case "resonant_ender" -> PhoenixProgressionMaterials.RESONANT_ENDER;
            case "source_gem" -> PhoenixProgressionMaterials.SOURCE_GEM;
            case "prismarine" -> PhoenixProgressionMaterials.PRISMARINE;
            default -> GTRegistries.MATERIALS.get(id);
        };

        if (mat == null && !id.isEmpty()) {
            String cap = id.substring(0, 1).toUpperCase() + id.substring(1);
            mat = GTRegistries.MATERIALS.get(cap);
        }
        return mat;
    }

    private static FluidStack safeFluid(String spec) {
        try {
            String[] parts = spec.split(" ");
            var fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(parts[0]));
            int amount = Integer.parseInt(parts[1]);
            return (fluid == null) ? FluidStack.EMPTY : new FluidStack(fluid, amount);
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    public static void generateSimulatedColonyRecipes(Consumer<FinishedRecipe> provider, ItemStack base) {
        for (FullBeeConfig config : BeeRecipeData.ALL_BEE_CONFIGS.values()) {
            Material mat = getMaterial(config.beeId());
            if (mat == null) continue;

            ItemStack beeCatalyst = ChemicalHelper.get(tierPrefix(config.tier()), mat);
            ItemStack combOutput = ChemicalHelper.get(PhoenixMaterialFlags.honeycomb, mat);
            ItemStack pollinationInput = safeStack(config.pollinationInputId(), 1);

            if (beeCatalyst.isEmpty() || combOutput.isEmpty() || pollinationInput.isEmpty()) continue;

            FluidStack sugarWater = PhoenixBeeMaterials.SUGAR_WATER.getFluid(BeeRecipeData.SUGAR_WATER_AMOUNT);

            SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/simulated_colony/" + config.beeId())
                    .EUt(config.decantingEut()).duration(config.decantingDuration())
                    .notConsumable(beeCatalyst)
                    .inputItems(base)
                    .inputItems(pollinationInput)
                    .inputFluids(sugarWater)
                    .outputItems(combOutput.copyWithCount(4))
                    .save(provider);

            ItemStack crystalRose = ChemicalHelper.get(PhoenixMaterialFlags.crystal_rose, mat);
            if (!crystalRose.isEmpty()) {
                SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/simulated_colony_boosted/" + config.beeId())
                        .EUt(config.boostedDecantingEut()).duration(config.boostedDecantingDuration())
                        .notConsumable(beeCatalyst)
                        .inputItems(base)
                        .inputItems(pollinationInput)
                        .inputFluids(sugarWater)
                        .inputItems(crystalRose.copyWithCount(1))
                        .outputItems(combOutput.copyWithCount(16))
                        .save(provider);
            }
        }
    }

    public static void loadBeeCombProductionRecipes(Consumer<FinishedRecipe> provider, ItemStack base) {
        for (FullBeeConfig config : BeeRecipeData.ALL_BEE_CONFIGS.values()) {
            String beeId = config.beeId();
            Material mat = getMaterial(beeId);
            if (mat == null) continue;

            ItemStack combIn = ChemicalHelper.get(PhoenixMaterialFlags.honeycomb, mat);
            Material honeyedMat = PhoenixBeeMaterials.HONEYED_MATERIALS.get(beeId);
            Material rawWaxMat = PhoenixBeeMaterials.RAW_WAX_MATERIALS.get(beeId);

            if (combIn.isEmpty() || honeyedMat == null || rawWaxMat == null) continue;

            COMB_DECANTING_RECIPES.recipeBuilder(MOD_ID + "/decanting/" + beeId)
                    .EUt(config.decantingEut()).duration(config.decantingDuration())
                    .inputItems(combIn)
                    .outputItems(TagPrefix.dust, rawWaxMat)
                    .outputItems(base)
                    .save(provider);

            BREWING_RECIPES.recipeBuilder(MOD_ID + "/wax_melting/" + beeId)
                    .EUt(config.waxEut()).duration(400)
                    .inputItems(TagPrefix.dust, rawWaxMat)
                    .inputFluids(PhoenixBeeMaterials.WAX_MELTING_CATALYST.getFluid(100))
                    .outputFluids(honeyedMat.getFluid(1000))
                    .save(provider);

            var centrifuge = CENTRIFUGE_RECIPES.recipeBuilder(MOD_ID + "/purifying/" + beeId)
                    .EUt(config.decantingEut()).duration(400)
                    .inputFluids(honeyedMat.getFluid(1000))
                    .outputFluids(PhoenixBeeMaterials.IMPURE_HONEY.getFluid(500));

            if (beeId.equals("water")) {
                centrifuge.outputItems(new ItemStack(Items.SALMON));
            } else if (!config.finalOutputItem().isEmpty()) {
                centrifuge.outputItems(config.finalOutputItem());
            }
            centrifuge.save(provider);
        }
    }

    public static void generateLumberBeeRecipes(Consumer<FinishedRecipe> provider, ItemStack base) {
        ItemStack lumberBee = safeStack("phoenixcore:lumber_bee", 1);
        if (lumberBee.isEmpty()) return;

        for (String logId : BeeRecipeData.LUMBER_LOG_TYPES) {
            ItemStack log = safeStack(logId, 1);
            if (log.isEmpty()) continue;

            SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/lumber/" + logId.replace(":", "_"))
                    .EUt(VA[HV]).duration(250)
                    .notConsumable(lumberBee)
                    .inputItems(base)
                    .inputItems(log)
                    .inputFluids(PhoenixBeeMaterials.SUGAR_WATER.getFluid(100))
                    .outputItems(log.copyWithCount(64))
                    .save(provider);
        }
    }

    public static void generateQuarryBeeRecipes(Consumer<FinishedRecipe> provider, ItemStack base) {
        ItemStack quarryBee = safeStack("phoenixcore:quarry_bee", 1);
        if (quarryBee.isEmpty()) return;

        List<String> stoneIds = List.of("minecraft:cobblestone", "minecraft:deepslate", "minecraft:sand",
                "minecraft:gravel");
        for (String id : stoneIds) {
            ItemStack stone = safeStack(id, 1);
            if (stone.isEmpty()) continue;

            SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/quarry/" + id.replace(":", "_"))
                    .EUt(VA[HV]).duration(800)
                    .notConsumable(quarryBee)
                    .inputItems(base)
                    .inputItems(stone)
                    .inputFluids(PhoenixBeeMaterials.SUGAR_WATER.getFluid(100))
                    .outputItems(stone.copyWithCount(64))
                    .save(provider);
        }
    }

    public static void generateApisProgenitorRecipes(Consumer<FinishedRecipe> provider) {
        for (ApisProgenitorConfig cfg : BeeRecipeData.UNIQUE_APIS_PROGENITOR_CONFIGS) {
            Material inMat = getMaterial(cfg.inputMaterialId());
            Material outMat = getMaterial(cfg.outputMaterialId());
            if (inMat == null || outMat == null) continue;

            ItemStack inBee = ChemicalHelper.get(tierPrefix(cfg.inputTier()), inMat);
            ItemStack outBee = ChemicalHelper.get(tierPrefix(cfg.outputTier()), outMat);

            if (inBee.isEmpty() || outBee.isEmpty()) continue;

            var builder = APIS_PROGENITOR_RECIPES.recipeBuilder(MOD_ID + "/apis_progenitor/" + cfg.id())
                    .EUt(cfg.EUt()).duration(cfg.duration())
                    .inputItems(inBee)
                    .outputItems(outBee);

            for (String itemSpec : cfg.extraItemInputs()) {
                applyExtraInput(builder, itemSpec);
            }
            for (String fluidSpec : cfg.extraFluidInputs()) {
                FluidStack f = safeFluid(fluidSpec);
                if (!f.isEmpty()) builder.inputFluids(f);
            }
            builder.save(provider);
        }
    }

    private static void applyExtraInput(GTRecipeBuilder builder, String spec) {
        if (spec.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(spec.substring(1)));
            builder.inputItems(Ingredient.of(tag), 1);
        } else {
            String[] parts = spec.split("x ");
            int count = parts.length > 1 ? Integer.parseInt(parts[0]) : 1;
            String id = parts.length > 1 ? parts[1] : parts[0];
            ItemStack stack = safeStack(id, count);
            if (!stack.isEmpty()) builder.inputItems(stack);
        }
    }

    public static void generateSpecialtyBeeRecipes(Consumer<FinishedRecipe> provider, ItemStack base) {
        Material source = PhoenixProgressionMaterials.SOURCE_GEM;
        ItemStack catalystBee = ChemicalHelper.get(PhoenixMaterialFlags.tier_one_bee, source);
        ItemStack jelly = safeStack("phoenixcore:royal_jelly", 8);

        if (!catalystBee.isEmpty() && !jelly.isEmpty()) {
            SOURCE_IMBUEMENT_RECIPES.recipeBuilder("source_bee_synthesis")
                    .inputItems(base)
                    .inputItems(jelly)
                    .inputItems(TagPrefix.block, source)
                    .input(SourceRecipeCapability.CAP, new SourceIngredient(10000))
                    .outputItems(catalystBee)
                    .duration(400).EUt(VA[HV])
                    .save(provider);
        }
    }
}
