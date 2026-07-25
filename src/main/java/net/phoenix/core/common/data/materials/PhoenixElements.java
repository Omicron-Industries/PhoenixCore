package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;

public class PhoenixElements {

    public static Element URANIUM_233;
    public static Element URANIUM_236;
    public static Element AMERICIUM_241;

    public static Element PHOENIX_ENRICHED_TRITANIUM;
    public static Element EMBER;
    public static Element PHOENIX_ENRICHED_NAQUADAH;
    public static Element AKASHIC_ZERONIUM;
    public static Element AETHERIUM_STEEL;
    public static Element VOID_TOUCHED_TUNGSTEN_STEEL;
    public static Element CELESTIAL_AURORIUM;
    public static Element PRIMORDIAL_FLUX_METAL;
    public static Element ETERNAL_STARFORGED_STEEL;
    public static Element DIMENSIONAL_REFLECTION_ALLOY;
    public static Element TIMEWOVEN_PLATINUM;
    public static Element SOULBOUND_ETHERSTEEL;
    public static Element TACHYON_INFUSED_CHROMIUM;
    public static Element ECHO_CRYSTAL_ALLOY;
    public static Element NEBULAR_RESONANCE_INGOT;
    public static Element PARADOXIUM;
    public static Element SUBSPACE_COBALT;
    public static Element SINGULARITY_FORGED_TITANIUM;
    public static Element EXOTIC_VANADIUM_COMPOSITE;
    public static Element DARK_MATTER_PLATED_IRIDIUM;
    public static Element CORRUPTED_HYPERALLOY;
    public static Element REALITY_BOUND_OSMIUM;
    public static Element VOID_STITCHED_NEODYMIUM;
    public static Element PHOENIX_TEMPERED_MITHRIL;
    public static Element VOID_SUNG_ADAMANTITE;
    public static Element ENTANGLED_PALLADIUM;
    public static Element ENTANGLED_NEUTRON_ALLOY;
    public static Element SUPERPOSITION_TUNGSTEN_MATRIX;
    public static Element EXO_TEMPORAL_ORICHALCUM;
    public static Element INFINITY;
    public static Element GALAXIUM_CORE_ALLOY;
    public static Element DARK_NEBULA_INFUSED_IRIDIUM;
    public static Element ELDRITCH_VOIDSTEEL;
    public static Element AKASHIC_CHRONO_METAL;
    public static Element ABSOLUTE_ZERO_ZERONIUM;
    public static Element MULTIVERSAL_HYPERALLOY;
    public static Element GLITCHED_CORRUPTION_SUBSTRATE;
    public static Element GALACTIC_HEART_MATTER;
    public static Element EVENT_HORIZON_MATTER;
    public static Element ANTIMATTER;
    public static Element ZIRCALLOY;
    public static Element SOURCE_IMBUED_TITANIUM;
    public static Element ICY_STEEL_MATRIX;

