package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * PhantasiaBlockFilterScreen
 *
 * Opened from the "Show Blocks" button in PhantasiaSceneScreen.
 * Three tabs:
 * FILTER — toggle which block categories are visible in the scene
 * LIST — shopping list (all block types + count)
 * INSPECT — inspect a clicked block (also reachable via right-click in scene)
 *
 * The filter selection is passed back to PhantasiaSceneScreen on close.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaBlockFilterScreen extends Screen {

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
    private PhantasiaSceneScreen.ViewFilter activeFilter;

    private Tab tab = Tab.FILTER;
    private int listScrollY = 0;

    // Inspect state
    private BlockPos inspectedWorldPos = null; // set by right-click from scene or click in filter list

    // Pre-built category sets (computed once)
    private final Set<BlockPos> hatchBusSet;
    private final Set<BlockPos> energySet;
    private final Map<String, List<BlockPos>> blocksByName; // for shopping list with positions

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

                // Shopping list grouping
                String name = state.getBlock().getName().getString();
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(wp);

                if (!(state.getBlock() instanceof MetaMachineBlock)) continue;
                if (wp.equals(pattern.controllerWorldPos)) continue;

                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (rl == null) continue;
                String path = rl.getPath();

                if (path.contains("hatch") || path.contains("bus") || path.contains("muffler") ||
                        path.contains("maintenance")) {
                    hb.add(wp);
                }
                if (path.contains("energy") || path.contains("dynamo") || path.contains("laser") ||
                        path.contains("power")) {
                    en.add(wp);
                }
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

    // Allow the scene screen to set the inspect target when player right-clicks scene
    public void setInspectedPos(BlockPos worldPos) {
        this.inspectedWorldPos = worldPos;
        this.tab = Tab.INSPECT;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, C_BG);

        // Header
        g.fill(0, 0, this.width, 22, 0xCC0A0A14);
        g.fill(0, 21, this.width, 22, C_ACCENT);
        g.drawString(font, "Block Filter — " + pattern.shoppingList.size() + " block types", 8, 7, C_ACCENT, false);

        // Tab bar
        renderTabBar(g, mx, my);

        // Content
        switch (tab) {
            case FILTER -> renderFilterTab(g, mx, my);
            case LIST -> renderListTab(g, mx, my);
            case INSPECT -> renderInspectTab(g, mx, my);
        }

        // Back button
        int bw = 80, bh = 18;
        int bx = this.width - bw - 8, by = this.height - bh - 6;
        boolean bHov = isOver(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, bHov ? C_BTN_HOV : C_BTN);
        if (bHov) {
            g.fill(bx, by, bx + bw, by + 1, C_ACCENT);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, C_ACCENT);
        }
        g.drawString(font, "\u2190 Back", bx + (bw - font.width("\u2190 Back")) / 2, by + 5, bHov ? C_ACCENT : C_TEXT,
                false);
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
            g.drawString(font, labels[i], tx + (tw - font.width(labels[i])) / 2, y + 4, active ? C_ACCENT : C_TEXT,
                    false);
        }
    }

    // ── FILTER TAB ────────────────────────────────────────────────────────────

    private void renderFilterTab(GuiGraphics g, int mx, int my) {
        int y = 50, bw = 200, x = (this.width - bw) / 2;

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
            String label = vfs[i].name().replace("_", " ");
            g.drawString(font, label, x + 8, y + 4, active ? C_ACCENT : C_TEXT, false);
            g.drawString(font, descs[i], x + 8, y + 13, C_DIM, false);

            // Count for this filter
            int count = countForFilter(vfs[i]);
            String countStr = count + " blocks";
            g.drawString(font, countStr, x + bw - font.width(countStr) - 6, y + 7, C_DIM, false);
            y += 26;
        }

        // --- ADDED: Common Mistakes Toggle ---
        if (script.hasCommonMistakes()) {
            boolean active = ((PhantasiaSceneScreen) parent).showMistakes;
            boolean hov = isOver(mx, my, x, y, bw, 18);
            g.fill(x, y, x + bw, y + 18, active ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
            g.drawString(font, "⚠ Show Common Mistakes", x + 8, y + 5, active ? C_ACCENT : C_TEXT, false);
            y += 22;
        }

        // --- ADDED: Heatmap Tiers ---
        if (!script.getHeatmapTiers().isEmpty()) {
            g.drawString(font, "Heatmap Layers:", x, y, C_DIM, false);
            y += 12;
            for (int i = 0; i < script.getHeatmapTiers().size(); i++) {
                var tier = script.getHeatmapTiers().get(i);
                boolean active = ((PhantasiaSceneScreen) parent).selectedTierIndex == i;
                boolean hov = isOver(mx, my, x, y, bw, 14);

                g.fill(x, y, x + bw, y + 14, active ? C_BTN_ACT : (hov ? C_BTN_HOV : C_BTN));
                // Use the tier's defined color for the label if active
                g.drawString(font, "• " + tier.name(), x + 8, y + 3, active ? tier.color() : C_TEXT, false);
                y += 16;
            }

            // Button to disable heatmap
            boolean noneActive = ((PhantasiaSceneScreen) parent).selectedTierIndex == -1;
            boolean nHov = isOver(mx, my, x, y, bw, 14);
            g.fill(x, y, x + bw, y + 14, noneActive ? C_BTN_ACT : (nHov ? C_BTN_HOV : C_BTN));
            g.drawString(font, "• Disable Heatmap", x + 8, y + 3, noneActive ? C_ACCENT : C_TEXT, false);
            y += 16;
        }

        // Apply button
        y += 6;
        int aw = 120;
        int ax = (this.width - aw) / 2;
        boolean aHov = isOver(mx, my, ax, y, aw, 18);
        g.fill(ax, y, ax + aw, y + 18, aHov ? C_BTN_HOV : C_BTN_ACT);
        if (aHov) {
            g.fill(ax, y, ax + aw, y + 1, C_ACCENT);
        }
        g.drawString(font, "Apply & Return", ax + (aw - font.width("Apply & Return")) / 2, y + 5, C_ACCENT, false);
    }

    private int countForFilter(PhantasiaSceneScreen.ViewFilter vf) {
        return switch (vf) {
            case ALL -> pattern.localToWorld.size();
            case HATCHES_BUSES -> hatchBusSet.size();
            case ENERGY_IO -> energySet.size();
            case BLOCK_ENTITIES -> pattern.blockEntityWorldPos.size();
            case CONTROLLER -> pattern.controllerWorldPos != null ? 1 : 0;
        };
    }

    // ── LIST TAB ─────────────────────────────────────────────────────────────

    private void renderListTab(GuiGraphics g, int mx, int my) {
        int startY = 50, contentH = this.height - startY - 36;
        int y = startY - listScrollY;
        int x = 12, w = this.width - 24;
        int total = 0;

        // Scroll clip — only draw inside content area
        // (GuiGraphics doesn't have scissors easily without PoseStack, so we just skip out-of-range rows)
        for (Map.Entry<String, List<BlockPos>> e : blocksByName.entrySet()) {
            int count = e.getValue().size();
            total += count;
            if (y + 18 < startY || y > startY + contentH) {
                y += 20;
                continue;
            }

            boolean hov = isOver(mx, my, x, y, w, 18);
            g.fill(x, y, x + w, y + 18, hov ? C_BTN_HOV : C_BTN);

            // Color bar for quick visual scan
            int barColor = blocksByName.size() > 0 ?
                    colorForCount(count, blocksByName.values().stream().mapToInt(List::size).max().orElse(1)) : C_DIM;
            g.fill(x, y, x + 4, y + 18, barColor);

            g.drawString(font, e.getKey(), x + 8, y + 5, C_TEXT, false);
            String cs = count + "\u00D7";
            g.drawString(font, cs, x + w - font.width(cs) - 4, y + 5, C_ACCENT, false);
            y += 20;
        }

        // Separator + total
        int totalY = startY + contentH + 2;
        g.fill(8, totalY, this.width - 8, totalY + 1, 0x44FFFFFF);
        g.drawString(font, "Total: " + total + " blocks", 12, totalY + 4, C_DIM, false);

        // Scroll hint
        if (blocksByName.size() * 20 > contentH) {
            g.drawString(font, "\u25B2\u25BC scroll", this.width - 60, startY + 4, C_DIM, false);
        }
    }

    private int colorForCount(int count, int max) {
        float t = max > 1 ? (float) count / max : 1f;
        // Lerp slate → accent blue
        int r = (int) (0x15 + t * (0x4F - 0x15));
        int gr = (int) (0x28 + t * (0xC3 - 0x28));
        int b = (int) (0x40 + t * (0xF7 - 0x40));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    // ── INSPECT TAB ───────────────────────────────────────────────────────────

    private void renderInspectTab(GuiGraphics g, int mx, int my) {
        int y = 50, x = 12, w = this.width - 24;

        if (inspectedWorldPos == null) {
            g.drawCenteredString(font, "Right-click a block in the scene to inspect it.", this.width / 2, y + 20,
                    C_DIM);
            g.drawCenteredString(font, "Or return and right-click directly.", this.width / 2, y + 32, C_DIM);
            return;
        }

        if (PhantasiaSceneScreen.SHARED_LEVEL == null) return;
        BlockState state;
        try {
            state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(inspectedWorldPos);
        } catch (Exception e) {
            return;
        }

        if (state.isAir()) {
            inspectedWorldPos = null;
            return;
        }

        // Name
        g.fill(x, y, x + w, y + 1, C_ACCENT);
        y += 6;
        g.drawString(font, state.getBlock().getName().getString(), x, y, C_ACCENT, false);
        y += 14;

        // Registry name
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (rl != null) {
            g.drawString(font, rl.toString(), x, y, C_DIM, false);
            y += 12;
        }

        // Local position
        BlockPos lp = pattern.toLocal(inspectedWorldPos);
        if (lp != null) {
            g.drawString(font, "Local pos: " + lp.getX() + ", " + lp.getY() + ", " + lp.getZ(), x, y, C_DIM, false);
            y += 12;
        }

        // Block properties
        var props = state.getValues();
        if (!props.isEmpty()) {
            g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
            y += 6;
            g.drawString(font, "Block State:", x, y, C_DIM, false);
            y += 11;
            for (var entry : props.entrySet()) {
                String line = "  " + entry.getKey().getName() + " = " + getPropName(entry.getKey(), entry.getValue());
                g.drawString(font, line, x, y, C_TEXT, false);
                y += 10;
                if (y > this.height - 60) break;
            }
        }

        // Block entity indicator
        if (pattern.hasBlockEntity(inspectedWorldPos)) {
            y += 4;
            g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
            y += 6;
            g.drawString(font, "\u26A1 Has Block Entity", x, y, C_WARN, false);
            y += 12;
        }

        // Is controller?
        if (inspectedWorldPos.equals(pattern.controllerWorldPos)) {
            g.drawString(font, "\u2605 Controller Block", x, y, C_ACCENT, false);
            y += 12;
        }

        // Is hatch/bus?
        if (hatchBusSet.contains(inspectedWorldPos)) {
            g.drawString(font, "\uD83D\uDD17 Hatch / Bus", x, y, C_GREEN, false);
            y += 12;
        }
        if (energySet.contains(inspectedWorldPos)) {
            g.drawString(font, "\u26A1 Energy I/O", x, y, C_WARN, false);
            y += 12;
        }

        // Clear button
        y += 4;
        if (y < this.height - 50) {
            boolean ch = isOver(mx, my, x, y, 80, 14);
            g.fill(x, y, x + 80, y + 14, ch ? C_BTN_HOV : C_BTN);
            g.drawString(font, "Clear", x + (80 - font.width("Clear")) / 2, y + 3, ch ? C_ACCENT : C_TEXT, false);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Input
    // ──────────────────────────────────────────────────────────────────────────

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

        // Back button
        int bw = 80, bh = 18, bx = this.width - bw - 8, by = this.height - bh - 6;
        if (isOver((int) mx, (int) my, bx, by, bw, bh)) {
            onClose();
            return true;
        }

        switch (tab) {
            case FILTER -> {
                PhantasiaSceneScreen.ViewFilter[] vfs = PhantasiaSceneScreen.ViewFilter.values();
                int fw = 200, fx = (this.width - fw) / 2;
                int y = 68; // Starting Y for filters

                // 1. View Filters Loop
                for (int i = 0; i < vfs.length; i++) {
                    if (isOver((int) mx, (int) my, fx, y, fw, 22)) {
                        activeFilter = vfs[i];
                        return true;
                    }
                    y += 26;
                }

                // 2. Space for the separator
                y += 20;

                // 3. Common Mistakes Toggle
                if (script.hasCommonMistakes()) {
                    if (isOver((int) mx, (int) my, fx, y, fw, 18)) {
                        if (parent instanceof PhantasiaSceneScreen p) {
                            p.showMistakes = !p.showMistakes;
                        }
                        return true;
                    }
                    y += 22;
                }

                // 4. Heatmap Tiers
                if (!script.getHeatmapTiers().isEmpty()) {
                    y += 12; // skip the "Heatmap Layers:" label height
                    for (int i = 0; i < script.getHeatmapTiers().size(); i++) {
                        if (isOver((int) mx, (int) my, fx, y, fw, 14)) {
                            if (parent instanceof PhantasiaSceneScreen p) {
                                p.selectedTierIndex = i;
                            }
                            return true;
                        }
                        y += 16;
                    }

                    // Disable Heatmap button
                    if (isOver((int) mx, (int) my, fx, y, fw, 14)) {
                        if (parent instanceof PhantasiaSceneScreen p) {
                            p.selectedTierIndex = -1;
                        }
                        return true;
                    }
                    y += 16;
                }

                // 5. Apply & Return
                y += 6;
                int aw = 120, ax = (this.width - aw) / 2;
                if (isOver((int) mx, (int) my, ax, y, aw, 18)) {
                    onClose();
                    return true;
                }
            }
            case LIST -> {
                // Click a list item → switch to inspect tab for that block type
                int startY = 50, contentH = this.height - startY - 36;
                int y = startY - listScrollY, x = 12, w = this.width - 24;
                for (Map.Entry<String, List<BlockPos>> e : blocksByName.entrySet()) {
                    if (y + 18 >= startY && y <= startY + contentH) {
                        if (isOver((int) mx, (int) my, x, y, w, 18)) {
                            // Inspect the first occurrence of this block type
                            if (!e.getValue().isEmpty()) {
                                inspectedWorldPos = e.getValue().get(0);
                                tab = Tab.INSPECT;
                            }
                            return true;
                        }
                    }
                    y += 20;
                }
            }
            case INSPECT -> {
                // Clear button
                if (inspectedWorldPos != null) {
                    int y = 50 + (int) (PhantasiaSceneScreen.SHARED_LEVEL != null &&
                            !PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(inspectedWorldPos).isAir() ? 100 : 0);
                    if (isOver((int) mx, (int) my, 12, (int) my, 80, 14)) { // approximate
                        inspectedWorldPos = null;
                        return true;
                    }
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

    @Override
    public void onClose() {
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

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getPropName(Property<?> p, Comparable<?> v) {
        try {
            return ((Property<T>) p).getName((T) v);
        } catch (Exception e) {
            return v.toString();
        }
    }
}
