package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.phoenix.core.integration.phoenix_chronicles.ChroniclesTheme;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

/**
 * Full-featured theme editor for Phoenix Chronicles.
 *
 * Layout:
 *   Left canvas  — live preview of quest node states + dep line + text swatches,
 *                  then a scrollable theme list below
 *   Right sidebar — hex edit fields grouped by category, name input, Save / Exit
 *
 * UX parity with PhantasiaThemeEditorScreen:
 *   - Ctrl+Z undo  · Ctrl+S save
 *   - Unsaved-changes guard (click-again-to-discard pattern)
 *   - Deferred deletions — custom themes are only purged from disk on onClose()
 *   - Live preview updates as you type
 */
public class ChroniclesThemeEditorScreen extends Screen {

    private final Screen parent;

    // ── Sidebar edit fields ───────────────────────────────────────────────────
    private final List<FieldEntry> fields = new ArrayList<>();
    private final List<SectionLabel> sections = new ArrayList<>();
    private EditBox nameInput;

    // ── Theme list ────────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    /** Y of the first theme row — updated in renderPreview so mouseClicked stays in sync. */
    private int listRowsStartY = 0;

    // ── Deferred deletions (purged on onClose) ────────────────────────────────
    private final List<String> pendingDeletions = new ArrayList<>();

    // ── Undo stack ────────────────────────────────────────────────────────────
    private record Snap(String bg, String panel, String header, String border, String accent,
                        String text, String dim, String faint, String done, String active,
                        String locked, String name) {}

    private final Stack<Snap> undoStack = new Stack<>();
    private Snap savedSnap;
    private String lastTrackedName = null;
    private boolean isUndoing = false;

    // ── Unsaved-changes guard ─────────────────────────────────────────────────
    private boolean confirmActive = false;
    private String pendingAction = null;   // theme name, or "EXIT"

    // ── Cached palette (refreshed from live theme on every field change) ──────
    private int C_BG, C_PANEL, C_HEADER, C_BORDER, C_ACCENT, C_TEXT, C_DIM, C_FAINT;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int SIDEBAR_MIN = 185;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ChroniclesThemeEditorScreen(Screen parent) {
        super(Component.literal("Theme Editor"));
        this.parent = parent;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();
        fields.clear();
        sections.clear();

        ChroniclesTheme t = ChroniclesTheme.current();
        String curName = ChroniclesTheme.getActiveName();

        syncPalette(t);

        // Reset undo / snapshot only when the active theme actually changes
        if (!curName.equals(lastTrackedName)) {
            lastTrackedName = curName;
            savedSnap = makeSnap(t, curName);
            undoStack.clear();
            pendingDeletions.clear();
            confirmActive = false;
            pendingAction = null;
        }

        int sbW = sidebarW();
        int sbX = width - sbW + 6;
        int boxW = sbW - 82;
        int y = 38;
        int rh = height > 360 ? 20 : 17;

        sections.add(new SectionLabel("■ Base Layers", sbX, y)); y += 12;
        addField("BG",       t.bg,          sbX, y, boxW); y += rh;
        addField("Panel",    t.panel,        sbX, y, boxW); y += rh;
        addField("Header",   t.header,       sbX, y, boxW); y += rh;
        addField("Border",   t.border,       sbX, y, boxW); y += rh + 6;

        sections.add(new SectionLabel("■ Typography", sbX, y)); y += 12;
        addField("Text",     t.text,         sbX, y, boxW); y += rh;
        addField("Text Dim", t.textDim,      sbX, y, boxW); y += rh;
        addField("Faint",    t.textFaint,    sbX, y, boxW); y += rh + 6;

        sections.add(new SectionLabel("■ State Colors", sbX, y)); y += 12;
        addField("Accent",   t.accent,       sbX, y, boxW); y += rh;
        addField("Done",     t.done,         sbX, y, boxW); y += rh;
        addField("Active",   t.activeColor,  sbX, y, boxW); y += rh;
        addField("Locked",   t.locked,       sbX, y, boxW); y += rh + 6;

        // Name + buttons
        int ctrlY = Math.max(y + 4, height - 70);
        nameInput = new EditBox(font, width - sbW + 10, ctrlY, sbW - 20, 16, Component.literal("Theme name"));
        nameInput.setValue(curName);
        nameInput.setMaxLength(32);
        nameInput.setResponder(s -> confirmActive = false);
        addWidget(nameInput);

        addRenderableWidget(net.minecraft.client.gui.components.Button
                .builder(Component.literal("§aSave"), b -> save())
                .bounds(width - sbW + 10, ctrlY + 20, sbW - 20, 18).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button
                .builder(Component.literal("§7Exit"), b -> {
                    if (hasChanges()) {
                        if (!confirmActive || !"EXIT".equals(pendingAction)) {
                            confirmActive = true;
                            pendingAction = "EXIT";
                            return;
                        }
                        restore(savedSnap);
                    }
                    onClose();
                })
                .bounds(width - sbW + 10, ctrlY + 42, sbW - 20, 18).build());
    }

