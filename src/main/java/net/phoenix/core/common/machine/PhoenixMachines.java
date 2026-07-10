package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidPipeProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder;
import com.gregtechceu.gtceu.common.data.*;

import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.CleaningMaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.storage.CrateMachine;
import com.gregtechceu.gtceu.common.machine.storage.DrumMachine;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.gregtechceu.gtceu.integration.kjs.helpers.MachineModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;
import net.phoenix.core.api.pattern.PhoenixPredicates;
import net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.machine.multiblock.electric.research.PhoenixHPCAMachine;
import net.phoenix.core.common.machine.multiblock.part.ShieldRenderProperty;
import net.phoenix.core.common.machine.multiblock.part.fluid.PlasmaHatchPartMachine;
import net.phoenix.core.common.machine.multiblock.part.special.ShieldSensorHatchPartMachine;
import net.phoenix.core.common.machine.multiblock.unique.BlazingCleanroom;
import net.phoenix.core.common.registry.PhoenixRegistration;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.datagen.models.PhoenixMachineModels;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.AlchemicalImbuerMachine;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.BioAethericEngineMachine;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.SourceMultiblockTankMachine;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.SourceReactorMachine;
import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;
import net.phoenix.core.integration.vocal_resonance.SoundHatchPartMachine;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.capability.recipe.IO.IN;
import static com.gregtechceu.gtceu.api.capability.recipe.IO.OUT;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt;
import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.gregtechceu.gtceu.api.multiblock.Predicates.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.GTMachines.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.*;
import static com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.VOID_PRISM;
import static net.phoenix.core.api.machine.PhoenixPartAbility.SOURCE_INPUT;
import static net.phoenix.core.api.machine.PhoenixPartAbility.SOURCE_OUTPUT;
import static net.phoenix.core.client.tooltips.PhoenixMachineTooltips.*;
import static net.phoenix.core.common.block.PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING;
import static net.phoenix.core.common.block.PhoenixBlocks.SUPER_STABLE_FUSION_CASING;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.*;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;
import static net.phoenix.core.configs.PhoenixConfigs.INSTANCE;

@SuppressWarnings("all")
public class PhoenixMachines {

