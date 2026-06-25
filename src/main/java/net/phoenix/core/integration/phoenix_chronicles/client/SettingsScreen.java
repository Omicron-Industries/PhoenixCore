package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.ChroniclesTheme;
import net.phoenix.core.integration.phoenix_chronicles.QuestChroniclesSettings;
import net.phoenix.core.integration.phoenix_chronicles.QuestChroniclesSettings.*;

/**
 * Settings screen for Quest Chronicles.
 */
public class SettingsScreen extends Screen {

    private int C_BG = 0xFF0B0B0F;
    private int C_PANEL = 0xFF14141A;
    private int C_HEADER = 0xFF0C0C10;
    private int C_BORDER = 0xFF353548;
    private int C_ACCENT = 0xFF00AA55;
    private int C_TEXT = 0xFFD8D8E4;
    private int C_TEXT_DIM = 0xFF7A7A8A;
    private int C_TEXT_FAINT = 0xFF404050;
    private int C_OK = 0xFF44CC88;
    private static final int C_CANCEL = 0xFF888898;

    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 28;
    private static final int MARGIN = 8;
    private static final int ROW_H = 24;
    private static final int ROW_GAP = 4;

    private final Screen parent;
    private QuestChroniclesSettings settings;
    private int scrollY = 0;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Quest Chronicles Settings"));
        this.parent = parent;
        this.settings = QuestChroniclesSettings.load();
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
        C_OK = t.done.getColor();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        // Background
        g.fill(0, 0, width, height, C_BG);

        // Header
        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.drawCenteredString(font, "§fQuest Chronicles Settings", width / 2, 9, C_TEXT);

        // Content area
        int contentTop = HEADER_H + MARGIN;
        int contentH = height - HEADER_H - MARGIN - FOOTER_H - MARGIN;

        g.enableScissor(0, contentTop, width, contentTop + contentH);

        int y = contentTop - scrollY;
        int x = MARGIN;
        int w = width - MARGIN * 2;

        // Settings rows
        y = renderSetting(g, x, y, w, "§fText Scale", settings.getTextScale().name(), mx, my) + ROW_GAP;
        y = renderSetting(g, x, y, w, "§fTheme", settings.getTheme().name(), mx, my) + ROW_GAP;
        y = renderSetting(g, x, y, w, "§fLayout Density", settings.getDensity().name(), mx, my) + ROW_GAP;
        y = renderSetting(g, x, y, w, "§fShow Dev Info by Default",
                settings.isShowDevInfoByDefault() ? "§aYes" : "§cNo", mx, my) + ROW_GAP * 2;

        // Theme Editor link row
        boolean themeHov = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (themeHov) g.fill(x, y, x + w, y + ROW_H, 0x10FFFFFF);
        int textY = y + (ROW_H - 8) / 2;
        g.drawString(font, "§fTheme Editor", x + 4, textY, C_TEXT, false);
        g.drawCenteredString(font, "§7→", x + w - ARROW_W / 2, textY, themeHov ? C_ACCENT : C_TEXT_DIM);
        y += ROW_H + ROW_GAP * 2;

        g.drawString(font, "§8HUD Settings:", x, y, C_TEXT_FAINT, false);
        y += ROW_H + ROW_GAP;

        y = renderSetting(g, x, y, w, "§fHUD Position", settings.getHudPosition().name(), mx, my) + ROW_GAP;
        y = renderSetting(g, x, y, w, "§fHUD Opacity", String.format("%.0f%%", settings.getHudOpacity() * 100), mx,
                my) + ROW_GAP;
        y = renderSetting(g, x, y, w, "§fShow HUD Title", settings.isShowHUDTitle() ? "§aYes" : "§cNo", mx, my) +
                ROW_GAP;
        y = renderSetting(g, x, y, w, "§fShow HUD Progress", settings.isShowHUDProgress() ? "§aYes" : "§cNo", mx, my) +
                ROW_GAP;
        y = renderSetting(g, x, y, w, "§fShow HUD Rewards", settings.isShowHUDRewards() ? "§aYes" : "§cNo", mx, my) +
                ROW_GAP;

