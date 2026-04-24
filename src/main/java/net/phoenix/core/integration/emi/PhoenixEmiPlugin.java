package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.integration.phoenix_fission.common.PhoenixFissionMachines;
import net.phoenix.core.integration.phoenix_fission.common.data.block.*;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.widget.Bounds;

/**
 * EMI plugin for PhoenixCore fission system.
 *
 * Changes from original:
 * - Removed blank EmiInfoRecipe registrations for moderators (they showed empty cards).
 * - Moderators now registered as workstations for BOTH FISSION_FUEL and FISSION_BREEDING
 * (moderator count affects breeder output parallels too).
 * - Fuel rods now also registered as FISSION_BREEDING workstations (they drive
 * breeding parallels and neutron bias).
 */
@EmiEntrypoint
public class PhoenixEmiPlugin implements EmiPlugin {

    // ── Category definitions ───────────────────────────────────────────────────
    public static final EmiStack COOLER_ICON = EmiStack.of(PhoenixFissionBlocks.COOLER_BASIC.asStack());

    public static final EmiRecipeCategory FISSION_FUEL = new EmiRecipeCategory(
            new ResourceLocation("phoenixcore", "fission_fuel"),
            EmiStack.of(ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Uranium235)));

    public static final EmiRecipeCategory FISSION_COOLANT = new EmiRecipeCategory(
            new ResourceLocation("phoenixcore", "fission_coolant"),
            EmiStack.of(Items.WATER_BUCKET));

    public static final EmiRecipeCategory FISSION_BREEDING = new EmiRecipeCategory(
            new ResourceLocation("phoenixcore", "fission_breeding"),
            EmiStack.of(Items.CAULDRON));

    // ── Registration ──────────────────────────────────────────────────────────
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FISSION_FUEL);
        registry.addCategory(FISSION_COOLANT);
        registry.addCategory(FISSION_BREEDING);

        // ── Calculators (one per category they belong to) ───────────────────
        registry.addRecipe(new FissionCalculatorEmiRecipe());
        registry.addRecipe(new BreederCalculatorEmiRecipe());

        // ── Fuel rods ────────────────────────────────────────────────────────
        for (FissionFuelRodBlock.FissionFuelRodTypes type : FissionFuelRodBlock.FissionFuelRodTypes.values()) {
            registry.addRecipe(new FuelRodEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                // Fuel rods are workstations for fuel AND breeding (they affect breeder parallels)
                registry.addWorkstation(FISSION_FUEL, stack);
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        // ── Coolants ─────────────────────────────────────────────────────────
        for (FissionCoolerBlock.FissionCoolerTypes type : FissionCoolerBlock.FissionCoolerTypes.values()) {
            registry.addRecipe(new CoolantEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_COOLANT, stack);
            }
        }

        // ── Breeding blankets ─────────────────────────────────────────────────
        for (FissionBlanketBlock.BreederBlanketTypes type : FissionBlanketBlock.BreederBlanketTypes.values()) {
            registry.addRecipe(new BreedingEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        // ── Moderators ────────────────────────────────────────────────────────
        // Moderators affect BOTH fuel heat/EU (FISSION_FUEL) and breeder parallels (FISSION_BREEDING).
        // The old code only added them to FISSION_FUEL and registered empty info pages; both issues fixed.
        for (FissionModeratorBlock.FissionModeratorTypes type : FissionModeratorBlock.FissionModeratorTypes.values()) {
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_FUEL, stack);
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        // ── Machine workstations ──────────────────────────────────────────────
        // High-Performance Breeder: all three categories
        registry.addWorkstation(FISSION_FUEL,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));
        registry.addWorkstation(FISSION_COOLANT,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));
        registry.addWorkstation(FISSION_BREEDING,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));

        // Pressurized Fission: fuel + coolant only (no breeding)
        registry.addWorkstation(FISSION_FUEL,
                EmiStack.of(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack()));
        registry.addWorkstation(FISSION_COOLANT,
                EmiStack.of(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack()));

        // ── Recipe Builder exclusion + drag-drop ─────────────────────────────
        registry.addExclusionArea(RecipeBuilderScreen.class, (screen, consumer) -> consumer.accept(new Bounds(
                screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize())));
        registry.addDragDropHandler(RecipeBuilderScreen.class, new RecipeBuilderDragDrop());
    }

    // ── Drag-drop handler ─────────────────────────────────────────────────────

    private static class RecipeBuilderDragDrop implements EmiDragDropHandler<RecipeBuilderScreen> {

        @Override
        public boolean dropStack(RecipeBuilderScreen screen, EmiIngredient ingredient, int x, int y) {
            if (ingredient.isEmpty()) return false;
            EmiStack first = ingredient.getEmiStacks().get(0);

            if (first instanceof ItemEmiStack itemEmi) {
                ItemStack mc = itemEmi.getItemStack();
                if (screen.itemInputPanel.isMouseOver(x, y))
                    return screen.itemInputPanel.acceptStack(mc, x, y);
                if (screen.itemOutputPanel.isMouseOver(x, y))
                    return screen.itemOutputPanel.acceptStack(mc, x, y);
                return screen.itemInputPanel.acceptStack(mc, x, y);
            }

            if (first instanceof FluidEmiStack fluidEmi) {
                ResourceLocation res = fluidEmi.getId();
                String id = (res != null) ? res.toString() : "minecraft:empty";
                int amount = (int) fluidEmi.getAmount();
                if (screen.fluidInputPanel.isMouseOver(x, y))
                    return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
                if (screen.fluidOutputPanel.isMouseOver(x, y))
                    return screen.fluidOutputPanel.acceptFluid(id, amount, x, y);
                return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
            }

            return false;
        }
    }
}