    private void addField(String label, ChroniclesTheme.ThemeColor target, int sbX, int y, int boxW) {
        EditBox box = new EditBox(font, sbX + 68, y, boxW, 16, Component.literal(label));
        box.setMaxLength(10);
        box.setValue(target.hex != null ? target.hex.toUpperCase(Locale.ROOT) : "FF000000");
        box.setResponder(v -> {
            if (!isUndoing) pushUndo();
            target.set(v);
            confirmActive = false;
            syncPalette(ChroniclesTheme.current());
        });
        addWidget(box);
        fields.add(new FieldEntry(label, target, box));
    }

    private void syncPalette(ChroniclesTheme t) {
        C_BG     = t.bg.getColor();
        C_PANEL  = t.panel.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_ACCENT = t.accent.getColor();
        C_TEXT   = t.text.getColor();
        C_DIM    = t.textDim.getColor();
        C_FAINT  = t.textFaint.getColor();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        int sbW = sidebarW();

        // Canvas background + sidebar
        g.fill(0, 0, width - sbW, height, C_BG);
        g.fill(width - sbW, 0, width, height, C_PANEL);
        g.fill(width - sbW, 0, width - sbW + 1, height, C_BORDER);

        // Sidebar header
        g.fill(width - sbW, 0, width, 28, C_HEADER);
        g.fill(width - sbW, 27, width, 28, C_BORDER);
        g.drawString(font, "§fTheme Editor", width - sbW + 8, 8, C_ACCENT, false);

        // Status line
        String status;
        int statusC;
        if (confirmActive) {
            status = "§eClick again to discard!";
            statusC = 0xFFFFBB33;
        } else if (hasChanges()) {
            status = "§7● " + ChroniclesTheme.getActiveName() + " (unsaved)";
            statusC = 0xFFFFBB33;
        } else {
            status = "§7○ " + ChroniclesTheme.getActiveName();
            statusC = C_DIM;
        }
        g.drawString(font, status, width - sbW + 8, 19, statusC, false);

        // Section headers + field labels
        for (SectionLabel s : sections) {
            g.drawString(font, "§8" + s.title, s.x, s.y, C_ACCENT, false);
        }
        for (FieldEntry f : fields) {
            // Label left of box
            g.drawString(font, f.label, f.box.getX() - 65, f.box.getY() + 4, C_TEXT, false);
            // Live color swatch after box
            int sx = f.box.getX() + f.box.getWidth() + 3;
            int sy = f.box.getY();
            int sw = 14;
            int previewColor;
            try { previewColor = (int) Long.parseUnsignedLong(f.box.getValue().trim().toUpperCase(Locale.ROOT), 16); }
            catch (Exception e) { previewColor = 0xFF888888; }
            g.fill(sx, sy, sx + sw, sy + 14, previewColor);
            // swatch border
            g.fill(sx,          sy,          sx + sw,     sy + 1,      C_BORDER);
            g.fill(sx,          sy + 13,     sx + sw,     sy + 14,     C_BORDER);
            g.fill(sx,          sy,          sx + 1,      sy + 14,     C_BORDER);
            g.fill(sx + sw - 1, sy,          sx + sw,     sy + 14,     C_BORDER);

            f.box.render(g, mx, my, partial);
        }
        if (nameInput != null) nameInput.render(g, mx, my, partial);

        renderPreview(g, mx, my, sbW);

        super.render(g, mx, my, partial);
    }

