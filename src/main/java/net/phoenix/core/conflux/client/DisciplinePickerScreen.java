package net.phoenix.core.conflux.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.conflux.client.render.MotionClock;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class DisciplinePickerScreen extends Screen {

    private static final int CARD_W = 210;
    private static final int CARD_H = 280;
    private static final float ARC_RADIUS = 380f;

    private record DisciplineCard(
                                  String id,
                                  String name,
                                  String tagline,
                                  String[] loreLines,
                                  int accentColor,
                                  int glowColor,
                                  int bgColor,
                                  boolean sealed) {}

    private static final DisciplineCard[] CARDS = {
            new DisciplineCard(
                    "phoenix",
                    "The Phoenix",
                    "Rebirth through consumption.",
                    new String[] {
                            "Every ending is an ignition.",
                            "The Phoenix does not endure,",
                            "it transcends.",
                            "",
                            "You will burn. You will rise.",
                            "The question is only how many",
                            "times you are willing to do it."
                    },
                    0xFFFF8800, 0xFFFF4400, 0xFF1A0800,
                    false),
            new DisciplineCard(
                    "sculk",
                    "The Sculk",
                    "The network remembers.",
                    new String[] {
                            "Before the first thing, there",
                            "was the space between things.",
                            "",
                            "You are not absorbed. You",
                            "persist, within something",
                            "immeasurably larger.",
                            "",
                            "The network has always been",
                            "thinking. Now it thinks with you."
                    },
                    0xFF00CC88, 0xFF004422, 0xFF000A06,
                    false),
            new DisciplineCard(
                    "void",
                    "The Void",
                    "Patience older than light.",
                    new String[] {
                            "Before the first thing, there",
                            "was the space between things.",
                            "The Void was not empty,",
                            "it was patient.",
                            "",
                            "Two eyes see one world.",
                            "One eye sees two.",
                            "Beyond the threshold, depth",
                            "is a negotiation."
                    },
                    0xFF6633FF, 0xFF220066, 0xFF010108,
                    false),
            new DisciplineCard(
                    "sealed_a",
                    "██████",
                    "You should not be reading this.",
                    new String[] {
                            "This record has been sealed",
                            "pending review.",
                            "",
                            "Access requires prior",
                            "commitment to an incompatible",
                            "path.",
                            "",
                            "We have not decided what",
                            "finding this means about you."
                    },
                    0xFFAAAAAA, 0xFF333333, 0xFF080808,
                    true),
            new DisciplineCard(
                    "sealed_b",
                    "???",
                    "...",
                    new String[] {
                            "...",
                            "",
                            "You are being watched.",
                            "You have always been watched.",
                            "This terminal is how",
                            "it watches back."
                    },
                    0xFF888855, 0xFF222210, 0xFF050504,
                    true),
    };

    private final MotionClock clock = new MotionClock();
    private long lastNanos = -1;

    private int hoveredIdx = -1;
    private int selectedIdx = -1;
    private float arcOffset = 0f;
    private float arcVelocity = 0f;
    private float introAnim = 0f;

    private final float[] cardScale = new float[CARDS.length];
    private final float[] cardGlow = new float[CARDS.length];
    private final float[] cardLift = new float[CARDS.length];
    private final float[] cardReveal = new float[CARDS.length];

    private float confirmAlpha = 0f;
    private boolean confirming = false;

    public DisciplinePickerScreen() {
        super(Component.literal("Choose Your Path"));
        for (int i = 0; i < CARDS.length; i++) {
            cardScale[i] = 0.88f;
            cardGlow[i] = 0f;
            cardReveal[i] = 1f;
        }
    }

    @Override
    protected void init() {
        arcOffset = 0f;
    }

    @Override
    public void tick() {
        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0.016f : (float) ((now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        dt = Math.min(dt, 0.1f);

        clock.tick(dt);
        introAnim = Math.min(1f, introAnim + dt * 1.8f);

        arcOffset += arcVelocity * dt;
        arcVelocity *= Math.pow(0.08, dt);

        for (int i = 0; i < CARDS.length; i++) {
            boolean hov = (i == hoveredIdx);
            boolean sel = (i == selectedIdx);
            float targetScale = sel ? 1.06f : hov ? 1.02f : 0.88f;
            float targetGlow = sel ? 1.0f : hov ? 0.55f : 0.0f;
            float targetLift = sel ? -18f : hov ? -8f : 0f;
            cardScale[i] = lerp(cardScale[i], targetScale, dt * 8f);
            cardGlow[i] = lerp(cardGlow[i], targetGlow, dt * 6f);
            cardLift[i] = lerp(cardLift[i], targetLift, dt * 7f);
        }

        float targetConfirm = confirming ? 1f : 0f;
        confirmAlpha = lerp(confirmAlpha, targetConfirm, dt * 7f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        tick();
        updateHover(mx, my);

        float ease = easeOutExpo(introAnim);
        int cx = width / 2, cy = height / 2;

        g.fill(0, 0, width, height, 0xFF020208);
        renderStarfield(g, ease);
        renderAmbientGlow(g, ease);

        int titleAlpha = (int) (ease * 220) << 24;
        String title = "CHOOSE YOUR PATH";
        int tw = font.width(title);

        g.drawString(font, title, cx - tw / 2 + 1, 31, 0xFF000000 & titleAlpha, false);
        g.drawString(font, title, cx - tw / 2, 30, titleAlpha | 0xCCCCDD, false);

        String sub = "Each discipline shapes how you relate to the machine. There is no wrong answer , only yours.";
        int subW = font.width(sub);
        int subAlpha = (int) (ease * 100) << 24;
        g.drawString(font, sub, cx - subW / 2, 46, subAlpha | 0x8888AA, false);

        for (int i = 0; i < CARDS.length; i++) {
            float[] pos = cardScreenPos(i);
            renderCard(g, CARDS[i], i, pos[0], pos[1], ease, mx, my);
        }

        int hintAlpha = (int) (ease * 120) << 24;
        String hint = "← → to browse   •   Click to select   •   ESC to close";
        g.drawCenteredString(font, hint, cx, height - 22, hintAlpha | 0x666688);

        if (confirmAlpha > 0.01f && selectedIdx >= 0) {
            renderConfirmOverlay(g, mx, my);
        }

        super.render(g, mx, my, pt);
    }

    private void renderStarfield(GuiGraphics g, float ease) {
        int count = 180;
        float t = clock.getElapsed();
        for (int i = 0; i < count; i++) {
            float hx = MotionClock.hash((long) (i * 7L + 1));
            float hy = MotionClock.hash((long) (i * 13L + 3));
            float hs = MotionClock.hash((long) (i * 31L + 7));
            float hp = MotionClock.hash((long) (i * 17L + 11));
            int sx = (int) (hx * width);
            int sy = (int) (hy * height);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * (0.4f + hs * 1.2f) + hp * 6.28f);
            int alpha = (int) (ease * twinkle * 160 * (0.4f + hs * 0.6f)) << 24;
            if (alpha < (5 << 24)) continue;
            g.fill(sx, sy, sx + 1, sy + 1, alpha | 0xCCDDFF);
        }
    }

    private void renderAmbientGlow(GuiGraphics g, float ease) {
        if (hoveredIdx < 0) return;
        DisciplineCard card = CARDS[hoveredIdx];
        float[] pos = cardScreenPos(hoveredIdx);
        int glowA = (int) (ease * cardGlow[hoveredIdx] * 30) << 24;
        int r = 200;
        g.fill((int) pos[0] - r, (int) pos[1] - r, (int) pos[0] + r, (int) pos[1] + r,
                glowA | (card.glowColor & 0x00FFFFFF));
    }

    private void renderCard(GuiGraphics g, DisciplineCard card, int idx,
                            float fx, float fy, float ease, int mx, int my) {
        float sc = cardScale[idx] * ease;
        float lift = cardLift[idx] * ease;
        int cx = (int) fx;
        int cy = (int) (fy + lift);

        int hw = (int) (CARD_W * sc / 2);
        int hh = (int) (CARD_H * sc / 2);
        int x0 = cx - hw, y0 = cy - hh, x1 = cx + hw, y1 = cy + hh;

        boolean hov = (idx == hoveredIdx);
        boolean sel = (idx == selectedIdx);

        if (cardGlow[idx] > 0.02f) {
            int gAlpha = (int) (cardGlow[idx] * 80) << 24;
            g.fill(x0 - 8, y0 - 8, x1 + 8, y1 + 8, gAlpha | (card.glowColor & 0x00FFFFFF));
            g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, gAlpha | (card.accentColor & 0x00FFFFFF));
        }

        g.fill(x0, y0, x1, y1, card.bgColor | 0xEE000000);

        float reveal = cardReveal[idx];
        if (reveal < 1f) {
            int blankY = y0 + (int) ((y1 - y0) * reveal);
            g.fill(x0, blankY, x1, y1, 0xFF020208);
        }

        int stripeH = Math.max(2, (int) (4 * sc));
        g.fill(x0, y0, x1, y0 + stripeH, card.accentColor);

        int brdCol = sel ? card.accentColor :
                hov ? (card.accentColor & 0x00FFFFFF | 0xAA000000) : (card.accentColor & 0x00FFFFFF | 0x33000000);
        g.fill(x0, y0, x1, y0 + 1, brdCol);
        g.fill(x0, y1 - 1, x1, y1, brdCol);
        g.fill(x0, y0, x0 + 1, y1, brdCol);
        g.fill(x1 - 1, y0, x1, y1, brdCol);

        if (card.sealed) {
            float glitch = (float) Math.sin(clock.getElapsed() * 11.3f + idx * 2.1f);
            int gx = cx + (int) (glitch * 2.5f);
            float pulse = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(clock.getElapsed() * 2.2f));
            int sealAlpha = (int) (pulse * 60) << 24;
            g.fill(x0, y0, x1, y1, sealAlpha | 0x000000);

            for (int li = 0; li < 5; li++) {
                int lx = x0 + (x1 - x0) * li / 5 + (int) (glitch * li * 1.5f);
                g.fill(lx, y0, lx + 1, y1, (int) (pulse * 20) << 24 | 0x888888);
            }
        }

        if (reveal > 0.15f) {
            int textY = y0 + stripeH + (int) (14 * sc);
            int nameAlpha = (int) (Math.min(1f, (reveal - 0.15f) / 0.35f) * 255) << 24;

            int nameFontW = font.width(card.name);
            g.drawString(font, card.name, cx - nameFontW / 2, textY, nameAlpha | (card.accentColor & 0x00FFFFFF),
                    false);
            textY += (int) (14 * sc) + 2;

            if (reveal > 0.3f) {
                int tagAlpha = (int) (Math.min(1f, (reveal - 0.3f) / 0.3f) * 180) << 24;
                int tagW = font.width(card.tagline);
                g.drawString(font, "§o" + card.tagline, cx - tagW / 2, textY, tagAlpha | 0xAAAAAA, false);
                textY += (int) (13 * sc) + 4;

                g.fill(x0 + (int) (16 * sc), textY, x1 - (int) (16 * sc), textY + 1,
                        tagAlpha | (card.accentColor & 0x00FFFFFF));
                textY += (int) (8 * sc);
            }

            if (reveal > 0.45f && sc > 0.7f) {
                int loreAlpha = (int) (Math.min(1f, (reveal - 0.45f) / 0.3f) * 160) << 24;
                int maxLines = (int) ((y1 - textY - (int) (24 * sc)) / (int) (10 * sc));
                for (int li = 0; li < Math.min(card.loreLines.length, maxLines); li++) {
                    String line = card.loreLines[li];
                    if (line.isEmpty()) {
                        textY += (int) (5 * sc);
                        continue;
                    }
                    int lw2 = font.width(line);
                    g.drawString(font, line, cx - lw2 / 2, textY, loreAlpha | 0x888899, false);
                    textY += (int) (10 * sc) + 1;
                }
            }
        }

        if (hov || sel) {
            int btnH = (int) (20 * sc);
            int btnY = y1 - btnH - (int) (8 * sc);
            int btnX0 = x0 + (int) (12 * sc);
            int btnX1 = x1 - (int) (12 * sc);
            boolean btnHov = mx >= btnX0 && mx < btnX1 && my >= btnY && my < btnY + btnH;

            int btnBg = sel ? (card.accentColor & 0x00FFFFFF | 0x88000000) : btnHov ?
                    (card.accentColor & 0x00FFFFFF | 0x66000000) : (card.accentColor & 0x00FFFFFF | 0x33000000);
            g.fill(btnX0, btnY, btnX1, btnY + btnH, btnBg);
            g.fill(btnX0, btnY, btnX1, btnY + 1, card.accentColor);
            g.fill(btnX0, btnY + btnH - 1, btnX1, btnY + btnH, card.accentColor);

            String btnLabel = sel ? "§lSelected" : card.sealed ? "§8Sealed" : "Choose This Path";
            int blw = font.width(btnLabel);
            g.drawString(font, btnLabel, cx - blw / 2, btnY + (btnH - 8) / 2,
                    sel ? (card.accentColor | 0xFF000000) : 0xFFCCCCCC, false);
        }
    }

    private void renderConfirmOverlay(GuiGraphics g, int mx, int my) {
        if (selectedIdx < 0) return;
        DisciplineCard card = CARDS[selectedIdx];

        int ovAlpha = (int) (confirmAlpha * 200) << 24;
        g.fill(0, 0, width, height, ovAlpha | 0x000000);

        int cx = width / 2, cy = height / 2;
        int bw = 360, bh = 220;
        int bx0 = cx - bw / 2, by0 = cy - bh / 2;

        g.fill(bx0, by0, bx0 + bw, by0 + bh, 0xEE050510);
        g.fill(bx0, by0, bx0 + bw, by0 + 3, card.accentColor);
        g.fill(bx0, by0, bx0 + 1, by0 + bh, card.accentColor & 0x00FFFFFF | 0x88000000);
        g.fill(bx0 + bw - 1, by0, bx0 + bw, by0 + bh, card.accentColor & 0x00FFFFFF | 0x88000000);
        g.fill(bx0, by0 + bh - 1, bx0 + bw, by0 + bh, card.accentColor & 0x00FFFFFF | 0x88000000);

        int ty = by0 + 20;
        int nameW = font.width(card.name);
        g.drawString(font, card.name, cx - nameW / 2, ty, card.accentColor | 0xFF000000, false);
        ty += 16;

        String[] confirmLines = card.sealed ?
                new String[] { "This path is sealed.", "Committing to it may have consequences",
                        "beyond what this terminal can show you." } :
                new String[] { "You are about to choose " + card.name + ".", "This choice can be changed later,",
                        "but not without cost.", "Are you certain?" };

        for (String line : confirmLines) {
            int lw = font.width(line);
            g.drawString(font, "§7" + line, cx - lw / 2, ty, 0xFFCCCCCC, false);
            ty += 12;
        }

        ty = by0 + bh - 44;

        int cbw = 130, cbh = 24;
        int cbx = cx - cbw - 8;
        boolean confHov = mx >= cbx && mx < cbx + cbw && my >= ty && my < ty + cbh;
        g.fill(cbx, ty, cbx + cbw, ty + cbh, confHov ? 0xAA224422 : 0x88112211);
        g.fill(cbx, ty, cbx + cbw, ty + 1, card.accentColor);
        g.fill(cbx, ty + cbh - 1, cbx + cbw, ty + cbh, card.accentColor);
        String confLabel = "Commit";
        g.drawCenteredString(font, confLabel, cbx + cbw / 2, ty + 8, card.sealed ? 0xFFAAAAAA : 0xFF88DD88);

        int cax = cx + 8;
        boolean canHov = mx >= cax && mx < cax + cbw && my >= ty && my < ty + cbh;
        g.fill(cax, ty, cax + cbw, ty + cbh, canHov ? 0xAA221111 : 0x88110808);
        g.fill(cax, ty, cax + cbw, ty + 1, 0xFF882222);
        g.fill(cax, ty + cbh - 1, cax + cbw, ty + cbh, 0xFF882222);
        g.drawCenteredString(font, "Go Back", cax + cbw / 2, ty + 8, 0xFFCC4444);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        if (confirming && selectedIdx >= 0) {
            int cx2 = width / 2, cy2 = height / 2;
            int bh = 220;
            int by0 = cy2 - bh / 2;
            int ty = by0 + bh - 44;
            int cbw = 130, cbh = 24;

            int cbx = cx2 - cbw - 8;
            if (mx >= cbx && mx < cbx + cbw && my >= ty && my < ty + cbh) {
                commitDiscipline(CARDS[selectedIdx]);
                return true;
            }

            int cax = cx2 + 8;
            if (mx >= cax && mx < cax + cbw && my >= ty && my < ty + cbh) {
                confirming = false;
                return true;
            }

            confirming = false;
            return true;
        }

        for (int i = 0; i < CARDS.length; i++) {
            float[] pos = cardScreenPos(i);
            float sc = cardScale[i];
            int cx3 = (int) pos[0];
            int cy3 = (int) (pos[1] + cardLift[i]);
            int hw = (int) (CARD_W * sc / 2);
            int hh = (int) (CARD_H * sc / 2);
            int x0 = cx3 - hw, y0 = cy3 - hh, x1 = cx3 + hw, y1 = cy3 + hh;
            if (mx >= x0 && mx <= x1 && my >= y0 && my <= y1) {
                if (selectedIdx == i) {
                    confirming = true;
                } else {
                    selectedIdx = i;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int kc, int sc2, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (confirming) {
                confirming = false;
                return true;
            }
            onClose();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_LEFT || kc == GLFW.GLFW_KEY_A) {
            arcVelocity -= 600f;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_RIGHT || kc == GLFW.GLFW_KEY_D) {
            arcVelocity += 600f;
            return true;
        }
        return super.keyPressed(kc, sc2, mod);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        arcVelocity += (float) (delta * -200f);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0) {
            arcOffset += (float) dx * 1.5f;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    private void updateHover(int mx, int my) {
        hoveredIdx = -1;
        for (int i = 0; i < CARDS.length; i++) {
            float[] pos = cardScreenPos(i);
            float sc = cardScale[i];
            int cx = (int) pos[0];
            int cy2 = (int) (pos[1] + cardLift[i]);
            int hw = (int) (CARD_W * sc / 2);
            int hh = (int) (CARD_H * sc / 2);
            if (mx >= cx - hw && mx <= cx + hw && my >= cy2 - hh && my <= cy2 + hh) {
                hoveredIdx = i;
                break;
            }
        }
    }

    private void commitDiscipline(DisciplineCard card) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("§6[Axiom] §7Path chosen: §r" + card.name +
                            "§7. Costs will be applied in a future update."),
                    false);
        }
        onClose();
    }

    private float[] cardScreenPos(int i) {
        int n = CARDS.length;
        float spacing = CARD_W + 40f;
        float totalW = spacing * n;
        float baseX = width / 2f - totalW / 2f + spacing * i + spacing / 2f + arcOffset;

        float normalised = (baseX - width / 2f) / (width * 0.4f);
        float arcY = height / 2f + 20f + normalised * normalised * 60f;
        return new float[] { baseX, arcY };
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, t);
    }

    private static float easeOutExpo(float t) {
        return t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
