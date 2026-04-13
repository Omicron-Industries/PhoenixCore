package net.phoenix.core.common.data.bees;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.phoenix.core.common.data.recipe.records.ApisProgenitorConfig;
import net.phoenix.core.common.data.recipe.records.FullBeeConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.phoenix.core.common.data.materials.PhoenixBeeMaterials.SUGAR_WATER;

@SuppressWarnings("All")
public class BeeRecipeData {

    public static final String MOD_ID = "phoenixcore";
    public static final int FINAL_HONEY_OUTPUT_AMOUNT = 250;

    public static final Material SUGAR_WATER_MATERIAL = SUGAR_WATER;
    public static final int SUGAR_WATER_AMOUNT = 100;
    public static final String HONEY_FLUID = "phoenixcore:honey";

    public static final int DEFAULT_IV_EUT = GTValues.VA[GTValues.IV];
    public static final int DEFAULT_LUV_EUT = GTValues.VA[GTValues.LuV];
    public static final int DEFAULT_ZPM_EUT = GTValues.VA[GTValues.ZPM];
    public static final int DEFAULT_UV_EUT = GTValues.VA[GTValues.UV];

    public static final Map<String, String> ALL_BEE_NAMES = Map.ofEntries(
            Map.entry("iron", "Iron Bee"), Map.entry("diamond", "Diamond Bee"), Map.entry("apatite", "Apatite Bee"),
            Map.entry("copper", "Copper Bee"), Map.entry("sulfur", "Sulfur Bee"), Map.entry("platinum", "Platinum Bee"),
            Map.entry("graphite", "Graphite Bee"), Map.entry("barite", "Barite Bee"),
            Map.entry("bismuth", "Bismuth Bee"),
            Map.entry("tetrahedrite", "Tetrahedrite Bee"), Map.entry("vanadium_magnetite", "Vanadium Magnetite Bee"),
            Map.entry("pyrochlore", "Pyrochlore Bee"), Map.entry("oilsands", "Oil Sands Bee"),
            Map.entry("bastnasite", "Bastnasite Bee"),
            Map.entry("emerald", "Emerald Bee"), Map.entry("gold", "Gold Bee"), Map.entry("redstone", "Redstone Bee"),
            Map.entry("lapis", "Lapis Bee"), Map.entry("zinc", "Zinc Bee"), Map.entry("tin", "Tin Bee"),
            Map.entry("lead", "Lead Bee"), Map.entry("silver", "Silver Bee"), Map.entry("nickel", "Nickel Bee"),
            Map.entry("coal", "Coal Bee"), Map.entry("constantan", "Constantan Bee"),
            Map.entry("electrotine", "Electrotine Bee"),
            Map.entry("invar", "Invar Bee"), Map.entry("sapphire", "Sapphire Bee"), Map.entry("ruby", "RuBee"),
            Map.entry("amethyst", "Amethyst Bee"), Map.entry("topaz", "Topaz Bee"),
            Map.entry("fluorite", "Fluorite Bee"),
            Map.entry("cinnabar", "Cinnabar Bee"), Map.entry("realgar", "Realgar Bee"),
            Map.entry("stibnite", "Stibnite Bee"),
            Map.entry("opal", "Opal Bee"), Map.entry("pyrope", "Pyrope Bee"), Map.entry("scheelite", "Scheelite Bee"),
            Map.entry("cobaltite", "Cobaltite Bee"), Map.entry("cobalt", "Cobalt Bee"),
            Map.entry("bauxite", "Bauxite Bee"),
            Map.entry("tungstate", "Tungstate Bee"), Map.entry("desh", "Desh Bee"), Map.entry("steel", "Steel Bee"),
            Map.entry("tricalcium_phosphate", "Tricalcium Phosphate Bee"), Map.entry("pitchblende", "Pitchblende Bee"),
            Map.entry("galena", "Galena Bee"), Map.entry("ilmenite", "Ilmenite Bee"),
            Map.entry("malachite", "Malachite Bee"),
            Map.entry("obsidian", "Obsidian Bee"), Map.entry("blaze", "Blazing Bee"),
            Map.entry("prismarine", "Prismarine Bee"),
            Map.entry("sculk", "Sculk Bee"), Map.entry("sponge", "Sponge Bee"), Map.entry("frosty", "Frosty Bee"),
            Map.entry("slime", "Slimy Bee"), Map.entry("menril", "Menril Bee"), Map.entry("salt", "Salty Bee"),
            Map.entry("warped", "Warped Shroombee"), Map.entry("brown_shroom", "Brown Shroombee"),
            Map.entry("red_shroom", "Red Shroombee"),
            Map.entry("crimson", "Crimson Shroombee"), Map.entry("arcane_crystal", "Arcanus Bee"),
            Map.entry("rune", "Rune Bee"),
            Map.entry("withered", "Withered Bee"), Map.entry("netherite", "Netherite Bee"),
            Map.entry("bone", "Skeletal Bee"),
            Map.entry("sticky_resin", "Sticky Resin Bee"), Map.entry("zombie", "ZomBee"),
            Map.entry("silicon", "Silicon Bee"),
            Map.entry("silky", "Silky Bee"), Map.entry("ghostly", "Ghostly Bee"),
            Map.entry("lepidolite", "Lepidolite Bee"),
            Map.entry("magma", "Magmatic Bee"), Map.entry("certus_quartz", "Certus Quartz Bee"),
            Map.entry("cheese", "CheesyB"),
            Map.entry("rock_salt", "Rock Salt Bee"), Map.entry("super_factory", "Super Factory Bee"),
            Map.entry("fluix", "Fluix Bee"),
            Map.entry("water", "Water Bee"), Map.entry("rancher", "Rancher Bee"), Map.entry("thorium", "Thorium Bee"),
            Map.entry("sphalerite", "Sphalerite Bee"), Map.entry("chromite", "Chromite Bee"),
            Map.entry("pyrolusite", "Pyrolusite Bee"),
            Map.entry("tantalite", "Tantalite Bee"), Map.entry("draconic", "Draconic Bee"),
            Map.entry("crystallized_fluxstone", "Fluxstone Bee"),
            Map.entry("ignisium", "Ignisium Bee"), Map.entry("sky_steel", "Sky Steel Bee"),
            Map.entry("infinity", "Infinity Bee"),
            Map.entry("source_gem", "Source Bee"), Map.entry("experience", "Experience Bee"),
            Map.entry("nether_quartz", "Nether Quartz Bee"),
            Map.entry("sodalite", "Sodalite Bee"), Map.entry("glowstone", "Glowstone Bee"), Map.entry("ice", "Ice Bee"),
            Map.entry("saltpeter", "Saltpeter Bee"), Map.entry("resonant_ender", "Ender Bee"),
            Map.entry("acidic", "Acidic Bee"),
            Map.entry("voidglass_shard", "Voidglass Shard Bee"));

