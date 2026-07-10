package net.phoenix.core.conflux.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.research.ResearchTree;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;

/**
 * In-game visual editor for Axiom research trees.
 *
 * Produces datapack-ready JSON that can be dropped into
 * {@code data/<namespace>/conflux/research/<id>.json}.
 *
 * Accessible via {@code /conflux edit} (op/creative only).
 */
@OnlyIn(Dist.CLIENT)
public class ResearchTreeEditorScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_W  = 240;
    private static final int TOOLBAR_H = 24;

    // ── Node geometry ─────────────────────────────────────────────────────────
    private static final int NODE_W   = 64;
    private static final int NODE_H   = 48;
    private static final int NODE_R   = 5;   // corner radius (approximated)
    private static final int GRID     = 80;  // canvas units → px at zoom 1.0

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xFF07070F;
    private static final int C_CANVAS    = 0xFF09090E;
    private static final int C_PANEL     = 0xFF0F0F1C;
    private static final int C_TOOLBAR   = 0xFF0C0C18;
    private static final int C_BORDER    = 0xFF252538;
    private static final int C_TEXT      = 0xFFCCCCEE;
    private static final int C_DIM       = 0xFF666688;
    private static final int C_ACCENT    = 0xFF5577FF;
    private static final int C_SELECTED  = 0xFFFFCC22;
    private static final int C_LINK_SRC  = 0xFF22CCFF;
    private static final int C_DELETE    = 0xFFCC3333;
    private static final int C_GRID      = 0x0AFFFFFF;

    // ── Mode ──────────────────────────────────────────────────────────────────
    private enum Mode { SELECT, PLACE, LINK }
    private Mode mode = Mode.SELECT;

    // ── Canvas state ──────────────────────────────────────────────────────────
    private float panX, panY, zoom = 1f;
    private boolean panDragging;
    private double lastDragX, lastDragY;

    // ── Editor state ──────────────────────────────────────────────────────────
    private final List<EditorNode>  nodes = new ArrayList<>();
    private final List<EditorEdge>  edges = new ArrayList<>();
    private EditorNode selected   = null;
    private EditorNode linkSource = null;   // pending LINK source
    private EditorNode hovering   = null;
    private boolean    nodeDragging = false;
    private double     nodeDragOffX, nodeDragOffY;
    private int        nodeIdCounter = 1;

    // ── Tree metadata ─────────────────────────────────────────────────────────
    private String treeName  = "my_tree";
    private String treeTitle = "New Tree";
    private String treeDesc  = "";

    // ── Property widgets ──────────────────────────────────────────────────────
    private EditBox wTreeName, wTreeTitle, wTreeDesc;
    private EditBox wNodeTitle, wNodeLore, wNodeIcon, wNodeExGroup;
    private final Map<ConfluxDataType, EditBox> wCosts = new EnumMap<>(ConfluxDataType.class);
    private final List<UnlockEntry>  unlockEntries = new ArrayList<>();

    // ── Unlock editor ─────────────────────────────────────────────────────────
    private EditBox wUnlockType, wUnlockValue;
    private record UnlockEntry(String type, String value) {}

    public ResearchTreeEditorScreen() {
        super(Component.literal("Axiom Tree Editor"));
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int px = canvasW() + 8;
        int py = TOOLBAR_H + 8;
        int fw = PANEL_W - 16;

        // Tree metadata
        wTreeName  = addBox(px, py,      fw, "tree name…");
        wTreeTitle = addBox(px, py + 18, fw, "display title…");
        wTreeDesc  = addBox(px, py + 36, fw, "description…");
        wTreeName.setValue(treeName); wTreeTitle.setValue(treeTitle); wTreeDesc.setValue(treeDesc);

        // Node properties (shown when node selected)
        int ny = py + 72;
        wNodeTitle  = addBox(px, ny,       fw, "node title…");
        wNodeLore   = addBox(px, ny + 18,  fw, "lore / flavour text…");
        wNodeIcon   = addBox(px, ny + 36,  fw, "icon item id…");
        wNodeExGroup = addBox(px, ny + 54, fw, "exclusion group…");

        // Cost boxes
        int cy = ny + 78;
        for (ConfluxDataType type : ConfluxDataType.values()) {
            EditBox b = addBox(px + 90, cy, fw - 90, "0");
            b.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
            wCosts.put(type, b);
            cy += 14;
        }

        // Unlock editor
        int uy = cy + 8;
        wUnlockType  = addBox(px,      uy, 60,       "recipe_tag");
        wUnlockValue = addBox(px + 64, uy, fw - 64,  "value…");

        refreshNodeWidgets();
    }

    private EditBox addBox(int x, int y, int w, String hint) {
        EditBox b = new EditBox(font, x, y, w, 12, Component.empty());
        b.setHint(Component.literal(hint));
        b.setMaxLength(256);
        addRenderableWidget(b);
        return b;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        updateTreeMeta();
        hovering = nodeAt(screenToCanvas(mx, my));

        g.fill(0, 0, width, height, C_BG);

        renderCanvas(g, mx, my);
        renderToolbar(g, mx, my);
        renderPanel(g, mx, my);

        super.render(g, mx, my, pt);
    }

    private void renderCanvas(GuiGraphics g, int mx, int my) {
        int cw = canvasW(), ch = canvasH();
        g.fill(0, TOOLBAR_H, cw, TOOLBAR_H + ch, C_CANVAS);

        // Grid
        g.pose().pushPose();
        g.pose().translate(panX, TOOLBAR_H + panY, 0);
        g.pose().scale(zoom, zoom, 1f);
        drawGrid(g, cw, ch);
        drawEdges(g, mx, my);
        drawNodes(g, mx, my);
        g.pose().popPose();

        // Canvas border
        g.fill(cw, TOOLBAR_H, cw + 1, height, C_BORDER);
    }

    private void drawGrid(GuiGraphics g, int cw, int ch) {
        int step = GRID;
        int startX = (int)((-panX / zoom) / step) * step - step;
        int startY = (int)((-panY / zoom) / step) * step - step;
        int endX   = startX + (int)(cw / zoom) + step * 2;
        int endY   = startY + (int)(ch / zoom) + step * 2;
        for (int x = startX; x < endX; x += step) g.fill(x, startY, x + 1, endY, C_GRID);
        for (int y = startY; y < endY; y += step) g.fill(startX, y, endX, y + 1, C_GRID);
    }

    private void drawEdges(GuiGraphics g, int mx, int my) {
        for (EditorEdge e : edges) {
            int x1 = e.from.x * GRID, y1 = e.from.y * GRID;
            int x2 = e.to.x   * GRID, y2 = e.to.y   * GRID;
            int col = dominantColor(e.from);
            drawBezier(g, x1, y1, x2, y2, col);
        }

        // Live preview line when linking
        if (mode == Mode.LINK && linkSource != null) {
            float[] c = screenToCanvas(mx, my);
            int x1 = linkSource.x * GRID, y1 = linkSource.y * GRID;
            drawBezier(g, x1, y1, (int)c[0], (int)c[1], 0xAA22CCFF);
        }
    }

    private void drawNodes(GuiGraphics g, int mx, int my) {
        for (EditorNode node : nodes) {
            boolean isSel  = node == selected;
            boolean isHov  = node == hovering && !isSel;
            boolean isLink = node == linkSource;
            drawNode(g, node, isSel, isHov, isLink);
        }
    }

    private void drawNode(GuiGraphics g, EditorNode node, boolean sel, boolean hov, boolean linkSrc) {
        int cx = node.x * GRID;
        int cy = node.y * GRID;
        int x  = cx - NODE_W / 2;
        int y  = cy - NODE_H / 2;

        int fill   = nodeFill(node);
        int border = sel ? C_SELECTED : linkSrc ? C_LINK_SRC : hov ? C_ACCENT : nodeBorder(node);

        // Glow layers (3 rings, decreasing alpha)
        if (sel || hov || linkSrc) {
            for (int i = 4; i >= 1; i--) {
                int alpha  = 0x18 * (5 - i);
                int glowC  = (alpha << 24) | (border & 0x00FFFFFF);
                drawRoundRect(g, x - i, y - i, NODE_W + i*2, NODE_H + i*2, NODE_R, 0, glowC);
            }
        }

        // Node body + border
        drawRoundRect(g, x, y, NODE_W, NODE_H, NODE_R, fill, border);

        // Item icon (16×16 centered)
        if (!node.icon.isEmpty()) {
            try {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(new ResourceLocation(node.icon));
                if (item != net.minecraft.world.item.Items.AIR) {
                    g.renderItem(new net.minecraft.world.item.ItemStack(item), cx - 8, cy - 12);
                }
            } catch (Exception ignored) {}
        }

        // Title below icon
        String label = node.title.isEmpty() ? node.id : node.title;
        if (label.length() > 9) label = label.substring(0, 8) + "…";
        g.drawCenteredString(font, label, cx, cy + 6, sel ? C_SELECTED : C_TEXT);

        // Root badge
        if (edges.stream().noneMatch(e -> e.to == node)) {
            g.drawString(font, "★", x + 2, y + 2, C_SELECTED, false);
        }
        // Exclusion group badge
        if (!node.exclusionGroup.isEmpty()) {
            g.drawString(font, "⊗", x + NODE_W - 9, y + 2, 0xFFFF6644, false);
        }
    }

    private void renderToolbar(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, width, TOOLBAR_H, C_TOOLBAR);
        g.fill(0, TOOLBAR_H - 1, width, TOOLBAR_H, C_BORDER);

        int x = 6;
        x = drawToolBtn(g, mx, my, x, "SELECT",  mode == Mode.SELECT,  0xFF334455);
        x = drawToolBtn(g, mx, my, x, "PLACE",   mode == Mode.PLACE,   0xFF334433);
        x = drawToolBtn(g, mx, my, x, "LINK",    mode == Mode.LINK,    0xFF333355);
        x += 8;
        g.fill(x, 3, x + 1, TOOLBAR_H - 3, C_BORDER); x += 9;
        x = drawToolBtn(g, mx, my, x, "UNDO",    false, 0xFF333333);
        x += 8;
        g.fill(x, 3, x + 1, TOOLBAR_H - 3, C_BORDER); x += 9;
        x = drawToolBtn(g, mx, my, x, "COPY JSON",   false, 0xFF333344);
        x = drawToolBtn(g, mx, my, x, "SAVE FILE",   false, 0xFF223322);
        x = drawToolBtn(g, mx, my, x, "LOAD TREE",   false, 0xFF332233);

        // Node count — right-aligned inside the side panel
        String countStr = "§7" + nodes.size() + " nodes  " + edges.size() + " edges";
        g.drawString(font, countStr, width - font.width(countStr) - 8, 8, 0xFFFFFF, false);
    }

    private int drawToolBtn(GuiGraphics g, int mx, int my, int x, String label, boolean active, int bg) {
        int w = font.width(label) + 10;
        boolean hov = mx >= x && mx < x + w && my >= 2 && my < TOOLBAR_H - 2;
        g.fill(x, 2, x + w, TOOLBAR_H - 2, active ? C_ACCENT : hov ? bg + 0x101010 : bg);
        if (active) g.fill(x, TOOLBAR_H - 3, x + w, TOOLBAR_H - 2, 0xFFFFFFFF);
        g.drawString(font, active || hov ? "§f" + label : "§7" + label, x + 5, 8, 0xFFFFFF, false);
        return x + w + 4;
    }

    private void renderPanel(GuiGraphics g, int mx, int my) {
        int px = canvasW();
        g.fill(px, 0, width, height, C_PANEL);

        int x = px + 8, y = TOOLBAR_H + 8;

        // Tree metadata section
        g.drawString(font, "§bTree", x, y - 2, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "§7ID",    x, y + 2, 0xFFFFFF, false);
        y += 14; // wTreeName
        g.drawString(font, "§7Title", x, y + 2, 0xFFFFFF, false);
        y += 14; // wTreeTitle
        g.drawString(font, "§7Desc",  x, y + 2, 0xFFFFFF, false);
        y += 18;

        g.fill(px + 4, y, width - 4, y + 1, C_BORDER); y += 6;

        if (selected == null) {
            g.drawCenteredString(font, "§7Click a node to edit", px + PANEL_W / 2, y + 20, 0xFFFFFF);
            renderUnlockEditor(g, mx, my, px + 8, height - 60);
            return;
        }

        // Node properties
        g.drawString(font, "§eNode: §f" + selected.id, x, y, 0xFFFFFF, false); y += 12;
        g.drawString(font, "§7Title",  x, y + 2, 0xFFFFFF, false); y += 14;
        g.drawString(font, "§7Lore",   x, y + 2, 0xFFFFFF, false); y += 14;
        g.drawString(font, "§7Icon",   x, y + 2, 0xFFFFFF, false); y += 14;
        g.drawString(font, "§7Group",  x, y + 2, 0xFFFFFF, false); y += 18;

        g.fill(px + 4, y, width - 4, y + 1, C_BORDER); y += 6;

        // Costs
        g.drawString(font, "§7Cost:", x, y, 0xFFFFFF, false); y += 12;
        for (ConfluxDataType type : ConfluxDataType.values()) {
            if (!type.isAvailable()) continue;
            g.drawString(font, type.displayComponent(), x, y + 2, 0xFFFFFF, false);
            y += 14;
        }

        g.fill(px + 4, y, width - 4, y + 1, C_BORDER); y += 6;

        // Unlocks list
        g.drawString(font, "§7Unlocks:", x, y, 0xFFFFFF, false); y += 12;
        for (int i = 0; i < unlockEntries.size(); i++) {
            UnlockEntry ue = unlockEntries.get(i);
            g.drawString(font, "§b" + ue.type() + " §f" + ue.value(), x, y, 0xFFFFFF, false);
            boolean hov = mx >= width - 16 && mx < width - 4 && my >= y && my < y + 9;
            g.drawString(font, hov ? "§c[×]" : "§8[×]", width - 16, y, 0xFFFFFF, false);
            y += 10;
        }

        renderUnlockEditor(g, mx, my, x, y);

        // Delete node button
        int btnY = height - 22;
        boolean hov = mx >= x && mx < x + 100 && my >= btnY && my < btnY + 14;
        g.fill(x, btnY, x + 100, btnY + 14, hov ? 0xFF551111 : 0xFF330A0A);
        g.renderOutline(x, btnY, 100, 14, C_DELETE);
        g.drawCenteredString(font, "§cDelete Node", x + 50, btnY + 3, 0xFFFFFF);
    }

    private void renderUnlockEditor(GuiGraphics g, int mx, int my, int x, int y) {
        g.drawString(font, "§7+ Unlock:", x, y + 2, 0xFFFFFF, false); y += 14;
        // EditBoxes positioned by init() — just draw the "Add" button
        int addX = x + 4, addY = y + 14;
        if (wUnlockType != null) { wUnlockType.setX(x); wUnlockType.setY(addY - 14); }
        if (wUnlockValue != null) { wUnlockValue.setX(x + 64); wUnlockValue.setY(addY - 14); }
        boolean hov = mx >= addX && mx < addX + 40 && my >= addY && my < addY + 12;
        g.fill(addX, addY, addX + 40, addY + 12, hov ? 0xFF224422 : 0xFF112211);
        g.renderOutline(addX, addY, 40, 12, 0xFF336633);
        g.drawCenteredString(font, "§aAdd", addX + 20, addY + 2, 0xFFFFFF);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        // Toolbar
        if (my < TOOLBAR_H) {
            handleToolbarClick((int)mx, (int)my, btn);
            return true;
        }

        // Panel
        if (mx >= canvasW()) {
            handlePanelClick((int)mx, (int)my, btn);
            return true;
        }

        // Canvas
        float[] pos  = screenToCanvas(mx, my);
        EditorNode hit = nodeAt(pos);

        if (btn == 2 || (btn == 0 && hasAltDown())) {
            // Middle / Alt+left = start pan from anywhere
            panDragging = true; lastDragX = mx; lastDragY = my;
            return true;
        }
        if (btn == 1 && nodeAt(screenToCanvas(mx, my)) == null) {
            // Right-click on empty canvas = pan
            panDragging = true; lastDragX = mx; lastDragY = my;
            return true;
        }

        if (btn == 0) {
            switch (mode) {
                case SELECT -> {
                    if (hit != null) {
                        selected = hit;
                        nodeDragging = true;
                        nodeDragOffX = pos[0] - hit.x * GRID;
                        nodeDragOffY = pos[1] - hit.y * GRID;
                        refreshNodeWidgets();
                    } else {
                        selected = null;
                        refreshNodeWidgets();
                        panDragging = true; lastDragX = mx; lastDragY = my;
                    }
                }
                case PLACE -> {
                    int gx = Math.round(pos[0] / GRID);
                    int gy = Math.round(pos[1] / GRID);
                    EditorNode n = new EditorNode("node_" + nodeIdCounter++, gx, gy);
                    nodes.add(n);
                    selected = n;
                    refreshNodeWidgets();
                    mode = Mode.SELECT;
                }
                case LINK -> {
                    if (hit != null) {
                        if (linkSource == null) {
                            linkSource = hit;
                        } else if (hit != linkSource) {
                            // Add edge: linkSource is prerequisite of hit
                            if (edges.stream().noneMatch(e -> e.from == linkSource && e.to == hit)) {
                                edges.add(new EditorEdge(linkSource, hit));
                            }
                            linkSource = null;
                            mode = Mode.SELECT;
                        }
                    } else {
                        linkSource = null;
                    }
                }
            }
            return true;
        }

        if (btn == 1 && hit != null) {
            // Right-click node = delete edge from selected to this
            if (selected != null) {
                edges.removeIf(e -> (e.from == selected && e.to == hit) || (e.from == hit && e.to == selected));
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) { nodeDragging = false; panDragging = false; }
        if (btn == 1 || btn == 2) panDragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (panDragging) {
            panX += (float)(mx - lastDragX);
            panY += (float)(my - lastDragY);
            lastDragX = mx; lastDragY = my;
            return true;
        }
        if (nodeDragging && selected != null) {
            float[] pos = screenToCanvas(mx, my);
            selected.x = Math.round((float)(pos[0] - nodeDragOffX) / GRID);
            selected.y = Math.round((float)(pos[1] - nodeDragOffY) / GRID);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < canvasW()) {
            float old = zoom;
            zoom = Math.max(0.3f, Math.min(3f, zoom + (float)(delta * 0.1f)));
            float f = zoom / old;
            panX = (float)(mx - f * (mx - panX));
            panY = (float)((my - TOOLBAR_H) - f * ((my - TOOLBAR_H) - panY));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (mode != Mode.SELECT || linkSource != null) {
                mode = Mode.SELECT; linkSource = null;
            } else {
                onClose();
            }
            return true;
        }
        if (kc == GLFW.GLFW_KEY_DELETE && selected != null) {
            deleteSelected(); return true;
        }
        // Mode shortcuts
        if (kc == GLFW.GLFW_KEY_S && !hasControlDown()) { mode = Mode.SELECT; return true; }
        if (kc == GLFW.GLFW_KEY_P)                      { mode = Mode.PLACE;  return true; }
        if (kc == GLFW.GLFW_KEY_L)                      { mode = Mode.LINK;   return true; }
        return super.keyPressed(kc, sc, mod);
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private void handleToolbarClick(int mx, int my, int btn) {
        int x = 6;
        if (hitBtn(mx, my, x, "SELECT"))  { mode = Mode.SELECT; linkSource = null; return; }
        x += font.width("SELECT") + 14;
        if (hitBtn(mx, my, x, "PLACE"))   { mode = Mode.PLACE;  return; }
        x += font.width("PLACE") + 14;
        if (hitBtn(mx, my, x, "LINK"))    { mode = Mode.LINK;   linkSource = null; return; }
        x += font.width("LINK") + 14 + 18;
        if (hitBtn(mx, my, x, "UNDO"))    { doUndo(); return; }
        x += font.width("UNDO") + 14 + 18;
        if (hitBtn(mx, my, x, "COPY JSON"))  { copyJson(); return; }
        x += font.width("COPY JSON") + 14;
        if (hitBtn(mx, my, x, "SAVE FILE")) { saveFile(); return; }
        x += font.width("SAVE FILE") + 14;
        if (hitBtn(mx, my, x, "LOAD TREE")) { showLoadDialog(); }
    }

    private boolean hitBtn(int mx, int my, int x, String label) {
        int w = font.width(label) + 10;
        return mx >= x && mx < x + w && my >= 2 && my < TOOLBAR_H - 2;
    }

    private void handlePanelClick(int mx, int my, int btn) {
        if (selected == null) return;
        // Unlock "Add" button
        int ux = canvasW() + 8 + 4;
        int uy = unlockAddButtonY();
        if (mx >= ux && mx < ux + 40 && my >= uy && my < uy + 12) {
            addUnlock(); return;
        }
        // Unlock remove buttons
        int lx = width - 16;
        int ly = unlockListStartY();
        for (int i = 0; i < unlockEntries.size(); i++) {
            if (mx >= lx && mx < width - 4 && my >= ly && my < ly + 9) {
                unlockEntries.remove(i); syncNodeUnlocks(); return;
            }
            ly += 10;
        }
        // Delete node button
        int bx = canvasW() + 8, by = height - 22;
        if (mx >= bx && mx < bx + 100 && my >= by && my < by + 14) {
            deleteSelected();
        }
    }

    // ── Node actions ──────────────────────────────────────────────────────────

    private void deleteSelected() {
        if (selected == null) return;
        nodes.remove(selected);
        edges.removeIf(e -> e.from == selected || e.to == selected);
        selected = null;
        refreshNodeWidgets();
    }

    private void addUnlock() {
        if (wUnlockType == null || wUnlockValue == null) return;
        String type  = wUnlockType.getValue().trim();
        String value = wUnlockValue.getValue().trim();
        if (type.isEmpty() || value.isEmpty()) return;
        unlockEntries.add(new UnlockEntry(type, value));
        wUnlockType.setValue(""); wUnlockValue.setValue("");
        syncNodeUnlocks();
    }

    private void syncNodeUnlocks() {
        if (selected == null) return;
        selected.unlocks.clear();
        unlockEntries.forEach(u -> selected.unlocks.add(u));
    }

    private void doUndo() {
        if (!nodes.isEmpty()) {
            EditorNode last = nodes.remove(nodes.size() - 1);
            edges.removeIf(e -> e.from == last || e.to == last);
            if (selected == last) { selected = null; refreshNodeWidgets(); }
        }
    }

    // ── Widget sync ───────────────────────────────────────────────────────────

    private void refreshNodeWidgets() {
        boolean has = selected != null;
        wNodeTitle.setValue(has ? selected.title : "");
        wNodeLore.setValue(has  ? selected.lore  : "");
        wNodeIcon.setValue(has  ? selected.icon  : "");
        wNodeExGroup.setValue(has ? selected.exclusionGroup : "");
        for (ConfluxDataType type : ConfluxDataType.values()) {
            long v = has ? selected.cost.getOrDefault(type, 0L) : 0L;
            wCosts.get(type).setValue(v == 0 ? "" : String.valueOf(v));
        }
        unlockEntries.clear();
        if (has) unlockEntries.addAll(selected.unlocks);

        wNodeTitle.setEditable(has); wNodeLore.setEditable(has);
        wNodeIcon.setEditable(has);  wNodeExGroup.setEditable(has);
        wCosts.values().forEach(b -> b.setEditable(has));
    }

    private void updateTreeMeta() {
        treeName  = wTreeName.getValue().trim().isEmpty()  ? treeName  : wTreeName.getValue().trim();
        treeTitle = wTreeTitle.getValue().trim().isEmpty() ? treeTitle : wTreeTitle.getValue().trim();
        treeDesc  = wTreeDesc.getValue();
        if (selected != null) {
            selected.title          = wNodeTitle.getValue();
            selected.lore           = wNodeLore.getValue();
            selected.icon           = wNodeIcon.getValue().trim();
            selected.exclusionGroup = wNodeExGroup.getValue().trim();
            for (ConfluxDataType type : ConfluxDataType.values()) {
                String raw = wCosts.get(type).getValue().trim();
                if (raw.isEmpty()) selected.cost.remove(type);
                else try { selected.cost.put(type, Long.parseLong(raw)); } catch (NumberFormatException ignored) {}
            }
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private String buildJson() {
        JsonObject root = new JsonObject();
        root.addProperty("title",       treeTitle);
        root.addProperty("description", treeDesc);
        JsonArray nodesArr = new JsonArray();
        for (EditorNode n : nodes) {
            JsonObject nj = new JsonObject();
            nj.addProperty("id",    "phoenixcore:" + n.id);
            nj.addProperty("title", n.title.isEmpty() ? n.id : n.title);
            if (!n.lore.isEmpty())          nj.addProperty("lore", n.lore);
            if (!n.icon.isEmpty())          nj.addProperty("icon", n.icon);
            if (!n.exclusionGroup.isEmpty()) nj.addProperty("exclusion_group", n.exclusionGroup);
            JsonArray pos = new JsonArray(); pos.add(n.x); pos.add(n.y);
            nj.add("position", pos);
            // Prerequisites
            JsonArray prereqs = new JsonArray();
            edges.stream().filter(e -> e.to == n)
                    .forEach(e -> prereqs.add("phoenixcore:" + e.from.id));
            if (prereqs.size() > 0) nj.add("prerequisites", prereqs);
            // Cost
            if (!n.cost.isEmpty()) {
                JsonObject cost = new JsonObject();
                n.cost.forEach((t, v) -> cost.addProperty(t.id(), v));
                nj.add("cost", cost);
            }
            // Unlocks
            if (!n.unlocks.isEmpty()) {
                JsonArray ul = new JsonArray();
                n.unlocks.forEach(u -> { JsonObject uj = new JsonObject();
                    uj.addProperty("type", u.type()); uj.addProperty("value", u.value()); ul.add(uj); });
                nj.add("unlocks", ul);
            }
            nodesArr.add(nj);
        }
        root.add("nodes", nodesArr);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private void copyJson() {
        Minecraft.getInstance().keyboardHandler.setClipboard(buildJson());
    }

    private void saveFile() {
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("axiom_trees");
            dir.toFile().mkdirs();
            File out = dir.resolve(treeName + ".json").toFile();
            try (FileWriter fw = new FileWriter(out)) { fw.write(buildJson()); }
        } catch (Exception e) {
            // Silent — editor is a dev tool, failures visible in log
            e.printStackTrace();
        }
    }

    private void showLoadDialog() {
        for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            // Load first tree found as a quick demo — a proper picker can come later
            treeName  = tree.id.getPath();
            treeTitle = tree.title;
            treeDesc  = tree.description;
            nodes.clear(); edges.clear();
            Map<String, EditorNode> byId = new HashMap<>();
            tree.getNodes().forEach(n -> {
                EditorNode en = new EditorNode(n.id.getPath(), n.posX, n.posY);
                en.title = n.title; en.lore = n.lore; en.icon = n.icon;
                en.exclusionGroup = n.exclusionGroup == null ? "" : n.exclusionGroup;
                en.cost.putAll(n.cost);
                n.unlocks.forEach(u -> en.unlocks.add(new UnlockEntry(u.type(), u.value())));
                nodes.add(en); byId.put(n.id.toString(), en);
            });
            tree.getNodes().forEach(n -> n.prerequisites.forEach(pid -> {
                EditorNode from = byId.get(pid.toString());
                EditorNode to   = byId.get(n.id.toString());
                if (from != null && to != null) edges.add(new EditorEdge(from, to));
            }));
            wTreeName.setValue(treeName); wTreeTitle.setValue(treeTitle); wTreeDesc.setValue(treeDesc);
            break;
        }
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private void drawRoundRect(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        if (fill != 0) {
            g.fill(x + r, y,     x + w - r, y + h,     fill);
            g.fill(x,     y + r, x + r,     y + h - r, fill);
            g.fill(x + w - r, y + r, x + w, y + h - r, fill);
            // corner fills
            g.fill(x + r - 1, y + 1,         x + r,         y + r,     fill);
            g.fill(x + w - r, y + 1,         x + w - r + 1, y + r,     fill);
            g.fill(x + r - 1, y + h - r,     x + r,         y + h - 1, fill);
            g.fill(x + w - r, y + h - r,     x + w - r + 1, y + h - 1, fill);
        }
        if (border != 0) {
            g.fill(x + r, y,         x + w - r, y + 1,     border);
            g.fill(x + r, y + h - 1, x + w - r, y + h,     border);
            g.fill(x,     y + r,     x + 1,     y + h - r, border);
            g.fill(x + w - 1, y + r, x + w,     y + h - r, border);
        }
    }

    private void drawBezier(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float cx1 = x1 + (x2 - x1) * 0.4f;
        float cx2 = x2 - (x2 - x1) * 0.4f;
        float px = x1, py = y1;
        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            float t  = i / (float)steps;
            float nt = 1 - t;
            float nx = nt*nt*nt*x1 + 3*nt*nt*t*cx1 + 3*nt*t*t*cx2 + t*t*t*x2;
            float ny = nt*nt*nt*y1 + 3*nt*nt*t*y1  + 3*nt*t*t*y2  + t*t*t*y2;
            g.fill((int)Math.min(px,nx), (int)Math.min(py,ny),
                   (int)Math.max(px,nx)+1, (int)Math.max(py,ny)+1, color);
            px = nx; py = ny;
        }
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private float[] screenToCanvas(double sx, double sy) {
        return new float[]{ (float)((sx - panX) / zoom), (float)((sy - TOOLBAR_H - panY) / zoom) };
    }

    private EditorNode nodeAt(float[] pos) {
        float px = pos[0], py = pos[1];
        for (EditorNode n : nodes) {
            float nx = n.x * GRID, ny = n.y * GRID;
            if (Math.abs(px - nx) <= NODE_W / 2f && Math.abs(py - ny) <= NODE_H / 2f) return n;
        }
        return null;
    }

    private int canvasW() { return width - PANEL_W; }
    private int canvasH() { return height - TOOLBAR_H; }

    // ── Color helpers ─────────────────────────────────────────────────────────

    private int dominantColor(EditorNode n) {
        return n.cost.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> typeRGB(e.getKey())).orElse(C_BORDER);
    }

    private int nodeFill(EditorNode n) {
        int base = dominantColor(n);
        return blendTowards(base, C_CANVAS, 0.75f);
    }

    private int nodeBorder(EditorNode n) { return dominantColor(n); }

    private int typeRGB(ConfluxDataType t) {
        return switch (t) {
            case MATERIAL      -> 0xFFCC8800;
            case BIOLOGICAL    -> 0xFF22AA44;
            case ENERGETIC     -> 0xFF2299CC;
            case COMPUTATIONAL -> 0xFF9933CC;
            case ARCANE        -> 0xFF880099;
        };
    }

    private int blendTowards(int color, int target, float factor) {
        int ar = (color >> 16) & 0xFF, ag = (color >> 8) & 0xFF, ab = color & 0xFF;
        int br = (target >> 16) & 0xFF, bg = (target >> 8) & 0xFF, bb = target & 0xFF;
        int r = (int)(ar + (br - ar) * factor);
        int g = (int)(ag + (bg - ag) * factor);
        int b = (int)(ab + (bb - ab) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ── Layout helpers for panel Y positions ─────────────────────────────────

    private int unlockListStartY() {
        // approximate — used for hit testing
        return TOOLBAR_H + 8 + 10 + 14 + 14 + 14 + 14 + 6 + 12 + 14 + 14 + 14 + 18 + 6 + 12 + (ConfluxDataType.values().length * 14) + 6 + 12;
    }

    private int unlockAddButtonY() { return unlockListStartY() + unlockEntries.size() * 10 + 14; }

    @Override public boolean isPauseScreen() { return false; }

    // ── Editor data classes ───────────────────────────────────────────────────

    private static class EditorNode {
        String id, title = "", lore = "", icon = "", exclusionGroup = "";
        int x, y;
        final Map<ConfluxDataType, Long> cost    = new EnumMap<>(ConfluxDataType.class);
        final List<UnlockEntry>        unlocks = new ArrayList<>();

        EditorNode(String id, int x, int y) { this.id = id; this.x = x; this.y = y; }
    }

    private record EditorEdge(EditorNode from, EditorNode to) {}
}
