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

/**
 * Interactive fission reactor calculator.
 *
 * Math mirrors the HTML balance tool (fission_reactor_balance_v2.html):
 *
 * modMult = 0.1 + Σ(mod.tier × 0.5 × modCount[mod]) / 10
 * modParBonus = Σ(mod.tier × modCount[mod])
 * parallels = (fuelCount × BASE_PAR) + modParBonus + floor(heatLevel / HEAT_PER_PAR)
 * heatGain = (rod.baseHeat × fuelCount × modMult) × parallels × burnMult
 * burnMult = 1 + 0.30 × min(1, burnMinutes × 60 / 1200)
 * totalCool = cooler.HU_t × coolerCount
 * net = totalCool − heatGain
 *
 * All static fields so state survives EMI's re-render cycle.
 */
public class FissionCalculatorEmiRecipe implements EmiRecipe {

    // ── Config constants (mirrors HTML DEFAULTS) ─────────────────────────────
    private static final int BASE_PAR = 1;      // parallels per fuel rod
    private static final int HEAT_PER_PAR = 10_000; // heat stored before +1 parallel
    private static final int MAX_PARALLELS = 256;
    private static final float BURN_BONUS_MAX = 0.30f;  // +30% at full ramp
    private static final int BURN_RAMP_SEC = 1200;   // seconds to reach max bonus
    private static final int MAX_EU_BOOST = 100;    // % cap
    private static final int MAX_FUEL_DISC = 90;     // % cap
    private static final int MAX_SAFE_HEAT = 100_000;

    // ── Snapshot heat levels to display in the table ─────────────────────────
    private static final int[] HEAT_LEVELS = { 0, 20_000, 40_000, 60_000, 80_000, 100_000 };

    // ── Persistent calculator state (static = survives re-render) ───────────
    private static int fuelIndex = 2;   // T3 default
    private static int fuelCount = 1;
    private static int coolerIndex = 0;
    private static int coolerCount = 0;
    private static int burnTimeMin = 0;

    // Per-moderator counts — indexed by FissionModeratorTypes ordinal
    private static int[] modCounts;

    static {
        modCounts = new int[FissionModeratorBlock.FissionModeratorTypes.values().length];
        // Default: 1× of the first real moderator (index 1 = Graphite or equivalent)
        if (modCounts.length > 1) modCounts[1] = 1;
    }

    // ── Display geometry ─────────────────────────────────────────────────────
    // Layout regions (all Y coords are from top of widget):
    // 0-12 : header bar
    // 12-xx : LEFT column — controls (fuel / cooler / burn time)
    // 12-xx : RIGHT column — moderator array
    // after : heat table rows
    // bottom : status / net balance

