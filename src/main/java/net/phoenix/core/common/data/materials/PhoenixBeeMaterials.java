package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;

import net.phoenix.core.PhoenixCore;

import java.util.HashMap;
import java.util.Map;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;

public class PhoenixBeeMaterials {

    public static Material FRUCTOSE, GLUCOSE, SUCROSE, SUGAR_WATER;
    public static Material PEANUT_BUTTER, PEANUT, CREAM, SKIM_MILK, MOLASSES;
    public static Material AMMONIUM_BISULFATE, AMMONIUM_BISULFATE_SOLUTION, AMMONIUM_PERSULFATE, PROTEIN_SOLUTION,
            AMINO_ACIDS;
    public static Material HONEY_CATALYST, HONEY_COMB_BASE_MIXTURE, POLLEN_CONCENTRATE_FLUID, WAX_MELTING_CATALYST;
    public static Material AURUM_WOOD, INVERT_SUGAR_SOLUTION, HONEY, SUPERCONDUCTIVE_HONEY;

    public static Material IMPURE_WAX, IMPURE_HONEY;

    public static final Map<String, Material> HONEYED_MATERIALS = new HashMap<>();
    public static final Map<String, Material> RAW_WAX_MATERIALS = new HashMap<>();

    public static void register() {
        HONEY = new Material.Builder(PhoenixCore.id("honey")).fluid().ingot().color(0xf9c901)
                .iconSet(MaterialIconSet.BRIGHT).buildAndRegister();
        SUPERCONDUCTIVE_HONEY = new Material.Builder(PhoenixCore.id("superconductive_honey"))
                .ingot().fluid()
                .color(0xFFBF00)
                .secondaryColor(0x87CEFA)
                .iconSet(MaterialIconSet.DULL)
                .flags(MaterialFlags.NO_SMELTING)
                .cableProperties(GTValues.V[GTValues.IV], 32, 0, true)
                .buildAndRegister();
        FRUCTOSE = new Material.Builder(PhoenixCore.id("fructose")).fluid().color(0xF0F0F0)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        GLUCOSE = new Material.Builder(PhoenixCore.id("glucose")).fluid().color(0xFFFAF0).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        SUCROSE = new Material.Builder(PhoenixCore.id("sucrose")).fluid().color(0xF8F8F8).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        SUGAR_WATER = new Material.Builder(PhoenixCore.id("sugar_water")).fluid().color(0xFFFFF0)
                .flags(DISABLE_DECOMPOSITION).iconSet(MaterialIconSet.DULL).buildAndRegister();
        INVERT_SUGAR_SOLUTION = new Material.Builder(PhoenixCore.id("invert_sugar_solution")).fluid().color(0xFFFCDE)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        MOLASSES = new Material.Builder(PhoenixCore.id("molasses")).fluid().color(0x82574C)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();

        PEANUT_BUTTER = new Material.Builder(PhoenixCore.id("peanut_butter")).fluid().color(0xD8BC9D)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        PEANUT = new Material.Builder(PhoenixCore.id("peanut")).dust().color(0xE0C8A0).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        CREAM = new Material.Builder(PhoenixCore.id("cream")).fluid().color(0xFFFBE6).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        SKIM_MILK = new Material.Builder(PhoenixCore.id("skim_milk")).fluid().color(0xF8F8FF)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();

        AMMONIUM_BISULFATE = new Material.Builder(PhoenixCore.id("ammonium_bisulfate")).dust().color(0xF0F0F0)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        AMMONIUM_BISULFATE_SOLUTION = new Material.Builder(PhoenixCore.id("ammonium_bisulfate_solution")).fluid()
                .color(0xF0F0F0).iconSet(MaterialIconSet.DULL).buildAndRegister();
        AMMONIUM_PERSULFATE = new Material.Builder(PhoenixCore.id("ammonium_persulfate")).fluid().color(0xF0FFFF)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        PROTEIN_SOLUTION = new Material.Builder(PhoenixCore.id("protein_solution")).fluid().color(0xFFE0C0)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        AMINO_ACIDS = new Material.Builder(PhoenixCore.id("amino_acids")).fluid().color(0xFFFFFF)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();

        HONEY_CATALYST = new Material.Builder(PhoenixCore.id("honey_catalyst")).fluid().color(0xFFF9E3)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        HONEY_COMB_BASE_MIXTURE = new Material.Builder(PhoenixCore.id("honey_comb_base_mixture")).fluid()
                .color(0xFFF0F5).iconSet(MaterialIconSet.DULL).buildAndRegister();
        POLLEN_CONCENTRATE_FLUID = new Material.Builder(PhoenixCore.id("pollen_concentrate_fluid")).fluid()
                .color(0xFFC200).iconSet(MaterialIconSet.DULL).buildAndRegister();
        WAX_MELTING_CATALYST = new Material.Builder(PhoenixCore.id("wax_melting_catalyst")).fluid().color(0xADD8E6)
                .secondaryColor(0x6A5ACD).iconSet(MaterialIconSet.DULL).buildAndRegister();

        AURUM_WOOD = new Material.Builder(PhoenixCore.id("aurum_wood")).dust().color(0x291306).secondaryColor(0xfccd31)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();

        IMPURE_WAX = new Material.Builder(PhoenixCore.id("impure_wax")).fluid().color(0xFFA500).secondaryColor(0xD2691E)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        IMPURE_HONEY = new Material.Builder(PhoenixCore.id("impure_honey")).fluid().color(0xD2B48C)
                .secondaryColor(0x8B4513).iconSet(MaterialIconSet.DULL).buildAndRegister();

        registerAllBeeMineralMaterials();
    }