    public static final String OVERLAY_PLASMA_HATCH_TEX = "overlay_plasma_hatch_input";
    public static final String OVERLAY_PLASMA_HATCH_HALF_PX_TEX = "overlay_plasma_hatch_half_px_out";
    public static MultiblockMachineDefinition DANCE = null;
    public static MachineDefinition BLAZING_CLEANING_MAINTENANCE_HATCH = null;
    public static MachineDefinition HIGH_YIELD_PHOTON_EMISSION_REGULATOR = null;

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
    }

    static {
        if (PhoenixConfigs.INSTANCE.features.blazingHatchEnabled) {
            BLAZING_CLEANING_MAINTENANCE_HATCH = REGISTRATE
                    .machine("blazing_cleaning_maintenance_hatch",
                            holder -> new CleaningMaintenanceHatchPartMachine(holder,
                                    BlazingCleanroom.BLAZING_CLEANROOM))
                    .langValue("Blazing Cleaning Maintenance Hatch")
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.MAINTENANCE)
                    .tooltips(Component.translatable("gtceu.part_sharing.disabled"),
                            Component.translatable("gtceu.machine.maintenance_hatch_cleanroom_auto.tooltip.0"),
                            Component.translatable("gtceu.machine.maintenance_hatch_cleanroom_auto.tooltip.1"))
                    .tooltipBuilder((stack, tooltips) -> tooltips.add(Component.literal("  ").append(Component
                            .translatable(BlazingCleanroom.BLAZING_CLEANROOM.getTranslationKey())
                            .withStyle(ChatFormatting.RED))))
                    .tier(UHV)

                    .overlayTieredHullModel(
                            PhoenixCore.id("block/machine/part/overlay_maintenance_blazing_cleaning"))
                    .register();
        }
    }
    String technicallyTrue = "false";

    public static MultiblockMachineDefinition registerMultiblockSourceTank(GTRegistrate registrate, String name,
                                                                           String displayName, int capacity,
                                                                           int maxConsumption,
                                                                           Supplier<? extends Block> casing,
                                                                           BiConsumer<MultiblockMachineBuilder<?, ?, ?>, ResourceLocation> rendererSetup) {
        MultiblockMachineBuilder<?, ?, ?> builder = registrate
                .multiblock(name, holder -> new SourceMultiblockTankMachine(holder, capacity, maxConsumption))
                .langValue(displayName)
                .tooltips(
                        Component.translatable("phoenixcore.machine.multiblock.source_tank.tooltip"),
                        Component.translatable("phoenixcore.universal.tooltip.source_storage_capacity", capacity))
                .rotationState(RotationState.ALL)
                .recipeType(DUMMY_RECIPES)
                .pattern(definition -> MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("CCC", "CCC", "CCC")
                        .slice("CCC", "C#C", "CCC")
                        .slice("CCC", "CSC", "CCC")
                        .where('S', Predicates.controller(definition))
                        .where('C', Predicates.blocks(casing.get())
                                .or(Predicates.abilities(PhoenixPartAbility.SOURCE_INPUT).setExactLimit(1))
                                .or(Predicates.abilities(PhoenixPartAbility.SOURCE_OUTPUT).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.controller(definition)))
                        .where('#', Predicates.air())
                        .build())
                .appearanceBlock(casing);

        rendererSetup.accept(builder, GTCEu.id("block/multiblock/multiblock_tank"));
        return builder.register();
    }

    public static final MultiblockMachineDefinition REFINED_MULTIBLOCK_SOURCE_TANK = registerMultiblockSourceTank(
            REGISTRATE, // 1. Pass your Registrate instance first!
            "refined_multiblock_source_tank", // 2. name
            "Refined Multiblock Source Tank", // 3. displayName
            50000 * 1000,                     // 4. capacity
            100,                              // 5. maxConsumption
            SOURCE_FIBER_MACHINE_CASING,      // 6. casing block supplier
            (builder, overlay) -> builder     // 7. renderer consumer lambda
                    .workableCasingModel(
                            // Explicitly cast or construct to ensure a true ResourceLocation object is bound
                            new ResourceLocation(PhoenixCore.MOD_ID, "block/casings/multiblock/machine_casing_source_fiber_mesh"),
                            overlay
                    )
    );

    public static final PartAbility SOUND_DISC = new PartAbility("sound_disc");
    public static final PartAbility SOUND_LIBRARY = new PartAbility("sound_library");
    public static final PartAbility SOUND_STREAM = new PartAbility("sound_stream");

    public static final MachineDefinition[] DISC_HATCH = registerSoundHatch(
            "disc_hatch", "Disc Reader Hatch", SoundHatchPartMachine.SoundHatchType.DISC, SOUND_DISC);

    public static final MachineDefinition[] LIBRARY_HATCH = registerSoundHatch(
            "library_hatch", "Sound Library Hatch", SoundHatchPartMachine.SoundHatchType.LIBRARY, SOUND_LIBRARY);

    public static final MachineDefinition[] STREAM_HATCH = registerSoundHatch(
            "stream_hatch", "Web Stream Hatch", SoundHatchPartMachine.SoundHatchType.STREAM, SOUND_STREAM);

    private static MachineDefinition[] registerSoundHatch(String name, String displayName,
                                                          SoundHatchPartMachine.SoundHatchType type,
                                                          PartAbility ability) {
        return registerTieredMachines(name,
                (holder, tier) -> new SoundHatchPartMachine(holder, tier, type), // Clean constructor
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + ' ' + displayName)
                        .abilities(ability)
                        .rotationState(RotationState.ALL)
                        .overlayTieredHullModel("source_hatch")
                        .register(),
                ELECTRIC_TIERS);
    }

    public static final MachineDefinition[] SOURCE_IMPORT_HATCH = registerSourceHatch(
            "source_input_hatch", "Source Input Hatch",
            IO.IN, ELECTRIC_TIERS, SOURCE_INPUT);
    public static final MachineDefinition[] SOURCE_EXPORT_HATCH = registerSourceHatch(
            "source_output_hatch", "Source Output Hatch",
            IO.OUT, ELECTRIC_TIERS, SOURCE_OUTPUT);

    private static MachineDefinition[] registerSourceHatch(String name, String displayName, IO io,
                                                           int[] tiers, PartAbility... abilities) {
        return registerTieredMachines(name,
                // Fixed: Pass holder directly without an invalid cast
                (holder, tier) -> new SourceHatchPartMachine(holder, tier, io),
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + ' ' + displayName)
                        .abilities(abilities)
                        .rotationState(RotationState.ALL)
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .overlayTieredHullModel("source_hatch")
                        .tooltipBuilder((item, tooltip) -> {
                            if (io == IO.IN) {
                                tooltip.add(Component.translatable("tooltip.phoenixcore.source_hatch.capacity",
                                                SourceHatchPartMachine.getMaxCapacity(tier))
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(0x8F00FF))));
                                tooltip.add(Component.translatable("tooltip.phoenixcore.source_hatch.consumption",
                                                SourceHatchPartMachine.getMaxConsumption(tier))
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(0x008080))));
                            } else
                                tooltip.add(Component.translatable("tooltip.phoenixcore.source_hatch.capacity",
                                                SourceHatchPartMachine.getMaxCapacity(tier))
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(0x8F00FF))));
                        }).register(),
                tiers);
    }

    public final static MachineDefinition[] PLASMA_INPUT_HATCH = registerPlasmaHatches(
            "plasma_input_hatch", "Plasma Input Hatch", "fluid_hatch.import",
            IN, PlasmaHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 1, new int[] { 7, 8, 9 },
            PhoenixPartAbility.PLASMA_INPUT);

    public static MachineDefinition[] registerPlasmaHatches(String name, String displayName, String tooltip,
                                                            IO io, int initialCapacity, int slots,
                                                            int[] tiers, PartAbility... abilities) {
        return registerPlasmaHatches(GTRegistration.REGISTRATE, name, displayName, tooltip, io, initialCapacity, slots,
                tiers,
                abilities);
    }

    public static MachineDefinition[] registerPlasmaHatches(GTRegistrate registrate, String name, String displayName,
                                                            String tooltip,
                                                            IO io, int initialCapacity, int slots,
                                                            int[] tiers, PartAbility... abilities) {
        final String pipeOverlay;
        if (slots >= 9) {
            pipeOverlay = "overlay_pipe_9x";
        } else if (slots >= 4) {
            pipeOverlay = "overlay_pipe_4x";
        } else {
            pipeOverlay = null;
        }
        final String ioOverlay = io == OUT ? "overlay_pipe_out_emissive" : "overlay_pipe_in_emissive";
        final String emissiveOverlay = slots > 4 ? OVERLAY_PLASMA_HATCH_HALF_PX_TEX : OVERLAY_PLASMA_HATCH_TEX;
        return registerTieredMachines(name,
                (holder, tier) -> new PlasmaHatchPartMachine(holder, tier, io, initialCapacity, slots),
                (tier, builder) -> {
                    builder.langValue(VNF[tier] + ' ' + displayName)
                            .rotationState(RotationState.ALL)
                            .colorOverlayTieredHullModel(ioOverlay, pipeOverlay, emissiveOverlay)
                            .abilities(abilities)
                            .modelProperty(IS_FORMED, false)
                            .tooltips(Component.translatable("gtceu.machine." + tooltip + ".tooltip"))
                            .allowCoverOnFront(true);

                    if (slots == 1) {
                        builder.tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                                FormattingUtil
                                        .formatNumbers(FluidHatchPartMachine.getTankCapacity(initialCapacity, tier))));
                    } else {
                        builder.tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult",
                                slots, FormattingUtil
                                        .formatNumbers(FluidHatchPartMachine.getTankCapacity(initialCapacity, tier))));
                    }
                    return builder.register();
                },
                tiers);
    }

    public static MachineDefinition[] registerTieredMachines(String name,
                                                             BiFunction<BlockEntityCreationInfo, Integer, MetaMachine> factory,
                                                             BiFunction<Integer, MachineBuilder<MachineDefinition, ?, ?>, MachineDefinition> builder,
                                                             int... tiers) {
        MachineDefinition[] definitions = new MachineDefinition[GTValues.TIER_COUNT];
        for (int tier : tiers) {
            var register = REGISTRATE
                    .machine(GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_" + name,
                            holder -> factory.apply(holder, tier))
                    .tier(tier);
            definitions[tier] = builder.apply(tier, register);
        }
        return definitions;
    }

    public static MachineDefinition registerDrum(Material material, int capacity, String lang) {
        return registerDrum(PhoenixRegistration.REGISTRATE, material, capacity, lang);
    }

    public static MachineDefinition registerDrum(GTRegistrate registrate, Material material, int capacity,
                                                 String lang) {
        boolean wooden = material.hasProperty(PropertyKey.WOOD);
        var definition = registrate
                .machine(material.getName() + "_drum",
                        holder -> new DrumMachine(holder, material, capacity)
                )
                .langValue(lang)
                .rotationState(RotationState.NONE)
                .simpleModel(GTCEu.id("block/machine/template/drum/" + (wooden ? "wooden" : "metal") + "_drum"))
                .tooltipBuilder((stack, list) -> {
                    TANK_TOOLTIPS.accept(stack, list);
                    if (material.hasProperty(PropertyKey.FLUID_PIPE)) {
                        FluidPipeProperties pipeprops = material.getProperty(PropertyKey.FLUID_PIPE);
                        pipeprops.appendTooltips(list, false, true);
                    }
                })
                .tooltips(Component.translatable("gtceu.machine.quantum_tank.tooltip"),
                        Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                                FormattingUtil.formatNumbers(capacity)))
                .paintingColor(wooden ? 0xFFFFFF : material.getMaterialRGB())
                .itemColor((s, i) -> wooden ? 0xFFFFFF : material.getMaterialRGB())
                .register();
        DRUM_CAPACITY.put(definition, capacity);
        return definition;
    }

    public static MachineDefinition registerCrate(Material material, int capacity, String lang) {
        return registerCrate(PhoenixRegistration.REGISTRATE, material, capacity, lang);
    }

    public static MachineDefinition registerCrate(GTRegistrate registrate, Material material, int capacity,
                                                  String lang) {
        final boolean wooden = material.hasProperty(PropertyKey.WOOD);
        final int rowLength = 9;

        return registrate.machine(material.getName() + "_crate",
                        holder -> new CrateMachine(holder, material, capacity, rowLength))
                .langValue(lang)
                .rotationState(RotationState.NONE)
                .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", capacity))
                .modelProperty(GTMachineModelProperties.IS_TAPED, false)
                .model((definition, frontCasing, renderer) -> GTMachineModels.createCrateModel(wooden))
                // FIXED: Replaced .color() with the class-validated .itemColor() hook
                .itemColor((itemStack, tintIndex) -> {
                    if (wooden) return 0xFFFFFF;
                    // Tint index 1 targets the structural overlay texture in the model builder setup
                    return tintIndex == 1 ? material.getMaterialRGB() : 0xFFFFFF;
                })
                .register();
    }


    public static MachineDefinition AURUM_STEEL_DRUM = registerDrum(AURUM_STEEL,
            (80 * FluidType.BUCKET_VOLUME),
            "Aurum Steel Drum");
    public static MachineDefinition ALUMINFROST_DRUM = registerDrum(ALUMINFROST,
            (160 * FluidType.BUCKET_VOLUME),
            "Aluminfrost Drum");
    public static MachineDefinition FROST_REINFORCED_STAINED_STEEL_DRUM = registerDrum(FROST_REINFORCED_STAINED_STEEL,
            (350 * FluidType.BUCKET_VOLUME),
            "Frost Reinforced Stained Steel Drum");
    public static MachineDefinition SOURCE_IMBUED_TITANIUM_DRUM = registerDrum(SOURCE_IMBUED_TITANIUM,
            (750 * FluidType.BUCKET_VOLUME),
            "Source Imbued Titanium Drum");
    public static MachineDefinition VOID_TOUCHED_TUNGSTEN_STEEL_DRUM = registerDrum(VOID_TOUCHED_TUNGSTEN_STEEL,
            (1300 * FluidType.BUCKET_VOLUME),
            "Void Touched Tungsten Steel Drum");
    public static MachineDefinition RESONANT_RHODIUM_ALLOY_DRUM = registerDrum(RESONANT_RHODIUM_ALLOY,
            (2300 * FluidType.BUCKET_VOLUME),
            "Resonant Rhodium Alloy Drum");

    public static MachineDefinition AURUM_STEEL_CRATE = registerCrate(AURUM_STEEL, 90,
            "Aurum Steel Crate");
    public static MachineDefinition ALUMINFROST_CRATE = registerCrate(ALUMINFROST, 100,
            "Aluminfrost Crate");
    public static MachineDefinition FROST_REINFORCED_STAINED_STEEL_CRATE = registerCrate(FROST_REINFORCED_STAINED_STEEL,
            116,
            "Frost Reinforced Stained Steel Crate");
    public static MachineDefinition SOURCE_IMBUED_TITANIUM_CRATE = registerCrate(
            SOURCE_IMBUED_TITANIUM, 140,
            "Source Imbued Titanium Crate");
    public static MachineDefinition VOID_TOUCHED_TUNGSTEN_STEEL_CRATE = registerCrate(VOID_TOUCHED_TUNGSTEN_STEEL,
            160,
            "Void Touched Tungsten Steel Crate");
    public static MachineDefinition RESONANT_RHODIUM_ALLOY_CRATE = registerCrate(RESONANT_RHODIUM_ALLOY, 200,
            "Resonant Rhodium Alloy Crate");

    public static MachineDefinition SHIELD_INTEGRITY_SENSOR_HATCH = REGISTRATE
            .machine("shield_stability_sensor_hatch", ShieldSensorHatchPartMachine::new)
            .langValue("Shield Stability Sensor Hatch")
            .rotationState(RotationState.ALL)
            .tooltips(Component.translatable("tooltip.phoenixcore.shield_stability_hatch.0"),
                    Component.translatable("tooltip.phoenixcore.shield_stability_hatch.1"))
            .tier(GTValues.HV)
            .modelProperty(ShieldRenderProperty.TYPE, ShieldRenderProperty.NORMAL)
            .modelProperty(IS_FORMED, false)
            .model(PhoenixMachineModels.createOverlayFillLevelCasingMachineModel("stability_hatch",
                    "casings/microverse"))
            .register();

    public static final MachineDefinition PHOENIXWARE_FUSION_MK1 = REGISTRATE
            .multiblock("phoenixware_fusion_mk1", holder -> new FusionReactorMachine(holder, GTValues.UIV))
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT,
                    MachineModifiers.FUSION_REACTOR, GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(PhoenixBlocks.INSANELY_SUPERCHARGED_TESLA_CASING)
            .pattern(definition -> MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT) //
                    // Converted aisles to multi-axis relative slice definitions
                    .slice("########BBCCCBB########", "########DDEEEDD########", "########DDEEEDD########",
                            "########DDEEEDD########", "########BBCCCBB########")
                    .slice("######BBBBBBBBBBB######", "######FFAAAAAAAFF######", "######FFAAAAAAAFF######",
                            "######FFAAAAAAAFF######", "######BBBBBBBBBBB######")
                    .slice("####BBBBBBBBBBBBBBB####", "####GGAAAAAAAAAAAGG####", "####GGAAAAAAAAAAAGG####",
                            "####GGAAAAAAAAAAAGG####", "####BBBBBBBBBBBBBBB####")
                    .slice("###BBBBBBBBBBBBBBBBB###", "###GAAAAAAAAAAAAAAAG###", "###GAAAAAAAAAAAAAAAG###",
                            "###GAAAAAAAAAAAAAAAG###", "###BBBBBBBBBBBBBBBBB###")
                    .slice("##BBBBBBBBBBBBBBBBBBB##", "##GAAAAAAAAAAAAAAAAAG##", "##GAAAAAAAHHHAAAAAAAG##",
                            "##GAAAAAAAAAAAAAAAAAG##", "##BBBBBBBBBBBBBBBBBBB##")
                    .slice("##BBBBBBBBIIIBBBBBBBB##", "##GAAAAAAAHHHAAAAAAAG##", "##GAAAAAHHAAAHHAAAAAG##",
                            "##GAAAAAAAHHHAAAAAAAG##", "##BBBBBBBBIIIBBBBBBBB##")
                    .slice("#BBBBBBBBIIEIIBBBBBBBB#", "#FAAAAAAHHAAAHHAAAAAAG#", "#FAAAAAHAAHHHAAHAAAAAG#",
                            "#FAAAAAAHHAAAHHAAAAAAG#", "#BBBBBBBBIIEIIBBBBBBBB#")
                    .slice("#BBBBBBBIIEJEIIBBBBBBB#", "#FAAAAAHAAAAAAAHAAAAAG#", "#FAAAAHFHHAKAHHFHAAAAG#",
                            "#FAAAAAHAAAAAAAHAAAAAG#", "#BBBBBBBIIEJEIIBBBBBBB#")
                    .slice("BBBBBBBIIEJJJEIIBBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHKAAKAAKHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBBIIEJJJEIIBBBBBBB")
                    .slice("BBBBBBIIEJJJJJEIIBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHAKAKAKAHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBIIEJJJJJEIIBBBBBB")
                    .slice("CBBBBIIEJJJCJJJEIIBBBBC", "EAAAAHAAAAAAAAAAAHAAAAE", "EAAAHAHAAAKKKAAAHAHAAAE",
                            "EAAAAHAAAAAAAAAAAHAAAAE", "CBBBBIIEJJJCJJJEIIBBBBC")
                    .slice("CBBBBIEJJJCCCJJJEIBBBBC", "EAAAAHAAAAAKAAAAAHAAAAE", "EAAAHAHKKKKLKKKKHAHAAAE",
                            "EAAAAHAAAAAKAAAAAHAAAAE", "CBBBBIEJJJCCCJJJEIBBBBC")
                    .slice("CBBBBIIEJJJCJJJEIIBBBBC", "EAAAAHAAAAAAAAAAAHAAAAE", "EAAAHAHAAAKKKAAAHAHAAAE",
                            "EAAAAHAAAAAAAAAAAHAAAAE", "CBBBBIIEJJJCJJJEIIBBBBC")
                    .slice("BBBBBBIIEJJJJJEIIBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHAKAKAKAHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBIIEJJJJJEIIBBBBBB")
                    .slice("BBBBBBBIIEJJJEIIBBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHKAAKAAKHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBBIIEJJJEIIBBBBBBB")
                    .slice("#BBBBBBBIIEJEIIBBBBBBB#", "#FAAAAAHAAAAAAAHAAAAAG#", "#FAAAAHFHHAKAHHFHAAAAG#",
                            "#FAAAAAHAAAAAAAHAAAAAG#", "#BBBBBBBIIEJEIIBBBBBBB#")
                    .slice("#BBBBBBBBIIEIIBBBBBBBB#", "#FAAAAAAHHAAAHHAAAAAAG#", "#FAAAAAHAAHHHAAHAAAAAG#",
                            "#FAAAAAAHHAAAHHAAAAAAG#", "#BBBBBBBBIIEIIBBBBBBBB#")
                    .slice("##BBBBBBBBIIIBBBBBBBB##", "##GAAAAAAAHHHAAAAAAAG##", "##GAAAAAHHAAAHHAAAAAG##",
                            "##GAAAAAAAHHHAAAAAAAG##", "##BBBBBBBBIIIBBBBBBBB##")
                    .slice("##BBBBBBBBBBBBBBBBBBB##", "##GAAAAAAAAAAAAAAAAAG##", "##GAAAAAAAHHHAAAAAAAG##",
                            "##GAAAAAAAAAAAAAAAAAG##", "##BBBBBBBBBBBBBBBBBBB##")
                    .slice("###BBBBBBBBBBBBBBBBB###", "###GAAAAAAAAAAAAAAAG###", "###GAAAAAAAAAAAAAAAG###",
                            "###GAAAAAAAAAAAAAAAG###", "###BBBBBBBBBBBBBBBBB###")
                    .slice("####BBBBBBBBBBBBBBB####", "####GGAAAAAAAAAAAGG####", "####GGAAAAAAAAAAAGG####",
                            "####GGAAAAAAAAAAAGG####", "####BBBBBBBBBBBBBBB####")
                    .slice("######BBBBBBBBBBB######", "######FFAAAAAAAFF######", "######FFAAAAAAAFF######",
                            "######FFAAAAAAAFF######", "######BBBBBBBBBBB######")
                    .slice("########BBCCCBB########", "########DDEEEDD########", "########DDENEDD########",
                            "########DDEEEDD########", "########BBCCCBB########")

                    // Modernized direct Controller definition mapping
                    .where('N', Predicates.controller(definition)) //
                    .where('A', Predicates.air()) //
                    .where('#', Predicates.any()) //
                    .where('B', Predicates.blocks(PhoenixBlocks.INSANELY_SUPERCHARGED_TESLA_CASING.get()))
                    .where('C', Predicates.blocks(ADVANCED_COMPUTER_CASING.get()))
                    .where('D', Predicates.blocks(COMPUTER_CASING.get()))

                    // Casing block 'E' updated with safe controller-override parameters
                    .where('E', Predicates.blocks(FUSION_COIL.get()).setMinGlobalLimited(10)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.blocks(GTMachines.ENERGY_INPUT_HATCH[GTValues.UHV].get()).setMaxGlobalLimited(4))
                            .or(Predicates.controller(definition))) //

                    .where('F', Predicates.blocks(SUPERCONDUCTING_COIL.get()))
                    .where('G', Predicates.blocks(FUSION_GLASS.get()))
                    .where('H', PhoenixPredicates.lampsByColor(DyeColor.BLUE))
                    .where('I', Predicates.blocks(CASING_STEEL_SOLID.get()))
                    .where('J', Predicates.blocks(CASING_STAINLESS_CLEAN.get()))
                    .where('K', Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where('L', Predicates.blocks(CASING_PRIMITIVE_BRICKS.get()))
                    .build())
            .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(
                    new ResourceLocation(PhoenixCore.MOD_ID, "block/casings/multiblock/tesla_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor")))
            .hasBER(true)
            .register();





    public static final MultiblockMachineDefinition ALCHEMICAL_IMBUER = REGISTRATE
            .multiblock("alchemical_imbuer", AlchemicalImbuerMachine::new)
            .langValue("§5Alchemical Imbuer")
            .recipeTypes(PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES, PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES)
            .recipeModifiers(AlchemicalImbuerMachine::recipeModifier, OC_NON_PERFECT_SUBTICK)
            .appearanceBlock(SOURCE_FIBER_MACHINE_CASING)
            .rotationState(RotationState.NON_Y_AXIS)
            .pattern(definition -> MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    // Converted old aisles to multi-axis relative slice definitions
                    .slice("BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")
                    .slice("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
                    .slice("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
                    .slice("CDDDDDC", "BCEEECB", "BBFEFBB", "BBFHFBB", "BBFEFBB", "BCDIDCB", "CDGJGDC")
                    .slice("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
                    .slice("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
                    .slice("BBCKCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")

                    // Uniform character token mapping definitions
                    .where('B', Predicates.any())

                    // Casing block 'C' configuration updated with strict type safety & controller context fallback
                    .where('C', Predicates.blocks(SOURCE_FIBER_MACHINE_CASING.get())
                            .or(Predicates.abilities(SOURCE_INPUT).setPreviewCount(1))
                            .or(Predicates.abilities(SOURCE_OUTPUT).setPreviewCount(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.controller(definition)))

                    .where('D', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:sourcestone"))))
                    .where('E', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:magebloom_block"))))
                    .where('F', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where('G', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:void_prism"))))
                    .where('H', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:source_gem_block"))))
                    .where('I', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:arcane_core"))))
                    .where('J', Predicates.blocks(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:vitalic_sourcelink"))))

                    // Modernized direct Controller definition mapping
                    .where('K', Predicates.controller(definition))
                    .build())
            .workableCasingModel(PhoenixCore.id("block/casings/multiblock/machine_casing_source_fiber_mesh"),
                    PhoenixCore.id("block/multiblock/alchemical_imbuer"))
            .tooltipBuilder(ALCHEMICAL_IMBUER_TOOLTIPS)
            .register();

    public static final MultiblockMachineDefinition SOURCE_REACTOR = REGISTRATE
            .multiblock("source_reactor", SourceReactorMachine::new)
            .rotationState(RotationState.ALL)
            .langValue("§5Source Reactor")
            .recipeType(PhoenixRecipeTypes.SOURCE_REACTOR_RECIPES)
            .recipeModifiers(SourceReactorMachine::recipeModifier, OC_NON_PERFECT_SUBTICK, BATCH_MODE)
            .appearanceBlock(SOURCE_FIBER_MACHINE_CASING)
            .pattern(definition -> {
                var casing = Predicates.blocks(SOURCE_FIBER_MACHINE_CASING.get()).setMinGlobalLimited(10);
                var abilities = Predicates.autoAbilities(definition.getRecipeTypes())
                        .or(Predicates.autoAbilities(true, false, false));

                // Modernized pattern utilizing direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT) //
                        .slice("XXX", "XXX", "XXX")
                        .slice("XXX", "XPX", "XXX")
                        .slice("XXX", "XSX", "XXX")

                        // Direct simplified controller definition assignment
                        .where('S', Predicates.controller(definition)) //

                        // Appended controller fallback predicate safely inside the main structural casing map
                        .where('X', casing.or(abilities).or(Predicates.abilities(SOURCE_INPUT).setPreviewCount(1)).or(Predicates.controller(definition)))
                        .where('P', Predicates.blocks(CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
                        .build();
            })
            .workableCasingModel(PhoenixCore.id("block/casings/multiblock/machine_casing_source_fiber_mesh"),
                    PhoenixCore.id("block/multiblock/source_spin"))
            .tooltipBuilder(SOURCE_REACTOR_TOOLTIPS)
            .register();

    public static final MultiblockMachineDefinition BIO_AETHERIC_ENGINE = REGISTRATE
            .multiblock("bio_aetheric_engine", BioAethericEngineMachine::new)
            .rotationState(RotationState.ALL)
            .langValue("§5Bio Aetheric Engine")
            .recipeType(PhoenixRecipeTypes.BIO_ENGINE_RECIPES)
            .recipeModifiers(BioAethericEngineMachine::recipeModifier, BATCH_MODE)
            .appearanceBlock(SOURCE_FIBER_MACHINE_CASING)
            .pattern(definition -> {
                // Modernized pattern utilizing direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT) //
                        .slice("BBCBB", "BBHBB", "CHBHC", "BBHBB", "BBCBB")
                        .slice("BBCBB", "BHCHB", "CCICC", "BHCHB", "BBCBB")
                        .slice("BBBBB", "BCCCB", "CCFCC", "BCCCB", "BBCBB")
                        .slice("BBBBB", "BCCCB", "CCBCC", "BCCCB", "BBCBB")
                        .slice("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .slice("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .slice("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .slice("BEBEB", "BCCCB", "BCFCB", "BCCCB", "BBBBB")
                        .slice("BBBBB", "BBCBB", "BCDCB", "BBCBB", "BBBBB")

                        .where('B', Predicates.any()) //

                        // Main framework block configuration with integrated controller fallback loop
                        .where('C', Predicates.blocks(SOURCE_FIBER_MACHINE_CASING.get())
                                .or(Predicates.abilities(SOURCE_INPUT).setPreviewCount(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.controller(definition))) //

                        .where('E', Predicates.blocks(ChemicalHelper.getBlock(frameGt, SOURCE_IMBUED_TITANIUM)))
                        .where('H', Predicates.blocks(VOID_PRISM.get()))
                        .where('G', Predicates.blocks(GTBlocks.CLEANROOM_GLASS.get()))

                        // Direct simplified controller definition assignment
                        .where('D', Predicates.controller(definition)) //
                        .where('F', Predicates.blocks(CASING_TITANIUM_GEARBOX.get()))
                        .where('I', Predicates.abilities(PartAbility.MUFFLER))
                        .build();
            })
            .model(
                    createWorkableCasingMachineModel(
                            PhoenixCore.id("block/casings/multiblock/machine_casing_source_fiber_mesh"),
                            GTCEu.id("block/multiblock/generator/large_gas_turbine"))
                            .andThen(d -> d
                                    .addDynamicRenderer(
                                            PhoenixDynamicRenderHelpers::getEngineGearboxRenderer)))
            .hasBER(true)
            .tooltipBuilder(BIO_ENGINE_TOOLTIPS)
            .register();


    public static final MultiblockMachineDefinition EMBERWAKE_ALLOY_HEARTH = REGISTRATE
            .multiblock("emberwake_alloy_hearth", CoilWorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(GCYMRecipeTypes.ALLOY_BLAST_RECIPES)
            .recipeModifiers(PARALLEL_HATCH, BATCH_MODE, GTRecipeModifiers::ebfOverclock)
            .appearanceBlock(CASING_STEEL_SOLID)
            .pattern(definition -> {
                // Modernized pattern utilizing direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BCCCB", "BCDCB", "BCDCB", "BEEEB", "BFFFB", "BCCCB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("CCCCC", "CDGDC", "CADAC", "EADAE", "FAFAF", "CCCCC", "BCFCB", "BBFBB", "BBFBB")
                        .slice("CCCCC", "DGHGD", "DDHDD", "EDHDE", "FFAFF", "CCCCC", "BFCFB", "BFAFB", "BFIFB")
                        .slice("CCCCC", "CDGDC", "CADAC", "EADAE", "FAFAF", "CCCCC", "BCFCB", "BBFBB", "BBFBB")
                        .slice("BCCCB", "BCJCB", "BCDCB", "BEEEB", "BFFFB", "BCCCB", "BBBBB", "BBBBB", "BBBBB")

                        .where('A', Predicates.air())
                        .where('B', Predicates.any())

                        // Added controller fallback predicate into the structural casing definition
                        .where('C', Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))

                        .where('D', Predicates.blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                        .where('E', Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('F', Predicates.heatingCoils())
                        .where('G', Predicates.blocks(FUSION_COIL.get()))
                        .where('H', Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                        .where('I', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))

                        // Direct simplified controller definition assignment
                        .where('J', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/gcym/high_temperature_smelting_casing"),
                    GTCEu.id("block/multiblock/gcym/blast_alloy_smelter"))
            .register();

    public static final MultiblockMachineDefinition ADVANCED_CRACKING_UNIT = REGISTRATE
            .multiblock("advanced_cracking_unit", CoilWorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(GTRecipeTypes.CRACKING_RECIPES)
            .recipeModifiers(PARALLEL_HATCH, BATCH_MODE, GTRecipeModifiers::crackerOverclock)
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_TURBINE)
            .pattern(definition -> {
                // Modernized pattern utilizing direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BBCCCCCBB", "DBDDDDDBD", "DBDDDDDBD", "DBDDDDDBD", "DBDDDDDBD", "DDDDDDDDD", "DDDDDDDDD", "DDDDDDDDD")
                        .slice("BEEEEEEEB", "BEFFFFFEB", "BEEEEEEEB", "BEEEEEEEB", "BEEEEEEEB", "DBCBBBCBD", "DDCBBBCDD", "DDCBBBCDD")
                        .slice("CEEEEEEEC", "DFAGAAAFD", "DHAAAAAHD", "DHAGAGAHD", "DHHEEEHHD", "DIHAAAHID", "DDHAAAHDD", "DDBJJJBDD")
                        .slice("CEEEEEEEC", "DFGKGKGFD", "DHAGAGAHD", "DHGKGKGHD", "DHHEAEHHD", "DIHAAAHID", "DDHAAAHDD", "DDBJLJBDD")
                        .slice("CEEEEEEEC", "DFAGAGAFD", "DHAAAAAHD", "DHAGAGAHD", "DHHEEEHHD", "DIHAAAHID", "DDHAAAHDD", "DDBJJJBDD")
                        .slice("BEEEEEEEB", "BEMMMMMEB", "BEMMNMMEB", "BEMMMMMEB", "BEEEEEEEB", "DBCBBBCBD", "DDCBBBCDD", "DDCBBBCDD")
                        .slice("BBCDDDCBB", "DBCDDDCBD", "DBCDDDCBD", "DBCDDDCBD", "DBCCCCCBD", "DDDDDDDDD", "DDDDDDDDD", "DDDDDDDDD")

                        .where('A', Predicates.air())

                        // Core structural casing mapped safely with integrated controller fallback loop
                        .where('B', Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_TURBINE.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))

                        .where('C', Predicates.blocks(ChemicalHelper.getBlock(frameGt, VOID_TOUCHED_TUNGSTEN_STEEL)))
                        .where('D', Predicates.any())
                        .where('E', Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                        .where('F', Predicates.blocks(FIREBOX_TUNGSTENSTEEL.get()))
                        .where('G', Predicates.blocks(CASING_TUNGSTENSTEEL_PIPE.get()))
                        .where('H', Predicates.heatingCoils())
                        .where('I', Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        .where('J', Predicates.blocks(CASING_HSSE_STURDY.get()))
                        .where('K', Predicates.blocks(CASING_EXTREME_ENGINE_INTAKE.get()))
                        .where('L', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where('M', Predicates.blocks(CASING_STAINLESS_CLEAN.get()))

                        // Direct simplified controller definition assignment
                        .where('N', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/mechanic/machine_casing_turbine_tungstensteel"),
                    GTCEu.id("block/multiblock/cracking_uni"))
            .register();

    public static final MultiblockMachineDefinition SUPERHEATED_PYROLYSING_OVEN = REGISTRATE
            .multiblock("superheated_pyrolysing_oven", CoilWorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(GTRecipeTypes.PYROLYSE_RECIPES)
            .recipeModifiers(PARALLEL_HATCH, BATCH_MODE, GTRecipeModifiers::ebfOverclock)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .pattern(definition -> {
                // Modernized pattern layout using direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BCBBBBBCB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BCBBBBBCB")
                        .slice("CCEEEEECC", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "CCCCCCCCC")
                        .slice("BEGGGGGEB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BCHCCCHCB")
                        .slice("BEGGGGGEB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BCCGGGCCB")
                        .slice("BEGGGGGEB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BCCGKGCCB")
                        .slice("BEGGGGGEB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BCCGGGCCB")
                        .slice("BEGGGGGEB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BCHCCCHCB")
                        .slice("CCEEEEECC", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "CCCCLCCCC")
                        .slice("BCBBBBBCB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BCBBBBBCB")

                        .where('A', Predicates.air())
                        .where('B', Predicates.any())

                        // Added controller fallback rule inside structural casing map 'C'
                        .where('C', Predicates.blocks(CASING_STEEL_SOLID.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))

                        .where('D', Predicates.blocks(ChemicalHelper.getBlock(frameGt, VOID_TOUCHED_TUNGSTEN_STEEL)))
                        .where('E', Predicates.blocks(FIREBOX_STEEL.get()))
                        .where('F', Predicates.blocks(CASING_LAMINATED_GLASS.get()))
                        .where('G', Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        .where('H', Predicates.blocks(ChemicalHelper.getBlock(frameGt, RESONANT_RHODIUM_ALLOY)))
                        .where('I', Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('J', Predicates.heatingCoils())
                        .where('K', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))

                        // Direct simplified controller definition assignment
                        .where('L', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                    GTCEu.id("block/multiblock/pyrolyse_oven"))
            .register();

    public static final MultiblockMachineDefinition MELLIFERIOUS_MATRIX = REGISTRATE
            .multiblock("melliferious_matrix", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(PhoenixRecipeTypes.MELLIFERIOUS_MATRIX_RECIPES)
            .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_NON_PERFECT_SUBTICK,
                    GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
            .pattern(definition -> {
                // Modernized pattern layout using direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BBBCCCBBB", "BBBCCCBBB", "BBBDDDBBB", "BBBDDDBBB", "BBBEEEBBB", "BBBBBBBBB")
                        .slice("BCCCCCCCB", "BCFGGGFCB", "BDDGGGDDB", "BDDGGGDDB", "BEEGGGEEB", "BBEEEEEBB")
                        .slice("CCCCCCCCC", "CHGIIIGHC", "DHGJJJGHD", "DIGAAAGID", "BEGAAAGEB", "BBEAGAEBB")
                        .slice("CCCCCCCCC", "CHGIJIGHC", "DHGJJJGHD", "DIGABAGID", "BEGAKAGEB", "BBEGGGEBB")
                        .slice("CCCCCCCCC", "CHGIIIGHC", "DHGJJJGHD", "DIGAAAGID", "BEGA2AGEB", "BBEAGAEBB")
                        .slice("BCCCCCCCB", "BCFGGGFCB", "BDDGGGDDB", "BDDGGGDDB", "BEEGGGEEB", "BBEEEEEBB")
                        .slice("BBBCCCBBB", "BBBCLCBBB", "BBBDDDBBB", "BBBDDDBBB", "BBBEEEBBB", "BBBBBBBBB")

                        .where('A', Predicates.air())
                        .where('2', Predicates.blocks(Blocks.DIRT))
                        .where('B', Predicates.any())

                        // Added controller fallback rule inside structural casing map 'C'
                        .where('C', Predicates.blocks(CASING_HSSE_STURDY.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))

                        .where('D', Predicates.blocks(ChemicalHelper.getBlock(frameGt, TungstenSteel)))
                        .where('E', Predicates.blocks(Blocks.WARPED_HYPHAE))
                        .where('F', Predicates.blocks(TUNGSTENSTEEL_CRATE.getBlock()))
                        .where('G', Predicates.blocks(ChemicalHelper.getBlock(frameGt, TreatedWood)))
                        .where('H', Predicates.blocks(CASING_TUNGSTENSTEEL_PIPE.get()))
                        .where('I', Predicates.blocks(Blocks.HONEYCOMB_BLOCK))
                        .where('J', Predicates.blocks(Blocks.HONEY_BLOCK))
                        .where('K', Predicates.blocks(Blocks.POPPY))

                        // Direct simplified controller definition assignment
                        .where('L', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_sturdy_hsse"),
                    GTCEu.id("block/multiblock/implosion_compressor"))
            .register();

    public static final MultiblockMachineDefinition DIMENSIONAL_ANCHOR = REGISTRATE
            .multiblock("dimensional_anchor", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(PhoenixRecipeTypes.DIMENSIONAL_ANCHORING_RECIPES)
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(CASING_TITANIUM_STABLE)
            .tooltipBuilder(DIMENSIONAL_ANCHOR_TOOLTIPS)
            .pattern(definition -> {
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BCDCB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("CCCCC", "BEFEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("DCCCD", "EFFFE", "EGHGE", "EEGEE", "BEGEB", "BEGEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB", "BBEBB", "BBEBB")
                        .slice("CCCCC", "BEFEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("BCICB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")

                        .where('B', Predicates.any())
                        // Added controller fallback rule inside structural casing map 'C'
                        .where('C', Predicates.blocks(CASING_TITANIUM_STABLE.get()).setMinGlobalLimited(5)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))
                        .where('D', Predicates.blocks(FIREBOX_TITANIUM.get()))
                        .where('E', Predicates.blocks(ChemicalHelper.getBlock(frameGt, SOURCE_IMBUED_TITANIUM)))
                        .where('F', Predicates.blocks(COIL_NICHROME.get()))
                        .where('G', Predicates.blocks(FIREBOX_TITANIUM.get()))
                        .where('H', Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        // Direct simplified controller definition assignment
                        .where('I', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("gtceu:block/casings/solid/machine_casing_stable_titanium"),
                    GTCEu.id("gtceu:block/multiblock/large_miner"))
            .register();

    public static final MultiblockMachineDefinition AETHERIAL_FABIRCATOR = REGISTRATE
            .multiblock("aetherical_fabricator", WorkableElectricMultiblockMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(PhoenixRecipeTypes.AETHERIAL_FABIRCATION_RECIPES)
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT_SUBTICK, GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
            .pattern(definition -> {
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("BCCCB", "BDDDB", "BCCCB", "BBCBB", "BBCBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("CEEEC", "DAAAD", "CAAAC", "BEAEB", "BEAEB", "BEEEB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("CEEEC", "DAAAD", "CAAAC", "CAAAC", "CAAAC", "BEAEB", "BEAEB", "BEAEB", "BBCBB", "BBDBB", "BBCBB")
                        .slice("CEEEC", "DAAAD", "CAAAC", "BEAEB", "BEAEB", "BEEEB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB")
                        .slice("BCCCB", "BCFCB", "BCCCB", "BBCBB", "BBCBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")

                        .where('A', Predicates.air())
                        .where('B', Predicates.any())
                        // Added controller fallback rule inside structural casing map 'C'
                        .where('C', Predicates.blocks(CASING_STAINLESS_CLEAN.get()).setMinGlobalLimited(5)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.controller(definition)))
                        .where('D', Predicates.blocks(COIL_KANTHAL.get()))
                        .where('E', Predicates.blocks(ChemicalHelper.getBlock(frameGt, FROST_REINFORCED_STAINED_STEEL)))
                        // Direct simplified controller definition assignment
                        .where('F', Predicates.controller(definition))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                    GTCEu.id("block/multiblock/large_miner"))
            .tooltipBuilder(AETHERIAL_FABRICATOR_TOOLTIPS)
            .register();

    public static final MultiblockMachineDefinition JUKEBLOCK = REGISTRATE
            .multiblock("jukeblock", ResonantJukeboxMachine::new)
            .rotationState(RotationState.ALL)
            .recipeTypes(PhoenixRecipeTypes.JUKEBLOCK)
            .langValue("Jukeblock ")
            .noRecipeModifier()
            .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
            .pattern(definition -> {
                // Modernized pattern layout using direction-relative slicing
                return MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT) //
                        .slice("aafaa", "bbcbb", "bdcdb", "bdcdb", "dbbbd")
                        .slice("aaaaa", "beeeb", "ddddd", "ddddd", "bbdbb")
                        .slice("aaaaa", "ceeec", "cdddc", "cdddc", "bdddb")
                        .slice("aaaaa", "beeeb", "ddddd", "ddddd", "bbdbb")
                        .slice("aaaaa", "bbcbb", "bdcdb", "bdcdd", "dbbbd")

                        // Core structural casing mapped safely with integrated controller fallback loop
                        .where('a', Predicates.blocks(CASING_STEEL_SOLID.get())
                                .or(PhoenixPredicates.soundHatches())
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY))
                                .or(Predicates.controller(definition))) //

                        .where('b', Predicates.blocks(ChemicalHelper.getBlock(frameGt, AURUM_STEEL)))
                        .where('c', Predicates.blocks(CASING_TEMPERED_GLASS.get()))
                        .where('d', Predicates.air()) //
                        .where('e', PhoenixPredicates.tieredSpeakers())

                        // Modernized direct Controller definition mapping
                        .where('f', Predicates.controller(definition)) //
                        .build();
            })
            .workableCasingModel(
                    GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                    new ResourceLocation("minecraft", "block/note_block")
            )
            .register();

    public static void init() {}
}
