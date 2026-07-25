package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.QuestChroniclesSettings.*;

import java.util.Comparator;
import java.util.List;

public class DepLineSettingsScreen extends Screen {

    private final ChronicleOverviewScreen parent;
    private final String category;

    private int C_BG, C_PANEL, C_HEADER, C_BORDER, C_ACCENT, C_TEXT, C_TEXT_DIM, C_TEXT_FAINT, C_DONE, C_ACTIVE;

    private static final int MARGIN = 8;
    private static final int HEADER_H = 28;
    private static final int SEARCH_H = 22;   
    private static final int FOOTER_H = 28;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;
    private static final int ARROW_W = 16;

    private LineStyle lineShape;
    private LineVisualStyle lineVisual;
    private QuestChroniclesSettings.LineAnimSpeed lineAnimSpeed;

    private EditBox searchBox;
    private String searchQuery = "";

    private int scrollY = 0;

    public DepLineSettingsScreen(ChronicleOverviewScreen parent, String category) {
        super(Component.literal("Dependency Line Settings"));
        this.parent = parent;
        this.category = category;
    }

    @Override
    protected void init() {
        super.init();
        ChroniclesTheme t = ChroniclesTheme.current();
        C_BG = t.bg.getColor();
        C_PANEL = t.panel.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_ACCENT = t.accent.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        C_DONE = t.done.getColor();
        C_ACTIVE = t.activeColor.getColor();

        QuestChroniclesSettings s = QuestChroniclesSettings.get();
        lineShape = s.getLineStyle();
        lineVisual = s.getLineVisualStyle();
        lineAnimSpeed = s.getLineAnimSpeed();

        searchBox = new EditBox(font, MARGIN, HEADER_H + 3, width - MARGIN * 2, SEARCH_H - 6, Component.empty());
        searchBox.setHint(Component.literal("§8Filter quests…"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(v -> {
            searchQuery = v.toLowerCase().trim();
            scrollY = 0;
        });
        addRenderableWidget(searchBox);
    }

    private static final int GLOBAL_SECTION_LABEL_H = 10 + ROW_GAP;
    private static final int GLOBAL_ROW_COUNT = 3;
    private static final int PREVIEW_H = 40;
    private static final int PREVIEW_GAP = 8;
    private static final int DIVIDER_H = 6;
    private static final int PER_QUEST_LABEL_H = 10 + ROW_GAP;

    private int globalEnd() {
        return GLOBAL_SECTION_LABEL_H + GLOBAL_ROW_COUNT * (ROW_H + ROW_GAP) + 4 + PREVIEW_H + PREVIEW_GAP;
    }

    private int perQuestStart() {
        return globalEnd() + DIVIDER_H + PER_QUEST_LABEL_H;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, C_BG);

        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.drawCenteredString(font, "§fDependency Line Settings", width / 2, 9, C_TEXT);

        g.fill(0, HEADER_H, width, HEADER_H + SEARCH_H, C_HEADER);
        g.fill(0, HEADER_H + SEARCH_H - 1, width, HEADER_H + SEARCH_H, C_BORDER);

        int contentTop = HEADER_H + SEARCH_H + MARGIN;
        int contentH = height - HEADER_H - SEARCH_H - MARGIN - FOOTER_H - MARGIN;
        int x = MARGIN, w = width - MARGIN * 2;

        g.enableScissor(0, contentTop, width, contentTop + contentH);
        int y = contentTop - scrollY;

        g.drawString(font, "§8GLOBAL APPEARANCE:", x, y, C_TEXT_FAINT, false);
        y += GLOBAL_SECTION_LABEL_H;

        y = renderCycleRow(g, x, y, w, "§fLine Shape", lineShape.name(), mx, my) + ROW_GAP;
        y = renderCycleRow(g, x, y, w, "§fLine Style", lineVisual.name(), mx, my) + ROW_GAP;
        y = renderCycleRow(g, x, y, w, "§fDot Speed", lineAnimSpeed.name(), mx, my) + ROW_GAP;

        y += 4;
        g.fill(x, y, x + w, y + PREVIEW_H, C_PANEL);
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + PREVIEW_H - 1, x + w, y + PREVIEW_H, C_BORDER);
        g.fill(x, y, x + 1, y + PREVIEW_H, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + PREVIEW_H, C_BORDER);
        g.drawString(font, "§8Preview", x + 4, y + 2, C_TEXT_FAINT, false);
        drawPreviewLines(g, x + 4, y + 12, w - 8, PREVIEW_H - 16);
        y += PREVIEW_H + PREVIEW_GAP;

        g.fill(x, y, x + w, y + 1, C_BORDER);
        y += DIVIDER_H;

        List<QuestNode> quests = QuestTreeRegistry.getAllQuests().values().stream()
                .filter(n -> category.equals(n.getCategory()))
                .filter(n -> searchQuery.isEmpty() || n.getTitle().getString().toLowerCase().contains(searchQuery) ||
                        n.getId().getPath().toLowerCase().contains(searchQuery))
                .sorted(Comparator.comparing(n -> n.getTitle().getString()))
                .toList();

        String countHint = searchQuery.isEmpty() ? "(" + category + ")" :
                "(" + quests.size() + " match" + (quests.size() == 1 ? "" : "es") + " in " + category + ")";
        g.drawString(font, "§8PER-QUEST  §7" + countHint, x, y, C_TEXT_FAINT, false);
        y += PER_QUEST_LABEL_H;

        for (QuestNode quest : quests) {
            boolean hidden = quest.isHideDepLine();
            int btnW = 60, btnX = x + w - btnW - 2, btnY = y + 2;
            boolean btnHov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + ROW_H - 4;
            boolean rowHov = mx >= x && mx < btnX - 2 && my >= y && my < y + ROW_H;

            if (rowHov || btnHov) g.fill(x, y, x + w, y + ROW_H, 0x10FFFFFF);

            String name = quest.getTitle().getString();
            int nameMaxW = btnX - x - 8;
            if (font.width(name) > nameMaxW) name = font.plainSubstrByWidth(name, Math.max(0, nameMaxW - 6)) + "…";
            g.drawString(font, "§7" + name, x + 4, y + 7, C_TEXT_DIM, false);

            g.fill(btnX, btnY, btnX + btnW, btnY + ROW_H - 4, btnHov ? 0x33FFFFFF : 0x11FFFFFF);
            g.fill(btnX, btnY, btnX + btnW, btnY + 1, hidden ? 0xFF444455 : C_DONE);
            g.drawCenteredString(font, hidden ? "§8HIDDEN" : "§aVISIBLE",
                    btnX + btnW / 2, btnY + 5, hidden ? C_TEXT_FAINT : C_DONE);

            y += ROW_H + ROW_GAP;
        }

        if (quests.isEmpty()) {
            String emptyMsg = searchQuery.isEmpty() ? "§8(no quests in this category)" :
                    "§8No quests match \"" + searchQuery + "\"";
            g.drawString(font, emptyMsg, x + 4, y, C_TEXT_FAINT, false);
        }

        g.disableScissor();

        int footerY = height - FOOTER_H;
        g.fill(0, footerY, width, height, C_HEADER);
        g.fill(0, footerY, width, footerY + 1, C_BORDER);

        int fbtnW = 80, fbtnGap = 8, fbtnY = footerY + 5;
        int saveX = width / 2 - fbtnW - fbtnGap / 2;
        int closeX = width / 2 + fbtnGap / 2;

        boolean saveHov = mx >= saveX && mx < saveX + fbtnW && my >= fbtnY && my < fbtnY + 18;
        boolean closeHov = mx >= closeX && mx < closeX + fbtnW && my >= fbtnY && my < fbtnY + 18;

        g.fill(saveX, fbtnY, saveX + fbtnW, fbtnY + 18, saveHov ? 0xFF2A4A2A : 0xFF1A2A1A);
        if (saveHov) g.fill(saveX, fbtnY, saveX + fbtnW, fbtnY + 1, C_DONE);
        g.drawCenteredString(font, "§a✓ Save", saveX + fbtnW / 2, fbtnY + 6, saveHov ? C_DONE : C_TEXT);

        g.fill(closeX, fbtnY, closeX + fbtnW, fbtnY + 18, closeHov ? 0xFF3A3A3A : 0xFF2A2A2A);
        if (closeHov) g.fill(closeX, fbtnY, closeX + fbtnW, fbtnY + 1, 0xFF888898);
        g.drawCenteredString(font, "§7✕ Close", closeX + fbtnW / 2, fbtnY + 6, closeHov ? 0xFFCCCCCC : C_TEXT);
    }

