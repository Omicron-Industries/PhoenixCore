package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.C2SClaimQuestRewardPacket;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Full quest detail screen with:
 * - Real reward slots (item icons, XP, command labels)
 * - Reward choice UI (choose 1 of N)
 * - Pin/unpin button for the HUD tracker
 * - Repeat / cooldown status display
 * - AND/OR prerequisite display
 *
 * ┌──────────────────────────────────────────────────────┐
 * │ ← Back [Title] [📌 Pin] [↻ DAILY / cooldown] │ header
 * ├──────────────────────┬───────────────────────────────┤
 * │ Description │ OBJECTIVES (done/total) │
 * │ ... │ ✔ task one │
 * │ PREREQUISITES │ ✗ task two │
 * │ (AND/OR badge) ├───────────────────────────────┤
 * │ ● req one │ REWARDS │
 * │ ○ req two │ [item][item][xp] ← slots │
 * │ │ or [choice A] [choice B] │
 * ├──────────────────────┴───────────────────────────────┤
 * │ 1/3 objectives [Claim Rewards / Already done]│ footer
 * └──────────────────────────────────────────────────────┘
 */
public class QuestTasksScreen extends Screen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_PANEL = 0xFF16161C;
    private static final int C_HEADER = 0xFF0C0C10;
    private static final int C_BORDER = 0xFF2A2A36;
    private static final int C_BORDER_LIT = 0xFF3A3A4C;
    private static final int C_DONE = 0xFF00AA55;
    private static final int C_ACTIVE = 0xFFBB8800;
    private static final int C_LOCKED = 0xFF444455;
    private static final int C_TEXT = 0xFFDDDDE8;
    private static final int C_TEXT_DIM = 0xFF888898;
    private static final int C_TEXT_FAINT = 0xFF4A4A5A;
    private static final int C_TASK_DONE = 0xFF0D1A0F;
    private static final int C_TASK_OPEN = 0xFF171720;
    private static final int C_SLOT_BG = 0xFF1A1A22;
    private static final int C_SLOT_BORDER = 0xFF333344;
    private static final int C_SLOT_SEL = 0xFF3A2A00;
    private static final int C_SLOT_SEL_B = 0xFFCC9900;
    private static final int C_PIN_ACTIVE = 0xFFAA44FF;
    private static final int C_REPEAT = 0xFF448888;
    private static final int C_DAILY = 0xFF448844;
    private static final int C_PROG_BG = 0xFF141420;
    private static final int C_PROG_FILL = 0xFF00AA55;
    private static final int C_PROG_ACT = 0xFFBB8800;

    private static final int HEADER_H = 26;
    private static final int FOOTER_H = 28;
    private static final int MARGIN = 12;
    private static final int SLOT_SIZE = 22;
    private static final int SCROLL_SPD = 8;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final QuestNode node;
    private final FullQuestData content;
    private final PlayerQuestData playerData;

    // Layout
    private int panelLeft, panelTop, panelW, panelH, splitX;

    // Scroll
    private int descScrollY = 0, tasksScrollY = 0;

    // Reward choice selection (-1 = none chosen yet)
    private int hoveredRewardSlot = -1;
    private int selectedRewardSlot = -1;

    public QuestTasksScreen(Screen parent, QuestNode node, FullQuestData content, PlayerQuestData playerData) {
        super(Component.literal("Chronicle"));
        this.parent = parent;
        this.node = node;
        this.content = content;
        this.playerData = playerData;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QuestState getState() {
        if (playerData == null) return QuestState.LOCKED;
        return playerData.getQuestState(node.getId(), QuestState.LOCKED);
    }

    private boolean isTaskDone(QuestTask task) {
        if (minecraft == null || minecraft.player == null) return false;
        return task.isCompletedFor(minecraft.player);
    }

    private boolean allTasksDone() {
        for (QuestTask t : node.getTasks()) if (!isTaskDone(t)) return false;
        return true;
    }

    private boolean rewardsClaimed() {
        return playerData != null && playerData.hasClaimedRewards(node.getId());
    }

    /** True when the quest uses a choice reward (player picks one). */
    private boolean isChoiceReward() {
        return node.getRewards().size() > 1;
    }

    private boolean isPinned() {
        return playerData != null && playerData.isPinned(node.getId());
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();

        panelW = Math.min(width - 30, 400);
        panelH = Math.min(height - 30, 270);
        panelLeft = (width - panelW) / 2;
        panelTop = (height - panelH) / 2;
        splitX = panelLeft + panelW * 2 / 5;

        // Back
        addRenderableWidget(Button.builder(Component.literal("§7‹ Back"),
                b -> {
                    if (minecraft != null) minecraft.setScreen(parent);
                }).bounds(panelLeft + MARGIN, panelTop + (HEADER_H - 14) / 2, 48, 14).build());

        // Pin / Unpin
        boolean pinned = isPinned();
        addRenderableWidget(Button.builder(
                Component.literal(pinned ? "§5📌 Unpin" : "§8📌 Pin"),
                b -> {
                    if (playerData == null) return;
                    if (playerData.isPinned(node.getId())) playerData.clearPin();
                    else playerData.setPinnedQuestId(node.getId());
                    // Refresh button label
                    clearWidgets();
                    init();
                }).bounds(panelLeft + panelW - 72 - MARGIN, panelTop + (HEADER_H - 14) / 2, 72, 14).build());

        // Claim / choice button
        QuestState state = getState();
        boolean canClaim = allTasksDone() && state != QuestState.COMPLETED && !rewardsClaimed();
        boolean hasTasks = !node.getTasks().isEmpty();
        boolean claimed = rewardsClaimed();

        String claimLabel;
        if (claimed || state == QuestState.COMPLETED) {
            claimLabel = "§8 Already Claimed ";
        } else if (canClaim) {
            if (isChoiceReward() && selectedRewardSlot < 0) {
                claimLabel = "§7 Choose a reward first ";
            } else {
                claimLabel = "§a Claim Rewards ";
            }
        } else {
            claimLabel = "§8 Objectives Incomplete ";
        }

        addRenderableWidget(Button.builder(Component.literal(claimLabel), b -> {
            if (!canClaim || claimed) return;
            if (minecraft == null || minecraft.player == null) return;

            // Send to server — reward granting and state change are server-authoritative.
            // We close the screen immediately (optimistic); the server validates tasks
            // before actually granting anything, so no exploit risk.
            int choice = isChoiceReward() ? selectedRewardSlot : -1;
            if (isChoiceReward() && choice < 0) return; // haven't picked yet

            net.phoenix.core.network.PhoenixNetwork.CHANNEL.sendToServer(
                    new C2SClaimQuestRewardPacket(node.getId(), choice));

            minecraft.setScreen(parent);
        }).bounds(panelLeft + panelW - 160 - MARGIN, panelTop + panelH - FOOTER_H + (FOOTER_H - 16) / 2, 160, 16)
                .build());

        // Restore chosen reward index from capability
        if (playerData != null) {
            int saved = playerData.getChosenRewardIndex(node.getId());
            if (saved >= 0 && saved < node.getRewards().size()) selectedRewardSlot = saved;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xA0000000);

        // Panel
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, C_PANEL);
        drawBorder(g, panelLeft, panelTop, panelW, panelH, stateColor());

        // Header
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + HEADER_H, C_HEADER);
        g.fill(panelLeft, panelTop + HEADER_H - 1, panelLeft + panelW, panelTop + HEADER_H, C_BORDER);

        QuestState state = getState();
        String badge = switch (state) {
            case COMPLETED -> "§a■";
            case ACTIVE -> "§6■";
            case LOCKED -> "§8■";
            default -> "§7■";
        };
        String titleStr = content != null ? content.title().getString() : node.getTitle().getString();
        int titleMaxW = panelW - 170;
        if (font.width(titleStr) > titleMaxW)
            titleStr = font.plainSubstrByWidth(titleStr, titleMaxW - 4) + "…";
        g.drawCenteredString(font, badge + " §f" + titleStr, panelLeft + panelW / 2, panelTop + 9, C_TEXT);

        // Repeat badge (right of title area)
        if (node.isRepeatable()) {
            String rLabel = switch (node.getRepeatMode()) {
                case DAILY -> "§2↻ DAILY";
                case COOLDOWN -> "§3↻ " + node.getRepeatCooldownHours() + "h";
                case INFINITE -> "§3↻ ∞";
                default -> "";
            };
            if (!rLabel.isEmpty()) {
                // Check if cooldown is active
                if (playerData != null && !QuestProgressTracker.canRepeatNow(node, playerData) &&
                        state == QuestState.COMPLETED) {
                    long remaining = remainingCooldownMs(node, playerData);
                    rLabel = "§8↻ " + formatRemaining(remaining);
                }
                g.drawString(font, rLabel, panelLeft + MARGIN + 56, panelTop + 9, 0xFFFFFFFF, false);
            }
        }

        // Divider + footer
        g.fill(splitX, panelTop + HEADER_H, splitX + 1, panelTop + panelH - FOOTER_H, C_BORDER);
        int footerY = panelTop + panelH - FOOTER_H;
        g.fill(panelLeft, footerY, panelLeft + panelW, footerY + 1, C_BORDER);
        g.fill(panelLeft, footerY + 1, panelLeft + panelW, panelTop + panelH, C_HEADER);

        // Footer progress text
        int done = 0;
        for (QuestTask t : node.getTasks()) if (isTaskDone(t)) done++;
        int total = node.getTasks().size();
        String prog = total > 0 ? done + " / " + total + " objectives" : "No objectives";
        g.drawString(font, "§8" + prog, panelLeft + MARGIN, footerY + (FOOTER_H - 8) / 2, C_TEXT_DIM, false);

        // Progress bar in footer
        if (total > 0) {
            int barX = panelLeft + MARGIN + font.width(prog) + 10;
            int barW = splitX - barX - MARGIN;
            if (barW > 20) {
                int barY = footerY + FOOTER_H / 2 - 2;
                int fill = (int) ((float) done / total * barW);
                int barCol = state == QuestState.COMPLETED ? C_PROG_FILL : C_PROG_ACT;
                g.fill(barX, barY, barX + barW, barY + 4, C_PROG_BG);
                if (fill > 0) g.fill(barX, barY, barX + fill, barY + 4, barCol);
            }
        }

        renderLeft(g, mx, my, state);
        renderRight(g, mx, my);

        super.render(g, mx, my, partial);
    }

    // ── Left column: description + prerequisites ──────────────────────────────

    private void renderLeft(GuiGraphics g, int mx, int my, QuestState state) {
        int colX = panelLeft + MARGIN;
        int colW = splitX - panelLeft - MARGIN * 2;
        int y = panelTop + HEADER_H + 6;

        g.enableScissor(panelLeft, panelTop + HEADER_H, splitX, panelTop + panelH - FOOTER_H);

        String desc = (content != null && !content.description().getString().isEmpty()) ?
                content.description().getString() : node.getDescription().getString();

        g.drawString(font, "§8DESCRIPTION", colX, y, C_TEXT_FAINT, false);
        y += 11;
        int py = y - descScrollY;
        for (String line : wrapText(desc, colW)) {
            if (py > panelTop + HEADER_H && py < panelTop + panelH - FOOTER_H)
                g.drawString(font, "§7" + line, colX, py, C_TEXT_DIM, false);
            py += 9;
        }
        y = Math.max(y + 10, py + descScrollY);

        // Prerequisites
        if (!node.getPrerequisites().isEmpty()) {
            y = Math.max(y, panelTop + HEADER_H + 60);
            g.fill(colX, y, colX + colW, y + 1, C_BORDER);
            y += 5;

            // AND/OR badge
            boolean andMode = node.getRequireAllPrerequisites();
            String gateLabel = andMode ? "§7ALL required" : "§7ANY one required";
            g.drawString(font, "§8REQUIRES  " + gateLabel, colX, y, C_TEXT_FAINT, false);
            y += 11;

            for (QuestNode req : node.getPrerequisites()) {
                QuestState rs = playerData != null ? playerData.getQuestState(req.getId(), QuestState.LOCKED) :
                        QuestState.LOCKED;
                String dot = rs == QuestState.COMPLETED ? "§a●" : rs == QuestState.ACTIVE ? "§6◐" : "§8○";
                String rname = req.getTitle().getString();
                if (font.width(rname) > colW - 16) rname = font.plainSubstrByWidth(rname, colW - 20) + "…";
                g.drawString(font, dot + " §8" + rname, colX + 4, y, C_TEXT_DIM, false);
                y += 9;
            }
        }

        g.disableScissor();
    }

    // ── Right column: objectives + rewards ────────────────────────────────────

    private void renderRight(GuiGraphics g, int mx, int my) {
        int colX = splitX + MARGIN;
        int colW = panelLeft + panelW - splitX - MARGIN * 2;
        int oy = panelTop + HEADER_H + 6;

        g.enableScissor(splitX + 1, panelTop + HEADER_H, panelLeft + panelW, panelTop + panelH - FOOTER_H);

        int py = oy - tasksScrollY;

        // Objectives section
        int done = 0;
        for (QuestTask t : node.getTasks()) if (isTaskDone(t)) done++;
        g.drawString(font, "§8OBJECTIVES  §7" + done + "/" + node.getTasks().size(), colX, py, C_TEXT_FAINT, false);
        py += 11;

        if (node.getTasks().isEmpty()) {
            g.drawString(font, "§8No objectives.", colX + 4, py, C_TEXT_FAINT, false);
            py += 10;
        } else {
            for (QuestTask task : node.getTasks()) {
                boolean taskDone = isTaskDone(task);
                g.fill(colX - 2, py - 1, colX + colW + 2, py + 13, taskDone ? C_TASK_DONE : C_TASK_OPEN);
                g.fill(colX - 2, py + 13, colX + colW + 2, py + 14, C_BORDER);
                String check = taskDone ? "§a✔" : "§8✗";
                String label = task.getDescription().getString();
                if (font.width(label) > colW - 16) label = font.plainSubstrByWidth(label, colW - 20) + "…";
                g.drawString(font, check + " " + (taskDone ? "§8" : "§7") + label, colX + 2, py + 2, 0xFFFFFFFF, false);
                py += 15;
            }
        }

        py += 6;
        g.fill(colX, py, colX + colW, py + 1, C_BORDER);
        py += 5;

        // Rewards section
        List<QuestReward> rewards = node.getRewards();
        g.drawString(font, "§8REWARDS" + (isChoiceReward() ? "  §8— pick one" : ""), colX, py, C_TEXT_FAINT, false);
        py += 11;

        hoveredRewardSlot = -1;
        if (rewards.isEmpty()) {
            g.drawString(font, "§8No rewards.", colX + 4, py, C_TEXT_FAINT, false);
        } else {
            int sx = colX;
            int slotY = py;
            for (int i = 0; i < rewards.size(); i++) {
                if (sx + SLOT_SIZE > colX + colW) {
                    sx = colX;
                    slotY += SLOT_SIZE + 4;
                }
                QuestReward reward = rewards.get(i);
                boolean hov = mx >= sx && mx <= sx + SLOT_SIZE && my >= slotY && my <= slotY + SLOT_SIZE;
                boolean sel = selectedRewardSlot == i;
                if (hov && !rewardsClaimed()) hoveredRewardSlot = i;

                int slotBg = sel ? C_SLOT_SEL : C_SLOT_BG;
                int slotBorder = sel ? C_SLOT_SEL_B : (hov ? 0xFF555568 : C_SLOT_BORDER);
                g.fill(sx, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, slotBg);
                g.fill(sx, slotY, sx + SLOT_SIZE, slotY + 1, slotBorder);
                g.fill(sx, slotY + SLOT_SIZE - 1, sx + SLOT_SIZE, slotY + SLOT_SIZE, slotBorder);
                g.fill(sx, slotY, sx + 1, slotY + SLOT_SIZE, slotBorder);
                g.fill(sx + SLOT_SIZE - 1, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, slotBorder);

                // Render reward content
                renderRewardSlot(g, reward, sx + 3, slotY + 3, SLOT_SIZE - 6);
                sx += SLOT_SIZE + 3;
            }
            py = slotY + SLOT_SIZE + 4;

            // Reward labels on hover
            if (hoveredRewardSlot >= 0) {
                QuestReward hovered = rewards.get(hoveredRewardSlot);
                String summary = hovered.getSummary().getString();
                g.drawString(font, "§7" + summary, colX, py + 2, C_TEXT_DIM, false);
            }

            // Choice instruction
            if (isChoiceReward() && !rewardsClaimed()) {
                py += 12;
                g.drawString(font,
                        selectedRewardSlot >= 0 ?
                                "§aSelected: " + rewards.get(selectedRewardSlot).getSummary().getString() :
                                "§8Click a reward to select it",
                        colX, py, C_TEXT_FAINT, false);
            }
        }

        g.disableScissor();
    }

    private void renderRewardSlot(GuiGraphics g, QuestReward reward, int x, int y, int size) {
        switch (reward.getType()) {
            case ITEM -> {
                if (reward instanceof QuestReward.ItemReward ir) {
                    g.renderItem(new ItemStack(ir.getItem()), x, y);
                    if (ir.getCount() > 1)
                        g.drawString(font, String.valueOf(ir.getCount()),
                                x + size - font.width(String.valueOf(ir.getCount())), y + size - 8, 0xFFFFFFFF, true);
                }
            }
            case XP -> {
                g.drawCenteredString(font, "§a✦", x + size / 2, y + size / 2 - 4, 0xFF44CC88);
                if (reward instanceof QuestReward.XPReward xr)
                    g.drawString(font, "§a" + xr.getLevels(), x, y + size - 8, 0xFF44CC88, false);
            }
            case COMMAND -> g.drawCenteredString(font, "§b⌘", x + size / 2, y + size / 2 - 4, 0xFF44AACC);
        }
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Reward slot selection (choice mode only)
        if (btn == 0 && isChoiceReward() && hoveredRewardSlot >= 0 && !rewardsClaimed()) {
            selectedRewardSlot = hoveredRewardSlot;
            if (playerData != null) playerData.setChosenRewardIndex(node.getId(), selectedRewardSlot);
            clearWidgets();
            init(); // rebuild claim button label
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < splitX) descScrollY = Math.max(0, descScrollY - (int) (delta * SCROLL_SPD));
        else tasksScrollY = Math.max(0, tasksScrollY - (int) (delta * SCROLL_SPD));
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int stateColor() {
        return switch (getState()) {
            case COMPLETED -> C_DONE;
            case ACTIVE -> C_ACTIVE;
            case LOCKED -> C_LOCKED;
            default -> C_BORDER_LIT;
        };
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private List<String> wrapText(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        for (String para : text.split("\n")) {
            if (para.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String test = cur.isEmpty() ? word : cur + " " + word;
                if (font.width(test) > maxW && !cur.isEmpty()) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(word);
                } else cur = new StringBuilder(test);
            }
            if (!cur.isEmpty()) lines.add(cur.toString());
        }
        return lines;
    }

    private long remainingCooldownMs(QuestNode n, PlayerQuestData d) {
        return switch (n.getRepeatMode()) {
            case COOLDOWN -> Math.max(0, TimeUnit.HOURS.toMillis(n.getRepeatCooldownHours()) -
                    (System.currentTimeMillis() - d.getLastCompletedTime(n.getId())));
            default -> 0L;
        };
    }

    private String formatRemaining(long ms) {
        long h = ms / 3_600_000, m = (ms % 3_600_000) / 60_000;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
