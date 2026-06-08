package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

import java.util.List;

/**
 * Renders the pinned quest as a compact tracker widget in the top-right corner of the HUD.
 *
 * Layout (fixed 160px wide, height grows with task count):
 *
 * ┌────────────────────────┐
 * │ ▶ Quest Title 📌 │ ← title row (state colour)
 * │ ───────────────────── │
 * │ ✔ Task one │ ← completed task (dim green)
 * │ ✗ Task two │ ← pending task
 * │ ✗ Task three │
 * │ ══════════ 1/3 │ ← progress bar
 * └────────────────────────┘
 *
 * The widget is only rendered when:
 * - A quest is pinned (pinnedQuestId != null in PlayerQuestData)
 * - The quest exists in the registry
 * - No screen is open (the HUD is hidden while GUIs are open)
 * - The player is alive
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class QuestHudOverlay {

    private static final int WIDGET_W = 164;
    private static final int MARGIN_R = 6;   // from right edge of screen
    private static final int MARGIN_T = 6;   // from top edge
    private static final int PAD = 5;
    private static final int ROW_H = 11;
    private static final int BAR_H = 4;

    // Colours
    private static final int C_BG = 0xCC0B0B0F;
    private static final int C_BORDER = 0xFF252530;
    private static final int C_TITLE_BG = 0xDD09090D;
    private static final int C_DONE_ROW = 0x220044FF; // slight tint for completed tasks
    private static final int C_PROG_BG = 0xFF141420;
    private static final int C_PROG_FILL = 0xFF00AA55;
    private static final int C_PROG_ACT = 0xFFBB8800;
    private static final int C_TEXT = 0xFFD8D8E4;
    private static final int C_TEXT_DIM = 0xFF888898;
    private static final int C_TEXT_DONE = 0xFF44CC88;
    private static final int C_TEXT_ACT = 0xFFFFBB33;
    private static final int C_PIN = 0xFFAA44FF;

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.player.isDeadOrDying()) return;

        PlayerQuestData data = mc.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).orElse(null);
        if (data == null) return;

        ResourceLocation pinnedId = data.getPinnedQuestId();
        if (pinnedId == null) return;

        QuestNode node = QuestTreeRegistry.getQuest(pinnedId);
        if (node == null) {
            data.clearPin();
            return;
        }

        QuestState state = data.getQuestState(pinnedId, QuestState.LOCKED);
        List<QuestTask> tasks = node.getTasks();

        // Count completions
        int done = 0;
        for (QuestTask t : tasks) if (t.isCompletedFor(mc.player)) done++;

        // Widget height: title row + divider + task rows + bar + bottom padding
        int taskRows = Math.min(tasks.size(), 6);
        int widgetH = PAD + ROW_H + 3 + taskRows * ROW_H + (tasks.isEmpty() ? 0 : BAR_H + 4) + PAD;
        if (tasks.size() > 6) widgetH += ROW_H; // overflow line

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int wx = screenW - WIDGET_W - MARGIN_R;
        int wy = MARGIN_T;

        // Background + border
        g.fill(wx, wy, wx + WIDGET_W, wy + widgetH, C_BG);
        g.fill(wx, wy, wx + WIDGET_W, wy + 1, C_BORDER);
        g.fill(wx, wy + widgetH - 1, wx + WIDGET_W, wy + widgetH, C_BORDER);
        g.fill(wx, wy, wx + 1, wy + widgetH, C_BORDER);
        g.fill(wx + WIDGET_W - 1, wy, wx + WIDGET_W, wy + widgetH, C_BORDER);

        // Title area background
        g.fill(wx + 1, wy + 1, wx + WIDGET_W - 1, wy + PAD + ROW_H + 1, C_TITLE_BG);

        // State icon + title
        String stateGlyph = switch (state) {
            case COMPLETED -> "§a✔";
            case ACTIVE -> "§6▶";
            case LOCKED -> "§8✕";
            default -> "§7○";
        };
        int titleColor = switch (state) {
            case COMPLETED -> C_TEXT_DONE;
            case ACTIVE -> C_TEXT_ACT;
            default -> C_TEXT;
        };

        // Item icon if set
        if (node.getIconItem() != null && node.getIconItem() != net.minecraft.world.item.Items.AIR) {
            g.renderItem(new ItemStack(node.getIconItem()), wx + PAD, wy + PAD - 2);
            String titleStr = truncate(font, node.getTitle().getString(), WIDGET_W - PAD * 2 - 20);
            g.drawString(font, stateGlyph + " " + titleStr, wx + PAD + 18, wy + PAD + 1, titleColor, false);
        } else {
            String titleStr = truncate(font, node.getTitle().getString(), WIDGET_W - PAD * 2 - 14);
            g.drawString(font, stateGlyph + " " + titleStr, wx + PAD, wy + PAD + 1, titleColor, false);
        }

        // Pin icon (top-right)
        g.drawString(font, "§5📌", wx + WIDGET_W - 14, wy + PAD, C_PIN, false);

        // Divider
        int divY = wy + PAD + ROW_H + 1;
        g.fill(wx + PAD, divY, wx + WIDGET_W - PAD, divY + 1, C_BORDER);

        // Task rows
        int ty = divY + 3;
        for (int i = 0; i < taskRows; i++) {
            QuestTask task = tasks.get(i);
            boolean isDone = task.isCompletedFor(mc.player);
            if (isDone) g.fill(wx + 1, ty, wx + WIDGET_W - 1, ty + ROW_H, C_DONE_ROW);
            String check = isDone ? "§a✔" : "§8✗";
            String label = truncate(font, task.getDescription().getString(), WIDGET_W - PAD * 2 - 14);
            g.drawString(font, check + " §7" + label, wx + PAD, ty + 1, isDone ? C_TEXT_DONE : C_TEXT_DIM, false);
            ty += ROW_H;
        }
        if (tasks.size() > 6) {
            g.drawString(font, "§8+" + (tasks.size() - 6) + " more…", wx + PAD, ty + 1, C_TEXT_DIM, false);
            ty += ROW_H;
        }

        // Progress bar
        if (!tasks.isEmpty()) {
            ty += 2;
            int barW = WIDGET_W - PAD * 2 - 28;
            int fill = (int) ((float) done / tasks.size() * barW);
            int barCol = state == QuestState.COMPLETED ? C_PROG_FILL : C_PROG_ACT;
            g.fill(wx + PAD, ty, wx + PAD + barW, ty + BAR_H, C_PROG_BG);
            if (fill > 0) g.fill(wx + PAD, ty, wx + PAD + fill, ty + BAR_H, barCol);
            g.drawString(font, "§8" + done + "/" + tasks.size(), wx + PAD + barW + 4, ty - 1, C_TEXT_DIM, false);
        }
    }

    private static String truncate(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, maxW - 6) + "…";
    }
}