    private static final int W = 300;
    private static final int LEFT_W = 135; // width of left control column
    private static final int RIGHT_X = 148; // x start of moderator column
    private static final int COL_SEP = 142; // x of the separator line

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
        return W;
    }

    @Override
    public int getDisplayHeight() {
        // header(13) + controlRows(3×22=66) + separator(6) + tableHeader(10) + tableRows(6×11=66) + status(22) +
        // footer(8)
        int modRows = FissionModeratorBlock.FissionModeratorTypes.values().length - 1; // skip None
        int rightH = 13 + modRows * 20; // header + one row per mod type
        int leftH = 13 + 22 + 22 + 22 + 10 + 10 * HEAT_LEVELS.length + 24; // header + 3 control rows + table
        return Math.max(leftH, rightH) + 16; // padding
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var fuel = FissionFuelRodBlock.FissionFuelRodTypes.values()[fuelIndex];
        var cooler = FissionCoolerBlock.FissionCoolerTypes.values()[coolerIndex];
        var modTypes = FissionModeratorBlock.FissionModeratorTypes.values();

        // Ensure modCounts array is sized (safety: mod types can change)
        if (modCounts.length != modTypes.length) {
            int[] next = new int[modTypes.length];
            System.arraycopy(modCounts, 0, next, 0, Math.min(modCounts.length, next.length));
            modCounts = next;
        }

        // ── Aggregate moderator stats ────────────────────────────────────────
        double modAdd = 0; // Σ tier×0.5×count → modMult = 0.1 + modAdd/10
        int modParBonus = 0; // Σ tier×count
        int totalEuPct = 0; // Σ eu%×count (capped later)
        int totalDiscPct = 0; // Σ disc%×count (capped later)

        for (int mi = 1; mi < modTypes.length; mi++) { // skip index 0 = None
            var mod = modTypes[mi];
            int cnt = modCounts[mi];
            modAdd += mod.getTier() * 0.5 * cnt;
            modParBonus += mod.getTier() * cnt;
            totalEuPct += mod.getEUBoost() * cnt;
            totalDiscPct += mod.getFuelDiscount() * cnt;
        }

        double modMult = 0.1 + (modAdd / 10.0);
        int euBoost = Math.min(totalEuPct, MAX_EU_BOOST);
        int disc = Math.min(totalDiscPct, MAX_FUEL_DISC);

        // Burn multiplier
        double burnMult = 1.0 + BURN_BONUS_MAX * Math.min(1.0, (burnTimeMin * 60.0) / BURN_RAMP_SEC);

        // Coolant display
        long coolantMbt = cooler.getCoolantUsagePerTick() * (long) coolerCount;
        int totalCool = cooler.getCoolerTemperature() * coolerCount;

        // ── Section header ─────────────────────────────────────────────────
        int y = 2;
        widgets.addText(
                Component.literal("⚛ FISSION CALCULATOR").withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD),
                8, y, 0xFFFFFF, false);

        // Vertical separator
        int finalModParBonus = modParBonus;
        double finalModMult = modMult;
        widgets.addDrawable(COL_SEP, 0, 1, getDisplayHeight() - 8,
                (gui, mx, my, dt) -> gui.fill(0, 0, 1, getDisplayHeight() - 8, 0x44AAAAAA));

        // ════════════════════════════════════════════════════════════════════
        // LEFT COLUMN — Fuel / Cooler / Burn controls
        // ════════════════════════════════════════════════════════════════════

        int ctrlY = 15;
        int slotX = 8;

        sectionLabel(widgets, "FUEL ROD", slotX, ctrlY);
        ctrlY += 11;

        // Fuel slot (clickable to cycle)
        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + fuel.getName()), slotX, ctrlY)
                .appendTooltip(Component.literal("Click to cycle fuel").withStyle(ChatFormatting.DARK_GRAY));
        widgets.addButton(slotX, ctrlY, 18, 18, 0, 0, () -> true, (mx, my, b) -> {
            fuelIndex = (fuelIndex + 1) % FissionFuelRodBlock.FissionFuelRodTypes.values().length;
            EmiApi.displayRecipe(this);
        });

        // Fuel count ± buttons + label
        stepButtons(widgets, slotX + 22, ctrlY + 1,
                val -> {
                    fuelCount = Math.max(1, fuelCount + val);
                    EmiApi.displayRecipe(this);
                });
        widgets.addText(Component.literal("×" + fuelCount + "  " + fuel.getName().replace("_", " "))
                .withStyle(ChatFormatting.WHITE), slotX + 38, ctrlY + 5, 0xFFFFFF, false);
        widgets.addText(Component.literal(fuel.getBaseHeatProduction() + " HU/t  bias +" + fuel.getNeutronBias())
                .withStyle(ChatFormatting.DARK_GRAY), slotX + 38, ctrlY + 13, 0xFFFFFF, false);

        ctrlY += 22;
        sectionLabel(widgets, "COOLER", slotX, ctrlY);
        ctrlY += 11;

        widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + cooler.getName()), slotX, ctrlY)
                .appendTooltip(Component.literal("Click to cycle cooler").withStyle(ChatFormatting.DARK_GRAY));
        widgets.addButton(slotX, ctrlY, 18, 18, 0, 0, () -> true, (mx, my, b) -> {
            coolerIndex = (coolerIndex + 1) % FissionCoolerBlock.FissionCoolerTypes.values().length;
            EmiApi.displayRecipe(this);
        });

        stepButtons(widgets, slotX + 22, ctrlY + 1,
                val -> {
                    coolerCount = Math.max(0, coolerCount + val);
                    EmiApi.displayRecipe(this);
                });

        String coolerDesc = coolerCount == 0 ? "No cooler" :
                "×" + coolerCount + "  −" + formatHeat(totalCool) + " HU/t";
        widgets.addText(Component.literal(coolerDesc).withStyle(ChatFormatting.WHITE), slotX + 38, ctrlY + 5, 0xFFFFFF,
                false);

        if (coolerCount > 0 && coolantMbt > 0) {
            widgets.addText(Component.literal(coolantMbt + " mB/t coolant").withStyle(ChatFormatting.DARK_GRAY),
                    slotX + 38, ctrlY + 13, 0xFFFFFF, false);
        }

        ctrlY += 22;
        sectionLabel(widgets, "BURN TIME", slotX, ctrlY);
        ctrlY += 11;

        stepButtons(widgets, slotX + 2, ctrlY,
                val -> {
                    burnTimeMin = Math.max(0, Math.min(20, burnTimeMin + val));
                    EmiApi.displayRecipe(this);
                });
        widgets.addText(Component.literal(burnTimeMin + " min  (×" + String.format("%.3f", burnMult) + ")")
                .withStyle(ChatFormatting.WHITE), slotX + 22, ctrlY + 4, 0xFFFFFF, false);
        widgets.addText(Component
                .literal(burnTimeMin == 0 ? "no ramp bonus" :
                        "+" + String.format("%.0f", (burnMult - 1) * 100) + "% heat & EU")
                .withStyle(ChatFormatting.DARK_GRAY), slotX + 22, ctrlY + 12, 0xFFFFFF, false);

        // ── Heat table ───────────────────────────────────────────────────────
        ctrlY += 24;
        sectionLabel(widgets, "HEAT LEVELS", slotX, ctrlY);
        ctrlY += 11;

        widgets.addText(Component.literal("Heat     Par    Gain/t   Net/t    EU/t")
                .withStyle(ChatFormatting.DARK_GRAY), slotX, ctrlY, 0xFFFFFF, false);
        ctrlY += 9;

        boolean everStable = false;
        int meltLevel = -1;

        for (int heat : HEAT_LEVELS) {
            int heatParBonus = heat / HEAT_PER_PAR;
            int parallels = Math.min(MAX_PARALLELS, (fuelCount * BASE_PAR) + finalModParBonus + heatParBonus);
            double heatGain = (fuel.getBaseHeatProduction() * fuelCount * finalModMult) * parallels * burnMult;
            double net = totalCool - heatGain;
            double euPerTick = euPerTick(heat, burnMult, euBoost);

            if (net >= 0) everStable = true;
            else if (meltLevel < 0) meltLevel = heat;

            ChatFormatting netColor = net > 0 ? ChatFormatting.GREEN :
                    net < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            String netStr = (net >= 0 ? "+" : "") + formatHeat((int) Math.round(net));

            widgets.addText(Component.literal(
                    padL(formatHeatShort(heat), 5) + "  " +
                            padL(parallels + "×", 5) + "  " +
                            padL(formatHeatShort((int) Math.round(heatGain)), 7) + "  ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(padL(netStr, 7)).withStyle(netColor))
                    .append(Component.literal("  " + formatHeatShort((int) Math.round(euPerTick)))
                            .withStyle(ChatFormatting.GOLD)),
                    slotX, ctrlY, 0xFFFFFF, false);
            ctrlY += 10;
        }

        // ── Status banner ─────────────────────────────────────────────────
        ctrlY += 3;
        if (meltLevel == 0) {
            // Unstable from cold start
            statusText(widgets, "⚠ FEEDBACK LOOP — melt on start", ChatFormatting.RED, ChatFormatting.DARK_RED, slotX,
                    ctrlY);
        } else if (!everStable) {
            statusText(widgets, "⚠ DANGER — cooling never sufficient", ChatFormatting.RED, ChatFormatting.DARK_RED,
                    slotX, ctrlY);
        } else if (meltLevel > 0) {
            statusText(widgets, "⚡ CEILING: unstable above " + formatHeatShort(meltLevel) + " HU", ChatFormatting.GOLD,
                    ChatFormatting.YELLOW, slotX, ctrlY);
        } else {
            statusText(widgets, "✔ STABLE — safe across all heat levels", ChatFormatting.GREEN,
                    ChatFormatting.DARK_GREEN, slotX, ctrlY);
        }

        // ── Mod summary (bottom of left col) ─────────────────────────────
        ctrlY += 12;
        widgets.addText(Component.literal(
                "Mod mult ×" + String.format("%.2f", modMult) + "  EU +" + euBoost + "%  disc −" + disc + "%")
                .withStyle(ChatFormatting.DARK_GRAY), slotX, ctrlY, 0xFFFFFF, false);

        // ════════════════════════════════════════════════════════════════════
        // RIGHT COLUMN — Moderator array (one row per mod type)
        // ════════════════════════════════════════════════════════════════════

        int modY = 15;
        sectionLabel(widgets, "MODERATOR ARRAY", RIGHT_X, modY);
        modY += 11;

        for (int mi = 1; mi < modTypes.length; mi++) { // skip index 0 = None
            var mod = modTypes[mi];
            int cnt = modCounts[mi];
            final int fi = mi; // effectively final for lambda

            // Icon slot (click to toggle between 0 and 1 as a convenience)
            widgets.addSlot(FuelRodEmiRecipe.getEmiStackFromId("phoenixcore:" + mod.getName()), RIGHT_X, modY)
                    .drawBack(cnt > 0);

            // ± step buttons
            stepButtons(widgets, RIGHT_X + 22, modY,
                    val -> {
                        modCounts[fi] = Math.max(0, modCounts[fi] + val);
                        EmiApi.displayRecipe(this);
                    });

            // Count + stat summary
            widgets.addText(
                    Component.literal("×" + cnt).withStyle(cnt > 0 ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                    RIGHT_X + 40, modY + 2, 0xFFFFFF, false);
            widgets.addText(
                    Component.literal(mod.getName().replace("_", " ")).withStyle(ChatFormatting.GRAY),
                    RIGHT_X + 56, modY + 2, 0xFFFFFF, false);

            // Per-block stats on second line
            widgets.addText(Component.literal(
                    "EU+" + mod.getEUBoost() + "%  disc−" + mod.getFuelDiscount() + "%  T" + mod.getTier())
                    .withStyle(ChatFormatting.DARK_GRAY), RIGHT_X + 40, modY + 11, 0xFFFFFF, false);

            modY += 20;
        }

        // Aggregated moderator result
        modY += 4;
        sectionLabel(widgets, "ARRAY TOTALS", RIGHT_X, modY);
        modY += 11;
        widgets.addText(Component.literal("Mod mult: ×" + String.format("%.3f", modMult))
                .withStyle(ChatFormatting.WHITE), RIGHT_X, modY, 0xFFFFFF, false);
        modY += 10;
        widgets.addText(Component.literal("Par bonus: +" + modParBonus)
                .withStyle(ChatFormatting.AQUA), RIGHT_X, modY, 0xFFFFFF, false);
        modY += 10;
        widgets.addText(
                Component.literal("EU boost: +" + euBoost + "%" + (totalEuPct > MAX_EU_BOOST ? " (capped)" : ""))
                        .withStyle(ChatFormatting.GOLD),
                RIGHT_X, modY, 0xFFFFFF, false);
        modY += 10;
        widgets.addText(
                Component.literal("Fuel disc: −" + disc + "%" + (totalDiscPct > MAX_FUEL_DISC ? " (capped)" : ""))
                        .withStyle(ChatFormatting.GREEN),
                RIGHT_X, modY, 0xFFFFFF, false);

        // Anchor EMI's own buttons in the bottom-right corner
        widgets.addSlot(EmiStack.EMPTY, W - 20, getDisplayHeight() - 20).drawBack(false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Mirrors the HTML eut() function. */
    private double euPerTick(int heat, double burnMult, int euBoostPct) {
        if (heat <= 0) return 0;
        double hf = (double) heat / MAX_SAFE_HEAT;
        double x = Math.max(0, Math.min(1, hf));
        double dangerBonus = 1.0 + Math.pow(x, 2.0) * 1.5;
        return heat * dangerBonus * burnMult * (1.0 + euBoostPct / 100.0);
    }

    private void sectionLabel(WidgetHolder w, String text, int x, int y) {
        w.addText(Component.literal(text).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD),
                x, y, 0xFFFFFF, false);
    }

    private void statusText(WidgetHolder w, String text, ChatFormatting primary, ChatFormatting bg, int x, int y) {
        w.addText(Component.literal(text).withStyle(primary), x, y, 0xFFFFFF, false);
    }

    /**
     * Two small ▲/▼ buttons stacked at (x, y), (x, y+11).
     * Each click fires action.accept(+1) or action.accept(-1).
     */
    private void stepButtons(WidgetHolder widgets, int x, int y, java.util.function.Consumer<Integer> action) {
        widgets.addButton(x, y, 14, 9, 0, 0, () -> true, (mx, my, b) -> action.accept(1));
        widgets.addButton(x, y + 10, 14, 9, 0, 0, () -> true, (mx, my, b) -> action.accept(-1));
    }

    /** Format as "100k", "4.8k", "850", etc. */
    private String formatHeatShort(int v) {
        if (Math.abs(v) >= 1000) return String.format("%.0fk", v / 1000.0);
        return String.valueOf(v);
    }

    /** Format as "10k", "10,000", keeping sign */
    private String formatHeat(int v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (Math.abs(v) >= 1000 && v % 1000 == 0) return (v / 1000) + "k";
        if (Math.abs(v) >= 1000) return String.format("%.1fk", v / 1000.0);
        return String.valueOf(v);
    }

    /** Right-pad a string to width with spaces. */
    private String padL(String s, int width) {
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
