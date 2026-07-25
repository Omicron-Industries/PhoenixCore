package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.api.machine.trait.ITeslaBattery;
import net.phoenix.core.integration.phoenix_tesla_network.common.block.TeslaBatteryBlock;
import net.phoenix.core.integration.vocal_resonance.ISpeakerType;
import net.phoenix.core.integration.vocal_resonance.SpeakerBlock;
import net.phoenix.core.integration.vocal_resonance.SpeakerTier;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("unused")
public class PhoenixBlocks {

    public static void init() {}

    private static @NotNull BlockEntry<Block> registerSimpleBlock(String name, String id, String texture,
                                                                  NonNullBiFunction<Block, Item.Properties, ? extends BlockItem> func) {
        return REGISTRATE
                .block(id, Block::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false)
                        .strength(5.0f, 6.0f)
                        .requiresCorrectToolForDrops())
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/" + texture))))
                .lang(name)
                .item(func)
                .build()
                .register();
    }

    public static final BlockEntry<FlowerBlock> CRYSTAL_ROSE = REGISTRATE
            .block("crystal_rose", p -> new FlowerBlock(() -> MobEffects.GLOWING, 5, p))
            .initialProperties(() -> Blocks.POPPY)
            .addLayer(() -> RenderType::cutout)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                    prov.models().cross(ctx.getName(), prov.modLoc("item/crystal_rose"))))
            .item()
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/crystal_rose")))
            .build()
            .register();

    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_UHV = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.UHV);
    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_UEV = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.UEV);
    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_UIV = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.UIV);
    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_UXV = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.UXV);
    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_OPV = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.OPV);
    public static final BlockEntry<TeslaBatteryBlock> TESLA_BATTERY_MAX = createTeslaBattery(
            TeslaBatteryBlock.TeslaBatteryType.MAX);

    private static BlockEntry<TeslaBatteryBlock> createTeslaBattery(ITeslaBattery batteryData) {
        String tierName = batteryData.getBatteryName();

        var battery = REGISTRATE
                .block("tesla_battery_%s".formatted(tierName), p -> new TeslaBatteryBlock(p, batteryData))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .lang("Tesla Battery " + (tierName.equalsIgnoreCase("opv") ? "OpV" : tierName.toUpperCase()))
                .blockstate((ctx, prov) -> {
                    String folderPath = "block/casings/batteries/tesla_" + tierName + "/";
                    var side = PhoenixCore.id(folderPath + "side");
                    var top = PhoenixCore.id(folderPath + "top");
                    var bottom = PhoenixCore.id(folderPath + "bottom");

                    prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeBottomTop(ctx.getName(), side, bottom, top));
                })
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.TESLA_BATTERIES.put(batteryData, battery);
        return battery;
    }

    public static final BlockEntry<CoilBlock> COIL_TRUE_HEAT_STABLE = createCoilBlock(
            PhoenixCoilBlock.CoilType.COIL_TRUE_HEAT_STABLE);

    public static BlockEntry<Block> PHOENIX_ENRICHED_TRITANIUM_CASING = registerSimpleBlock(
            "§6Extremely Heat-Stable Casing", "phoenix_enriched_tritanium_casing",
            "phoenix_enriched_tritanium_casing", BlockItem::new);
    public static BlockEntry<Block> PHOENIX_GAZE_PANEL = registerSimpleBlock(
            "§cPhoenix Gaze Panel", "phoenix_gaze_panel",
            "phoenix_gaze_panel", BlockItem::new);
    public static BlockEntry<Block> TRUE_PHOENIX_INFUSED_CASING = registerSimpleBlock(
            "§cTrue Phoenix Infused Casing", "true_phoenix_infused_casing",
            "true_phoenix_infused_casing", BlockItem::new);
    public static BlockEntry<Block> AKASHIC_ZERONIUM_CASING = registerSimpleBlock(
            "§5Akashic Zeronium Casing", "akashic_zeronium_casing",
            "akashic_zeronium_casing", BlockItem::new);
    public static BlockEntry<Block> PERFECTED_LOGIC = registerSimpleBlock(
            "§5Perfected Logic Casing", "perfected_logic",
            "perfected_logic", BlockItem::new);
    public static BlockEntry<Block> PHOENIX_ENRICHED_NEUTRONIUM_CASING = registerSimpleBlock(
            "§5Phoenix Enriched Neutronium Casing", "phoenix_enriched_neutronium_casing",
            "phoenix_enriched_neutronium_casing", BlockItem::new);
    public static BlockEntry<Block> AKASHIC_COIL_BLOCK = registerSimpleBlock(
            "§5Computation Coil", "akashic_coil_block",
            "akashic_coil_block", BlockItem::new);
    public static BlockEntry<Block> SPACE_TIME_COOLED_ETERNITY_CASING = registerSimpleBlock(
            "§5Space Time Cooled Eternity Casing", "space_time_cooled_eternity_casing",
            "space_time_cooled_eternity_casing", BlockItem::new);
    public static BlockEntry<Block> TWISTED_COMPUTER_CASING = registerSimpleBlock(
            "§5Twisted Computer Casing", "twisted_computer_casing",
            "twisted_computer_casing", BlockItem::new);
    public static BlockEntry<Block> STABLE_LOGIC_CASING = registerSimpleBlock(
            "§cStable Logic Casing", "stable_logic_casing",
            "stable_logic", BlockItem::new);
    public static BlockEntry<Block> RELIABLE_NAQUADAH_ALLOY_MACHINE_CASING = registerSimpleBlock(
            "§cReliable Naquadah Alloy Machine Casing", "reliable_naquadah_alloy_machine_casing",
            "reliable_naquadah_alloy_machine_casing", BlockItem::new);
    public static BlockEntry<Block> SUPER_STABLE_FUSION_CASING = registerSimpleBlock(
            "§cPhoenix Fusion Casing", "super_stable_fusion_casing",
            "super_stable_fusion_casing", BlockItem::new);
    public static BlockEntry<Block> BLAZING_CORE_STABILIZER = registerSimpleBlock(
            "§cBlazing Core Stabilizer", "blazing_core_stabilizer",
            "blazing_core_stabilizer", BlockItem::new);
    public static BlockEntry<Block> GLITCHED_ENTROPY_CASING = registerSimpleBlock(
            "§aGlitched Entropy Casing", "glitched_entropy_casing",
            "glitched_entropy_casing", BlockItem::new);
    public static BlockEntry<Block> PHOENIX_HEART_CASING = registerSimpleBlock(
            "§cPhoenix Heart Casing", "phoenix_heart_casing",
            "phoenix_heart_casing", BlockItem::new);
    public static BlockEntry<Block> FISSILE_HEAT_SAFE_CASING = registerSimpleBlock(
            "§bFissile Heat Safe Casing", "fissile_heat_safe_casing",
            "fissile_heat_safe_casing", BlockItem::new);
    public static BlockEntry<Block> FISSILE_REACTION_SAFE_CASING = registerSimpleBlock(
            "§bFissile Reaction Safe Casing", "fissile_reaction_safe_casing",
            "fissile_reaction_safe_casing", BlockItem::new);
    public static BlockEntry<Block> FISSILE_SAFE_GEARBOX_CASING = registerSimpleBlock(
            "§bFissile Safe Gearbox", "fissile_safe_gearbox_casing",
            "fissile_safe_gearbox", BlockItem::new);
    public static BlockEntry<Block> INSANELY_SUPERCHARGED_TESLA_CASING = registerSimpleBlock(
            "§4Insanely Supercharged Tesla Tower Casing", "insanely_supercharged_tesla_casing",
            "casings/multiblock/tesla_casing", BlockItem::new);
    public static BlockEntry<Block> SOURCE_FIBER_MACHINE_CASING = registerSimpleBlock(
            "§dSource Fiber Machine Casing", "source_fiber_machine_casing",
            "casings/multiblock/machine_casing_source_fiber_mesh", BlockItem::new);
    public static BlockEntry<Block> MACHINE_CASING_RHODIUM_PLATED_PALLADIUM = registerSimpleBlock(
            "Pristine Rhodium Plated Palladium Machine Casing", "machine_casing_pristine_rhodium_plated_palladium",
            "casings/multiblock/machine_casing_pristine_rhodium_plated_palladium", BlockItem::new);
    public static BlockEntry<Block> MACHINE_CASING_NAQUADAH_ALLOY = registerSimpleBlock(
            "Invariant Naquadah Alloy Machine Casing", "machine_casing_invariant_naquadah_alloy",
            "casings/multiblock/machine_casing_invariant_naquadah_alloy", BlockItem::new);

    static {
        if (PhoenixConfigs.INSTANCE.features.blazingCleanroomEnabled) {
            final BlockEntry<Block> BLAZING_CLEANROOM_FILTER_CASING = createCleanroomFilters(
                    BlazingFilterType.FILTER_CASING_BLAZING);
        }
    }

    private static BlockEntry<Block> createCleanroomFilters(IFilterType filterType) {
        var filterBlock = REGISTRATE.block(filterType.getSerializedName(), Block::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(properties -> properties.strength(2.0f, 8.0f).sound(SoundType.METAL)
                        .isValidSpawn((blockState, blockGetter, blockPos, entityType) -> false))
                .blockstate(GTModels.createCleanroomFilterModel(filterType))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH, CustomTags.TOOL_TIERS[1])
                .item(BlockItem::new)
                .build()
                .register();
        GTCEuAPI.CLEANROOM_FILTERS.put(filterType, filterBlock);
        return filterBlock;
    }

    private static BlockEntry<CoilBlock> createCoilBlock(PhoenixCoilBlock.@UnknownNullability CoilType coilType) {
        var coilBlock = REGISTRATE
                .block("%s_coil_block".formatted(coilType.getName()), p -> new CoilBlock(p, coilType))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(GTModels.createCoilModel(coilType))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();
        GTCEuAPI.HEATING_COILS.put(coilType, coilBlock);
        return coilBlock;
    }

    public static final BlockEntry<SpeakerBlock> SPEAKER_LV = createSpeaker(SpeakerTier.LV);
    public static final BlockEntry<SpeakerBlock> SPEAKER_MV = createSpeaker(SpeakerTier.MV);
    public static final BlockEntry<SpeakerBlock> SPEAKER_HV = createSpeaker(SpeakerTier.HV);

    private static BlockEntry<SpeakerBlock> createSpeaker(ISpeakerType speakerData) {
        String tierName = speakerData.getName().toLowerCase();

        return REGISTRATE
                .block("speaker_%s".formatted(tierName), p -> new SpeakerBlock(p, speakerData))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .lang(speakerData.getName() + " Speaker Component")
                .blockstate((ctx, prov) -> {

                    String folder = "block/casings/speakers/";
                    var side = PhoenixCore.id(folder + "side");
                    var front = PhoenixCore.id(folder + tierName + "_front");
                    prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeColumn(ctx.getName(), front, side));
                })
                .item(BlockItem::new)
                .build()
                .register();
    }
}
