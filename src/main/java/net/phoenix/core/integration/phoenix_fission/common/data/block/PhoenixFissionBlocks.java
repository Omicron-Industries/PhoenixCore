package net.phoenix.core.integration.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.datagen.models.PhoenixFissionMachineModels;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionModeratorType;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import org.jetbrains.annotations.NotNull;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("unused")
public class PhoenixFissionBlocks {

    public static void init() {}

    public static final BlockEntry<NukeBlock> NUKE_BLOCK = REGISTRATE
            .block("nuke", NukeBlock::new)
            .initialProperties(() -> Blocks.TNT)
            .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeBottomTop(
                            ctx.getName(),
                            PhoenixCore.id("block/nuke_side"),
                            PhoenixCore.id("block/nuke_bottom"),
                            PhoenixCore.id("block/nuke_top"))))
            .item(BlockItem::new)
            .build()
            .register();

    // --- Coolers ---
    public static final BlockEntry<FissionCoolerBlock> COOLER_BASIC = createCoolerBlock(
            FissionCoolerBlock.FissionCoolerTypes.COOLER_BASIC);
    public static final BlockEntry<FissionCoolerBlock> COOLER_EV = createCoolerBlock(
            FissionCoolerBlock.FissionCoolerTypes.COOLER_EV);
    public static final BlockEntry<FissionCoolerBlock> COOLER_IV = createCoolerBlock(
            FissionCoolerBlock.FissionCoolerTypes.COOLER_IV);
    public static final BlockEntry<FissionCoolerBlock> COOLER_LUV = createCoolerBlock(
            FissionCoolerBlock.FissionCoolerTypes.COOLER_LUV);

    // --- Moderators ---
    public static final BlockEntry<FissionModeratorBlock> MODERATOR_GRAPHITE = createModeratorBlock(
            FissionModeratorBlock.FissionModeratorTypes.GRAPHITE);
    public static final BlockEntry<FissionModeratorBlock> MODERATOR_BERYLLIUM = createModeratorBlock(
            FissionModeratorBlock.FissionModeratorTypes.BERYLLIUM);
    public static final BlockEntry<FissionModeratorBlock> MODERATOR_HEAVY_WATER = createModeratorBlock(
            FissionModeratorBlock.FissionModeratorTypes.HEAVY_WATER);
    public static final BlockEntry<FissionModeratorBlock> MODERATOR_NIOBIUM_SIC = createModeratorBlock(
            FissionModeratorBlock.FissionModeratorTypes.NIOBIUM_SIC);

    // --- Fuel Rods ---
    public static final BlockEntry<FissionFuelRodBlock> FUEL_ROD_T1 = createFuelRodBlock(
            FissionFuelRodBlock.FissionFuelRodTypes.T1_FUEL_ROD);
    public static final BlockEntry<FissionFuelRodBlock> FUEL_ROD_T2 = createFuelRodBlock(
            FissionFuelRodBlock.FissionFuelRodTypes.T2_FUEL_ROD);
    public static final BlockEntry<FissionFuelRodBlock> FUEL_ROD_T3 = createFuelRodBlock(
            FissionFuelRodBlock.FissionFuelRodTypes.T3_FUEL_ROD);
    public static final BlockEntry<FissionFuelRodBlock> FUEL_ROD_T4 = createFuelRodBlock(
            FissionFuelRodBlock.FissionFuelRodTypes.T4_FUEL_ROD);
    public static final BlockEntry<FissionFuelRodBlock> FUEL_ROD_T5 = createFuelRodBlock(
            FissionFuelRodBlock.FissionFuelRodTypes.T5_FUEL_ROD);

    // --- Breeder Blankets ---
    public static final BlockEntry<FissionBlanketBlock> THORIUM_BLANKET = createBlanketBlock(
            FissionBlanketBlock.BreederBlanketTypes.THORIUM_BLANKET);
    public static final BlockEntry<FissionBlanketBlock> URANIUM_BLANKET = createBlanketBlock(
            FissionBlanketBlock.BreederBlanketTypes.URANIUM_BLANKET);
    public static final BlockEntry<FissionBlanketBlock> NEPTUNIUM_BLANKET = createBlanketBlock(
            FissionBlanketBlock.BreederBlanketTypes.NEPTUNIUM_BLANKET);
    public static final BlockEntry<FissionBlanketBlock> PLUTONIUM_BLANKET = createBlanketBlock(
            FissionBlanketBlock.BreederBlanketTypes.PLUTONIUM_BLANKET);
    public static final BlockEntry<FissionBlanketBlock> AMERICIUM_BLANKET = createBlanketBlock(
            FissionBlanketBlock.BreederBlanketTypes.AMERICIUM_BLANKET);

    // --- Casings ---
    public static BlockEntry<Block> FISSILE_HEAT_SAFE_CASING = registerSimpleBlock("§bFissile Heat Safe Casing",
            "fissile_heat_safe_casing", "fissile_heat_safe_casing", BlockItem::new);
    public static BlockEntry<Block> FISSILE_REACTION_SAFE_CASING = registerSimpleBlock("§bFissile Reaction Safe Casing",
            "fissile_reaction_safe_casing", "fissile_reaction_safe_casing", BlockItem::new);
    public static BlockEntry<Block> FISSILE_SAFE_GEARBOX_CASING = registerSimpleBlock("§bFissile Safe Gearbox",
            "fissile_safe_gearbox_casing", "fissile_safe_gearbox", BlockItem::new);

    public static final BlockEntry<Block> EMPTY_REACTOR_COMPONENT = REGISTRATE
            .block("empty_reactor_component", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/fission/cooler_base"))))
            .lang("Empty Reactor Component")
            .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.phoenix.empty_component"))
            .build()
            .register();

    private static BlockEntry<FissionModeratorBlock> createModeratorBlock(IFissionModeratorType type) {
        var moderator = REGISTRATE
                .block("%s".formatted(type.getName()),
                        p -> new FissionModeratorBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createFissionModeratorModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_MODERATORS.put(type, moderator);
        return moderator;
    }

    private static BlockEntry<FissionFuelRodBlock> createFuelRodBlock(IFissionFuelRodType type) {
        var rod = REGISTRATE
                .block("%s".formatted(type.getName()),
                        p -> new FissionFuelRodBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createFuelRodModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();
        PhoenixAPI.FISSION_FUEL_RODS.put(type, rod);
        return rod;
    }

    private static BlockEntry<FissionBlanketBlock> createBlanketBlock(IFissionBlanketType type) {
        var blanket = REGISTRATE
                .block("%s".formatted(type.getName()),
                        p -> new FissionBlanketBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createBlanketRodModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_BLANKETS.put(type, blanket);
        return blanket;
    }

    private static BlockEntry<FissionCoolerBlock> createCoolerBlock(IFissionCoolerType type) {
        var cooler = REGISTRATE
                .block("%s".formatted(type.getName()),
                        p -> new FissionCoolerBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createActiveCoolerModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_COOLERS.put(type, cooler);
        return cooler;
    }

    private static @NotNull BlockEntry<Block> registerSimpleBlock(String name, String id, String texture,
                                                                  NonNullBiFunction<Block, Item.Properties, ? extends BlockItem> func) {
        return REGISTRATE
                .block(id, Block::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/" + texture))))
                .lang(name)
                .item(func)
                .build()
                .register();
    }
}
