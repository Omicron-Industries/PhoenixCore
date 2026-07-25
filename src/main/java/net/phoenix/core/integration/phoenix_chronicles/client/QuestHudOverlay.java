package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

import java.util.List;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class QuestHudOverlay {

    private static final int WIDGET_W = 164;
    private static final int MARGIN_R = 6;
    private static final int MARGIN_T = 6;
    private static final int PAD = 5;
    private static final int ROW_H = 11;
    private static final int BAR_H = 4;

    private static final int C_BG = 0xCC0B0B0F;
    private static final int C_BORDER = 0xFF252530;
    private static final int C_TITLE_BG = 0xDD09090D;
    private static final int C_DONE_ROW = 0x220044FF;
    private static final int C_PROG_BG = 0xFF141420;
    private static final int C_PROG_FILL = 0xFF00AA55;
    private static final int C_PROG_ACT = 0xFFBB8800;
    private static final int C_TEXT = 0xFFD8D8E4;
    private static final int C_TEXT_DIM = 0xFF888898;
    private static final int C_TEXT_DONE = 0xFF44CC88;
    private static final int C_TEXT_ACT = 0xFFFFBB33;
    private static final int C_PIN = 0xFFAA44FF;

    private static int lastWx = -1, lastWy = -1, lastWh = -1;

    private static ResourceLocation lastPinnedId = null;
    private static long pinChangeTimeMs = -1;
    private static final long FADE_MS = 200;

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        if (event.getButton() != 0 || event.getAction() != 1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || lastWx < 0) return;
        double scale = mc.getWindow().getGuiScale();
        int mx = (int) (mc.mouseHandler.xpos() / scale);
        int my = (int) (mc.mouseHandler.ypos() / scale);
        if (mx >= lastWx && mx <= lastWx + WIDGET_W && my >= lastWy && my <= lastWy + lastWh) {
            mc.setScreen(new ChronicleOverviewScreen());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        QuestToastManager.get().tick();
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.player.isDeadOrDying()) return;

        PlayerQuestData data = mc.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).orElse(null);
        if (data == null) return;

        ResourceLocation pinnedId = data.getPinnedQuestId();
        if (pinnedId == null) {
            lastPinnedId = null;
            return;
        }

        if (!pinnedId.equals(lastPinnedId)) {
            lastPinnedId = pinnedId;
            pinChangeTimeMs = System.currentTimeMillis();
        }

        QuestNode node = QuestTreeRegistry.getQuest(pinnedId);
        if (node == null) {
            data.clearPin();
            return;
        }

        QuestState state = data.getQuestState(pinnedId, QuestState.LOCKED);
        List<QuestTask> tasks = node.getTasks();

        int done = 0;
        for (QuestTask t : tasks) if (t.isCompletedFor(mc.player)) done++;

        QuestChroniclesSettings cfg = QuestChroniclesSettings.get();
        boolean showTitle = cfg.isShowHUDTitle();
        boolean showProgress = cfg.isShowHUDProgress();

        int taskRows = Math.min(tasks.size(), 6);
        int titleH = showTitle ? PAD + ROW_H + 3 : PAD;
        int barSection = (showProgress && !tasks.isEmpty()) ? BAR_H + 4 : 0;
        int widgetH = titleH + taskRows * ROW_H + (tasks.size() > 6 ? ROW_H : 0) + barSection + PAD;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int wx, wy;
        switch (cfg.getHudPosition()) {
            case TOP_LEFT -> {
                wx = MARGIN_R;
                wy = MARGIN_T;
            }
            case TOP_CENTER -> {
                wx = (screenW - WIDGET_W) / 2;
                wy = MARGIN_T;
            }
            case BOTTOM_LEFT -> {
                wx = MARGIN_R;
                wy = screenH - widgetH - MARGIN_T;
            }
            case BOTTOM_CENTER -> {
                wx = (screenW - WIDGET_W) / 2;
                wy = screenH - widgetH - MARGIN_T;
            }
            case BOTTOM_RIGHT -> {
                wx = screenW - WIDGET_W - MARGIN_R;
                wy = screenH - widgetH - MARGIN_T;
            }
            default -> {
                wx = screenW - WIDGET_W - MARGIN_R;
                wy = MARGIN_T;
            }
        }

        int bgAlpha = (int) (cfg.getHudOpacity() * 0xCC);
        int dynBg = (bgAlpha << 24) | 0x0B0B0F;

        g.fill(wx, wy, wx + WIDGET_W, wy + widgetH, dynBg);
        g.fill(wx, wy, wx + WIDGET_W, wy + 1, C_BORDER);
        g.fill(wx, wy + widgetH - 1, wx + WIDGET_W, wy + widgetH, C_BORDER);
        g.fill(wx, wy, wx + 1, wy + widgetH, C_BORDER);
        g.fill(wx + WIDGET_W - 1, wy, wx + WIDGET_W, wy + widgetH, C_BORDER);

        int ty = wy + PAD;

        if (showTitle) {

            g.fill(wx + 1, wy + 1, wx + WIDGET_W - 1, wy + PAD + ROW_H + 1, C_TITLE_BG);

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

            if (node.getIconItem() != null && node.getIconItem() != net.minecraft.world.item.Items.AIR) {
                g.renderItem(new ItemStack(node.getIconItem()), wx + PAD, wy + PAD - 2);
                String titleStr = truncate(font, node.getTitle().getString(), WIDGET_W - PAD * 2 - 20);
                g.drawString(font, stateGlyph + " " + titleStr, wx + PAD + 18, wy + PAD + 1, titleColor, false);
            } else {
                String titleStr = truncate(font, node.getTitle().getString(), WIDGET_W - PAD * 2 - 14);
                g.drawString(font, stateGlyph + " " + titleStr, wx + PAD, wy + PAD + 1, titleColor, false);
            }

            g.drawString(font, "§5📌", wx + WIDGET_W - 14, wy + PAD, C_PIN, false);

            int divY = wy + PAD + ROW_H + 1;
            g.fill(wx + PAD, divY, wx + WIDGET_W - PAD, divY + 1, C_BORDER);
            ty = divY + 3;
        }

        for (int i = 0; i < taskRows; i++) {
            QuestTask task = tasks.get(i);
            boolean isDone = task.isCompletedFor(mc.player);
            if (isDone) g.fill(wx + 1, ty, wx + WIDGET_W - 1, ty + ROW_H, C_DONE_ROW);
            String check = isDone ? "§a✔" : "§8✗";
            String progress = !isDone ? task.getProgressString(mc.player) : null;
            String rawLabel = task.getDescription().getString();
            String label;
            if (progress != null) {

                String suffix = " §8(" + progress + ")";
                int suffixW = font.width(suffix.replaceAll("§.", ""));
                int maxLabelW = WIDGET_W - PAD * 2 - 14 - suffixW;
                label = truncate(font, rawLabel, maxLabelW) + suffix;
            } else {
                label = truncate(font, rawLabel, WIDGET_W - PAD * 2 - 14);
            }
            g.drawString(font, check + " §7" + label, wx + PAD, ty + 1, isDone ? C_TEXT_DONE : C_TEXT_DIM, false);
            ty += ROW_H;
        }
        if (tasks.size() > 6) {
            g.drawString(font, "§8+" + (tasks.size() - 6) + " more…", wx + PAD, ty + 1, C_TEXT_DIM, false);
            ty += ROW_H;
        }

        if (showProgress && !tasks.isEmpty()) {
            ty += 3;
            int n = tasks.size();
            int maxPips = Math.min(n, 20);
            int pipAreaW = WIDGET_W - PAD * 2 - 2;
            int pipW = Math.max(3, Math.min(9, (pipAreaW - (maxPips - 1)) / maxPips));
            int gap = 1;
            int totalPipW = maxPips * pipW + (maxPips - 1) * gap;
            int pipX = wx + PAD + (pipAreaW - totalPipW) / 2;
            int barCol = state == QuestState.COMPLETED ? C_PROG_FILL : C_PROG_ACT;
            for (int pi = 0; pi < maxPips; pi++) {
                boolean pipDone = pi < done;
                int px = pipX + pi * (pipW + gap);

                g.fill(px, ty, px + pipW, ty + BAR_H, C_PROG_BG);

                if (pipDone) g.fill(px, ty, px + pipW, ty + BAR_H, barCol);

                if (pipDone) g.fill(px, ty, px + pipW, ty + 1, 0x33FFFFFF);
            }
            if (n > maxPips) {

                g.drawString(font, "§8+" + (n - maxPips), pipX + totalPipW + 3, ty - 1, C_TEXT_DIM, false);
            } else {
                g.drawString(font, "§8" + done + "/" + n, wx + PAD + pipAreaW - font.width(done + "/" + n) + 1, ty - 1,
                        C_TEXT_DIM, false);
            }
        }

        lastWx = wx;
        lastWy = wy;
        lastWh = widgetH;

        if (pinChangeTimeMs > 0) {
            long elapsed = System.currentTimeMillis() - pinChangeTimeMs;
            if (elapsed < FADE_MS) {
                float t = 1f - (float) elapsed / FADE_MS;
                int fadeAlpha = (int) (t * t * 0xEE) & 0xFF;
                g.fill(wx, wy, wx + WIDGET_W, wy + widgetH, (fadeAlpha << 24) | 0x000000);
            }
        }

        QuestToastManager.get().render(g, screenW, mc.getWindow().getGuiScaledHeight());
    }

    private static String truncate(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, maxW - 6) + "…";
    }
}
