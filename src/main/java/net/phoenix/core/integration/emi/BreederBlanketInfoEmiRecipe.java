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

public class BreederBlanketInfoEmiRecipe implements EmiRecipe {

    private static final int W = 220;
    private static final int H = 150;

    private static final String[] LOGS = {
            "LOG-B001 // Primary fertile blanket. Thorium matrix absorbs thermal neutrons " +
                    "with high efficiency. Long cycle time; output is predictable. " +
                    "Preferred blanket for first-generation breeding programs.",

            "LOG-B002 // Depleted uranium packing. Dense and inexpensive. " +
                    "Breeds Pu-239 reliably at moderate flux. " +
                    "Instability of daughter products within acceptable safety margins.",

            "LOG-B003 // Lithium ceramic composite. Tritium yield correlates tightly with fast flux. " +
                    "Short cycle. Handle output with shielded containment — " +
                    "decay heat observed above background for 48 hours post-extraction.",

            "LOG-B004 // Experimental mixed-actinide target matrix. " +
                    "Output spread is wider than standard blankets; neutron bias of paired fuel shifts distribution. " +
                    "Not approved for unattended operation.",

            "LOG-B005 // Classified fertile assembly. Material composition redacted per directive 7-G. " +
                    "Output logged as anomalous. Engineering staff rotated after third cycle.",
    };

    private final FissionBlanketBlock.BreederBlanketTypes blanket;
    private final int ordinal;

    public BreederBlanketInfoEmiRecipe(FissionBlanketBlock.BreederBlanketTypes blanket) {
        this.blanket = blanket;
        this.ordinal = blanket.ordinal();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_BREEDING;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "breeder_blanket_info/" + blanket.getName());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + blanket.getName()));
    }

    @Override
    public int getDisplayWidth() {
        return W;
    }

    @Override
    public int getDisplayHeight() {
        return H;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int y = 6;

        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + blanket.getName()), 6, y)
                .drawBack(true);

        String displayName = blanket.getName().replace("_", " ").toUpperCase();
        widgets.addText(
                Component.literal(displayName)
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(ChatFormatting.BOLD),
                28, y + 1, 0xFFFFFF, false);

        widgets.addText(
                Component.literal("Breeder Blanket")
                        .withStyle(ChatFormatting.DARK_GRAY),
                28, y + 11, 0xFFFFFF, false);

        y += 24;

        final int ruleY = y;
        widgets.addDrawable(6, ruleY, W - 12, 1,
                (gui, mx, my, dt) -> gui.fill(0, 0, W - 12, 1, 0x44AAAAAA));
        y += 6;

        int col1 = 6, col2 = 114;

        int durationSec = blanket.getDurationTicks() / 20;
        statLine(widgets, col1, y, "Cycle Duration",
                durationSec + "s", cycleColor(durationSec));
        statLine(widgets, col2, y, "Amount / Cycle",
                String.valueOf(blanket.getAmountPerCycle()), ChatFormatting.WHITE);
        y += 16;

        var outputs = blanket.getOutputs();
        int ox = col1;
        int maxInstability = 0;
        for (var o : outputs) {
            if (o.instability() > maxInstability) maxInstability = o.instability();
        }

        statLine(widgets, col1, y, "Outputs",
                outputs.size() + " product" + (outputs.size() != 1 ? "s" : ""), ChatFormatting.WHITE);
        statLine(widgets, col2, y, "Peak Instability",
                String.valueOf(maxInstability), instColor(maxInstability));
        y += 16;

        for (var o : outputs) {
            widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId(o.key()), ox, y)
                    .drawBack(true)
                    .appendTooltip(Component.literal("Instability: " + o.instability())
                            .withStyle(instColor(o.instability())))
                    .appendTooltip(Component.literal("Weight: " + o.weight())
                            .withStyle(ChatFormatting.GRAY));
            ox += 20;
        }
        y += 22;

        final int ruleY2 = y;
        widgets.addDrawable(6, ruleY2, W - 12, 1,
                (gui, mx, my, dt) -> gui.fill(0, 0, W - 12, 1, 0x33AAAAAA));
        y += 6;

        widgets.addText(
                Component.literal("// FIELD LOG").withStyle(ChatFormatting.DARK_GRAY),
                6, y, 0xFFFFFF, false);
        y += 9;

        String log = ordinal < LOGS.length ? LOGS[ordinal] : "LOG-??? // No record found.";
        for (String line : wrapText(log, 34)) {
            widgets.addText(
                    Component.literal(line).withStyle(ChatFormatting.GRAY),
                    6, y, 0xFFFFFF, false);
            y += 9;
        }
    }

    private void statLine(WidgetHolder w, int x, int y,
                          String label, String value, ChatFormatting valueColor) {
        w.addText(Component.literal(label + ":").withStyle(ChatFormatting.DARK_GRAY),
                x, y, 0xFFFFFF, false);
        w.addText(Component.literal(value).withStyle(valueColor),
                x, y + 8, 0xFFFFFF, false);
    }

    private ChatFormatting cycleColor(int sec) {
        if (sec <= 30) return ChatFormatting.GREEN;
        if (sec <= 120) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private ChatFormatting instColor(int inst) {
        if (inst == 0) return ChatFormatting.GREEN;
        if (inst <= 2) return ChatFormatting.YELLOW;
        if (inst <= 4) return ChatFormatting.RED;
        return ChatFormatting.DARK_RED;
    }

    private static List<String> wrapText(String text, int charWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > charWidth) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(' ');
        }
        if (!line.isEmpty()) lines.add(line.toString().trim());
        return lines;
    }
}
