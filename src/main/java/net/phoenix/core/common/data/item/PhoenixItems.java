package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;
import com.gregtechceu.gtceu.common.item.armor.GTArmorMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.Tags;
import net.phoenix.core.common.item.ChameleonSprayCanItem;
import net.phoenix.core.integration.ars_nouveau.common.data.item.SoulLensItem;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixItems {

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    public static ItemEntry<ComponentItem> POWER_UNIT_LUV = REGISTRATE.item("luv_power_unit", ComponentItem::create)
            .lang("LuV Power Unit")
            .properties(p -> p.stacksTo(8))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tools/power_unit_luv")))
            .onRegister((c) -> c.attachComponents(ElectricStats.createElectricItem(102400000L, GTValues.LuV)))
            .register();
    public static ItemEntry<ComponentItem> POWER_UNIT_ZPM = REGISTRATE.item("zpm_power_unit", ComponentItem::create)
            .lang("ZPM Power Unit")
            .properties(p -> p.stacksTo(8))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tools/power_unit_zpm")))
            .onRegister((c) -> c.attachComponents(ElectricStats.createElectricItem(409600000L, GTValues.ZPM)))
            .register();
    public static ItemEntry<TeslaBinderItem> TESLA_BINDER = REGISTRATE
            .item("tesla_binder", TeslaBinderItem::new)
            .lang("Tesla Binder")
            .properties(p -> p.stacksTo(1))
            .onRegister(c -> c.attachComponents(c))
            .model((ctx, prov) -> prov.handheld(ctx, prov.modLoc("item/tools/tesla_binder")))
            .register();

    public static final ItemEntry<ChameleonSprayCanItem> CHAMELEON_SPRAY_CAN = REGISTRATE
            .item("chameleon_spray_can", ChameleonSprayCanItem::new)
            .lang("Chameleon Spray Can")
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.handheld(ctx, prov.modLoc("item/tools/chameleon_spray_can")))
            .register();
    public static ItemEntry<SoulLensItem> SOUL_LENS = REGISTRATE
            .item("soul_lens", SoulLensItem::new)
            .lang("Soul Lens")
            .properties(p -> p.stacksTo(1))
            .onRegister(c -> c.attachComponents(c))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/soul_lens")))
            .register();
    public static ItemEntry<Item> HONEY_COMB_BASE_MOLD = REGISTRATE
            .item("honey_comb_base_mold", Item::new)
            .lang("§6Honeycomb Base Mold")
            .register();
    public static ItemEntry<Item> SOURCE_FIBERS = REGISTRATE.item("source_fibers", Item::new)
            .lang("§dRaw Source Fibers")
            .register();
    public static ItemEntry<Item> SOURCE_FIBER_MESH = REGISTRATE.item("source_fiber_mesh", Item::new)
            .lang("§dSource Fiber Mesh")
            .register();
    public static ItemEntry<Item> QUARRY_BEE = REGISTRATE
            .item("quarry_bee", Item::new)
            .lang("Quarry Bee")
            .register();
    public static ItemEntry<Item> LUMBER_BEE = REGISTRATE
            .item("lumber_bee", Item::new)
            .lang("Lumber Bee")
            .register();
    public static ItemEntry<Item> ROYAL_JELLY = REGISTRATE
            .item("royal_jelly", Item::new)
            .lang("§dRoyal Jelly")
            .register();
    public static ItemEntry<Item> HONEY_COMB_BASE = REGISTRATE
            .item("honey_comb_base", Item::new)
            .lang("§6Honeycomb Base")
            .register();

    public static ItemEntry<TooltipItem> THORIUM_FUEL_PELLET = REGISTRATE
            .item("thorium_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("A compacted pellet of fertile thorium.")
                            .withStyle(ChatFormatting.GOLD),
                    () -> Component
                            .literal("Designed for efficient neutron capture and U-233 breeding in reactor blankets.")
                            .withStyle(ChatFormatting.GOLD)))
            .lang("§2Thorium Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U33_FUEL_PELLET = REGISTRATE
            .item("u233_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("A highly concentrated pellet of bred Uranium-233.")
                            .withStyle(ChatFormatting.GREEN),
                    () -> Component.literal("Delivers exceptional energy output as primary fissile fuel.")
                            .withStyle(ChatFormatting.GREEN)))
            .lang("§aUranium-233 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> PLUTONIUM_241_FUEL_PELLET = REGISTRATE
            .item("plutonium_241_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("A compacted pellet of Plutonium-241.")
                            .withStyle(ChatFormatting.GOLD),
                    () -> Component.literal("Delivers exceptional energy output as primary fissile fuel.")
                            .withStyle(ChatFormatting.GREEN)))
            .lang("§cPlutonium-241 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U236_FUEL_PELLET = REGISTRATE
            .item("u236_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("A highly concentrated pellet of bred Uranium-236.")
                            .withStyle(ChatFormatting.GREEN),
                    () -> Component.literal(
                            "Designed for efficient neutron capture and Pu-241 breeding reactions as a blanket.")
                            .withStyle(ChatFormatting.GOLD)))
            .lang("§aUranium-236 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U235_FUEL_PELLET = REGISTRATE
            .item("u235_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("A compacted pellet of Uranium-235.")
                            .withStyle(ChatFormatting.DARK_GREEN),
                    () -> Component.literal("Serves as the primary driver fuel for fission reactors.")
                            .withStyle(ChatFormatting.DARK_GREEN)))
            .lang("§aUranium-235 Fuel Pellet")
            .register();

    public static ItemEntry<Item> HONEY_TREAT = REGISTRATE
            .item("honey_treat", Item::new)
            .lang("§6Honey Treat")
            .register();
    public static ItemEntry<Item> BASIC_FUEL_ROD = REGISTRATE
            .item("basic_fuel_rod", Item::new)
            .lang("Basic Fuel Rod")
            .register();
    public static ItemEntry<Item> ZIRCONIUM__ROD = REGISTRATE
            .item("zirconium_rod", Item::new)
            .lang("§2Zirconium Rod")
            .register();

    public static final ItemEntry<TooltipItem> SPACE_GRADE_STEEL_DRILLING_KIT = REGISTRATE
            .item("space_grade_steel_drilling_kit", p -> new TooltipItem(p,
                    () -> Component
                            .literal("A state-of-the-art drilling kit, engineered for extraterrestrial exploration.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Its components are hermetically sealed and radiation-hardened for deep space operations.")
                            .withStyle(ChatFormatting.DARK_PURPLE)))
            .lang("§7Space Grade Steel Drilling Kit")
            .register();

    public static final ItemEntry<TooltipItem> FROST_REINFORCED_STAINED_STEEL_DRILLING_KIT = REGISTRATE
            .item("frost_reinforced_stained_steel_drilling_kit", p -> new TooltipItem(p,
                    () -> Component.literal("A heavy-duty drilling kit, reinforced with cryogenic alloys.")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Its components are exceptionally durable and resistant to extreme temperature fluctuations.")
                            .withStyle(ChatFormatting.BLUE)))
            .lang("§3Frost Reinforced Stained Steel Drill Kit")
            .register();

    public static final ItemEntry<TooltipItem> ALUMIN_FROST_DRILLING_KIT = REGISTRATE
            .item("aluminfrost_drilling_kit", p -> new TooltipItem(p,
                    () -> Component
                            .literal("A drilling kit designed for precision in the most frigid, brittle environments.")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Its components remain perfectly chilled, preventing overheating and material degradation.")
                            .withStyle(ChatFormatting.BLUE)))
            .lang("§bAluminfrost Drill Kit")
            .register();

    public static final ItemEntry<TooltipItem> AURUM_STEEL_DRILLING_KIT = REGISTRATE
            .item("aurum_steel_drilling_kit", p -> new TooltipItem(p,
                    () -> Component
                            .literal("A kit imbued with ancient power, capable of penetrating some stubborn materials.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Forged from Aurum Steel, its strength is matched by its inherent, volatile mystery.")
                            .withStyle(ChatFormatting.BLUE)))
            .lang("§6Aurum Steel Drill Kit")
            .register();

    public static final ItemEntry<TooltipItem> SPACE_MINER_MODULE = REGISTRATE
            .item("space_miner_module", p -> new TooltipItem(p,
                    () -> Component.literal("Designed for deep-space resource extraction.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Enables efficient mining on celestial bodies and asteroids, far from terrestrial interference.")
                            .withStyle(ChatFormatting.DARK_PURPLE)))
            .lang("§dSpace Miner Module")
            .register();

    public static final ItemEntry<TooltipItem> EARTHBOUND_MINER_MODULE = REGISTRATE
            .item("earthbound_miner_module", p -> new TooltipItem(p,
                    () -> Component.literal("A versatile mining module, adaptable to multiple dimensions.")
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC),
                    () -> Component.literal(
                            "Optimized for resource gathering in the Overworld, Nether, and End, maximizing terrestrial yields.")
                            .withStyle(ChatFormatting.GOLD)))
            .lang("§eEarth Bound Miner Module")
            .register();

    public static final ItemEntry<TooltipItem> SPACE_GRADE_STEEL_DRILL_HEAD = REGISTRATE
            .item("space_grade_steel_drill_head", p -> new TooltipItem(p,
                    () -> Component
                            .literal(
                                    "An orbital-grade drill head, designed to pierce lunar regolith or asteroid cores.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    () -> Component.literal(
                            "Extremely strong in its ability to withstand vacuum, extreme temperatures, and cosmic radiation.")
                            .withStyle(ChatFormatting.DARK_PURPLE)))
            .lang("§7Space Grade Steel Drill Head")
            .register();

    public static final ItemEntry<TooltipItem> FROST_REINFORCED_STAINED_STEEL_DRILL_HEAD = REGISTRATE
            .item("frost_reinforced_stained_steel_drill_head", p -> new TooltipItem(p,
                    () -> Component
                            .literal("This drill head bears a subtle, frozen pattern, a testament to its resilience.")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    () -> Component.literal(
                            "Capable of breaking through solidified barriers while maintaining its integrity in the cold.")
                            .withStyle(ChatFormatting.BLUE)))
            .lang("§3Frost Reinforced Stained Steel Drill Head")
            .register();

    public static final ItemEntry<TooltipItem> ALUMIN_FROST_DRILL_HEAD = REGISTRATE
            .item("aluminfrost_drill_head", p -> new TooltipItem(p,
                    () -> Component
                            .literal("An icy drill head, its surface supercooled to reduce friction to almost nothing.")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    () -> Component.literal(
                            "It carves through rock and ice alike with unnerving silence, leaving a trail of frost.")
                            .withStyle(ChatFormatting.BLUE)))
            .lang("§bAluminfrost Drill Head")
            .register();

    public static final ItemEntry<TooltipItem> AURUM_STEEL_DRILL_HEAD = REGISTRATE
            .item("aurum_steel_drill_head", p -> new TooltipItem(p,
                    () -> Component.literal("The head of an ancient drill, shimmering with a golden, arcane glow.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    () -> Component.literal(
                            "It can carve through dimensions, but is prone to unexpected failures if its power is mishandled.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)))
            .lang("§6Aurum Steel Drill Head")
            .register();

    public static final ItemEntry<TooltipItem> SOURCE_IMBUED_TITANIUM_DRILL_HEAD = REGISTRATE
            .item("source_imbued_titanium_drill_head", p -> new TooltipItem(p,
                    () -> Component.literal("A marvel of metallurgy and magic, humming with pure Source energy.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    () -> Component.literal(
                            "The titanium core stabilizes the flux, allowing it to dissolve the toughest bonds at a molecular level.")
                            .withStyle(ChatFormatting.DARK_PURPLE)))
            .lang("§dSource Imbued Titanium Drill Head")
            .register();

    public static final ItemEntry<TooltipItem> FLAMING_MESH = REGISTRATE
            .item("flaming_mesh", p -> new TooltipItem(p,
                    () -> Component.literal("A superheated lattice used to filter neural essences.")
                            .withStyle(ChatFormatting.GOLD),
                    () -> Component
                            .literal("Essential for stabilizing simulation chambers during high-intensity processing.")
                            .withStyle(ChatFormatting.YELLOW)))
            .lang("§cFlaming Mesh")
            .register();

    public static final ItemEntry<TooltipItem> FLAMING_DUST = REGISTRATE
            .item("flaming_dust", p -> new TooltipItem(p,
                    () -> Component.literal("Infinitesimal embers recovered from a phoenix's pyre.")
                            .withStyle(ChatFormatting.GOLD),
                    () -> Component
                            .literal("Acts as a thermal catalyst for reconstructing mob drops from digital data.")
                            .withStyle(ChatFormatting.YELLOW)))
            .lang("§cFlaming Dust")
            .register();

    public static final ItemEntry<TooltipItem> PHOENIX_FEATHER = REGISTRATE
            .item("phoenix_feather", p -> new TooltipItem(p,
                    () -> Component.literal("A shimmering feather that pulses with the heat of a thousand suns.")
                            .withStyle(ChatFormatting.GOLD),
                    () -> Component.literal("Used to harness the ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Phoenix Force").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" for extreme metallurgy.").withStyle(ChatFormatting.YELLOW))))
            .lang("§cFeather §6Of §cRebirth")
            .register();
    public static ItemEntry<ArmorComponentItem> PHOENIX_HELMET = REGISTRATE
            .item("phoenix_helmet", (p) -> new ArmorComponentItem(GTArmorMaterials.ARMOR, ArmorItem.Type.HELMET, p)
                    .setArmorLogic(new PhoenixTechSuite(ArmorItem.Type.HELMET,
                            16384,
                            500_000_000L,
                            8)))
            .lang("Phoenix Tech Suite Helmet")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_HELMETS)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<PhoenixArmorItem> PHOENIX_WINGS = REGISTRATE
            .item("phoenix_wings", (p) -> new PhoenixArmorItem(GTArmorMaterials.ARMOR, ArmorItem.Type.CHESTPLATE, p,
                    new PhoenixTechSuite(ArmorItem.Type.CHESTPLATE, 16384, 500_000_000L, 6)))
            .lang("Phoenix Wings")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_CHESTPLATES)
            .register();

    public static ItemEntry<PhoenixArmorItem> PHOENIX_CHESTPLATE = REGISTRATE
            .item("phoenix_chestplate",
                    (p) -> new PhoenixArmorItem(GTArmorMaterials.ARMOR, ArmorItem.Type.CHESTPLATE, p,
                            new PhoenixTechSuite(ArmorItem.Type.CHESTPLATE, 16384, 500_000_000L, 6)))
            .lang("Phoenix Tech Suite Chestplate")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_CHESTPLATES)
            .tag(ItemTags.FREEZE_IMMUNE_WEARABLES)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<ArmorComponentItem> PHOENIX_LEGGINGS = REGISTRATE
            .item("phoenix_leggings", (p) -> new ArmorComponentItem(GTArmorMaterials.ARMOR, ArmorItem.Type.LEGGINGS, p)
                    .setArmorLogic(new PhoenixTechSuite(ArmorItem.Type.LEGGINGS,
                            16384,
                            500_000_000L,
                            6)))
            .lang("Phoenix Tech Suite Leggings")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_LEGGINGS)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<ArmorComponentItem> PHOENIX_BOOTS = REGISTRATE
            .item("phoenix_boots", (p) -> new ArmorComponentItem(GTArmorMaterials.ARMOR, ArmorItem.Type.BOOTS, p)
                    .setArmorLogic(new PhoenixTechSuite(ArmorItem.Type.BOOTS,
                            16384,
                            500_000_000L,
                            6)))
            .lang("Phoenix Tech Suite Boots")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_BOOTS)
            .tag(CustomTags.PPE_ARMOR)
            .tag(CustomTags.STEP_BOOTS)
            .register();

    public static ItemEntry<TeslaStabilizerItem> LV_TESLA_STABILIZER = REGISTRATE
            .item("lv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("LV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "LV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for low-voltage wireless power.\nIs this even useful?");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/lv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> MV_TESLA_STABILIZER = REGISTRATE
            .item("mv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("MV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "MV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for medium-voltage wireless power.\nYou probably won't use this.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/mv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> HV_TESLA_STABILIZER = REGISTRATE
            .item("hv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("HV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "HV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for high-voltage wireless power.\nOne could say, this is quite shocking!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/hv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> EV_TESLA_STABILIZER = REGISTRATE
            .item("ev_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("EV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "EV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for extreme-voltage wireless power. \nRadical!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/ev_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> IV_TESLA_STABILIZER = REGISTRATE
            .item("iv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("IV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "IV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for insane-voltage wireless power.\nCrazy? I was crazy once.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/iv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> LuV_TESLA_STABILIZER = REGISTRATE
            .item("luv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("LuV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "LuV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ludicrous-voltage wireless power.\nYour progress is quite impressive! Keep going!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/luv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> ZPM_TESLA_STABILIZER = REGISTRATE
            .item("zpm_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("ZPM Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "ZPM Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for zero-point-module wireless power.\nWe getting sci-fi up in here.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/zpm_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> UV_TESLA_STABILIZER = REGISTRATE
            .item("uv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("UV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "UV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ultimate-voltage wireless power.\nIs this the ultimate? Not quite!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/uv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> UHV_TESLA_STABILIZER = REGISTRATE
            .item("uhv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("UHV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "UHV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ultra-high-voltage wireless power.\nIs this the peak of power? Or merely the beginning?");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/uhv_tesla_stabilizer")))
            .register();

    public static void init() {}
}