    public static int tierFor(String beeId) {
        return 1;
    }

    public static final List<ApisProgenitorConfig> UNIQUE_APIS_PROGENITOR_CONFIGS = createProgenitorConfigs();

    private static List<ApisProgenitorConfig> createProgenitorConfigs() {
        List<ApisProgenitorConfig> p = new ArrayList<>();
        String jelly = "phoenixcore:royal_jelly";

        p.add(progenitor("root_copper", "crystalline", "copper", jelly, 360, DEFAULT_IV_EUT));
        p.add(progenitor("root_iron", "crystalline", "iron", jelly, 360, DEFAULT_IV_EUT));
        p.add(progenitor("root_coal", "leafcutter", "coal", jelly, 360, DEFAULT_IV_EUT));
        p.add(progenitor("root_nickel", "quartz", "nickel", jelly, 360, DEFAULT_IV_EUT));

        p.add(progenitor("iron_to_tin", "iron", "tin", "4x gtceu:tin_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("tin_to_lead", "tin", "lead", "4x gtceu:lead_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("lead_to_zinc", "lead", "zinc", "4x gtceu:zinc_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("zinc_to_silver", "zinc", "silver", "4x gtceu:silver_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("silver_to_gold", "silver", "gold", "4x minecraft:gold_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("gold_to_platinum", "gold", "platinum", "4x gtceu:raw_platinum_block", 400, DEFAULT_LUV_EUT));
        p.add(progenitor("iron_to_steel", "iron", "steel", "4x gtceu:steel_block", 360, DEFAULT_IV_EUT));

        p.add(progenitor("ender_to_diamond", "resonant_ender", "diamond", "4x minecraft:diamond_block", 360,
                DEFAULT_IV_EUT));
        p.add(diamondProg("emerald", "4x minecraft:emerald_block", DEFAULT_IV_EUT));
        p.add(diamondProg("ruby", "4x gtceu:ruby_block", DEFAULT_IV_EUT));
        p.add(diamondProg("sapphire", "4x gtceu:sapphire_block", DEFAULT_IV_EUT));
        p.add(diamondProg("topaz", "4x gtceu:topaz_block", DEFAULT_IV_EUT));
        p.add(diamondProg("amethyst", "4x minecraft:amethyst_block", DEFAULT_IV_EUT));
        p.add(diamondProg("opal", "4x gtceu:opal_block", DEFAULT_IV_EUT));
        p.add(diamondProg("lapis", "4x minecraft:lapis_block", DEFAULT_IV_EUT));

        p.add(progenitor("coal_to_sulfur", "coal", "sulfur", "4x gtceu:sulfur_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("quarry_to_apatite", "quarry", "apatite", "4x gtceu:apatite_block", 360, DEFAULT_IV_EUT));
        p.add(progenitor("quarry_to_quartz", "quarry", "nether_quartz", "4x minecraft:quartz_block", 360,
                DEFAULT_IV_EUT));
        p.add(progenitor("quartz_to_certus", "nether_quartz", "certus_quartz", "4x gtceu:certus_quartz_block", 360,
                DEFAULT_IV_EUT));

        p.add(progenitor("jelly_to_blaze", "magma", "blaze", jelly, 400, DEFAULT_IV_EUT));
        p.add(progenitor("jelly_to_wither", "bone", "withered", jelly, 600, DEFAULT_LUV_EUT));
        p.add(progenitor("jelly_to_ghostly", "withered", "ghostly", jelly, 400, DEFAULT_IV_EUT));
        p.add(progenitor("jelly_to_sculk", "obsidian", "sculk", jelly, 400, DEFAULT_IV_EUT));

        p.add(progenitor("diamond_to_netherite", "diamond", "netherite", "4x minecraft:ancient_debris", 600,
                DEFAULT_LUV_EUT));
        p.add(progenitor("ender_to_draconic", "resonant_ender", "draconic", "1x minecraft:dragon_egg", 1200,
                DEFAULT_LUV_EUT));
        p.add(new ApisProgenitorConfig("infinity_special", "draconic", 1, "infinity", 1,
                List.of("gtceu:neutronium_ingot"), List.of("gtceu:infinity_fluid 200"), 600, DEFAULT_UV_EUT));

        return p;
    }

    public static final List<String> LUMBER_LOG_TYPES = List.of(
            "minecraft:oak_log",
            "minecraft:spruce_log",
            "minecraft:birch_log",
            "minecraft:jungle_log",
            "minecraft:acacia_log",
            "minecraft:dark_oak_log",
            "minecraft:mangrove_log",
            "minecraft:cherry_log",
            "minecraft:crimson_stem",
            "gtceu:rubber_log",
            "forbidden_arcanus:edelwood_log",
            "forbidden_arcanus:aurum_log",
            "minecraft:warped_stem",
            "ars_nouveau:red_archwood_log",
            "ars_nouveau:blue_archwood_log",
            "ars_nouveau:green_archwood_log",
            "ars_nouveau:purple_archwood_log");

    public static final Map<String, FullBeeConfig> ALL_BEE_CONFIGS = createAllBeeConfigs();

    private static Map<String, FullBeeConfig> createAllBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();
        configs.putAll(createOreBeeConfigs());
        configs.putAll(createMiscBeeConfigs());
        return configs;
    }

    private static Map<String, FullBeeConfig> createOreBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();
        List<String> miscIds = List.of("water", "rancher", "acidic", "brown_shroom", "red_shroom", "crimson", "warped",
                "ghostly", "sponge", "slime", "source_gem", "sculk", "withered", "bone", "blaze", "glowstone", "ice",
                "fluix", "resonant_ender", "magma", "silky", "skeletal", "zombie", "prismarine");

        for (String id : ALL_BEE_NAMES.keySet()) {
            if (miscIds.contains(id)) continue;

            String namespace = "gtceu";
            if (List.of("iron", "gold", "copper", "diamond", "emerald", "lapis", "redstone", "coal", "netherite",
                    "amethyst").contains(id))
                namespace = "minecraft";
            else if (List.of("fluorite", "ignisium", "crystallized_fluxstone", "voidglass_shard").contains(id))
                namespace = "phoenixcore";

            String blockId = namespace + ":" + id + "_block";
            String outputId = namespace + ":raw_" + id;
            int outputCount = 10;

            switch (id) {
                case "coal" -> outputId = "gtceu:coal_ore";
                case "redstone" -> outputId = "gtceu:raw_redstone";
                case "silicon" -> {
                    outputId = "gtceu:silicon_dust";
                    outputCount = 6;
                }
                case "netherite" -> {
                    blockId = "minecraft:ancient_debris";
                    outputId = "minecraft:ancient_debris";
                }
                case "sulfur" -> {
                    blockId = "gtceu:raw_sulfur_block";
                    outputId = "gtceu:raw_sulfur";
                }
                case "vanadium_magnetite" -> {
                    blockId = "gtceu:raw_vanadium_magnetite_block";
                    outputId = "gtceu:raw_vanadium_magnetite";
                }
                case "bastnasite" -> {
                    blockId = "gtceu:raw_bastnasite_block";
                    outputId = "gtceu:raw_bastnasite";
                }
                case "zinc", "steel" -> {
                    outputId = "gtceu:" + id + "_ingot";
                    if (id.equals("steel")) outputCount = 3;
                }
                case "sky_steel" -> {
                    blockId = "megacells:sky_steel_block";
                    outputId = "megacells:sky_steel_ingot";
                    outputCount = 3;
                }
                case "obsidian" -> {
                    blockId = "minecraft:obsidian";
                    outputId = "gtceu:obsidian_dust";
                    outputCount = 20;
                }
                case "infinity" -> {
                    blockId = "enderio:grains_of_infinity";
                    outputId = "enderio:grains_of_infinity";
                }
                case "desh" -> {
                    blockId = "ad_astra:desh_block";
                    outputId = "ad_astra:raw_desh";
                    outputCount = 9;
                }
                case "bismuth" -> {
                    blockId = "gtceu:bismuth_block";
                    outputId = "gtceu:bismuth_dust";
                    outputCount = 15;
                }
                case "voidglass_shard" -> {
                    blockId = "phoenixcore:raw_voidglass_shard_block";
                    outputId = "phoenixcore:raw_voidglass_shard";
                }
                case "ignisium" -> {
                    blockId = "phoenixcore:raw_ignisium_block";
                    outputId = "phoenixcore:raw_ignisium";
                }
                case "tetrahedrite" -> {
                    blockId = "gtceu:raw_tetrahedrite_block";
                    outputId = "gtceu:raw_tetrahedrite";
                }
                case "oilsands" -> {
                    blockId = "gtceu:raw_oilsands_block";
                    outputId = "gtceu:raw_oilsands";
                }
                case "platinum" -> {
                    blockId = "gtceu:raw_platinum_block";
                    outputId = "gtceu:raw_platinum";
                }
                case "tantalite" -> {
                    blockId = "gtceu:raw_tantalite_block";
                    outputId = "gtceu:raw_tantalite";
                }
                case "graphite" -> {
                    blockId = "gtceu:raw_graphite_block";
                    outputId = "gtceu:raw_graphite";
                }
                case "certus_quartz" -> {
                    blockId = "gtceu:raw_certus_quartz_block";
                    outputId = "gtceu:raw_certus_quartz";
                }
                case "ilmenite" -> {
                    blockId = "gtceu:raw_ilmenite_block";
                    outputId = "gtceu:raw_ilmenite";
                }
                case "cobaltite" -> {
                    blockId = "gtceu:raw_cobaltite_block";
                    outputId = "gtceu:raw_cobaltite";
                }
                case "scheelite" -> {
                    blockId = "gtceu:raw_scheelite_block";
                    outputId = "gtceu:raw_scheelite";
                }
                case "tungstate" -> {
                    blockId = "gtceu:raw_tungstate_block";
                    outputId = "gtceu:raw_tungstate";
                }
                case "electrotine" -> {
                    blockId = "gtceu:raw_electrotine_block";
                    outputId = "gtceu:raw_electrotine";
                }
                case "pitchblende" -> {
                    blockId = "gtceu:raw_pitchblende_block";
                    outputId = "gtceu:raw_pitchblende";
                }
                case "bauxite" -> {
                    blockId = "gtceu:raw_bauxite_block";
                    outputId = "gtceu:raw_bauxite";
                }
                case "galena" -> {
                    blockId = "gtceu:raw_galena_block";
                    outputId = "gtceu:raw_galena";
                }
                case "fluix" -> {
                    blockId = "ae2:fluix_block";
                    outputId = "gtceu:fluix_dust";
                    outputCount = 8;
                }
                case "fluorite" -> blockId = "phoenixcore:raw_fluorite_block";
                case "barite" -> blockId = "gtceu:raw_barite_block";
                case "tricalcium_phosphate" -> blockId = "gtceu:raw_tricalcium_phosphate_block";
                case "sphalerite" -> blockId = "gtceu:raw_sphalerite_block";
                case "pyrochlore" -> blockId = "gtceu:raw_pyrochlore_block";
                case "nether_quartz" -> blockId = "minecraft:quartz_block";
                case "pyrolusite" -> blockId = "gtceu:raw_pyrolusite_block";
                case "saltpeter" -> blockId = "gtceu:raw_saltpeter_block";
                case "salt" -> blockId = "gtceu:raw_salt_block";
                case "chromite" -> blockId = "gtceu:raw_chromite_block";
                case "lepidolite" -> blockId = "gtceu:raw_lepidolite_block";
                case "stibnite" -> blockId = "gtceu:raw_stibnite_block";
                case "cobalt" -> outputId = "gtceu:cobalt_dust";
            }

            configs.put(id, buildConfig(id, blockId, outputId, outputCount, DEFAULT_IV_EUT, 80));
        }
        return configs;
    }

    private static Map<String, FullBeeConfig> createMiscBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();
        Map<String, List<String>> miscData = Map.ofEntries(
                Map.entry("ghostly", List.of("minecraft:ghast_tear", "minecraft:ghast_tear", "16")),
                Map.entry("sponge", List.of("minecraft:sponge", "minecraft:sponge", "4")),
                Map.entry("sculk", List.of("minecraft:sculk", "minecraft:sculk", "5")),
                Map.entry("withered", List.of("minecraft:wither_rose", "minecraft:wither_rose", "5")),
                Map.entry("skeletal", List.of("minecraft:bone_block", "minecraft:bone", "5")),
                Map.entry("blaze", List.of("minecraft:blaze_rod", "minecraft:blaze_powder", "4")),
                Map.entry("water", List.of("minecraft:water_bucket", "minecraft:salmon", "1")),
                Map.entry("slime", List.of("minecraft:slime_block", "minecraft:slime_ball", "8")));

        miscData.forEach((id, data) -> configs.put(id,
                buildConfig(id, data.get(0), data.get(1), Integer.parseInt(data.get(2)), DEFAULT_IV_EUT, 200)));
        return configs;
    }

    private static FullBeeConfig buildConfig(String id, String blockId, String outputId, int count, int eut, int dur) {
        return new FullBeeConfig(id, 1, blockId, outputId, count, eut, dur, eut, 200, eut, 300, eut, 100, eut, 250, eut,
                400);
    }

    private static ApisProgenitorConfig progenitor(String name, String parent, String target, String item, int duration,
                                                   int eut) {
        return new ApisProgenitorConfig(name, parent, 1, target, 1, item.isEmpty() ? List.of() : List.of(item),
                List.of(), duration, eut);
    }

    private static ApisProgenitorConfig diamondProg(String target, String item, int eut) {
        return progenitor(target + "_progenitor", "diamond", target, item, 360, eut);
    }

    private static ApisProgenitorConfig fluidProg(String name, String parent, String target, String fluid, int duration,
                                                  int eut) {
        return new ApisProgenitorConfig(name, parent, 1, target, 1, List.of(), List.of(fluid), duration, eut);
    }
}
