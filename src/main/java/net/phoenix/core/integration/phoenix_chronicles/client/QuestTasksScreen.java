package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.C2SClaimQuestRewardPacket;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.List;

public class QuestTasksScreen extends Screen {

    private int C_BG = 0xFF0B0B0F;
    private int C_PANEL = 0xFF14141A;
    private int C_HEADER = 0xFF0C0C10;
    private int C_BORDER = 0xFF353548;
    private int C_DONE = 0xFF44CC88;
    private int C_ACTIVE = 0xFFFFBB33;
    private int C_LOCKED = 0xFF606070;
    private int C_TEXT = 0xFFD8D8E4;
    private int C_TEXT_DIM = 0xFF7A7A8A;
    private int C_TEXT_FAINT = 0xFF404050;

    private static final int C_SLOT_BG = 0xFF0E0E14;
    private static final int C_SLOT_HI = 0xFF3A2A00;

    private static final int HEADER_H = 28;
    private static final int REQBAR_H = 48;
    private static final int FOOTER_H = 22;
    private static final int MARGIN = 8;
    private static final int REWARD_W = 140;
    private static final int TASK_ICON_SZ = 24;

    private static final int CARD_W = 310;
    private static final int CARD_PAD = 6;
    private static final int CARD_TASK_ROW_H = 22;
    private static final int CARD_MAX_TASKS = 6;
    private static final int CARD_MAX_DESC = 10;

    private final Screen parent;
    private final QuestNode node;
    private final FullQuestData content;
    private final PlayerQuestData playerData;
    private final Player player;

    private int descScrollY = 0;
    private long openTimeMs = -1;
    private static final long OPEN_FADE_MS = 100;
    private int inspectorTab = 2; 
    private int inspectorScrollY = 0;
    private boolean isFullscreen = false;

    public QuestTasksScreen(Screen parent, QuestNode node, FullQuestData content, PlayerQuestData playerData) {
        super(Component.literal("Quest Details"));
        this.parent = parent;
        this.node = node;
        this.content = content;
        this.playerData = playerData;
        this.player = net.minecraft.client.Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init();
        openTimeMs = System.currentTimeMillis();
        ChroniclesTheme t = ChroniclesTheme.current();
        C_BG = t.bg.getColor();
        C_PANEL = t.panel.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        C_DONE = t.done.getColor();
        C_ACTIVE = t.activeColor.getColor();
        C_LOCKED = t.locked.getColor();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (isFullscreen) {
            renderFullscreen(g, mx, my, partial);
        } else {
            renderCompact(g, mx, my, partial);
        }

        if (openTimeMs > 0) {
            long elapsed = System.currentTimeMillis() - openTimeMs;
            if (elapsed < OPEN_FADE_MS) {
                float frac = 1f - (float) elapsed / OPEN_FADE_MS;
                int alpha = (int) (frac * frac * 0xFF) & 0xFF;
                if (alpha > 0) g.fill(0, 0, width, height, (alpha << 24));
            }
        }
    }

    private static final int TASK_LIST_ROW_H = 24; 
    
    private static final int REWARD_MINI_SZ = 14;

    private int compactCardH(List<QuestTask> tasks, List<QuestReward> rewards,
                             java.util.List<net.minecraft.util.FormattedCharSequence> descLines) {
        int visible = Math.min(tasks.size(), CARD_MAX_TASKS);
        int taskSecH = 14 + visible * TASK_LIST_ROW_H + (tasks.size() > CARD_MAX_TASKS ? 10 : 0) + 2;
        int fixedH = 20 + 1 + taskSecH + 1 + 18;
        int allDescLines = buildAllDescLines(tasks, descLines).size();
        int rawDesc = Math.min(allDescLines, CARD_MAX_DESC);
        int fitted = Math.max(0, Math.min(rawDesc, ((height - 20) - fixedH - 9) / 10));
        int descH = fitted > 0 ? 4 + fitted * 10 + 4 : 0;
        return fixedH + descH + (descH > 0 ? 1 : 0);
    }

    private java.util.List<net.minecraft.util.FormattedCharSequence> buildAllDescLines(
                                                                                       List<QuestTask> tasks,
                                                                                       java.util.List<net.minecraft.util.FormattedCharSequence> questDescLines) {
        java.util.List<net.minecraft.util.FormattedCharSequence> all = new java.util.ArrayList<>();
        for (QuestTask task : tasks) {
            if (task instanceof net.phoenix.core.integration.phoenix_chronicles.tasks.InfoTask info) {
                String body = info.getBody();
                if (body != null && !body.isBlank()) {
                    all.addAll(font.split(Component.literal(body), CARD_W - CARD_PAD * 2));
                }
            }
        }
        if (!all.isEmpty() && !questDescLines.isEmpty()) {
            
            all.add(net.minecraft.util.FormattedCharSequence.EMPTY);
        }
        all.addAll(questDescLines);
        return all;
    }

    private QuestTask hoveredTask = null;
    private QuestReward hoveredReward = null;
    private int hoveredSlotX, hoveredSlotY; 

    private void renderCompact(GuiGraphics g, int mx, int my, float partial) {
        hoveredTask = null;
        hoveredReward = null;

        if (parent instanceof ChronicleOverviewScreen overview) {
            overview.renderForChildScreen(g);
        } else if (parent != null) {
            parent.render(g, -9999, -9999, partial);
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        } else {
            g.fill(0, 0, width, height, C_BG);
        }

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 300f);
        g.fill(0, 0, width, height, 0x88000000);

