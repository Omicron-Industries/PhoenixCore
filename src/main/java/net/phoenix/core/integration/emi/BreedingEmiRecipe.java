package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

import javax.annotation.Nullable;

public class BreedingEmiRecipe implements EmiRecipe {

    private final FissionBlanketBlock.BreederBlanketTypes type;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public BreedingEmiRecipe(FissionBlanketBlock.BreederBlanketTypes type) {
        this.type = type;
        this.inputs = List.of(FuelRodEmiRecipe.getEmiStackFromId(type.getInputKey()));

        // Map all possible outputs from the list
        this.outputs = type.getOutputs().stream()
                .map(out -> FuelRodEmiRecipe.getEmiStackFromId(out.key()))
                .toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_BREEDING;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_breeding/" + type.getName());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 144;
    }

    @Override
    public int getDisplayHeight() {
        return 60;
    } // Extra height for multiple outputs

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 10, 15);

        // Draw outputs in a grid or row
        int xOffset = 70;
        for (int i = 0; i < outputs.size(); i++) {
            var outData = type.getOutputs().get(i);
            widgets.addSlot(outputs.get(i), xOffset + (i * 20), 15)
                    .appendTooltip(Component.literal("Weight: " + outData.weight()))
                    .appendTooltip(Component.literal("Instability: " + outData.instability()));
        }

        widgets.addText(Component.literal("Duration: " + (type.getDurationTicks() / 20) + "s")
                .withStyle(ChatFormatting.GOLD), 10, 45, 0xFFFFFF, true);
    }
}
