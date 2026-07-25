package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.PhoenixCore;

import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;

import static appeng.core.definitions.AEItems.FLUIX_CRYSTAL;
import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.SOURCE_GEM;

public class PhoenixProgressionMaterials {

    public static Material ALUMINFROST, SOURCE_IMBUED_TITANIUM, RESONANT_RHODIUM_ALLOY;
    public static Material EightyFivePercentPureNevonianSteel, AURUM_STEEL,
            FIERY_BRONZE, VOID_TOUCHED_TUNGSTEN_STEEL, FROST_REINFORCED_STAINED_STEEL, FROST,
            ADVANCED_QUIN_NAQUADIAN_ALLOY, NEBULAR_GOLD;

    public static Material MAGMATIC_MANGANESE_LEAD, CRYOGENIC_ALUMINUM_STRAND, ICY_STEEL_MATRIX,
            SOURCE_TITANIUM_FILAMENT, MAGMA, SOURCE_GEM, SPONGE, SLIME, ZOMBIE, WITHERED, GHOSTLY, FLUIX, PRISMARINE,
            SILKY;

    public static Material AKASHIC_ZERONIUM, PHOENIX_ENRICHED_NAQUADAH, PHOENIX_ENRICHED_TRITANIUM, AETHERIUM_STEEL,
            SUBSPACE_COBALT, UNREFINED_ALUMINFROST;
    public static Material SINGULARITY_FORGED_TITANIUM, EXOTIC_VANADIUM_COMPOSITE, DARK_MATTER_PLATED_IRIDIUM,
            CORRUPTED_HYPERALLOY;
    public static Material REALITY_BOUND_OSMIUM, VOID_STITCHED_NEODYMIUM, CELESTIAL_AURORIUM, PRIMORDIAL_FLUX_METAL,
            ETERNAL_STARFORGED_STEEL, BLAZING_DRILLING_FLUID, VOIDIC_DRILLING_FLUID, CRYO_EMBER_FLUID;
    public static Material DIMENSIONAL_REFLECTION_ALLOY, TIMEWOVEN_PLATINUM, SOULBOUND_ETHERSTEEL,
            TACHYON_INFUSED_CHROMIUM, ECHO_CRYSTAL_ALLOY;
    public static Material NEBULAR_RESONANCE_INGOT, PARADOXIUM, PHOENIX_TEMPERED_MITHRIL, VOID_SUNG_ADAMANTITE,
            ENTANGLED_PALLADIUM;
    public static Material ENTANGLED_NEUTRON_ALLOY, SUPERPOSITION_TUNGSTEN_MATRIX, EXO_TEMPORAL_ORICHALCUM,
            GALACTIC_HEART_MATTER;
    public static Material DARK_NEBULA_INFUSED_IRIDIUM, ELDRITCH_VOIDSTEEL, AKASHIC_CHRONO_METAL,
            ABSOLUTE_ZERO_ZERONIUM;
    public static Material MULTIVERSAL_HYPERALLOY, GLITCHED_CORRUPTION_SUBSTRATE, EVENT_HORIZON_MATTER, ANTIMATTER,
            RESONANT_ENDER, PRISMATIC_PAINT;
    public static Material NAQUADATITE_ACIDIC, HEAVILY_FLUORINE_CONTAMINATED_NAQUADATITE,
            RADIOACTIVE_AND_HEAVY_METAL_CONTAMINATED_NAQUADATITE, RADIOACTIVE_CONTAMINATED_NAQUADATITE,
            RADIOACTIVE_NAQUADACID, NAQUADAH_DIOXIDE, LIGHTLY_CHLORINE_CONTAMINATED_WASTE,
            CALCIUM_FLUORINE, ALUMINIUM_CAESIUM_CHLORIDE, ASTATINE, NAQUADATIA_ASTATINE,
            SODIUM_ASTATINE_BISULFATE, HEAVY_METAL_SLUDGE, ACIDIC_WASTE_SLUDGE,
            HEAVILY_ACIDIC_NAQUADATITE, CHLORINE_WASTE_NAQUADATIA_ASTATINE, NAQUADATITE, GRAPHENE_SLUDGE;

