package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.phoenix.core.PhoenixCore;

public class PhoenixOres {

    public static Material NEVVONIAN_IRON;
    public static Material FLUORITE;
    public static Material IGNISIUM;
    public static Material POLARITY_FLIPPED_BISMUTHITE;
    public static Material VOIDGLASS_SHARD;
    public static Material CRYSTALLIZED_FLUXSTONE;
    public static Material PERMAFROST;

    public static Material QUANTARIUMITE;
    public static Material PHOENIXITE;
    public static Material AKASHIC_BISMUTHITE;
    public static Material STARNITE;
    public static Material ENTROPY_RICH_ORE;
    public static Material FRACTALINE_CORE;
    public static Material VOLTANITE;
    public static Material QUANTUM_ENTROPITE;
    public static Material MYCELIAL_NETHERITE;
    public static Material GLACIAL_CRYONITE;
    public static Material CHRONOTON;
    public static Material SINGULIUM_CLUSTER;
    public static Material IRREALITY_SHARD;
    public static Material DIMENSIUM_CRYSTAL_LATTICE;
    public static Material EXOTIC_HADRONITE;
    public static Material AKASHIC_RESONANCE_CRYSTAL;
    public static Material FINALITY_SHARD;
    public static Material OBLIVIUM_CLUSTER;
    public static Material DORMANT_EMBER;
    public static Material OSMIRIDIUM_80_20;
    public static Material ISMIRIDIUM_80_20;

    public static void register() {
        NEVVONIAN_IRON = new Material.Builder(PhoenixCore.id("nevvonian_iron"))
                .ingot()
                .ore()
                .color(0x7A687F)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        VOIDGLASS_SHARD = new Material.Builder(PhoenixCore.id("voidglass_shard"))
                .gem()
                .ore()
                .color(0x6A00AA)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        POLARITY_FLIPPED_BISMUTHITE = new Material.Builder(PhoenixCore.id("polarity_flipped_bismuthite"))
                .dust()
                .ore()
                .color(0xE4D6FF)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        PERMAFROST = new Material.Builder(PhoenixCore.id("permafrost"))
                .dust()
                .ore()
                .color(0xA7D1EB)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        FLUORITE = new Material.Builder(PhoenixCore.id("fluorite"))
                .gem()
                .ore()
                .color(0x0C9949)
                .iconSet(MaterialIconSet.DIAMOND)
                .components(GTMaterials.Fluorine, 2, GTMaterials.Calcium, 1)
                .buildAndRegister();

        IGNISIUM = new Material.Builder(PhoenixCore.id("ignisium"))
                .dust()
                .ore()
                .color(0xFF4500)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        CRYSTALLIZED_FLUXSTONE = new Material.Builder(PhoenixCore.id("crystallized_fluxstone"))
                .dust()
                .ore()
                .color(0xD4BFFF)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        QUANTARIUMITE = new Material.Builder(PhoenixCore.id("quantariumite"))
                .dust().ore().color(0x7E00FF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        PHOENIXITE = new Material.Builder(PhoenixCore.id("phoenixite"))
                .dust().ore().color(0xFF4500).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        AKASHIC_BISMUTHITE = new Material.Builder(PhoenixCore.id("akashic_bismuthite"))
                .dust().ore().color(0xFBF8FF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        STARNITE = new Material.Builder(PhoenixCore.id("starnite"))
                .gem().ore().color(0xFFCCFF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        ENTROPY_RICH_ORE = new Material.Builder(PhoenixCore.id("entropy_rich_ore"))
                .dust().ore().color(0xDE00E0).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        FRACTALINE_CORE = new Material.Builder(PhoenixCore.id("fractaline_core"))
                .gem().ore().color(0xFF0080).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        VOLTANITE = new Material.Builder(PhoenixCore.id("voltanite_ore"))
                .dust().ore().color(0x00CFFF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        QUANTUM_ENTROPITE = new Material.Builder(PhoenixCore.id("quantum_entropite"))
                .dust().ore().color(0xC710FF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        MYCELIAL_NETHERITE = new Material.Builder(PhoenixCore.id("mycelial_netherite_ore"))
                .dust().ore().color(0x643A3A).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        GLACIAL_CRYONITE = new Material.Builder(PhoenixCore.id("glacial_cryonite_ore"))
                .gem().ore().color(0xADD8E6).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        CHRONOTON = new Material.Builder(PhoenixCore.id("chronoton_ore"))
                .dust().ore().color(0x77E3FF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        SINGULIUM_CLUSTER = new Material.Builder(PhoenixCore.id("singulium_cluster"))
                .gem().ore().color(0xBFFFFF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        IRREALITY_SHARD = new Material.Builder(PhoenixCore.id("irreality_shard"))
                .dust().ore().color(0x8888FF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        DIMENSIUM_CRYSTAL_LATTICE = new Material.Builder(PhoenixCore.id("dimensium_crystal_lattice"))
                .gem().ore().color(0xFFEEDD).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        EXOTIC_HADRONITE = new Material.Builder(PhoenixCore.id("exotic_hadronite"))
                .dust().ore().color(0xFF55CC).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        AKASHIC_RESONANCE_CRYSTAL = new Material.Builder(PhoenixCore.id("akashic_resonance_crystal"))
                .gem().ore().color(0xFFFFFF).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        FINALITY_SHARD = new Material.Builder(PhoenixCore.id("finality_shard"))
                .gem().ore().color(0xF6F6F6).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        OBLIVIUM_CLUSTER = new Material.Builder(PhoenixCore.id("oblivium_cluster"))
                .gem().ore().color(0x111111).iconSet(MaterialIconSet.DIAMOND).buildAndRegister();

        DORMANT_EMBER = new Material.Builder(PhoenixCore.id("dormant_ember"))
                .ingot().ore().color(0x704214).iconSet(MaterialIconSet.SHINY)
                .addOreByproducts(GTMaterials.Iron)
                .element(PhoenixElements.EMBER)
                .buildAndRegister();

        OSMIRIDIUM_80_20 = new Material.Builder(PhoenixCore.id("osmiridium_80_20"))
                .dust().ore().color(0xD3D3D3).iconSet(MaterialIconSet.SHINY)
                .components(GTMaterials.Osmium, 4, GTMaterials.Iridium, 1)
                .buildAndRegister();

        ISMIRIDIUM_80_20 = new Material.Builder(PhoenixCore.id("ismiridium_80_20"))
                .dust().ore().color(0xE0E0E0).iconSet(MaterialIconSet.SHINY)
                .components(GTMaterials.Iridium, 4, GTMaterials.Osmium, 1)
                .buildAndRegister();
    }
}