        g.drawString(font, "§8Dep line appearance is controlled via right-click on the quest canvas.",
                x, y, C_TEXT_FAINT, false);

        g.disableScissor();

        // Footer
        int footerY = height - FOOTER_H;
        g.fill(0, footerY, width, height, C_HEADER);
        g.fill(0, footerY, width, footerY + 1, C_BORDER);

        int btnW = 100;
        int btnGap = 8;
        int btnY = footerY + 5;

        // Save button
        boolean saveHov = mx >= width / 2 - btnW - btnGap / 2 && mx < width / 2 - btnGap / 2 && my >= btnY &&
                my < btnY + 18;
        g.fill(width / 2 - btnW - btnGap / 2, btnY, width / 2 - btnGap / 2, btnY + 18,
                saveHov ? 0xFF2A4A2A : 0xFF1A2A1A);
        if (saveHov) g.fill(width / 2 - btnW - btnGap / 2, btnY, width / 2 - btnGap / 2, btnY + 1, C_OK);
        g.drawCenteredString(font, "§a✓ Save", width / 2 - btnW / 2 - btnGap / 2, btnY + 6, saveHov ? C_OK : C_TEXT);

        // Cancel button
        boolean cancelHov = mx >= width / 2 + btnGap / 2 && mx < width / 2 + btnW + btnGap / 2 && my >= btnY &&
                my < btnY + 18;
        g.fill(width / 2 + btnGap / 2, btnY, width / 2 + btnW + btnGap / 2, btnY + 18,
                cancelHov ? 0xFF3A3A3A : 0xFF2A2A2A);
        if (cancelHov) g.fill(width / 2 + btnGap / 2, btnY, width / 2 + btnW + btnGap / 2, btnY + 1, C_CANCEL);
        g.drawCenteredString(font, "§7✕ Cancel", width / 2 + btnW / 2 + btnGap / 2, btnY + 6,
                cancelHov ? C_CANCEL : C_TEXT);
    }

    private static final int ARROW_W = 18;
    private static final int ARROW_GAP = 2;

    private int renderSetting(GuiGraphics g, int x, int y, int w, String label, String value, int mx, int my) {
        int textY = y + (ROW_H - 8) / 2;
        boolean rowHov = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (rowHov) g.fill(x, y, x + w, y + ROW_H, 0x10FFFFFF);

        // Two arrow buttons flush to the right
        int rArrowX = x + w - ARROW_W;
        int lArrowX = rArrowX - ARROW_GAP - ARROW_W;
        boolean leftHov = mx >= lArrowX && mx < lArrowX + ARROW_W && my >= y && my < y + ROW_H;
        boolean rightHov = mx >= rArrowX && mx < rArrowX + ARROW_W && my >= y && my < y + ROW_H;
        if (leftHov) g.fill(lArrowX, y, lArrowX + ARROW_W, y + ROW_H, 0x33FFFFFF);
        if (rightHov) g.fill(rArrowX, y, rArrowX + ARROW_W, y + ROW_H, 0x33FFFFFF);
        g.drawCenteredString(font, "§7<", lArrowX + ARROW_W / 2, textY, leftHov ? C_ACCENT : C_TEXT_DIM);
        g.drawCenteredString(font, "§7>", rArrowX + ARROW_W / 2, textY, rightHov ? C_ACCENT : C_TEXT_DIM);

        // Label left, value right-aligned just before arrows
        g.drawString(font, label, x + 4, textY, C_TEXT, false);
        int valueX = lArrowX - 6 - font.width(value);
        g.drawString(font, value, valueX, textY, C_TEXT_DIM, false);

        return y + ROW_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int footerY = height - FOOTER_H;
        int btnW = 100;
        int btnGap = 8;
        int btnY = footerY + 5;

        // Save button
        if (mx >= width / 2 - btnW - btnGap / 2 && mx < width / 2 - btnGap / 2 && my >= btnY && my < btnY + 18) {
            settings.save();
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }

        // Cancel button
        if (mx >= width / 2 + btnGap / 2 && mx < width / 2 + btnW + btnGap / 2 && my >= btnY && my < btnY + 18) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }

        // Settings navigation
        int contentTop = HEADER_H + MARGIN;
        int contentH = height - HEADER_H - MARGIN - FOOTER_H - MARGIN;

        if (my >= contentTop && my < contentTop + contentH) {
            int y = contentTop - scrollY;
            int x = MARGIN;
            int w = width - MARGIN * 2;

            // Text Scale
            if (handleSettingClick(x, y, w, mx, my)) {
                TextScale[] scales = TextScale.values();
                int idx = settings.getTextScale().ordinal();
                settings.setTextScale(
                        scales[(idx + (isRightArrow(x, w, mx) ? 1 : -1) + scales.length) % scales.length]);
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Theme
            if (handleSettingClick(x, y, w, mx, my)) {
                Theme[] themes = Theme.values();
                int idx = settings.getTheme().ordinal();
                settings.setTheme(themes[(idx + (isRightArrow(x, w, mx) ? 1 : -1) + themes.length) % themes.length]);
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Density
            if (handleSettingClick(x, y, w, mx, my)) {
                Density[] densities = Density.values();
                int idx = settings.getDensity().ordinal();
                settings.setDensity(
                        densities[(idx + (isRightArrow(x, w, mx) ? 1 : -1) + densities.length) % densities.length]);
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Show Dev Info
            if (handleSettingClick(x, y, w, mx, my)) {
                settings.setShowDevInfoByDefault(!settings.isShowDevInfoByDefault());
                return true;
            }
            y += ROW_H + ROW_GAP * 2;

            // Theme Editor
            if (my >= y && my < y + ROW_H && mx >= x && mx < x + w) {
                if (minecraft != null) minecraft.setScreen(new ChroniclesThemeEditorScreen(this));
                return true;
            }
            y += ROW_H + ROW_GAP * 2;

            y += ROW_H + ROW_GAP; // Skip HUD Settings label

            // HUD Position
            if (handleSettingClick(x, y, w, mx, my)) {
                HUDPosition[] positions = HUDPosition.values();
                int idx = settings.getHudPosition().ordinal();
                settings.setHudPosition(
                        positions[(idx + (isRightArrow(x, w, mx) ? 1 : -1) + positions.length) % positions.length]);
                return true;
            }
            y += ROW_H + ROW_GAP;

            // HUD Opacity
            if (handleSettingClick(x, y, w, mx, my)) {
                float opacity = settings.getHudOpacity();
                opacity += isRightArrow(x, w, mx) ? 0.1f : -0.1f;
                settings.setHudOpacity(opacity);
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Show HUD Title
            if (handleSettingClick(x, y, w, mx, my)) {
                settings.setShowHUDTitle(!settings.isShowHUDTitle());
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Show HUD Progress
            if (handleSettingClick(x, y, w, mx, my)) {
                settings.setShowHUDProgress(!settings.isShowHUDProgress());
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Show HUD Rewards
            if (handleSettingClick(x, y, w, mx, my)) {
                settings.setShowHUDRewards(!settings.isShowHUDRewards());
                return true;
            }
            y += ROW_H + ROW_GAP;

            // Line settings moved to right-click context menu on the quest canvas.
        }

        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleSettingClick(int x, int y, int w, double mx, double my) {
        int rArrowX = x + w - ARROW_W;
        int lArrowX = rArrowX - ARROW_GAP - ARROW_W;
        return my >= y && my < y + ROW_H && mx >= lArrowX;
    }

    private boolean isRightArrow(int x, int w, double mx) {
        int rArrowX = x + w - ARROW_W;
        return mx >= rArrowX;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollY = Math.max(0, (int) (scrollY - delta * 12));
        return true;
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
