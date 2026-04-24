package net.phoenix.core.integration.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;

import java.util.List;

/**
 * Interactive breeder reactor calculator.
 *
 * Fixed bugs vs. original:
 *  - All state is STATIC so EMI re-renders don't reset selections.
 *  - Breeder math matches the HTML tool exactly:
 *      amount  = ceil(amtPerCycle × parallels × burnMult)
 *      adjWeight[i] = max(0.1×w, w × (1 + (bias/100) × instability × 0.5))
 *    Cycle DURATION is fixed — bias only shifts output weights, not time.
 *  - Cycle buttons call EmiApi.displayRecipe(this) so the display updates.
 *  - Instability warnings shown when high-bias fuel shifts toward dangerous isotopes.
 */
public class BreederCalculatorEmiRecipe implements EmiRecipe {

    // ── Persistent state ────────────────────────────────────────────────────────
    private static int fuelIndex      = 0;
    private static int blanketIndex   = 0;
    private static int moderatorIndex = 0;
    private static int rodCount       = 1;
    private static int burnTimeMin    = 0;

    // ── Constants ────────────────────────────────────────────────────────────────
    private static final int   BASE_PAR_PER_ROD = 1;
    private static final double BURN_BONUS_MAX  = 0.30;
    private static final int   BURN_RAMP_MIN    = 20;

    // ── Layout ───────────────────────────────────────────────────────────────────
    private static final int W = 200;
    private static final int H = 210;

    @Override public EmiRecipeCategory getCategory()   { return PhoenixEmiPlugin.FISSION_BREEDING; }
    @Override public ResourceLocation getId()           { return new ResourceLocation("phoenixcore", "breeder_calculator"); }
    @Override public List<EmiIngredient> getInputs()   { return List.of(); }
    @Override public List<EmiStack> getOutputs()       { return List.of(); }
    @Override public int getDisplayWidth()              { return W; }
    @Override public int getDisplayHeight()             { return H; }

    // ── Math ─────────────────────────────────────────────────────────────────────