    private void drawPreviewLines(GuiGraphics g, int x, int y, int w, int h) {
        boolean spline = lineShape == LineStyle.SPLINE;
        int midX = x + w / 2;
        int ty = y + h / 4;
        int by = y + 3 * h / 4;

        drawPreviewLine(g, x, ty, midX, by, 0xFF00CC66, spline);
        drawPreviewLine(g, midX, by, x + w, ty, 0xFFFFAA00, spline);

        g.drawString(font, "§8" + lineShape.name() + "  ·  " + lineVisual.name(),
                x + w - font.width(lineShape.name() + "  ·  " + lineVisual.name()), y - 10, C_TEXT_FAINT, false);
    }

    private void drawPreviewLine(GuiGraphics g, int x1, int y1, int x2, int y2, int col, boolean spline) {
        int steps = Math.max(16, Math.abs(x2 - x1) / 2 + Math.abs(y2 - y1) / 2);
        int dx = x2 - x1, dy = y2 - y1;
        int cx1 = x1 + dx / 3, cy1 = y1;
        int cx2 = x2 - dx / 3, cy2 = y2;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px, py;
            if (spline) {
                float u = 1 - t;
                px = (int) (u * u * u * x1 + 3 * u * u * t * cx1 + 3 * u * t * t * cx2 + t * t * t * x2);
                py = (int) (u * u * u * y1 + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y2);
            } else {
                px = x1 + (int) (t * dx);
                py = y1 + (int) (t * dy);
            }
            int rgb = col & 0x00FFFFFF;
            switch (lineVisual) {
                case THIN -> g.fill(px, py, px + 1, py + 1, col);
                case BOLD -> {
                    g.fill(px - 2, py - 2, px + 3, py + 3, col);
                    g.fill(px - 3, py - 2, px - 2, py + 3, rgb | 0x33000000);
                    g.fill(px + 3, py - 2, px + 4, py + 3, rgb | 0x33000000);
                    g.fill(px - 2, py - 3, px + 3, py - 2, rgb | 0x33000000);
                    g.fill(px - 2, py + 3, px + 3, py + 4, rgb | 0x33000000);
                }
                case THICK -> {
                    g.fill(px - 4, py - 4, px + 5, py + 5, rgb | 0x1C000000);
                    g.fill(px - 3, py - 3, px + 4, py + 4, rgb | 0x55000000);
                    g.fill(px - 3, py - 3, px + 4, py + 4, col);
                    g.fill(px - 1, py - 1, px + 2, py + 2, rgb | 0xFF000000);
                }
                case WIDE -> {
                    g.fill(px - 6, py - 6, px + 7, py + 7, rgb | 0x0F000000);
                    g.fill(px - 5, py - 5, px + 6, py + 6, rgb | 0x1E000000);
                    g.fill(px - 4, py - 4, px + 5, py + 5, rgb | 0x32000000);
                    g.fill(px - 4, py - 4, px + 5, py + 5, col);
                    g.fill(px - 2, py - 2, px + 3, py + 3, rgb | 0xFF000000);
                }
                case GLOW -> {
                    g.fill(px - 3, py - 3, px + 4, py + 4, rgb | 0x44000000);
                    g.fill(px - 2, py - 2, px + 3, py + 3, rgb | 0xAA000000);
                    g.fill(px - 1, py - 1, px + 2, py + 2, col);
                }
                default -> {  
                    g.fill(px - 1, py - 1, px + 2, py + 2, col);
                    g.fill(px - 2, py - 1, px - 1, py + 2, rgb | 0x33000000);
                    g.fill(px + 2, py - 1, px + 3, py + 2, rgb | 0x33000000);
                }
            }
        }
    }

    private int renderCycleRow(GuiGraphics g, int x, int y, int w, String label, String value, int mx, int my) {
        int textY = y + (ROW_H - 8) / 2;
        int rArrowX = x + w - ARROW_W;
        int lArrowX = rArrowX - 2 - ARROW_W;
        boolean leftH = mx >= lArrowX && mx < lArrowX + ARROW_W && my >= y && my < y + ROW_H;
        boolean rightH = mx >= rArrowX && mx < rArrowX + ARROW_W && my >= y && my < y + ROW_H;

        if (leftH) g.fill(lArrowX, y, lArrowX + ARROW_W, y + ROW_H, 0x33FFFFFF);
        if (rightH) g.fill(rArrowX, y, rArrowX + ARROW_W, y + ROW_H, 0x33FFFFFF);
        g.drawCenteredString(font, "§7<", lArrowX + ARROW_W / 2, textY, leftH ? C_ACCENT : C_TEXT_FAINT);
        g.drawCenteredString(font, "§7>", rArrowX + ARROW_W / 2, textY, rightH ? C_ACCENT : C_TEXT_FAINT);

        g.drawString(font, label, x + 4, textY, C_TEXT, false);
        int valX = lArrowX - 6 - font.width(value);
        g.drawString(font, "§7" + value, valX, textY, C_TEXT_DIM, false);

        return y + ROW_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int footerY = height - FOOTER_H;
        int fbtnW = 80, fbtnGap = 8, fbtnY = footerY + 5;
        int saveX = width / 2 - fbtnW - fbtnGap / 2;
        int closeX = width / 2 + fbtnGap / 2;

        if (mx >= saveX && mx < saveX + fbtnW && my >= fbtnY && my < fbtnY + 18) {
            saveGlobal();
            return true;
        }
        if (mx >= closeX && mx < closeX + fbtnW && my >= fbtnY && my < fbtnY + 18) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }

        int contentTop = HEADER_H + SEARCH_H + MARGIN;
        int contentH = height - HEADER_H - SEARCH_H - MARGIN - FOOTER_H - MARGIN;
        if (my < contentTop || my >= contentTop + contentH) return super.mouseClicked(mx, my, btn);

        int x = MARGIN, w = width - MARGIN * 2;
        int y = contentTop - scrollY;

        y += GLOBAL_SECTION_LABEL_H;

        if (hitArrows(x, y, w, mx, my)) {
            LineStyle[] vals = LineStyle.values();
            lineShape = vals[(lineShape.ordinal() + (isRight(x, w, mx) ? 1 : -1) + vals.length) % vals.length];
            return true;
        }
        y += ROW_H + ROW_GAP;

        if (hitArrows(x, y, w, mx, my)) {
            LineVisualStyle[] vals = LineVisualStyle.values();
            lineVisual = vals[(lineVisual.ordinal() + (isRight(x, w, mx) ? 1 : -1) + vals.length) % vals.length];
            return true;
        }
        y += ROW_H + ROW_GAP;

        if (hitArrows(x, y, w, mx, my)) {
            QuestChroniclesSettings.LineAnimSpeed[] vals = QuestChroniclesSettings.LineAnimSpeed.values();
            lineAnimSpeed = vals[(lineAnimSpeed.ordinal() + (isRight(x, w, mx) ? 1 : -1) + vals.length) % vals.length];
            return true;
        }
        y += ROW_H + ROW_GAP;

        y += 4 + PREVIEW_H + PREVIEW_GAP;
        
        y += DIVIDER_H + PER_QUEST_LABEL_H;

        List<QuestNode> quests = QuestTreeRegistry.getAllQuests().values().stream()
                .filter(n -> category.equals(n.getCategory()))
                .filter(n -> searchQuery.isEmpty() || n.getTitle().getString().toLowerCase().contains(searchQuery) ||
                        n.getId().getPath().toLowerCase().contains(searchQuery))
                .sorted(Comparator.comparing(n -> n.getTitle().getString()))
                .toList();

        for (QuestNode quest : quests) {
            int btnW = 60, btnX = x + w - btnW - 2, btnY = y + 2;
            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + ROW_H - 4) {
                quest.setHideDepLine(!quest.isHideDepLine());
                parent.saveNodeHideDepLineToDisk(quest);
                parent.rebuildFromExternal();
                return true;
            }
            y += ROW_H + ROW_GAP;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void saveGlobal() {
        QuestChroniclesSettings s = QuestChroniclesSettings.get();
        s.setLineStyle(lineShape);
        s.setLineVisualStyle(lineVisual);
        s.setLineAnimSpeed(lineAnimSpeed);
        s.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private boolean hitArrows(int x, int y, int w, double mx, double my) {
        int rArrowX = x + w - ARROW_W;
        int lArrowX = rArrowX - 2 - ARROW_W;
        return my >= y && my < y + ROW_H && mx >= lArrowX;
    }

    private boolean isRight(int x, int w, double mx) {
        return mx >= x + w - ARROW_W;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollY = Math.max(0, (int) (scrollY - delta * 12));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {  
            if (searchBox != null && searchBox.isFocused() && !searchQuery.isEmpty()) {
                searchBox.setValue("");
                return true;
            }
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
