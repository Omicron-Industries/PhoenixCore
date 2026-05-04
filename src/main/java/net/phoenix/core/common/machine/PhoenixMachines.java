package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidPipeProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.item.DrumMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.CleaningMaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.storage.CrateMachine;
import com.gregtechceu.gtceu.common.machine.storage.DrumMachine;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
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

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.capability.recipe.IO.IN;
import static com.gregtechceu.gtceu.api.capability.recipe.IO.OUT;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt;
import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
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

    public static MultiblockMachineDefinition registerMultiblockSourceTank(String name, String displayName,
                                                                           int capacity,
                                                                           Supplier<? extends Block> casing,
                                                                           int maxConsumption,
                                                                           BiConsumer<MultiblockMachineBuilder<?, ?>, ResourceLocation> rendererSetup) {
        return registerMultiblockSourceTank(PhoenixRegistration.REGISTRATE, name, displayName, capacity, maxConsumption,
                casing, rendererSetup);
    }

    public static MultiblockMachineDefinition registerMultiblockSourceTank(GTRegistrate registrate, String name,
                                                                           String displayName, int capacity,
                                                                           int maxConsumption,
                                                                           Supplier<? extends Block> casing,
                                                                           BiConsumer<MultiblockMachineBuilder<?, ?>, ResourceLocation> rendererSetup) {
        MultiblockMachineBuilder<?, ?> builder = registrate
                .multiblock(name, holder -> new SourceMultiblockTankMachine(holder, capacity, maxConsumption))
                .langValue(displayName)
                .tooltips(
                        Component.translatable("phoenixcore.machine.multiblock.source_tank.tooltip"),
                        Component.translatable("phoenixcore.universal.tooltip.source_storage_capacity", capacity))
                .rotationState(RotationState.ALL)
                .recipeType(DUMMY_RECIPES)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("CCC", "CCC", "CCC")
                        .aisle("CCC", "C#C", "CCC")
                        .aisle("CCC", "CSC", "CCC")
                        .where('S', controller(blocks(definition.get())))
                        .where('C', blocks(casing.get())
                                .or(abilities(PhoenixPartAbility.SOURCE_INPUT).setExactLimit(1))
                                .or(abilities(PhoenixPartAbility.SOURCE_OUTPUT).setExactLimit(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('#', air())
                        .build())
                .shapeInfo(definition -> MultiblockShapeInfo.builder()
                        .aisle("CCC", "CCC", "CCC")
                        .aisle("CCC", "C#C", "CCC")
                        .aisle("CCC", "CSC", "CCC")
                        .where('S', definition.get(), Direction.NORTH)
                        .where('C', casing.get().defaultBlockState())
                        .where('#', Blocks.AIR.defaultBlockState())
                        .build())
                .appearanceBlock(casing);

        rendererSetup.accept(builder, GTCEu.id("block/multiblock/multiblock_tank"));
        return builder.register();
    }

    /*
     * public static final MultiblockMachineDefinition REFINED_MULTIBLOCK_SOURCE_TANK = registerMultiblockSourceTank(
     * "refined_multiblock_source_tank",
     * "Refined Multiblock Source Tank",
     * 50000 * 1000,
     * SOURCE_FIBER_MACHINE_CASING,
     * 100,
     * (builder, overlay) -> builder
     * .workableCasingModel(PhoenixCore.id("block/casings/multiblock/machine_casing_source_fiber_mesh"),
     * overlay));
     * 
     */

    public static final MachineDefinition[] SOURCE_IMPORT_HATCH = registerSourceHatch(
            "source_input_hatch", "Source Input Hatch",
            IO.IN, ELECTRIC_TIERS, SOURCE_INPUT);
    public static final MachineDefinition[] SOURCE_EXPORT_HATCH = registerSourceHatch(
            "source_output_hatch", "Source Output Hatch",
            IO.OUT, ELECTRIC_TIERS, SOURCE_OUTPUT);

    private static MachineDefinition[] registerSourceHatch(String name, String displayName, IO io,
                                                           int[] tiers, PartAbility... abilities) {
        return registerTieredMachines(name,
                (holder, tier) -> new SourceHatchPartMachine((MetaMachineBlockEntity) holder, tier, io),
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
                                                             BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory,
                                                             BiFunction<Integer, MachineBuilder<MachineDefinition, ?>, MachineDefinition> builder,
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
                .machine(material.getName() + "_drum", MachineDefinition::new,
                        holder -> new DrumMachine(holder, material, capacity), MetaMachineBlock::new,
                        (holder, prop) -> DrumMachineItem.create(holder, prop, material),
                        MetaMachineBlockEntity::new)
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

        return registrate.machine(material.getName() + "_crate", holder -> new CrateMachine(holder, material, capacity))
                .langValue(lang)
                .rotationState(RotationState.NONE)
                .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", capacity))
                .modelProperty(GTMachineModelProperties.IS_TAPED, false)
                .model(GTMachineModels.createCrateModel(wooden))
                .paintingColor(wooden ? 0xFFFFFF : material.getMaterialRGB())
                .itemColor((s, t) -> wooden ? 0xFFFFFF : material.getMaterialRGB())
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
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("########BBCCCBB########", "########DDEEEDD########", "########DDEEEDD########",
                            "########DDEEEDD########", "########BBCCCBB########")
                    .aisle("######BBBBBBBBBBB######", "######FFAAAAAAAFF######", "######FFAAAAAAAFF######",
                            "######FFAAAAAAAFF######", "######BBBBBBBBBBB######")
                    .aisle("####BBBBBBBBBBBBBBB####", "####GGAAAAAAAAAAAGG####", "####GGAAAAAAAAAAAGG####",
                            "####GGAAAAAAAAAAAGG####", "####BBBBBBBBBBBBBBB####")
                    .aisle("###BBBBBBBBBBBBBBBBB###", "###GAAAAAAAAAAAAAAAG###", "###GAAAAAAAAAAAAAAAG###",
                            "###GAAAAAAAAAAAAAAAG###", "###BBBBBBBBBBBBBBBBB###")
                    .aisle("##BBBBBBBBBBBBBBBBBBB##", "##GAAAAAAAAAAAAAAAAAG##", "##GAAAAAAAHHHAAAAAAAG##",
                            "##GAAAAAAAAAAAAAAAAAG##", "##BBBBBBBBBBBBBBBBBBB##")
                    .aisle("##BBBBBBBBIIIBBBBBBBB##", "##GAAAAAAAHHHAAAAAAAG##", "##GAAAAAHHAAAHHAAAAAG##",
                            "##GAAAAAAAHHHAAAAAAAG##", "##BBBBBBBBIIIBBBBBBBB##")
                    .aisle("#BBBBBBBBIIEIIBBBBBBBB#", "#FAAAAAAHHAAAHHAAAAAAG#", "#FAAAAAHAAHHHAAHAAAAAG#",
                            "#FAAAAAAHHAAAHHAAAAAAG#", "#BBBBBBBBIIEIIBBBBBBBB#")
                    .aisle("#BBBBBBBIIEJEIIBBBBBBB#", "#FAAAAAHAAAAAAAHAAAAAG#", "#FAAAAHFHHAKAHHFHAAAAG#",
                            "#FAAAAAHAAAAAAAHAAAAAG#", "#BBBBBBBIIEJEIIBBBBBBB#")
                    .aisle("BBBBBBBIIEJJJEIIBBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHKAAKAAKHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBBIIEJJJEIIBBBBBBB")
                    .aisle("BBBBBBIIEJJJJJEIIBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHAKAKAKAHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBIIEJJJJJEIIBBBBBB")
                    .aisle("CBBBBIIEJJJCJJJEIIBBBBC", "EAAAAHAAAAAAAAAAAHAAAAE", "EAAAHAHAAAKKKAAAHAHAAAE",
                            "EAAAAHAAAAAAAAAAAHAAAAE", "CBBBBIIEJJJCJJJEIIBBBBC")
                    .aisle("CBBBBIEJJJCCCJJJEIBBBBC", "EAAAAHAAAAAKAAAAAHAAAAE", "EAAAHAHKKKKLKKKKHAHAAAE",
                            "EAAAAHAAAAAKAAAAAHAAAAE", "CBBBBIEJJJCCCJJJEIBBBBC")
                    .aisle("CBBBBIIEJJJCJJJEIIBBBBC", "EAAAAHAAAAAAAAAAAHAAAAE", "EAAAHAHAAAKKKAAAHAHAAAE",
                            "EAAAAHAAAAAAAAAAAHAAAAE", "CBBBBIIEJJJCJJJEIIBBBBC")
                    .aisle("BBBBBBIIEJJJJJEIIBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHAKAKAKAHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBIIEJJJJJEIIBBBBBB")
                    .aisle("BBBBBBBIIEJJJEIIBBBBBBB", "DAAAAAHAAAAAAAAAHAAAAAD", "DAAAAHAHKAAKAAKHAHAAAAD",
                            "DAAAAAHAAAAAAAAAHAAAAAD", "BBBBBBBIIEJJJEIIBBBBBBB")
                    .aisle("#BBBBBBBIIEJEIIBBBBBBB#", "#FAAAAAHAAAAAAAHAAAAAG#", "#FAAAAHFHHAKAHHFHAAAAG#",
                            "#FAAAAAHAAAAAAAHAAAAAG#", "#BBBBBBBIIEJEIIBBBBBBB#")
                    .aisle("#BBBBBBBBIIEIIBBBBBBBB#", "#FAAAAAAHHAAAHHAAAAAAG#", "#FAAAAAHAAHHHAAHAAAAAG#",
                            "#FAAAAAAHHAAAHHAAAAAAG#", "#BBBBBBBBIIEIIBBBBBBBB#")
                    .aisle("##BBBBBBBBIIIBBBBBBBB##", "##GAAAAAAAHHHAAAAAAAG##", "##GAAAAAHHAAAHHAAAAAG##",
                            "##GAAAAAAAHHHAAAAAAAG##", "##BBBBBBBBIIIBBBBBBBB##")
                    .aisle("##BBBBBBBBBBBBBBBBBBB##", "##GAAAAAAAAAAAAAAAAAG##", "##GAAAAAAAHHHAAAAAAAG##",
                            "##GAAAAAAAAAAAAAAAAAG##", "##BBBBBBBBBBBBBBBBBBB##")
                    .aisle("###BBBBBBBBBBBBBBBBB###", "###GAAAAAAAAAAAAAAAG###", "###GAAAAAAAAAAAAAAAG###",
                            "###GAAAAAAAAAAAAAAAG###", "###BBBBBBBBBBBBBBBBB###")
                    .aisle("####BBBBBBBBBBBBBBB####", "####GGAAAAAAAAAAAGG####", "####GGAAAAAAAAAAAGG####",
                            "####GGAAAAAAAAAAAGG####", "####BBBBBBBBBBBBBBB####")
                    .aisle("######BBBBBBBBBBB######", "######FFAAAAAAAFF######", "######FFAAAAAAAFF######",
                            "######FFAAAAAAAFF######", "######BBBBBBBBBBB######")
                    .aisle("########BBCCCBB########", "########DDEEEDD########", "########DDENEDD########",
                            "########DDEEEDD########", "########BBCCCBB########")
                    .where("N", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("A", Predicates.air())
                    .where("#", Predicates.any())
                    .where("B", Predicates.blocks(PhoenixBlocks.INSANELY_SUPERCHARGED_TESLA_CASING.get()))
                    .where("C", Predicates.blocks(ADVANCED_COMPUTER_CASING.get()))
                    .where("D", Predicates.blocks(COMPUTER_CASING.get()))
                    .where("E", Predicates.blocks(FUSION_COIL.get()).setMinGlobalLimited(10)
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.blocks(GTMachines.ENERGY_INPUT_HATCH[GTValues.UHV].get())
                                    .setMaxGlobalLimited(4)))
                    .where("F", Predicates.blocks(SUPERCONDUCTING_COIL.get()))
                    .where("G", Predicates.blocks(FUSION_GLASS.get()))
                    .where("H", PhoenixPredicates.lampsByColor(DyeColor.BLUE))
                    .where("I", Predicates.blocks(CASING_STEEL_SOLID.get()))
                    .where("J", Predicates.blocks(CASING_STAINLESS_CLEAN.get()))
                    .where("K", Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where("L", Predicates.blocks(CASING_PRIMITIVE_BRICKS.get()))
                    .build())
            .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
            .model(createWorkableCasingMachineModel(
                    new ResourceLocation(PhoenixCore.MOD_ID, "block/casings/multiblock/tesla_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor")))
            .hasBER(true)
            .register();

    static {
        if (PhoenixConfigs.INSTANCE.features.creativeEnergyEnabled) {
            DANCE = REGISTRATE
                    .multiblock("phoenix_infuser", WorkableElectricMultiblockMachine::new)
                    .langValue("§cPhoenix Infuser")
                    .rotationState(RotationState.NON_Y_AXIS)
                    .recipeType(PhoenixRecipeTypes.PLEASE)
                    .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                            GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK))
                    .pattern(definition -> FactoryBlockPattern.start()
                            .aisle("BBAAAAAAAAAAAAAAAAAAAAAAAAAAAAABB", "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "AABBBAAAAAAAAAAACAAAAAAAAAAABBBAA",
                                    "AAABBBAAAAAAAAACCCAAAAAAAAABBBAAA", "AAAABBBAAAAAAACCDCCAAAAAAABBBAAAA",
                                    "AAAAABBBAAAAACCEDECCAAAAABBBAAAAA", "AAAAAABBBAAACCFEDEFCCAAABBBAAAAAA",
                                    "AAAAAAABBBACCFGEHEGFCCABBBAAAAAAA", "AAAAAAAABBBCFIIEHEIIFCBBBAAAAAAAA",
                                    "AAAAAAAAABBBFIIEHEIIFBBBAAAAAAAAA", "AAAAAAAACCBBBFGEHEGFBBBCCAAAAAAAA",
                                    "AAAAAAACCFFBBBFEGEFBBBFFCCAAAAAAA", "AAAAAACCFIIFBBBEGEBBBFIIFCCAAAAAA",
                                    "AAAAACCFGIIGFBBBGBBBFGIIGFCCAAAAA", "AAAACCEEEEEEEEBBBBBEEEEEEEECCAAAA",
                                    "AAACCDDDHHHHGGGBBBGGGHHHHDDDCCAAA", "AAAACCEEEEEEEEBBBBBEEEEEEEECCAAAA",
                                    "AAAAACCFGIIGFBBBGBBBFGIIGFCCAAAAA", "AAAAAACCFIIFBBBEGEBBBFIIFCCAAAAAA",
                                    "AAAAAAACCFFBBBFEGEFBBBFFCCAAAAAAA", "AAAAAAAACCBBBFGEHEGFBBBCCAAAAAAAA",
                                    "AAAAAAAAABBBFIIEHEIIFBBBAAAAAAAAA", "AAAAAAAABBBCFIIEHEIIFCBBBAAAAAAAA",
                                    "AAAAAAABBBACCFGEHEGFCCABBBAAAAAAA", "AAAAAABBBAAACCFEDEFCCAAABBBAAAAAA",
                                    "AAAAABBBAAAAACCEDECCAAAAABBBAAAAA", "AAAABBBAAAAAAACCDCCAAAAAAABBBAAAA",
                                    "AAABBBAAAAAAAAACCCAAAAAAAAABBBAAA", "AABBBAAAAAAAAAAACAAAAAAAAAAABBBAA",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB",
                                    "BBAAAAAAAAAAAAAAAAAAAAAAAAAAAAABB")
                            .aisle("BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAGGGGGGGAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAJJGGGGGJJAAAAAAAAAAAA", "AAAAAAAAAAAAAGGGGGGGAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB")
                            .aisle("ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAJJAAAAAAAJJAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA")
                            .aisle("AABBBAAAAAAAAAAAAAAAAAAAAAAABBBAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAJJAAAAAAAAAJJAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AABBBAAAAAAAAAAAAAAAAAAAAAAABBBAA")
                            .aisle("AAABBBAAAAAAAAAAAAAAAAAAAAABBBAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAJJAAAAAAAAAAAJJAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAABBBAAAAAAAAAAAAAAAAAAAAABBBAAA")
                            .aisle("AAAABBBAAAAAAAAAAAAAAAAAAABBBAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "DAAAAAAAJJAAAAAAAAAAAAAJJAAAAAAAD", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAABBBAAAAAAAAAAAAAAAAAAABBBAAAA")
                            .aisle("AAAAABBBAAAAAAAACAAAAAAAABBBAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "DAAAAAAJJAAAAAAJJJAAAAAAJJAAAAAAD", "EAAAAAAAAAAAAJJJJJJJAAAAAAAAAAAAE",
                                    "CAAAAAAAAAAAAAAJJJAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAABBBAAAAAAAACAAAAAAAABBBAAAAA")
                            .aisle("AAAAAABBBAAAAAACCCAAAAAABBBAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF", "EAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAE",
                                    "DAAAAAJJEAAAJJJJJJJJJAAAEJJAAAAAD", "EAAAAAAEEAAJJJJJJJJJJJAAEEAAAAAAE",
                                    "FAAAAAAEEAAAJJJJJJJJJAAAEEAAAAAAF", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAABBBAAAAAACCCAAAAAABBBAAAAAA")
                            .aisle("AAAAAAABBBAAAACCDCCAAAABBBAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF",
                                    "GAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAG", "EAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAE",
                                    "HAAAAJJEEAJJJJJJJJJJJJJAEEJJAAAAH", "EAAAAAAEEAJJJJJJJJJJJJJAEEAAAAAAE",
                                    "GAAAAAAEEAJJJJJJJJJJJJJAEEAAAAAAG", "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAABBBAAAACCGCCAAAABBBAAAAAAA")
                            .aisle("AAAAAAAABBBAACCEDECCAABBBAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "HAAAJJAAAJJJJAAAAAAAJJJJAAAJJAAAH", "EAAAAAAAAJJJJJAAKAAJJJJJAAAAAAAAE",
                                    "IAAAAAAAAJJJJAAAAAAAJJJJAAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAABBBAACCEGECCAABBBAAAAAAAA")
                            .aisle("AAAAAAAAABBBCCEEHEECCBBBAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "HAAJJAAAJJJAAAAAKAAAAAJJJAAAJJAAH", "EAAAAAAAJJJJAAAKKKAAAJJJJAAAAAAAE",
                                    "IAAAAAAAJJJAAAAAKAAAAAJJJAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAABBBCCEEHEECCBBBAAAAAAAAA")
                            .aisle("AAAAAAAAAABBBDDHHHDDBBBAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "EAAAAAAAAAAAAAALLLAAAAAAAAAAAAAAE",
                                    "HAJJAAAAJJAAAALLLLLAAAAJJAAAAJJAH", "EAAAAAAJJJJAAALLLLLAAAJJJJAAAAAAE",
                                    "GAAAAAAAJJAAAALLLLLAAAAJJAAAAAAAG", "FAAAAAAAAAAAAAALLLAAAAAAAAAAAAAAF",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAABBBGGHHHGGBBBAAAAAAAAAA")
                            .aisle("AAAAAAAAAACBBBEEHEEBBBCAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAF", "EAAAAAAAAAAAALLAAALLAAAAAAAAAAAAE",
                                    "GJJAAAAJJJAAALAMMMALAAAJJJAAAAJJG", "EAAAAAAJJJAAALAMMMALAAAJJJAAAAAAE",
                                    "FAAAAAAJJJAAALAMMMALAAAJJJAAAAAAF", "BAAAAAAAAAAAALLAAALLAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAACBBBEEHEEBBBCAAAAAAAAAA")
                            .aisle("AAAAAAAAACCDBBBEGEBBBDCCAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAKAAAAAKAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "BAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAKLNAAANLKAAAAAAAAAAAB", "EGAAAAAAAAAALKAAAAAKLAAAAAAAAAAGE",
                                    "GJAAAAAJJAAALKAAAAAKLAAAJJAAAAAJG", "EGAAAAJJJJAALKAAKAAKLAAJJJJAAAAGE",
                                    "BAAAAAAJJAAALKAAAAAKLAAAJJAAAAAAB", "BAAAAAAAAAAALKAAAAAKLAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAKLNAAANLKAAAAAAAAAAAB", "FAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAF",
                                    "IAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAC", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAACCGBBBEGEBBBGCCAAAAAAAAA")
                            .aisle("AAAAAAAACCEDEBBBGBBBEDECCAAAAAAAA", "AAAAAAAAAAAAAACCCCCAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEAEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAC", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "GAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAG",
                                    "FAAAAAAAAAAAAAKLLLKAAAAAAAAAAAAAF", "BAAAAAAAAAAAKLLAAALLKAAAAAAAAAAAB",
                                    "BAAAAAAAAAAALNKAAAKNLAAAAAAAAAAAB", "BGAAAAAAAAAALAAAAAAALAAAAAAAAAAGB",
                                    "GGAAAAAJJAALAAAAAAAAALAAJJAAAAAGG", "BGAAAAJJJAALAAAAKAAAALAAJJJAAAAGB",
                                    "BAAAAAAJJAALAAAAAAAAALAAJJAAAAAAB", "BAAAAAAAAAAALAAAAAAALAAAAAAAAAAAB",
                                    "FAAAAAAAAAAALNKAAAKNLAAAAAAAAAAAF", "GAAAAAAAAAAAALLAAALLAAAAAAAAAAAAG",
                                    "IAAAAAAAAAAAAKKLLLKKAAAAAAAAAAAAI", "IAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAI",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "CAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAEEKKKEEAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEAEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAACCCCCAAAAAAAAAAAAAA",
                                    "AAAAAAAACCEGEBBBGBBBEGECCAAAAAAAA")
                            .aisle("AAAAAAACCEEHEEBBBBBEEHEECCAAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEKEEAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "EAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAOOOAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAKGGGKAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAE", "EAAAAAAAAAAAALAMMMALAAAAAAAAAAAAE",
                                    "BAAAAAAAAAAALAAAAAAALAAAAAAAAAAAB", "BGAAAAAAAAALAAAAPAAAALAAAAAAAAAGB",
                                    "BGAAAAJJJAALMAAPAPAAMLAAJJJAAAAGB", "BGAAAAJJJAKLMAAAKAAAMLKAJJJAAAAGB",
                                    "BAAAAAJJJAALMAAPAPAAMLAAJJJAAAAAB", "EAAAAAAAAAALAAAAPAAAALAAAAAAAAAAE",
                                    "EAAAAAAAAAAALAAAAAAALAAAAAAAAAAAE", "EAAAAAAAAAAAALAMMMALAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAALLLLLAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAKGGGKAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAE", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAEEKEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAAACCEEHEEBBBBBEEHEECCAAAAAAA")
                            .aisle("AAAAAACCDDHHHGGBBBGGHHHDDCCAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAKQKAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAC", "DAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAD",
                                    "DAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAD", "DAAAAAAAAAAAAAAOOOAAAAAAAAAAAAAAD",
                                    "HAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAH", "HAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAH",
                                    "HAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAH", "HAAAAAAAAAAAAAAGQGAAAAAAAAAAAAAAH",
                                    "GAAAAAAAAAAAAALLLLLAAAAAAAAAAAAAG", "GAAAAAAAAAAAALAMMMALAAAAAAAAAAAAG",
                                    "GAAAAAAAAAAALAAAKAAALAAAAAAAAAAAG", "BGAAAAAGAAALAAAPKPAAALAAAAAAAAAGB",
                                    "BGAAAAJJJAKLMAAAKAAAMLKAJJJAAAAGB", "BGAAAAJJJKKLMKKKRKKKMLKKJJJAAAAGB",
                                    "GAAAAAJJJAKLMAAAKAAAMLKAJJJAAAAAG", "GAAAAAAAAAALAAAPKPAAALAAAAAAAAAAG",
                                    "GAAAAAAAAAAALAAAKAAALAAAAAAAAAAAG", "HAAAAAAAAAAAALAMMMALAAAAAAAAAAAAH",
                                    "HAAAAAAAAAAAAALLLLLAAAAAAAAAAAAAH", "HAAAAAAAAAAAAAAGQGAAAAAAAAAAAAAAH",
                                    "HAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAH", "DAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAD",
                                    "DAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAD", "DAAAAAAAAAAAAAKAKAKAAAAAAAAAAAAAD",
                                    "CAAAAAAAAAAAAAKAKAKAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAKQKAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAACCGGHHHGGBBBGGHHHGGCCAAAAAA")
                            .aisle("AAAAAAACCEEHEEBBBBBEEHEECCAAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEKEEAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "EAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAOOOAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAKKKAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAKGGGKAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAE", "EAAAAAAAAAAAALAMMMALAAAAAAAAAAAAE",
                                    "BAAAAAAAAAAALAAAAAAALAAAAAAAAAAAB", "BGAAAAAAAAALAAAAPAAAALAAAAAAAAAGB",
                                    "BGAAAAJJJAALMAAPAPAAMLAAJJJAAAAGB", "BGAAAAJJJAKLMAAAKAAAMLKAJJJAAAAGB",
                                    "BAAAAAJJJAALMAAPAPAAMLAAJJJAAAAAB", "EAAAAAAAAAALAAAAPAAAALAAAAAAAAAAE",
                                    "EAAAAAAAAAAALAAAAAAALAAAAAAAAAAAE", "EAAAAAAAAAAAALAMMMALAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAALLLLLAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAKGGGKAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "EAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAE", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAKAKAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAEEKEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAEEEAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAACGGGCAAAAAAAAAAAAAA",
                                    "AAAAAAACCEEHEEBBBBBEEHEECCAAAAAAA")
                            .aisle("AAAAAAAACCEDEBBBGBBBEDECCAAAAAAAA", "AAAAAAAAAAAAAACCCCCAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEAEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAC", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "GAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAG",
                                    "FAAAAAAAAAAAAAKLLLKAAAAAAAAAAAAAF", "BAAAAAAAAAAAKLLAAALLKAAAAAAAAAAAB",
                                    "BAAAAAAAAAAALNKAAAKNLAAAAAAAAAAAB", "BGAAAAAAAAAALAAAAAAALAAAAAAAAAAGB",
                                    "GGAAAAAJJAALAAAAAAAAALAAJJAAAAAGG", "BGAAAAJJJAALAAAAKAAAALAAJJJAAAAGB",
                                    "BAAAAAAJJAALAAAAAAAAALAAJJAAAAAAB", "BAAAAAAAAAAALAAAAAAALAAAAAAAAAAAB",
                                    "FAAAAAAAAAAALNKAAAKNLAAAAAAAAAAAF", "GAAAAAAAAAAAALLAAALLAAAAAAAAAAAAG",
                                    "IAAAAAAAAAAAAKKLLLKKAAAAAAAAAAAAI", "IAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAI",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "CAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAEEKKKEEAAAAAAAAAAAAA", "AAAAAAAAAAAAAAEEAEEAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAACCCCCAAAAAAAAAAAAAA",
                                    "AAAAAAAACCEGEBBBGBBBEGECCAAAAAAAA")
                            .aisle("AAAAAAAAACCDBBBEGEBBBDCCAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAKAAAAAKAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "BAAAAAAAAAAAAAAKAKAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAKLNAAANLKAAAAAAAAAAAB", "EGAAAAAAAAAALKAAAAAKLAAAAAAAAAAGE",
                                    "GJAAAAAJJAAALKAAAAAKLAAAJJAAAAAJG", "EGAAAAJJJJAALKAAKAAKLAAJJJJAAAAGE",
                                    "BAAAAAAJJAAALKAAAAAKLAAAJJAAAAAAB", "BAAAAAAAAAAALKAAAAAKLAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAKLNAAANLKAAAAAAAAAAAB", "FAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAF",
                                    "IAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAC", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAEEAAAEEAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAACCGBBBEGEBBBGCCAAAAAAAAA")
                            .aisle("AAAAAAAAAACBBBEEHEEBBBCAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAKAAAKAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAF", "EAAAAAAAAAAAALLAAALLAAAAAAAAAAAAE",
                                    "GJJAAAAJJJAAALAMMMALAAAJJJAAAAJJG", "EAAAAAAJJJAAALAMMMALAAAJJJAAAAAAE",
                                    "FAAAAAAJJJAAALAMMMALAAAJJJAAAAAAF", "BAAAAAAAAAAAALLAAALLAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAKLLLLLKAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAAAEEAAAAAEEAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAACBBBEEHEEBBBCAAAAAAAAAA")
                            .aisle("AAAAAAAAAABBBDDHHHDDBBBAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF",
                                    "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG", "EAAAAAAAAAAAAAALLLAAAAAAAAAAAAAAE",
                                    "HAJJAAAAJJAAAALLLLLAAAAJJAAAAJJAH", "EAAAAAAJJJJAAALLLLLAAAJJJJAAAAAAE",
                                    "GAAAAAAAJJAAAALLLLLAAAAJJAAAAAAAG", "FAAAAAAAAAAAAAALLLAAAAAAAAAAAAAAF",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAAAEEAAAAAAAEEAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAABBBGGHHHGGBBBAAAAAAAAAA")
                            .aisle("AAAAAAAAABBBCCEEHEECCBBBAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "HAAJJAAAJJJAAAAAKAAAAAJJJAAAJJAAH", "EAAAAAAAJJJJAAAKKKAAAJJJJAAAAAAAE",
                                    "IAAAAAAAJJJAAAAAKAAAAAJJJAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAA", "AAAAAAAAAAEEAAAAAAAAAEEAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAABBBCCEEHEECCBBBAAAAAAAAA")
                            .aisle("AAAAAAAABBBAACCEDECCAABBBAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "HAAAJJAAAJJJJAAAAAAAJJJJAAAJJAAAH", "EAAAAAAAAJJJJJAAKAAJJJJJAAAAAAAAE",
                                    "IAAAAAAAAJJJJAAAAAAAJJJJAAAAAAAAI", "IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI",
                                    "FAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAEEAAAAAAAAAAAEEAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAABBBAACCEGECCAABBBAAAAAAAA")
                            .aisle("AAAAAAABBBAAAACCDCCAAAABBBAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF",
                                    "GAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAG", "EAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAE",
                                    "HAAAAJJEEAJJJJJJJJJJJJJAEJJJAAAAH", "EAAAAAAEEAJJJJJJJJJJJJJAEEAAAAAAE",
                                    "GAAAAAAEEAJJJJJJJJJJJJJAEEAAAAAAG", "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "BAAAAAAAEEAAAAAAAAAAAAAEEAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAABBBAAAACCGCCAAAABBBAAAAAAA")
                            .aisle("AAAAAABBBAAAAAACCCAAAAAABBBAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "FAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAF", "EAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAE",
                                    "DAAAAAJJEAAAJJJJJJJJJAAAJJJAAAAAD", "EAAAAAAEEAAJJJJJJJJJJJAAEEAAAAAAE",
                                    "FAAAAAAEEAAAJJJJJJJJJAAAEEAAAAAAF", "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC",
                                    "CAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAC", "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA",
                                    "AAAAAAAEEAAAAAAAAAAAAAAAEEAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAABBBAAAAAACCCAAAAAABBBAAAAAA")
                            .aisle("AAAAABBBAAAAAAAACAAAAAAAABBBAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE",
                                    "DAAAAAAJJAAAAAAJJJAAAAAAJJAAAAAAD", "EAAAAAAAAAAAAJJJJJJJAAAAAAAAAAAAE",
                                    "CAAAAAAAAAAAAAAJJJAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAABBBAAAAAAAACAAAAAAAABBBAAAAA")
                            .aisle("AAAABBBAAAAAAAAAAAAAAAAAAABBBAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "DAAAAAAAJJAAAAAAAAAAAAAJJAAAAAAAD", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAABBBAAAAAAAAAAAAAAAAAAABBBAAAA")
                            .aisle("AAABBBAAAAAAAAAAAAAAAAAAAAABBBAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "CAAAAAAAAJJAAAAAAAAAAAJJAAAAAAAAC", "CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAABBBAAAAAAAAAAAAAAAAAAAAABBBAAA")
                            .aisle("AABBBAAAAAAAAAAAAAAAAAAAAAAABBBAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "CAAAAAAAAAJJAAAAAAAAAJJAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AABBBAAAAAAAAAAAAAAAAAAAAAAABBBAA")
                            .aisle("ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAJJAAAAAAAJJAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA")
                            .aisle("BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAGGGGGGGAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAJJGGGGGJJAAAAAAAAAAAA", "AAAAAAAAAAAAAGGGGGGGAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB", "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                                    "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB")
                            .aisle("BBAAAAAAAAAAAAAAAAAAAAAAAAAAAAABB", "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "AABBBAAAAAAAAAAACAAAAAAAAAAABBBAA",
                                    "AAABBBAAAAAAAAACCCAAAAAAAAABBBAAA", "AAAABBBAAAAAAACCDCCAAAAAAABBBAAAA",
                                    "AAAAABBBAAAAACCEDECCAAAAABBBAAAAA", "AAAAAABBBAAACCFEDEFCCAAABBBAAAAAA",
                                    "AAAAAAABBBACCFGEHEGFCCABBBAAAAAAA", "AAAAAAAABBBCFIIEHEIIFCBBBAAAAAAAA",
                                    "AAAAAAAAABBBFIIEHEIIFBBBAAAAAAAAA", "AAAAAAAACCBBBFGEHEGFBBBCCAAAAAAAA",
                                    "AAAAAAACCFFBBBFEGEFBBBFFCCAAAAAAA", "AAAAAACCFIIFBBBEGEBBBFIIFCCAAAAAA",
                                    "AAAAACCFGIIGFBBBGBBBFGIIGFCCAAAAA", "AAAACCEEEEEEEEBBBBBEEEEEEEECCAAAA",
                                    "AAACCDDDHHHHGGGBSBGGGHHHHDDDCCAAA", "AAAACCEEEEEEEEBBBBBEEEEEEEECCAAAA",
                                    "AAAAACCFGIIGFBBBGBBBFGIIGFCCAAAAA", "AAAAAACCFIIFBBBEGEBBBFIIFCCAAAAAA",
                                    "AAAAAAACCFFBBBFEGEFBBBFFCCAAAAAAA", "AAAAAAAACCBBBFGEHEGFBBBCCAAAAAAAA",
                                    "AAAAAAAAABBBFIIEHEIIFBBBAAAAAAAAA", "AAAAAAAABBBCFIIEHEIIFCBBBAAAAAAAA",
                                    "AAAAAAABBBACCFGEHEGFCCABBBAAAAAAA", "AAAAAABBBAAACCFEDEFCCAAABBBAAAAAA",
                                    "AAAAABBBAAAAACCEDECCAAAAABBBAAAAA", "AAAABBBAAAAAAACCDCCAAAAAAABBBAAAA",
                                    "AAABBBAAAAAAAAACCCAAAAAAAAABBBAAA", "AABBBAAAAAAAAAAACAAAAAAAAAAABBBAA",
                                    "ABBBAAAAAAAAAAAAAAAAAAAAAAAAABBBA", "BBBAAAAAAAAAAAAAAAAAAAAAAAAAAABBB",
                                    "BBAAAAAAAAAAAAAAAAAAAAAAAAAAAAABB")
                            .where('A', any())
                            .where('B',
                                    blocks(PhoenixBlocks.PHOENIX_ENRICHED_TRITANIUM_CASING.get())
                                            .setMinGlobalLimited(575).setPreviewCount(1200)
                                            .or(Predicates.blockTag(CustomTags.CLEANROOM_DOORS))
                                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                                            .or(Predicates.abilities(PhoenixPartAbility.PLASMA_INPUT)
                                                    .setPreviewCount(1))
                                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2)
                                                    .setMinGlobalLimited(1))
                                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1)
                                                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS)
                                                            .setPreviewCount(1)))
                                            .or(autoAbilities(true, false, true)))
                            .where('C',
                                    blocks(ChemicalHelper.getBlock(frameGt,
                                            PHOENIX_ENRICHED_TRITANIUM)))
                            .where('D', blocks(ADVANCED_COMPUTER_CASING.get()))
                            .where('E', blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                            .where('F', blocks(COMPUTER_CASING.get()))
                            .where('G', blocks(PhoenixBlocks.COIL_TRUE_HEAT_STABLE.get()))
                            .where('H', blocks(PhoenixBlocks.STABLE_LOGIC_CASING.get()))
                            .where('I', blocks(GTResearchMachines.HPCA_BRIDGE_COMPONENT.get()))
                            .where('J', blocks(PhoenixBlocks.RELIABLE_NAQUADAH_ALLOY_MACHINE_CASING.get()))
                            .where('K',
                                    blocks(ChemicalHelper.getBlock(frameGt,
                                            PHOENIX_ENRICHED_NAQUADAH)))
                            .where('L', blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                    .or(blocks(ForgeRegistries.BLOCKS
                                            .getValue(ResourceLocation.fromNamespaceAndPath("kubejs",
                                                    "phoenix_gaze_panel")))))
                            .where('M', blocks(PhoenixBlocks.BLAZING_CORE_STABILIZER.get()))
                            .where("N",
                                    blocks(ForgeRegistries.BLOCKS.getValue(
                                            ResourceLocation.fromNamespaceAndPath("draconicevolution",
                                                    "awakened_draconium_block"))))
                            .where("O",
                                    blocks(ForgeRegistries.BLOCKS
                                            .getValue(ResourceLocation.fromNamespaceAndPath("expatternprovider",
                                                    "fishbig"))))
                            .where('P', blocks(PhoenixBlocks.GLITCHED_ENTROPY_CASING.get()))
                            .where("Q",
                                    blocks(ForgeRegistries.BLOCKS
                                            .getValue(ResourceLocation.fromNamespaceAndPath("ae2",
                                                    "creative_energy_cell"))))
                            .where('R', blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                            .where('S', controller(blocks(definition.getBlock())))
                            .build())
                    .model(
                            createWorkableCasingMachineModel(
                                    PhoenixCore.id("block/phoenix_enriched_tritanium_casing"),
                                    GTCEu.id("block/multiblock/fusion_reactor"))
                                    .andThen(d -> d
                                            .addDynamicRenderer(
                                                    PhoenixDynamicRenderHelpers::getPlasmaArcFurnaceRenderer)))
                    .hasBER(true)
                    .register();
        }
    }

    static {
        if (PhoenixConfigs.INSTANCE.features.PHPCAEnabled) {
            HIGH_YIELD_PHOTON_EMISSION_REGULATOR = REGISTRATE
                    .multiblock("high_yield_photon_emission_regulator", PhoenixHPCAMachine::new)
                    .langValue("§dHigh Yield Photon Emission Regulator (HPCA)")
                    .tooltips(Component.translatable("phoenixcore.tooltip.hyper_machine_purpose",
                            GTMaterials.get(INSTANCE.features.ActiveCoolerCoolantBase).getLocalizedName()
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolantBase).getMaterialARGB()))),
                            GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant1).getLocalizedName()
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolant1).getMaterialARGB()))),
                            GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant2).getLocalizedName()
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolant2).getMaterialARGB())))),
                            Component.translatable("phoenixcore.tooltip.hyper_machine_1"),
                            Component
                                    .translatable("phoenixcore.tooltip.hyper_machine_coolant_base",
                                            GTMaterials.get(INSTANCE.features.ActiveCoolerCoolantBase)
                                                    .getLocalizedName(),
                                            INSTANCE.features.BaseCoolantBoost)
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolantBase).getMaterialARGB()))),
                            Component.translatable("phoenixcore.tooltip.hyper_machine_coolant2",
                                    GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant1).getLocalizedName(),
                                    INSTANCE.features.CoolantBoost1)
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolant1).getMaterialARGB()))),
                            Component
                                    .translatable("phoenixcore.tooltip.hyper_machine_coolant3",
                                            GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant2).getLocalizedName(),
                                            INSTANCE.features.CoolantBoost2)
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(GTMaterials
                                            .get(INSTANCE.features.ActiveCoolerCoolant2).getMaterialARGB()))))
                    .rotationState(RotationState.NON_Y_AXIS)
                    .appearanceBlock(ADVANCED_COMPUTER_CASING)
                    .recipeType(GTRecipeTypes.DUMMY_RECIPES)
                    .tooltips(LangHandler.getMultiLang("gtceu.machine.high_performance_computation_array.tooltip"))
                    .pattern(definition -> FactoryBlockPattern.start()
                            .aisle("BBBBCCCBBBB", "CDDCCCCCDDC", "CCDCCCCCDCC", "CCCCCCCCCCC", "CCCCCCCCCCC",
                                    "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC")
                            .aisle("BEEBBBBBEEB", "DEEEFFFEEED", "DEEFGGGFEED", "DDDFGGGFDDD", "CBBFGGGFBBC",
                                    "CBBFGGGFBBC", "CCBFGGGFBCC", "CCBEFFFEBCC", "CCBBBBBBBCC", "CCBBBBBBBCC")
                            .aisle("BBHHIIIHHBB", "CDDAAAAADDC", "CIDAAAAADIC", "CIAAAAAAAIC", "CIAAAAAAAIC",
                                    "CEAAAAAAAEC", "CEAAAAAAAEC", "CEAAAAAAAEC", "CEAAAAAAAEC", "CBBIIIIIBBC")
                            .aisle("CBIIIIIIIBC", "CJAAAKAAAJC", "CJAAAKAAAJC", "CJAAAKAAAJC", "CFAAAKAAAFC",
                                    "CLAAAKAAALC", "CLAAAKAAALC", "CLAAAKAAALC", "CFAAAAAAAFC", "CBIIIIIIIBC")
                            .aisle("CBIIIIIIIBC", "CJAAKMKAAJC", "CJAAKMKAAJC", "CJAAKMKAAJC", "CFAAKMKAAFC",
                                    "CLAAKMKAALC", "CLAAKMKAALC", "CLAAKMKAALC", "CFAAAAAAAFC", "CBIIIIIIIBC")
                            .aisle("CBIIIIIIIBC", "CJAAAKAAAJC", "CJAAAKAAAJC", "CJAAAKAAAJC", "CFAAAKAAAFC",
                                    "CLAAAKAAALC", "CLAAAKAAALC", "CLAAAKAAALC", "CFAAAAAAAFC", "CBIIIIIIIBC")
                            .aisle("BBHHIIIHHBB", "CDDAAAAADDC", "CIDAAAAADIC", "CIAAAAAAAIC", "CIAAAAAAAIC",
                                    "CEAAAAAAAEC", "CEAAAAAAAEC", "CEAAAAAAAEC", "CEAAAAAAAEC", "CBBIIIIIBBC")
                            .aisle("BEEBBNBBEEB", "DEEEFFFEEED", "DEEOJJJOEED", "DDDOJJJODDD", "CBBOJJJOBBC",
                                    "CBBOJJJOBBC", "CCBBJJJBBCC", "CCBBJJJBBCC", "CCBBBBBBBCC", "CCBBBBBBBCC")
                            .aisle("BBBBCCCBBBB", "CDDCCCCCDDC", "CCDCCCCCDCC", "CCCCCCCCCCC", "CCCCCCCCCCC",
                                    "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC", "CCCCCCCCCCC")
                            .where("A", air())
                            .where('B', blocks(PhoenixBlocks.SPACE_TIME_COOLED_ETERNITY_CASING.get()))
                            .where('C', any())
                            .where('D', blocks(PhoenixBlocks.AKASHIC_ZERONIUM_CASING.get()))
                            .where('E', blocks(ADVANCED_COMPUTER_CASING.get()).setMinGlobalLimited(20)
                                    .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                            .setMaxGlobalLimited(2, 1))
                                    .or(abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1))
                                    .or(abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setExactLimit(1))
                                    .or(autoAbilities(true, false, false)))
                            .where('F', blocks(COMPUTER_HEAT_VENT.get()))
                            .where('G', blocks(PhoenixResearchMachines.ADVANCED_PHOENIX_COMPUTATION_COMPONENT.get())
                                    .or(blocks(PhoenixResearchMachines.ADVANCED_PHOENIX_COMPUTATION_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_ADVANCED_COMPUTATION_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_EMPTY_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_COMPUTATION_COMPONENT.get())))
                            .where('H', blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                            .where('I', blocks(COMPUTER_CASING.get()))
                            .where('J', blocks(FUSION_GLASS.get()))
                            .where('K', blocks(PhoenixBlocks.AKASHIC_COIL_BLOCK.get()))
                            .where('L', blocks(PhoenixResearchMachines.ACTIVE_PHOENIX_COOLER_COMPONENT.get())
                                    .or(blocks(PhoenixResearchMachines.PHOENIX_COOLER_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_EMPTY_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_BRIDGE_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_ACTIVE_COOLER_COMPONENT.get()))
                                    .or(blocks(GTResearchMachines.HPCA_HEAT_SINK_COMPONENT.get())))
                            .where('M', blocks(PhoenixBlocks.PERFECTED_LOGIC.get()))
                            .where('N', controller(blocks(definition.getBlock())))
                            .where('O', blocks(GCYMBlocks.HEAT_VENT.get()))
                            .build())
                    .sidedWorkableCasingModel(GTCEu.id("block/casings/hpca/advanced_computer_casing"),
                            GTCEu.id("block/multiblock/hpca"))
                    .register();
        }
    }

    public static final MultiblockMachineDefinition ALCHEMICAL_IMBUER = REGISTRATE
            .multiblock("alchemical_imbuer", AlchemicalImbuerMachine::new)
            .langValue("§5Alchemical Imbuer")
            .recipeTypes(PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES, PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES)
            .recipeModifiers(AlchemicalImbuerMachine::recipeModifier, OC_NON_PERFECT_SUBTICK)
            .appearanceBlock(SOURCE_FIBER_MACHINE_CASING)
            .rotationState(RotationState.NON_Y_AXIS)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")
                    .aisle("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
                    .aisle("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
                    .aisle("CDDDDDC", "BCEEECB", "BBFEFBB", "BBFHFBB", "BBFEFBB", "BCDIDCB", "CDGJGDC")
                    .aisle("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
                    .aisle("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
                    .aisle("BBCKCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")
                    .where("B", Predicates.any())
                    .where("C",
                            Predicates.blocks(SOURCE_FIBER_MACHINE_CASING.get())
                                    .or(Predicates.abilities(SOURCE_INPUT).setPreviewCount(1))
                                    .or(Predicates.abilities(SOURCE_OUTPUT).setPreviewCount(1))
                                    .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                    .where("D",
                            Predicates.blocks(
                                    ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:sourcestone"))))
                    .where("E",
                            Predicates.blocks(ForgeRegistries.BLOCKS
                                    .getValue(ResourceLocation.parse("ars_nouveau:magebloom_block"))))
                    .where("F", Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where("G",
                            Predicates.blocks(
                                    ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:void_prism"))))
                    .where("H",
                            Predicates.blocks(ForgeRegistries.BLOCKS
                                    .getValue(ResourceLocation.parse("ars_nouveau:source_gem_block"))))
                    .where("I",
                            Predicates.blocks(
                                    ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:arcane_core"))))
                    .where("J",
                            Predicates.blocks(ForgeRegistries.BLOCKS
                                    .getValue(ResourceLocation.parse("ars_nouveau:vitalic_sourcelink"))))
                    .where("K", Predicates.controller(Predicates.blocks(definition.get())))
                    .build())
            .workableCasingModel(GTCEu.id("block/casings/mechanic/machine_casing_turbine_tungstensteel"),
                    PhoenixCore.id("block/multiblock/alchemical_imbuer"))
            .tooltipBuilder(ALCHEMICAL_IMBUER_TOOLTIPS)
            .register();

    public static final MultiblockMachineDefinition SOURCE_REACTOR = REGISTRATE
            .multiblock("source_reactor", SourceReactorMachine::new)
            .rotationState(RotationState.ALL)
            .langValue("§5Source Reactor")
            .recipeType(PhoenixRecipeTypes.SOURCE_REACTOR_RECIPES)
            .recipeModifiers(SourceReactorMachine::recipeModifier, OC_NON_PERFECT_SUBTICK, BATCH_MODE)
            .appearanceBlock(SUPER_STABLE_FUSION_CASING)
            .pattern(definition -> {
                var casing = blocks(SOURCE_FIBER_MACHINE_CASING.get()).setMinGlobalLimited(10);
                var abilities = Predicates.autoAbilities(definition.getRecipeTypes())
                        .or(Predicates.autoAbilities(true, false, false));
                return FactoryBlockPattern.start()
                        .aisle("XXX", "XXX", "XXX")
                        .aisle("XXX", "XPX", "XXX")
                        .aisle("XXX", "XSX", "XXX")
                        .where('S', Predicates.controller(blocks(definition.getBlock())))
                        .where('X', casing.or(abilities).or(abilities(SOURCE_INPUT).setPreviewCount(1)))
                        .where('P', blocks(CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()))
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
                return FactoryBlockPattern.start()
                        .aisle("BBCBB", "BBHBB", "CHBHC", "BBHBB", "BBCBB")
                        .aisle("BBCBB", "BHCHB", "CCICC", "BHCHB", "BBCBB")
                        .aisle("BBBBB", "BCCCB", "CCFCC", "BCCCB", "BBCBB")
                        .aisle("BBBBB", "BCCCB", "CCBCC", "BCCCB", "BBCBB")
                        .aisle("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .aisle("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .aisle("BBBBB", "BCCCB", "BGBGB", "BCCCB", "BBBBB")
                        .aisle("BEBEB", "BCCCB", "BCFCB", "BCCCB", "BBBBB")
                        .aisle("BBBBB", "BBCBB", "BCDCB", "BBCBB", "BBBBB")
                        .where('B', Predicates.any())
                        .where('C',
                                Predicates.blocks(SOURCE_FIBER_MACHINE_CASING.get())
                                        .or(Predicates.abilities(SOURCE_INPUT).setPreviewCount(1))
                                        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('E', blocks(ChemicalHelper.getBlock(frameGt, SOURCE_IMBUED_TITANIUM)))
                        .where('H', blocks(VOID_PRISM.get()))
                        .where('G', blocks(GTBlocks.CLEANROOM_GLASS.get()))
                        .where('D', Predicates.controller(blocks(definition.getBlock())))
                        .where('F', blocks(CASING_TITANIUM_GEARBOX.get()))
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
            .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
            .pattern(definition -> {
                return FactoryBlockPattern.start()
                        .aisle("BCCCB", "BCDCB", "BCDCB", "BEEEB", "BFFFB", "BCCCB", "BBBBB", "BBBBB", "BBBBB")
                        .aisle("CCCCC", "CDGDC", "CADAC", "EADAE", "FAFAF", "CCCCC", "BCFCB", "BBFBB", "BBFBB")
                        .aisle("CCCCC", "DGHGD", "DDHDD", "EDHDE", "FFAFF", "CCCCC", "BFCFB", "BFAFB", "BFIFB")
                        .aisle("CCCCC", "CDGDC", "CADAC", "EADAE", "FAFAF", "CCCCC", "BCFCB", "BBFBB", "BBFBB")
                        .aisle("BCCCB", "BCJCB", "BCDCB", "BEEEB", "BFFFB", "BCCCB", "BBBBB", "BBBBB", "BBBBB")
                        .where('A', air())
                        .where('B', any())
                        .where('C', blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()).setMinGlobalLimited(10)
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where('D',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        Neutronium)))

                        .where("E", Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where("F", Predicates.heatingCoils())
                        .where("G", Predicates.blocks(FUSION_COIL.get()))
                        .where("H", Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                        .where("I", Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where("J", Predicates.controller(Predicates.blocks(definition.get())))
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
                return FactoryBlockPattern.start()
                        .aisle("BBCCCCCBB", "DBDDDDDBD", "DBDDDDDBD", "DBDDDDDBD", "DBDDDDDBD", "DDDDDDDDD",
                                "DDDDDDDDD", "DDDDDDDDD")
                        .aisle("BEEEEEEEB", "BEFFFFFEB", "BEEEEEEEB", "BEEEEEEEB", "BEEEEEEEB", "DBCBBBCBD",
                                "DDCBBBCDD", "DDCBBBCDD")
                        .aisle("CEEEEEEEC", "DFAGAAAFD", "DHAAAAAHD", "DHAGAGAHD", "DHHEEEHHD", "DIHAAAHID",
                                "DDHAAAHDD", "DDBJJJBDD")
                        .aisle("CEEEEEEEC", "DFGKGKGFD", "DHAGAGAHD", "DHGKGKGHD", "DHHEAEHHD", "DIHAAAHID",
                                "DDHAAAHDD", "DDBJLJBDD")
                        .aisle("CEEEEEEEC", "DFAGAGAFD", "DHAAAAAHD", "DHAGAGAHD", "DHHEEEHHD", "DIHAAAHID",
                                "DDHAAAHDD", "DDBJJJBDD")
                        .aisle("BEEEEEEEB", "BEMMMMMEB", "BEMMNMMEB", "BEMMMMMEB", "BEEEEEEEB", "DBCBBBCBD",
                                "DDCBBBCDD", "DDCBBBCDD")
                        .aisle("BBCDDDCBB", "DBCDDDCBD", "DBCDDDCBD", "DBCDDDCBD", "DBCCCCCBD", "DDDDDDDDD",
                                "DDDDDDDDD", "DDDDDDDDD")
                        .where("A", air())
                        .where("B",
                                Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_TURBINE.get()).setMinGlobalLimited(10)
                                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                        .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where('C',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        VOID_TOUCHED_TUNGSTEN_STEEL)))
                        .where("D", Predicates.any())
                        .where("E", Predicates.blocks(CASING_TUNGSTENSTEEL_ROBUST.get()))
                        .where("F", Predicates.blocks(FIREBOX_TUNGSTENSTEEL.get()))
                        .where("G", Predicates.blocks(CASING_TUNGSTENSTEEL_PIPE.get()))
                        .where("H", Predicates.heatingCoils())
                        .where("I", Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        .where("J", Predicates.blocks(CASING_HSSE_STURDY.get()))
                        .where("K", Predicates.blocks(CASING_EXTREME_ENGINE_INTAKE.get()))
                        .where("L", Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where("M", Predicates.blocks(CASING_STAINLESS_CLEAN.get()))
                        .where("N", Predicates.controller(Predicates.blocks(definition.get())))
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
                return FactoryBlockPattern.start()
                        .aisle("BCBBBBBCB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB",
                                "BDBBBBBDB", "BCBBBBBCB")
                        .aisle("CCEEEEECC", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD",
                                "DCFFFFFCD", "CCCCCCCCC")
                        .aisle("BEGGGGGEB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB",
                                "BFHIJIHFB", "BCHCCCHCB")
                        .aisle("BEGGGGGEB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB",
                                "BFIAAAIFB", "BCCGGGCCB")
                        .aisle("BEGGGGGEB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB", "BFJAAAJFB",
                                "BFJAAAJFB", "BCCGKGCCB")
                        .aisle("BEGGGGGEB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB", "BFIAAAIFB",
                                "BFIAAAIFB", "BCCGGGCCB")
                        .aisle("BEGGGGGEB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB", "BFHIJIHFB",
                                "BFHIJIHFB", "BCHCCCHCB")
                        .aisle("CCEEEEECC", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD", "DCFFFFFCD",
                                "DCFFFFFCD", "CCCCLCCCC")
                        .aisle("BCBBBBBCB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB", "BDBBBBBDB",
                                "BDBBBBBDB", "BCBBBBBCB")
                        .where("A", air())
                        .where("B", any())
                        .where("C", Predicates.blocks(CASING_STEEL_SOLID.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where('D',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        VOID_TOUCHED_TUNGSTEN_STEEL)))
                        .where("E", Predicates.blocks(FIREBOX_STEEL.get()))
                        .where("F", Predicates.blocks(CASING_LAMINATED_GLASS.get()))
                        .where("G", Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        .where('H',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        RESONANT_RHODIUM_ALLOY)))
                        .where("I", Predicates.blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where("J", Predicates.heatingCoils())
                        .where("K", Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where("L", Predicates.controller(Predicates.blocks(definition.get())))
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
                return FactoryBlockPattern.start()
                        .aisle("BBBCCCBBB", "BBBCCCBBB", "BBBDDDBBB", "BBBDDDBBB", "BBBEEEBBB", "BBBBBBBBB")
                        .aisle("BCCCCCCCB", "BCFGGGFCB", "BDDGGGDDB", "BDDGGGDDB", "BEEGGGEEB", "BBEEEEEBB")
                        .aisle("CCCCCCCCC", "CHGIIIGHC", "DHGJJJGHD", "DIGAAAGID", "BEGAAAGEB", "BBEAGAEBB")
                        .aisle("CCCCCCCCC", "CHGIJIGHC", "DHGJJJGHD", "DIGABAGID", "BEGAKAGEB", "BBEGGGEBB")
                        .aisle("CCCCCCCCC", "CHGIIIGHC", "DHGJJJGHD", "DIGAAAGID", "BEGA2AGEB", "BBEAGAEBB")
                        .aisle("BCCCCCCCB", "BCFGGGFCB", "BDDGGGDDB", "BDDGGGDDB", "BEEGGGEEB", "BBEEEEEBB")
                        .aisle("BBBCCCBBB", "BBBCLCBBB", "BBBDDDBBB", "BBBDDDBBB", "BBBEEEBBB", "BBBBBBBBB")
                        .where("A", air())
                        .where("2", Predicates.blocks(Blocks.DIRT))
                        .where("B", Predicates.any())
                        .where("C", Predicates.blocks(CASING_HSSE_STURDY.get()).setMinGlobalLimited(10)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where('D',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        TungstenSteel)))
                        .where("E", Predicates.blocks(Blocks.WARPED_HYPHAE))
                        .where("F", Predicates.blocks(TUNGSTENSTEEL_CRATE.getBlock()))
                        .where('G',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        TreatedWood)))
                        .where("H", Predicates.blocks(CASING_TUNGSTENSTEEL_PIPE.get()))
                        .where("I", Predicates.blocks(Blocks.HONEYCOMB_BLOCK))
                        .where("J", Predicates.blocks(Blocks.HONEY_BLOCK))
                        .where("K", blocks(Blocks.POPPY))
                        .where("L", Predicates.controller(Predicates.blocks(definition.get())))
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
                return FactoryBlockPattern.start()
                        .aisle("BCDCB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB", "BBBBB")
                        .aisle("CCCCC", "BEFEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB", "BBBBB")
                        .aisle("DCCCD", "EFFFE", "EGHGE", "EEGEE", "BEGEB", "BEGEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB",
                                "BBEBB", "BBEBB")
                        .aisle("CCCCC", "BEFEB", "BEGEB", "BBEBB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB", "BBBBB")
                        .aisle("BCICB", "BBEBB", "BBEBB", "BBEBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB", "BBBBB")
                        .where("B", Predicates.any())
                        .where("C", Predicates.blocks(CASING_TITANIUM_STABLE.get()).setMinGlobalLimited(5)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where("D", Predicates.blocks(FIREBOX_TITANIUM.get()))
                        .where("E", Predicates.blocks(ChemicalHelper.getBlock(frameGt, SOURCE_IMBUED_TITANIUM)))
                        .where("F", Predicates.blocks(COIL_NICHROME.get()))
                        .where("G", Predicates.blocks(FIREBOX_TITANIUM.get()))
                        .where("H", Predicates.blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                        .where("I", Predicates.controller(Predicates.blocks(definition.get())))
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
                return FactoryBlockPattern.start()
                        .aisle("BCCCB", "BDDDB", "BCCCB", "BBCBB", "BBCBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB")
                        .aisle("CEEEC", "DAAAD", "CAAAC", "BEAEB", "BEAEB", "BEEEB", "BBEBB", "BBEBB", "BBBBB", "BBBBB",
                                "BBBBB")
                        .aisle("CEEEC", "DAAAD", "CAAAC", "CAAAC", "CAAAC", "BEAEB", "BEAEB", "BEAEB", "BBCBB", "BBDBB",
                                "BBCBB")
                        .aisle("CEEEC", "DAAAD", "CAAAC", "BEAEB", "BEAEB", "BEEEB", "BBEBB", "BBEBB", "BBBBB", "BBBBB",
                                "BBBBB")
                        .aisle("BCCCB", "BCFCB", "BCCCB", "BBCBB", "BBCBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB",
                                "BBBBB")
                        .where("A", air())
                        .where("B", Predicates.any())
                        .where("C", Predicates.blocks(CASING_STAINLESS_CLEAN.get()).setMinGlobalLimited(5)
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                        .where("D", Predicates.blocks(COIL_KANTHAL.get()))
                        .where('E',
                                blocks(ChemicalHelper.getBlock(frameGt,
                                        FROST_REINFORCED_STAINED_STEEL)))
                        .where("F", Predicates.controller(Predicates.blocks(definition.get())))
                        .build();
            })
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                    GTCEu.id("block/multiblock/large_miner"))
            .tooltipBuilder(AETHERIAL_FABRICATOR_TOOLTIPS)
            .register();

    public static void init() {}
}
