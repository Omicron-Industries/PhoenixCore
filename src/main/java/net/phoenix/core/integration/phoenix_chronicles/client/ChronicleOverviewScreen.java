package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.capability.PlayerQuestData;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ChronicleOverviewScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 130;
    private static final int DETAIL_W = 180;
    private static final int HEADER_H = 38;  // two rows: title bar (22) + search bar (16)
    private static final int NODE_SIZE = 32;   // base size — scaled by zoom at render time

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF0B0B0F;
    private static final int C_PANEL = 0xFF14141A;
    private static final int C_PANEL_DARK = 0xFF0E0E12;
    private static final int C_HEADER = 0xFF09090D;
    private static final int C_BORDER = 0xFF252530;
    private static final int C_BORDER_LIT = 0xFF353548;
    private static final int C_SEL_TAB = 0xFF1A1A26;
    private static final int C_SEL_ACCENT = 0xFF00AA55;
    private static final int C_LINE_LOCKED = 0x38FFFFFF;
    private static final int C_LINE_DONE = 0x9900CC66;
    private static final int C_LINE_ACTIVE = 0x88FFAA00;
    private static final int C_NODE_LOCKED = 0xFF1A1A24;
    private static final int C_NODE_UNLOCKED = 0xFF1E1E2C;
    private static final int C_NODE_ACTIVE = 0xFF221C00;
    private static final int C_NODE_DONE = 0xFF081A0E;
    private static final int C_NBORD_LOCKED = 0xFF2E2E40;
    private static final int C_NBORD_UNLOCKED = 0xFF4A4A60;
    private static final int C_NBORD_ACTIVE = 0xFFCC9900;
    private static final int C_NBORD_DONE = 0xFF00BB66;
    private static final int C_NBORD_SEL = 0xFF6688FF;
    private static final int C_NBORD_DEV = 0xFF8844AA;
    private static final int C_TEXT = 0xFFD8D8E4;
    private static final int C_TEXT_DIM = 0xFF7A7A8A;
    private static final int C_TEXT_FAINT = 0xFF404050;
    private static final int C_TEXT_DONE = 0xFF44CC88;
    private static final int C_TEXT_ACT = 0xFFFFBB33;
    private static final int C_DOT = 0x14FFFFFF;
    private static final int C_CTX_BG = 0xFF1A1A22;
    private static final int C_CTX_HOVER = 0xFF252532;
    private static final int C_CTX_BORDER = 0xFF8844AA;
    private static final int C_CTX_SEP = 0xFF2A2A38;
    private static final int C_CTX_TEXT = 0xFFCCCCD8;
    private static final int C_CTX_DANGER = 0xFFCC4444;
    private static final int C_PROG_BG = 0xFF1A1A22;
    private static final int C_PROG_FILL = 0xFF00AA55;
    private static final int C_PROG_ACT = 0xFFBB8800;

    // ── State ─────────────────────────────────────────────────────────────────
    private String selectedCategory = "ALL";
    private QuestNode selectedNode = null;
    private boolean isDevMode = false;
    private String feedbackMsg = "";
    private int feedbackTimer = 0;

    // ── Panning & zoom ────────────────────────────────────────────────────────
    private int viewOffX = 0, viewOffY = 0;
    private float zoom = 1.0f;
    private static final float ZOOM_MIN = 0.35f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.12f;
    private boolean isPanning = false;

    // ── Dev drag ──────────────────────────────────────────────────────────────
    private QuestNode draggedNode = null;
    private int dragGrabX = 0, dragGrabY = 0;

    // ── Context menu (pure-render, no hidden buttons) ─────────────────────────
    private static final int CTX_ROW = 16;
    private static final int CTX_SEP = 5;
    private static final int CTX_W = 128;
    private boolean ctxOpen = false;
    private int ctxX, ctxY;
    private QuestNode ctxNode = null;
    private boolean ctxMoveCatOpen = false;

    // ── New-category inline form ───────────────────────────────────────────────
    private boolean newCatFormOpen = false;
    private EditBox newCatBox = null;

    // ── Search + state filter ─────────────────────────────────────────────────
    private String searchQuery = "";
    private String stateFilter = "ALL"; // ALL | AVAILABLE | ACTIVE | COMPLETE | LOCKED
    private EditBox searchBox = null;

    // ── Canvas caches ─────────────────────────────────────────────────────────
    private final Map<ResourceLocation, int[]> nodeScreenPos = new LinkedHashMap<>();
    private final Map<ResourceLocation, Button> nodeButtons = new LinkedHashMap<>();
    private final List<int[]> lineCache = new ArrayList<>();

    // ── Detail panel ──────────────────────────────────────────────────────────
    private FullQuestData detailData = null;
    private PlayerQuestData playerData = null;

    public ChronicleOverviewScreen() {
        super(Component.literal("Chronicle"));
    }

    // ── Capability helpers ────────────────────────────────────────────────────

    private QuestState getState(QuestNode node) {
        if (playerData == null) return QuestState.LOCKED;
        return playerData.getQuestState(node.getId(), QuestState.LOCKED);
    }

    private boolean isTaskDone(QuestTask task) {
        if (minecraft == null || minecraft.player == null) return false;
        return task.isCompletedFor(minecraft.player);
    }

    // ── Category persistence ──────────────────────────────────────────────────

    /**
     * Returns the path of the flat file that stores stub category names
     * (categories that exist but have no quests in them yet).
     */
    private Path categoriesFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles").resolve("categories.txt");
    }

    /**
     * Loads stub categories from disk and merges them with whatever categories
     * are already present in the registry (from quests).
     */
    private List<String> buildCategoryList() {
        List<String> cats = new ArrayList<>();
        cats.add("ALL");

        // Categories derived from actual quests
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            String c = n.getCategory();
            if (c != null && !cats.contains(c)) cats.add(c);
        }

        // Stub categories persisted to disk (empty categories the dev created)
        try {
            Path f = categoriesFile();
            if (Files.exists(f)) {
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    String cat = line.trim().toUpperCase();
                    if (!cat.isEmpty() && !cats.contains(cat)) cats.add(cat);
                }
            }
        } catch (IOException ignored) {}

        return cats;
    }

    /** Persists the current set of stub categories (those with no quests) to disk. */
    private void saveStubCategories(List<String> fullCatList) {
        try {
            Path f = categoriesFile();
            Files.createDirectories(f.getParent());
            // Only write categories that have NO quests — quest-backed ones reload naturally
            Set<String> questCats = new HashSet<>();
            questCats.add("ALL");
            for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
                if (n.getCategory() != null) questCats.add(n.getCategory());
            }
            List<String> stubs = new ArrayList<>();
            for (String c : fullCatList) {
                if (!questCats.contains(c)) stubs.add(c);
            }
            Files.writeString(f, String.join("\n", stubs), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Init / rebuild ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        nodeScreenPos.clear();
        nodeButtons.clear();
        lineCache.clear();
        ctxOpen = false;
        ctxMoveCatOpen = false;
        newCatBox = null;

        if (minecraft != null && minecraft.player != null) {
            isDevMode = minecraft.player.isCreative() || minecraft.player.hasPermissions(2);
            playerData = minecraft.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).orElse(null);
        }

        int cl = SIDEBAR_W, cr = width - DETAIL_W;

        // ── Sidebar category tabs ─────────────────────────────────────────────
        List<String> cats = buildCategoryList();
        int tabY = HEADER_H + 18;
        for (String cat : cats) {
            boolean sel = cat.equals(selectedCategory);
            addRenderableWidget(Button.builder(
                    Component.literal(sel ? "§f" + friendly(cat) : "§8" + friendly(cat)),
                    b -> {
                        selectedCategory = cat;
                        selectedNode = null;
                        detailData = null;
                        viewOffX = 0;
                        viewOffY = 0;
                        ctxOpen = false;
                        ctxMoveCatOpen = false;
                        rebuild();
                    }).bounds(2, tabY, SIDEBAR_W - 4, 16).build());
            tabY += 18;
        }

        // "New category" button + form (dev only)
        if (isDevMode) {
            addRenderableWidget(Button.builder(
                    Component.literal(newCatFormOpen ? "§8– Cancel" : "§a+ Category"),
                    b -> {
                        newCatFormOpen = !newCatFormOpen;
                        rebuild();
                    }).bounds(4, height - (newCatFormOpen ? 38 : 22), SIDEBAR_W - 8, 14).build());

            if (newCatFormOpen) {
                newCatBox = new EditBox(font, 4, height - 22, SIDEBAR_W - 8, 14, Component.empty());
                newCatBox.setHint(Component.literal("§8Name, press Enter"));
                newCatBox.setMaxLength(32);
                addRenderableWidget(newCatBox);
            }
        }

        // ── Search box (canvas header, row 2) ────────────────────────────────
        int searchX = cl + 4;
        int searchW = (cr - cl) / 2 - 6;
        searchBox = new EditBox(font, searchX, 23, searchW, 13, Component.empty());
        searchBox.setHint(Component.literal("§8Search quests…"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(v -> {
            searchQuery = v;
            rebuild();
        });
        addRenderableWidget(searchBox);

        // ── State filter tabs (row 2, right side of header) ───────────────────
        String[] filterLabels = { "ALL", "AVAILABLE", "ACTIVE", "COMPLETE", "LOCKED" };
        String[] filterKeys = { "ALL", "AVAILABLE", "ACTIVE", "COMPLETE", "LOCKED" };
        int filterX = searchX + searchW + 6;
        int filterW = (cr - filterX - 4) / filterLabels.length;
        for (int fi = 0; fi < filterLabels.length; fi++) {
            String key = filterKeys[fi];
            String label = filterLabels[fi].charAt(0) + filterLabels[fi].substring(1).toLowerCase();
            boolean sel = stateFilter.equals(key);
            addRenderableWidget(Button.builder(
                    Component.literal(sel ? "§f" + label : "§8" + label),
                    b -> {
                        stateFilter = key;
                        selectedNode = null;
                        detailData = null;
                        rebuild();
                    }).bounds(filterX + fi * filterW, 23, filterW - 1, 13).build());
        }

        // ── Quest node buttons ────────────────────────────────────────────────
        for (QuestNode root : QuestTreeRegistry.getRootChapters().values()) {
            if (!catMatches(root)) continue;
            placeNodeRecursive(root, cl, cr);
        }
        buildLineCache();
    }

    // ── Node placement (zoom-aware) ───────────────────────────────────────────

    private void placeNodeRecursive(QuestNode node, int cl, int cr) {
        int cx = node.getCustomX() != 0 ? node.getCustomX() : 20;
        int cy = node.getCustomY() != 0 ? node.getCustomY() : 40;

        int sz = scaledNodeSize();
        int sx = (int) (cx * zoom) + viewOffX + cl;
        int sy = (int) (cy * zoom) + viewOffY + HEADER_H;

        boolean offCanvas = sx < cl - sz - 2 || sx > cr + 2 || sy < HEADER_H - sz - 2 || sy > height + 2;

        QuestState state = getState(node);
        Button btn = Button.builder(Component.empty(), b -> onNodeClicked(node))
                .bounds(sx, sy, sz, sz).build();
        btn.setAlpha(0f);
        btn.visible = !offCanvas;
        if (state == QuestState.LOCKED && !isDevMode) btn.active = false;
        addRenderableWidget(btn);
        nodeButtons.put(node.getId(), btn);
        nodeScreenPos.put(node.getId(), new int[] { sx, sy });

        for (QuestNode child : node.getChildren()) {
            if (catMatches(child)) placeNodeRecursive(child, cl, cr);
        }
    }

    private int scaledNodeSize() {
        return Math.max(8, (int) (NODE_SIZE * zoom));
    }

    private void onNodeClicked(QuestNode node) {
        selectedNode = node;
        ctxOpen = false;
        ctxMoveCatOpen = false;
        QuestState st = getState(node);
        if (st != QuestState.LOCKED || isDevMode) {
            Path mdPath = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles")
                    .resolve(node.getId().getPath() + ".md");
            detailData = loadMarkdownContent(mdPath);
        } else {
            detailData = null;
        }
    }

    private void buildLineCache() {
        lineCache.clear();
        int sz = scaledNodeSize();
        for (Map.Entry<ResourceLocation, int[]> e : nodeScreenPos.entrySet()) {
            QuestNode parent = QuestTreeRegistry.getQuest(e.getKey());
            if (parent == null || !catMatches(parent)) continue;
            int[] pPos = e.getValue();
            int px = pPos[0] + sz / 2, py = pPos[1] + sz / 2;
            QuestState ps = getState(parent);
            for (QuestNode child : parent.getChildren()) {
                if (!catMatches(child)) continue;
                int[] cPos = nodeScreenPos.get(child.getId());
                if (cPos == null) continue;
                int cx2 = cPos[0] + sz / 2, cy2 = cPos[1] + sz / 2;
                int col = ps == QuestState.COMPLETED ? C_LINE_DONE :
                        ps == QuestState.ACTIVE ? C_LINE_ACTIVE : C_LINE_LOCKED;
                lineCache.add(new int[] { px, py - 1, cx2 + 1, py + 1, col });
                lineCache.add(new int[] { cx2 - 1, py, cx2 + 1, cy2, col });
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 && newCatFormOpen && newCatBox != null && newCatBox.isFocused()) {
            commitNewCategory();
            return true;
        }
        if (key == 256) {
            if (ctxOpen) {
                ctxOpen = false;
                ctxMoveCatOpen = false;
                return true;
            }
            if (newCatFormOpen) {
                newCatFormOpen = false;
                rebuild();
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void commitNewCategory() {
        if (newCatBox == null) return;
        String name = newCatBox.getValue().trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "_");
        if (!name.isEmpty()) {
            List<String> current = buildCategoryList();
            if (!current.contains(name)) {
                // Add it to the list and persist to disk immediately
                current.add(name);
                saveStubCategories(current);
                selectedCategory = name;
                setFeedback("Category '" + friendly(name) + "' created");
            }
        }
        newCatFormOpen = false;
        rebuild();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int cl = SIDEBAR_W, cr = width - DETAIL_W;
        if (mx <= cl || mx >= cr || my <= HEADER_H) return super.mouseScrolled(mx, my, delta);

        float oldZoom = zoom;
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom + (float) delta * ZOOM_STEP));
        if (zoom == oldZoom) return true;

        // Anchor zoom to mouse cursor: keep the canvas point under the cursor fixed
        float ratio = zoom / oldZoom;
        int canvasMx = (int) mx - cl;
        int canvasMy = (int) my - HEADER_H;
        viewOffX = (int) (canvasMx - (canvasMx - viewOffX) * ratio);
        viewOffY = (int) (canvasMy - (canvasMy - viewOffY) * ratio);

        rebuild();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int cl = SIDEBAR_W, cr = width - DETAIL_W;

        // ── Context menu ──────────────────────────────────────────────────────
        if (ctxOpen && btn == 0) {
            if (handleCtxClick((int) mx, (int) my)) return true;
            ctxOpen = false;
            ctxMoveCatOpen = false;
            return true;
        }

        // ── "Open quest" button in detail panel ───────────────────────────────
        int openBtnY = height - 32;
        if (btn == 0 && selectedNode != null && mx >= cr + 6 && mx <= width - 6 && my >= openBtnY &&
                my <= openBtnY + 18) {
            if (getState(selectedNode) != QuestState.LOCKED || isDevMode) {
                minecraft.setScreen(new QuestTasksScreen(this, selectedNode, detailData, playerData));
            }
            return true;
        }

        // ── Shift + left-click = dev node drag ────────────────────────────────
        if (btn == 0 && isDevMode && hasShiftDown()) {
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    draggedNode = QuestTreeRegistry.getQuest(e.getKey());
                    if (draggedNode != null) {
                        dragGrabX = (int) mx - e.getValue().getX();
                        dragGrabY = (int) my - e.getValue().getY();
                        selectedNode = draggedNode;
                    }
                    return true;
                }
            }
        }

        // ── Shift + right-click = open quest directly ─────────────────────────
        if (btn == 1 && hasShiftDown() && mx > cl && mx < cr) {
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    QuestNode node = QuestTreeRegistry.getQuest(e.getKey());
                    if (node != null && (getState(node) != QuestState.LOCKED || isDevMode)) {
                        onNodeClicked(node); // load detail data first
                        minecraft.setScreen(new QuestTasksScreen(this, node, detailData, playerData));
                        return true;
                    }
                }
            }
            return true;
        }

        // ── Right-click on canvas = dev context menu ──────────────────────────
        if (btn == 1 && isDevMode && mx > cl && mx < cr) {
            QuestNode hit = null;
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    hit = QuestTreeRegistry.getQuest(e.getKey());
                    break;
                }
            }
            openCtx((int) mx, (int) my, hit);
            return true;
        }

        // ── Left-click on canvas = pan start (if no widget consumed it) ───────
        if (btn == 0 && mx > cl && mx < cr && my > HEADER_H) {
            boolean handled = super.mouseClicked(mx, my, btn);
            if (!handled) isPanning = true;
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleCtxClick(int mx, int my) {
        List<CtxItem> items = buildCtxItems();
        int x = ctxX, y = ctxY + 2;

        for (CtxItem item : items) {
            if (item.isSep) {
                y += CTX_SEP;
                continue;
            }
            if (mx >= x && mx <= x + CTX_W && my >= y && my <= y + CTX_ROW) {
                item.action.run();
                return true;
            }
            y += CTX_ROW;
        }

        if (ctxMoveCatOpen) {
            List<String> cats = buildCategoryList();
            cats.remove("ALL");
            int subX = x + CTX_W + 2;
            int subY = ctxMoveCatY(items);
            for (int i = 0; i < cats.size(); i++) {
                int ry = subY + i * CTX_ROW;
                if (mx >= subX && mx <= subX + CTX_W && my >= ry && my <= ry + CTX_ROW) {
                    String newCat = cats.get(i);
                    if (ctxNode != null) {
                        ctxNode.setCategory(newCat);
                        saveNodeCategoryToDisk(ctxNode, newCat);
                        setFeedback("Moved to " + friendly(newCat));
                    }
                    ctxOpen = false;
                    ctxMoveCatOpen = false;
                    rebuild();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0) {
            if (draggedNode != null) {
                int sz = scaledNodeSize();
                int nx = (int) mx - dragGrabX;
                int ny = (int) my - dragGrabY;
                Button b = nodeButtons.get(draggedNode.getId());
                if (b != null) {
                    b.setX(nx);
                    b.setY(ny);
                }
                nodeScreenPos.put(draggedNode.getId(), new int[] { nx, ny });
                // Convert back to logical coords (undo zoom + offset)
                int cl = SIDEBAR_W;
                draggedNode.setCustomPosition(
                        (int) ((nx - cl - viewOffX) / zoom),
                        (int) ((ny - HEADER_H - viewOffY) / zoom));
                buildLineCache();
                return true;
            }
            if (isPanning) {
                viewOffX += (int) dx;
                viewOffY += (int) dy;
                rebuild();
                return true;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) {
            if (draggedNode != null) {
                saveNodeToDisk(draggedNode);
                draggedNode = null;
                rebuild();
                return true;
            }
            isPanning = false;
        }
        return super.mouseReleased(mx, my, btn);
    }

    // ── Context menu construction ─────────────────────────────────────────────

    private record CtxItem(String label, String color, boolean isSep, boolean isDanger, Runnable action) {

        static CtxItem sep() {
            return new CtxItem("", "", true, false, () -> {});
        }
    }

    private List<CtxItem> buildCtxItems() {
        List<CtxItem> items = new ArrayList<>();
        boolean hasNode = (ctxNode != null);

        items.add(new CtxItem("+ New quest", "§a", false, false,
                () -> {
                    ctxOpen = false;
                    minecraft.setScreen(new QuestCreatorScreen(this));
                }));

        if (hasNode) {
            items.add(CtxItem.sep());
            items.add(new CtxItem("Edit quest", "§7", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new QuestCreatorScreen(this, ctxNode));
                    }));
            items.add(new CtxItem("Edit tasks / rewards", "§7", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new TaskRewardEditorScreen(this, ctxNode));
                    }));
            items.add(new CtxItem("Set icon item…", "§7", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                            ctxNode.setIconItem(stack.getItem());
                            saveNodeIconToDisk(ctxNode);
                            setFeedback("Icon → " + stack.getHoverName().getString());
                            rebuild();
                        }));
                    }));
            items.add(new CtxItem("Clear icon", "§8", false, false,
                    () -> {
                        ctxNode.setIconItem(null);
                        saveNodeIconToDisk(ctxNode);
                        setFeedback("Icon cleared");
                        ctxOpen = false;
                        rebuild();
                    }));
            items.add(CtxItem.sep());
            items.add(new CtxItem("Move to category  ▸", "§7", false, false,
                    () -> ctxMoveCatOpen = !ctxMoveCatOpen));
            items.add(CtxItem.sep());
            items.add(new CtxItem("Shift+drag to move", "§8", false, false,
                    () -> {
                        ctxOpen = false;
                        setFeedback("Shift-click and drag the node");
                    }));
            items.add(CtxItem.sep());
            items.add(new CtxItem("Delete quest", "§c", false, true,
                    () -> {
                        QuestTreeRegistry.removeQuest(ctxNode.getId());
                        deleteQuestFiles(ctxNode);
                        if (selectedNode == ctxNode) {
                            selectedNode = null;
                            detailData = null;
                        }
                        ctxOpen = false;
                        rebuild();
                        setFeedback("Quest deleted");
                    }));
        }
        return items;
    }

    private void openCtx(int x, int y, QuestNode node) {
        ctxOpen = true;
        ctxMoveCatOpen = false;
        ctxX = x;
        ctxY = y;
        ctxNode = node;
        List<CtxItem> items = buildCtxItems();
        int menuH = menuHeight(items);
        if (ctxY + menuH > height - 4) ctxY = height - menuH - 4;
        if (ctxX + CTX_W > width - 4) ctxX = width - CTX_W - 4;
    }

    private int menuHeight(List<CtxItem> items) {
        int h = 4;
        for (CtxItem i : items) h += i.isSep ? CTX_SEP : CTX_ROW;
        return h;
    }

    private int ctxMoveCatY(List<CtxItem> items) {
        int y = ctxY + 2;
        for (CtxItem item : items) {
            if (!item.isSep && item.label.contains("Move to category")) return y;
            y += item.isSep ? CTX_SEP : CTX_ROW;
        }
        return y;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        if (feedbackTimer > 0) feedbackTimer--;

        // Live drag update
        if (draggedNode != null) {
            int nx = mx - dragGrabX, ny = my - dragGrabY;
            Button b = nodeButtons.get(draggedNode.getId());
            if (b != null) {
                b.setX(nx);
                b.setY(ny);
            }
            nodeScreenPos.put(draggedNode.getId(), new int[] { nx, ny });
            draggedNode.setCustomPosition(
                    (int) ((nx - SIDEBAR_W - viewOffX) / zoom),
                    (int) ((ny - HEADER_H - viewOffY) / zoom));
            buildLineCache();
        }

        int cl = SIDEBAR_W, cr = width - DETAIL_W;

        renderBackground(g);
        g.fill(0, 0, SIDEBAR_W, height, C_PANEL_DARK);
        g.fill(cl, 0, cr, height, C_BG);
        g.fill(cr, 0, width, height, C_PANEL);

        // Header — row 1 (title) + row 2 (search/filter)
        g.fill(0, 0, width, 22, C_HEADER);
        g.fill(0, 21, width, 22, C_BORDER);
        g.fill(0, 22, width, HEADER_H, C_PANEL_DARK);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.drawString(font, "§8Chronicle  §8⟫  §7" + friendly(selectedCategory), cl + 8, 7, C_TEXT);

        // Zoom level
        String zoomStr = Math.round(zoom * 100) + "%";
        int zpx = cr - font.width(zoomStr) - 50;
        g.drawString(font, "§8" + zoomStr, zpx, 7, C_TEXT_DIM);

        int[] prog = computeProgress();
        int pct = prog[1] == 0 ? 0 : prog[0] * 100 / prog[1];
        String progStr = prog[0] + " / " + prog[1] + "  (" + pct + "%)";
        g.drawString(font, "§8" + progStr, cr - font.width(progStr) - 8, 7, C_TEXT_DIM);

        // Active filter highlight in row 2
        String[] filterKeys = { "ALL", "AVAILABLE", "ACTIVE", "COMPLETE", "LOCKED" };
        int searchW = (cr - cl) / 2 - 6;
        int filterX = cl + 4 + searchW + 6;
        int filterW = (cr - filterX - 4) / filterKeys.length;
        for (int fi = 0; fi < filterKeys.length; fi++) {
            if (stateFilter.equals(filterKeys[fi])) {
                g.fill(filterX + fi * filterW, 23, filterX + fi * filterW + filterW - 1, 36, C_SEL_TAB);
                g.fill(filterX + fi * filterW, 35, filterX + fi * filterW + filterW - 1, 36, C_SEL_ACCENT);
            }
        }

        // Sidebar
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, C_BORDER);
        g.fill(0, HEADER_H, SIDEBAR_W - 1, HEADER_H + 14, C_PANEL_DARK);
        g.drawCenteredString(font, "§8CHAPTERS", SIDEBAR_W / 2, HEADER_H + 3, C_TEXT_FAINT);
        g.fill(0, HEADER_H + 13, SIDEBAR_W - 1, HEADER_H + 14, C_BORDER);

        // Active tab accent
        List<String> cats = buildCategoryList();
        int tabY = HEADER_H + 16;
        for (String cat : cats) {
            if (cat.equals(selectedCategory)) {
                g.fill(0, tabY - 1, SIDEBAR_W - 1, tabY + 17, C_SEL_TAB);
                g.fill(0, tabY - 1, 3, tabY + 17, C_SEL_ACCENT);
            }
            tabY += 18;
        }

        drawDotGrid(g, cl, HEADER_H, cr, height);

        for (int[] ln : lineCache) g.fill(ln[0], ln[1], ln[2], ln[3], ln[4]);

        super.render(g, mx, my, partial); // widgets

        int sz = scaledNodeSize();

        // Node visuals
        for (Map.Entry<ResourceLocation, int[]> entry : nodeScreenPos.entrySet()) {
            QuestNode node = QuestTreeRegistry.getQuest(entry.getKey());
            if (node == null) continue;
            Button btn = nodeButtons.get(node.getId());
            if (btn == null || !btn.visible) continue;
            int[] pos = entry.getValue();
            renderNode(g, node, pos[0], pos[1], sz, btn.isMouseOver(mx, my), node == selectedNode);
        }

        // Node labels (separate pass to avoid overlap)
        for (Map.Entry<ResourceLocation, int[]> entry : nodeScreenPos.entrySet()) {
            QuestNode node = QuestTreeRegistry.getQuest(entry.getKey());
            if (node == null) continue;
            Button btn = nodeButtons.get(node.getId());
            if (btn == null || !btn.visible) continue;
            // Only draw labels when zoomed in enough to be readable
            if (zoom >= 0.6f) {
                int[] pos = entry.getValue();
                QuestState st = getState(node);
                int lc = st == QuestState.COMPLETED ? C_TEXT_DONE :
                        st == QuestState.ACTIVE ? C_TEXT_ACT : st == QuestState.LOCKED ? C_TEXT_FAINT : C_TEXT_DIM;
                g.drawCenteredString(font, shortLabel(node), pos[0] + sz / 2, pos[1] + sz + 2, lc);
            }
        }

        g.fill(cr, HEADER_H - 1, cr + 1, height, C_BORDER);
        renderDetailPanel(g, mx, my, cr);

        if (feedbackTimer > 0 && !feedbackMsg.isEmpty()) {
            g.fill(cl, height - 13, cr, height, C_HEADER);
            g.fill(cl, height - 13, cl + 1, height, C_SEL_ACCENT);
            g.drawString(font, "§7" + feedbackMsg, cl + 6, height - 10, C_TEXT_DIM);
        }

        if (ctxOpen && isDevMode) renderCtxMenu(g, mx, my);
    }

    // ── Node rendering (zoom + shape aware) ───────────────────────────────────

    private void renderNode(GuiGraphics g, QuestNode node, int x, int y, int sz,
                            boolean hovered, boolean selected) {
        QuestState st = getState(node);
        int fill = switch (st) {
            case COMPLETED -> C_NODE_DONE;
            case ACTIVE -> C_NODE_ACTIVE;
            case LOCKED -> C_NODE_LOCKED;
            default -> C_NODE_UNLOCKED;
        };
        int border = switch (st) {
            case COMPLETED -> C_NBORD_DONE;
            case ACTIVE -> C_NBORD_ACTIVE;
            case LOCKED -> isDevMode ? C_NBORD_DEV : C_NBORD_LOCKED;
            default -> C_NBORD_UNLOCKED;
        };
        if (selected) border = C_NBORD_SEL;
        if (hovered) fill = blendColor(fill, 0xFFFFFFFF, 0.08f);

        // Selection glow halo
        if (selected)
            g.fill(x - 2, y - 2, x + sz + 2, y + sz + 2, (border & 0x00FFFFFF) | 0x44000000);

        String shape = node.getShapeType() != null ? node.getShapeType().toUpperCase() : "SQUARE";
        switch (shape) {
            case "CIRCLE" -> {
                fillCircle(g, x, y, sz, fill);
                outlineCircle(g, x, y, sz, border);
            }
            case "DIAMOND" -> {
                fillDiamond(g, x, y, sz, fill);
                outlineDiamond(g, x, y, sz, border);
            }
            case "HEXAGON" -> {
                fillHexagon(g, x, y, sz, fill);
                outlineHexagon(g, x, y, sz, border);
            }
            case "TRIANGLE" -> {
                fillTriangle(g, x, y, sz, fill);
                outlineTriangle(g, x, y, sz, border);
            }
            case "STAR" -> {
                fillStar(g, x, y, sz, fill);
                outlineStar(g, x, y, sz, border);
            }
            case "PENTAGON" -> {
                fillPentagon(g, x, y, sz, fill);
                outlinePentagon(g, x, y, sz, border);
            }
            case "SHIELD" -> {
                fillShield(g, x, y, sz, fill);
                outlineShield(g, x, y, sz, border);
            }
            case "CROSS" -> {
                fillCross(g, x, y, sz, fill);
                outlineCross(g, x, y, sz, border);
            }
            default -> {  // SQUARE
                g.fill(x, y, x + sz, y + sz, fill);
                g.fill(x, y, x + sz, y + 1, border);
                g.fill(x, y + sz - 1, x + sz, y + sz, border);
                g.fill(x, y, x + 1, y + sz, border);
                g.fill(x + sz - 1, y, x + sz, y + sz, border);
            }
        }

        // Inner content: item icon, or state glyph fallback
        Item icon = node.getIconItem();
        if (icon != null && icon != Items.AIR && sz >= 16) {
            int offset = (sz - 16) / 2;
            g.renderItem(new ItemStack(icon), x + offset, y + offset);
        } else if (sz >= 10) {
            String glyph = switch (st) {
                case COMPLETED -> "✔";
                case ACTIVE -> "▶";
                case LOCKED -> "✕";
                default -> "○";
            };
            int gc = switch (st) {
                case COMPLETED -> C_NBORD_DONE;
                case ACTIVE -> C_NBORD_ACTIVE;
                case LOCKED -> isDevMode ? C_NBORD_DEV : C_NBORD_LOCKED;
                default -> C_NBORD_UNLOCKED;
            };
            g.drawCenteredString(font, glyph, x + sz / 2, y + sz / 2 - 4, gc);
        }
    }

    // ── Shape fill primitives ─────────────────────────────────────────────────

    /** Fills every pixel inside a circle inscribed in the [x,y,sz] box. */
    private void fillCircle(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 0.5f;
        for (int py = y; py < y + sz; py++) {
            float dy = py + 0.5f - cy;
            float dx = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            int x0 = (int) Math.ceil(cx - dx), x1 = (int) Math.floor(cx + dx);
            if (x1 >= x0) g.fill(x0, py, x1 + 1, py + 1, color);
        }
    }

    /** 1-pixel outline of a circle. */
    private void outlineCircle(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 1f;
        int steps = Math.max(32, sz * 3);
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int px = (int) (cx + Math.cos(a) * r);
            int py = (int) (cy + Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Diamond (rotated square). */
    private void fillDiamond(GuiGraphics g, int x, int y, int sz, int color) {
        int cx = x + sz / 2, cy = y + sz / 2, h = sz / 2;
        for (int py = y; py < y + sz; py++) {
            int dist = Math.abs(py - cy);
            int half = h - dist;
            if (half > 0) g.fill(cx - half, py, cx + half, py + 1, color);
        }
    }

    private void outlineDiamond(GuiGraphics g, int x, int y, int sz, int color) {
        int cx = x + sz / 2, cy = y + sz / 2, h = sz / 2 - 1;
        // Four edges: top-left, top-right, bottom-left, bottom-right
        for (int i = 0; i <= h; i++) {
            g.fill(cx - i, cy - h + i, cx - i + 1, cy - h + i + 1, color); // TL edge
            g.fill(cx + i, cy - h + i, cx + i + 1, cy - h + i + 1, color); // TR edge
            g.fill(cx - i, cy + h - i, cx - i + 1, cy + h - i + 1, color); // BL edge
            g.fill(cx + i, cy + h - i, cx + i + 1, cy + h - i + 1, color); // BR edge
        }
    }

    /** Flat-top hexagon. */
    private void fillHexagon(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 1;
        float qr = r * 0.866f; // sqrt(3)/2
        for (int py = y; py < y + sz; py++) {
            float dy = Math.abs(py + 0.5f - cy);
            float hw;
            if (dy <= r / 2f) hw = qr;
            else hw = qr * (1f - (dy - r / 2f) / (r / 2f));
            if (hw > 0) g.fill((int) (cx - hw), py, (int) (cx + hw) + 1, py + 1, color);
        }
    }

    private void outlineHexagon(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 1;
        int sides = 6, steps = 2;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI / 6 + i * Math.PI / 3;
            double a1 = Math.PI / 6 + (i + 1) * Math.PI / 3;
            int x0 = (int) (cx + Math.cos(a0) * r), y0 = (int) (cy + Math.sin(a0) * r);
            int x1 = (int) (cx + Math.cos(a1) * r), y1 = (int) (cy + Math.sin(a1) * r);
            drawLine(g, x0, y0, x1, y1, color);
        }
    }

    /** Upward-pointing triangle. */
    private void fillTriangle(GuiGraphics g, int x, int y, int sz, int color) {
        int cx = x + sz / 2;
        int top = y + 1, bot = y + sz - 1;
        for (int py = top; py <= bot; py++) {
            float t = (float) (py - top) / (bot - top);
            int half = (int) (t * sz / 2);
            g.fill(cx - half, py, cx + half + 1, py + 1, color);
        }
    }

    private void outlineTriangle(GuiGraphics g, int x, int y, int sz, int color) {
        int cx = x + sz / 2, top = y + 1, bot = y + sz - 1;
        int bl = x + 1, br = x + sz - 1;
        drawLine(g, cx, top, bl, bot, color); // left edge
        drawLine(g, cx, top, br, bot, color); // right edge
        drawLine(g, bl, bot, br, bot, color); // base
    }

    /** 5-pointed star. */
    private void fillStar(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f;
        float outerR = sz / 2f - 1, innerR = outerR * 0.4f;
        int points = 5;
        // Scan-line fill of star polygon
        float[] px = new float[points * 2], py2 = new float[points * 2];
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points;
            float r2 = (i % 2 == 0) ? outerR : innerR;
            px[i] = cx + (float) (Math.cos(a) * r2);
            py2[i] = cy + (float) (Math.sin(a) * r2);
        }
        for (int scanY = y; scanY < y + sz; scanY++) {
            List<Float> xs = new ArrayList<>();
            for (int i = 0; i < points * 2; i++) {
                int j = (i + 1) % (points * 2);
                float y0 = py2[i], y1 = py2[j];
                if ((y0 <= scanY && y1 > scanY) || (y1 <= scanY && y0 > scanY)) {
                    xs.add(px[i] + (scanY - y0) / (y1 - y0) * (px[j] - px[i]));
                }
            }
            xs.sort(null);
            for (int i = 0; i + 1 < xs.size(); i += 2)
                g.fill((int) xs.get(i).floatValue(), scanY,
                        (int) xs.get(i + 1).floatValue() + 1, scanY + 1, color);
        }
    }

    private void outlineStar(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f;
        float outerR = sz / 2f - 1, innerR = outerR * 0.4f;
        int points = 5;
        int prevX = 0, prevY2 = 0;
        for (int i = 0; i <= points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points;
            float r2 = (i % 2 == 0) ? outerR : innerR;
            int nx = (int) (cx + Math.cos(a) * r2), ny = (int) (cy + Math.sin(a) * r2);
            if (i > 0) drawLine(g, prevX, prevY2, nx, ny, color);
            prevX = nx;
            prevY2 = ny;
        }
    }

    /** 5-sided pentagon. */
    private void fillPentagon(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 1;
        int sides = 5;
        float[] px = new float[sides], py2 = new float[sides];
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / sides;
            px[i] = cx + (float) (Math.cos(a) * r);
            py2[i] = cy + (float) (Math.sin(a) * r);
        }
        fillPolygon(g, px, py2, y, y + sz, color);
    }

    private void outlinePentagon(GuiGraphics g, int x, int y, int sz, int color) {
        float cx = x + sz / 2f, cy = y + sz / 2f, r = sz / 2f - 1;
        int sides = 5;
        int prevX = 0, prevY2 = 0;
        for (int i = 0; i <= sides; i++) {
            double a = -Math.PI / 2 + (i % sides) * 2 * Math.PI / sides;
            int nx = (int) (cx + Math.cos(a) * r), ny = (int) (cy + Math.sin(a) * r);
            if (i > 0) drawLine(g, prevX, prevY2, nx, ny, color);
            prevX = nx;
            prevY2 = ny;
        }
    }

    /** Shield shape: square top half, pointed bottom half. */
    private void fillShield(GuiGraphics g, int x, int y, int sz, int color) {
        int midY = y + sz * 2 / 3;
        // Rectangular top
        g.fill(x + 1, y, x + sz - 1, midY, color);
        // Pointed lower triangle
        int cx = x + sz / 2;
        for (int py = midY; py < y + sz; py++) {
            float t = (float) (py - midY) / (y + sz - midY);
            int half = (int) ((1f - t) * (sz / 2f - 1));
            if (half > 0) g.fill(cx - half, py, cx + half + 1, py + 1, color);
        }
    }

    private void outlineShield(GuiGraphics g, int x, int y, int sz, int color) {
        int midY = y + sz * 2 / 3, cx = x + sz / 2;
        // Top edge
        g.fill(x + 1, y, x + sz - 1, y + 1, color);
        // Left/right sides of rectangle part
        g.fill(x, y, x + 1, midY, color);
        g.fill(x + sz - 1, y, x + sz, midY, color);
        // Converging lines from rect corners to bottom point
        drawLine(g, x, midY, cx, y + sz - 1, color);
        drawLine(g, x + sz, midY, cx, y + sz - 1, color);
    }

    /** Cross / plus shape. */
    private void fillCross(GuiGraphics g, int x, int y, int sz, int color) {
        int arm = sz / 3;
        int cx = x + sz / 2, cy = y + sz / 2;
        g.fill(cx - arm / 2, y + arm / 2, cx + arm / 2 + 1, y + sz - arm / 2, color); // vertical bar
        g.fill(x + arm / 2, cy - arm / 2, x + sz - arm / 2, cy + arm / 2 + 1, color); // horizontal bar
    }

    private void outlineCross(GuiGraphics g, int x, int y, int sz, int color) {
        int arm = sz / 3;
        int cx = x + sz / 2, cy = y + sz / 2;
        int x0 = cx - arm / 2, x1 = cx + arm / 2, y0 = cy - arm / 2, y1 = cy + arm / 2;
        int ax0 = x + arm / 2, ax1 = x + sz - arm / 2;
        int ay0 = y + arm / 2, ay1 = y + sz - arm / 2;
        // Trace the 12-corner outline clockwise
        int[][] pts = { { x0, ay0 }, { x0, y0 }, { ax0, y0 }, { ax0, ay0 }, { ax0, y0 }, { x0, y0 },  // redraw is fine
                                                                                                      // — just hits
                                                                                                      // same pixels
                { x0, ay0 }, { ax0, ay0 }, { ax0, ay1 }, { x0, ay1 }, { ax0, ay1 }, { ax0, ay0 },
                { x1, ay0 }, { ax1, ay0 }, { ax1, y0 }, { x1, y0 }, { ax1, y0 }, { ax1, ay0 },
                { x1, ay1 }, { ax1, ay1 }, { ax1, ay1 }, { x1, ay1 } };
        // Simpler: just draw the 12-sided polygon outline directly
        int[] ox = { x0, x1, x1, ax1, ax1, x1, x1, x0, x0, ax0, ax0, x0, x0 };
        int[] oy = { ay0, ay0, y0, y0, ay0, ay0, ay1, ay1, ay0, ay0, y0, y0, ay0 };
        for (int i = 0; i < 12; i++) drawLine(g, ox[i], oy[i], ox[i + 1], oy[i + 1], color);
    }

    // ── Generic polygon fill (scan-line) ──────────────────────────────────────

    private void fillPolygon(GuiGraphics g, float[] vx, float[] vy, int yMin, int yMax, int color) {
        int n = vx.length;
        for (int scanY = yMin; scanY < yMax; scanY++) {
            List<Float> xs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float y0 = vy[i], y1 = vy[j];
                if ((y0 <= scanY && y1 > scanY) || (y1 <= scanY && y0 > scanY))
                    xs.add(vx[i] + (scanY - y0) / (y1 - y0) * (vx[j] - vx[i]));
            }
            xs.sort(null);
            for (int i = 0; i + 1 < xs.size(); i += 2)
                g.fill((int) xs.get(i).floatValue(), scanY,
                        (int) xs.get(i + 1).floatValue() + 1, scanY + 1, color);
        }
    }

    // ── Bresenham line ────────────────────────────────────────────────────────

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void renderDetailPanel(GuiGraphics g, int mx, int my, int px) {
        int pw = DETAIL_W - 12;
        int x = px + 6;
        int y = HEADER_H + 8;

        if (selectedNode == null) {
            g.drawCenteredString(font, "§8Click a quest", px + DETAIL_W / 2, height / 2 - 16, C_TEXT_FAINT);
            g.drawCenteredString(font, "§8to view details", px + DETAIL_W / 2, height / 2 - 4, C_TEXT_FAINT);
            g.drawCenteredString(font, "§8Shift+right-click", px + DETAIL_W / 2, height / 2 + 12, C_TEXT_FAINT);
            g.drawCenteredString(font, "§8to open directly", px + DETAIL_W / 2, height / 2 + 22, C_TEXT_FAINT);
            if (isDevMode) {
                g.drawCenteredString(font, "§8Right-click canvas", px + DETAIL_W / 2, height / 2 + 38, C_TEXT_FAINT);
                g.drawCenteredString(font, "§8for dev options", px + DETAIL_W / 2, height / 2 + 48, C_TEXT_FAINT);
            }
            return;
        }

        QuestState st = getState(selectedNode);

        // State badge
        int bc = switch (st) {
            case COMPLETED -> 0xFF00CC66;
            case ACTIVE -> 0xFFFFAA00;
            case LOCKED -> 0xFF666678;
            default -> 0xFF8888AA;
        };
        String bt = switch (st) {
            case COMPLETED -> "COMPLETE";
            case ACTIVE -> "IN PROGRESS";
            case LOCKED -> "LOCKED";
            default -> "AVAILABLE";
        };
        int bw = font.width(bt) + 8;
        g.fill(x, y, x + bw, y + 11, (bc & 0x00FFFFFF) | 0x40000000);
        g.fill(x, y, x + bw, y + 1, bc);
        g.fill(x, y + 10, x + bw, y + 11, bc);
        g.fill(x, y, x + 1, y + 11, bc);
        g.fill(x + bw - 1, y, x + bw, y + 11, bc);
        g.drawString(font, bt, x + 4, y + 2, bc);
        y += 14;

        // Icon + title
        Item icon = selectedNode.getIconItem();
        String title = detailData != null ? detailData.title().getString() : selectedNode.getTitle().getString();
        if (icon != null && icon != Items.AIR) {
            g.renderItem(new ItemStack(icon), x, y);
            List<String> tl = wrapText(title, pw - 22);
            int ty = y + (tl.size() == 1 ? 4 : 0);
            for (String l : tl) {
                g.drawString(font, "§f" + l, x + 20, ty, C_TEXT);
                ty += 9;
            }
            y += Math.max(18, tl.size() * 9 + 4);
        } else {
            for (String l : wrapText(title, pw)) {
                g.drawString(font, "§f" + l, x, y, C_TEXT);
                y += 9;
            }
        }
        y += 4;

        g.drawString(font,
                "§8" + friendly(selectedNode.getCategory()) + "  §8·  " +
                        (selectedNode.getShapeType() != null ? selectedNode.getShapeType() : "SQUARE"),
                x, y, C_TEXT_FAINT);
        y += 10;
        y = secDiv(g, x, y, pw);

        // Description
        if (st == QuestState.LOCKED && !isDevMode) {
            g.drawString(font, "§8Complete prerequisites", x, y, C_TEXT_FAINT);
            y += 10;
        } else {
            String desc = detailData != null && !detailData.description().getString().isEmpty() ?
                    detailData.description().getString() : selectedNode.getDescription().getString();
            if (!desc.isEmpty()) {
                List<String> dl = wrapText(desc, pw);
                for (int i = 0; i < Math.min(4, dl.size()); i++) {
                    g.drawString(font, "§8" + dl.get(i), x, y, C_TEXT_DIM);
                    y += 9;
                }
                if (dl.size() > 4) {
                    g.drawString(font, "§8…", x, y, C_TEXT_FAINT);
                    y += 9;
                }
            }
        }
        y += 3;

        // Prerequisites
        if (!selectedNode.getPrerequisites().isEmpty()) {
            y = secDiv(g, x, y, pw);
            g.drawString(font, "§8REQUIRES", x, y, C_TEXT_FAINT);
            y += 11;
            for (QuestNode req : selectedNode.getPrerequisites()) {
                QuestState rs = getState(req);
                String dot = rs == QuestState.COMPLETED ? "§a●" : rs == QuestState.ACTIVE ? "§6◐" : "§8○";
                g.drawString(font, dot + " §8" + shortName(req, pw - 14), x + 4, y, C_TEXT_DIM);
                y += 9;
            }
            y += 2;
        }

        // Objectives
        List<QuestTask> tasks = selectedNode.getTasks();
        if (!tasks.isEmpty()) {
            y = secDiv(g, x, y, pw);
            g.drawString(font, "§8OBJECTIVES  §8" + countDone(tasks) + "/" + tasks.size(), x, y, C_TEXT_FAINT);
            y += 11;
            int barFill = (int) ((float) countDone(tasks) / tasks.size() * pw);
            g.fill(x, y, x + pw, y + 3, C_PROG_BG);
            if (barFill > 0) g.fill(x, y, x + barFill, y + 3,
                    st == QuestState.COMPLETED ? C_PROG_FILL : C_PROG_ACT);
            y += 6;
            for (int i = 0; i < Math.min(5, tasks.size()); i++) {
                QuestTask t = tasks.get(i);
                boolean done = isTaskDone(t);
                String check = done ? "§a✔" : "§8✗";
                String tt = t.getDescription().getString();
                if (font.width(tt) > pw - 14) tt = font.plainSubstrByWidth(tt, pw - 18) + "…";
                g.fill(x, y, x + pw, y + 10, done ? 0x18004400 : 0x10FFFFFF);
                g.drawString(font, check + " §7" + tt, x + 2, y + 1, 0xFFFFFFFF);
                y += 11;
            }
            if (tasks.size() > 5) {
                g.drawString(font, "§8+" + (tasks.size() - 5) + " more…", x + 4, y, C_TEXT_FAINT);
                y += 10;
            }
        }

        // Open quest button
        int btnY = height - 30;
        boolean canOpen = st != QuestState.LOCKED || isDevMode;
        int btnBg = canOpen ? 0xFF0D1F0D : 0xFF141420;
        int btnBrd = canOpen ? C_NBORD_DONE : C_BORDER;
        g.fill(px + 4, btnY, px + DETAIL_W - 4, btnY + 18, btnBg);
        g.fill(px + 4, btnY, px + DETAIL_W - 4, btnY + 1, btnBrd);
        g.fill(px + 4, btnY + 17, px + DETAIL_W - 4, btnY + 18, btnBrd);
        g.fill(px + 4, btnY, px + 5, btnY + 18, btnBrd);
        g.fill(px + DETAIL_W - 5, btnY, px + DETAIL_W - 4, btnY + 18, btnBrd);
        boolean hovBtn = mx >= px + 4 && mx <= px + DETAIL_W - 4 && my >= btnY && my <= btnY + 18;
        if (hovBtn && canOpen) g.fill(px + 5, btnY + 1, px + DETAIL_W - 5, btnY + 17, 0xFF122012);
        String btnLabel = canOpen ? (hovBtn ? "§a» Open quest" : "§aOpen quest  §8→") : "§8🔒 Locked";
        g.drawCenteredString(font, btnLabel, px + DETAIL_W / 2, btnY + 5, 0xFFFFFFFF);
    }

    private int secDiv(GuiGraphics g, int x, int y, int pw) {
        g.fill(x, y, x + pw, y + 1, C_BORDER);
        return y + 5;
    }

    private int countDone(List<QuestTask> tasks) {
        int n = 0;
        for (QuestTask t : tasks) if (isTaskDone(t)) n++;
        return n;
    }

    // ── Context menu render ───────────────────────────────────────────────────

    private void renderCtxMenu(GuiGraphics g, int mx, int my) {
        List<CtxItem> items = buildCtxItems();
        int menuH = menuHeight(items);
        int x = ctxX, y = ctxY;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        g.fill(x + 3, y + 3, x + CTX_W + 3, y + menuH + 3, 0x55000000);
        g.fill(x, y, x + CTX_W, y + menuH, C_CTX_BG);
        g.fill(x, y, x + CTX_W, y + 1, C_CTX_BORDER);
        g.fill(x, y + menuH - 1, x + CTX_W, y + menuH, C_CTX_BORDER);
        g.fill(x, y, x + 1, y + menuH, C_CTX_BORDER);
        g.fill(x + CTX_W - 1, y, x + CTX_W, y + menuH, C_CTX_BORDER);

        int iy = y + 2;
        if (ctxNode != null) {
            g.fill(x + 1, iy, x + 3, iy + CTX_ROW, C_CTX_BORDER);
            g.drawString(font, "§5" + shortName(ctxNode, CTX_W - 12), x + 6, iy + 4, C_CTX_TEXT);
        }

        for (CtxItem item : items) {
            if (item.isSep) {
                g.fill(x + 6, iy + 2, x + CTX_W - 6, iy + 3, C_CTX_SEP);
                iy += CTX_SEP;
                continue;
            }
            boolean hov = mx >= x + 1 && mx <= x + CTX_W - 1 && my >= iy && my <= iy + CTX_ROW;
            if (hov) g.fill(x + 1, iy, x + CTX_W - 1, iy + CTX_ROW, C_CTX_HOVER);
            g.drawString(font, (item.isDanger ? "§c" : item.color) + item.label, x + 8, iy + 4,
                    item.isDanger ? C_CTX_DANGER : C_CTX_TEXT);
            iy += CTX_ROW;
        }

        // Move-category submenu
        if (ctxMoveCatOpen && ctxNode != null) {
            List<String> cats = buildCategoryList();
            cats.remove("ALL");
            int subX = x + CTX_W + 2;
            int subY = ctxMoveCatY(items);
            int subH = cats.size() * CTX_ROW + 4;
            g.fill(subX + 2, subY + 2, subX + CTX_W + 2, subY + subH + 2, 0x55000000);
            g.fill(subX, subY, subX + CTX_W, subY + subH, C_CTX_BG);
            g.fill(subX, subY, subX + CTX_W, subY + 1, C_CTX_BORDER);
            g.fill(subX, subY + subH - 1, subX + CTX_W, subY + subH, C_CTX_BORDER);
            g.fill(subX, subY, subX + 1, subY + subH, C_CTX_BORDER);
            g.fill(subX + CTX_W - 1, subY, subX + CTX_W, subY + subH, C_CTX_BORDER);
            int sy = subY + 2;
            for (String cat : cats) {
                boolean hov = mx >= subX && mx <= subX + CTX_W && my >= sy && my <= sy + CTX_ROW;
                if (hov) g.fill(subX + 1, sy, subX + CTX_W - 1, sy + CTX_ROW, C_CTX_HOVER);
                String mark = cat.equals(ctxNode.getCategory()) ? "§a● " : "§8  ";
                g.drawString(font, mark + "§7" + friendly(cat), subX + 8, sy + 4, C_CTX_TEXT);
                sy += CTX_ROW;
            }
        }

        g.pose().popPose();
    }

    // ── Dot grid ──────────────────────────────────────────────────────────────

    private void drawDotGrid(GuiGraphics g, int x1, int y1, int x2, int y2) {
        // Scale dot spacing with zoom so grid feels consistent
        int sp = Math.max(6, (int) (18 * zoom));
        int sx = x1 + ((viewOffX % sp + sp) % sp);
        int sy = y1 + ((viewOffY % sp + sp) % sp);
        for (int x = sx; x < x2; x += sp)
            for (int y = sy; y < y2; y += sp)
                g.fill(x, y, x + 1, y + 1, C_DOT);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean catMatches(QuestNode n) {
        // Category filter
        if (!selectedCategory.equals("ALL") && !selectedCategory.equals(n.getCategory())) return false;
        // Search filter (title or id substring, case-insensitive)
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            boolean titleMatch = n.getTitle().getString().toLowerCase().contains(q);
            boolean idMatch = n.getId().getPath().toLowerCase().contains(q);
            if (!titleMatch && !idMatch) return false;
        }
        // State filter
        if (!stateFilter.equals("ALL")) {
            QuestState st = getState(n);
            boolean stateMatch = switch (stateFilter) {
                case "AVAILABLE" -> st == QuestState.UNLOCKED;
                case "ACTIVE" -> st == QuestState.ACTIVE;
                case "COMPLETE" -> st == QuestState.COMPLETED;
                case "LOCKED" -> st == QuestState.LOCKED;
                default -> true;
            };
            if (!stateMatch) return false;
        }
        return true;
    }

    private String friendly(String cat) {
        if (cat == null || cat.equals("ALL")) return "All Chapters";
        StringBuilder sb = new StringBuilder();
        for (String w : cat.toLowerCase().replace("_", " ").split(" "))
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }

    private String shortLabel(QuestNode node) {
        String t = node.getTitle().getString();
        int maxW = scaledNodeSize() + 28;
        return font.width(t) > maxW ? font.plainSubstrByWidth(t, maxW - 4) + "…" : t;
    }

    private String shortName(QuestNode node, int maxW) {
        String t = node.getTitle().getString();
        return font.width(t) > maxW ? font.plainSubstrByWidth(t, maxW - 4) + "…" : t;
    }

    private int[] computeProgress() {
        int done = 0, total = 0;
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            if (!selectedCategory.equals("ALL") && !selectedCategory.equals(n.getCategory())) continue;
            total++;
            if (getState(n) == QuestState.COMPLETED) done++;
        }
        return new int[] { done, total };
    }

    private List<String> wrapText(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(test) > maxW && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else cur = new StringBuilder(test);
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    private static int blendColor(int base, int over, float a) {
        int br = (base >> 16) & 0xFF, bg = (base >> 8) & 0xFF, bb = base & 0xFF;
        int or = (over >> 16) & 0xFF, og = (over >> 8) & 0xFF, ob = over & 0xFF;
        return 0xFF000000 | ((int) (br + (or - br) * a) << 16) | ((int) (bg + (og - bg) * a) << 8) |
                (int) (bb + (ob - bb) * a);
    }

    private void setFeedback(String msg) {
        feedbackMsg = msg;
        feedbackTimer = 100;
    }

    // ── Disk persistence ──────────────────────────────────────────────────────

    private Path questSnbt(QuestNode node) {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles")
                .resolve(node.getId().getPath() + ".snbt");
    }

    private void saveNodeToDisk(QuestNode node) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            s = s.replaceAll("positionX:\\s*\\d+", "positionX: " + node.getCustomX());
            s = s.replaceAll("positionY:\\s*\\d+", "positionY: " + node.getCustomY());
            Files.writeString(p, s, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNodeCategoryToDisk(QuestNode node, String cat) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            s = s.replaceAll("category:\\s*\"[^\"]*\"", "category: \"" + cat + "\"");
            Files.writeString(p, s, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNodeIconToDisk(QuestNode node) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            String iconId = node.getIconItemId();
            if (s.contains("icon_item:"))
                s = s.replaceAll("icon_item:\\s*\"[^\"]*\"", "icon_item: \"" + iconId + "\"");
            else {
                int last = s.lastIndexOf('}');
                if (last >= 0) s = s.substring(0, last) + "  icon_item: \"" + iconId + "\"\n" + s.substring(last);
            }
            Files.writeString(p, s, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteQuestFiles(QuestNode node) {
        try {
            Path base = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles");
            Files.deleteIfExists(base.resolve(node.getId().getPath() + ".snbt"));
            Files.deleteIfExists(base.resolve(node.getId().getPath() + ".md"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FullQuestData loadMarkdownContent(Path mdPath) {
        Component title = Component.empty();
        StringBuilder desc = new StringBuilder();
        try (BufferedReader r = Files.newBufferedReader(mdPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("# ") && title.getString().isEmpty())
                    title = Component.literal(t.substring(2).trim());
                else if (!t.startsWith("#") && !t.isEmpty())
                    desc.append(t).append(' ');
            }
        } catch (IOException ignored) {}
        return new FullQuestData(title, Component.literal(desc.toString().trim()), List.of());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