    private void renderPreview(GuiGraphics g, int mx, int my, int sbW) {
        ChroniclesTheme t = ChroniclesTheme.current();
        int canvasW = width - sbW;
        int mockW = Math.min(canvasW - 20, 360);
        int mockX = 10;

        // ── Animated header strip ─────────────────────────────────────────────
        int animY = 8;
        g.drawString(font, "§7Node states · dep line · text hierarchy", mockX, animY, C_FAINT, false);

        // ── Quest-node mockup ─────────────────────────────────────────────────
        int mockTop = animY + 13;
        int mockH = Math.min(150, (height - 20) / 2);

        // Canvas area
        g.fill(mockX, mockTop, mockX + mockW, mockTop + mockH, t.bg.getColor());
        drawBorder(g, mockX, mockTop, mockW, mockH, t.border.getColor());

        // Header strip
        int hdrH = 17;
        g.fill(mockX, mockTop, mockX + mockW, mockTop + hdrH, t.header.getColor());
        g.fill(mockX, mockTop + hdrH - 1, mockX + mockW, mockTop + hdrH, t.border.getColor());
        g.drawString(font, "§f✦ Phoenix Chronicles", mockX + 5, mockTop + 5, t.text.getColor(), false);

        // Sidebar strip
        int sideW = Math.min(52, mockW / 5);
        g.fill(mockX, mockTop + hdrH, mockX + sideW, mockTop + mockH, t.panel.getColor());
        g.fill(mockX + sideW, mockTop + hdrH, mockX + sideW + 1, mockTop + mockH, t.border.getColor());
        g.drawString(font, "§8All",    mockX + 4, mockTop + hdrH + 4,  t.textDim.getColor(), false);
        g.drawString(font, "§8Main",   mockX + 4, mockTop + hdrH + 15, t.textFaint.getColor(), false);
        g.drawString(font, "§8Expert", mockX + 4, mockTop + hdrH + 26, t.textFaint.getColor(), false);

        // Three fake quest nodes: Done · Active · Locked
        int sz = Math.max(16, Math.min(26, (mockW - sideW - 30) / 6));
        int n1x = mockX + sideW + 14;
        int n2x = n1x + sz * 2 + 14;
        int n3x = n2x + sz * 2 + 14;
        int ny  = mockTop + hdrH + (mockH - hdrH) / 2 - sz / 2;

        // Dep lines (behind nodes)
        drawMockLine(g, n1x + sz, ny + sz / 2, n2x,      ny + sz / 2, t.done.getColor());
        drawMockLine(g, n2x + sz, ny + sz / 2, n3x,      ny + sz / 2,
                (t.locked.getColor() & 0x00FFFFFF) | 0x66000000);

        // Node 1 — DONE (green)
        g.fill(n1x, ny, n1x + sz, ny + sz, (t.done.getColor() & 0x00FFFFFF) | 0xFF081A0E);
        drawBorder(g, n1x, ny, sz, sz, t.done.getColor());
        g.drawCenteredString(font, "§a✔", n1x + sz / 2, ny + sz / 2 - 4, t.done.getColor());

        // Node 2 — ACTIVE (gold pulse sim: static here)
        g.fill(n2x, ny, n2x + sz, ny + sz, (t.activeColor.getColor() & 0x00FFFFFF) | 0xFF221C00);
        drawBorder(g, n2x, ny, sz, sz, t.activeColor.getColor());
        g.drawCenteredString(font, "§e◎", n2x + sz / 2, ny + sz / 2 - 4, t.activeColor.getColor());

        // Node 3 — LOCKED (dim)
        g.fill(n3x, ny, n3x + sz, ny + sz, (t.locked.getColor() & 0x00FFFFFF) | 0xFF1A1A24);
        drawBorder(g, n3x, ny, sz, sz, t.locked.getColor());
        g.fill(n3x + 1, ny + 1, n3x + sz - 1, ny + sz - 1, 0x880B0B0F);
        g.drawCenteredString(font, "§8✕", n3x + sz / 2, ny + sz / 2 - 4, t.locked.getColor());

        int lblY = ny + sz + 3;
        if (lblY + 8 < mockTop + mockH) {
            g.drawCenteredString(font, "§8Done",   n1x + sz / 2, lblY, t.textFaint.getColor());
            g.drawCenteredString(font, "§8Active", n2x + sz / 2, lblY, t.textFaint.getColor());
            g.drawCenteredString(font, "§8Locked", n3x + sz / 2, lblY, t.textFaint.getColor());
        }

        // Accent + text color swatches at bottom of mock
        int swY = mockTop + mockH - 7;
        if (swY > mockTop + hdrH + 4) {
            int sw = Math.max(4, (mockW - sideW - 10) / 5);
            int sx = mockX + sideW + 5;
            g.fill(sx,           swY, sx + sw,           swY + 5, t.accent.getColor());
            g.fill(sx + sw + 2,  swY, sx + sw * 2 + 2,  swY + 5, t.text.getColor());
            g.fill(sx + sw * 2 + 4, swY, sx + sw * 3 + 4, swY + 5, t.textDim.getColor());
            g.fill(sx + sw * 3 + 6, swY, sx + sw * 4 + 6, swY + 5, t.textFaint.getColor());
        }

        // ── Theme list ────────────────────────────────────────────────────────
        int listY = mockTop + mockH + 8;
        g.drawString(font, "§8Themes  §7(click to swap)", mockX, listY, C_FAINT, false);
        listY += 12;
        listRowsStartY = listY;

        List<String> vis = visibleThemes();
        int itemH = 14;
        int maxVis = Math.max(1, (height - listY - 6) / itemH);
        int maxScroll = Math.max(0, vis.size() - maxVis);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        for (int i = scrollOffset; i < vis.size() && listY + itemH <= height - 4; i++) {
            String name = vis.get(i);
            boolean sel = name.equals(ChroniclesTheme.getActiveName());
            boolean hov = mx >= mockX && mx <= mockX + mockW && my >= listY && my < listY + itemH;

            int rowBg = sel ? ((t.accent.getColor() & 0x00FFFFFF) | 0x33000000)
                    : hov ? ((t.border.getColor() & 0x00FFFFFF) | 0x22000000) : 0;
            if (rowBg != 0) g.fill(mockX, listY, mockX + mockW, listY + itemH, rowBg);
            drawBorder(g, mockX, listY, mockW, itemH, (t.border.getColor() & 0x00FFFFFF) | 0x44000000);

            int nameColor = sel ? t.accent.getColor() : hov ? t.text.getColor() : t.textDim.getColor();
            g.drawString(font, (sel ? "●" : "○") + " " + name, mockX + 6, listY + 3, nameColor, false);

            if (!ChroniclesTheme.isBuiltin(name)) {
                int dX = mockX + mockW - 14;
                boolean dHov = mx >= dX && mx <= dX + 12 && my >= listY && my < listY + itemH;
                g.drawString(font, "✕", dX + 1, listY + 3, dHov ? 0xFFFF5555 : 0x66FF5555, false);
            }

            listY += itemH;
        }
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int sbW = sidebarW();
        int canvasW = width - sbW;
        int mockW = Math.min(canvasW - 20, 360);
        int mockX = 10;
        int itemH = 14;

        List<String> vis = visibleThemes();
        int listY = listRowsStartY;

        for (int i = scrollOffset; i < vis.size() && listY + itemH <= height - 4; i++) {
            String name = vis.get(i);
            if (my >= listY && my < listY + itemH) {
                // Delete button (custom themes only)
                if (!ChroniclesTheme.isBuiltin(name)) {
                    int dX = mockX + mockW - 14;
                    if (mx >= dX && mx <= dX + 12) {
                        pendingDeletions.add(name.toUpperCase(Locale.ROOT));
                        if (name.equalsIgnoreCase(ChroniclesTheme.getActiveName()))
                            ChroniclesTheme.setCurrent("DARK");
                        confirmActive = false;
                        pendingAction = null;
                        lastTrackedName = null; // force snapshot reset on reinit
                        init();
                        return true;
                    }
                }
                // Swap theme
                if (mx >= mockX && mx <= mockX + mockW - 16) {
                    if (hasChanges()) {
                        if (!confirmActive || !name.equals(pendingAction)) {
                            confirmActive = true;
                            pendingAction = name;
                            return true;
                        }
                        restore(savedSnap);
                    }
                    ChroniclesTheme.setCurrent(name);
                    confirmActive = false;
                    pendingAction = null;
                    lastTrackedName = null;
                    init();
                    return true;
                }
            }
            listY += itemH;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int sbW = sidebarW();
        if (mx < width - sbW) {
            List<String> vis = visibleThemes();
            int itemH = 14;
            int maxVis = Math.max(1, (height - listRowsStartY - 6) / itemH);
            int maxScroll = Math.max(0, vis.size() - maxVis);
            scrollOffset = Mth.clamp(scrollOffset - (int) delta, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (hasControlDown()) {
            if (key == 90) { tryUndo(); return true; }  // Ctrl+Z
            if (key == 83) { save();    return true; }  // Ctrl+S
        }
        return super.keyPressed(key, scan, mods);
    }

    // ── Save / Undo ───────────────────────────────────────────────────────────

    private void save() {
        String name = nameInput != null ? nameInput.getValue().trim().toUpperCase(Locale.ROOT) : "";
        if (name.isEmpty()) return;
        pendingDeletions.remove(name);
        ChroniclesTheme copy = ChroniclesTheme.current().copy();
        ChroniclesTheme.saveCustomTheme(name, copy);
        ChroniclesTheme.setCurrent(name);
        savedSnap = makeSnap(ChroniclesTheme.current(), name);
        confirmActive = false;
        pendingAction = null;
        lastTrackedName = name;
        init();
    }

    private void pushUndo() {
        Snap cur = makeSnap(ChroniclesTheme.current(),
                nameInput != null ? nameInput.getValue() : ChroniclesTheme.getActiveName());
        if (undoStack.isEmpty() || !undoStack.peek().equals(cur)) {
            undoStack.push(cur);
            if (undoStack.size() > 50) undoStack.remove(0);
        }
    }

    private void tryUndo() {
        if (undoStack.isEmpty()) return;
        isUndoing = true;
        Snap prev = undoStack.pop();
        restore(prev);
        // Sync edit boxes back to restored values
        for (FieldEntry f : fields) {
            String v = fieldValue(prev, f.label);
            if (v != null) f.box.setValue(v.toUpperCase(Locale.ROOT));
        }
        if (nameInput != null) nameInput.setValue(prev.name());
        confirmActive = false;
        isUndoing = false;
    }

    private String fieldValue(Snap s, String label) {
        return switch (label) {
            case "BG"       -> s.bg();
            case "Panel"    -> s.panel();
            case "Header"   -> s.header();
            case "Border"   -> s.border();
            case "Accent"   -> s.accent();
            case "Text"     -> s.text();
            case "Text Dim" -> s.dim();
            case "Faint"    -> s.faint();
            case "Done"     -> s.done();
            case "Active"   -> s.active();
            case "Locked"   -> s.locked();
            default         -> null;
        };
    }

    private boolean hasChanges() {
        if (savedSnap == null) return false;
        return !savedSnap.equals(makeSnap(ChroniclesTheme.current(),
                nameInput != null ? nameInput.getValue() : ChroniclesTheme.getActiveName()));
    }

    private Snap makeSnap(ChroniclesTheme t, String name) {
        return new Snap(t.bg.hex, t.panel.hex, t.header.hex, t.border.hex, t.accent.hex,
                t.text.hex, t.textDim.hex, t.textFaint.hex, t.done.hex,
                t.activeColor.hex, t.locked.hex, name);
    }

    private void restore(Snap s) {
        ChroniclesTheme t = ChroniclesTheme.current();
        t.bg.set(s.bg());           t.panel.set(s.panel());
        t.header.set(s.header());   t.border.set(s.border());
        t.accent.set(s.accent());   t.text.set(s.text());
        t.textDim.set(s.dim());     t.textFaint.set(s.faint());
        t.done.set(s.done());       t.activeColor.set(s.active());
        t.locked.set(s.locked());
        syncPalette(t);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        for (String name : pendingDeletions) ChroniclesTheme.deleteCustom(name);
        pendingDeletions.clear();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(@NotNull GuiGraphics g) { /* themed canvas, not dirt */ }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int sidebarW() { return Math.max(SIDEBAR_MIN, width / 4); }

    private List<String> visibleThemes() {
        List<String> out = new ArrayList<>();
        for (String name : ChroniclesTheme.REGISTRY.keySet()) {
            if (!pendingDeletions.contains(name.toUpperCase(Locale.ROOT))) out.add(name);
        }
        return out;
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w,     y + 1,     c);
        g.fill(x,         y + h - 1, x + w,     y + h,     c);
        g.fill(x,         y,         x + 1,     y + h,     c);
        g.fill(x + w - 1, y,         x + w,     y + h,     c);
    }

    private void drawMockLine(GuiGraphics g, int x1, int y, int x2, int y2, int color) {
        // Simple horizontal line for the preview (real dep lines are bezier)
        for (int x = x1; x < x2; x++) g.fill(x, y - 1, x + 1, y + 2, color);
    }

    // ── Records ───────────────────────────────────────────────────────────────

    private record FieldEntry(String label, ChroniclesTheme.ThemeColor target, EditBox box) {}
    private record SectionLabel(String title, int x, int y) {}
}
