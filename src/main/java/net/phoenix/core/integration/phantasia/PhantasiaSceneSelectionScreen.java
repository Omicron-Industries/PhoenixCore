package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * PhantasiaSceneSelectionScreen
 *
 * Card-grid selection screen. Each card shows:
 * - The controller block as a 2D item icon (reliable, no FBO per card)
 * - Machine name
 * - Script step count (green dot = has custom script)
 *
 * The previous FBO-per-card approach was architecturally broken because
 * FBOWorldSceneRenderer.render() signature doesn't accept x/y/w/h as we
 * were passing. Using ItemStack rendering is simpler, always works, and
 * has essentially zero performance cost.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaSceneSelectionScreen extends Screen {

    public static final List<MultiblockMachineDefinition> PHANTASIA_SCENES = new ArrayList<>();

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
    private static final int FOOTER_H = 30;

    private final Screen parent;
    private int scrollOffset = 0; // in rows
    private int hoveredCard = -1;

    public PhantasiaSceneSelectionScreen(Screen parent) {
        super(Component.literal("Phantasia"));
        this.parent = parent;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rendering
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, this.width, this.height, C_BG, C_BG_BOT);
        renderHeader(g);
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
        int startY = HEADER_H + 6;
        int maxRows = visibleRows();

        hoveredCard = -1;

        for (int i = 0; i < PHANTASIA_SCENES.size(); i++) {
            int row = i / COLS - scrollOffset;
            int col = i % COLS;
            if (row < 0 || row >= maxRows) continue;

            int cx = startX + col * (CARD_W + CARD_PAD);
            int cy = startY + row * (CARD_H + CARD_PAD);

            boolean hov = mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H;
            if (hov) hoveredCard = i;

            renderCard(g, mx, my, PHANTASIA_SCENES.get(i), cx, cy, hov);
        }
    }

    private void renderCard(GuiGraphics g, int mx, int my,
                            MultiblockMachineDefinition def,
                            int cx, int cy, boolean hovered) {
        // Card background
        g.fill(cx, cy, cx + CARD_W, cy + CARD_H, hovered ? C_CARD_HOV : C_CARD);
        g.fill(cx, cy, cx + CARD_W, cy + 2, hovered ? C_ACCENT : 0x664FC3F7);
        if (hovered) {
            g.fill(cx, cy, cx + 1, cy + CARD_H, C_ACCENT);
            g.fill(cx + CARD_W - 1, cy, cx + CARD_W, cy + CARD_H, C_ACCENT);
            g.fill(cx, cy + CARD_H - 1, cx + CARD_W, cy + CARD_H, C_ACCENT);
        }

        // ── Block icon — renders the controller block as a 2D item sprite ──
        // This always works; no FBO, no SceneWidget, no camera state needed.
        Block block = def.getBlock();
        if (block != null) {
            ItemStack stack = new ItemStack(block);
            // GuiGraphics.renderItem handles all the heavy lifting
            int iconSize = 32;
            int iconX = cx + (CARD_W - iconSize) / 2;
            int iconY = cy + 6;
            // Scale up via pose stack (default renderItem is 16×16)
            g.pose().pushPose();
            g.pose().translate(iconX, iconY, 0);
            g.pose().scale(2f, 2f, 1f); // 2× scale → 32×32
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }

        // ── Machine name ──
        String name = def.getLangValue();
        while (font.width(name) > CARD_W - 8 && name.length() > 3)
            name = name.substring(0, name.length() - 2) + "\u2026";
        int nameY = cy + CARD_H - 22;
        g.drawString(font, name, cx + 4, nameY, hovered ? C_ACCENT : C_TEXT, false);

        // ── Script info ──
        boolean hasScript = PhantasiaScripts.has(def);
        if (hasScript) {
            // Green dot top-right
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

        // Scroll indicator
        int totalRows = (PHANTASIA_SCENES.size() + COLS - 1) / COLS;
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
        // Back button
        int fy = this.height - FOOTER_H;
        int bw = 80, bh = 18;
        int bx = (this.width - bw) / 2, by = fy + (FOOTER_H - bh) / 2;
        if (isOver((int) mx, (int) my, bx, by, bw, bh)) {
            onClose();
            return true;
        }

        // Card click
        if (hoveredCard >= 0 && hoveredCard < PHANTASIA_SCENES.size()) {
            Minecraft.getInstance().setScreen(
                    new PhantasiaSceneScreen(PHANTASIA_SCENES.get(hoveredCard), this));
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int totalRows = (PHANTASIA_SCENES.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (delta > 0 ? -1 : 1)));
        return true;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int visibleRows() {
        return Math.max(1, (this.height - HEADER_H - FOOTER_H - 8) / (CARD_H + CARD_PAD));
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
