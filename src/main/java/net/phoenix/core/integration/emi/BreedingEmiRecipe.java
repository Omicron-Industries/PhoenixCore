package net.phoenix.core.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BreedingEmiRecipe implements EmiRecipe {
    private final FissionBlanketBlock.BreederBlanketTypes type;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    // Layout constants
    private static final int SLOT_SIZE  = 18;
    private static final int SLOT_GAP   = 2;
    private static final int MAX_COLS   = 4;

    public BreedingEmiRecipe(FissionBlanketBlock.BreederBlanketTypes type) {
        this.type = type;
        this.inputs = List.of(FuelRodEmiRecipe.getEmiStackFromId(type.getInputKey()));
        this.outputs = type.getOutputs().stream()
                .map(out -> FuelRodEmiRecipe.getEmiStackFromId(out.key()))
                .toList();
    }

    @Override public EmiRecipeCategory getCategory() { return PhoenixEmiPlugin.FISSION_BREEDING; }

    @Override
    public @Nullable ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_breeding/" + type.getName());
    }

    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return 144; }

    @Override
    public int getDisplayHeight() {
        // Header (14) + input row (20) + arrow (14) + output grid (dynamic) + footer (16) + padding
        int outputCount = outputs.size();
        int outputRows  = (int) Math.ceil(outputCount / (double) MAX_COLS);
        return 14 + 20 + 14 + (outputRows * (SLOT_SIZE + SLOT_GAP)) + 18;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        @NotNull List<IFissionBlanketType.BlanketOutput> outData = type.getOutputs();
        int outputCount = outData.size();

        // ── Header: blanket name + duration ─────────────────────────────────
        int durSec = type.getDurationTicks() / 20;
        String durStr = durSec >= 60
                ? (durSec / 60) + "m " + (durSec % 60) + "s"
                : durSec + "s";

        widgets.addText(
                Component.literal("Cycle: " + durStr).withStyle(ChatFormatting.GOLD),
                8, 3, 0xFFFFFF, false);
        widgets.addText(
                Component.literal("×" + type.getAmountPerCycle() + " consumed").withStyle(ChatFormatting.GRAY),
                80, 3, 0xFFFFFF, false);

        // ── Input slot ───────────────────────────────────────────────────────
        int inputY = 14;
        widgets.addSlot(inputs.get(0), 8, inputY);

        // ── Arrow below input ─────────────────────────────────────────────
        int arrowY = inputY + SLOT_SIZE + 2;
        widgets.addText(
                Component.literal("▼").withStyle(ChatFormatting.DARK_GRAY),
                16, arrowY + 1, 0xFFFFFF, false);

        // ── Output grid ───────────────────────────────────────────────────
        int gridStartY = arrowY + 14;
        int gridStartX = 8;

        for (int i = 0; i < outputCount; i++) {
            int col = i % MAX_COLS;
            int row = i / MAX_COLS;
            int x = gridStartX + col * (SLOT_SIZE + SLOT_GAP);
            int y = gridStartY + row * (SLOT_SIZE + SLOT_GAP);

            IFissionBlanketType.BlanketOutput data = outData.get(i);

            // Instability colour: 0-1=green, 2-3=amber, 4-5=red, 6+=dark red
            ChatFormatting instColor = data.instability() >= 6 ? ChatFormatting.DARK_RED
                    : data.instability() >= 4 ? ChatFormatting.RED
                    : data.instability() >= 2 ? ChatFormatting.GOLD
                    : ChatFormatting.GREEN;

            // Weight as a rough percentage for the tooltip
            int totalWeight = outData.stream().mapToInt(o -> o.weight()).sum();
            int pct = totalWeight > 0 ? Math.round(data.weight() * 100f / totalWeight) : 0;

            widgets.addSlot(outputs.get(i), x, y)
                    .appendTooltip(Component.literal("~" + pct + "% chance").withStyle(ChatFormatting.WHITE))
                    .appendTooltip(Component.literal("Instability: " + data.instability()).withStyle(instColor))
                    .appendTooltip(Component.literal("Weight: " + data.weight()).withStyle(ChatFormatting.DARK_GRAY));
        }

        // ── Footer: instability legend ────────────────────────────────────
        int footerY = gridStartY + ((int) Math.ceil(outputCount / (double) MAX_COLS)) * (SLOT_SIZE + SLOT_GAP) + 2;
        widgets.addText(
                Component.literal("Instability: ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal("■").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("■").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("■").withStyle(ChatFormatting.RED))
                        .append(Component.literal("■").withStyle(ChatFormatting.DARK_RED))
                        .append(Component.literal(" 0→6+").withStyle(ChatFormatting.DARK_GRAY)),
                8, footerY, 0xFFFFFF, false);
    }
}