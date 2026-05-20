package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.phantasia.PhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PhantasiaSceneSelectionScreen
 *
 * Card-grid selection screen with an active search filter. Each card shows:
 * - The controller block as a 2D item icon
 * - Machine name
 * - Script step count (green dot = has custom script)
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaSceneSelectionScreen extends Screen {

    public static final List<MultiblockMachineDefinition> PHANTASIA_SCENES = new ArrayList<>();

    // Runtime filtered list matching the search query
    private final List<MultiblockMachineDefinition> filteredScenes = new ArrayList<>();

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF080810;
    private static final int C_BG_BOT = 0xFF0B0B18;
    private static final int C_ACCENT = 0xFF4FC3F7;
    private static final int C_CARD = 0xBB111128;
    private static final int C_CARD_HOV = 0xBB182040;
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF667788;
    private static final int C_SCRIPT = 0xFF66BB6A;
    private static final int C_BTN = 0xBB151530;
    private static final int C_BTN_HOV = 0xBB1A2840;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int CARD_W = 104;
    private static final int CARD_H = 86;
    private static final int CARD_PAD = 8;
    private static final int COLS = 3;
    private static final int HEADER_H = 38;
    private static final int SEARCH_H = 24; // Extra space allocated for the search bar
    private static final int FOOTER_H = 30;

    private final Screen parent;
    private EditBox searchBox;
    private int scrollOffset = 0; // in rows
    private int hoveredCard = -1;

    public PhantasiaSceneSelectionScreen(Screen parent) {
        super(Component.literal("Phantasia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Compute search bar width matching the exact layout grid width
        int totalGridW = COLS * CARD_W + (COLS - 1) * CARD_PAD;
        int searchX = (this.width - totalGridW) / 2;
        int searchY = HEADER_H + 4;

        // Initialize Minecraft's built-in text field widget
        this.searchBox = new EditBox(this.font, searchX, searchY, totalGridW, 16, Component.literal("Search..."));
        this.searchBox.setHint(Component.literal("Search machines...").withStyle(style -> style.withColor(0xFF888888)));
        this.searchBox.setBordered(true);
        this.searchBox.setMaxLength(32);

        // Listen for typing events to update results actively
        this.searchBox.setResponder(this::onSearchChanged);

        this.addWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // Re-run filter on init to capture initial list state or retain previous queries
        updateFilteredList();
    }

    private void onSearchChanged(String query) {
        updateFilteredList();
    }

    private void updateFilteredList() {
        filteredScenes.clear();
        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";

        for (MultiblockMachineDefinition def : PHANTASIA_SCENES) {
            if (query.isEmpty()) {
                filteredScenes.add(def);
                continue;
            }

            // Match against translation string name
            String name = def.getLangValue();
            if (name != null && name.toLowerCase(Locale.ROOT).contains(query)) {
                filteredScenes.add(def);
                continue;
            }

            // Fallback match against ID path string
            String idPath = def.getId().getPath().replace('_', ' ');
            if (idPath.toLowerCase(Locale.ROOT).contains(query)) {
                filteredScenes.add(def);
            }
        }

        // Reset scroll position so users don't view empty rows if they were scrolled down
        this.scrollOffset = 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rendering
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, this.width, this.height, C_BG, C_BG_BOT);
        renderHeader(g);

        // Draw the search box widget over the screen background
        if (this.searchBox != null) {
            this.searchBox.render(g, mx, my, partial);
        }

        renderCards(g, mx, my);
        renderFooter(g, mx, my);
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, this.width, HEADER_H, 0xCC0A0A14);
        g.fill(0, HEADER_H - 2, this.width, HEADER_H, C_ACCENT);
        g.drawCenteredString(font, "\u2736 Phantasia", this.width / 2, 8, C_ACCENT);
        g.drawCenteredString(font, "Select a Multiblock to Explore", this.width / 2, 22, C_DIM);
    }

    private void renderCards(GuiGraphics g, int mx, int my) {
        int totalW = COLS * CARD_W + (COLS - 1) * CARD_PAD;
        int startX = (this.width - totalW) / 2;
        // Shift content down by SEARCH_H to prevent overlapping with our input bar
        int startY = HEADER_H + SEARCH_H + 6;
        int maxRows = visibleRows();

        hoveredCard = -1;

        if (filteredScenes.isEmpty()) {
            g.drawCenteredString(font, "No matching machines found", this.width / 2, startY + 20, C_DIM);
            return;
        }

        for (int i = 0; i < filteredScenes.size(); i++) {
            int row = i / COLS - scrollOffset;
            int col = i % COLS;
            if (row < 0 || row >= maxRows) continue;

            int cx = startX + col * (CARD_W + CARD_PAD);
            int cy = startY + row * (CARD_H + CARD_PAD);

            boolean hov = mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H;
            if (hov) hoveredCard = i;

            renderCard(g, mx, my, filteredScenes.get(i), cx, cy, hov);
        }
    }

    private void renderCard(GuiGraphics g, int mx, int my,
                            MultiblockMachineDefinition def,
                            int cx, int cy, boolean hovered) {
        // 1. Card background and borders
        g.fill(cx, cy, cx + CARD_W, cy + CARD_H, hovered ? C_CARD_HOV : C_CARD);
        g.fill(cx, cy, cx + CARD_W, cy + 2, hovered ? C_ACCENT : 0x664FC3F7);
        if (hovered) {
            g.fill(cx, cy, cx + 1, cy + CARD_H, C_ACCENT);
            g.fill(cx + CARD_W - 1, cy, cx + CARD_W, cy + CARD_H, C_ACCENT);
            g.fill(cx, cy + CARD_H - 1, cx + CARD_W, cy + CARD_H, C_ACCENT);
        }

        // 2. Block icon (2D Item Sprite)
        Block block = def.getBlock();
        if (block != null) {
            ItemStack stack = new ItemStack(block);
            int iconSize = 32;
            int iconX = cx + (CARD_W - iconSize) / 2;
            int iconY = cy + 6;

            g.pose().pushPose();
            g.pose().translate(iconX, iconY, 0);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }

        // 3. Machine name with Truncation and Fallback
        String name = def.getLangValue();

        if (name == null || name.isEmpty() || name.contains("gtceu.multiblock.")) {
            name = def.getId().getPath().replace('_', ' ');
            name = org.apache.commons.lang3.text.WordUtils.capitalizeFully(name);
        }

        int maxWidth = CARD_W - 8;
        int nameY = cy + CARD_H - 22;

        if (font.width(name) > maxWidth) {
            name = font.plainSubstrByWidth(name, maxWidth - font.width("...")) + "...";
        }

        g.drawString(font, name, cx + 4, nameY, hovered ? C_ACCENT : C_TEXT, false);

        // 4. Script info
        boolean hasScript = PhantasiaScripts.has(def);
        if (hasScript) {
            g.fill(cx + CARD_W - 8, cy + 4, cx + CARD_W - 4, cy + 8, C_SCRIPT);
            PhantasiaScript script = PhantasiaScripts.get(def);
            String steps = script.getSteps().size() + " steps";
            g.drawString(font, steps, cx + 4, cy + CARD_H - 10, C_DIM, false);
        } else {
            g.drawString(font, "No script", cx + 4, cy + CARD_H - 10, C_DIM, false);
        }
    }

    private void renderFooter(GuiGraphics g, int mx, int my) {
        int fy = this.height - FOOTER_H;
        g.fill(0, fy, this.width, this.height, 0xCC0A0A14);
        g.fill(0, fy, this.width, fy + 1, 0x44FFFFFF);

        // Scroll indicator evaluated against the filtered view count
        int totalRows = (filteredScenes.size() + COLS - 1) / COLS;
        if (totalRows > visibleRows()) {
            g.drawCenteredString(font, "\u25B2 \u25BC  scroll to see more", this.width / 2, fy + 4, C_DIM);
        }

        // Back button
        int bw = 80, bh = 18;
        int bx = (this.width - bw) / 2, by = fy + (FOOTER_H - bh) / 2;
        boolean bHov = isOver(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, bHov ? C_BTN_HOV : C_BTN);
        if (bHov) {
            g.fill(bx, by, bx + bw, by + 1, C_ACCENT);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, C_ACCENT);
        }
        g.drawString(font, "\u2190 Back", bx + (bw - font.width("\u2190 Back")) / 2, by + 5,
                bHov ? C_ACCENT : C_TEXT, false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Input
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Let the searchBox handle clicks first so it updates its focus state
        if (this.searchBox != null && this.searchBox.mouseClicked(mx, my, btn)) {
            return true;
        }

        // Back button
        int fy = this.height - FOOTER_H;
        int bw = 80, bh = 18;
        int bx = (this.width - bw) / 2, by = fy + (FOOTER_H - bh) / 2;
        if (isOver((int) mx, (int) my, bx, by, bw, bh)) {
            onClose();
            return true;
        }

        // Card click matches against the filtered array context
        if (hoveredCard >= 0 && hoveredCard < filteredScenes.size()) {
            Minecraft.getInstance().setScreen(
                    new PhantasiaSceneScreen(filteredScenes.get(hoveredCard), this));
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int totalRows = (filteredScenes.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (delta > 0 ? -1 : 1)));
        return true;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        // Allow character deletion or text operations to be captured by input field
        if (this.searchBox != null && this.searchBox.keyPressed(kc, sc, mod)) {
            return true;
        }
        if (kc == 256) { // ESC Key
            onClose();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Crucial for text components to actually receive alphabetical character entries
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int visibleRows() {
        // Deduct both HEADER_H and SEARCH_H from vertical room calculation
        return Math.max(1, (this.height - HEADER_H - SEARCH_H - FOOTER_H - 8) / (CARD_H + CARD_PAD));
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
