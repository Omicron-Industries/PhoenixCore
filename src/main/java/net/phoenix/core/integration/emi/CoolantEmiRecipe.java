package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionCoolerBlock;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

import javax.annotation.Nullable;

public class CoolantEmiRecipe implements EmiRecipe {

    private final FissionCoolerBlock.FissionCoolerTypes type;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public CoolantEmiRecipe(FissionCoolerBlock.FissionCoolerTypes type) {
        this.type = type;
        this.inputs = List.of(FuelRodEmiRecipe.getEmiStackFromId(type.getRequiredCoolantMaterialId())
                .setAmount(type.getCoolantUsagePerTick()));
        this.outputs = List.of(FuelRodEmiRecipe.getEmiStackFromId(type.getOutputCoolantFluidId())
                .setAmount(type.getCoolantUsagePerTick()));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_COOLANT;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_coolant/" + type.getName());
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
        return 52;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 8, 16);
        widgets.addSlot(outputs.get(0), 114, 16).recipeContext(this);
        widgets.addFillingArrow(48, 16, 1000);

        widgets.addText(
                Component.literal("−" + formatHeat(type.getCoolerTemperature()) + " HU/t")
                        .withStyle(ChatFormatting.AQUA),
                8, 4, 0xFFFFFF, false);

        long mbt = type.getCoolantUsagePerTick();
        widgets.addText(
                Component.literal(mbt + " mB/t").withStyle(ChatFormatting.DARK_AQUA),
                8, 37, 0xFFFFFF, false);

        widgets.addText(
                Component.literal("continuous").withStyle(ChatFormatting.DARK_GRAY),
                95, 37, 0xFFFFFF, false);
    }

    private String formatHeat(int hut) {
        if (hut >= 1000 && hut % 1000 == 0) return (hut / 1000) + "k";
        if (hut >= 1000) return String.format("%.1fk", hut / 1000.0);
        return String.valueOf(hut);
    }
}
