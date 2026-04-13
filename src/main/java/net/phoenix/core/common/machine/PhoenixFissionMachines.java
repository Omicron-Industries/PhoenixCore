package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.pattern.PhoenixPredicates;
import net.phoenix.core.common.block.PhoenixFissionBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.machine.multiblock.fission.BreederWorkableElectricMultiblockMachine;
import net.phoenix.core.common.machine.multiblock.fission.DynamicFissionReactorMachine;
import net.phoenix.core.common.machine.multiblock.fission.HeatExchangerMachine;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.GTMachines.*;
import static com.gregtechceu.gtceu.common.data.GTMachines.FLUID_EXPORT_HATCH;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Steel;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class PhoenixFissionMachines {

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
    }

    public static final MultiblockMachineDefinition HIGH_PERFORMANCE_BREEDER_REACTOR = REGISTRATE
            .multiblock("high_performance_breeder_reactor", BreederWorkableElectricMultiblockMachine::new)
            .langValue("§bHigh Performance Breeder Reactor")
            .recipeType(PhoenixRecipeTypes.HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES)
            .generator(true)
            .regressWhenWaiting(false)
            .recipeModifiers(BreederWorkableElectricMultiblockMachine::recipeModifier,
                    GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK))
            .appearanceBlock(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBCCCCCBB", "BBDEEEDBB", "BBDEEEDBB", "BBDEEEDBB", "BBDFFFDBB", "BBDFFFDBB", "BBCFFFCBB",
                            "BBCFFFCBB", "BBBBBBBBB")
                    .aisle("BCCCCCCCB", "BGAAAAHGB", "BGAAAAHGB", "BGAAAAHGB", "BGAAAAHGB", "BGAAAAHGB", "BCAAAAHCB",
                            "BCAAAAHCB", "BBCCCCCBB")
                    .aisle("CCCCCCCCC", "DHIAAAIHD", "DHIAAAIHD", "DHIAAAIHD", "DHIAAAIHD", "DHIJJJIHD", "CHIKKKIHC",
                            "CHIAAAIHC", "BCCCCCCCB")
                    .aisle("CCCCCCCCC", "EAALILAAE", "EAALILAAE", "EAALILAAE", "FAALILAAF", "FAJJIJJAF", "FAKKIKKAF",
                            "FAAAIAAAF", "BCCGGGCCB")
                    .aisle("CCCCCCCCC", "EAAIJIAAE", "EAAIJIAAE", "EAAIJIAAE", "FAAIJIAAF", "FAJIJIJAF", "FAKIDIKAF",
                            "FAAIDIAAF", "BCCGJGCCB")
                    .aisle("CCCCCCCCC", "EAALILAAE", "EAALILAAE", "EAALILAAE", "FAALILAAF", "FAJJIJJAF", "FAKKIKKAF",
                            "FAAAIAAAF", "BCCGGGCCB")
                    .aisle("CCCCCCCCC", "DHIAAAIHD", "DHIAAAIHD", "DHIAAAIHD", "DHIAAAIHD", "DHIJJJIHD", "CHIKKKIHC",
                            "CHIAAAIHC", "BCCCCCCCB")
                    .aisle("BCCCCCCCB", "BGHAAAHGB", "BGHAAAHGB", "BGHAAAHGB", "BGHAAAHGB", "BGHAAAHGB", "BCHAAAHCB",
                            "BCHAAAHCB", "BBCCCCCBB")
                    .aisle("BBCCMCCBB", "BBDEEEDBB", "BBDEEEDBB", "BBDEEEDBB", "BBDFFFDBB", "BBDFFFDBB", "BBCFFFCBB",
                            "BBCFFFCBB", "BBBBBBBBB")
                    .where('A', Predicates.air())
                    .where('B', Predicates.any())
                    .where("C", blocks(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.get()).setMinGlobalLimited(10)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.SUBSTATION_OUTPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('D', blocks(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.get()))
                    .where('E', blocks(Blocks.TINTED_GLASS))
                    .where('F', Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                    .where("G", Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                    .where("H", Predicates.blocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
                    .where('I', PhoenixPredicates.fissionModerators().or(PhoenixPredicates.fissionBlankets()))
                    .where("J", Predicates.blocks(COIL_HSSG.get()))
                    .where("K", PhoenixPredicates.fissionCoolers().or(PhoenixPredicates.fissionFuelRods()))
                    .where("L", Predicates.blocks(CASING_TUNGSTENSTEEL_PIPE.get()))
                    .where("M", Predicates.controller(Predicates.blocks(definition.get())))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            PhoenixCore.id("block/fission/fissile_reaction_safe_casing"),
                            GTCEu.id("block/multiblock/fusion_reactor")))
            .register();

    public static final MultiblockMachineDefinition PRESSURIZED_FISSION_REACTOR = REGISTRATE
            .multiblock("pressurized_fission_reactor", DynamicFissionReactorMachine::new)
            .langValue("§bPressurized Fission Reactor")
            .recipeType(PhoenixRecipeTypes.PRESSURIZED_FISSION_REACTOR_RECIPES)
            .generator(true)
            .regressWhenWaiting(false)
            .recipeModifiers(DynamicFissionReactorMachine::recipeModifier,
                    GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK))
            .appearanceBlock(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BCCCB", "CDDDC", "CDDDC", "CDDDC", "BCCCB")
                    .aisle("CEFEC", "DGGGD", "DGGGD", "DGGGD", "CDHDC")
                    .aisle("CFEFC", "DGFGD", "DGFGD", "DGFGD", "CHEHC")
                    .aisle("CEFEC", "DGGGD", "DGGGD", "DGGGD", "CDHDC")
                    .aisle("BCICB", "CDDDC", "CDDDC", "CDDDC", "BCCCB")
                    .where("A", air())
                    .where("B", any())
                    .where("C", blocks(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.get()).setMinGlobalLimited(12)
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where("D", blocks(Blocks.TINTED_GLASS))
                    .where("E", blocks(COIL_KANTHAL.get()))
                    .where('F', PhoenixPredicates.fissionModerators().or(PhoenixPredicates.fissionBlankets()))
                    .where("G", PhoenixPredicates.fissionCoolers().or(PhoenixPredicates.fissionFuelRods()))
                    .where("H", blocks(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.get()))
                    .where("I", Predicates.controller(Predicates.blocks(definition.get())))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            PhoenixCore.id("block/fission/fissile_reaction_safe_casing"),
                            GTCEu.id("block/multiblock/fusion_reactor")))
            .register();

    public static final MultiblockMachineDefinition HEAT_EXCHANGER = REGISTRATE
            .multiblock("heat_exchanger", HeatExchangerMachine::new)
            .langValue("§bHeat Exchanger")
            .rotationState(RotationState.ALL)
            .allowExtendedFacing(true)
            .recipeType(PhoenixRecipeTypes.HEAT_EXCHANGER_RECIPES)
            .recipeModifiers(HeatExchangerMachine::recipeModifier)
            .appearanceBlock(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING)
            .pattern(definition -> FactoryBlockPattern.start(LEFT, UP, BACK)
                    .aisle("ABBBBBA", "BBBCBBB", "BBCCCBB", "BCCGCCB", "BBCCCBB", "BBBCBBB", "ABBBBBA")
                    .aisle("ABCCCBA", "BBAAABB", "CAAEAAC", "CAESEAC", "CAAEAAC", "BBAAABB", "ABCCCBA")
                    .aisle("ABCCCBA", "BBAAABB", "CAAEAAC", "CAESEAC", "CAAEAAC", "BBAAABB", "ABCCCBA")
                    .aisle("ABCCCBA", "BBAAABB", "CAAEAAC", "CAESEAC", "CAAEAAC", "BBAAABB", "ABCCCBA")
                    .aisle("ABCCCBA", "BBAAABB", "CAAEAAC", "CAESEAC", "CAAEAAC", "BBAAABB", "ABCCCBA")
                    .setRepeatable(1, 20)
                    .aisle("ABBBBBA", "BBBCBBB", "BBCCCBB", "BCCCCCB", "BBCCCBB", "BBBCBBB", "ABBBBBA")

                    .where('A', Predicates.any())
                    .where('B', Predicates.blocks(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.get()))
                    .where('C',
                            Predicates.blocks(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.get())
                                    .setMinGlobalLimited(6)
                                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                                    .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY)))
                    .where('E', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, Steel)))
                    .where('S', Predicates.blocks(PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.get()))
                    .where('G', Predicates.controller(Predicates.blocks(definition.get())))
                    .build())
            .workableCasingModel(
                    PhoenixCore.id("block/fission/fissile_heat_safe_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .shapeInfos(definition -> {
                List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();

                for (int length = 1; length <= 20; length++) {
                    MultiblockShapeInfo.ShapeInfoBuilder builder = MultiblockShapeInfo.builder()
                            .where('G', definition, Direction.NORTH)
                            .where('B', PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.getDefaultState())
                            .where('C', PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.getDefaultState())
                            .where('S', PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.getDefaultState())
                            .where('E', ChemicalHelper.getBlock(TagPrefix.frameGt, Steel).defaultBlockState())
                            .where('I', ITEM_IMPORT_BUS[GTValues.LV], Direction.NORTH)
                            .where('O', ITEM_EXPORT_BUS[GTValues.LV], Direction.NORTH)
                            .where('P', FLUID_IMPORT_HATCH[GTValues.LV], Direction.NORTH)
                            .where('Q', FLUID_EXPORT_HATCH[GTValues.LV], Direction.NORTH)
                            .where('Z', ENERGY_OUTPUT_HATCH[GTValues.LV], Direction.SOUTH)
                            .where('A', Blocks.AIR.defaultBlockState())
                            .aisle("ABBBBBA",
                                    "BBBCBBB",
                                    "BBCCCBB",
                                    "BCCGCCB",
                                    "BBCCCBB",
                                    "BBBCBBB",
                                    "ABBBBBA")
                            .aisle("ABCCCBA",
                                    "BBAAABB",
                                    "CAAEAAC",
                                    "CAESEAC",
                                    "CAAEAAC",
                                    "BBAAABB",
                                    "ABCCCBA")
                            .aisle("ABCCCBA",
                                    "BBAAABB",
                                    "CAAEAAC",
                                    "CAESEAC",
                                    "CAAEAAC",
                                    "BBAAABB",
                                    "ABCCCBA")
                            .aisle("ABCCCBA",
                                    "BBAAABB",
                                    "CAAEAAC",
                                    "CAESEAC",
                                    "CAAEAAC",
                                    "BBAAABB",
                                    "ABCCCBA");

                    for (int i = 0; i < length; i++) {
                        builder.aisle("ABCCCBA",
                                "BBAAABB",
                                "CAAEAAC",
                                "CAESEAC",
                                "CAAEAAC",
                                "BBAAABB",
                                "ABCCCBA");
                    }

                    builder.aisle("ABBBBBA",
                            "BBBCBBB",
                            "BBCCCBB",
                            "BCCCCCB",
                            "BBCCCBB",
                            "BBBCBBB",
                            "ABBBBBA");

                    shapeInfos.add(builder.build());
                }

                return shapeInfos;
            })
            .register();

    public static void init() {}
}
