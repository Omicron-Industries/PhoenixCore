package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.machine.multiblock.api.PhoenixMultiblockProperties;
import net.phoenix.core.common.machine.multiblock.api.PhoenixPartAppearance;
import net.phoenix.core.common.machine.multiblock.api.PhoenixPartAppearance.AppearanceResult;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class AetherCrucibleMachines {

    public static final PartAbility AETHER_CATALYST_ABILITY = new PartAbility("aether_catalyst");

    public static final MachineDefinition AETHER_CATALYST_HATCH_EV = registerCatalystHatch(GTValues.EV);
    public static final MachineDefinition AETHER_CATALYST_HATCH_IV = registerCatalystHatch(GTValues.IV);

    private static MachineDefinition registerCatalystHatch(int tier) {
        return REGISTRATE
                .machine("aether_catalyst_hatch_" + GTValues.VN[tier].toLowerCase(),
                        holder -> new AetherCrucibleMachine.AetherCatalystHatch(holder, tier))
                .langValue(GTValues.VNF[tier] + " Aether Catalyst Hatch")
                .tier(tier)
                .rotationState(RotationState.ALL)
                .abilities(AETHER_CATALYST_ABILITY)
                .tooltips(
                        Component.literal("Enables §bResonant§r mode on the Aether Crucible."),
                        Component.literal("§730% faster processing, 2× parallel.§r"))
                .overlayTieredHullModel("aether_catalyst_hatch")
                .register();
    }

    public static final MachineDefinition AETHER_CRUCIBLE = REGISTRATE
            .multiblock("aether_crucible", AetherCrucibleMachine::new)
            .langValue("Aether Crucible")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.ASSEMBLER_RECIPES)
            .recipeModifiers(
                    GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_NON_PERFECT,
                    AetherCrucibleMachine::recipeModifier)
            .appearanceBlock(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING)
            
            .modelProperty(PhoenixMultiblockProperties.FORMATION_TIER, 0)

            .partAppearance(PhoenixPartAppearance.rules()
                    .ownTextureForAbilitiesAtTier(1,
                            PartAbility.IMPORT_ITEMS, PartAbility.EXPORT_ITEMS,
                            PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS)
                    .build(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING))
            .tooltips(
                    Component.literal("Transmutes materials through aetheric resonance."),
                    Component.literal("§7Upgrade to Aetheric§r: Place a Fluid Input Hatch in the structure"),
                    Component.literal("§7Upgrade to Resonant§r: Replace it with an Aether Catalyst Hatch"),
                    Component.literal("§8Bonuses unlock when the structure is re-formed.§r"))
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    
                    .slice("CCCCC", "CCCCC", "CCCCC")
                    
                    .slice("CCCCC", "C   C", "CSCCC", "C   C", "CCCCC")
                    
                    .slice("CCCCC", "CCCCC", "CCCCC")
                    .where('S', Predicates.controller(definition))
                    .where('C', Predicates.blocks(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING.get())
                            
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            
                            .or(Predicates.abilities(AETHER_CATALYST_ABILITY).setMaxGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    new net.minecraft.resources.ResourceLocation("phoenixcore:block/casings/multiblock/aether_casing"),
                    new net.minecraft.resources.ResourceLocation("phoenixcore:block/multiblock/aether_crucible")
            )
            .register();
    public static void init() {  }

    private AetherCrucibleMachines() {}
}
