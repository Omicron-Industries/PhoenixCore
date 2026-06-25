package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

/**
 * Inline popup for editing the background theme of a chapter category.
 * Opened from the context menu in ChronicleOverviewScreen.
 *
 * Lets the user choose:
 * - Background style (DOT_GRID / GRID_LINES / HEX_GRID / DIAGONAL / SOLID / CUSTOM)
 * - Background color tint (hex RRGGBB, no alpha — we add our own alpha)
 * - Custom texture resource location (only relevant for CUSTOM style)
 */
public class CategoryThemeScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 220;
    private static final int MARGIN = 14;
    private static final int FIELD_H = 16;
    private static final int STRIDE = FIELD_H + 10;

    private static final int COL_PANEL = 0xFF16161C;
    private static final int COL_HDR = 0xFF0C0C10;
    private static final int COL_BORDER = 0xFF884499;
    private static final int COL_TEXT = 0xFFDDDDE8;
    private static final int COL_DIM = 0xFF888898;
    private static final int COL_FAINT = 0xFF4A4A5A;

    private final Screen parent;
    private final String category;

    private CategoryConfig.BgStyle selectedStyle;
    private int cachedColor;
    private String cachedTexture;

    private EditBox colorBox;
    private EditBox textureBox;

    private boolean styleDropOpen = false;
    private int panelLeft, panelTop;

    private static final CategoryConfig.BgStyle[] STYLES = CategoryConfig.BgStyle.values();

    public CategoryThemeScreen(Screen parent, String category) {
        super(Component.literal("Chapter Theme"));
        this.parent = parent;
        this.category = category;

        CategoryConfig cfg = CategoryConfig.get(category);
        this.selectedStyle = cfg.getStyle();
        this.cachedColor = cfg.getColor();
        this.cachedTexture = cfg.getTexture();
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;

        int fx = panelLeft + MARGIN;
        int fw = PANEL_W - MARGIN * 2;
        int y = panelTop + 28;

        // Style button
        addRenderableWidget(Button.builder(
                Component.literal("§8Style: §7" + selectedStyle.name() + " §8▾"),
                b -> styleDropOpen = !styleDropOpen).bounds(fx, y, fw, FIELD_H).build());
        y += STRIDE;

        // Color
        colorBox = new EditBox(font, fx, y + 9, fw, FIELD_H, Component.empty());
        colorBox.setMaxLength(7);
        colorBox.setHint(Component.literal("§8#RRGGBB  (empty = default)"));
        colorBox.setValue(cachedColor != 0 ? String.format("#%06X", cachedColor & 0x00FFFFFF) : "");
        colorBox.setResponder(v -> {
            try {
                cachedColor = (int) Long.parseLong(v.replace("#", ""), 16);
            } catch (Exception ignored) {
                if (v.isBlank()) cachedColor = 0;
            }
        });
        addRenderableWidget(colorBox);
        y += STRIDE + 10;

        // Texture (CUSTOM style only — always shown for simplicity)
        textureBox = new EditBox(font, fx, y + 9, fw, FIELD_H, Component.empty());
        textureBox.setMaxLength(128);
        textureBox.setHint(Component.literal("§8modid:textures/gui/bg.png  (CUSTOM style only)"));
        textureBox.setValue(cachedTexture);
        textureBox.setResponder(v -> cachedTexture = v.trim());
        addRenderableWidget(textureBox);
        y += STRIDE + 10;

        // Preview swatch row (static colored row)
        y += 8;

        // Buttons
        int btnY = panelTop + PANEL_H - 10 - 18;
        int half = (fw - 6) / 2;
        addRenderableWidget(Button.builder(Component.literal("§aSave"), b -> save())
                .bounds(fx, btnY, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§7Cancel"),
                b -> {
                    if (minecraft != null) minecraft.setScreen(parent);
                })
                .bounds(fx + half + 6, btnY, half, 18).build());
    }

    private void save() {
        CategoryConfig cfg = new CategoryConfig();
        cfg.setStyle(selectedStyle);
        cfg.setColor(cachedColor);
        cfg.setTexture(cachedTexture);
        CategoryConfig.put(category, cfg);
        CategoryConfig.save();
        CategoryConfig.invalidate();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g) { /* parent renders behind us */ }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        if (parent != null) parent.render(g, -1, -1, partial);
        g.fill(0, 0, width, height, 0x70000000);

        // Panel
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL);
        drawBorder(g, panelLeft, panelTop, PANEL_W, PANEL_H, COL_BORDER);

        // Header
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + 22, COL_HDR);
        g.fill(panelLeft, panelTop + 21, panelLeft + PANEL_W, panelTop + 22, COL_BORDER);
        g.drawCenteredString(font, "§dTheme — §7" + category, panelLeft + PANEL_W / 2, panelTop + 7, COL_TEXT);

        int fx = panelLeft + MARGIN;
        int fw = PANEL_W - MARGIN * 2;
        int y = panelTop + 28;

        // Labels
        y += STRIDE;
        g.drawString(font, "§8Background color tint", fx, y, COL_FAINT);
        y += STRIDE + 10;
        g.drawString(font, "§8Custom texture (resource location)", fx, y, COL_FAINT);

        // Preview strip at bottom of the form area
        int previewY = panelTop + 28 + STRIDE * 2 + 24 + 8;
        int bg = (cachedColor != 0) ? (0xFF000000 | cachedColor) : 0xFF0B0B0F;
        g.fill(fx, previewY, fx + fw, previewY + 22, bg);
        drawBorder(g, fx, previewY, fw, 22, 0xFF333344);
        // Draw mini background preview based on selected style
        renderStylePreview(g, fx + 1, previewY + 1, fw - 2, 20);

        super.render(g, mx, my, partial);

        // Style dropdown (elevated z)
        if (styleDropOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            int dy = panelTop + 28 + FIELD_H + 1;
            int dropH = STYLES.length * 16;
            g.fill(fx, dy, fx + fw, dy + dropH, COL_PANEL);
            drawBorder(g, fx, dy, fw, dropH, COL_BORDER);
            for (int i = 0; i < STYLES.length; i++) {
                int rowY = dy + i * 16;
                boolean hov = mx >= fx && mx <= fx + fw && my >= rowY && my <= rowY + 16;
                if (hov) g.fill(fx + 1, rowY, fx + fw - 1, rowY + 16, 0xFF1E1E2A);
                String mark = STYLES[i] == selectedStyle ? "§a● §7" : "§8  §7";
                g.drawString(font, mark + STYLES[i].name(), fx + 6, rowY + 4, hov ? COL_TEXT : COL_DIM);
            }
            g.pose().popPose();
        }
    }

    /** Renders a tiny preview of what the selected style looks like in the swatch strip. */
    private void renderStylePreview(GuiGraphics g, int x, int y, int w, int h) {
        switch (selectedStyle) {
            case DOT_GRID -> {
                for (int px = x + 4; px < x + w; px += 8)
                    for (int py = y + 4; py < y + h; py += 8)
                        g.fill(px, py, px + 1, py + 1, 0x33FFFFFF);
            }
            case GRID_LINES -> {
                for (int px = x; px < x + w; px += 10) g.fill(px, y, px + 1, y + h, 0x22FFFFFF);
                for (int py = y; py < y + h; py += 10) g.fill(x, py, x + w, py + 1, 0x22FFFFFF);
            }
            case HEX_GRID -> {
                // Simplified hex preview: staggered dots
                for (int row = 0; row < 3; row++) {
                    int ox = (row % 2 == 0) ? 0 : 6;
                    for (int col = 0; col < 5; col++) {
                        int px = x + 4 + ox + col * 12;
                        int py = y + 3 + row * 8;
                        g.fill(px, py, px + 2, py + 2, 0x44FFFFFF);
                    }
                }
            }
            case DIAGONAL_LINES -> {
                for (int d = 0; d < w + h; d += 8) {
                    int x0 = x + d, y0 = y;
                    int x1 = x, y1 = y + d;
                    // simplified single pixel diagonal
                    int len = Math.min(d, Math.min(w, h));
                    for (int i = 0; i < len; i++) {
                        int px = x0 - i, py = y0 + i;
                        if (px >= x && px < x + w && py >= y && py < y + h)
                            g.fill(px, py, px + 1, py + 1, 0x22FFFFFF);
                    }
                }
            }
            case SOLID -> {} // just the background color, nothing drawn
            case CUSTOM -> g.drawCenteredString(font, "§8custom", x + w / 2, y + h / 2 - 4, 0xFF555566);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && styleDropOpen) {
            int fx = panelLeft + MARGIN;
            int fw = PANEL_W - MARGIN * 2;
            int dy = panelTop + 28 + FIELD_H + 1;
            for (int i = 0; i < STYLES.length; i++) {
                int rowY = dy + i * 16;
                if (mx >= fx && mx <= fx + fw && my >= rowY && my <= rowY + 16) {
                    selectedStyle = STYLES[i];
                    styleDropOpen = false;
                    clearWidgets();
                    init();
                    return true;
                }
            }
            styleDropOpen = false;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
