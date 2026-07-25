package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.phoenix.core.api.item.tool.PhoenixToolType;
import net.phoenix.core.common.data.item.PhoenixItems;

import com.tterrag.registrate.util.entry.ItemEntry;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

public class PhoenixToolRecipeHelper {

    public static final Int2ReferenceMap<ItemEntry<? extends Item>> powerUnitItems = new Int2ReferenceArrayMap<>(
            GTValues.tiersBetween(GTValues.LV, GTValues.ZPM),
            new ItemEntry[] { GTItems.POWER_UNIT_LV, GTItems.POWER_UNIT_MV, GTItems.POWER_UNIT_HV,
                    GTItems.POWER_UNIT_EV, GTItems.POWER_UNIT_IV, PhoenixItems.POWER_UNIT_LUV,
                    PhoenixItems.POWER_UNIT_ZPM });

    private PhoenixToolRecipeHelper() {}

    public static void run(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material) {
        ToolProperty property = material.getProperty(PropertyKey.TOOL);
        if (property == null) {
            return;
        }

        processElectricTool(provider, property, material);
    }

    private static void processElectricTool(@NotNull Consumer<FinishedRecipe> provider, @NotNull ToolProperty property,
                                            @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(plate)) {
            return;
        }

        final int voltageMultiplier = material.getBlastTemperature() > 2800 ? GTValues.VA[GTValues.LV] :
                GTValues.VA[GTValues.ULV];
        TagPrefix toolPrefix;

        if (material.hasFlag(GENERATE_PLATE)) {
            final MaterialEntry plate = new MaterialEntry(TagPrefix.plate, material);
            final MaterialEntry steelPlate = new MaterialEntry(TagPrefix.plate, GTMaterials.Steel);
            final MaterialEntry steelRing = new MaterialEntry(TagPrefix.ring, GTMaterials.Steel);

            if (property.hasType(GTToolType.CHAINSAW_LV)) {
                toolPrefix = TagPrefix.toolHeadChainsaw;
                VanillaRecipeHelper.addShapedRecipe(provider, String.format("chainsaw_head_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "SRS", "XhX", "SRS",
                        'X', plate,
                        'S', steelPlate,
                        'R', steelRing);

                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.CHAINSAW_MV,
                                PhoenixToolType.CHAINSAW_EV, PhoenixToolType.CHAINSAW_LuV,
                                PhoenixToolType.CHAINSAW_ZPM, },
                        material);
            }

            if (property.hasType(GTToolType.DRILL_LV)) {
                toolPrefix = TagPrefix.toolHeadDrill;
                VanillaRecipeHelper.addShapedRecipe(provider, String.format("drill_head_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "XSX", "XSX", "ShS",
                        'X', plate,
                        'S', steelPlate);

                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.DRILL_LUV, PhoenixToolType.DRILL_ZPM }, material);
            }

            if (property.hasType(GTToolType.WIRE_CUTTER_LV)) {
                toolPrefix = toolHeadWireCutter;
                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.WIRE_CUTTER_MV, PhoenixToolType.WIRE_CUTTER_EV,
                                PhoenixToolType.WIRE_CUTTER_LuV, PhoenixToolType.WIRE_CUTTER_ZPM },
                        material);

                VanillaRecipeHelper.addShapedRecipe(provider, String.format("wirecutter_head_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "XfX", "X X", "SRS",
                        'X', plate,
                        'R', steelRing,
                        'S', new MaterialEntry(screw, GTMaterials.Steel));
            }

            if (property.hasType(GTToolType.BUZZSAW)) {
                toolPrefix = TagPrefix.toolHeadBuzzSaw;
                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.BUZZSAW_MV, PhoenixToolType.BUZZSAW_HV,
                                PhoenixToolType.BUZZSAW_EV, PhoenixToolType.BUZZSAW_IV, PhoenixToolType.BUZZSAW_LuV,
                                PhoenixToolType.BUZZSAW_ZPM, },
                        material);

                VanillaRecipeHelper.addShapedRecipe(provider, String.format("buzzsaw_blade_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "sXh", "X X", "fXx",
                        'X', plate);
            }

            if (property.hasType(GTToolType.WRENCH_LV)) {
                toolPrefix = TagPrefix.toolHeadWrench;
                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.WRENCH_MV, PhoenixToolType.WRENCH_EV,
                                PhoenixToolType.WRENCH_LuV, PhoenixToolType.WRENCH_ZPM },
                        material);

                VanillaRecipeHelper.addShapedRecipe(provider, String.format("wrench_head_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "hXW", "XRX", "WXd",
                        'X', plate,
                        'R', steelRing,
                        'W', new MaterialEntry(TagPrefix.screw, GTMaterials.Steel));
            }

        }

        if (property.hasType(GTToolType.SCREWDRIVER_LV)) {

            if (material.hasFlag(GENERATE_LONG_ROD)) {
                toolPrefix = TagPrefix.toolHeadScrewdriver;
                addElectricToolRecipe(provider, toolPrefix,
                        new GTToolType[] { PhoenixToolType.SCREWDRIVER_MV,
                                PhoenixToolType.SCREWDRIVER_EV,
                                PhoenixToolType.SCREWDRIVER_LuV, PhoenixToolType.SCREWDRIVER_ZPM, },
                        material);

                VanillaRecipeHelper.addShapedRecipe(provider, String.format("screwdriver_tip_%s", material.getName()),
                        ChemicalHelper.get(toolPrefix, material),
                        "fR", " h",
                        'R', new MaterialEntry(TagPrefix.rodLong, material));
            }

        }
    }

    private static void addElectricToolRecipe(@NotNull Consumer<FinishedRecipe> provider, @NotNull TagPrefix toolHead,
                                              @NotNull GTToolType @NotNull [] toolItems,
                                              @NotNull Material material) {
        for (GTToolType toolType : toolItems) {
            if (!material.getProperty(PropertyKey.TOOL).hasType(toolType)) continue;

            int tier = toolType.electricTier;
            ItemStack powerUnitStack = powerUnitItems.get(tier).asStack();
            IElectricItem powerUnit = GTCapabilityHelper.getElectricItem(powerUnitStack);
            ItemStack tool = GTMaterialItems.TOOL_ITEMS.get(material, toolType).get().get(0, powerUnit.getMaxCharge());
            VanillaRecipeHelper.addShapedEnergyTransferRecipe(provider,
                    true, true, true,
                    String.format("%s_%s", material.getName(), toolType.name),
                    Ingredient.of(powerUnitStack),
                    tool,
                    "wHd", " U ",
                    'H', new MaterialEntry(toolHead, material),
                    'U', powerUnitStack);
        }
    }
}
