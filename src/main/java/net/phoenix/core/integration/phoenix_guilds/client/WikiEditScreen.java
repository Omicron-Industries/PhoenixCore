package net.phoenix.core.integration.phoenix_guilds.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.phoenix_guilds.network.C2SGuildActionPacket;
import net.phoenix.core.network.PhoenixNetwork;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static net.phoenix.core.integration.phoenix_guilds.client.GuildThemeUtils.*;

@OnlyIn(Dist.CLIENT)
public class WikiEditScreen extends Screen {

    // Colors delegated to GuildThemeUtils via static import

    private static final int MAX_CONTENT = 512;
    private static final int MAX_TITLE   = 64;

    private static final char[] COLOR_CODES = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f','r'};
    private static final int[]  COLOR_VALUES = {
            0xFF000000,0xFF0000AA,0xFF00AA00,0xFF00AAAA,0xFFAA0000,0xFFAA00AA,0xFFFFAA00,0xFFAAAAAA,
            0xFF555555,0xFF5555FF,0xFF55FF55,0xFF55FFFF,0xFFFF5555,0xFFFF55FF,0xFFFFFF55,0xFFFFFFFF,
            0xFFFFFFFF
    };

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int W = 420;
    private static final int H = 210;
    private int px, py;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final String initialTitle;
    private final String initialContent;

    private EditBox     titleBox;
    private ContentArea contentArea;

    public WikiEditScreen(Screen parent, String existingTitle, String existingContent) {
        super(Component.literal(existingTitle.isEmpty() ? "New Wiki Page" : "Edit: " + existingTitle));
        this.parent         = parent;
        this.initialTitle   = existingTitle;
        this.initialContent = existingContent;
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;

        titleBox = new EditBox(font, px + 8, py + 24, W - 16, 14, Component.empty());
        titleBox.setMaxLength(MAX_TITLE);
        titleBox.setHint(Component.literal("Page title..."));
        titleBox.setValue(initialTitle);
        addRenderableWidget(titleBox);

        contentArea = new ContentArea(px + 8, py + 44, W - 16, H - 88);
        contentArea.setValue(initialContent);
        addRenderableWidget(contentArea);

        setInitialFocus(initialTitle.isEmpty() ? titleBox : contentArea);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, C_BG());
        g.fill(px, py, px + W, py + H, C_PANEL());
        g.fill(px, py, px + W, py + 1, C_ACCENT());
        g.drawCenteredString(font, getTitle(), px + W / 2, py + 7, C_ACCENT());
        g.drawString(font, "Title:", px + 8, py + 15, C_DIM(), false);

        super.render(g, mx, my, pt);

