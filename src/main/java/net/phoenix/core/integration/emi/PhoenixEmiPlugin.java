package net.phoenix.core.integration.emi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;

/**
 * EMI plugin for PhoenixCore.
 * Registers the drag-drop handler so items and fluids can be dragged
 * from EMI directly into the Recipe Builder screen's slots.
 * You MUST declare this class in your mods.toml (or emi.json if your
 * EMI version uses that) under the [[emi.plugins]] section:
 * # In mods.toml, add inside [[dependencies.phoenixcore]]:
 * [[emi.plugins]]
 * plugin = "net.phoenix.core.client.gui.emi.PhoenixEmiPlugin"
 *
 * OR, if EMI uses the @EmiEntrypoint annotation style:
 *
 * The @EmiEntrypoint annotation on this class handles registration automatically.
 */
@EmiEntrypoint

public class PhoenixEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(RecipeBuilderScreen.class, (screen, consumer) -> {
            // This pushes the EMI sidebar to the right of your 338px width
            consumer.accept(new Bounds(
                    screen.getGuiLeft(),
                    screen.getGuiTop(),
                    screen.getXSize(), // imageWidth (338)
                    screen.getYSize()  // imageHeight (238)
            ));
        });

        registry.addDragDropHandler(RecipeBuilderScreen.class, new RecipeBuilderDragDrop());
    }

    // ─────────────────────────────────────────────────────────────────────────
    private static class RecipeBuilderDragDrop implements EmiDragDropHandler<RecipeBuilderScreen> {

        @Override
        public boolean dropStack(RecipeBuilderScreen screen, EmiIngredient ingredient, int x, int y) {
            if (ingredient.isEmpty()) return false;
            EmiStack first = ingredient.getEmiStacks().get(0);

            // ── Item stack ────────────────────────────────────────────────────
            if (first instanceof dev.emi.emi.api.stack.ItemEmiStack itemEmi) {
                ItemStack mc = itemEmi.getItemStack();

                // Try item-input panel first, then output
                if (screen.itemInputPanel.isMouseOver(x, y))
                    return screen.itemInputPanel.acceptStack(mc, x, y);
                if (screen.itemOutputPanel.isMouseOver(x, y))
                    return screen.itemOutputPanel.acceptStack(mc, x, y);
                // Default: inputs
                return screen.itemInputPanel.acceptStack(mc, x, y);
            }

            // ── Fluid stack ───────────────────────────────────────────────────
            // ── Fluid stack ───────────────────────────────────────────────────
            if (first instanceof dev.emi.emi.api.stack.FluidEmiStack fluidEmi) {
                // 1. Get the ID directly from the EmiStack (this returns a ResourceLocation)
                ResourceLocation res = fluidEmi.getId();

                // 2. Format it for your Recipe Builder expression
                // We convert it to a GT-style or Phoenix-style string
                String id = (res != null) ? res.toString() : "minecraft:empty";

                // 3. Get the amount (long to int cast)
                int amount = (int) fluidEmi.getAmount();

                // Determine which panel should accept it based on mouse position
                if (screen.fluidInputPanel.isMouseOver(x, y))
                    return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
                if (screen.fluidOutputPanel.isMouseOver(x, y))
                    return screen.fluidOutputPanel.acceptFluid(id, amount, x, y);

                // Fallback default
                return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
            }

            return false;
        }
    }
}
