package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

public class FissionFuelInfoEmiRecipe implements EmiRecipe {

    private static final int W = 220;
    private static final int H = 140;

    private static final String[] LOGS = {

            "LOG-001 // Standard fissile charge. Stable neutron cross-section. " +
                    "Recommended for initial reactor commissioning and baseline calibration runs.",

            "LOG-002 // Enriched oxide pellet. Heat curve peaks early; " +
                    "moderate bias keeps daughter products predictable. " +
                    "Standard issue for civilian grid reactors.",

            "LOG-003 // High-density metallic slug. Elevated base heat observed at all flux levels. " +
                    "Neutron bias measured at upper tolerance — review moderator pairing before deployment.",

            "LOG-004 // Experimental ternary alloy. Documented output variance under prolonged burn. " +
                    "Cleared for controlled use; continuous monitoring recommended beyond 8-minute cycles.",

            "LOG-005 // Surplus weapons-grade material, downblended for reactor service. " +
                    "Extreme heat signature. Approved only for hardened thermal loop installations.",

            "LOG-006 // Proprietary exotic-matrix rod. Origin classified. " +
                    "Heat production anomalous relative to declared mass. " +
                    "Engineering division has filed three separate incident reports. Handle with care.",
    };

    private final FissionFuelRodBlock.FissionFuelRodTypes fuel;
    private final int ordinal;

    public FissionFuelInfoEmiRecipe(FissionFuelRodBlock.FissionFuelRodTypes fuel) {
        this.fuel = fuel;
        this.ordinal = fuel.ordinal();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_FUEL;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_fuel_info/" + fuel.getName());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + fuel.getName()));
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

        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + fuel.getName()), 6, y)
                .drawBack(true);

        String displayName = fuel.getName().replace("_", " ").toUpperCase();
        widgets.addText(
                Component.literal(displayName)
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(ChatFormatting.BOLD),
                28, y + 1, 0xFFFFFF, false);

        widgets.addText(
                Component.literal("Fission Fuel Rod")
                        .withStyle(ChatFormatting.DARK_GRAY),
                28, y + 11, 0xFFFFFF, false);

        y += 24;

        final int ruleY = y;
        widgets.addDrawable(6, ruleY, W - 12, 1,
                (gui, mx, my, dt) -> gui.fill(0, 0, W - 12, 1, 0x44AAAAAA));
        y += 6;

        int col1 = 6, col2 = 114;

        statLine(widgets, col1, y, "Base Heat",
                fuel.getBaseHeatProduction() + " HU/t", heatColor(fuel.getBaseHeatProduction()));
        statLine(widgets, col2, y, "Neutron Bias",
                "+" + fuel.getNeutronBias(), biasColor(fuel.getNeutronBias()));
        y += 16;

        statLine(widgets, col1, y, "Heat Class",
                heatClass(fuel.getBaseHeatProduction()), ChatFormatting.WHITE);
        statLine(widgets, col2, y, "Bias Class",
                biasClass(fuel.getNeutronBias()), ChatFormatting.WHITE);
        y += 20;

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

    private ChatFormatting heatColor(int heat) {
        if (heat < 50) return ChatFormatting.GREEN;
        if (heat < 150) return ChatFormatting.YELLOW;
        if (heat < 300) return ChatFormatting.GOLD;
        return ChatFormatting.RED;
    }

    private String heatClass(int heat) {
        if (heat < 50) return "Low";
        if (heat < 150) return "Moderate";
        if (heat < 300) return "High";
        return "Extreme";
    }

    private ChatFormatting biasColor(int bias) {
        if (bias < 5) return ChatFormatting.GREEN;
        if (bias < 10) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private String biasClass(int bias) {
        if (bias < 5) return "Stable";
        if (bias < 10) return "Elevated";
        return "Volatile";
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
