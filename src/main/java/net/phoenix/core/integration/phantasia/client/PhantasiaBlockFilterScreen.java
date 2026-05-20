package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phantasia.PhantasiaLoadedPattern;
import net.phoenix.core.integration.phantasia.PhantasiaScript;

import java.util.*;

/**
 * PhantasiaBlockFilterScreen
 *
 * Opened from the "Block List" button in PhantasiaSceneScreen.
 * Three tabs:
 * FILTER — toggle which block categories are visible in the scene
 * LIST — shopping list (all block types + count)
 * INSPECT — inspect a clicked block
 *
 * FIX (B5/B6): onClose() now calls applyVisibility() on the parent PhantasiaSceneScreen
 * so filter changes are immediately reflected in the 3D scene on return, with no extra click needed.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaBlockFilterScreen extends Screen {

    // ── Colors (unified with Phantasia theme) ─────────────────────────────────
    private static final int C_BG = 0xFF080810;
    private static final int C_PANEL = 0xEE0C0C1A;
    private static final int C_ACCENT = 0xFF4FC3F7;
    private static final int C_BTN = 0xBB151528;
    private static final int C_BTN_HOV = 0xBB1A2840;
    private static final int C_BTN_ACT = 0xBB0D3050;
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF667788;
    private static final int C_WARN = 0xFFFFB74D;
    private static final int C_GREEN = 0xFF66BB6A;

    private enum Tab {
        FILTER,
        LIST,
        INSPECT
    }

    private final Screen parent;
    private final PhantasiaLoadedPattern pattern;
    private final PhantasiaScript script;

    // The filter that is active when this screen was opened — updated live as the user clicks
    private PhantasiaSceneScreen.ViewFilter activeFilter;

    private Tab tab = Tab.FILTER;
    private int listScrollY = 0;

    // Inspect state
    private BlockPos inspectedWorldPos = null;

    // Pre-built category sets (computed once in constructor)
    private final Set<BlockPos> hatchBusSet;
    private final Set<BlockPos> energySet;
    private final Map<String, List<BlockPos>> blocksByName;

    public PhantasiaBlockFilterScreen(PhantasiaLoadedPattern pattern,
                                      PhantasiaScript script,
                                      PhantasiaSceneScreen.ViewFilter currentFilter,
                                      Screen parent) {
        super(Component.literal("Block Filter"));
        this.parent = parent;
        this.pattern = pattern;
        this.script = script;
        this.activeFilter = currentFilter;

        // Build category sets
        Set<BlockPos> hb = new HashSet<>(), en = new HashSet<>();
        Map<String, List<BlockPos>> byName = new LinkedHashMap<>();

        if (PhantasiaSceneScreen.SHARED_LEVEL != null) {
            for (Map.Entry<BlockPos, BlockPos> e : pattern.localToWorld.entrySet()) {
                BlockPos wp = e.getValue();
                BlockState state = null;
                try {
                    state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                } catch (Exception ignored) {}
                if (state == null || state.isAir()) continue;

                // Shopping list
                String name = state.getBlock().getName().getString();
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(wp);

                if (!(state.getBlock() instanceof MetaMachineBlock)) continue;
                if (wp.equals(pattern.controllerWorldPos)) continue;

                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (rl == null) continue;
                String path = rl.getPath();

                if (path.contains("hatch") || path.contains("bus") || path.contains("muffler") ||
                        path.contains("maintenance"))
                    hb.add(wp);

                if (path.contains("energy") || path.contains("dynamo") || path.contains("laser") ||
                        path.contains("power"))
                    en.add(wp);
            }
        }
        this.hatchBusSet = Collections.unmodifiableSet(hb);
        this.energySet = Collections.unmodifiableSet(en);

        // Sort shopping list by count desc
        List<Map.Entry<String, List<BlockPos>>> sorted = new ArrayList<>(byName.entrySet());
        sorted.sort((a, b) -> b.getValue().size() - a.getValue().size());
        Map<String, List<BlockPos>> ordered = new LinkedHashMap<>();
        for (var entry : sorted) ordered.put(entry.getKey(), entry.getValue());
        this.blocksByName = Collections.unmodifiableMap(ordered);
    }

    /** Called by PhantasiaSceneScreen when the player right-clicks a block in the scene. */
    public void setInspectedPos(BlockPos worldPos) {
        this.inspectedWorldPos = worldPos;
        this.tab = Tab.INSPECT;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, C_BG);

        // Header
        g.fill(0, 0, this.width, 22, 0xCC0A0A14);
        g.fill(0, 21, this.width, 22, C_ACCENT);
        g.drawString(font, "Block Filter — " + pattern.shoppingList.size() + " block types", 8, 7, C_ACCENT, false);

        renderTabBar(g, mx, my);

        switch (tab) {
            case FILTER -> renderFilterTab(g, mx, my);
            case LIST -> renderListTab(g, mx, my);
            case INSPECT -> renderInspectTab(g, mx, my);
        }

        // Back button
        int bw = 80, bh = 18;
        int bx = (this.width - bw) / 2, by = this.height - bh - 6;
        boolean bHov = isOver(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, bHov ? C_BTN_HOV : C_BTN);
        if (bHov) {
            g.fill(bx, by, bx + bw, by + 1, C_ACCENT);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, C_ACCENT);
        }
        g.drawString(font, "\u2190 Back",
                bx + (bw - font.width("\u2190 Back")) / 2, by + 5,
                bHov ? C_ACCENT : C_TEXT, false);
    }

    private void renderTabBar(GuiGraphics g, int mx, int my) {
        int y = 24, tw = 80, th = 16;
        Tab[] tabs = Tab.values();
        String[] labels = { "Filter", "Shopping", "Inspect" };
        for (int i = 0; i < tabs.length; i++) {
            int tx = 8 + i * (tw + 4);
            boolean active = tab == tabs[i];
            boolean hov = isOver(mx, my, tx, y, tw, th);
            g.fill(tx, y, tx + tw, y + th, active ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
            g.fill(tx, y + th - 1, tx + tw, y + th, active ? C_ACCENT : 0x33FFFFFF);
            g.drawString(font, labels[i],
                    tx + (tw - font.width(labels[i])) / 2, y + 4,
                    active ? C_ACCENT : C_TEXT, false);
        }
    }

    // ── FILTER TAB ────────────────────────────────────────────────────────────

    private void renderFilterTab(GuiGraphics g, int mx, int my) {
        int y = 50;
        int bw = 200, x = (this.width - bw) / 2;

        g.drawCenteredString(font, "Select which blocks to highlight", this.width / 2, y, C_DIM);
        y += 18;

        PhantasiaSceneScreen.ViewFilter[] vfs = PhantasiaSceneScreen.ViewFilter.values();
        String[] descs = {
                "Show all blocks",
                "Hatches, buses, muffler, maintenance",
                "Energy & dynamo hatches, laser I/O",
                "All blocks with a block entity",
                "The controller block only"
        };

        for (int i = 0; i < vfs.length; i++) {
            boolean active = activeFilter == vfs[i];
            boolean hov = isOver(mx, my, x, y, bw, 22);
            g.fill(x, y, x + bw, y + 22, active ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
            if (active) {
                g.fill(x, y, x + bw, y + 1, C_ACCENT);
                g.fill(x, y + 21, x + bw, y + 22, C_ACCENT);
            }
            g.drawString(font, vfs[i].name(), x + 6, y + 4, active ? C_ACCENT : C_TEXT, false);
            g.drawString(font, trunc(descs[i], bw - 12), x + 6, y + 13, C_DIM, false);
            y += 26;
        }

        y += 8;
        g.fill(x, y, x + bw, y + 1, 0x33FFFFFF);
        y += 8;

        // Common mistakes toggle
        if (script.hasCommonMistakes()) {
            boolean showM = parent instanceof PhantasiaSceneScreen pss && pss.showMistakes;
            boolean hov = isOver(mx, my, x, y, bw, 18);
            g.fill(x, y, x + bw, y + 18, showM ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
            if (showM) g.fill(x, y, x + bw, y + 1, C_WARN);
            g.drawString(font, (showM ? "✓ " : "  ") + "Show Common Mistakes",
                    x + 6, y + 5, showM ? C_WARN : C_TEXT, false);
            y += 22;
        }

        // Heatmap tiers
        if (!script.getHeatmapTiers().isEmpty()) {
            g.drawString(font, "Heatmap Layers:", x + 6, y, C_DIM, false);
            y += 12;
            int sel = parent instanceof PhantasiaSceneScreen pss ? pss.selectedTierIndex : -1;
            for (int i = 0; i < script.getHeatmapTiers().size(); i++) {
                PhantasiaScript.HeatmapTier tier = script.getHeatmapTiers().get(i);
                boolean active = sel == i;
                boolean hov = isOver(mx, my, x, y, bw, 14);
                g.fill(x, y, x + bw, y + 14, active ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
                g.fill(x, y, x + 4, y + 14, tier.color());
                g.drawString(font, tier.name(), x + 8, y + 3, active ? C_ACCENT : C_TEXT, false);
                y += 16;
            }
            boolean disHov = isOver(mx, my, x, y, bw, 14);
            g.fill(x, y, x + bw, y + 14, sel == -1 ? C_BTN_ACT : (disHov ? C_BTN_HOV : C_BTN));
            g.drawString(font, "Disable Heatmap", x + 8, y + 3, sel == -1 ? C_ACCENT : C_DIM, false);
            y += 16;
        }

        y += 6;
        // Apply & Return button
        int aw = 120, ax = (this.width - aw) / 2;
        boolean aHov = isOver(mx, my, ax, y, aw, 18);
        g.fill(ax, y, ax + aw, y + 18, aHov ? C_BTN_HOV : C_BTN);
        if (aHov) g.fill(ax, y, ax + aw, y + 1, C_ACCENT);
        g.drawString(font, "Apply & Return",
                ax + (aw - font.width("Apply & Return")) / 2, y + 5,
                aHov ? C_ACCENT : C_TEXT, false);
    }

    // ── LIST TAB ──────────────────────────────────────────────────────────────

    private void renderListTab(GuiGraphics g, int mx, int my) {
        int startY = 50;
        int contentH = this.height - startY - 36;
        int x = 12, w = this.width - 24;

        g.fill(0, startY, this.width, startY + contentH, 0x22FFFFFF);

        int y = startY - listScrollY;
        for (Map.Entry<String, List<BlockPos>> e : blocksByName.entrySet()) {
            if (y + 18 >= startY && y <= startY + contentH) {
                boolean hov = isOver(mx, my, x, y, w, 18);
                g.fill(x, y, x + w, y + 18, hov ? C_BTN_HOV : 0x00000000);
                if (hov) g.fill(x, y, x + w, y + 1, 0x33FFFFFF);

                // Count badge
                String cnt = String.valueOf(e.getValue().size());
                int cntW = font.width(cnt) + 8;
                g.fill(x, y + 1, x + cntW, y + 17, 0xBB1A2840);
                g.drawString(font, cnt, x + 4, y + 5, C_ACCENT, false);

                g.drawString(font, trunc(e.getKey(), w - cntW - 8), x + cntW + 4, y + 5, C_TEXT, false);
            }
            y += 20;
        }

        // Scroll indicator
        int total = blocksByName.size() * 20;
        if (total > contentH) {
            int trackH = contentH - 4;
            int thumbH = Math.max(20, trackH * contentH / total);
            int thumbY = startY + 2 + (trackH - thumbH) * listScrollY / Math.max(1, total - contentH);
            g.fill(this.width - 6, startY + 2, this.width - 2, startY + 2 + trackH, 0x33FFFFFF);
            g.fill(this.width - 6, thumbY, this.width - 2, thumbY + thumbH, C_ACCENT);
        }
    }

    // ── INSPECT TAB (Enhanced Technical Visual Style) ─────────────────────────

    // ── INSPECT TAB (Enhanced Technical Visual Style with Full Tooltips) ─────

    private void renderInspectTab(GuiGraphics g, int mx, int my) {
        int leftCol = 24;
        int leftColWidth = 120;
        int rightCol = 160;
        int maxRightWidth = this.width - rightCol - 24;
        int y = 55;

        if (inspectedWorldPos == null) {
            g.drawCenteredString(font, "Click a block in the Shopping tab to inspect it.",
                    this.width / 2, y + 20, C_DIM);
            return;
        }

        if (PhantasiaSceneScreen.SHARED_LEVEL == null) return;

        BlockState state = null;
        try {
            state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(inspectedWorldPos);
        } catch (Exception ignored) {}

        if (state == null || state.isAir()) {
            g.drawCenteredString(font, "No block at this position.", this.width / 2, y + 20, C_DIM);
            return;
        }

        net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(
                state.getBlock().asItem());

        // --- LEFT COLUMN: IDENTITY & STRUCTURAL DATA ---
        // Render large Block Icon
        g.pose().pushPose();
        g.pose().translate(leftCol, y, 100);
        g.pose().scale(4.0f, 4.0f, 1.0f);
        g.renderFakeItem(new net.minecraft.world.item.ItemStack(state.getBlock()), 0, 0);
        g.pose().popPose();

        y += 75;

        // Wrapped Block Name
        String name = state.getBlock().getName().getString();
        for (net.minecraft.util.FormattedCharSequence seq : font.split(Component.literal(name), leftColWidth)) {
            g.drawString(font, seq, leftCol, y, 0xFFFFFFFF, false);
            y += 10;
        }
        y += 4;

        // Wrapped Registry ID
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (rl != null) {
            for (net.minecraft.util.FormattedCharSequence seq : font.split(
                    Component.literal(rl.toString()).withStyle(net.minecraft.ChatFormatting.ITALIC), leftColWidth)) {
                g.drawString(font, seq, leftCol, y, C_DIM, false);
                y += 10;
            }
        }

        y += 16;
        g.drawString(font, "COORDINATES", leftCol, y, C_ACCENT, false);
        y += 12;
        g.drawString(font, "X: " + inspectedWorldPos.getX(), leftCol, y, C_TEXT, false);
        g.drawString(font, "Y: " + inspectedWorldPos.getY(), leftCol + 40, y, C_TEXT, false);
        g.drawString(font, "Z: " + inspectedWorldPos.getZ(), leftCol + 80, y, C_TEXT, false);

        // --- RIGHT COLUMN: ATTRIBUTES, TOOLTIPS, & EMI ACTION ---
        y = 60;
        g.drawString(font, "SPECIFICATIONS & UTILITY", rightCol, y - 15, C_ACCENT, false);

        // Dynamic Tag/Role Badges
        if (pattern.hasBlockEntity(inspectedWorldPos)) {
            g.drawString(font, "\u26A1 Has Block Entity", rightCol, y, C_WARN, false);
            y += 12;
        }
        if (inspectedWorldPos.equals(pattern.controllerWorldPos)) {
            g.drawString(font, "\u2605 Multiblock Controller", rightCol, y, C_ACCENT, false);
            y += 12;
        }
        if (hatchBusSet.contains(inspectedWorldPos)) {
            g.drawString(font, "\uD83D\uDD17 Component: Hatch / Bus", rightCol, y, C_GREEN, false);
            y += 12;
        }
        if (energySet.contains(inspectedWorldPos)) {
            g.drawString(font, "\u26A1 System: Energy I/O", rightCol, y, C_WARN, false);
            y += 12;
        }

        y += 6;

        // Dynamic Tooltip Lines Extraction
        if (!itemStack.isEmpty()) {
            List<Component> tooltips = itemStack.getTooltipLines(Minecraft.getInstance().player,
                    net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            for (Component line : tooltips) {
                for (net.minecraft.util.FormattedCharSequence sequence : font.split(line, maxRightWidth)) {
                    g.drawString(font, sequence, rightCol, y, C_TEXT, false);
                    y += 10;
                }
                y += 2;
                if (y > this.height - 85) break;
            }
        }

        y += 8;

        // BlockState Property Grid
        if (!state.getProperties().isEmpty()) {
            g.fill(rightCol, y, rightCol + 120, y + 1, 0x22FFFFFF);
            y += 8;
            g.drawString(font, "BLOCKSTATE PROPERTIES", rightCol, y, C_DIM, false);
            y += 12;

            for (Property<?> prop : state.getProperties()) {
                Comparable<?> val = state.getValue(prop);
                String line = prop.getName() + " = " + getPropName(prop, val);
                for (net.minecraft.util.FormattedCharSequence seq : font.split(Component.literal(line),
                        maxRightWidth)) {
                    g.drawString(font, seq, rightCol, y, C_TEXT, false);
                    y += 10;
                }
                if (y > this.height - 75) break;
            }
        }

        y += 14;

        // --- ACTION BUTTONS (EMI RECIPES & CLEAR) ---
        if (!itemStack.isEmpty()) {
            boolean emiHov = isOver(mx, my, rightCol, y, 90, 16);
            g.fill(rightCol, y, rightCol + 90, y + 16, emiHov ? C_BTN_HOV : C_BTN);
            if (emiHov) g.fill(rightCol, y, rightCol + 90, y + 1, C_ACCENT);
            g.drawString(font, "EMI Recipes", rightCol + (90 - font.width("EMI Recipes")) / 2, y + 4,
                    emiHov ? C_ACCENT : C_TEXT, false);

            boolean clrHov = isOver(mx, my, rightCol + 96, y, 60, 16);
            g.fill(rightCol + 96, y, rightCol + 156, y + 16, clrHov ? C_BTN_HOV : C_BTN);
            g.drawString(font, "Clear", rightCol + 96 + (60 - font.width("Clear")) / 2, y + 4,
                    clrHov ? C_WARN : C_DIM, false);
        } else {
            boolean clrHov = isOver(mx, my, rightCol, y, 70, 16);
            g.fill(rightCol, y, rightCol + 70, y + 16, clrHov ? C_BTN_HOV : C_BTN);
            g.drawString(font, "Clear", rightCol + (70 - font.width("Clear")) / 2, y + 4,
                    clrHov ? C_WARN : C_DIM, false);
        }
    }    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Tab bar
        Tab[] tabs = Tab.values();
        int tw = 80, th = 16, ty = 24;
        for (int i = 0; i < tabs.length; i++) {
            int tx = 8 + i * (tw + 4);
            if (isOver((int) mx, (int) my, tx, ty, tw, th)) {
                tab = tabs[i];
                return true;
            }
        }

        // Back button (centred)
        int bw = 80, bh = 18;
        int bx = (this.width - bw) / 2, by = this.height - bh - 6;
        if (isOver((int) mx, (int) my, bx, by, bw, bh)) {
            onClose();
            return true;
        }

        switch (tab) {
            case FILTER -> {
                int fw = 200, fx = (this.width - fw) / 2;
                int y = 68;

                PhantasiaSceneScreen.ViewFilter[] vfs = PhantasiaSceneScreen.ViewFilter.values();
                for (PhantasiaSceneScreen.ViewFilter vf : vfs) {
                    if (isOver((int) mx, (int) my, fx, y, fw, 22)) {
                        // Toggle: clicking the active filter deactivates it (returns to ALL)
                        activeFilter = (activeFilter == vf) ? PhantasiaSceneScreen.ViewFilter.ALL : vf;
                        // FIX (B5/B6): push the change into the parent scene immediately
                        if (parent instanceof PhantasiaSceneScreen pss) {
                            pss.applyViewFilter(activeFilter);
                        }
                        return true;
                    }
                    y += 26;
                }
                y += 16; // separator

                // Common mistakes toggle
                if (script.hasCommonMistakes()) {
                    if (isOver((int) mx, (int) my, fx, y, fw, 18)) {
                        if (parent instanceof PhantasiaSceneScreen pss) pss.showMistakes = !pss.showMistakes;
                        return true;
                    }
                    y += 22;
                }

                // Heatmap tiers
                if (!script.getHeatmapTiers().isEmpty()) {
                    y += 12;
                    for (int i = 0; i < script.getHeatmapTiers().size(); i++) {
                        if (isOver((int) mx, (int) my, fx, y, fw, 14)) {
                            if (parent instanceof PhantasiaSceneScreen pss) pss.selectedTierIndex = i;
                            return true;
                        }
                        y += 16;
                    }
                    if (isOver((int) mx, (int) my, fx, y, fw, 14)) {
                        if (parent instanceof PhantasiaSceneScreen pss) pss.selectedTierIndex = -1;
                        return true;
                    }
                    y += 16;
                }

                // Apply & Return
                y += 6;
                int aw = 120, ax = (this.width - aw) / 2;
                if (isOver((int) mx, (int) my, ax, y, aw, 18)) {
                    onClose();
                    return true;
                }
            }

            case LIST -> {
                int startY = 50, contentH = this.height - startY - 36;
                int y = startY - listScrollY, x = 12, w = this.width - 24;
                for (Map.Entry<String, List<BlockPos>> e : blocksByName.entrySet()) {
                    if (y + 18 >= startY && y <= startY + contentH) {
                        if (isOver((int) mx, (int) my, x, y, w, 18) && !e.getValue().isEmpty()) {
                            inspectedWorldPos = e.getValue().get(0);
                            tab = Tab.INSPECT;
                            return true;
                        }
                    }
                    y += 20;
                }
            }

            case INSPECT -> {
                if (inspectedWorldPos != null && PhantasiaSceneScreen.SHARED_LEVEL != null) {
                    try {
                        BlockState state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(inspectedWorldPos);
                        if (!state.isAir()) {
                            int rightCol = 160;
                            int maxRightWidth = this.width - rightCol - 24;
                            int y = 55;

                            if (pattern.hasBlockEntity(inspectedWorldPos)) y += 12;
                            if (inspectedWorldPos.equals(pattern.controllerWorldPos)) y += 12;
                            if (hatchBusSet.contains(inspectedWorldPos)) y += 12;
                            if (energySet.contains(inspectedWorldPos)) y += 12;
                            y += 6;

                            net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(
                                    state.getBlock().asItem());
                            if (!itemStack.isEmpty()) {
                                List<Component> tooltips = itemStack.getTooltipLines(Minecraft.getInstance().player,
                                        net.minecraft.world.item.TooltipFlag.Default.NORMAL);
                                for (Component line : tooltips) {
                                    int lines = font.split(line, maxRightWidth).size();
                                    y += (lines * 10) + 2;
                                    if (y > this.height - 85) break;
                                }
                            }

                            y += 8;

                            if (!state.getProperties().isEmpty()) {
                                y += 20;
                                for (Property<?> prop : state.getProperties()) {
                                    int lines = font.split(
                                            Component.literal(
                                                    prop.getName() + " = " + getPropName(prop, state.getValue(prop))),
                                            maxRightWidth).size();
                                    y += lines * 10;
                                    if (y > this.height - 75) break;
                                }
                            }
                            y += 14;

                            if (!itemStack.isEmpty()) {
                                if (isOver((int) mx, (int) my, rightCol, y, 90, 16)) {
                                    if (dev.emi.emi.api.EmiApi.getRecipeManager() != null) {
                                        dev.emi.emi.api.EmiApi
                                                .displayRecipes(dev.emi.emi.api.stack.EmiStack.of(itemStack));
                                    }
                                    return true;
                                }
                                if (isOver((int) mx, (int) my, rightCol + 96, y, 60, 16)) {
                                    inspectedWorldPos = null;
                                    return true;
                                }
                            } else {
                                if (isOver((int) mx, (int) my, rightCol, y, 70, 16)) {
                                    inspectedWorldPos = null;
                                    return true;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == Tab.LIST) {
            int maxScroll = Math.max(0, blocksByName.size() * 20 - (this.height - 86));
            listScrollY = Math.max(0, Math.min(maxScroll, listScrollY + (delta > 0 ? -15 : 15)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    /**
     * FIX (B5/B6): Call applyVisibility() on the parent scene so whatever filter state
     * was set during this screen is immediately applied when returning to the scene.
     */
    @Override
    public void onClose() {
        if (parent instanceof PhantasiaSceneScreen pss) {
            pss.applyViewFilter(activeFilter);
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trunc(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) s = s.substring(0, s.length() - 2) + "\u2026";
        return s;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getPropName(Property<?> p, Comparable<?> v) {
        try {
            return ((Property<T>) p).getName((T) v);
        } catch (Exception e) {
            return v.toString();
        }
    }
}
