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

@EmiEntrypoint
public class PhoenixEmiPlugin implements EmiPlugin {

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

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FISSION_FUEL);
        registry.addCategory(FISSION_COOLANT);
        registry.addCategory(FISSION_BREEDING);

        for (FissionFuelRodBlock.FissionFuelRodTypes type : FissionFuelRodBlock.FissionFuelRodTypes.values()) {
            registry.addRecipe(new FuelRodEmiRecipe(type));
            registry.addRecipe(new FissionFuelInfoEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_FUEL, stack);
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        for (FissionCoolerBlock.FissionCoolerTypes type : FissionCoolerBlock.FissionCoolerTypes.values()) {
            registry.addRecipe(new CoolantEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_COOLANT, stack);
            }
        }

        for (FissionBlanketBlock.BreederBlanketTypes type : FissionBlanketBlock.BreederBlanketTypes.values()) {
            registry.addRecipe(new BreedingEmiRecipe(type));
            registry.addRecipe(new BreederBlanketInfoEmiRecipe(type));
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        for (FissionModeratorBlock.FissionModeratorTypes type : FissionModeratorBlock.FissionModeratorTypes.values()) {
            EmiStack stack = FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + type.getName());
            if (!stack.isEmpty()) {
                registry.addWorkstation(FISSION_FUEL, stack);
                registry.addWorkstation(FISSION_BREEDING, stack);
            }
        }

        registry.addWorkstation(FISSION_FUEL,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));
        registry.addWorkstation(FISSION_COOLANT,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));
        registry.addWorkstation(FISSION_BREEDING,
                EmiStack.of(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack()));

        registry.addWorkstation(FISSION_FUEL,
                EmiStack.of(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack()));
        registry.addWorkstation(FISSION_COOLANT,
                EmiStack.of(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack()));

        registry.addExclusionArea(RecipeBuilderScreen.class, (screen, consumer) -> consumer.accept(new Bounds(
                screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize())));
        registry.addDragDropHandler(RecipeBuilderScreen.class, new RecipeBuilderDragDrop());
    }

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