    public static void register() {
        PRISMATIC_PAINT = new Material.Builder(PhoenixCore.id("prismatic_paint"))
                .fluid()
                .color(0x00FFFF)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        RESONANT_ENDER = new Material.Builder(PhoenixCore.id("resonant_ender"))
                .ingot()
                .fluid()
                .color(0x032620)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(RESONANT_ENDER, Items.ENDER_PEARL.asItem());

        FLUIX = new Material.Builder(PhoenixCore.id("fluix"))
                .ingot()
                .color(0x6E00FF)
                .secondaryColor(0xB266FF)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(FLUIX, FLUIX_CRYSTAL.asItem());

        SLIME = new Material.Builder(PhoenixCore.id("slime"))
                .ingot()
                .color(0x2ce551)
                .secondaryColor(0x1c9234)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(SLIME, Items.SLIME_BALL.asItem());
        block.setIgnored(SLIME, Blocks.SLIME_BLOCK.asItem());

        ZOMBIE = new Material.Builder(PhoenixCore.id("zombie"))
                .ingot()
                .color(0x3e3434)
                .secondaryColor(0x786464)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(ZOMBIE, Items.ROTTEN_FLESH.asItem());

        PRISMARINE = new Material.Builder(PhoenixCore.id("prismarine"))
                .dust()
                .color(0x7FFFD4)
                .secondaryColor(0x40E0D0)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(PRISMARINE, Blocks.PRISMARINE.asItem());

        SILKY = new Material.Builder(PhoenixCore.id("silky"))
                .dust()
                .color(0xdce2f3)
                .secondaryColor(0x6e95bf)
                .iconSet(MaterialIconSet.SHINY).buildAndRegister();

        ingot.setIgnored(SILKY, Items.STRING.asItem());

        MAGMA = new Material.Builder(PhoenixCore.id("magma"))
                .dust()
                .ingot()
                .fluid()
                .color(0xff6d00)
                .secondaryColor(0xeaa92e)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ingot.setIgnored(MAGMA, Blocks.MAGMA_BLOCK.asItem());

        SPONGE = new Material.Builder(PhoenixCore.id("sponge"))
                .dust()
                .ingot()
                .color(0xbcbc96)
                .secondaryColor(0xe81123)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ingot.setIgnored(SPONGE, Blocks.SPONGE.asItem());

        WITHERED = new Material.Builder(PhoenixCore.id("withered"))
                .dust()
                .ingot()
                .color(0x131313)
                .secondaryColor(0x141414)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ingot.setIgnored(WITHERED, Items.WITHER_SKELETON_SKULL.asItem());

        GHOSTLY = new Material.Builder(PhoenixCore.id("ghostly"))
                .dust()
                .ingot()
                .color(0xb5c3c8)
                .secondaryColor(0xbdc8cd)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ingot.setIgnored(GHOSTLY, Items.GHAST_TEAR.asItem());

        SOURCE_GEM = new Material.Builder(PhoenixCore.id("source_gem"))
                .dust()
                .color(0xBF00FF)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ingot.setIgnored(SOURCE_GEM, ItemsRegistry.SOURCE_GEM::get);

        FROST = new Material.Builder(PhoenixCore.id("frost"))
                .langValue("Frost")
                .fluid()
                .color(0xA7D1EB)
                .secondaryColor(0x778899)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();

        UNREFINED_ALUMINFROST = new Material.Builder(PhoenixCore.id("unrefined_aluminfrost"))
                .dust()
                .color(0xA7D1EB)
                .cableProperties(GTValues.MV, 1, 2, false)
                .secondaryColor(0x778899)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ALUMINFROST = new Material.Builder(PhoenixCore.id("aluminfrost")).ingot().fluid().color(0xadd8e6)
                .secondaryColor(0xc0c0c0).iconSet(MaterialIconSet.BRIGHT)
                .fluidPipeProperties(1200, 110, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, GENERATE_SMALL_GEAR,
                        PHOSPHORESCENT, GENERATE_LONG_ROD,
                        GENERATE_ROD, GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        GENERATE_FOIL)
                .buildAndRegister();
        SOURCE_IMBUED_TITANIUM = new Material.Builder(PhoenixCore.id("source_imbued_titanium")).ingot().fluid()
                .color(0xc600ff).formula("✨C✨Ti").iconSet(MaterialIconSet.METALLIC)
                .cableProperties(GTValues.EV, 1, 2, false)
                .fluidPipeProperties(2800, 200, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_SMALL_GEAR, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_LONG_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR, GENERATE_FOIL,
                        GENERATE_ROTOR)
                .buildAndRegister();
        RESONANT_RHODIUM_ALLOY = new Material.Builder(PhoenixCore.id("resonant_rhodium_alloy"))
                .ingot().fluid()
                .color(0xE245F8).secondaryColor(0xA345B0).iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Rhodium, 3, GTMaterials.Palladium, 4, PhoenixOres.POLARITY_FLIPPED_BISMUTHITE,
                        1, GTMaterials.Cerium, 4)
                .cableProperties(GTValues.LuV, 1, 2, false)
                .blastTemp(3600, HIGH, 480, 400).fluidPipeProperties(2800, 200, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, PHOSPHORESCENT, GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_GEAR,
                        GENERATE_SMALL_GEAR, GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        GENERATE_FOIL)
                .buildAndRegister();

