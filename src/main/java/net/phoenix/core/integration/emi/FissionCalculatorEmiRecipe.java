package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionCoolerBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

public class FissionCalculatorEmiRecipe implements EmiRecipe {

    // Statics ensure the calculator state persists when clicking buttons
    private static int fuelIndex = 2;
    private static int fuelCount = 1;
    private static int coolerIndex = 0;
    private static int coolerCount = 0;
    private static int moderatorIndex = 1;
    private static int moderatorCount = 1;
    private static int burnTimeMinutes = 0;

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixEmiPlugin.FISSION_FUEL;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("phoenixcore", "fission_calculator");
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return 170;
    }

    @Override
    public int getDisplayHeight() {
        // We set this to 130 to provide a "footer" area for EMI system buttons
        return 125;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var fuel = FissionFuelRodBlock.FissionFuelRodTypes.values()[fuelIndex];
        var moderator = FissionModeratorBlock.FissionModeratorTypes.values()[moderatorIndex];
        var cooler = FissionCoolerBlock.FissionCoolerTypes.values()[coolerIndex];

        // --- MATH ---
        double burnMult = 1.0 + (0.3) * Math.min(1.0, (burnTimeMinutes * 60.0) / 1200.0);
        double modMult = 0.1 + ((moderator.getTier() * 0.5 * moderatorCount) / 10.0);
        int parallels = (fuelCount) + (moderator.getTier() * moderatorCount);
        int totalHeatGain = (int) ((fuel.getBaseHeatProduction() * fuelCount * modMult) * parallels * burnMult);
        int totalCooling = cooler.getCoolerTemperature() * coolerCount;
        int netBalance = totalCooling - totalHeatGain;

        // --- CONFIG ---
        int startY = 10;
        int rowHeight = 26;
        int col1_Icons = 6;
        int col2_Buttons = 28;
        int col3_Values = 44;
        int col4_Labels = 105; // Results column

        // Background Separator (Vertical line)
        widgets.addDrawable(95, 10, 1, 100, (gui, mouseX, mouseY, delta) -> {
            gui.fill(0, 0, 1, 100, 0x44FFFFFF);
        });

        // Row 1: Fuel
        drawCleanRow(widgets, startY, "Fuel", fuel.getName(), fuelCount, col1_Icons, col2_Buttons, col3_Values,
                () -> {
                    fuelIndex = (fuelIndex + 1) % FissionFuelRodBlock.FissionFuelRodTypes.values().length;
                },
                (val) -> fuelCount = Math.max(1, val));

        // Row 2: Moderator
        drawCleanRow(widgets, startY + rowHeight, "Mod", moderator.getName(), moderatorCount, col1_Icons, col2_Buttons,
                col3_Values,
                () -> {
                    moderatorIndex = (moderatorIndex + 1) % FissionModeratorBlock.FissionModeratorTypes.values().length;
                },
                (val) -> moderatorCount = Math.max(0, val));

        // Row 3: Cooler
        drawCleanRow(widgets, startY + (rowHeight * 2), "Cool", cooler.getName(), coolerCount, col1_Icons, col2_Buttons,
                col3_Values,
                () -> {
                    coolerIndex = (coolerIndex + 1) % FissionCoolerBlock.FissionCoolerTypes.values().length;
                },
                (val) -> coolerCount = Math.max(0, val));

        // Row 4: Time (No icon, so we offset text)
        int timeY = startY + (rowHeight * 3) + 2;
        widgets.addText(Component.literal("TIME").withStyle(ChatFormatting.DARK_GRAY), col1_Icons, timeY + 6, 0xFFFFFF,
                false);
        addStepButtons(widgets, col2_Buttons, timeY, (val) -> burnTimeMinutes = Math.max(0, burnTimeMinutes + val));
        widgets.addText(Component.literal(burnTimeMinutes + "m"), col3_Values, timeY + 6, 0xFFFFFF, false);

        // --- RESULTS COLUMN ---
        widgets.addText(Component.literal("HEAT").withStyle(ChatFormatting.GOLD), col4_Labels, startY, 0xFFFFFF, false);
        widgets.addText(Component.literal("-" + totalHeatGain).withStyle(ChatFormatting.RED), col4_Labels, startY + 10,
                0xFFFFFF, false);

        widgets.addText(Component.literal("COOLING").withStyle(ChatFormatting.AQUA), col4_Labels, startY + 30, 0xFFFFFF,
                false);
        widgets.addText(Component.literal("+" + totalCooling).withStyle(ChatFormatting.BLUE), col4_Labels, startY + 40,
                0xFFFFFF, false);

        // Status Box
        String statusText = netBalance >= 0 ? "STABLE" : "DANGER";
        ChatFormatting statusColor = netBalance >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        widgets.addText(Component.literal(statusText).withStyle(statusColor).withStyle(ChatFormatting.BOLD),
                col4_Labels, startY + 65, 0xFFFFFF, false);

        // Multiplier Subtext
        widgets.addText(Component.literal(String.format("%.1fx Heat", burnMult)).withStyle(ChatFormatting.ITALIC)
                .withStyle(ChatFormatting.GRAY), col4_Labels, startY + 85, 0xFFFFFF, false);

        // Footer spacer for EMI
        widgets.addSlot(EmiStack.EMPTY, 150, 110).drawBack(false);
    }

    private void drawCleanRow(WidgetHolder widgets, int y, String label, String name, int count, int xI, int xB, int xV,
                              Runnable cycler, java.util.function.Consumer<Integer> setter) {
        // Icon and Cycler Button Overlay
        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + name), xI, y).drawBack(true);
        widgets.addButton(xI, y, 18, 18, 0, 0, () -> true, (x, y1, b) -> {
            cycler.run();
            EmiApi.displayRecipe(this);
        });

        // Up/Down Buttons
        addStepButtons(widgets, xB, y - 2, (val) -> setter.accept(count + val));

        // Value Display
        widgets.addText(Component.literal("x" + count), xV, y + 4, 0xFFFFFF, false);
    }

    private void addStepButtons(WidgetHolder widgets, int x, int y, java.util.function.Consumer<Integer> action) {
        widgets.addButton(x, y, 12, 10, 0, 0, () -> true, (mx, my, b) -> {
            action.accept(1);
            EmiApi.displayRecipe(this);
        });
        widgets.addButton(x, y + 11, 12, 10, 0, 0, () -> true, (mx, my, b) -> {
            action.accept(-1);
            EmiApi.displayRecipe(this);
        });
    }
}
