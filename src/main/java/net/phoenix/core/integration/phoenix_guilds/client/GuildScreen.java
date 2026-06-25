package net.phoenix.core.integration.phoenix_guilds.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.phoenix_guilds.network.C2SGuildActionPacket;
import net.phoenix.core.integration.phoenix_guilds.network.S2CGuildSyncPacket;
import net.phoenix.core.network.PhoenixNetwork;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static net.phoenix.core.integration.phoenix_guilds.client.GuildThemeUtils.*;

@OnlyIn(Dist.CLIENT)
public class GuildScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int W           = 340;
    private static final int H           = 260;
    private static final int HEADER      = 28;
    private static final int TAB_H       = 20;
    private static final int ROW_H       = 15;
    private static final int CONTENT_TOP = HEADER + TAB_H + 6;
    private int px, py;

    // ── State ─────────────────────────────────────────────────────────────────
    private static final String[] TABS = { "Guild", "Allies", "Browse", "Log", "Wiki" };
    private int activeTab = 0;
    private int scrollOff = 0;

    private String selectedWikiTitle = null;

    private record InlineBtn(int x, int y, int w, int h, Runnable action, int colorNorm, int colorHover, String label) {}
    private List<InlineBtn> inlineBtns = new ArrayList<>();

    private EditBox primaryBox;
    private EditBox secondaryBox;
    private EditBox wikiSearchBox;

    public GuildScreen() { super(Component.literal("Guilds")); }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        scrollOff = 0;
        buildWidgets();
    }

    public void onDataRefreshed() { buildWidgets(); }

    // ── Widget construction ───────────────────────────────────────────────────

    private void buildWidgets() {
        clearWidgets();
        inlineBtns = new ArrayList<>();
        switch (activeTab) {
            case 0 -> buildGuildWidgets();
            case 1 -> buildAlliesWidgets();
            case 2 -> buildBrowseWidgets();
            case 4 -> buildWikiWidgets();
        }
    }

    private void buildGuildWidgets() {
        if (!ClientGuildCache.isInGuild()) {
            primaryBox = box(px + 10, py + CONTENT_TOP + 24, W - 20, 16, "Guild name...");
            primaryBox.setMaxLength(32);
            addRenderableWidget(primaryBox);
            addRenderableWidget(btn("Create Guild", px + W / 2 - 55, py + CONTENT_TOP + 46, 110, 18,
                    () -> act(C2SGuildActionPacket.Action.CREATE, primaryBox.getValue().trim())));
            return;
        }
        if (ClientGuildCache.isAtLeastOfficer()) {
            primaryBox = box(px + 10, py + H - 52, W - 114, 14, "Player name...");
            primaryBox.setMaxLength(32);
            addRenderableWidget(primaryBox);
            addRenderableWidget(btn("Invite", px + W - 100, py + H - 53, 90, 16,
                    () -> { act(C2SGuildActionPacket.Action.INVITE, primaryBox.getValue().trim()); primaryBox.setValue(""); }));
        }
        int halfW = (W - 24) / 2;
        int btnY  = py + H - 28;
        addRenderableWidget(btn("Leave Guild", px + 10, btnY,
                ClientGuildCache.isOwner() ? halfW : W - 20, 18,
                () -> act(C2SGuildActionPacket.Action.LEAVE, "")));
        if (ClientGuildCache.isOwner())
            addRenderableWidget(btn("Disband", px + 14 + halfW, btnY, halfW, 18,
                    () -> act(C2SGuildActionPacket.Action.DISBAND, "")));
    }

    private void buildAlliesWidgets() {
        if (!ClientGuildCache.isInGuild() || !ClientGuildCache.isAtLeastOfficer()) return;
        secondaryBox = box(px + 10, py + H - 52, W - 130, 14, "Guild name...");
        secondaryBox.setMaxLength(64);
        addRenderableWidget(secondaryBox);
        addRenderableWidget(btn("Send Request", px + W - 116, py + H - 53, 106, 16,
                () -> { act(C2SGuildActionPacket.Action.ALLY_REQUEST, secondaryBox.getValue().trim()); secondaryBox.setValue(""); }));
    }

    private void buildBrowseWidgets() {
        if (ClientGuildCache.isInGuild()) return;
        primaryBox = box(px + 10, py + H - 52, W - 130, 14, "New guild name...");
        primaryBox.setMaxLength(32);
        addRenderableWidget(primaryBox);
        addRenderableWidget(btn("Create Guild", px + W - 116, py + H - 53, 106, 16,
                () -> act(C2SGuildActionPacket.Action.CREATE, primaryBox.getValue().trim())));
    }

    private void buildWikiWidgets() {
        if (!ClientGuildCache.isInGuild()) return;
        wikiSearchBox = box(px + 4, py + CONTENT_TOP + 2, WIKI_LIST_W, 12, "Search...");
        wikiSearchBox.setMaxLength(32);
        addRenderableWidget(wikiSearchBox);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, C_BG());
        drawFrame(g);
        drawTabs(g);
        inlineBtns = new ArrayList<>();
        switch (activeTab) {
            case 0 -> renderGuildTab(g);
            case 1 -> renderAlliesTab(g);
            case 2 -> renderBrowseTab(g);
            case 3 -> renderLogTab(g);
            case 4 -> renderWikiTab(g, mx, my);
        }
        super.render(g, mx, my, pt);
        for (InlineBtn b : inlineBtns) {
            boolean hover = mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h();
            g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hover ? b.colorHover() : b.colorNorm());
            if (!b.label().isEmpty())
                g.drawCenteredString(font, b.label(), b.x() + b.w() / 2, b.y() + (b.h() - 8) / 2, C_TEXT());
        }
    }

    private void drawFrame(GuiGraphics g) {
        g.fill(px, py, px + W, py + H, C_PANEL());
        g.fill(px, py, px + W, py + HEADER, C_HEADER());
        g.fill(px,         py,         px + W, py + 1,     C_BORDER());
        g.fill(px,         py + H - 1, px + W, py + H,     C_BORDER());
        g.fill(px,         py,         px + 1, py + H,     C_BORDER());
        g.fill(px + W - 1, py,         px + W, py + H,     C_BORDER());
        String title = ClientGuildCache.isInGuild() ? "  " + ClientGuildCache.guildName : "  GUILDS";
        g.drawCenteredString(font, title, px + W / 2, py + (HEADER - 8) / 2,
                ClientGuildCache.isInGuild() ? C_GOLD() : C_TEXT());
        // Theme editor shortcut in top-right corner of the header
        g.drawString(font, "Themes", px + W - 46, py + (HEADER - 8) / 2, C_DIM(), false);
    }

    private void drawTabs(GuiGraphics g) {
        int tabW = W / TABS.length;
        int ty   = py + HEADER;
        for (int i = 0; i < TABS.length; i++) {
            int tx    = px + i * tabW;
            boolean active = i == activeTab;
            boolean alert  = i == 1 && !ClientGuildCache.pendingIncoming.isEmpty();
            g.fill(tx, ty, tx + tabW, ty + TAB_H, active ? C_ACCENT() : C_HEADER());
            if (!active) g.fill(tx, ty + TAB_H - 1, tx + tabW, ty + TAB_H, C_BORDER());
            g.fill(tx + tabW - 1, ty, tx + tabW, ty + TAB_H, C_BORDER2());
            String label = TABS[i] + (alert ? " (!)" : "");
            g.drawCenteredString(font, label, tx + tabW / 2, ty + (TAB_H - 8) / 2, active ? C_TEXT() : C_DIM());
        }
        g.fill(px, py + HEADER + TAB_H, px + W, py + HEADER + TAB_H + 1, C_BORDER());
    }

    // ── Guild tab ─────────────────────────────────────────────────────────────

    private void renderGuildTab(GuiGraphics g) {
        int y = py + CONTENT_TOP;
        if (!ClientGuildCache.isInGuild()) {
            g.drawCenteredString(font, "You are not in a guild.", px + W / 2, y + 4, C_DIM());
            g.drawString(font, "Guild name:", px + 10, y + 16, C_DIM(), false);
            return;
        }

        if (!ClientGuildCache.motd.isBlank()) {
            g.drawString(font, ClientGuildCache.motd, px + 10, y, C_GOLD(), false);
            y += 12;
        }

        String ffStr = "FF: " + (ClientGuildCache.friendlyFire ? "ON" : "OFF");
        String hmStr = ClientGuildCache.homeSet ? "Home Set" : "No Home";
        g.drawString(font, ffStr, px + 10, y, C_DIM(), false);
        g.drawString(font, hmStr, px + W - 10 - font.width(hmStr), y, C_DIM(), false);
        y += 12;

        sectionHeader(g, "Members", px + 10, y, W - 20);
        y += 12;

        int listTop = y;
        int listBot = py + H - (ClientGuildCache.isAtLeastOfficer() ? 62 : 34);
        int listH   = listBot - listTop;
        int maxRows = listH / ROW_H;

        List<S2CGuildSyncPacket.MemberEntry> members = ClientGuildCache.members;
        scissor(listTop, listH);
        for (int i = scrollOff; i < members.size() && i - scrollOff < maxRows; i++) {
            S2CGuildSyncPacket.MemberEntry m = members.get(i);
            int ry = listTop + (i - scrollOff) * ROW_H;
            if (i % 2 == 0) g.fill(px + 2, ry, px + W - 2, ry + ROW_H - 1, C_ROW_ALT());
            int nameColor;
            String rankIcon;
            switch (m.rank()) {
                case "OWNER"   -> { rankIcon = "*"; nameColor = C_GOLD(); }
                case "OFFICER" -> { rankIcon = "^"; nameColor = C_OFFICER(); }
                default        -> { rankIcon = "-"; nameColor = C_TEXT(); }
            }
            g.fill(px + 10, ry + 4, px + 14, ry + 9, m.isOnline() ? C_ONLINE() : C_OFFLINE());
            g.drawString(font, rankIcon + " " + m.name(), px + 18, ry + 3, nameColor, false);
            if (!ClientGuildCache.isOwner()) {
                String status = m.isOnline() ? "online" : "offline";
                g.drawString(font, status, px + W - 10 - font.width(status), ry + 3,
                        m.isOnline() ? C_ONLINE() : C_OFFLINE(), false);
            }
        }
        RenderSystem.disableScissor();

        if (ClientGuildCache.isOwner()) {
            for (int i = scrollOff; i < members.size() && i - scrollOff < maxRows; i++) {
                S2CGuildSyncPacket.MemberEntry m = members.get(i);
                if (m.uuid().equals(ClientGuildCache.ownerUUID)) continue;
                int ry = listTop + (i - scrollOff) * ROW_H;
                if (ry < listTop || ry + ROW_H > listTop + listH) continue;
                int bh = ROW_H - 3;
                int bw = 24;
                int bx = px + W - 8 - bw;
                final String mName = m.name();
                final String mRank = m.rank();
                inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                        () -> act(C2SGuildActionPacket.Action.REMOVE, mName),
                        0xFF330A0A, 0xFF551010, "X"));
                bx -= bw + 2;
                if ("OFFICER".equals(mRank))
                    inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                            () -> act(C2SGuildActionPacket.Action.DEMOTE, mName),
                            0xFF0A1033, 0xFF101855, "v"));
                else
                    inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                            () -> act(C2SGuildActionPacket.Action.PROMOTE, mName),
                            0xFF0A330A, 0xFF105510, "^"));
            }
        }

        if (members.isEmpty()) g.drawCenteredString(font, "No members.", px + W / 2, listTop + 4, C_DIM());
        if (ClientGuildCache.isAtLeastOfficer())
            g.drawString(font, "Invite player:", px + 10, py + H - 70, C_DIM(), false);
        g.fill(px, py + H - 34, px + W, py + H - 33, C_BORDER2());
    }

    // ── Allies tab ────────────────────────────────────────────────────────────

    private void renderAlliesTab(GuiGraphics g) {
        if (!ClientGuildCache.isInGuild()) {
            g.drawCenteredString(font, "You must be in a guild to form alliances.", px + W / 2, py + 100, C_DIM());
            return;
        }
        int y = py + CONTENT_TOP;

        List<S2CGuildSyncPacket.PendingEntry> incoming = ClientGuildCache.pendingIncoming;
        if (!incoming.isEmpty()) {
            sectionHeader(g, "Incoming Requests", px + 10, y, W - 20);
            y += 14;
            for (int i = 0; i < incoming.size(); i++) {
                S2CGuildSyncPacket.PendingEntry req = incoming.get(i);
                int ry = y + i * (ROW_H + 2);
                g.fill(px + 6, ry, px + W - 6, ry + ROW_H, 0xFF100A20);
                g.drawString(font, req.guildName(), px + 12, ry + 3, 0xFFFFCC44, false);
                if (ClientGuildCache.isAtLeastOfficer()) {
                    int bw = 46; int bh = ROW_H - 2;
                    int bx = px + W - 10 - bw;
                    final String rName = req.guildName();
                    inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                            () -> act(C2SGuildActionPacket.Action.ALLY_DECLINE, rName),
                            0xFF330A10, 0xFF551020, "Decline"));
                    bx -= bw + 2;
                    inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                            () -> act(C2SGuildActionPacket.Action.ALLY_ACCEPT, rName),
                            0xFF0A3318, 0xFF105528, "Accept"));
                }
            }
            y += incoming.size() * (ROW_H + 2) + 6;
        }

        sectionHeader(g, "Allied Guilds", px + 10, y, W - 20);
        y += 14;
        List<S2CGuildSyncPacket.AllyEntry> allies = ClientGuildCache.allies;
        if (allies.isEmpty()) {
            g.drawString(font, "No allies yet.", px + 12, y + 3, C_FAINT(), false);
        } else {
            for (int i = 0; i < allies.size(); i++) {
                S2CGuildSyncPacket.AllyEntry a = allies.get(i);
                int ry = y + i * (ROW_H + 2);
                if (i % 2 == 0) g.fill(px + 2, ry, px + W - 2, ry + ROW_H - 1, C_ROW_ALT());
                g.drawString(font, a.name(), px + 12, ry + 3, C_ALLY(), false);
                String cnt = a.onlineCount() + "/" + a.memberCount() + " online";
                g.drawString(font, cnt, px + W - 10 - font.width(cnt), ry + 3, C_DIM(), false);
                if (ClientGuildCache.isAtLeastOfficer()) {
                    int bw = 38; int bh = ROW_H - 2;
                    int bx = px + W - 10 - font.width(cnt) - bw - 4;
                    final String aName = a.name();
                    inlineBtns.add(new InlineBtn(bx, ry + 1, bw, bh,
                            () -> act(C2SGuildActionPacket.Action.ALLY_BREAK, aName),
                            0xFF330A10, 0xFF551020, "Break"));
                }
            }
            y += allies.size() * (ROW_H + 2) + 4;
        }

        List<S2CGuildSyncPacket.PendingEntry> outgoing = ClientGuildCache.pendingOutgoing;
        if (!outgoing.isEmpty()) {
            y += 4;
            sectionHeader(g, "Sent Requests", px + 10, y, W - 20);
            y += 14;
            for (int i = 0; i < outgoing.size(); i++)
                g.drawString(font, "- " + outgoing.get(i).guildName(), px + 12, y + i * ROW_H + 3, C_DIM(), false);
        }

        if (ClientGuildCache.isAtLeastOfficer())
            g.drawString(font, "Send alliance request:", px + 10, py + H - 70, C_DIM(), false);
    }

    // ── Browse tab ────────────────────────────────────────────────────────────

    private void renderBrowseTab(GuiGraphics g) {
        List<S2CGuildSyncPacket.GuildSummary> all = ClientGuildCache.allGuilds;
        int listTop = py + CONTENT_TOP + 14;
        int listBot = ClientGuildCache.isInGuild() ? py + H - 10 : py + H - 60;
        int listH   = listBot - listTop;
        int maxRows = listH / ROW_H;

        sectionHeader(g, "All Guilds (" + all.size() + ")", px + 10, py + CONTENT_TOP, W - 20);

        scissor(listTop, listH);
        for (int i = scrollOff; i < all.size() && i - scrollOff < maxRows; i++) {
            S2CGuildSyncPacket.GuildSummary gs = all.get(i);
            int ry = listTop + (i - scrollOff) * ROW_H;
            if (i % 2 == 0) g.fill(px + 2, ry, px + W - 2, ry + ROW_H - 1, C_ROW_ALT());
            g.drawString(font, gs.name(), px + 12, ry + 3, C_TEXT(), false);
            String cnt = gs.onlineCount() + "/" + gs.memberCount();
            g.drawString(font, cnt, px + W - 10 - font.width(cnt), ry + 3, C_DIM(), false);
        }
        RenderSystem.disableScissor();

        if (all.isEmpty()) g.drawCenteredString(font, "No guilds exist yet.", px + W / 2, listTop + 4, C_DIM());
        if (!ClientGuildCache.isInGuild())
            g.drawString(font, "Create a new guild:", px + 10, py + H - 70, C_DIM(), false);
    }

    // ── Log tab ───────────────────────────────────────────────────────────────

    private void renderLogTab(GuiGraphics g) {
        if (!ClientGuildCache.isInGuild()) {
            g.drawCenteredString(font, "You must be in a guild to view the log.", px + W / 2, py + 100, C_DIM());
            return;
        }
        List<S2CGuildSyncPacket.LogEntry> log = ClientGuildCache.logEntries;
        sectionHeader(g, "Guild Log", px + 10, py + CONTENT_TOP, W - 20);
        int listTop = py + CONTENT_TOP + 14;
        int listH   = py + H - 10 - listTop;
        int maxRows = listH / ROW_H;

        if (log.isEmpty()) {
            g.drawCenteredString(font, "No events recorded yet.", px + W / 2, listTop + 4, C_DIM());
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd HH:mm");
        scissor(listTop, listH);
        for (int i = scrollOff; i < log.size() && i - scrollOff < maxRows; i++) {
            S2CGuildSyncPacket.LogEntry e = log.get(i);
            int ry = listTop + (i - scrollOff) * ROW_H;
            if (i % 2 == 0) g.fill(px + 2, ry, px + W - 2, ry + ROW_H - 1, C_ROW_ALT());
            String time = fmt.format(new Date(e.timestamp()));
            g.drawString(font, time, px + 10, ry + 3, C_DIM(), false);
            g.drawString(font, e.message(), px + 14 + font.width(time), ry + 3, C_TEXT(), false);
        }
        RenderSystem.disableScissor();
    }

    // ── Wiki tab ──────────────────────────────────────────────────────────────

    private static final int WIKI_LIST_W = 114;

    private void renderWikiTab(GuiGraphics g, int mx, int my) {
        if (!ClientGuildCache.isInGuild()) {
            g.drawCenteredString(font, "You must be in a guild to view the wiki.", px + W / 2, py + 100, C_DIM());
            return;
        }

        List<S2CGuildSyncPacket.WikiPage> pages = ClientGuildCache.wikiPages;
        boolean officer = ClientGuildCache.isAtLeastOfficer();

        int lx      = px + 4;
        int listTop = py + CONTENT_TOP + 16;
        int listBot = py + H - (officer ? 24 : 10);
        int listH   = listBot - listTop;
        int maxRows = listH / ROW_H;

        String query = wikiSearchBox != null ? wikiSearchBox.getValue().trim().toLowerCase() : "";
        List<S2CGuildSyncPacket.WikiPage> filtered = pages.stream()
                .filter(p -> query.isEmpty() || p.title().toLowerCase().contains(query))
                .collect(Collectors.toList());

        int divX = lx + WIKI_LIST_W + 2;
        g.fill(divX, py + CONTENT_TOP, divX + 1, py + H - 2, C_BORDER2());

        scissor(listTop, listH);
        for (int i = scrollOff; i < filtered.size() && i - scrollOff < maxRows; i++) {
            S2CGuildSyncPacket.WikiPage page = filtered.get(i);
            int ry = listTop + (i - scrollOff) * ROW_H;
            boolean selected = page.title().equals(selectedWikiTitle);
            g.fill(lx, ry, lx + WIKI_LIST_W, ry + ROW_H - 1,
                    selected ? C_ACCENT() : (i % 2 == 0 ? C_ROW_ALT() : 0));
            String title = page.title();
            if (font.width(title) > WIKI_LIST_W - 8) title = font.plainSubstrByWidth(title, WIKI_LIST_W - 14) + "…";
            g.drawString(font, title, lx + 4, ry + 3, selected ? C_TEXT() : C_DIM(), false);
        }
        RenderSystem.disableScissor();

        if (filtered.isEmpty())
            g.drawString(font, pages.isEmpty() ? "No pages yet." : "No results.", lx + 4, listTop + 3, C_FAINT(), false);

        if (officer)
            inlineBtns.add(new InlineBtn(lx, py + H - 22, WIKI_LIST_W, 14,
                    () -> minecraft.setScreen(new WikiEditScreen(this, "", "")),
                    0xFF0A1A33, 0xFF10285A, "+ New Page"));

        for (int i = scrollOff; i < filtered.size() && i - scrollOff < maxRows; i++) {
            S2CGuildSyncPacket.WikiPage page = filtered.get(i);
            int ry = listTop + (i - scrollOff) * ROW_H;
            final String t = page.title();
            inlineBtns.add(new InlineBtn(lx, ry, WIKI_LIST_W, ROW_H - 1,
                    () -> { selectedWikiTitle = t; scrollOff = 0; }, 0, 0, ""));
        }

        int rx = divX + 4;
        int rw = W - WIKI_LIST_W - 12;
        int ry = py + CONTENT_TOP;

        if (selectedWikiTitle != null) {
            S2CGuildSyncPacket.WikiPage sel = pages.stream()
                    .filter(p -> p.title().equals(selectedWikiTitle)).findFirst().orElse(null);
            if (sel != null) {
                g.drawString(font, sel.title(), rx, ry, C_GOLD(), false);
                g.fill(rx, ry + 10, rx + rw, ry + 11, C_BORDER2());
                ry += 14;
                int contentBot = py + H - (officer ? 26 : 10);
                scissor(ry, contentBot - ry);
                List<net.minecraft.util.FormattedCharSequence> lines =
                        font.split(Component.literal(sel.content()), rw);
                for (int i = 0; i < lines.size(); i++)
                    g.drawString(font, lines.get(i), rx, ry + i * 10, C_TEXT(), false);
                RenderSystem.disableScissor();
                if (officer) {
                    final String selTitle   = sel.title();
                    final String selContent = sel.content();
                    int bw = 46; int by = py + H - 22;
                    inlineBtns.add(new InlineBtn(rx, by, bw, 14,
                            () -> minecraft.setScreen(new WikiEditScreen(this, selTitle, selContent)),
                            0xFF0A1A33, 0xFF10285A, "Edit"));
                    inlineBtns.add(new InlineBtn(rx + bw + 4, by, bw, 14,
                            () -> { act(C2SGuildActionPacket.Action.WIKI_DELETE, selTitle); selectedWikiTitle = null; },
                            0xFF330A0A, 0xFF551010, "Delete"));
                }
            } else { selectedWikiTitle = null; }
        } else {
            g.drawCenteredString(font, "Select a page.", rx + rw / 2, py + CONTENT_TOP + 40, C_FAINT());
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int tabW = W / TABS.length;
        if (my >= py + HEADER && my < py + HEADER + TAB_H) {
            for (int i = 0; i < TABS.length; i++) {
                int tx = px + i * tabW;
                if (mx >= tx && mx < tx + tabW) {
                    activeTab = i; scrollOff = 0; buildWidgets(); return true;
                }
            }
        }
        // Header "Themes" click
        int themesBtnX = px + W - 46;
        int themesBtnY = py + (HEADER - 8) / 2;
        if (mx >= themesBtnX && mx < themesBtnX + 40 && my >= themesBtnY && my < themesBtnY + 8) {
            minecraft.setScreen(new GuildThemeEditorScreen(this));
            return true;
        }
        for (InlineBtn b : inlineBtns) {
            if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
                b.action().run(); return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int size = switch (activeTab) {
            case 0 -> ClientGuildCache.members.size();
            case 2 -> ClientGuildCache.allGuilds.size();
            case 3 -> ClientGuildCache.logEntries.size();
            case 4 -> ClientGuildCache.wikiPages.size();
            default -> 0;
        };
        scrollOff = (int) Math.max(0, Math.min(Math.max(0, size - 8), scrollOff - delta));
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void act(C2SGuildActionPacket.Action action, String arg) {
        boolean needsArg = action != C2SGuildActionPacket.Action.LEAVE
                        && action != C2SGuildActionPacket.Action.DISBAND
                        && action != C2SGuildActionPacket.Action.TOGGLE_FF
                        && action != C2SGuildActionPacket.Action.HOME
                        && action != C2SGuildActionPacket.Action.SET_HOME;
        if (needsArg && arg.isEmpty()) return;
        PhoenixNetwork.CHANNEL.sendToServer(new C2SGuildActionPacket(action, arg));
    }

    private EditBox box(int x, int y, int w, int h, String hint) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(hint));
        b.setHint(Component.literal(hint));
        return b;
    }

    private Button btn(String label, int x, int y, int w, int h, Runnable action) {
        return Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, h).build();
    }

    private void sectionHeader(GuiGraphics g, String label, int x, int y, int w) {
        g.fill(x, y + 5, x + w, y + 6, C_BORDER2());
        int lw = font.width(label) + 6;
        g.fill(x, y + 5, x + lw, y + 6, C_PANEL());
        g.drawString(font, label, x + 2, y, C_DIM(), false);
    }

    private void scissor(int top, int h) {
        double scale = minecraft.getWindow().getGuiScale();
        int    sh    = minecraft.getWindow().getGuiScaledHeight();
        RenderSystem.enableScissor(
                (int) ((px + 1)       * scale),
                (int) ((sh - top - h) * scale),
                (int) ((W - 2)        * scale),
                (int) (h              * scale));
    }

    @Override public boolean isPauseScreen() { return false; }
}
