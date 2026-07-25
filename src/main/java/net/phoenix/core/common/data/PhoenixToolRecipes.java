package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import com.gregtechceu.gtceu.utils.ToolItemHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.phoenix.core.common.data.item.PhoenixItems;

import com.tterrag.registrate.util.entry.ItemEntry;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static net.phoenix.core.common.data.recipe.generated.PhoenixToolRecipeHelper.powerUnitItems;

public final class PhoenixToolRecipes {

    private PhoenixToolRecipes() {}

    private static final Int2ReferenceMap<ItemEntry<? extends Item>> motorItems = new Int2ReferenceArrayMap<>();
    private static final Int2ReferenceMap<Material> baseMaterials = new Int2ReferenceArrayMap<>();
    private static final Int2ReferenceMap<List<ItemEntry<? extends Item>>> batteryItems = new Int2ReferenceArrayMap<>();

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        initTierMaps();
        registerPowerUnitRecipes(provider);

        for (Material material : GTRegistries.MATERIALS) {
            net.phoenix.core.common.data.recipe.generated.PhoenixToolRecipeHelper.run(provider, material);
        }
    }

    private static void initTierMaps() {
        motorItems.put(GTValues.LuV, GTItems.ELECTRIC_MOTOR_LuV);
        baseMaterials.put(GTValues.LuV, GTMaterials.RhodiumPlatedPalladium);
        batteryItems.put(GTValues.LuV, List.of(
                GTItems.ENERGY_LAPOTRONIC_ORB_CLUSTER));

        powerUnitItems.put(GTValues.LuV, PhoenixItems.POWER_UNIT_LUV);
    }

    private static void registerPowerUnitRecipes(@NotNull Consumer<FinishedRecipe> provider) {
        for (int tier : powerUnitItems.keySet()) {
            var powerUnitEntry = powerUnitItems.get(tier);
            var motorEntry = motorItems.get(tier);
            var material = baseMaterials.get(tier);
            var batteries = batteryItems.get(tier);

            if (powerUnitEntry == null || motorEntry == null || material == null || batteries == null ||
                    batteries.isEmpty()) {
                continue;
            }

            for (ItemEntry<? extends Item> batteryItem : batteries) {
                ItemStack batteryStack = batteryItem.asStack();
                long maxCharge = GTCapabilityHelper.getElectricItem(batteryStack).getMaxCharge();

                ItemStack powerUnitStack = ToolItemHelper.getMaxChargeOverrideStack(powerUnitEntry.get(), maxCharge);

                String recipeName = String.format(
                        "%s_%s",
                        BuiltInRegistries.ITEM.getKey(powerUnitEntry.get()).getPath(),
                        BuiltInRegistries.ITEM.getKey(batteryItem.get()).getPath());

                VanillaRecipeHelper.addShapedEnergyTransferRecipe(provider, true, false, true, recipeName,
                        Ingredient.of(batteryStack), powerUnitStack,
                        "S d", "GMG", "PBP",
                        'M', motorEntry.asStack(),
                        'S', new MaterialEntry(screw, material),
                        'P', new MaterialEntry(plate, material),
                        'G', new MaterialEntry(gearSmall, material),
                        'B', batteryStack);
            }
        }
    }
}
