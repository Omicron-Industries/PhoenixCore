package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.ChroniclesTheme;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestState;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchOverlayScreen extends Screen {

    private int C_PANEL = 0xFF14141A;
    private int C_PANEL_DARK = 0xFF0E0E12;
    private int C_BORDER = 0xFF252530;
    private int C_BORDER_LIT = 0xFF353548;
    private int C_SEL_ACCENT = 0xFF00AA55;
    private int C_TEXT = 0xFFD8D8E4;
    private int C_TEXT_DIM = 0xFF7A7A8A;
    private int C_TEXT_FAINT = 0xFF404050;
    private static final int[] CAT_ACCENTS = {
            0xFF5566EE, 0xFF44BB77, 0xFFCC7722, 0xFFAA44CC,
            0xFF22AABB, 0xFFBB4444, 0xFF88AA22, 0xFF448899
    };

    private static final int OVL_ROW_H = 46;
    private static final int OVL_MAX_ROWS = 8;

    private final ChronicleOverviewScreen parent;

    private EditBox searchBox;
    private String query = "";
    private String[] words = new String[0];
    private List<QuestNode> results = List.of();
    private int selectedIdx = 0;
    private int scrollY = 0;
    private String catFilter = "";

    public SearchOverlayScreen(ChronicleOverviewScreen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        ChroniclesTheme t = ChroniclesTheme.current();
        C_PANEL = t.panel.getColor();
        C_PANEL_DARK = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_BORDER_LIT = t.accent.getColor();
        C_SEL_ACCENT = t.accent.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        int panW = Math.min(width - 80, 560);
        int panX = (width - panW) / 2;
        int panY = height / 10;

        searchBox = new EditBox(font, panX + 14, panY + 10, panW - 28, 20, Component.empty());
        searchBox.setHint(Component.literal("§8Search quests…  §7(@category, task names, descriptions, items…)"));
        searchBox.setMaxLength(128);
        searchBox.setFocused(true);
        searchBox.setResponder(v -> {
            query = v;
            words = v.isBlank() ? new String[0] : Arrays.stream(v.toLowerCase().split("\\s+"))
                    .filter(w -> !w.isEmpty()).toArray(String[]::new);
            scrollY = 0;
            selectedIdx = 0;
            results = computeResults();
        });

        results = computeResults();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xFF000000);

        if (searchBox == null) return;

        int panW = Math.min(width - 80, 560);
        int panX = (width - panW) / 2;
        int panY = height / 10;

        int searchRowH = 40;
        int catRowH = 22;
        int dividerH = 9;
        int visRows = Math.min(results.size(), OVL_MAX_ROWS);
        int resultsH = visRows * OVL_ROW_H + (results.isEmpty() ? 32 : 0);
        int footerH = 18;
        int panH = searchRowH + catRowH + dividerH + resultsH + footerH + 16;
        panH = Math.min(panH, height - 80);

        g.fill(panX, panY, panX + panW, panY + panH, C_PANEL);
        g.fill(panX, panY, panX + panW, panY + 1, C_BORDER_LIT);
        g.fill(panX, panY + panH - 1, panX + panW, panY + panH, C_BORDER_LIT);
        g.fill(panX, panY, panX + 1, panY + panH, C_BORDER_LIT);
        g.fill(panX + panW - 1, panY, panX + panW, panY + panH, C_BORDER_LIT);
        g.fill(panX + 1, panY + 1, panX + panW - 1, panY + 2, C_SEL_ACCENT);

        searchBox.setX(panX + 14);
        searchBox.setY(panY + 10);
        searchBox.setWidth(panW - 28);
        searchBox.render(g, mx, my, partial);

        g.fill(panX + panW - 32, panY + 12, panX + panW - 14, panY + 26, 0x22FFFFFF);
        g.drawCenteredString(font, "§8ESC", panX + panW - 23, panY + 16, C_TEXT_FAINT);

        int cy = panY + searchRowH;

        List<String> cats = parent.buildCategoryList();
        int cpx = panX + 14, cpy = cy + 4, cph = 14;

        {
            boolean sel = catFilter.isEmpty();
            int cpw = font.width("All") + 10;
            boolean hov = mx >= cpx && mx < cpx + cpw && my >= cpy && my < cpy + cph;
            int bg = sel ? (C_SEL_ACCENT & 0x00FFFFFF | 0x44000000) : (hov ? 0x22FFFFFF : 0x11FFFFFF);
            g.fill(cpx, cpy, cpx + cpw, cpy + cph, bg);
            if (sel) g.fill(cpx, cpy + cph - 1, cpx + cpw, cpy + cph, C_SEL_ACCENT);
            g.drawString(font, sel ? "§aAll" : "§8All", cpx + 5, cpy + 3, C_TEXT_DIM, false);
            cpx += cpw + 4;
        }
        for (String cat : cats) {
            String label = parent.friendly(cat);
            int cpw = font.width(label) + 10;
            if (cpx + cpw > panX + panW - 14) break;
            boolean sel = catFilter.equalsIgnoreCase(cat);
            boolean hov = mx >= cpx && mx < cpx + cpw && my >= cpy && my < cpy + cph;
            int accent = CAT_ACCENTS[Math.abs(cat.hashCode()) % CAT_ACCENTS.length];
            int bg = sel ? ((accent & 0x00FFFFFF) | 0x44000000) : (hov ? 0x22FFFFFF : 0x11FFFFFF);
            g.fill(cpx, cpy, cpx + cpw, cpy + cph, bg);
            if (sel) g.fill(cpx, cpy + cph - 1, cpx + cpw, cpy + cph, accent);
            g.drawString(font, label, cpx + 5, cpy + 3, sel ? accent : (hov ? C_TEXT_DIM : C_TEXT_FAINT), false);
            cpx += cpw + 4;
        }
        cy += catRowH;

        g.fill(panX + 10, cy + 3, panX + panW - 10, cy + 4, C_BORDER);
        cy += dividerH;

        int listY = cy;
        int listH = panH - (cy - panY) - footerH - 8;
        g.enableScissor(panX + 1, listY, panX + panW - 1, listY + listH);

        if (results.isEmpty()) {
            String msg = words.length == 0 ? "Type to search…" : "No results";
            g.drawCenteredString(font, "§8" + msg, panX + panW / 2, listY + 12, C_TEXT_FAINT);
        }

        int ry = listY - scrollY;
        for (int i = 0; i < results.size(); i++) {
            QuestNode node = results.get(i);
            if (ry + OVL_ROW_H < listY) {
                ry += OVL_ROW_H;
                continue;
            }
            if (ry > listY + listH) break;

            boolean sel = i == selectedIdx;
            boolean hov = mx >= panX + 8 && mx < panX + panW - 8 && my >= ry && my < ry + OVL_ROW_H;
            int rowBg = sel ? 0xFF16162A : (hov ? 0xFF111120 : C_PANEL_DARK);
            g.fill(panX + 8, ry + 2, panX + panW - 8, ry + OVL_ROW_H - 2, rowBg);
            if (sel) g.fill(panX + 8, ry + 2, panX + 10, ry + OVL_ROW_H - 2, 0xFF6688FF);

            String cat = node.getCategory() != null ? node.getCategory() : "MAIN";
            int catAccent = CAT_ACCENTS[Math.abs(cat.hashCode()) % CAT_ACCENTS.length];
            g.fill(panX + 12, ry + 10, panX + 14, ry + OVL_ROW_H - 10, catAccent);

            String title = node.getTitle().getString();
            String titleLow = title.toLowerCase();
            int titleX = panX + 20;
            int titleColor = sel ? 0xFFDDEEFF : C_TEXT;
            String ql = query.toLowerCase().trim();
            if (!ql.isEmpty() && titleLow.contains(ql)) {
                int mi = titleLow.indexOf(ql);
                int preW = font.width(title.substring(0, mi));
                int matchW = font.width(title.substring(mi, mi + ql.length()));
                g.drawString(font, "§8" + title.substring(0, mi), titleX, ry + 8, C_TEXT_DIM, false);
                g.drawString(font, "§f" + title.substring(mi, mi + ql.length()), titleX + preW, ry + 8, 0xFFFFFFAA,
                        false);
                g.drawString(font, "§8" + title.substring(mi + ql.length()), titleX + preW + matchW, ry + 8, titleColor,
                        false);
            } else {
                g.drawString(font, title, titleX, ry + 8, titleColor, false);
            }

            int badgeW = font.width(parent.friendly(cat)) + 8;
            int badgeX = panX + panW - 16 - badgeW;
            g.fill(badgeX, ry + 7, badgeX + badgeW, ry + 18, (catAccent & 0x00FFFFFF) | 0x33000000);
            g.drawString(font, parent.friendly(cat), badgeX + 4, ry + 9, catAccent, false);

            String snippet = extractSnippet(node);
            if (!snippet.isEmpty()) {
                int maxSnipW = badgeX - titleX - 6;
                g.drawString(font, "§8" + font.plainSubstrByWidth(snippet, maxSnipW), titleX, ry + 20, C_TEXT_FAINT,
                        false);
            }

            QuestState st = parent.getState(node);
            String dot = st == QuestState.COMPLETED ? "§a●" : st == QuestState.ACTIVE ? "§6◐" : "§8○";
            g.drawString(font, dot, titleX + Math.min(font.width(title), badgeX - titleX - 30) + 4, ry + 8, 0xFFFFFFFF,
                    false);

            ry += OVL_ROW_H;
        }
        g.disableScissor();

        int footerY = panY + panH - footerH - 2;
        g.fill(panX + 1, footerY, panX + panW - 1, footerY + 1, C_BORDER);
        String footerLeft = results.size() + (results.size() == 1 ? " result" : " results");
        String footerRight = "↑↓ navigate  ·  Enter select  ·  Ctrl+F close";
        g.drawString(font, "§8" + footerLeft, panX + 14, footerY + 5, C_TEXT_FAINT, false);
        g.drawString(font, "§8" + footerRight, panX + panW - font.width(footerRight) - 14, footerY + 5, C_TEXT_FAINT,
                false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 2) != 0;

        if (key == 256 || (key == 70 && ctrl)) {
            onClose();
            return true;
        }
        if (key == 257 || key == 335) {
            if (!results.isEmpty()) selectResult(results.get(Math.min(selectedIdx, results.size() - 1)));
            return true;
        }
        if (key == 264) {
            selectedIdx = Math.min(selectedIdx + 1, results.size() - 1);
            ensureSelectionVisible(OVL_MAX_ROWS * OVL_ROW_H);
            return true;
        }
        if (key == 265) {
            selectedIdx = Math.max(0, selectedIdx - 1);
            ensureSelectionVisible(OVL_MAX_ROWS * OVL_ROW_H);
            return true;
        }
        if (searchBox != null) searchBox.keyPressed(key, scan, mods);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (searchBox != null) searchBox.charTyped(c, mods);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollY = Math.max(0, scrollY - (int) (delta * OVL_ROW_H));
        int max = Math.max(0, results.size() * OVL_ROW_H - OVL_MAX_ROWS * OVL_ROW_H);
        scrollY = Math.min(scrollY, max);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int panW = Math.min(width - 80, 560);
        int panX = (width - panW) / 2;
        int panY = height / 10;

        int searchRowH = 40, catRowH = 22, dividerH = 9;
        int visRows = Math.min(results.size(), OVL_MAX_ROWS);
        int resultsH = visRows * OVL_ROW_H + (results.isEmpty() ? 32 : 0);
        int panH = Math.min(searchRowH + catRowH + dividerH + resultsH + 18 + 16, height - 80);
        if (mx < panX || mx > panX + panW || my < panY || my > panY + panH) {
            onClose();
            return true;
        }

        if (btn == 0) {

            List<String> cats = parent.buildCategoryList();
            int cpx = panX + 14, cpy = panY + searchRowH + 4, cph = 14;
            int allW = font.width("All") + 10;
            if (mx >= cpx && mx < cpx + allW && my >= cpy && my < cpy + cph) {
                catFilter = "";
                results = computeResults();
                selectedIdx = 0;
                scrollY = 0;
                return true;
            }
            cpx += allW + 4;
            for (String cat : cats) {
                int cpw = font.width(parent.friendly(cat)) + 10;
                if (cpx + cpw > panX + panW - 14) break;
                if (mx >= cpx && mx < cpx + cpw && my >= cpy && my < cpy + cph) {
                    catFilter = catFilter.equalsIgnoreCase(cat) ? "" : cat;
                    results = computeResults();
                    selectedIdx = 0;
                    scrollY = 0;
                    return true;
                }
                cpx += cpw + 4;
            }

            int listY = panY + searchRowH + catRowH + dividerH;
            int ry = listY - scrollY;
            for (int i = 0; i < results.size(); i++) {
                if (mx >= panX + 8 && mx < panX + panW - 8 && my >= ry + 2 && my < ry + OVL_ROW_H - 2) {
                    selectResult(results.get(i));
                    return true;
                }
                ry += OVL_ROW_H;
            }
        }
        return true;
    }

    private List<QuestNode> computeResults() {
        List<QuestNode> all = new ArrayList<>(QuestTreeRegistry.getAllQuests().values());
        List<QuestNode> filtered = new ArrayList<>();

        String activeCat = catFilter;
        for (String w : words) {
            if (w.startsWith("@") && w.length() > 1) activeCat = w.substring(1).toUpperCase();
        }

        for (QuestNode n : all) {
            if (n.isFlagDisabled()) continue;
            if (n.getVisibility() == QuestNode.Visibility.HIDDEN) continue;
            if (!activeCat.isEmpty() && !activeCat.equalsIgnoreCase(n.getCategory())) continue;

            if (words.length == 0) {
                filtered.add(n);
                continue;
            }

            String hay = parent.buildSearchHaystack(n);
            boolean allMatch = true;
            for (String w : words) {
                if (!w.startsWith("@") && !hay.contains(w)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) filtered.add(n);
        }

        String q = query.toLowerCase().trim();
        filtered.sort((a, b) -> score(b, q) - score(a, q));
        return filtered;
    }

    private int score(QuestNode n, String q) {
        if (q.isEmpty()) return 0;
        String title = n.getTitle().getString().toLowerCase();
        if (title.startsWith(q)) return 200;
        if (title.contains(q)) return 100;
        int hits = 0;
        for (String w : words) if (!w.startsWith("@") && title.contains(w)) hits++;
        return hits * 30;
    }

    private String extractSnippet(QuestNode n) {
        if (words.length == 0) {
            String d = n.getDescription().getString();
            return d.length() > 72 ? d.substring(0, 70) + "…" : d;
        }
        String hay = parent.searchCache.computeIfAbsent(n.getId(), id -> parent.buildSearchHaystack(n));
        for (String w : words) {
            if (w.startsWith("@")) continue;
            int idx = hay.indexOf(w);
            if (idx < 0) continue;
            int start = Math.max(0, idx - 20);
            int end = Math.min(hay.length(), idx + w.length() + 52);
            String s = (start > 0 ? "…" : "") + hay.substring(start, end).replace('\n', ' ') +
                    (end < hay.length() ? "…" : "");
            return s.length() > 74 ? s.substring(0, 72) + "…" : s;
        }
        return "";
    }

    private void selectResult(QuestNode node) {
        if (minecraft != null) minecraft.setScreen(parent);
        parent.navigateToNode(node);
    }

    private void ensureSelectionVisible(int listH) {
        int top = selectedIdx * OVL_ROW_H;
        int bot = top + OVL_ROW_H;
        if (top < scrollY) scrollY = top;
        else if (bot > scrollY + listH) scrollY = bot - listH;
    }
}