        ADVANCED_QUIN_NAQUADIAN_ALLOY = new Material.Builder(PhoenixCore.id("advanced_quin_naquadian_alloy"))
                .ingot()
                .liquid(7400)
                .color(0x000000)
                .secondaryColor(0x8B0000)
                .cableProperties(GTValues.ZPM, 1, 2, false)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .fluidPipeProperties(8000, 800, true, true, true, true)
                .components(GTMaterials.Naquadah, 5, GTMaterials.Trinium, 1, GTMaterials.Technetium, 3,
                        GTMaterials.Strontium, 4, GTMaterials.Iodine, 1)
                .blastTemp(7200, HIGH, GTValues.VA[GTValues.ZPM], 1950)
                .flags(GENERATE_PLATE,
                        GENERATE_RING,
                        PHOSPHORESCENT,
                        GENERATE_ROD,
                        GENERATE_BOLT_SCREW,
                        GENERATE_FRAME,
                        GENERATE_DENSE,
                        GENERATE_SMALL_GEAR,
                        GENERATE_ROTOR,
                        GENERATE_FOIL,
                        GENERATE_LONG_ROD)
                .buildAndRegister();

        FROST_REINFORCED_STAINED_STEEL = new Material.Builder(PhoenixCore.id("frost_reinforced_stained_steel"))
                .ingot()
                .fluid()
                .color(0xB0E0E6).secondaryColor(0x708090)
                .iconSet(MaterialIconSet.SHINY)
                .cableProperties(GTValues.HV, 1, 2, false)
                .components(GTMaterials.StainlessSteel, 4, FROST, 2, GTMaterials.Aluminium, 2)
                .blastTemp(4500, BlastProperty.GasTier.HIGH, GTValues.VA[GTValues.EV], 1200)
                .flags(
                        GENERATE_PLATE,
                        GENERATE_ROD,
                        GENERATE_LONG_ROD,
                        GENERATE_GEAR,
                        GENERATE_SMALL_GEAR,
                        GENERATE_FRAME,
                        GENERATE_BOLT_SCREW,
                        GENERATE_ROTOR,
                        GENERATE_RING,
                        GENERATE_DENSE,
                        GENERATE_FOIL)
                .buildAndRegister();
        EightyFivePercentPureNevonianSteel = new Material.Builder(
                PhoenixCore.id("eighty_five_percent_pure_nevonian_steel")).ingot().color(0xFFFFE0)
                .secondaryColor(0xFFD700).iconSet(PhoenixMaterialSet.ALMOST_PURE_NEVONIAN_STEEL)
                .blastTemp(3800, MID, GTValues.VA[GTValues.EV], 1200)
                .flags(GENERATE_PLATE, GENERATE_GEAR, GENERATE_SMALL_GEAR, GENERATE_SPRING, PHOSPHORESCENT,
                        GENERATE_ROD, GENERATE_DENSE, GENERATE_BOLT_SCREW, GENERATE_FRAME)
                .buildAndRegister();
        VOID_TOUCHED_TUNGSTEN_STEEL = new Material.Builder(PhoenixCore.id("void_touched_tungsten_steel"))
                .ingot()
                .liquid(3100)
                .color(0x4B0082).secondaryColor(0x000000)
                .iconSet(MaterialIconSet.METALLIC)
                .cableProperties(GTValues.IV, 1, 2, false)
                .components(GTMaterials.Tungsten, 4, PhoenixOres.VOIDGLASS_SHARD, 4, GTMaterials.Molybdenum, 2)
                .blastTemp(4200, BlastProperty.GasTier.MID, GTValues.VA[GTValues.EV], 1000)
                .fluidPipeProperties(3800, 250, true, true, true, true)
                .flags(
                        GENERATE_PLATE,
                        GENERATE_RING,
                        PHOSPHORESCENT,
                        GENERATE_ROD,
                        GENERATE_LONG_ROD,
                        GENERATE_BOLT_SCREW,
                        GENERATE_FRAME,
                        GENERATE_GEAR,
                        GENERATE_SMALL_GEAR,
                        GENERATE_DENSE,
                        GENERATE_ROTOR, GENERATE_FOIL)
                .buildAndRegister();
        AURUM_STEEL = new Material.Builder(PhoenixCore.id("aurum_steel")).ingot().fluid().color(0xd0a860)
                .secondaryColor(0xc0c0c0).iconSet(MaterialIconSet.METALLIC)
                .cableProperties(LV, 1, 2, true)
                .fluidPipeProperties(5000, 50, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR, GENERATE_FOIL,
                        GENERATE_SMALL_GEAR, GENERATE_LONG_ROD)
                .buildAndRegister();
        FIERY_BRONZE = new Material.Builder(PhoenixCore.id("fiery_bronze")).ingot().fluid().color(0xff6d00)
                .secondaryColor(0xa0522d).iconSet(MaterialIconSet.DULL)
                .fluidPipeProperties(2000, 40, true, false, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        PhoenixMaterialFlags.GENERATE_BEE_HONEYCOMB_BLOCK, PhoenixMaterialFlags.GENERATE_BEE_COMB)
                .buildAndRegister();

