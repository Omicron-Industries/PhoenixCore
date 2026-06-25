package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Manages animated toast notifications for quest state changes.
 *
 * Two toast types:
 * UNLOCKED — small blue banner: "Quest Unlocked: <title>"
 * COMPLETED — gold banner: "Quest Complete! <title>"
 *
 * Toasts slide in from the right, stay visible for ~4 s, then fade out.
 * Up to 3 toasts visible simultaneously; excess entries queue behind them.
 *
 * Call from the client HUD overlay renderer (ChronicleClientEvents or QuestHudOverlay).
 */
public class QuestToastManager {

    public enum ToastType {
        UNLOCKED,
        COMPLETED
    }

    private static final int TOAST_W = 200;
    private static final int TOAST_H = 32;
    private static final int MARGIN_R = 2;
    private static final int GAP = 3;
    private static final int SLIDE_TICKS = 8;
    private static final int STAY_TICKS = 80; // 4 s at 20 tps
    private static final int FADE_TICKS = 12;
    private static final int MAX_VISIBLE = 3;

    private static final int C_BG_UNLOCK = 0xDD0A1230;
    private static final int C_BG_DONE = 0xDD1A1000;
    private static final int C_BAR_UNLOCK = 0xFF3366FF;
    private static final int C_BAR_DONE = 0xFFFFAA00;
    private static final int C_TITLE_UNLOCK = 0xFF99BBFF;
    private static final int C_TITLE_DONE = 0xFFFFDD66;
    private static final int C_LABEL = 0xFFCCCCCC;

    private static final QuestToastManager INSTANCE = new QuestToastManager();

    public static QuestToastManager get() {
        return INSTANCE;
    }

    private final Deque<ToastEntry> queue = new ArrayDeque<>();
    private final List<ActiveToast> active = new ArrayList<>();

    public void push(QuestNode node, ToastType type) {
        queue.addLast(new ToastEntry(node, type));
    }

    /** Call once per client tick to advance animations and promote queued toasts. */
    public void tick() {
        active.removeIf(t -> t.ticksAlive > SLIDE_TICKS + STAY_TICKS + FADE_TICKS);
        while (active.size() < MAX_VISIBLE && !queue.isEmpty()) {
            active.add(new ActiveToast(queue.pollFirst()));
        }
        for (ActiveToast t : active) t.ticksAlive++;
    }

    /** Render all active toasts — call from HUD overlay post-render. */
    public void render(GuiGraphics g, int screenW, int screenH) {
        Font font = Minecraft.getInstance().font;
        int slotY = screenH / 4; // start at ~1/4 from top so they don't clash with the pinned quest widget

        for (ActiveToast t : active) {
            float progress = computeX(t);
            int x = (int) (screenW - MARGIN_R - (TOAST_W * progress));
            int y = slotY;
            slotY += TOAST_H + GAP;

            float alpha = computeAlpha(t);
            int a = (int) (alpha * 0xFF) << 24;

            int bg = (t.entry.type == ToastType.COMPLETED) ? C_BG_DONE : C_BG_UNLOCK;
            int bar = (t.entry.type == ToastType.COMPLETED) ? C_BAR_DONE : C_BAR_UNLOCK;
            int titleCol = (t.entry.type == ToastType.COMPLETED) ? C_TITLE_DONE : C_TITLE_UNLOCK;

            // Background
            g.fill(x, y, x + TOAST_W, y + TOAST_H, (bg & 0x00FFFFFF) | a);
            // Left accent bar
            g.fill(x, y, x + 3, y + TOAST_H, (bar & 0x00FFFFFF) | a);

            // Icon
            QuestNode node = t.entry.node;
            int textX = x + 6;
            if (node.getIconItem() != null && node.getIconItem() != net.minecraft.world.item.Items.AIR) {
                g.renderItem(new ItemStack(node.getIconItem()), x + 6, y + TOAST_H / 2 - 8);
                textX = x + 24;
            }

            // Labels
            String label = (t.entry.type == ToastType.COMPLETED) ? "Quest Complete!" : "Quest Unlocked";
            g.drawString(font, "§7" + label, textX, y + 5, (C_LABEL & 0x00FFFFFF) | a, false);
            String titleStr = font.width(node.getTitle().getString()) > TOAST_W - textX - x - 8 ?
                    font.plainSubstrByWidth(node.getTitle().getString(), TOAST_W - textX - x - 8) + "…" :
                    node.getTitle().getString();
            g.drawString(font, titleStr, textX, y + 16, (titleCol & 0x00FFFFFF) | a, false);
        }
    }

    private float computeX(ActiveToast t) {
        if (t.ticksAlive < SLIDE_TICKS) {
            return t.ticksAlive / (float) SLIDE_TICKS;
        }
        return 1.0f;
    }

    private float computeAlpha(ActiveToast t) {
        int fadeStart = SLIDE_TICKS + STAY_TICKS;
        if (t.ticksAlive >= fadeStart) {
            int fadeAge = t.ticksAlive - fadeStart;
            return 1.0f - (fadeAge / (float) FADE_TICKS);
        }
        if (t.ticksAlive < SLIDE_TICKS) {
            return t.ticksAlive / (float) SLIDE_TICKS;
        }
        return 1.0f;
    }

    private record ToastEntry(QuestNode node, ToastType type) {}

    private static class ActiveToast {

        final ToastEntry entry;
        int ticksAlive = 0;

        ActiveToast(ToastEntry e) {
            this.entry = e;
        }
    }
}