    public static void init() {
        URANIUM_233 = create("uranium_233", 92L, 141L, -1L, null, "Uranium-233", "U²²³", true);
        URANIUM_236 = create("uranium_236", 92L, 144L, -1L, null, "Uranium-236", "U²³⁶", true);
        AMERICIUM_241 = create("americium_241", 95L, 146L, -1L, null, "Americium-241", "Am-241", true);

        PHOENIX_ENRICHED_TRITANIUM = create("phoenix_enriched_tritanium", 1, 32, "PET");
        EMBER = create("ember", 1, 2, "🔥");
        PHOENIX_ENRICHED_NAQUADAH = create("phoenix_enriched_naquadah", 25, 32, "PENaq");
        AKASHIC_ZERONIUM = create("akashic_zeronium", 24, 12, "ASHK");
        AETHERIUM_STEEL = create("aetherium_steel", 26, 30, "AES");
        VOID_TOUCHED_TUNGSTEN_STEEL = create("void_touched_tungsten_steel", 74, 110, "VTT");

        CELESTIAL_AURORIUM = create("celestial_aurorium", -1, -1, "CAu");
        PRIMORDIAL_FLUX_METAL = create("primordial_flux_metal", -1, -1, "PFM");
        ETERNAL_STARFORGED_STEEL = create("eternal_starforged_steel", -1, -1, "ESS");
        DIMENSIONAL_REFLECTION_ALLOY = create("dimensional_reflection_alloy", -1, -1, "DRA");
        TIMEWOVEN_PLATINUM = create("timewoven_platinum", -1, -1, "TWPt");
        SOULBOUND_ETHERSTEEL = create("soulbound_ethersteel", -1, -1, "SEth");
        TACHYON_INFUSED_CHROMIUM = create("tachyon_infused_chromium", -1, -1, "TiCr");
        ECHO_CRYSTAL_ALLOY = create("echo_crystal_alloy", -1, -1, "ECA");
        NEBULAR_RESONANCE_INGOT = create("nebular_resonance_ingot", -1, -1, "NRI");
        PARADOXIUM = create("paradoxium", -1, -1, "Px");

        SUBSPACE_COBALT = create("subspace_cobalt", 27, 33, "QIC");
        SINGULARITY_FORGED_TITANIUM = create("singularity_forged_titanium", 22, 26, "SFTi");
        EXOTIC_VANADIUM_COMPOSITE = create("exotic_vanadium_composite", 23, 28, "EVC");
        DARK_MATTER_PLATED_IRIDIUM = create("dark_matter_plated_iridium", 77, 116, "DMPIr");

        CORRUPTED_HYPERALLOY = create("corrupted_hyperalloy", -1, -1, "CHA");
        REALITY_BOUND_OSMIUM = create("reality_bound_osmium", 76, 114, "RBOs");
        VOID_STITCHED_NEODYMIUM = create("void_stitched_neodymium", 60, 84, "VSNd");
        PHOENIX_TEMPERED_MITHRIL = create("phoenix_tempered_mithril", -1, -1, "PTMi");
        VOID_SUNG_ADAMANTITE = create("void_sung_adamantite", -1, -1, "VSA");

        ENTANGLED_PALLADIUM = create("entangled_palladium", 46, 60, "QLPd");
        ENTANGLED_NEUTRON_ALLOY = create("entangled_neutron_alloy", -1, -1, "ENeA");
        SUPERPOSITION_TUNGSTEN_MATRIX = create("superposition_tungsten_matrix", -1, -1, "SWM");
        EXO_TEMPORAL_ORICHALCUM = create("exo_temporal_orichalcum", -1, -1, "ETO");

        INFINITY = create("infinity", -1, -1, "∞");
        GALAXIUM_CORE_ALLOY = create("galaxium_core_alloy", -1, -1, "GCA");
        DARK_NEBULA_INFUSED_IRIDIUM = create("dark_nebula_infused_iridium", -1, -1, "DNIIr");
        ELDRITCH_VOIDSTEEL = create("eldritch_voidsteel", -1, -1, "EVSt");
        AKASHIC_CHRONO_METAL = create("akashic_chrono_metal", -1, -1, "ACM");
        ABSOLUTE_ZERO_ZERONIUM = create("absolute_zero_zeronium", -1, -1, "AZZr");
        MULTIVERSAL_HYPERALLOY = create("multiversal_hyperalloy", -1, -1, "MHA");
        GLITCHED_CORRUPTION_SUBSTRATE = create("glitched_corruption_substrate", -1, -1, "GCS");
        GALACTIC_HEART_MATTER = create("galactic_heart_matter", -1, -1, "GHM");
        EVENT_HORIZON_MATTER = create("event_horizon_matter", -1, -1, "EHM");
        ANTIMATTER = create("antimatter", -1, -1, "aM");

        ZIRCALLOY = create("zircalloy", 77, 125, "Zr⁷BiHf³");
        SOURCE_IMBUED_TITANIUM = create("source_imbued_titanium", 10, 118, "✨C✨Ti");
        ICY_STEEL_MATRIX = create("icy_steel_matrix", 8, 118, "❆Is<>");
    }

    private static Element create(String name, int protons, int neutrons, String symbol) {
        return create(name, (long) protons, (long) neutrons, -1L, (String) null, name, symbol, false);
    }

    private static Element create(String id, long protons, long neutrons, long halfLife, String decayTo, String name,
                                  String symbol, boolean isIsotope) {
        Element element = new Element(protons, neutrons, halfLife, decayTo, name, symbol, isIsotope);

        ResourceLocation registryId = new ResourceLocation("phoenixcore", id);

        GTRegistries.ELEMENTS.register(registryId, element);

        return element;
    }
}