    private static void registerAllBeeMineralMaterials() {
        Map<String, int[]> colors = new HashMap<>();
        colors.put("pitchblende", new int[] { 0xffd647, 0x0d1e2f });
        colors.put("steel", new int[] { 0x3b3b3b, 0x727272 });
        colors.put("apatite", new int[] { 0x06cdf1, 0x701c07 });
        colors.put("cobalt", new int[] { 0x0f3d79, 0x0f3d79 });
        colors.put("salt", new int[] { 0x945e5a, 0x68fcfc });
        colors.put("sponge", new int[] { 0xbcbc96, 0xe81123 });
        colors.put("ghostly", new int[] { 0xb5c3c8, 0xbdc8cd });
        colors.put("copper", new int[] { 0xB87333, 0xA0522D });
        colors.put("magma", new int[] { 0xcd691b, 0x7b3f10 });
        colors.put("rock_salt", new int[] { 0x646060, 0xc1b9b8 });
        colors.put("slime", new int[] { 0x2ce551, 0x1c9234 });
        colors.put("brown_shroom", new int[] { 0x71492e, 0x2ce551 });
        colors.put("sculk", new int[] { 0x131313, 0x0c0c0c });
        colors.put("nether_quartz", new int[] { 0x7a7672, 0x8c8883 });
        colors.put("scheelite", new int[] { 0xd7e8b3, 0x143cae });
        colors.put("certus_quartz", new int[] { 0xdce2f3, 0x6e95bf });
        colors.put("silky", new int[] { 0xdce2f3, 0x6e95bf });
        colors.put("ice", new int[] { 0x979797, 0x8e908b });
        colors.put("withered", new int[] { 0x131313, 0x141414 });
        colors.put("zombie", new int[] { 0x3e3434, 0x786464 });
        colors.put("blaze", new int[] { 0xf9d678, 0x3e3434 });
        colors.put("bone", new int[] { 0x636363, 0x727272 });
        colors.put("lepidolite", new int[] { 0xffdae4, 0x75376f });
        colors.put("source_gem", new int[] { 0xb403e9, 0x8e02b8 });
        colors.put("cinnabar", new int[] { 0xE34234, 0xB22222 });
        colors.put("topaz", new int[] { 0xFFC87C, 0xFFB347 });
        colors.put("amethyst", new int[] { 0x9966CC, 0x6C3483 });
        colors.put("prismarine", new int[] { 0x7FFFD4, 0x40E0D0 });
        colors.put("realgar", new int[] { 0xff3d33, 0x3f0110 });
        colors.put("pyrope", new int[] { 0x8B0000, 0xB22222 });
        colors.put("zinc", new int[] { 0x7D7F7D, 0xA9A9A9 });
        colors.put("tin", new int[] { 0xD9D6CF, 0xB0B0B0 });
        colors.put("diamond", new int[] { 0xB9F2FF, 0xE0FFFF });
        colors.put("iron", new int[] { 0xD8D8D8, 0xA9A9A9 });
        colors.put("fluorite", new int[] { 0x0C9949, 0x0C9949 });
        colors.put("ruby", new int[] { 0xE0115F, 0xA52A2A });
        colors.put("sapphire", new int[] { 0x0F52BA, 0x4682B4 });
        colors.put("stibnite", new int[] { 0x484848, 0xA9A9A9 });
        colors.put("opal", new int[] { 0xA8C3BC, 0xE0FFFF });
        colors.put("lapis", new int[] { 0x26619C, 0x1C39BB });
        colors.put("electrotine", new int[] { 0x83cbf5, 0x004585 });
        colors.put("redstone", new int[] { 0xFF0000, 0x8B0000 });
        colors.put("saltpeter", new int[] { 0xEDEDED, 0xB0B0B0 });
        colors.put("coal", new int[] { 0x222222, 0x444444 });
        colors.put("ilmenite", new int[] { 0x4B3A2F, 0x8B5F2F });
        colors.put("silicon", new int[] { 0xC0C0C0, 0x808080 });
        colors.put("galena", new int[] { 0x8B8B8B, 0x5A5A5A });
        colors.put("sodalite", new int[] { 0x284387, 0x1C1C7C });
        colors.put("gold", new int[] { 0xFFD700, 0xB8860B });
        colors.put("obsidian", new int[] { 0x2D2D2D, 0x4B0082 });
        colors.put("cobaltite", new int[] { 0x3D59AB, 0x1E3A5C });
        colors.put("bauxite", new int[] { 0xA0522D, 0x8B4513 });
        colors.put("silver", new int[] { 0xC0C0C0, 0xB0B0B0 });
        colors.put("tungstate", new int[] { 0xe0ffc4, 0xab4400 });
        colors.put("emerald", new int[] { 0x50C878, 0x228B22 });
        colors.put("tricalcium_phosphate", new int[] { 0xF5F5DC, 0xE0E0E0 });
        colors.put("nickel", new int[] { 0xAFAFAF, 0x7F7F7F });
        colors.put("fluix", new int[] { 0x6E00FF, 0xB266FF });
        colors.put("malachite", new int[] { 0x43B48C, 0x228B22 });
        colors.put("lead", new int[] { 0x575757, 0x363636 });
        colors.put("invar", new int[] { 0xB2B2B2, 0x8B8B8B });
        colors.put("thorium", new int[] { 0x14230e, 0x111e0c });
        colors.put("graphite", new int[] { 0x10130b, 0x262821 });
        colors.put("sphalerite", new int[] { 0xffdc88, 0x0f1605 });
        colors.put("netherite", new int[] { 0x4c484c, 0x474447 });
        colors.put("resonant_ender", new int[] { 0x0b0b0b, 0x0b0b0b });
        colors.put("chromite", new int[] { 0x544155, 0x2c222c });
        colors.put("pyrolusite", new int[] { 0x1f1e1d, 0x363230 });
        colors.put("platinum", new int[] { 0xfff4ba, 0x8d8d71 });
        colors.put("bismuth", new int[] { 0x5fdddd, 0x517385 });
        colors.put("glowstone", new int[] { 0x816f40, 0xe7c874 });
        colors.put("bastnasite", new int[] { 0x733a21, 0x452314 });
        colors.put("tetrahedrite", new int[] { 0x171d16, 0x242d22 });
        colors.put("sulfur", new int[] { 0xd3ec8a, 0xe1fc93 });
        colors.put("oilsands", new int[] { 0x181814, 0x3c3729 });
        colors.put("tantalite", new int[] { 0x180b04, 0x302828 });
        colors.put("barite", new int[] { 0xe8e2d1, 0xf4b74b });
        colors.put("vanadium_magnetite", new int[] { 0x373741, 0x130f17 });
        colors.put("pyrochlore", new int[] { 0x160d07, 0x2a1a0d });
        colors.put("voidglass_shard", new int[] { 0x6900a8, 0x62009d });
        colors.put("ignisium", new int[] { 0x972900, 0x6c2207 });

        for (Map.Entry<String, int[]> entry : colors.entrySet()) {
            String name = entry.getKey();
            int baseColor = entry.getValue()[0];
            int secColor = entry.getValue()[1];

            Material honeyed = new Material.Builder(PhoenixCore.id("honeyed_" + name))
                    .fluid()
                    .color(baseColor)
                    .secondaryColor(secColor)
                    .iconSet(MaterialIconSet.DULL)
                    .buildAndRegister();
            HONEYED_MATERIALS.put(name, honeyed);

            Material rawWax = new Material.Builder(PhoenixCore.id("raw_" + name + "_wax"))
                    .dust()
                    .color(baseColor)
                    .secondaryColor(secColor)
                    .iconSet(MaterialIconSet.DULL)
                    .buildAndRegister();
            RAW_WAX_MATERIALS.put(name, rawWax);
        }
    }
}