        List<QuestTask> tasks = node.getTasks();
        List<QuestReward> rewards = node.getRewards();
        String descText = (content != null && content.description() != null) ? content.description().getString() : null;
        java.util.List<net.minecraft.util.FormattedCharSequence> questDescLines = descText != null ?
                font.split(Component.literal(descText), CARD_W - CARD_PAD * 2) : java.util.List.of();
        java.util.List<net.minecraft.util.FormattedCharSequence> descLines = buildAllDescLines(tasks, questDescLines);

        int taskSectionH = 14 + Math.min(tasks.size(), CARD_MAX_TASKS) * TASK_LIST_ROW_H +
                (tasks.size() > CARD_MAX_TASKS ? 10 : 0) + 2;
        int fixedH = 20 + 1 + taskSectionH + 1 + 18;
        int rawDesc = Math.min(descLines.size(), CARD_MAX_DESC);
        int fittedDesc = Math.max(0, Math.min(rawDesc, ((height - 20) - fixedH - 9) / 10));
        int descH = fittedDesc > 0 ? 4 + fittedDesc * 10 + 4 : 0;
        int cardH = fixedH + descH + (descH > 0 ? 1 : 0);

        int cardX = (width - CARD_W) / 2;
        int cardY = Math.max(10, (height - cardH) / 2);

        g.fill(cardX + 3, cardY + 3, cardX + CARD_W + 3, cardY + cardH + 3, 0x66000000);
        g.fill(cardX, cardY, cardX + CARD_W, cardY + cardH, C_BG);
        drawBorder(g, cardX, cardY, CARD_W, cardH);

        int cy = cardY;

        g.fill(cardX, cy, cardX + CARD_W, cy + 20, C_HEADER);
        String title = node.getTitle().getString();
        if (font.width(title) > CARD_W - 32) title = font.plainSubstrByWidth(title, CARD_W - 38) + "…";
        g.drawString(font, "§f" + title, cardX + CARD_PAD, cy + 6, C_TEXT, false);
        boolean fsHov = mx >= cardX + CARD_W - 18 && mx < cardX + CARD_W - 4 && my >= cy + 3 && my < cy + 17;
        if (fsHov) g.fill(cardX + CARD_W - 18, cy + 3, cardX + CARD_W - 4, cy + 17, 0x33FFFFFF);
        g.drawCenteredString(font, "§7⛶", cardX + CARD_W - 11, cy + 6, fsHov ? C_ACTIVE : C_TEXT_FAINT);
        cy += 20;
        g.fill(cardX, cy, cardX + CARD_W, cy + 1, C_BORDER);
        cy += 1;

        int listX = cardX + CARD_PAD;
        int listW = CARD_W - CARD_PAD * 2;
        int visible = Math.min(tasks.size(), CARD_MAX_TASKS);
        boolean hasMore = tasks.size() > CARD_MAX_TASKS;

        long doneCount = tasks.stream().filter(this::isTaskDone).count();
        g.drawString(font, "§8TASKS §7" + doneCount + "§8/§7" + tasks.size(),
                listX, cy + 3, C_TEXT_FAINT, false);

        int maxRewardMini = Math.min(rewards.size(), (listW - 70) / (REWARD_MINI_SZ + 2));
        int rx = cardX + CARD_W - CARD_PAD;
        for (int i = 0; i < maxRewardMini; i++) {
            rx -= REWARD_MINI_SZ + 2;
            QuestReward rw = rewards.get(i);
            boolean rhov = mx >= rx && mx < rx + REWARD_MINI_SZ && my >= cy + 1 && my < cy + 1 + REWARD_MINI_SZ;
            if (rhov) {
                hoveredReward = rw;
                hoveredSlotX = rx;
                hoveredSlotY = cy + 1;
            }
            renderRewardSlot(g, rx, cy + 1, REWARD_MINI_SZ, rw, mx, my);
        }
        if (rewards.size() > maxRewardMini && maxRewardMini > 0) {
            rx -= font.width("+" + (rewards.size() - maxRewardMini)) + 2;
            g.drawString(font, "§8+" + (rewards.size() - maxRewardMini), rx, cy + 3, C_TEXT_FAINT, false);
        }
        cy += 14;

        for (int i = 0; i < visible; i++) {
            renderCompactTaskRow(g, listX, cy, listW, tasks.get(i), mx, my);
            cy += TASK_LIST_ROW_H;
        }
        if (hasMore) {
            g.drawString(font, "§8+ " + (tasks.size() - CARD_MAX_TASKS) + " more…",
                    listX + 4, cy + 1, C_TEXT_FAINT, false);
            cy += 10;
        }
        cy += 2;
        g.fill(cardX, cy, cardX + CARD_W, cy + 1, C_BORDER);
        cy += 1;

        if (descH > 0) {
            int dy = cy + 4;
            for (int i = 0; i < fittedDesc; i++) {
                g.drawString(font, descLines.get(i), cardX + CARD_PAD, dy, C_TEXT_DIM, false);
                dy += 10;
            }
            cy += descH;
            g.fill(cardX, cy, cardX + CARD_W, cy + 1, C_BORDER);
            cy += 1;
        }

