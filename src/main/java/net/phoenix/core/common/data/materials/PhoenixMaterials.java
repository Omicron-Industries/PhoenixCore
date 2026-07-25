package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.*;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.api.item.tool.PhoenixToolType;
import net.phoenix.core.common.data.recipe.generated.BeePrefixHelper;
import net.phoenix.core.common.data.recipe.generated.CrystalRoseHelper;

import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static net.phoenix.core.common.data.materials.PhoenixOres.*;
import static net.phoenix.core.common.data.materials.PhoenixOres.NAQUADATITE;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.*;

public class PhoenixMaterials {

    public static void register() {
        GTMaterials.Francium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Technetium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Radium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Actinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Polonium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Protactinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Neptunium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Curium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Berkelium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Californium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Einsteinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Fermium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Mendelevium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Nobelium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Lawrencium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Strontium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Zirconium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Hafnium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Hafnium.setProperty(PropertyKey.ORE, new OreProperty());
        GTMaterials.Zirconium.setProperty(PropertyKey.ORE, new OreProperty());

        addFluid(GTMaterials.Iodine, FluidStorageKeys.GAS);
        addFluid(GTMaterials.Oganesson, FluidStorageKeys.GAS);
    }

    public static void addFluid(Material m, FluidStorageKey key) {
        FluidProperty prop = new FluidProperty();

        prop.getStorage().enqueueRegistration(key, new FluidBuilder());

        m.setProperty(PropertyKey.FLUID, prop);
    }

    @NotNull
    public static Material get(String name) {
        Material mat = GTRegistries.MATERIALS.get(new ResourceLocation("phoenixcore", name));

        if (mat == null) {
            PhoenixAPI.LOGGER.warn("{} is not a known Material", name);
            return GTMaterials.get(null);
        }
        return mat;
    }

    public static void modifyMaterials() {
        CrystalRoseHelper.addCrystalRoseFlags(
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite, PERMAFROST, DORMANT_EMBER, NAQUADATITE,
                NetherQuartz, RockSalt, Sodalite,
                Coal, Redstone, Tin, Obsidian, Netherite, CertusQuartz, NetherQuartz, VOIDGLASS_SHARD, Saltpeter,
                PhoenixOres.FLUORITE, PhoenixProgressionMaterials.SOURCE_GEM, Glowstone, Ice, IGNISIUM,
                RESONANT_ENDER, FLUIX, SPONGE, Sculk, SLIME, MAGMA, Blaze, Salt, Bone, ZOMBIE, WITHERED, GHOSTLY, SILKY,
                PRISMARINE, Goethite,
                Magnetite, Vanadium,
                Bentonite, Olivine,
                Almandine, Spessartine, Grossular,
                Chalcopyrite, Pyrite,
                Cassiterite, CassiteriteSand,
                Monazite, Neodymium,
                Bornite, Chalcocite,
                Pentlandite,
                Lazurite,
                Grossular,
                Mica, Pollucite,

                Naquadah, NaquadahEnriched,
                Beryllium,
                Garnierite,
                Cooperite, Palladium,
                Tungsten);

        BeePrefixHelper.addBeeCombFlag(
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,
                NetherQuartz, RockSalt, Sodalite,
                Coal, Redstone, Tin, Obsidian, Netherite, CertusQuartz, NetherQuartz, VOIDGLASS_SHARD, Saltpeter,
                PhoenixOres.FLUORITE, PhoenixProgressionMaterials.SOURCE_GEM, Glowstone, Ice, PhoenixOres.IGNISIUM,
                RESONANT_ENDER, FLUIX, SPONGE, Sculk, SLIME, MAGMA, Blaze, Salt, Bone, ZOMBIE, WITHERED, GHOSTLY, SILKY,
                PRISMARINE);

        BeePrefixHelper.addTierOneBeeFlag(
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,
                NetherQuartz, RockSalt, Sodalite,
                Coal, Redstone, Tin, Obsidian, Netherite, CertusQuartz, NetherQuartz, VOIDGLASS_SHARD, Saltpeter,
                PhoenixOres.FLUORITE, PhoenixProgressionMaterials.SOURCE_GEM, Glowstone, Ice, PhoenixOres.IGNISIUM,
                RESONANT_ENDER, FLUIX, SPONGE, Sculk, SLIME, MAGMA, Blaze, Salt, Bone, ZOMBIE, WITHERED, GHOSTLY, SILKY,
                PRISMARINE);

        for (Material material : GTRegistries.MATERIALS.values()) {
            ToolProperty toolProperty = material.getProperty(PropertyKey.TOOL);

            if (toolProperty != null && toolProperty.hasType(GTToolType.SCREWDRIVER_LV)) {
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_MV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_EV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_LuV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.BUZZSAW)) {
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_MV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_HV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_EV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_IV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_LuV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.CHAINSAW_LV)) {
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_MV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_EV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_LuV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.WIRE_CUTTER_LV)) {
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_MV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_EV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_LuV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.WRENCH_LV)) {
                toolProperty.addTypes(PhoenixToolType.WRENCH_MV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_EV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_LuV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.DRILL_LV)) {
                toolProperty.addTypes(PhoenixToolType.DRILL_LUV, PhoenixToolType.DRILL_ZPM);
            }
        }
    }
}