        renderColorPicker(g, mx, my);
        int btnY = py + H - 20;
        int half = W / 2 - 6;
        drawBtn(g, mx, my, px + 4,        btnY, half, 16, "Confirm", 0xFF0A3318, 0xFF105528);
        drawBtn(g, mx, my, px + W / 2 + 2, btnY, half, 16, "Cancel",  0xFF330A0A, 0xFF551010);
    }

    private void renderColorPicker(GuiGraphics g, int mx, int my) {
        int pickerY = py + H - 40;
        g.drawString(font, "Colors:", px + 8, pickerY + 2, C_DIM(), false);
        int boxX = px + 8 + font.width("Colors:") + 4;
        int sz = 11; int gap = 2;
        for (int i = 0; i < COLOR_CODES.length; i++) {
            int cx  = boxX + i * (sz + gap);
            boolean hov = mx >= cx && mx < cx + sz && my >= pickerY && my < pickerY + sz;
            g.fill(cx, pickerY, cx + sz, pickerY + sz, COLOR_VALUES[i]);
            g.renderOutline(cx, pickerY, sz, sz, hov ? C_ACCENT() : 0xFF333355);
            if (hov) g.renderTooltip(font, Component.literal("§" + COLOR_CODES[i] + "§" + COLOR_CODES[i]), mx, my);
        }
    }

    private void drawBtn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, int col, int colHov) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hov ? colHov : col);
        if (hov) { g.fill(x, y, x + w, y + 1, C_ACCENT()); g.fill(x, y + h - 1, x + w, y + h, C_ACCENT()); }
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hov ? C_ACCENT() : C_TEXT());
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int btnY = py + H - 20;
        int half = W / 2 - 6;
        if (mx >= px + 4 && mx < px + 4 + half && my >= btnY && my < btnY + 16) { confirm(); return true; }
        if (mx >= px + W / 2 + 2 && mx < px + W && my >= btnY && my < btnY + 16) { Minecraft.getInstance().setScreen(parent); return true; }

        int pickerY = py + H - 40;
        int boxX    = px + 8 + font.width("Colors:") + 4;
        int sz = 11; int gap = 2;
        for (int i = 0; i < COLOR_CODES.length; i++) {
            int cx = boxX + i * (sz + gap);
            if (mx >= cx && mx < cx + sz && my >= pickerY && my < pickerY + sz) {
                setInitialFocus(contentArea);
                contentArea.insertFormatCode("§" + COLOR_CODES[i]);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && !hasShiftDown()) { confirm(); return true; }
        if (kc == GLFW.GLFW_KEY_ESCAPE) { Minecraft.getInstance().setScreen(parent); return true; }
        return super.keyPressed(kc, sc, mod);
    }

    private void confirm() {
        String title   = titleBox != null ? titleBox.getValue().trim() : "";
        String content = contentArea != null ? contentArea.getValue() : "";
        if (title.isEmpty()) return;
        //  is the title/content separator (not a printable char, won't appear in user input)
        PhoenixNetwork.CHANNEL.sendToServer(
                new C2SGuildActionPacket(C2SGuildActionPacket.Action.WIKI_SET, title + '' + content));
        Minecraft.getInstance().setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Multiline content widget ──────────────────────────────────────────────

    class ContentArea extends AbstractWidget {

        private final MultilineTextField tf;
        private final List<LinePos>      lineCache = new ArrayList<>();

        ContentArea(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.tf = new MultilineTextField(WikiEditScreen.this.font, w - 10);
            this.tf.setCharacterLimit(MAX_CONTENT);
        }

        void setValue(String v)   { tf.setValue(v); }
        String getValue()          { return tf.value(); }

        void insertFormatCode(String code) {
            String full    = tf.value();
            int    cur     = tf.cursor();
            String updated = full.substring(0, cur) + code + full.substring(cur);
            if (updated.length() <= MAX_CONTENT) {
                tf.setValue(updated);
                tf.seekCursor(Whence.ABSOLUTE, cur + code.length());
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF060610);
            g.renderOutline(getX(), getY(), width, height, isFocused() ? C_ACCENT() : 0xFF222240);

            int tx   = getX() + 5;
            int ty   = getY() + 5;
            String full = tf.value();
            int    cur  = tf.cursor();

            lineCache.clear();
            if (full.isEmpty()) {
                lineCache.add(new LinePos(0, 0, ""));
                g.drawString(WikiEditScreen.this.font, "Write your wiki page here... (Shift+Enter for new line)", tx, ty, 0xFF2A2A4A, false);
            } else {
                WikiEditScreen.this.font.getSplitter().splitLines(full, width - 10, Style.EMPTY, false,
                        (style, s, e) -> lineCache.add(new LinePos(s, e, full.substring(s, e))));
                if (full.endsWith("\n"))
                    lineCache.add(new LinePos(full.length(), full.length(), ""));
            }

            // Selection
            if (tf.hasSelection()) {
                String sel = tf.getSelectedText();
                int si     = full.indexOf(sel);
                if (si >= 0) {
                    int se = si + sel.length();
                    for (int i = 0; i < lineCache.size(); i++) {
                        LinePos lp = lineCache.get(i);
                        int ly = ty + i * 9;
                        if (se > lp.start && si < lp.end) {
                            int a  = Math.max(si, lp.start) - lp.start;
                            int b  = Math.min(se, lp.end)   - lp.start;
                            int x1 = tx + WikiEditScreen.this.font.width(lp.text.substring(0, a));
                            int x2 = tx + WikiEditScreen.this.font.width(lp.text.substring(0, b));
                            g.fill(x1, ly, x2, ly + 9, 0xFF2244AA);
                        }
                    }
                }
            }

            // Text + caret
            for (int i = 0; i < lineCache.size(); i++) {
                LinePos lp = lineCache.get(i);
                int ly = ty + i * 9;
                g.drawString(WikiEditScreen.this.font, lp.text, tx, ly, C_TEXT(), false);
                if (isFocused() && cur >= lp.start && cur <= lp.end && (System.currentTimeMillis() / 500) % 2 == 0) {
                    int off  = Math.min(cur - lp.start, lp.text.length());
                    int curX = tx + WikiEditScreen.this.font.width(lp.text.substring(0, off));
                    g.fill(curX, ly, curX + 1, ly + 9, C_ACCENT());
                }
            }

            // Char count
            String cc = tf.value().length() + "/" + MAX_CONTENT;
            g.drawString(WikiEditScreen.this.font, cc,
                    getX() + width - WikiEditScreen.this.font.width(cc) - 4, getY() + height - 10, C_DIM(), false);
        }

        private int coordToIndex(double mx, double my) {
            if (lineCache.isEmpty()) return 0;
            int row = Math.max(0, Math.min(lineCache.size() - 1, (int)((my - (getY() + 5)) / 9)));
            LinePos lp    = lineCache.get(row);
            int     local = (int)(mx - (getX() + 5));
            int     off   = 0; int vw = 0;
            while (off < lp.text.length()) {
                if (lp.text.charAt(off) == '§' && off + 1 < lp.text.length()) { off += 2; continue; }
                vw = WikiEditScreen.this.font.width(lp.text.substring(0, off + 1));
                if (vw > local) break;
                off++;
            }
            return lp.start + off;
        }

        private void setCursorDirect(int pos) {
            try {
                java.lang.reflect.Field f;
                try   { f = MultilineTextField.class.getDeclaredField("cursor"); }
                catch (NoSuchFieldException e) { f = MultilineTextField.class.getDeclaredField("f_239201_"); }
                f.setAccessible(true); f.setInt(tf, pos);
            } catch (Exception ignored) {}
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (mx >= getX() && mx < getX() + width && my >= getY() && my < getY() + height) {
                setFocused(true);
                tf.seekCursor(Whence.ABSOLUTE, coordToIndex(mx, my));
                return true;
            }
            setFocused(false); return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (isFocused() && btn == 0) { setCursorDirect(coordToIndex(mx, my)); return true; }
            return super.mouseDragged(mx, my, btn, dx, dy);
        }

        @Override
        public boolean keyPressed(int kc, int sc, int mod) {
            if (!isFocused()) return false;
            if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && hasShiftDown()) { tf.insertText("\n"); return true; }
            return tf.keyPressed(kc) || super.keyPressed(kc, sc, mod);
        }

        @Override
        public boolean charTyped(char c, int mod) {
            if (isFocused() && SharedConstants.isAllowedChatCharacter(c)) { tf.insertText(Character.toString(c)); return true; }
            return false;
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput out) {}

        private record LinePos(int start, int end, String text) {}
    }
}
