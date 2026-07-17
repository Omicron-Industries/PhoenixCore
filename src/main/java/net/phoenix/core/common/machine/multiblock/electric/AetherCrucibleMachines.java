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
// Replace these two with your actual recipe type and casing block:
//   import static net.phoenix.core.common.data.PhoenixRecipeTypes.AETHER_CRUCIBLE;
//   import static net.phoenix.core.common.data.PhoenixBlocks.AETHER_CASING;

/**
 * Registers the Aether Crucible multiblock and its Aether Catalyst Hatch.
 *
 * <p>This is a complete example of using {@link net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine}
 * in a real machine definition.
 *
 * <h2>Structure (5×3×5, front facing out)</h2>
 * <pre>
 *  Top/Bottom:   CCCCC
 *                CCCCC
 *                CCCCC
 *
 *  Middle layer: CCCCC
 *                C   C
 *                CSCCC   (S = controller, spaces = air)
 *                C   C
 *                CCCCC
 * </pre>
 *
 * <h2>Tier summary</h2>
 * <table>
 *   <tr><th>Tier</th><th>Required part</th><th>Bonus</th></tr>
 *   <tr><td>0 — Standard</td><td>none</td><td>baseline</td></tr>
 *   <tr><td>1 — Aetheric</td><td>any GTM Fluid Input Hatch</td><td>15% faster</td></tr>
 *   <tr><td>2 — Resonant</td><td>Aether Catalyst Hatch</td><td>30% faster + 2× parallel</td></tr>
 * </table>
 */
public final class AetherCrucibleMachines {

    // ── Controller ────────────────────────────────────────────────────────────



    // ── Aether Catalyst Hatch ─────────────────────────────────────────────────

    /** Dedicated PartAbility so the pattern predicate and capability system can find the hatch. */
    public static final PartAbility AETHER_CATALYST_ABILITY = new PartAbility("aether_catalyst");

    // EV and IV tiers — indices 4 and 5 in GTValues tier constants
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
            // Register FORMATION_TIER so the blockstate JSON can use it for model variants
            .modelProperty(PhoenixMultiblockProperties.FORMATION_TIER, 0)
            // Tier-0: all parts blend into casing.
            // Tier-1+: input/output hatches reveal their own texture so you can see them.
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
                    // Top and bottom caps
                    .slice("CCCCC", "CCCCC", "CCCCC")
                    // Middle hollow ring — sides hold hatches, center is air
                    .slice("CCCCC", "C   C", "CSCCC", "C   C", "CCCCC")
                    // Bottom cap (mirrors top)
                    .slice("CCCCC", "CCCCC", "CCCCC")
                    .where('S', Predicates.controller(definition))
                    .where('C', Predicates.blocks(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING.get())
                            // Any of these parts can substitute a casing block:
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            // The Aether Catalyst Hatch can also go in a casing slot:
                            .or(Predicates.abilities(AETHER_CATALYST_ABILITY).setMaxGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    new net.minecraft.resources.ResourceLocation("phoenixcore:block/casings/multiblock/aether_casing"),
                    new net.minecraft.resources.ResourceLocation("phoenixcore:block/multiblock/aether_crucible")
            )
            .register();
    public static void init() { /* triggers class loading / static initializers */ }

    private AetherCrucibleMachines() {}
}
