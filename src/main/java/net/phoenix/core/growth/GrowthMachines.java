package net.phoenix.core.growth;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.growth.example.BloomvineCoreMachine;
import net.phoenix.core.growth.example.CrystalGardenMachine;
import net.phoenix.core.growth.example.VerdantBloomMachine;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.blocks;
import static com.gregtechceu.gtceu.api.multiblock.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GrowthMachines {

    public static final MultiblockMachineDefinition CRYSTAL_GARDEN = REGISTRATE
            .multiblock("crystal_garden", CrystalGardenMachine::new)
            .langValue("Crystal Garden")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.GROWTH_RECIPES)
            .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)
            .pattern(definition -> GrowthPatternHelper.getPattern(
                    definition,
                    Predicates.blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)),
                    CrystalGardenMachine.MAX_BOUNDS))

            .model(createWorkableCasingMachineModel(
                    PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                    PhoenixCore.id("block/multiblock/tesla_tower")))
            .register();

    public static final MultiblockMachineDefinition BLOOMVINE_CORE = REGISTRATE
            .multiblock("bloomvine_core", BloomvineCoreMachine::new)
            .langValue("Bloomvine Core")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.GROWTH_RECIPES)
            .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)

            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .slice("CCC", "CCC", "CCC")
                    .slice("CHC", "COC", "CJC")
                    .slice("CCC", "CCC", "CCC")
                    .where(' ', Predicates.any())
                    .where('C', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get()))
                    .where('H', Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                    .where('J', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('O', controller(blocks(definition.get())))
                    .build())

            .model(createWorkableCasingMachineModel(
                    PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                    PhoenixCore.id("block/multiblock/tesla_tower")))
            .register();

    public static final MultiblockMachineDefinition VERDANT_BLOOM = REGISTRATE
            .multiblock("verdant_bloom", VerdantBloomMachine::new)
            .langValue("Verdant Bloom")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.GROWTH_RECIPES)
            .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)

            .pattern(definition -> GrowthPatternHelper.getPattern(
                    definition,
                    Predicates.blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)),
                    VerdantBloomMachine.MAX_BOUNDS))

            .model(createWorkableCasingMachineModel(
                    PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                    PhoenixCore.id("block/multiblock/tesla_tower")))
            .register();

    public static void init() {
        BloomvineCoreMachine.registerTendrilShapes();
        VerdantBloomMachine.registerTendrilShapes();
    }
}
