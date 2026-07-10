package net.phoenix.core.conflux.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/**
 * Registers the three Axiom multiblock research generators updated for GTM 8.0.0.
 */
public final class ConfluxMultiblockRegistry {

    // ── Cascade Engine — 5×5×5 hollow cube ───────────────────────────────────
    public static final MultiblockMachineDefinition CASCADE_ENGINE = REGISTRATE
            .multiblock("axiom_cascade_engine", ConfluxCascadeEngine::new)
            .langValue("Axiom Cascade Engine")
            .tier(GTValues.ZPM)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bMomentum-based Axiom data generator.§r"),
                    Component.literal("Builds momentum over time — full power takes ~25 seconds."),
                    Component.literal("§cAny power loss resets momentum rapidly!§r"),
                    Component.literal("Screwdriver to cycle output data type."))
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
            // FIXED: Using MultiblockPatternBuilder instead of FactoryBlockPattern
            .pattern(def -> MultiblockPatternBuilder.start()
                    // Z=0 — back wall (solid)
                    .slice("XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX")
                    // Z=1 — hollow
                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")
                    // Z=2 — hollow
                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")
                    // Z=3 — hollow
                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")
                    // Z=4 — front face (controller face)
                    .slice("XXXXX", "XEXEX", "XMHMX", "XSXSX", "XXXXX")
                    .where('H', controller(def)) // FIXED: Use 8.0 controller mapping
                    .where('X', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4)))
                    .where('S', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2)))
                    .where('M', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .model(createWorkableCasingMachineModel(
                    GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    GTCEu.id("block/multiblock/electric_blast_furnace")))
            .register();

    // ── Resonance Web — 9×5×9 sensor array ───────────────────────────────────
    public static final MultiblockMachineDefinition RESONANCE_WEB = REGISTRATE
            .multiblock("axiom_resonance_web", ConfluxResonanceWeb::new)
            .langValue("Axiom Resonance Web")
            .tier(GTValues.UV)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bPassive all-type data generator.§r"),
                    Component.literal("Scans nearby Axiom pipes and scales output with network size."),
                    Component.literal("§eDiversity bonus: +25% per distinct pipe type in range.§r"),
                    Component.literal("§7Scan radius: 24 blocks every 5 seconds.§r"))
            .appearanceBlock(GTBlocks.CASING_HSSE_STURDY)
            // FIXED: Using MultiblockPatternBuilder instead of FactoryBlockPattern
            .pattern(def -> MultiblockPatternBuilder.start()
                    // Z=0 — back wall (solid frame)
                    .slice("CCCCCCCCC", "C       C", "C       C", "C       C", "CCCCCCCCC")
                    // Z=1
                    .slice("CCCCCCCCC", "C       C", "C G   G C", "C       C", "CCCCCCCCC")
                    // Z=2
                    .slice("CCCCCCCCC", "C  G G  C", "C G   G C", "C  G G  C", "CCCCCCCCC")
                    // Z=3
                    .slice("CCCCCCCCC", "C       C", "CGG   GGC", "C       C", "CCCCCCCCC")
                    // Z=4 — center column (controller + hatches here)
                    .slice("CCCECCCCC", "C       C", "CGGGEGGGC", "C       C", "CCCECCCCC")
                    // Z=5
                    .slice("CCCCCCCCC", "C       C", "CGGHCGGGC", "C       C", "CCCCCCCCC")
                    // Z=6
                    .slice("CCCCCCCCC", "C  G G  C", "C G   G C", "C  G G  C", "CCCCCCCCC")
                    // Z=7
                    .slice("CCCCCCCCC", "C       C", "C G   G C", "C       C", "CCCCCCCCC")
                    // Z=8 — front wall (solid frame, maintenance)
                    .slice("CCCCCCCCC", "C       C", "C  CMC  C", "C       C", "CCCCCCCCC")
                    .where('H', controller(def)) // FIXED: Use 8.0 controller mapping
                    .where('C', blocks(GTBlocks.CASING_HSSE_STURDY.get()))
                    .where('G', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(blocks(GTBlocks.CASING_LAMINATED_GLASS.get())))
                    .where('E', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(6)))
                    .where('M', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .model(createWorkableCasingMachineModel(
                    GTCEu.id("block/casings/solid/machine_casing_sturdy_hsse"),
                    GTCEu.id("block/multiblock/large_chemical_reactor")))
            .register();

    // ── Harmonic Lens — 3×3×7 refraction prism ───────────────────────────────
    public static final MultiblockMachineDefinition HARMONIC_LENS = REGISTRATE
            .multiblock("axiom_harmonic_lens", ConfluxHarmonicLens::new)
            .langValue("Axiom Harmonic Lens")
            .tier(GTValues.ZPM)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bAxiom data type transmuter.§r"),
                    Component.literal("Converts one Axiom data type to another at 70% base efficiency."),
                    Component.literal("§eNearby researchers increase conversion efficiency (+0.5% / 50 nodes).§r"),
                    Component.literal("§7Screwdriver: cycle types. Shift+Screwdriver: cycle input type.§r"))
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
            // FIXED: Using MultiblockPatternBuilder instead of FactoryBlockPattern
            .pattern(def -> MultiblockPatternBuilder.start()
                    // Z=0 — controller face (input end)
                    .slice("XXX", "XHX", "XXX")
                    // Z=1
                    .slice("XXX", "XSX", "XXX")
                    // Z=2 — energy hatches on sides
                    .slice("XEX", "X X", "XEX")
                    // Z=3 — center (open conversion chamber)
                    .slice("XXX", "X X", "XXX")
                    // Z=4 — energy hatches
                    .slice("XEX", "X X", "XEX")
                    // Z=5
                    .slice("XXX", "XMX", "XXX")
                    // Z=6 — output end (solid back)
                    .slice("XXX", "XXX", "XXX")
                    .where('H', controller(def)) // FIXED: Use 8.0 controller mapping
                    .where('X', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4)))
                    .where('S', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1)))
                    .where('M', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .model(createWorkableCasingMachineModel(
                    GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    GTCEu.id("block/multiblock/implosion_compressor")))
            .register();

    public static void init() { /* triggers static initializer */ }

    private ConfluxMultiblockRegistry() {}
}