        MAGMATIC_MANGANESE_LEAD = new Material.Builder(PhoenixCore.id("magmatic_manganese_lead")).ingot().fluid()
                .color(0x8B4513).cableProperties(GTValues.V[LV], 2, 0, true).buildAndRegister();
        CRYOGENIC_ALUMINUM_STRAND = new Material.Builder(PhoenixCore.id("cryogenic_aluminum_strand")).ingot().fluid()
                .color(0xADD8E6).cableProperties(GTValues.V[GTValues.MV], 4, 0, true).buildAndRegister();
        ICY_STEEL_MATRIX = new Material.Builder(PhoenixCore.id("icy_steel_matrix")).ingot().fluid().color(0xE0FFFF)
                .secondaryColor(0x696969).element(PhoenixElements.ICY_STEEL_MATRIX)
                .cableProperties(GTValues.V[GTValues.HV], 8, 0, true).buildAndRegister();
        SOURCE_TITANIUM_FILAMENT = new Material.Builder(PhoenixCore.id("source_titanium_filament")).ingot().fluid()
                .color(0x8B008B).secondaryColor(0x454545).cableProperties(GTValues.V[GTValues.EV], 16, 0, true)
                .buildAndRegister();
        BLAZING_DRILLING_FLUID = new Material.Builder(PhoenixCore.id("blazing_drilling_fluid"))
                .fluid()
                .color(0xFF6600)
                .secondaryColor(0xEE4400)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        VOIDIC_DRILLING_FLUID = new Material.Builder(PhoenixCore.id("voidic_drilling_fluid"))
                .fluid()
                .color(0x400080)
                .secondaryColor(0x7B68EE)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        CRYO_EMBER_FLUID = new Material.Builder(PhoenixCore.id("cryo_ember_fluid"))
                .liquid(6000)
                .color(0xFF4500)
                .secondaryColor(0xA020F0)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        AKASHIC_ZERONIUM = new Material.Builder(PhoenixCore.id("akashic_zeronium")).ingot().fluid().color(0x8F00FF)
                .cableProperties(GTValues.V[GTValues.MAX], 400000, 400000, true)
                .blastTemp(200000, HIGHEST, GTValues.VA[GTValues.MAX], 100000000).flags(GENERATE_PLATE, GENERATE_ROD)
                .buildAndRegister();
        PHOENIX_ENRICHED_NAQUADAH = new Material.Builder(PhoenixCore.id("phoenix_enriched_naquadah")).ingot()
                .color(0xFFA500).iconSet(MaterialIconSet.SHINY).cableProperties(GTValues.V[GTValues.ZPM], 64, 8, true)
                .blastTemp(1500, MID, GTValues.VA[GTValues.ZPM], 750000)
                .flags(GENERATE_FRAME, PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE).buildAndRegister();
        PHOENIX_ENRICHED_TRITANIUM = new Material.Builder(PhoenixCore.id("phoenix_enriched_tritanium")).ingot()
                .color(0xFF0000).iconSet(PhoenixMaterialSet.ALMOST_PURE_NEVONIAN_STEEL)
                .cableProperties(GTValues.V[GTValues.ZPM], 64, 8, true)
                .blastTemp(1800, MID, GTValues.VA[GTValues.ZPM], 900000)
                .flags(GENERATE_FRAME, PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE).buildAndRegister();
        AETHERIUM_STEEL = new Material.Builder(PhoenixCore.id("aetherium_steel")).ingot().fluid().color(0xADD8E6)
                .cableProperties(GTValues.V[GTValues.ZPM], 32, 4, true)
                .blastTemp(1300, MID, GTValues.VA[GTValues.ZPM], 600000).flags(GENERATE_PLATE).buildAndRegister();
        SUBSPACE_COBALT = new Material.Builder(PhoenixCore.id("subspace_cobalt")).ingot().fluid().color(0x00FFFF)
                .cableProperties(GTValues.V[GTValues.UV], 32, 4, true)
                .blastTemp(1600, MID, GTValues.VA[GTValues.UV], 700000).flags(GENERATE_PLATE).buildAndRegister();
        SINGULARITY_FORGED_TITANIUM = new Material.Builder(PhoenixCore.id("singularity_forged_titanium")).ingot()
                .fluid().color(0x808080).cableProperties(GTValues.V[GTValues.UV], 64, 8, true)
                .blastTemp(1700, MID, GTValues.VA[GTValues.UV], 850000).flags(GENERATE_PLATE).buildAndRegister();
        EXOTIC_VANADIUM_COMPOSITE = new Material.Builder(PhoenixCore.id("exotic_vanadium_composite")).ingot().fluid()
                .color(0xFFA07A).cableProperties(GTValues.V[GTValues.UV], 32, 4, true)
                .blastTemp(1400, MID, GTValues.VA[GTValues.UV], 780000).flags(GENERATE_PLATE).buildAndRegister();
        DARK_MATTER_PLATED_IRIDIUM = new Material.Builder(PhoenixCore.id("dark_matter_plated_iridium")).ingot().fluid()
                .color(0x191970).cableProperties(GTValues.V[GTValues.UV], 128, 16, true)
                .blastTemp(1900, MID, GTValues.VA[GTValues.UV], 950000).flags(GENERATE_PLATE).buildAndRegister();
        CORRUPTED_HYPERALLOY = new Material.Builder(PhoenixCore.id("corrupted_hyperalloy")).ingot().fluid()
                .color(0xFF00FF).cableProperties(GTValues.V[GTValues.UEV], 64, 8, true)
                .blastTemp(1650, MID, GTValues.VA[GTValues.UEV], 820000).flags(GENERATE_PLATE).buildAndRegister();
        REALITY_BOUND_OSMIUM = new Material.Builder(PhoenixCore.id("reality_bound_osmium")).ingot().fluid()
                .color(0x00FF00).cableProperties(GTValues.V[GTValues.UEV], 128, 16, true)
                .blastTemp(2000, MID, GTValues.VA[GTValues.UEV], 1050000).flags(GENERATE_PLATE).buildAndRegister();
        VOID_STITCHED_NEODYMIUM = new Material.Builder(PhoenixCore.id("void_stitched_neodymium")).ingot().fluid()
                .color(0x9400D3).cableProperties(GTValues.V[GTValues.UEV], 64, 8, true)
                .blastTemp(1550, MID, GTValues.VA[GTValues.UEV], 900000).flags(GENERATE_PLATE).buildAndRegister();
        CELESTIAL_AURORIUM = new Material.Builder(PhoenixCore.id("celestial_aurorium")).ingot().fluid().color(0xFFD700)
                .cableProperties(GTValues.V[GTValues.MAX], 4096, 512, true)
                .blastTemp(2650, MID, GTValues.VA[GTValues.MAX], 2100000).flags(GENERATE_PLATE).buildAndRegister();
        PRIMORDIAL_FLUX_METAL = new Material.Builder(PhoenixCore.id("primordial_flux_metal")).ingot().fluid()
                .color(0x8A2BE2).cableProperties(GTValues.V[GTValues.OpV], 2048, 256, true)
                .blastTemp(2500, MID, GTValues.VA[GTValues.OpV], 1950000).flags(GENERATE_PLATE).buildAndRegister();
        ETERNAL_STARFORGED_STEEL = new Material.Builder(PhoenixCore.id("eternal_starforged_steel")).ingot().fluid()
                .color(0x708090).cableProperties(GTValues.V[GTValues.UIV], 512, 64, true)
                .blastTemp(2150, MID, GTValues.VA[GTValues.UIV], 1600000).flags(GENERATE_PLATE).buildAndRegister();
        DIMENSIONAL_REFLECTION_ALLOY = new Material.Builder(PhoenixCore.id("dimensional_reflection_alloy")).ingot()
                .fluid().color(0x00CED1).cableProperties(GTValues.V[GTValues.UEV], 1024, 128, true)
                .blastTemp(2400, MID, GTValues.VA[GTValues.UEV], 1850000).flags(GENERATE_PLATE).buildAndRegister();
        TIMEWOVEN_PLATINUM = new Material.Builder(PhoenixCore.id("timewoven_platinum")).ingot().fluid().color(0xE5E4E2)
                .cableProperties(GTValues.V[GTValues.UXV], 2048, 256, true)
                .blastTemp(2550, MID, GTValues.VA[GTValues.UXV], 2000000).flags(GENERATE_PLATE).buildAndRegister();
        SOULBOUND_ETHERSTEEL = new Material.Builder(PhoenixCore.id("soulbound_ethersteel")).ingot().fluid()
                .color(0x87CEEB).cableProperties(GTValues.V[GTValues.UIV], 512, 64, true)
                .blastTemp(2000, MID, GTValues.VA[GTValues.UIV], 1500000).flags(GENERATE_PLATE).buildAndRegister();
        TACHYON_INFUSED_CHROMIUM = new Material.Builder(PhoenixCore.id("tachyon_infused_chromium")).ingot().fluid()
                .color(0xB0C4DE).cableProperties(GTValues.V[GTValues.OpV], 1024, 128, true)
                .blastTemp(2450, MID, GTValues.VA[GTValues.OpV], 1750000).flags(GENERATE_PLATE).buildAndRegister();
        ECHO_CRYSTAL_ALLOY = new Material.Builder(PhoenixCore.id("echo_crystal_alloy")).ingot().fluid().color(0xDA70D6)
                .cableProperties(GTValues.V[GTValues.OpV], 4096, 512, true)
                .blastTemp(2750, MID, GTValues.VA[GTValues.OpV], 2250000).flags(GENERATE_PLATE).buildAndRegister();
        NEBULAR_RESONANCE_INGOT = new Material.Builder(PhoenixCore.id("nebular_resonance_ingot")).ingot().fluid()
                .color(0x4682B4).cableProperties(GTValues.V[GTValues.MAX], 8192, 1024, true)
                .blastTemp(2850, MID, GTValues.VA[GTValues.MAX], 2400000).flags(GENERATE_PLATE).buildAndRegister();
        PARADOXIUM = new Material.Builder(PhoenixCore.id("paradoxium")).ingot().fluid().color(0x000000)
                .cableProperties(GTValues.V[GTValues.MAX], 16384, 2048, true)
                .blastTemp(3000, MID, GTValues.VA[GTValues.MAX], 3000000).flags(GENERATE_PLATE).buildAndRegister();
        PHOENIX_TEMPERED_MITHRIL = new Material.Builder(PhoenixCore.id("phoenix_tempered_mithril")).ingot().fluid()
                .color(0xE0FFFF).cableProperties(GTValues.V[GTValues.UIV], 128, 16, true)
                .blastTemp(2100, MID, GTValues.VA[GTValues.UIV], 1150000).flags(GENERATE_PLATE).buildAndRegister();
        VOID_SUNG_ADAMANTITE = new Material.Builder(PhoenixCore.id("void_sung_adamantite")).ingot().fluid()
                .color(0x2F4F4F).cableProperties(GTValues.V[GTValues.UIV], 256, 32, true)
                .blastTemp(1850, MID, GTValues.VA[GTValues.UIV], 1200000).flags(GENERATE_PLATE).buildAndRegister();
        ENTANGLED_PALLADIUM = new Material.Builder(PhoenixCore.id("entangled_palladium")).ingot().fluid()
                .color(0xF0E68C).cableProperties(GTValues.V[GTValues.UIV], 128, 16, true)
                .blastTemp(1950, MID, GTValues.VA[GTValues.UIV], 1000000).flags(GENERATE_PLATE).buildAndRegister();
        ENTANGLED_NEUTRON_ALLOY = new Material.Builder(PhoenixCore.id("entangled_neutron_alloy")).ingot().fluid()
                .color(0x778899).cableProperties(GTValues.V[GTValues.UXV], 256, 32, true)
                .blastTemp(2300, MID, GTValues.VA[GTValues.UXV], 1300000).flags(GENERATE_PLATE).buildAndRegister();
        SUPERPOSITION_TUNGSTEN_MATRIX = new Material.Builder(PhoenixCore.id("superposition_tungsten_matrix")).ingot()
                .fluid().color(0xD3D3D3).cableProperties(GTValues.V[GTValues.UXV], 512, 64, true)
                .blastTemp(2050, MID, GTValues.VA[GTValues.UXV], 1400000).flags(GENERATE_PLATE).buildAndRegister();
        EXO_TEMPORAL_ORICHALCUM = new Material.Builder(PhoenixCore.id("exo_temporal_orichalcum")).ingot().fluid()
                .color(0xFFD700).cableProperties(GTValues.V[GTValues.UXV], 256, 32, true)
                .blastTemp(2250, MID, GTValues.VA[GTValues.UXV], 1250000).flags(GENERATE_PLATE).buildAndRegister();
        GALACTIC_HEART_MATTER = new Material.Builder(PhoenixCore.id("galactic_heart_matter")).ingot().fluid()
                .color(0xF08080).cableProperties(GTValues.V[GTValues.OpV], 256, 32, true)
                .blastTemp(2200, MID, GTValues.VA[GTValues.OpV], 1550000).flags(GENERATE_PLATE).buildAndRegister();
        DARK_NEBULA_INFUSED_IRIDIUM = new Material.Builder(PhoenixCore.id("dark_nebula_infused_iridium")).ingot()
                .fluid().color(0x483D8B).cableProperties(GTValues.V[GTValues.OpV], 1024, 128, true)
                .blastTemp(2150, MID, GTValues.VA[GTValues.OpV], 1600000).flags(GENERATE_PLATE).buildAndRegister();
        ELDRITCH_VOIDSTEEL = new Material.Builder(PhoenixCore.id("eldritch_voidsteel")).ingot().fluid().color(0x000000)
                .cableProperties(GTValues.V[GTValues.OpV], 512, 64, true)
                .blastTemp(2350, MID, GTValues.VA[GTValues.OpV], 1700000).flags(GENERATE_PLATE).buildAndRegister();
        AKASHIC_CHRONO_METAL = new Material.Builder(PhoenixCore.id("akashic_chrono_metal")).ingot().fluid()
                .color(0xF5F5DC).cableProperties(GTValues.V[GTValues.MAX], 1024, 128, true)
                .blastTemp(2500, MID, GTValues.VA[GTValues.MAX], 1800000).flags(GENERATE_PLATE).buildAndRegister();
        ABSOLUTE_ZERO_ZERONIUM = new Material.Builder(PhoenixCore.id("absolute_zero_zeronium")).ingot().fluid()
                .color(0x00FFFF).cableProperties(GTValues.V[GTValues.MAX], 2048, 256, true)
                .blastTemp(2200, MID, GTValues.VA[GTValues.MAX], 1900000).flags(GENERATE_PLATE).buildAndRegister();
        MULTIVERSAL_HYPERALLOY = new Material.Builder(PhoenixCore.id("multiversal_hyperalloy")).ingot().fluid()
                .color(0xFFFAFA).cableProperties(GTValues.V[GTValues.MAX], 1024, 128, true)
                .blastTemp(2600, MID, GTValues.VA[GTValues.MAX], 2000000).flags(GENERATE_PLATE).buildAndRegister();
        GLITCHED_CORRUPTION_SUBSTRATE = new Material.Builder(PhoenixCore.id("glitched_corruption_substrate")).ingot()
                .fluid().color(0x800080).cableProperties(GTValues.V[GTValues.UEV], 32, 4, true)
                .blastTemp(1450, MID, GTValues.VA[GTValues.UEV], 880000).flags(GENERATE_PLATE).buildAndRegister();
        EVENT_HORIZON_MATTER = new Material.Builder(PhoenixCore.id("event_horizon_matter")).ingot().fluid()
                .color(0x000000).cableProperties(GTValues.V[GTValues.MAX], 4096, 512, true)
                .blastTemp(2700, MID, GTValues.VA[GTValues.MAX], 2200000).flags(GENERATE_PLATE).buildAndRegister();
        ANTIMATTER = new Material.Builder(PhoenixCore.id("antimatter")).ingot().fluid().color(0x000000)
                .cableProperties(GTValues.V[GTValues.MAX], 8192, 1024, true)
                .blastTemp(2800, MID, GTValues.VA[GTValues.MAX], 2500000).flags(GENERATE_PLATE).buildAndRegister();