    private static double burnMult() {
        return 1.0 + BURN_BONUS_MAX * Math.min(1.0, (double) burnTimeMin / BURN_RAMP_MIN);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var fuel     = FissionFuelRodBlock.FissionFuelRodTypes.values()[fuelIndex];
        var blanket  = FissionBlanketBlock.BreederBlanketTypes.values()[blanketIndex];
        var moderator = FissionModeratorBlock.FissionModeratorTypes.values()[moderatorIndex];

        // ── MATH ──────────────────────────────────────────────────────────────
        double bm       = burnMult();
        int modParBonus = moderator.getTier() * 1; // single moderator block as reference
        int parallels   = Math.min(256, rodCount * BASE_PAR_PER_ROD + modParBonus);
        int amount      = (int) Math.ceil(blanket.getAmountPerCycle() * parallels * bm);
        int durationSec = blanket.getDurationTicks() / 20;
        double biasFactor = fuel.getNeutronBias() / 100.0;

        // EU/fuel discount from moderator
        int euBoost  = moderator.getEUBoost();
        int fuelDisc = moderator.getFuelDiscount();

        // Adjusted output weights (bias shifts toward high-instability outputs)
        var outputs  = blanket.getOutputs();
        double[] adjW = new double[outputs.size()];
        double   totalW = 0;
        for (int i = 0; i < outputs.size(); i++) {
            var o = outputs.get(i);
            adjW[i] = Math.max(0.1 * o.weight(), o.weight() * (1.0 + biasFactor * o.instability() * 0.5));
            totalW += adjW[i];
        }

        // Danger metric: weighted average instability of expected output
        double avgInst = 0;
        for (int i = 0; i < outputs.size(); i++) {
            avgInst += (adjW[i] / totalW) * outputs.get(i).instability();
        }

        // ── HEADER ────────────────────────────────────────────────────────────
        int y = 6;
        widgets.addText(mkLabel("BREEDER CALCULATOR"), 6, y, 0xFFFFFF, false);
        y += 10;
        widgets.addDrawable(6, y, W - 10, 1, (gui, mx, my, d) -> gui.fill(0, 0, W, 1, 0x44FFFFFF));
        y += 6;

        // ── ROW: Fuel rod ─────────────────────────────────────────────────────
        int rowY = y;
        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + fuel.getName()), 6, rowY).drawBack(true);
        widgets.addButton(6, rowY, 18, 18, 0, 0, () -> true, (mx, my, b) -> {
            fuelIndex = (fuelIndex + 1) % FissionFuelRodBlock.FissionFuelRodTypes.values().length;
            EmiApi.displayRecipe(this);
        });
        addStepButtons(widgets, 28, rowY - 1,
            v -> { rodCount = Math.max(1, rodCount + v); EmiApi.displayRecipe(this); });
        widgets.addText(Component.literal("×" + rodCount), 46, rowY + 5, 0xFFFFFF, false);
        widgets.addText(Component.literal(fuel.getName()).withStyle(ChatFormatting.WHITE), 68, rowY + 1, 0xFFFFFF, false);
        widgets.addText(
            Component.literal("Bias: +" + fuel.getNeutronBias()).withStyle(ChatFormatting.LIGHT_PURPLE),
            68, rowY + 10, 0xFFFFFF, false);
        y += 22;

        // ── ROW: Blanket ───────────────────────────────────────────────────────
        rowY = y;
        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + blanket.getName()), 6, rowY).drawBack(true);
        widgets.addButton(6, rowY, 18, 18, 0, 0, () -> true, (mx, my, b) -> {
            blanketIndex = (blanketIndex + 1) % FissionBlanketBlock.BreederBlanketTypes.values().length;
            EmiApi.displayRecipe(this);
        });
        widgets.addText(Component.literal(blanket.getName()).withStyle(ChatFormatting.WHITE), 28, rowY + 1, 0xFFFFFF, false);
        widgets.addText(
            Component.literal(durationSec + "s · " + blanket.getAmountPerCycle() + "/cycle").withStyle(ChatFormatting.GRAY),
            28, rowY + 10, 0xFFFFFF, false);
        y += 22;

        // ── ROW: Moderator ────────────────────────────────────────────────────
        rowY = y;
        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + moderator.getName()), 6, rowY).drawBack(true);
        widgets.addButton(6, rowY, 18, 18, 0, 0, () -> true, (mx, my, b) -> {
            moderatorIndex = (moderatorIndex + 1) % FissionModeratorBlock.FissionModeratorTypes.values().length;
            EmiApi.displayRecipe(this);
        });
        widgets.addText(Component.literal(moderator.getName()).withStyle(ChatFormatting.WHITE), 28, rowY + 1, 0xFFFFFF, false);
        widgets.addText(
            Component.literal("+" + euBoost + "% EU · -" + fuelDisc + "% fuel").withStyle(ChatFormatting.GRAY),
            28, rowY + 10, 0xFFFFFF, false);
        y += 22;

        // ── ROW: Burn time ────────────────────────────────────────────────────
        rowY = y;
        widgets.addText(mkLabel("TIME"), 8, rowY + 5, 0xFFFFFF, false);
        addStepButtons(widgets, 28, rowY - 1,
            v -> { burnTimeMin = Math.max(0, Math.min(BURN_RAMP_MIN, burnTimeMin + v)); EmiApi.displayRecipe(this); });
        widgets.addText(Component.literal(burnTimeMin + " min"), 46, rowY + 5, 0xFFFFFF, false);
        widgets.addText(
            Component.literal("×" + String.format("%.3f", bm) + " burn mult").withStyle(ChatFormatting.GRAY),
            68, rowY + 5, 0xFFFFFF, false);
        y += 18;

        // ── DIVIDER ───────────────────────────────────────────────────────────
        final int divY2 = y;
        widgets.addDrawable(6, divY2, W - 10, 1, (gui, mx, my, d) -> gui.fill(0, 0, W, 1, 0x33FFFFFF));
        y += 6;

        // ── RESULTS ───────────────────────────────────────────────────────────
        // Output expected amounts
        widgets.addText(mkLabel("EXPECTED OUTPUT / CYCLE"), 6, y, 0xFFFFFF, false);
        y += 9;

        // Output slots in a row with expected counts as tooltips
        int slotX = 6;
        for (int i = 0; i < outputs.size(); i++) {
            var o   = outputs.get(i);
            double pct   = adjW[i] / totalW * 100;
            double exp   = amount * pct / 100.0;
            ChatFormatting col = instColor(o.instability());

            widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId(o.key()), slotX, y)
                .appendTooltip(Component.literal(o.key()).withStyle(ChatFormatting.GRAY))
                .appendTooltip(Component.literal(String.format("%.1f%%", pct) + " chance").withStyle(ChatFormatting.WHITE))
                .appendTooltip(Component.literal(String.format("~%.1f expected", exp)).withStyle(ChatFormatting.YELLOW))
                .appendTooltip(Component.literal("Instability: " + o.instability()).withStyle(col));
            slotX += 20;
        }
        y += 22;

        // Totals row
        widgets.addText(
            Component.literal("~" + amount + " rolls · " + parallels + " parallels").withStyle(ChatFormatting.YELLOW),
            6, y, 0xFFFFFF, false);
        y += 10;

        // ── INSTABILITY WARNING ───────────────────────────────────────────────
        if (fuel.getNeutronBias() >= 5) {
            ChatFormatting warnColor = fuel.getNeutronBias() >= 12 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            widgets.addText(
                Component.literal("⚠ High bias (+" + fuel.getNeutronBias() + "): shifts toward unstable isotopes")
                    .withStyle(warnColor),
                6, y, 0xFFFFFF, false);
            y += 9;
            widgets.addText(
                Component.literal(String.format("  Avg instability: %.2f", avgInst)).withStyle(ChatFormatting.GRAY),
                6, y, 0xFFFFFF, false);
        }

        // Footer spacer
        widgets.addSlot(EmiStack.EMPTY, W - 20, H - 20).drawBack(false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void addStepButtons(WidgetHolder widgets, int x, int y, java.util.function.Consumer<Integer> action) {
        widgets.addButton(x, y + 1,  12, 9, 0, 0, () -> true, (mx, my, b) -> action.accept(1));
        widgets.addButton(x, y + 11, 12, 9, 0, 0, () -> true, (mx, my, b) -> action.accept(-1));
    }

    private static Component mkLabel(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static ChatFormatting instColor(int inst) {
        if (inst == 0) return ChatFormatting.GREEN;
        if (inst <= 2)  return ChatFormatting.YELLOW;
        if (inst <= 4)  return ChatFormatting.RED;
        return ChatFormatting.DARK_RED;
    }
}
