package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

import javax.annotation.Nullable;

public class FuelRodEmiRecipe implements EmiRecipe {

    private final FissionFuelRodBlock.FissionFuelRodTypes type;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public FuelRodEmiRecipe(FissionFuelRodBlock.FissionFuelRodTypes type) {
        this.type = type;
        this.inputs = List.of(getEmiStackFromId(type.getFuelKey()));
        this.outputs = List.of(getEmiStackFromId(type.getOutputKey()));
    }

    public static EmiStack getEmiStackFromId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return EmiStack.EMPTY;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item != null && item != Items.AIR) {
            return EmiStack.of(new ItemStack(item));
        }

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid != null && fluid != Fluids.EMPTY) {
            return EmiStack.of(fluid);
        }

        return EmiStack.EMPTY;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_FUEL;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_fuel/" + type.getName());
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
        widgets.addFillingArrow(48, 16, type.getDurationTicks() * 50);

        int durSec = type.getDurationTicks() / 20;
        String durStr = durSec >= 60 ? (durSec / 60) + "m " + (durSec % 60) + "s" : durSec + "s";

        widgets.addText(
                Component.literal(type.getBaseHeatProduction() + " HU/t").withStyle(ChatFormatting.RED),
                8, 37, 0xFFFFFF, false);

        widgets.addText(
                Component.literal(durStr).withStyle(ChatFormatting.GRAY),
                114, 37, 0xFFFFFF, false);

        int bias = type.getNeutronBias();
        if (bias > 0) {
            ChatFormatting biasColor = bias >= 12 ? ChatFormatting.RED :
                    bias >= 5 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            widgets.addText(
                    Component.literal("bias +" + bias).withStyle(biasColor),
                    52, 4, 0xFFFFFF, false);
        }
    }
}
