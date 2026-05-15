package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.PhoenixBeeMaterials;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.common.data.materials.PhoenixOres;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipeConditons.SoulCondition;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SOURCE_GEM_BLOCK;
import static com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.SOURCE_GEM;
import static net.phoenix.core.common.data.PhoenixRecipeTypes.*;
import static net.phoenix.core.common.data.item.PhoenixItems.*;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.*;

@SuppressWarnings("removal")
public class PhoenixMachineRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        /*
         * if (PhoenixConfigs.INSTANCE.features.creativeEnergyEnabled) {
         * ASSEMBLY_LINE_RECIPES.recipeBuilder("dance")
         * .inputItems(GTMultiMachines.ACTIVE_TRANSFORMER)
         * .inputItems(TagPrefix.plate, Neutronium, 32)
         * .inputItems(SENSOR.get(UV), 8)
         * .inputItems(EMITTER.get(UV), 8)
         * .addCondition(FluidInHatchCondition.of("PhoenixCore:quantum_coolant_plasma"))
         * .inputItems(FIELD_GENERATOR.get(UV), 4)
         * .inputItems(CustomTags.UHV_CIRCUITS, 2)
         * .inputItems(TagPrefix.pipeLargeFluid, Neutronium, 4)
         * .inputItems(CABLE_QUAD.get(UV), 8)
         * .inputFluids(SolderingAlloy.getFluid(L * 32))
         * .EUt(VA[LV]).duration(400)
         * .duration(1200)
         * .outputItems(PhoenixMachines.DANCE)
         * .stationResearch(b -> b
         * .researchStack(GTMultiMachines.ACTIVE_TRANSFORMER.asStack()).CWUt(16))
         * .save(provider);
         * }
         * if (PhoenixConfigs.INSTANCE.features.recipesEnabled) {
         * if (ConfigHolder.INSTANCE.machines.high1Content) {
         * ASSEMBLY_LINE_RECIPES.recipeBuilder("high_performance_computing_array")
         * .inputItems(DATA_BANK)
         * .inputItems(CustomTags.ZPM_CIRCUITS, 4)
         * .inputItems(FIELD_GENERATOR_LuV, 8)
         * .inputItems(TOOL_DATA_ORB)
         * .inputItems(COVER_SCREEN)
         * .inputItems(wireGtDouble, UraniumRhodiumDinaquadide, 64)
         * .inputItems(OPTICAL_PIPES[0].asStack(16))
         * .inputFluids(SolderingAlloy, L * 8)
         * .inputFluids(VanadiumGallium, L * 8)
         * .inputFluids(PCBCoolant, 4000)
         * .outputItems(HIGH_PERFORMANCE_COMPUTING_ARRAY)
         * .scannerResearch(b -> b
         * .researchStack(COVER_SCREEN.asStack())
         * .duration(2400)
         * .EUt(VA[IV]))
         * .duration(1200).EUt(100000)
         * .addMaterialInfo(true, true).save(provider);
         * ASSEMBLER_RECIPES.recipeBuilder("hpca_computation_component")
         * .inputItems(HPCA_EMPTY_COMPONENT)
         * .inputItems(CustomTags.ZPM_CIRCUITS, 4)
         * .inputItems(FIELD_GENERATOR_LuV)
         * .outputItems(HPCA_COMPUTATION_COMPONENT)
         * .inputFluids(PCBCoolant, 1000)
         * .cleanroom(CleanroomType.CLEANROOM)
         * .duration(200).EUt(VA[LuV])
         * .addMaterialInfo(true).save(provider);
         * 
         * ASSEMBLER_RECIPES.recipeBuilder("hpca_advanced_computation_component")
         * .inputItems(PHOENIX_COMPUTATION_COMPONENT)
         * .inputItems(CustomTags.UEV_CIRCUITS, 4)
         * .inputItems(FIELD_GENERATOR_UEV)
         * .outputItems(ADVANCED_PHOENIX_COMPUTATION_COMPONENT)
         * .inputFluids(PCBCoolant, 10000)
         * .cleanroom(CleanroomType.CLEANROOM)
         * .duration(200).EUt(VA[UEV])
         * .addMaterialInfo(true).save(provider);
         * ASSEMBLER_RECIPES.recipeBuilder("hpca_heat_sink_component")
         * .inputItems(HPCA_EMPTY_COMPONENT)
         * .inputItems(plate, Aluminium, 32)
         * .inputItems(screw, StainlessSteel, 8)
         * .outputItems(HPCA_HEAT_SINK_COMPONENT)
         * .inputFluids(PCBCoolant, 1000)
         * .cleanroom(CleanroomType.CLEANROOM)
         * .duration(200).EUt(VA[IV])
         * .addMaterialInfo(true).save(provider);
         * 
         * ASSEMBLER_RECIPES.recipeBuilder("hpca_active_cooler_component")
         * .inputItems(ADVANCED_COMPUTER_CASING.asStack())
         * .inputItems(plate, Aluminium, 16)
         * .inputItems(pipeTinyFluid, StainlessSteel, 16)
         * .inputItems(screw, StainlessSteel, 8)
         * .outputItems(HPCA_ACTIVE_COOLER_COMPONENT)
         * .inputFluids(PCBCoolant, 1000)
         * .cleanroom(CleanroomType.CLEANROOM)
         * .duration(200).EUt(VA[IV])
         * .addMaterialInfo(true).save(provider);
         * }
         * }
         * PhoenixRecipeTypes.PLEASE.recipeBuilder("plasma_furnace_shield_activation")
         * 
         * .input(SHIELDTYPES, INACTIVE)
         * 
         * .inputItems(TagPrefix.plate, Neutronium, 16)
         * .inputItems(FIELD_GENERATOR.get(UV), 4)
         * .inputFluids(GTMaterials.SolderingAlloy.getFluid(L * 32))
         * 
         * .addData("shield_activation", true)
         * .addData("updated_shield_key", NORMAL.key)
         * 
         * .EUt(VA[UV])
         * .duration(1000)
         * .save(provider);
         * 
         * PhoenixRecipeTypes.PLEASE.recipeBuilder("please")
         * .inputFluids(GTMaterials.SolderingAlloy.getFluid(GTValues.L * 32))
         * .duration(1200)
         * .EUt(VA[LV]).duration(400)
         * .inputFluids(Acetone.getFluid(GTValues.L * 32))
         * .save(provider);
         * PhoenixRecipeTypes.PLEASE.recipeBuilder("modify")
         * .inputFluids(Neutronium.getFluid(1))
         * .duration(1200)
         * .EUt(VA[LV]).duration(400)
         * .save(provider);
         * 
         * 
         * PhoenixRecipeTypes.PLEASE.recipeBuilder("plasma_test")
         * .inputFluids(Argon.getFluid(FluidStorageKeys.PLASMA, 100))
         * .inputFluids(Water.getFluid(L * 16))
         * .addCondition(FluidInHatchCondition.of("PhoenixCore:quantum_coolant_plasma"))
         * .duration(600)
         * .EUt(VA[LV]).duration(400)
         * .EUt(ZPM * 2)
         * .outputItems(PhoenixMachines.DANCE)
         * .save(provider);
         * 
         */
        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("carbon_and_helium_3_to_oxygen_plasma")
                .inputFluids(GTMaterials.Carbon.getFluid(16))
                .inputFluids(GTMaterials.Helium3.getFluid(125))
                .outputFluids(GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(32)
                .EUt(4096)
                .fusionStartEU(180_000_000)
                .save(provider);
        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("deuterium_and_tritium_to_helium_plasma")
                .inputFluids(Deuterium.getFluid(125))
                .inputFluids(Tritium.getFluid(125))
                .outputFluids(Helium.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(16)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(2))
                .EUt(4096)
                .fusionStartEU(40_000_000)
                .save(provider);
        PhoenixRecipeTypes.COMB_DECANTING_RECIPES.recipeBuilder("deuterium_and_tritium_to_helium_plasma")
                .inputFluids(Deuterium.getFluid(125))
                .outputFluids(Helium.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(16)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(2))
                .EUt(4096)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("carbon_and_helium_3_to_oxygen_plasma")
                .inputFluids(GTMaterials.Carbon.getFluid(16))
                .inputFluids(GTMaterials.Helium3.getFluid(125))
                .outputFluids(GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(32)
                .EUt(4096)
                .fusionStartEU(180_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("beryllium_and_deuterium_to_nitrogen_plasma")
                .inputFluids(GTMaterials.Beryllium.getFluid(16))
                .inputFluids(GTMaterials.Deuterium.getFluid(375))
                .outputFluids(GTMaterials.Nitrogen.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(16)
                .EUt(16384)
                .fusionStartEU(280_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("silicon_and_magnesium_to_iron_plasma")
                .inputFluids(GTMaterials.Silicon.getFluid(16))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA, 16))
                .duration(32)
                .EUt(VA[IV])
                .fusionStartEU(360_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("potassium_and_fluorine_to_nickel_plasma")
                .inputFluids(GTMaterials.Potassium.getFluid(16))
                .inputFluids(GTMaterials.Fluorine.getFluid(125))
                .outputFluids(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA, 16))
                .duration(16)
                .EUt(VA[LuV])
                .fusionStartEU(480_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("carbon_and_magnesium_to_argon_plasma")
                .inputFluids(GTMaterials.Carbon.getFluid(16))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Argon.getFluid(FluidStorageKeys.PLASMA, 125))
                .duration(32)
                .EUt(24576)
                .fusionStartEU(180_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("neodymium_and_hydrogen_to_europium_plasma")
                .inputFluids(GTMaterials.Neodymium.getFluid(16))
                .inputFluids(GTMaterials.Hydrogen.getFluid(375))
                .outputFluids(GTMaterials.Europium.getFluid(16))
                .duration(64)
                .EUt(24576)
                .fusionStartEU(150_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("lutenium_and_chromium_to_americium_plasma")
                .inputFluids(GTMaterials.Lutetium.getFluid(16))
                .inputFluids(GTMaterials.Chromium.getFluid(16))
                .outputFluids(GTMaterials.Americium.getFluid(16))
                .duration(64)
                .EUt(49152)
                .fusionStartEU(200_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("americium_and_naquadria_to_neutronium_plasma")
                .inputFluids(GTMaterials.Americium.getFluid(128))
                .inputFluids(GTMaterials.Naquadria.getFluid(128))
                .outputFluids(GTMaterials.Neutronium.getFluid(32))
                .duration(200)
                .EUt(98304)
                .fusionStartEU(600_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("silver_and_copper_to_osmium_plasma")
                .inputFluids(GTMaterials.Silver.getFluid(16))
                .inputFluids(GTMaterials.Copper.getFluid(16))
                .outputFluids(GTMaterials.Osmium.getFluid(16))
                .duration(64)
                .EUt(24578)
                .fusionStartEU(150_000_000)
                .save(provider);

        PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1.recipeBuilder("mercury_and_magnesium_to_uranium_235_plasma")
                .inputFluids(GTMaterials.Mercury.getFluid(125))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Uranium235.getFluid(16))
                .duration(128)
                .EUt(24576)
                .fusionStartEU(140_000_000)
                .save(provider);

        PhoenixRecipeTypes.HONEY_CHAMBER_RECIPES.recipeBuilder("mercury_and_magnesium_to_uranium_235_plasma")
                .inputFluids(GTMaterials.Mercury.getFluid(125))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Uranium235.getFluid(16))
                .duration(1000028)
                .EUt(24576)
                .save(provider);

        CIRCUIT_ASSEMBLER_RECIPES.recipeBuilder("electronic_circuit_mv_universal").EUt(VA[LV]).duration(300)
                .inputItems(GOOD_CIRCUIT_BOARD)
                .inputItems(CustomTags.LV_CIRCUITS, 2)
                .inputItems(CustomTags.DIODES, 2)
                .inputItems(wireGtSingle, Copper, 2)
                .outputItems(ELECTRONIC_CIRCUIT_MV)
                .save(provider);

        /*
         * PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES.recipeBuilder("soul_infused_zirconium_rod")
         * .input(SourceRecipeCapability.CAP, new SourceIngredient(1000))
         * .addCondition(new SoulCondition(false, 3.0f))
         * .addData("soul_growth_perm", 0.01f)
         * .addData("soul_growth_temp", 0.5f)
         * .save(provider);
         * 
         */

        PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES.recipeBuilder("source_imbued_titanium")
                .inputItems(ingot, Titanium, 1)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(260))
                .duration(160)
                .EUt(GTValues.VA[GTValues.HV] / 3)
                .outputItems(ingot, SOURCE_IMBUED_TITANIUM, 1)
                // .addData("soul_growth_perm", 0.01f)
                // .addData("soul_growth_temp", 0.5f)
                .addCondition(new SoulCondition(false, 0.5f))
                .save(provider);
        PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES.recipeBuilder("source_gem")
                .inputItems(gem, Amethyst, 1)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(200))
                .duration(40)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .outputItems(SOURCE_GEM, 2)
                .save(provider);
        SOURCE_IMBUEMENT_RECIPES.recipeBuilder("soul_lens")
                .inputItems(lens, Amethyst, 1)
                .inputItems(ring, Gold, 2)
                .inputItems(PhoenixItems.SOURCE_FIBER_MESH)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(400))
                .duration(400)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .outputItems(SOUL_LENS, 1)
                .save(provider);
        PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES.recipeBuilder("source_gem_without_source")
                .inputItems(gem, Amethyst, 1)
                .duration(1500)
                .circuitMeta(2)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .outputItems(SOURCE_GEM, 1)
                .save(provider);
        PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES.recipeBuilder("85_percent_pure_nevonian_steel_cooling")
                .inputItems(ingotHot, EightyFivePercentPureNevonianSteel, 1)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(250))
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .outputItems(ingot, EightyFivePercentPureNevonianSteel, 1)
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_wheat")
                .inputItems(new ItemStack(Items.WHEAT), 3)
                .circuitMeta(1)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(100))
                .duration(210)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_flowers")
                .inputItems(PTags.FLOWERS, 3)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(140))
                .duration(170)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_crops")
                .inputItems(PTags.CROPS, 3)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(140))
                .duration(180)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_mushrooms")
                .inputItems(PTags.MUSHROOMS, 4)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(120))
                .duration(165)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_coal")
                .inputItems(gem, Coal, 2)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(110))
                .duration(240)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_coke")
                .inputItems(gem, Coke, 1)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(110))
                .duration(270)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_charcoal")
                .inputItems(gem, Charcoal, 2)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(120))
                .duration(230)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_logs")
                .inputItems(PTags.LOGS, 4)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(120))
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_planks")
                .inputItems(PTags.PLANKS, 4)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(700))
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
        PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.recipeBuilder("source_from_lava")
                .inputFluids(Lava, 1000)
                .output(SourceRecipeCapability.CAP, new SourceIngredient(110))
                .duration(260)
                .EUt(GTValues.VA[GTValues.HV] / 3)
                .save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("honey_reduction_to_jelly")
                .inputFluids(PhoenixBeeMaterials.HONEY.getFluid(2000))
                .outputItems(PhoenixItems.ROYAL_JELLY)
                .duration(400)
                .EUt(VA[MV])
                .save(provider);

        APIS_PROGENITOR_RECIPES.recipeBuilder("synth_resonant_ender")
                .inputItems(HONEY_COMB_BASE)
                .inputItems(PhoenixItems.ROYAL_JELLY, 4)
                .inputItems(TagPrefix.block, GTMaterials.EnderPearl, 4)
                .outputItems(PhoenixMaterialFlags.tier_one_bee, RESONANT_ENDER)
                .duration(600).EUt(VA[IV])
                .save(provider);

        APIS_PROGENITOR_RECIPES.recipeBuilder("synth_iron")
                .inputItems(HONEY_COMB_BASE)
                .inputItems(PhoenixItems.ROYAL_JELLY, 4)
                .inputItems(new ItemStack(Items.IRON_BLOCK, 4))
                .outputItems(PhoenixMaterialFlags.tier_one_bee, Iron)
                .duration(600).EUt(VA[IV])
                .save(provider);

        APIS_PROGENITOR_RECIPES.recipeBuilder("synth_coal")
                .inputItems(HONEY_COMB_BASE)
                .inputItems(PhoenixItems.ROYAL_JELLY, 4)
                .inputItems(new ItemStack(Items.COAL_BLOCK, 4))
                .outputItems(PhoenixMaterialFlags.tier_one_bee, GTMaterials.Coal)
                .duration(400).EUt(VA[IV])
                .save(provider);

        APIS_PROGENITOR_RECIPES.recipeBuilder("synth_quarry")
                .inputItems(HONEY_COMB_BASE)
                .inputItems(PhoenixItems.ROYAL_JELLY, 4)
                .inputItems(TagPrefix.block, GTMaterials.Stone, 16)
                .outputItems(PhoenixItems.QUARRY_BEE)
                .duration(400).EUt(VA[IV])
                .save(provider);

        APIS_PROGENITOR_RECIPES.recipeBuilder("synth_lumber")
                .inputItems(HONEY_COMB_BASE)
                .inputItems(PhoenixItems.ROYAL_JELLY, 4)
                .inputItems(PTags.LOGS, 16)
                .outputItems(PhoenixItems.LUMBER_BEE)
                .duration(400).EUt(VA[IV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("alchemical_imbuer")
                .inputItems(SOURCE_GEM_BLOCK, 4)
                .inputItems(pipeLargeFluid, StainlessSteel, 4)
                .inputItems(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "sourcestone")), 8)
                .inputItems(CASING_STAINLESS_CLEAN, 4)
                .inputItems(CustomTags.HV_CIRCUITS, 2)
                .outputItems(PhoenixMachines.ALCHEMICAL_IMBUER)
                .inputFluids(SolderingAlloy, 613)
                .duration(250)
                .EUt(VA[HV] / 2)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("bio_aetheric_engine")
                .inputItems(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "sourcestone")), 32)
                .inputItems(SOURCE_GEM_BLOCK, 12)
                .inputItems(pipeLargeFluid, SOURCE_IMBUED_TITANIUM, 2)
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputItems(CASING_TITANIUM_STABLE, 2)
                .inputItems(SOURCE_FIBERS, 8)
                .inputItems(gear, FROST_REINFORCED_STAINED_STEEL, 2)
                .outputItems(PhoenixMachines.BIO_AETHERIC_ENGINE)
                .inputFluids(SolderingAlloy, 613)
                .duration(300)
                .EUt(VA[HV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("source_reactor")
                .inputItems(SOURCE_GEM_BLOCK, 8)
                .inputItems(pipeNormalFluid, SOURCE_IMBUED_TITANIUM, 4)
                .inputItems(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "sourcestone")), 8)
                .inputItems(CustomTags.EV_CIRCUITS, 2)
                .inputItems(frameGt, SOURCE_IMBUED_TITANIUM, 1)
                .inputItems(CASING_TITANIUM_STABLE, 2)
                .inputItems(rotor, Titanium, 1)
                .outputItems(PhoenixMachines.SOURCE_REACTOR)
                .inputFluids(SolderingAlloy, 613)
                .duration(240)
                .EUt(VA[HV])
                .save(provider);

        SOURCE_REACTOR_RECIPES.recipeBuilder("eighty_five_percent_pure_nevonian_steel_dust")
                .inputItems(gemExquisite, Coke, 4)
                .inputItems(dust, Diamond, 16)
                .inputItems(dust, PhoenixOres.NEVVONIAN_IRON, 2)
                .inputFluids(Titanium, 250)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(1000))
                .outputFluids(TitaniumTetrachloride.getFluid(100))
                .outputItems(dust, EightyFivePercentPureNevonianSteel, 4)
                .duration(800)
                .EUt(GTValues.VA[GTValues.EV])
                .save(provider);
        BIO_ENGINE_RECIPES.recipeBuilder("eighty_five_percent_pure_nevonian_steel_dust")
                .notConsumable(SOURCE_FIBERS)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(200))
                .outputFluids(FROST.getFluid(20))
                .duration(140)
                .EUt(-VA[HV] * 2L)
                .save(provider);

        SOURCE_REACTOR_RECIPES.recipeBuilder("source_titanium_filament_alloy")
                .inputItems(dust, Titanium, 4)
                .inputItems(SOURCE_GEM, 64)
                .inputItems(dust, Molybdenum, 2)
                .input(SourceRecipeCapability.CAP, new SourceIngredient(2200))
                .outputItems(dust, SOURCE_TITANIUM_FILAMENT, 22)
                .duration(200)
                .EUt(VA[EV])
                .save(provider);

        SOURCE_REACTOR_RECIPES.recipeBuilder("voidic_drilling_fluid_recipe_base")
                .inputFluids(DrillingFluid.getFluid(500))
                .inputFluids(FROST.getFluid(250))
                .inputFluids(CRYO_EMBER_FLUID.getFluid(25000))
                .input(SourceRecipeCapability.CAP, new SourceIngredient(1400))
                .inputItems(dust, PhoenixOres.PERMAFROST, 1)
                .inputItems(dust, EnderEye, 1)
                .outputFluids(VOIDIC_DRILLING_FLUID.getFluid(1000))
                .duration(300)
                .EUt(VA[EV] / 2)
                .save(provider);

        SOURCE_REACTOR_RECIPES.recipeBuilder("voidic_drilling_fluid_recipe_better")
                .inputFluids(DrillingFluid.getFluid(500))
                .input(SourceRecipeCapability.CAP, new SourceIngredient(1400))
                .inputItems(dust, PhoenixOres.CRYSTALLIZED_FLUXSTONE, 1)
                .outputFluids(VOIDIC_DRILLING_FLUID.getFluid(3000))
                .duration(300)
                .EUt(VA[EV] / 2)
                .save(provider);
        CHEMICAL_RECIPES.recipeBuilder("butraldehyde")
                .circuitMeta(4)
                .inputFluids(Propene.getFluid(1000))
                .inputFluids(Hydrogen.getFluid(2000))
                .inputFluids(CarbonMonoxide.getFluid(1000))
                .outputFluids(Butyraldehyde.getFluid(1000))
                .duration(200).EUt(VA[HV]).save(provider);

        FORMING_PRESS_RECIPES.recipeBuilder("source_gem_to_fiber")
                .inputItems(SOURCE_GEM)
                .notConsumable(SHAPE_MOLD_SMALL_PIPE)
                .outputItems(SOURCE_FIBERS, 2)
                .duration(110)
                .EUt(VA[MV])
                .save(provider);

        COMPRESSOR_RECIPES.recipeBuilder("source_fibers_to_mesh")
                .inputItems(SOURCE_FIBERS, 2)
                .outputItems(PhoenixItems.SOURCE_FIBER_MESH)
                .duration(80)
                .EUt(VA[LV])
                .save(provider);

        JUKEBLOCK.recipeBuilder("source_fibers_to_mesh")
                .inputItems(SOURCE_FIBERS, 2)
                .outputItems(PhoenixItems.SOURCE_FIBER_MESH)
                .duration(80)
                .EUt(VA[LV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("source_fiber_machine_casing")
                .inputItems(CARBON_MESH, 4)
                .inputItems(frameGt, FROST_REINFORCED_STAINED_STEEL)
                .inputItems(PhoenixItems.SOURCE_FIBER_MESH, 4)
                .outputItems(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING, 2)
                .duration(75)
                .EUt(VA[HV])
                .save(provider);
    }
}
