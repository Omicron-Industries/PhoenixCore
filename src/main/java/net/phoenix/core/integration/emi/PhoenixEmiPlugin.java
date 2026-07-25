package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    @Override
    public void register(EmiRegistry registry) {

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
