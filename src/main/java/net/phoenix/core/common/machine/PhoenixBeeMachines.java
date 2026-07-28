package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.machine.multiblock.electric.HoneyCrystallizationChamberMachine;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixBeeMachines {

    public static final MultiblockMachineDefinition HONEY_CRYSTALLIZATION_CHAMBER = REGISTRATE
            .multiblock("honey_crystallization_chamber", HoneyCrystallizationChamberMachine::new)
            .langValue("Honey Crystallization Chamber")
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT, GTRecipeModifiers.PARALLEL_HATCH,
                    HoneyCrystallizationChamberMachine::recipeModifier)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.HONEY_CHAMBER_RECIPES)
            .appearanceBlock(CASING_STAINLESS_CLEAN)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBCBBBBB",
                            "BBBBBCBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBCBBBBB", "BBBBEEEBBBB",
                            "BBBBEEEBBBB", "BBBBBCBBBBB", "BBBBBBBBBBB")
                    .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB", "BBGGAAAGGBB",
                            "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB", "BBGAAAAAGBB",
                            "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB", "BEAAAAAAAEB",
                            "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBCDCBBBB", "BBBBCDCBBBB", "BBCCCCCCCBB", "BCGAACAAGCB", "CEAAACAAAEC",
                            "CEAAACAAAEC", "BCGGACAGGCB", "BBCCCCCCCBB")
                    .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB", "BEAAAAAAAEB",
                            "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB", "BBGAAAAAGBB",
                            "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB", "BBGGAAAGGBB",
                            "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBCCCBBBB", "BBBCEHECBBB",
                            "BBBCEEECBBB", "BBBBCCCBBBB", "BBBBBBBBBBB")
                    .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB",
                            "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")

                    .where('A', Predicates.air())
                    .where('B', Predicates.any())
                    .where('C',
                            Predicates.blocks(ForgeRegistries.BLOCKS
                                    .getValue(ResourceLocation.fromNamespaceAndPath("gtceu", "steel_frame"))))
                    .where('D', Predicates.blocks(CASING_BRONZE_BRICKS.get()))
                    .where('E', Predicates.blocks(CASING_LAMINATED_GLASS.get()))
                    .where('F', Predicates.blocks(CASING_STEEL_SOLID.get()))
                    .where('G', Predicates.blocks(CASING_STAINLESS_CLEAN.get())
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2)
                                    .setMinGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                            .or(Predicates.autoAbilities(true, false, true)))
                    .where('H', Predicates.controller(definition))
                    .build())

            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                            GTCEu.id("block/multiblock/large_miner")))
            .register();

    public static final MultiblockMachineDefinition COMB_DECANTER = REGISTRATE
            .multiblock("comb_decanter", WorkableElectricMultiblockMachine::new)
            .langValue("Comb Decanter")
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT_SUBTICK,
                    GTRecipeModifiers.BATCH_MODE)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.COMB_DECANTING_RECIPES)
            .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("BCDDDCB", "BCDDDCB", "BCDDDCB", "BCDDDCB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB")
                    .slice("CDDDDDC", "CDEAEDC", "CDAAADC", "CDAAADC", "BCFFFCB", "BBGGGBB", "BBFFFBB", "BBDDDBB")
                    .slice("DDDDDDD", "DECACED", "DACACAD", "DACACAD", "BFFAFFB", "BGGAGGB", "BFFAFFB", "BDDDDDB")
                    .slice("DDDDDDD", "DAAAAAD", "DAAAAAD", "DAAAAAD", "BFAAAFB", "BGAAAGB", "BFAAAFB", "BDDHDDB")
                    .slice("DDDDDDD", "DECACED", "DACACAD", "DACACAD", "BFFAFFB", "BGGAGGB", "BFFAFFB", "BDDDDDB")
                    .slice("CDDDDDC", "CDEAEDC", "CDAAADC", "CDAAADC", "BCFFFCB", "BBGGGBB", "BBFFFBB", "BBDDDBB")
                    .slice("BCDDDCB", "BCDJDCB", "BCDDDCB", "BCDDDCB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB")

                    .where('A', Predicates.air())
                    .where('B', Predicates.any())
                    .where('C',
                            Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.TungstenSteel)))
                    .where('D',
                            Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()).setMinGlobalLimited(10)
                                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                    .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                    .or(Predicates.abilities(PhoenixPartAbility.SOURCE_OUTPUT).setMaxGlobalLimited(1))
                                    .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('E', Predicates.blocks(Blocks.HONEY_BLOCK))
                    .where('F', Predicates.blocks(COIL_RTMALLOY.get()))
                    .where('G', Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                    .where('H', Predicates.abilities(PartAbility.MUFFLER))

                    .where('J', Predicates.controller(definition))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/gcym/high_temperature_smelting_casing"),
                            GTCEu.id("block/multiblock/large_miner")))
            .register();

    public static final MultiblockMachineDefinition SWARM_NURTURER = REGISTRATE
            .multiblock("swarm_nurturer", WorkableElectricMultiblockMachine::new)
            .langValue("Swarm Nurturer")
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.SWARM_NURTURING_RECIPES)
            .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("BCCCB", "BDDDB", "BDDDB", "BCCCB")
                    .slice("CBBBC", "DEEED", "DAAAD", "CEEEC")
                    .slice("CBBBC", "DFFFD", "DAAAD", "CFFFC")
                    .slice("CBBBC", "DEEED", "DAAAD", "CEEEC")
                    .slice("CBBBC", "CBGBC", "CBBBC", "CCCCC")

                    .where('A', Predicates.air())

                    .where('B', Predicates.blocks(CASING_STAINLESS_CLEAN.get()).setMinGlobalLimited(2)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.controller(definition)))
                    .where('C',
                            Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.StainlessSteel)))
                    .where('D', Predicates.blocks(GCYMBlocks.MOLYBDENUM_DISILICIDE_COIL_BLOCK.get()))
                    .where('E', Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Steel)))
                    .where('F', Predicates.blocks(CASING_STAINLESS_STEEL_GEARBOX.get()))

                    .where('G', Predicates.controller(definition))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                            GTCEu.id("block/multiblock/large_miner")))
            .register();

    public static final MultiblockMachineDefinition APIS_PROGENITOR = REGISTRATE
            .multiblock("apis_progenitor", WorkableElectricMultiblockMachine::new)
            .langValue("Apis Progenitor")
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.APIS_PROGENITOR_RECIPES)
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .slice("BBBBB", "CDDDC", "CDDDC", "CDDDC", "EFFFE")
                    .slice("BGGGB", "DHHHD", "DAAAD", "DAAAD", "FBBBF")
                    .slice("BGGGB", "DHHHD", "DAAAD", "DAAAD", "FBBBF")
                    .slice("BGGGB", "DHHHD", "DAAAD", "DAAAD", "FBBBF")
                    .slice("BBIBB", "CDDDC", "CDDDC", "CDDDC", "EFFFE")

                    .where('A', Predicates.air())

                    .where('B', Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()).setMinGlobalLimited(2)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.controller(definition)))
                    .where('C',
                            Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.TungstenSteel)))
                    .where('D', Predicates.blocks(CASING_TEMPERED_GLASS.get()))
                    .where('E', Predicates.any())
                    .where('F', Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.TreatedWood)))
                    .where('G', Predicates.blocks(Blocks.DIRT))
                    .where('H', Predicates.blocks(Blocks.POPPY))

                    .where('I', Predicates.controller(definition))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                            PhoenixCore.id("block/multiblock/apis_progenitor")))
            .register();

    public static final MultiblockMachineDefinition SIMULATED_COLONY = REGISTRATE
            .multiblock("simulated_colony", WorkableElectricMultiblockMachine::new)
            .langValue("Simulated Colony")
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.BATCH_MODE)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.SIMULATED_COLONY_RECIPES)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("BCDDDCB", "BCEEECB", "BCEEECB", "BCEEECB", "BBBBBBB")
                    .slice("CDFFFDC", "CGAHAGC", "CGAIAGC", "CGAAAGC", "BBFFFBB")
                    .slice("DFFFFFD", "EAAAAAE", "EAAAAAE", "EAAAAAE", "BFFFFFB")
                    .slice("DFFFFFD", "EHAHAHE", "EIAIAIE", "EAAAAAE", "BFFFFFB")
                    .slice("DFFFFFD", "EAAAAAE", "EAAAAAE", "EAAAAAE", "BFFFFFB")
                    .slice("CDFFFDC", "CGAHAGC", "CGAIAGC", "CGAAAGC", "BBFFFBB")
                    .slice("BCDJDCB", "BCEEECB", "BCEEECB", "BCEEECB", "BBBBBBB")

                    .where('A', Predicates.air())
                    .where('B', Predicates.any())
                    .where('C', Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.TreatedWood)))

                    .where('D', Predicates.blocks(CASING_STEEL_SOLID.get()).setMinGlobalLimited(2)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.controller(definition)))

                    .where('E', Predicates.blocks(CASING_TEMPERED_GLASS.get()))
                    .where('F', Predicates.blocks(TREATED_WOOD_PLANK.get()))
                    .where('G', Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Steel)))
                    .where('H', Predicates.blocks(Blocks.DIRT))
                    .where('I', Predicates.blocks(Blocks.POPPY))

                    .where('J', Predicates.controller(definition))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                            GTCEu.id("block/multiblock/large_miner")))
            .register();

    public static void init() {}
}