        NAQUADATITE_ACIDIC = new Material.Builder(PhoenixCore.id("naquadatite_acidic"))
                .dust()
                .color(0x3B5A40)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        HEAVILY_FLUORINE_CONTAMINATED_NAQUADATITE = new Material.Builder(
                PhoenixCore.id("heavily_fluorine_contaminated_naquadatite"))
                .dust()
                .color(0x4E7C59)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        RADIOACTIVE_AND_HEAVY_METAL_CONTAMINATED_NAQUADATITE = new Material.Builder(
                PhoenixCore.id("radioactive_and_heavy_metal_contaminated_naquadatite"))
                .dust()
                .color(0x2B3A2C)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        RADIOACTIVE_CONTAMINATED_NAQUADATITE = new Material.Builder(
                PhoenixCore.id("radioactive_contaminated_naquadatite"))
                .dust()
                .color(0x334D37)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        RADIOACTIVE_NAQUADACID = new Material.Builder(PhoenixCore.id("radioactive_naquadacid"))
                .dust()
                .color(0x1E4620)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        NAQUADAH_DIOXIDE = new Material.Builder(PhoenixCore.id("naquadah_dioxide"))
                .dust()
                .color(0x222222)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        LIGHTLY_CHLORINE_CONTAMINATED_WASTE = new Material.Builder(
                PhoenixCore.id("lightly_chlorine_contaminated_waste"))
                .dust()
                .color(0x8FBC8F)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        CALCIUM_FLUORINE = new Material.Builder(PhoenixCore.id("calcium_fluorine"))
                .dust()
                .color(0xE0EEEE)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ALUMINIUM_CAESIUM_CHLORIDE = new Material.Builder(PhoenixCore.id("aluminium_caesium_chloride"))
                .dust()
                .color(0x778899)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ASTATINE = new Material.Builder(PhoenixCore.id("astatine"))
                .dust()
                .color(0xBF00FF)
                .secondaryColor(0x41006F)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        NAQUADATIA_ASTATINE = new Material.Builder(PhoenixCore.id("naquadatia_astatine"))
                .dust()
                .color(0x5B1382)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        SODIUM_ASTATINE_BISULFATE = new Material.Builder(PhoenixCore.id("sodium_astatine_bisulfate"))
                .dust()
                .color(0x8A2BE2)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        HEAVY_METAL_SLUDGE = new Material.Builder(PhoenixCore.id("heavy_metal_sludge"))
                .dust()
                .liquid()
                .color(0x2F4F4F)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ACIDIC_WASTE_SLUDGE = new Material.Builder(PhoenixCore.id("acidic_waste_sludge"))
                .dust()
                .liquid()
                .color(0x4B5320)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        HEAVILY_ACIDIC_NAQUADATITE = new Material.Builder(PhoenixCore.id("heavily_acidic_naquadatite"))
                .dust()
                .color(0x1B4D3E)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        CHLORINE_WASTE_NAQUADATIA_ASTATINE = new Material.Builder(PhoenixCore.id("chlorine_waste_naquadatia_astatine"))
                .dust()
                .color(0x3D6346)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        GRAPHENE_SLUDGE = new Material.Builder(PhoenixCore.id("graphene_sludge"))
                .dust()
                .liquid()
                .color(0x1C1C1C)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
    }
}