        renderCompactFooter(g, cardX, cy, CARD_W, 18, mx, my);

        if (hoveredTask != null) {
            g.pose().translate(0f, 0f, 200f);
            g.renderComponentTooltip(font, buildTaskTooltip(hoveredTask), mx, my);
        } else if (hoveredReward != null) {
            g.pose().translate(0f, 0f, 200f);
            g.renderComponentTooltip(font, buildRewardTooltip(hoveredReward), mx, my);
        }

        g.pose().popPose();
    }

    private void renderCompactTaskRow(GuiGraphics g, int x, int y, int w, QuestTask task, int mx, int my) {
        boolean done = isTaskDone(task);
        String progress = player != null ? task.getProgressString(player) : null;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + TASK_LIST_ROW_H - 6;
        if (hov) {
            hoveredTask = task;
            hoveredSlotX = x;
            hoveredSlotY = y;
        }

        if (hov) g.fill(x, y, x + w, y + TASK_LIST_ROW_H - 6, 0x18FFFFFF);

        int accent = done ? C_DONE : (task.isOptional() ? C_TEXT_FAINT : C_ACTIVE);
        g.fill(x, y + 1, x + 2, y + TASK_LIST_ROW_H - 7, accent);

        String mark = done ? "§a✔" : (task.isOptional() ? "§8○" : "§c✗");
        g.drawString(font, mark, x + 4, y + 3, 0xFFFFFFFF, false);

        int textX = x + 16;
        ItemStack icon = getTaskIcon(task);
        if (!icon.isEmpty()) {
            g.renderItem(icon, textX, y);
            textX += 18;
        } else {
            g.drawString(font, getTaskGlyph(task), textX, y + 3, 0xFFFFFFFF, false);
            textX += 10;
        }

        String desc = task.getDescription().getString();
        String detail = getTaskDetail(task);
        
        boolean descIsId = desc.isEmpty() || desc.matches("[a-z0-9_]+");
        String primary = (descIsId && detail != null) ? detail : desc;

        String prog = done ? "§a✔" : (progress != null ? "§8" + progress : "");
        int progW = prog.isEmpty() ? 0 : font.width(prog) + 2;
        int labelW = w - (textX - x) - progW - 2;
        if (font.width(primary) > labelW)
            primary = font.plainSubstrByWidth(primary, labelW - 5) + "…";
        g.drawString(font, (done ? "§7" : "§f") + primary, textX, y + 3, done ? C_TEXT_DIM : C_TEXT, false);
        if (!prog.isEmpty()) {
            g.drawString(font, prog, x + w - progW, y + 3, C_TEXT_FAINT, false);
        }

        float pct = done ? 1f : parseProgress(progress);
        int barY = y + 14;
        g.fill(x, barY, x + w, barY + 3, 0xFF1A1A22);
        if (pct > 0) g.fill(x, barY, x + (int) (w * pct), barY + 3, done ? C_DONE : C_ACTIVE);
    }

    private void renderCompactFooter(GuiGraphics g, int cardX, int cy, int cardW, int h, int mx, int my) {
        QuestState state = playerData != null ? playerData.getQuestState(node.getId(), QuestState.LOCKED) :
                QuestState.LOCKED;
        boolean canClaim = state == QuestState.COMPLETED && !rewardsClaimed() && !node.getRewards().isEmpty();

        if (canClaim) {
            int btnW = 120;
            int btnX = cardX + (cardW - btnW) / 2;
            int btnY = cy + 1;
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + h - 2;
            g.fill(btnX, btnY, btnX + btnW, btnY + h - 2, hov ? 0xFF2A4A2A : 0xFF1A2A1A);
            g.fill(btnX, btnY, btnX + btnW, btnY + 1, hov ? C_DONE : 0xFF333333);
            g.drawCenteredString(font, "§a✓ Claim Rewards", btnX + btnW / 2, btnY + 4, hov ? C_DONE : C_TEXT);
        } else if (rewardsClaimed()) {
            g.drawCenteredString(font, "§8Rewards claimed", cardX + cardW / 2, cy + 5, C_TEXT_FAINT);
        } else {
            g.drawCenteredString(font, "§8Complete tasks to claim", cardX + cardW / 2, cy + 5, C_TEXT_FAINT);
        }
    }

    private void renderFullscreen(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, C_BG);
        renderHeader(g, mx, my);
        renderRequirementsBar(g, mx, my);

        int contentTop = HEADER_H + REQBAR_H + MARGIN;
        int contentRight = width - REWARD_W - MARGIN - MARGIN;
        int contentH = height - contentTop - FOOTER_H - MARGIN;

        g.fill(MARGIN, contentTop, contentRight, contentTop + contentH, C_PANEL);
        drawBorder(g, MARGIN, contentTop, contentRight - MARGIN, contentH);
        renderContent(g, MARGIN + 8, contentTop + 8, contentRight - MARGIN - 16, contentH - 16, mx, my);

        renderInspector(g, contentRight + MARGIN, contentTop, REWARD_W, contentH, mx, my);
        renderFooter(g, mx, my);
    }

    private void renderHeader(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);

        if (mx >= 4 && mx < 20 && my >= 6 && my < 22) g.fill(4, 6, 20, 22, 0x22FFFFFF);
        g.drawCenteredString(font, "§7←", 12, 10, C_TEXT_DIM);
        
        int titleMaxW = width - 28 - 60;
        String titleStr = node.getTitle().getString();
        if (font.width(titleStr) > titleMaxW) titleStr = font.plainSubstrByWidth(titleStr, titleMaxW - 6) + "…";
        g.drawString(font, "§f" + titleStr, 28, 10, C_TEXT, false);

        int fsX = width - 36;
        if (mx >= fsX && mx < fsX + 16 && my >= 6 && my < 22) g.fill(fsX, 6, fsX + 16, 22, 0x22FFFFFF);
        g.drawCenteredString(font, "§d⛶", fsX + 8, 10, 0xFFAA44FF);

        boolean pinned = playerData != null && playerData.isPinned(node.getId());
        int pinX = width - 20;
        if (mx >= pinX && mx < width - 4 && my >= 6 && my < 22) g.fill(pinX, 6, width - 4, 22, 0x22FFFFFF);
        g.drawCenteredString(font, pinned ? "§d📌" : "§8📌", width - 12, 10, pinned ? 0xFFAA44FF : C_TEXT_FAINT);
    }

    private void renderRequirementsBar(GuiGraphics g, int mx, int my) {
        g.fill(0, HEADER_H, width, HEADER_H + REQBAR_H, C_PANEL);
        g.fill(0, HEADER_H, width, HEADER_H + 1, C_BORDER);
        g.fill(0, HEADER_H + REQBAR_H - 1, width, HEADER_H + REQBAR_H, C_BORDER);
        g.drawString(font, "§8TASKS:", MARGIN, HEADER_H + 6, C_TEXT_FAINT, false);

        List<QuestTask> tasks = node.getTasks();
        int iconX = MARGIN + 50;
        int iconY = HEADER_H + (REQBAR_H - TASK_ICON_SZ) / 2;
        int done = 0;

        for (QuestTask task : tasks) {
            if (iconX + TASK_ICON_SZ + 4 > width - MARGIN - 40) break;
            boolean taskDone = isTaskDone(task);
            if (taskDone) done++;

            int bg = taskDone ? 0xFF0D1A0F : 0xFF171720;
            int border = taskDone ? C_DONE : (task.isOptional() ? C_TEXT_FAINT : C_ACTIVE);
            g.fill(iconX, iconY, iconX + TASK_ICON_SZ, iconY + TASK_ICON_SZ, bg);
            g.fill(iconX, iconY, iconX + TASK_ICON_SZ, iconY + 1, border);
            g.fill(iconX, iconY + TASK_ICON_SZ - 1, iconX + TASK_ICON_SZ, iconY + TASK_ICON_SZ, border);
            g.fill(iconX, iconY, iconX + 1, iconY + TASK_ICON_SZ, border);
            g.fill(iconX + TASK_ICON_SZ - 1, iconY, iconX + TASK_ICON_SZ, iconY + TASK_ICON_SZ, border);

            ItemStack icon = getTaskIcon(task);
            if (!icon.isEmpty()) {
                g.renderItem(icon, iconX + 4, iconY + 4);
                if (taskDone) {
                    g.fill(iconX, iconY, iconX + TASK_ICON_SZ, iconY + TASK_ICON_SZ, 0x5500AA44);
                    g.drawCenteredString(font, "§a✔", iconX + TASK_ICON_SZ / 2, iconY + 6, 0xFFFFFFFF);
                }
            } else {
                g.drawCenteredString(font, taskDone ? "§a✔" : (task.isOptional() ? "§8○" : "§c✗"),
                        iconX + TASK_ICON_SZ / 2, iconY + 6, 0xFFFFFFFF);
            }
            iconX += TASK_ICON_SZ + 4;
        }

        String prog = done + "/" + tasks.size();
        g.drawString(font, "§8" + prog, width - font.width(prog) - MARGIN, HEADER_H + 16, C_TEXT_FAINT, false);
    }

    private void renderContent(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int ly = y;
        if (content != null && content.description() != null) {
            var lines = font.getSplitter().splitLines(content.description().getString(), w,
                    net.minecraft.network.chat.Style.EMPTY);
            for (var line : lines) {
                if (ly >= y + h) break;
                g.drawString(font, "§7" + line.getString(), x, ly, C_TEXT_DIM, false);
                ly += 10;
            }
        }
        List<QuestNode> prereqs = node.getPrerequisites();
        if (!prereqs.isEmpty()) {
            if (ly > y) ly += 8;
            if (ly < y + h) {
                g.drawString(font, "§8PREREQUISITES:", x, ly, C_TEXT_FAINT, false);
                ly += 12;
            }
            for (QuestNode req : prereqs) {
                if (ly >= y + h) break;
                QuestState state = playerData != null ? playerData.getQuestState(req.getId(), QuestState.LOCKED) :
                        QuestState.LOCKED;
                String reqTitle = req.getTitle().getString();
                if (font.width(reqTitle) > w - 12)
                    reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(0, w - 18)) + "…";
                g.drawString(font, (state == QuestState.COMPLETED ? "§a●" : "§8○") + " §7" + reqTitle,
                        x, ly, C_TEXT_DIM, false);
                ly += 10;
            }
        }
    }

    private static final String[] INSP_TABS = { "Info", "Prereqs", "Tasks", "Rewards" };
    private static final int INSP_TAB_H = 16;

    private void renderInspector(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, C_PANEL);
        drawBorder(g, x, y, w, h);

        int tabX = x + 6;
        int tabY = y + 4;
        for (int i = 0; i < INSP_TABS.length; i++) {
            String label = INSP_TABS[i];
            int tabW = font.width(label) + 6;
            boolean active = inspectorTab == i;
            boolean hov = mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + INSP_TAB_H;
            if (active) {
                g.fill(tabX, tabY, tabX + tabW, tabY + INSP_TAB_H, 0xFF1A1A26);
                g.fill(tabX, tabY + INSP_TAB_H - 1, tabX + tabW, tabY + INSP_TAB_H, C_DONE);
            } else if (hov) {
                g.fill(tabX, tabY, tabX + tabW, tabY + INSP_TAB_H, 0x22FFFFFF);
            }
            g.drawString(font, (active ? "§a" : "§8") + label, tabX + 3, tabY + 3, C_TEXT_DIM, false);
            tabX += tabW + 2;
        }

        int cY = y + INSP_TAB_H + 6;
        int cH = h - INSP_TAB_H - 12;
        g.enableScissor(x, cY, x + w, cY + cH);
        switch (inspectorTab) {
            case 0 -> renderInfoTab(g, x, cY, w, cH);
            case 1 -> renderPrereqsTab(g, x, cY, w, cH);
            case 2 -> renderTasksTab(g, x, cY, w, cH, mx, my);
            case 3 -> renderRewardsTab(g, x, cY, w, cH, mx, my);
        }
        g.disableScissor();
    }

    private void renderInfoTab(GuiGraphics g, int x, int y, int w, int h) {
        int m = 6;
        int cy = y - inspectorScrollY + 4;
        g.drawString(font, "§8Category:", x + m, cy, C_TEXT_FAINT, false);
        g.drawString(font, "§7" + (node.getCategory() != null ? node.getCategory() : "(none)"), x + m, cy + 10, C_TEXT,
                false);
        cy += 24;
        g.drawString(font, "§8Visibility:", x + m, cy, C_TEXT_FAINT, false);
        g.drawString(font, "§7" + (node.getVisibility() != null ? node.getVisibility() : "NORMAL"), x + m, cy + 10,
                C_TEXT, false);
        cy += 24;
        g.drawString(font, "§8ID:", x + m, cy, C_TEXT_FAINT, false);
        g.drawString(font, "§7" + (node.getId() != null ? node.getId() : "unknown"), x + m, cy + 10, C_TEXT, false);
        cy += 24;

        for (QuestTask task : node.getTasks()) {
            if (!(task instanceof InfoTask info)) continue;
            String body = info.getBody();
            if (body == null || body.isBlank()) continue;
            if (cy > y + h) break;
            g.fill(x + m, cy, x + w - m, cy + 1, C_BORDER);
            cy += 6;
            for (var line : font.split(Component.literal(body), w - m * 2)) {
                if (cy > y + h) break;
                g.drawString(font, line, x + m, cy, C_TEXT_DIM, false);
                cy += 10;
            }
            cy += 4;
        }
    }

    private void renderPrereqsTab(GuiGraphics g, int x, int y, int w, int h) {
        List<QuestNode> prereqs = node.getPrerequisites();
        int m = 6;
        int cy = y - inspectorScrollY + 4;
        if (prereqs.isEmpty()) {
            g.drawString(font, "§8(none)", x + m, cy, C_TEXT_FAINT, false);
            return;
        }
        for (QuestNode req : prereqs) {
            if (cy > y + h) break;
            QuestState state = playerData != null ? playerData.getQuestState(req.getId(), QuestState.LOCKED) :
                    QuestState.LOCKED;
            String reqTitle = req.getTitle().getString();
            int titleMaxW = w - m * 2 - 12;
            if (font.width(reqTitle) > titleMaxW)
                reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(0, titleMaxW - 6)) + "…";
            g.drawString(font, (state == QuestState.COMPLETED ? "§a●" : "§8○") + " §7" + reqTitle,
                    x + m, cy, C_TEXT_DIM, false);
            cy += 10;
        }
    }

    private void renderTasksTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        List<QuestTask> tasks = node.getTasks();
        int m = 6;
        int cy = y - inspectorScrollY + 4;
        if (tasks.isEmpty()) {
            g.drawString(font, "§8(none)", x + m, cy, C_TEXT_FAINT, false);
            return;
        }
        for (QuestTask task : tasks) {
            if (cy > y + h) break;
            if (task instanceof InfoTask) continue; 
            cy = renderRichTaskRow(g, x + m, cy, w - m * 2, task);
        }
    }

    private int renderRichTaskRow(GuiGraphics g, int x, int y, int w, QuestTask task) {
        boolean done = isTaskDone(task);
        String progress = player != null ? task.getProgressString(player) : null;

        g.drawString(font, done ? "§a✔" : (task.isOptional() ? "§8○" : "§c✗"), x, y + 1, 0xFFFFFFFF, false);

        int cx = x + 10;
        ItemStack icon = getTaskIcon(task);
        if (!icon.isEmpty()) {
            g.renderItem(icon, cx, y);
            if (done) g.fill(cx, y, cx + 16, y + 16, 0x5500AA44);
            cx += 18;
        } else {
            g.drawString(font, getTaskGlyph(task), cx, y + 1, 0xFFFFFFFF, false);
            cx += 10;
        }

        String desc = task.getDescription().getString();
        int progW = (progress != null && !done) ? font.width(progress) + 4 : 0;
        int descAvailW = w - (cx - x) - progW;
        if (font.width(desc) > descAvailW) desc = font.plainSubstrByWidth(desc, Math.max(0, descAvailW - 6)) + "…";
        g.drawString(font, "§f" + desc, cx, y + 1, done ? C_DONE : C_TEXT, false);

        String detail = getTaskDetail(task);
        if (detail != null) {
            int detailAvailW = w - (cx - x);
            if (font.width(detail) > detailAvailW)
                detail = font.plainSubstrByWidth(detail, Math.max(0, detailAvailW - 6)) + "…";
            g.drawString(font, "§8" + detail, cx, y + 11, C_TEXT_FAINT, false);
        }

        float pct = done ? 1f : parseProgress(progress);
        int barY = y + (detail != null ? 22 : 14);
        int barW = w - 12;
        g.fill(x + 10, barY, x + 10 + barW, barY + 3, 0xFF1A1A22);
        if (pct > 0) g.fill(x + 10, barY, x + 10 + (int) (barW * pct), barY + 3, done ? C_DONE : C_ACTIVE);
        if (progress != null) {
            g.drawString(font, "§8" + progress, x + w - font.width(progress), barY - 1, C_TEXT_FAINT, false);
        }

        return barY + 3 + 5; 
    }

    private void renderRewardsTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        List<QuestReward> rewards = node.getRewards();
        int m = 6;
        int cy = y - inspectorScrollY + 4;
        if (rewards.isEmpty()) {
            g.drawString(font, "§8(none)", x + m, cy, C_TEXT_FAINT, false);
            return;
        }

        int slotSz = 18;
        for (QuestReward reward : rewards) {
            if (cy > y + h) break;
            renderRewardSlot(g, x + m, cy, slotSz, reward, mx, my);

            String label;
            if (reward instanceof QuestReward.ItemReward ir) {
                label = ir.getItem().getDefaultInstance().getHoverName().getString() + " ×" + ir.getCount();
            } else {
                label = switch (reward.getType()) {
                    case XP -> "XP Reward";
                    case COMMAND -> "Command";
                    case LOOT_TABLE -> "Loot Table";
                    case SCRIPT_EVENT -> "Script Event";
                    default -> reward.getType().name();
                };
            }
            int labelMaxW = w - m * 2 - slotSz - 8;
            if (font.width(label) > labelMaxW) label = font.plainSubstrByWidth(label, Math.max(0, labelMaxW - 6)) + "…";
            g.drawString(font, "§7" + label, x + m + slotSz + 4, cy + 4, C_TEXT_DIM, false);
            cy += slotSz + 4;
        }
    }

    private void renderRewardSlot(GuiGraphics g, int x, int y, int sz, QuestReward reward, int mx, int my) {
        boolean hov = mx >= x && mx < x + sz && my >= y && my < y + sz;
        g.fill(x, y, x + sz, y + sz, hov ? C_SLOT_HI : C_SLOT_BG);
        g.fill(x, y, x + sz, y + 1, C_BORDER);
        g.fill(x, y + sz - 1, x + sz, y + sz, C_BORDER);
        g.fill(x, y, x + 1, y + sz, C_BORDER);
        g.fill(x + sz - 1, y, x + sz, y + sz, C_BORDER);

        if (reward instanceof QuestReward.ItemReward ir) {
            int off = (sz - 16) / 2;
            g.renderItem(new ItemStack(ir.getItem(), ir.getCount()), x + off, y + off);
            if (sz >= 18) g.renderItemDecorations(font, new ItemStack(ir.getItem(), ir.getCount()), x + off, y + off);
        } else {
            String glyph = switch (reward.getType()) {
                case XP -> "⚡";
                case COMMAND -> "◆";
                case LOOT_TABLE -> "📦";
                case SCRIPT_EVENT -> "✦";
                default -> "?";
            };
            g.drawCenteredString(font, "§7" + glyph, x + sz / 2, y + sz / 2 - 4, C_TEXT_DIM);
        }
    }

    private void renderFooter(GuiGraphics g, int mx, int my) {
        int footerY = height - FOOTER_H;
        g.fill(0, footerY, width, height, C_HEADER);
        g.fill(0, footerY, width, footerY + 1, C_BORDER);

        QuestState state = playerData != null ? playerData.getQuestState(node.getId(), QuestState.LOCKED) :
                QuestState.LOCKED;
        boolean canClaim = state == QuestState.COMPLETED && !rewardsClaimed() && !node.getRewards().isEmpty();

        if (canClaim) {
            int btnW = 120, btnX = (width - 120) / 2, btnY = footerY + 2;
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + 18;
            g.fill(btnX, btnY, btnX + btnW, btnY + 18, hov ? 0xFF2A4A2A : 0xFF1A2A1A);
            g.fill(btnX, btnY, btnX + btnW, btnY + 1, hov ? C_DONE : 0xFF333333);
            g.drawCenteredString(font, "§a✓ Claim Rewards", btnX + btnW / 2, btnY + 6, hov ? C_DONE : C_TEXT);
        } else if (rewardsClaimed()) {
            g.drawCenteredString(font, "§8Rewards already claimed", width / 2, footerY + 10, C_TEXT_FAINT);
        } else {
            g.drawCenteredString(font, "§8Complete all tasks to claim rewards", width / 2, footerY + 10, C_TEXT_FAINT);
        }
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 1, y + h, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER);
    }

    private ItemStack getTaskIcon(QuestTask task) {
        ResourceLocation id = task.getDisplayItemId();
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return (item != null && item != net.minecraft.world.item.Items.AIR) ? new ItemStack(item) : ItemStack.EMPTY;
    }

    private String getTaskGlyph(QuestTask task) {
        if (task instanceof ItemRequirementTask) return "§6■";
        if (task instanceof CraftItemTask) return "§e⚒";
        if (task instanceof KillEntityTask) return "§c⚔";
        if (task instanceof FluidRequirementTask) return "§3≋";
        if (task instanceof ExperienceTask) return "§a✦";
        if (task instanceof StatTrackerTask) return "§9≡";
        if (task instanceof AdvancementTask) return "§d★";
        if (task instanceof CheckmarkTask) return "§7☑";
        if (task instanceof InfoTask) return "§7✎";
        if (task instanceof TagItemTask) return "§e◈";
        if (task instanceof EnergyStorageTask) return "§6⚡";
        return "§8◇";
    }

    private String getTaskDetail(QuestTask task) {
        if (task instanceof ItemRequirementTask t) {
            String name = t.getItem() != null ? t.getItem().getDefaultInstance().getHoverName().getString() : "item";
            return name + (t.getRequiredCount() > 1 ? "  ×" + t.getRequiredCount() : "") +
                    (t.shouldConsume() ? "  (consumed)" : "");
        }
        if (task instanceof CraftItemTask t) {
            Item item = t.getItemId() != null ? ForgeRegistries.ITEMS.getValue(t.getItemId()) : null;
            String name = item != null ? item.getDefaultInstance().getHoverName().getString() :
                    (t.getItemId() != null ? t.getItemId().getPath() : "item");
            return "Craft: " + name + (t.getRequiredCount() > 1 ? " ×" + t.getRequiredCount() : "");
        }
        if (task instanceof KillEntityTask t) {
            String entity = t.getEntityId() != null ? prettifyId(t.getEntityId()) : "entity";
            return "Kill: " + entity + " ×" + t.getRequiredCount();
        }
        if (task instanceof FluidRequirementTask t) {
            String fluid = t.getFluidId() != null ? prettifyId(t.getFluidId()) : "fluid";
            return fluid + " — " + t.getRequiredAmount() + " mB";
        }
        if (task instanceof ExperienceTask t) {
            return "Reach Level " + t.getRequiredLevel();
        }
        if (task instanceof StatTrackerTask t) {
            String stat = t.getStatId() != null ? prettifyId(t.getStatId()) : "stat";
            return stat + " → " + t.getTargetValue();
        }
        if (task instanceof AdvancementTask t) {
            String adv = t.getAdvancementId() != null ?
                    t.getAdvancementId().getPath().replace('/', ' ').replace('_', ' ') : "advancement";
            return "Unlock: " + adv;
        }
        if (task instanceof InfoTask) {
            return null; 
        }
        if (task instanceof TagItemTask t) {
            String tag = t.getTag() != null ? "#" + t.getTag().location().getPath() : "#unknown";
            return tag + " ×" + t.getRequired();
        }
        return null;
    }

    private static String prettifyId(ResourceLocation id) {
        return id.getPath().replace('_', ' ');
    }

    private java.util.List<Component> buildTaskTooltip(QuestTask task) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        boolean done = isTaskDone(task);
        String status = done ? "§a✔ Complete" : (task.isOptional() ? "§8Optional" : "§c✗ Incomplete");
        lines.add(Component.literal(status + "  §7" + task.getDescription().getString()));
        String detail = getTaskDetail(task);
        if (detail != null) lines.add(Component.literal("§8" + detail));
        String prog = player != null ? task.getProgressString(player) : null;
        if (prog != null && !done) lines.add(Component.literal("§7Progress: §f" + prog));
        ItemStack icon = getTaskIcon(task);
        if (!icon.isEmpty()) lines.add(Component.literal("§8[Click to view in recipe browser]"));
        return lines;
    }

    private java.util.List<Component> buildRewardTooltip(QuestReward reward) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        if (reward instanceof QuestReward.ItemReward ir) {
            lines.add(Component.literal("§fReward: " + ir.getItem().getDefaultInstance().getHoverName().getString() +
                    " §8×" + ir.getCount()));
            lines.add(Component.literal("§8[Click to view in recipe browser]"));
        } else {
            lines.add(Component.literal("§f" + reward.getType().name() + " Reward"));
        }
        return lines;
    }

    private void tryOpenInRecipeViewer(ItemStack stack) {
        if (stack.isEmpty() || minecraft == null) return;
        
        try {
            Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> esClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Object es = esClass.getMethod("of", ItemStack.class).invoke(null, stack);
            api.getMethod("displayRecipes", esClass).invoke(null, es);
            return;
        } catch (Exception ignored) {}
        
        try {
            Class<?> jeiApi = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            
        } catch (Exception ignored) {}
        
        isFullscreen = true;
    }

    private float parseProgress(String prog) {
        if (prog == null) return 0f;
        int slash = prog.indexOf('/');
        if (slash < 0) return 0f;
        try {
            float cur = Float.parseFloat(prog.substring(0, slash).trim());
            float tot = Float.parseFloat(prog.substring(slash + 1).trim());
            return tot > 0 ? Math.min(1f, cur / tot) : 0f;
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        return isFullscreen ? handleFullscreenClick(mx, my, btn) : handleCompactClick(mx, my, btn);
    }

    private boolean handleCompactClick(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        List<QuestTask> tasks = node.getTasks();
        List<QuestReward> rewards = node.getRewards();
        String descText = (content != null && content.description() != null) ? content.description().getString() : null;
        java.util.List<net.minecraft.util.FormattedCharSequence> descLines2 = descText != null ?
                font.split(Component.literal(descText), CARD_W - CARD_PAD * 2) : java.util.List.of();
        int cardH = compactCardH(tasks, rewards, descLines2);
        int cardX = (width - CARD_W) / 2;
        int cardY = Math.max(10, (height - cardH) / 2);

        if (mx < cardX || mx >= cardX + CARD_W || my < cardY || my >= cardY + cardH) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }

        if (mx >= cardX + CARD_W - 18 && mx < cardX + CARD_W - 4 && my >= cardY + 3 && my < cardY + 17) {
            isFullscreen = true;
            return true;
        }

        if (hoveredTask != null) {
            ItemStack icon = getTaskIcon(hoveredTask);
            if (!icon.isEmpty()) tryOpenInRecipeViewer(icon);
            else isFullscreen = true; 
            return true;
        }
        if (hoveredReward != null) {
            if (hoveredReward instanceof QuestReward.ItemReward ir) {
                tryOpenInRecipeViewer(new ItemStack(ir.getItem(), ir.getCount()));
            }
            return true;
        }

        int footerY = cardY + cardH - 18;
        if (my >= footerY && my < footerY + 18) {
            QuestState state = playerData != null ? playerData.getQuestState(node.getId(), QuestState.LOCKED) :
                    QuestState.LOCKED;
            if (state == QuestState.COMPLETED && !rewardsClaimed() && !rewards.isEmpty()) {
                PhoenixNetwork.CHANNEL.sendToServer(new C2SClaimQuestRewardPacket(node.getId(), -1));
            }
        }
        return true;
    }

    private boolean handleFullscreenClick(double mx, double my, int btn) {
        
        if (mx >= 4 && mx < 20 && my >= 6 && my < 22) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        
        if (mx >= width - 36 && mx < width - 20 && my >= 6 && my < 22 && btn == 0) {
            isFullscreen = false;
            return true;
        }
        
        if (mx >= width - 20 && mx < width - 4 && my >= 6 && my < 22 && btn == 0) {
            if (playerData != null) {
                if (playerData.isPinned(node.getId())) playerData.clearPin();
                else playerData.setPinnedQuestId(node.getId());
            }
            return true;
        }
        
        int contentTop = HEADER_H + REQBAR_H + MARGIN;
        int contentRight = width - REWARD_W - MARGIN - MARGIN;
        int rightX = contentRight + MARGIN;
        if (mx >= rightX && mx < rightX + REWARD_W && my >= contentTop && my < contentTop + INSP_TAB_H + 8) {
            int tabX = rightX + 6;
            for (int i = 0; i < INSP_TABS.length; i++) {
                int tabW = font.width(INSP_TABS[i]) + 6;
                if (mx >= tabX && mx < tabX + tabW && my >= contentTop + 4 && my < contentTop + 4 + INSP_TAB_H) {
                    inspectorTab = i;
                    inspectorScrollY = 0;
                    return true;
                }
                tabX += tabW + 2;
            }
        }
        
        int footerY = height - FOOTER_H;
        if (my >= footerY + 2 && my < footerY + 20) {
            QuestState state = playerData != null ? playerData.getQuestState(node.getId(), QuestState.LOCKED) :
                    QuestState.LOCKED;
            if (state == QuestState.COMPLETED && !rewardsClaimed() && !node.getRewards().isEmpty()) {
                PhoenixNetwork.CHANNEL.sendToServer(new C2SClaimQuestRewardPacket(node.getId(), -1));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isFullscreen) return false;
        int contentTop = HEADER_H + REQBAR_H + MARGIN;
        int contentRight = width - REWARD_W - MARGIN - MARGIN;
        int rightX = contentRight + MARGIN;
        if (mx >= rightX && mx < rightX + REWARD_W && my >= contentTop && my < height - FOOTER_H - MARGIN) {
            inspectorScrollY = Math.max(0, (int) (inspectorScrollY - delta * 12));
        } else {
            descScrollY = Math.max(0, (int) (descScrollY - delta * 12));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { 
            if (isFullscreen) {
                isFullscreen = false;
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

    private boolean isTaskDone(QuestTask task) {
        return player != null && task.isCompletedFor(player);
    }

    private boolean rewardsClaimed() {
        return playerData != null && playerData.hasClaimedRewards(node.getId());
    }
}
