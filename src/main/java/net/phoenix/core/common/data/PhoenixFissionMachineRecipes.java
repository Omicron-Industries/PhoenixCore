package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.integration.phoenix_fission.common.PhoenixFissionMachines;
import net.phoenix.core.integration.phoenix_fission.common.data.block.PhoenixFissionBlocks;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static net.phoenix.core.PhoenixCore.id;
import static net.phoenix.core.common.data.materials.PhoenixFissionMaterials.*;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.FROST;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.MAGMA;

public class PhoenixFissionMachineRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        var assembler = GTRecipeTypes.ASSEMBLER_RECIPES;

        assembler.recipeBuilder("fissile_heat_safe_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 6)
                .inputItems(TagPrefix.frameGt, StainlessSteel, 1)
                .inputItems(TagPrefix.pipeLargeFluid, Aluminium, 2)
                .circuitMeta(2)
                .inputFluids(FROST.getFluid(100))
                .outputItems(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.asItem(), 2)
                .duration(160)
                .EUt(VA[HV])
                .save(provider);

        assembler.recipeBuilder("fissile_reaction_safe_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 3)
                .inputItems(TagPrefix.plate, StainlessSteel, 2)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 1)
                .circuitMeta(6)
                .inputFluids(StainlessSteel.getFluid(250))
                .outputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.asItem(), 2)
                .duration(145)
                .EUt(VA[HV])
                .save(provider);

        assembler.recipeBuilder("fisile_safe_gearbox_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 4)
                .inputItems(TagPrefix.gear, ZIRCALLOY, 2)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 1)
                .circuitMeta(4)
                .inputFluids(Gold.getFluid(1000))
                .outputItems(PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.asItem(), 2)
                .duration(120)
                .EUt(VA[LV])
                .save(provider);

        assembler.recipeBuilder("presurized_fission_reactor")
                .inputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING, 4)
                .inputItems(TagPrefix.pipeLargeFluid, StainlessSteel, 8)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 4)
                .inputItems(CustomTags.HV_CIRCUITS)
                .inputFluids(SolderingAlloy.getFluid(613))
                .outputItems(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR, 1)
                .duration(320)
                .EUt(VA[HV])
                .save(provider);

        assembler.recipeBuilder("heat_exhanger")
                .inputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.asItem(), 2)
                .inputItems(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.asItem(), 8)
                .inputItems(TagPrefix.plateDense, ZIRCALLOY, 2)
                .inputItems(CustomTags.EV_CIRCUITS)
                .inputItems(TagPrefix.pipeLargeFluid, Titanium, 2)
                .inputFluids(SolderingAlloy.getFluid(613))
                .outputItems(PhoenixFissionMachines.HEAT_EXCHANGER.asStack(), 1)
                .duration(180)
                .EUt(VA[EV])
                .save(provider);

        assembler.recipeBuilder("high_performnce_breeder_reactor")
                .inputItems(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack(), 1)
                .inputItems(TagPrefix.rotor, BORON_CARBIDE, 2)
                .inputItems(CustomTags.IV_CIRCUITS)
                .inputItems(TagPrefix.gear, ZIRCALLOY, 2)
                .inputItems(GTMachines.HULL[IV])
                .inputItems(TagPrefix.rod, Uranium238, 16)
                .inputFluids(CRYO_GRAPHITE_BINDING_SOLUTION.getFluid(6000))
                .outputItems(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack(), 1)
                .duration(800)
                .EUt(VA[IV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_BATH_RECIPES.recipeBuilder(id("zirconium_dust_to_hafnium_chloride"))
                .inputItems(TagPrefix.dust, Zirconium, 4)
                .inputFluids(HydrochloricAcid.getFluid(1000))
                .outputFluids(HAFNIUM_CHLORIDE.getFluid(2000))
                .duration(200)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.CENTRIFUGE_RECIPES.recipeBuilder(id("zircon_dust_processing"))
                .inputItems(TagPrefix.dust, ZIRCON, 20)
                .outputItems(TagPrefix.dustImpure, Zirconium, 8)
                .outputItems(TagPrefix.dustImpure, Hafnium, 4)
                .duration(400)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.ELECTROLYZER_RECIPES.recipeBuilder(id("hafnium_chloride"))
                .inputFluids(HAFNIUM_CHLORIDE.getFluid(500))
                .outputFluids(HydrochloricAcid.getFluid(200))
                .outputItems(TagPrefix.dust, Hafnium, 1)
                .duration(100)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.MIXER_RECIPES.recipeBuilder(id("zircalloy_dust"))
                .inputItems(TagPrefix.dust, Zirconium, 5)
                .inputItems(TagPrefix.dustSmall, Bismuth, 1)
                .inputItems(TagPrefix.dust, Hafnium, 2)
                .outputItems(TagPrefix.dust, ZIRCALLOY, 7)
                .circuitMeta(15)
                .duration(100)
                .EUt(VA[HV])
                .save(provider);

        PhoenixRecipeTypes.HEAT_EXCHANGER_RECIPES.recipeBuilder("lava_exchange")
                .inputFluids(Lava.getFluid(100))
                .outputFluids(MAGMA.getFluid(100))
                .notConsumable(TagPrefix.spring, Steel)
                .duration(40)
                .EUt(-VA[EV])
                .save(provider);

        PhoenixRecipeTypes.HEAT_EXCHANGER_RECIPES.recipeBuilder("steam_exchange")
                .inputFluids(Steam.getFluid(2000))
                .outputFluids(DistilledWater.getFluid(250))
                .notConsumable(TagPrefix.spring, Aluminium)
                .duration(20)
                .EUt(-VA[HV])
                .save(provider);

        PhoenixRecipeTypes.HEAT_EXCHANGER_RECIPES.recipeBuilder("sodium_potassium_exchange")
                .inputFluids(HOT_SODIUM_POTASSIUM.getFluid(100))
                .outputFluids(SodiumPotassium.getFluid(100))
                .notConsumable(TagPrefix.spring, Copper)
                .duration(140)
                .EUt(-VA[EV])
                .save(provider);

        PhoenixRecipeTypes.HEAT_EXCHANGER_RECIPES.recipeBuilder("critical_steam_exchange")
                .inputFluids(CRITICAL_STEAM.getFluid(100))
                .outputFluids(Steam.getFluid(100))
                .notConsumable(TagPrefix.spring, RTMAlloy)
                .duration(120)
                .EUt(-VA[EV] * 2L)
                .save(provider);
    }
}
