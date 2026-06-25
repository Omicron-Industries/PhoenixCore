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
import net.phoenix.core.integration.phoenix_chronicles.integration.phantasia.PhantasiaCompat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class ChronicleOverviewScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 110;
    private static final int HEADER_H = 38;  // title bar (22) + search/filter row (16)
    private static final int TOOLBAR_Y = 22;  // search row starts here
    private static final int TOOLBAR_H = 16;
    private static final int NODE_SIZE = 32;

    // ── Palette (themed fields are instance vars, set in init via ChroniclesTheme) ──
    private int C_BG = 0xFF0B0B0F;
    private int C_PANEL = 0xFF14141A;
    private int C_PANEL_DARK = 0xFF0E0E12;
    private int C_HEADER = 0xFF09090D;
    private int C_BORDER = 0xFF252530;
    private int C_BORDER_LIT = 0xFF353548;
    private int C_SEL_TAB = 0xFF1A1A26;
    private int C_SEL_ACCENT = 0xFF00AA55;
    private static final int C_LINE_LOCKED = 0x38FFFFFF;
    private static final int C_LINE_DONE = 0x9900CC66;
    private static final int C_LINE_ACTIVE = 0x88FFAA00;
    // Node fill/border — instance fields so they update when the theme changes
    private int C_NODE_LOCKED   = 0xFF1A1A24;
    private int C_NODE_UNLOCKED = 0xFF1E1E2C;
    private int C_NODE_ACTIVE   = 0xFF221C00;
    private int C_NODE_DONE     = 0xFF081A0E;
    private int C_NBORD_LOCKED  = 0xFF2E2E40;
    private int C_NBORD_UNLOCKED= 0xFF4A4A60;
    private int C_NBORD_ACTIVE  = 0xFFCC9900;
    private int C_NBORD_DONE    = 0xFF00BB66;
    private static final int C_NBORD_SEL = 0xFF6688FF;
    private int C_NBORD_DEV     = 0xFF8844AA;
    private int C_TEXT = 0xFFD8D8E4;
    private int C_TEXT_DIM = 0xFF7A7A8A;
    private int C_TEXT_FAINT = 0xFF404050;
    private int C_TEXT_DONE = 0xFF44CC88;
    private int C_TEXT_ACT = 0xFFFFBB33;
    private static final int C_DOT = 0x14FFFFFF;
    private static final int C_CTX_BG = 0xFF1A1A22;
    private static final int C_CTX_HOVER = 0xFF252532;
    private static final int C_CTX_BORDER = 0xFF8844AA;
    private static final int C_CTX_SEP = 0xFF2A2A38;
    private static final int C_CTX_TEXT = 0xFFCCCCD8;
    private static final int C_CTX_DANGER = 0xFFCC4444;
    private static final int C_PROG_BG = 0xFF1A1A22;
    private int C_PROG_FILL = 0xFF00AA55;
    private static final int C_PROG_ACT = 0xFFBB8800;

    // ── State ─────────────────────────────────────────────────────────────────
    private String selectedCategory = "";
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

    // ── Group drag ────────────────────────────────────────────────────────────
    @Nullable
    private QuestGroup draggedGroup = null;
    private int groupDragGrabX = 0, groupDragGrabY = 0;

    // ── Context menu (pure-render, no hidden buttons) ─────────────────────────
    private static final int CTX_ROW = 16;
    private static final int CTX_SEP = 5;
    private static final int CTX_W = 128;
    private boolean ctxOpen = false;
    private int ctxX, ctxY;
    private QuestNode ctxNode = null;
    private boolean ctxMoveCatOpen = false;
    @Nullable
    private QuestGroup ctxGroup = null;

    // ── New-category inline form ───────────────────────────────────────────────
    private boolean newCatFormOpen = false;
    private EditBox newCatBox = null;

    // Set while a child screen (e.g. QuestTasksScreen compact) renders us as backdrop.
    // Skips widget rendering and tooltips so they don't bleed over the child's card.
    private boolean renderingAsBackdrop = false;

    // ── State filter (toolbar pills) ─────────────────────────────────────────
    private String stateFilter = "ALL";
    private EditBox searchBox = null;
    private String searchQuery = "";
    private String[] searchWords = new String[0];
    // Per-node search haystacks — built once per quest, cleared only on rebuild() or screen open.
    final Map<ResourceLocation, String> searchCache = new HashMap<>();

    // ── Phantasia 3D preview widget ───────────────────────────────────────────
    /** Phantasia 3D preview widget — typed as Object to avoid compile dep on Phantasia. */
    private Object phantasiaPreview = null;

    // ── Multi-select (dev mode) ───────────────────────────────────────────────
    private final Set<ResourceLocation> multiSelection = new LinkedHashSet<>();

    // ── Undo / redo ───────────────────────────────────────────────────────────
    private final Deque<Runnable> undoStack = new ArrayDeque<>();
    private final Deque<Runnable> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 30;

    // ── Tutorial overlay ──────────────────────────────────────────────────────
    // Button hit-boxes computed each frame in renderTutorialOverlay, used in mouseClicked
    private int[] tutPrevBtn  = null;
    private int[] tutNextBtn  = null;
    private int[] tutSkipBtn  = null;

    // ── Canvas caches ─────────────────────────────────────────────────────────
    private final Map<ResourceLocation, int[]> nodeScreenPos = new LinkedHashMap<>();
    private final Map<ResourceLocation, Button> nodeButtons = new LinkedHashMap<>();
    private final List<int[]> lineCache = new ArrayList<>();
    /** Parallel to lineCache — stores [parentId, childId] for hover-highlight. */
    private final List<ResourceLocation[]> lineCacheNodes = new ArrayList<>();
    /** Per-category progress cache; invalidated by rebuild(). */
    private final Map<String, int[]> progressCache = new HashMap<>();

    // ── Bulk-ops extra state ──────────────────────────────────────────────────
    private boolean bulkMoveCatOpen = false;

    // ── Prereq link drag (Alt+drag in dev mode) ───────────────────────────────
    private QuestNode linkDragSource = null;
    private int linkDragX, linkDragY;

    // ── Grid snap (Shift-drag) ────────────────────────────────────────────────
    private static final int GRID_SNAP = 8; // logical units; nodes snap to this grid

    // ── Line right-click context (dev mode) ───────────────────────────────────
    private boolean lineCtxOpen = false;
    private int lineCtxX, lineCtxY;
    private ResourceLocation lineCtxParentId, lineCtxChildId;

    // ── Unlock path visualization ─────────────────────────────────────────────
    private final Set<ResourceLocation> unlockPathHighlight = new HashSet<>();

    // ── Validation panel ─────────────────────────────────────────────────────
    private boolean validationOpen = false;

    // ── Open-fade animation ───────────────────────────────────────────────────
    private long openTimeMs = -1;
    private static final long OPEN_FADE_MS = 120;

    // ── Category accent colors (cycling palette keyed by hash) ────────────────
    private static final int[] CAT_ACCENTS = {
            0xFF5566EE, 0xFF44BB77, 0xFFCC7722, 0xFFAA44CC,
            0xFF22AABB, 0xFFBB4444, 0xFF88AA22, 0xFF448899
    };

    // ── Detail panel ──────────────────────────────────────────────────────────
    private PlayerQuestData playerData = null;

    public ChronicleOverviewScreen() {
        super(Component.literal("Chronicle"));
    }

    // ── Capability helpers ────────────────────────────────────────────────────

    QuestState getState(QuestNode node) {
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
    List<String> buildCategoryList() {
        List<String> cats = new ArrayList<>();

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
        ChroniclesTheme t = ChroniclesTheme.current();
        C_BG = t.bg.getColor();
        C_PANEL = t.panel.getColor();
        C_PANEL_DARK = t.header.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_BORDER_LIT = t.accent.getColor();
        C_SEL_TAB = t.panel.getColor();
        C_SEL_ACCENT = t.accent.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        C_TEXT_DONE = t.done.getColor();
        C_TEXT_ACT = t.activeColor.getColor();
        C_PROG_FILL = t.accent.getColor();

        // Node fills — bg tinted toward each state color
        int bg = t.bg.getColor();
        C_NODE_LOCKED   = blendColor(bg, t.locked.getColor(),      0.18f);
        C_NODE_UNLOCKED = blendColor(bg, t.border.getColor(),      0.35f);
        C_NODE_ACTIVE   = blendColor(bg, t.activeColor.getColor(), 0.22f);
        C_NODE_DONE     = blendColor(bg, t.done.getColor(),        0.18f);
        // Node borders — straight from theme palette
        C_NBORD_LOCKED   = blendColor(t.locked.getColor(),      0xFF000000, 0.25f);
        C_NBORD_UNLOCKED = blendColor(t.border.getColor(),      0xFFFFFFFF, 0.15f);
        C_NBORD_ACTIVE   = t.activeColor.getColor();
        C_NBORD_DONE     = t.done.getColor();
        C_NBORD_DEV      = blendColor(t.accent.getColor(),      0xFFCC44FF, 0.5f);

        QuestGroupManager.invalidate(); // force reload from disk each time the screen opens
        openTimeMs = System.currentTimeMillis();
        rebuild();
    }

    private Path groupsConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");
    }

    private void rebuild() {
        clearWidgets();
        nodeScreenPos.clear();
        nodeButtons.clear();
        lineCache.clear();
        searchCache.clear();
        progressCache.clear();
        ctxOpen = false;
        ctxMoveCatOpen = false;
        ctxGroup = null;
        newCatBox = null;

        // Load quest groups (reads from disk only if not already loaded)
        QuestGroupManager.load(groupsConfigPath());

        if (minecraft != null && minecraft.player != null) {
            isDevMode = minecraft.player.isCreative() || minecraft.player.hasPermissions(2);
            playerData = minecraft.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).orElse(null);
        }

        int cl = SIDEBAR_W, cr = width;

        // ── Sidebar category tabs ─────────────────────────────────────────────
        List<String> cats = buildCategoryList();
        // Auto-select first available category if current selection is gone
        if (!cats.isEmpty() && !cats.contains(selectedCategory)) selectedCategory = cats.get(0);
        int tabY = HEADER_H + 18;
        for (String cat : cats) {
            boolean sel = cat.equals(selectedCategory);
            addRenderableWidget(Button.builder(
                    Component.literal(sel ? "§f" + friendly(cat) : "§8" + friendly(cat)),
                    b -> {
                        selectedCategory = cat;
                        selectedNode = null;
                        PhantasiaCompat.closePreview(phantasiaPreview);
                        phantasiaPreview = null;
                        viewOffX = 0;
                        viewOffY = 0;
                        ctxOpen = false;
                        ctxMoveCatOpen = false;
                        rebuild();
                    }).bounds(2, tabY, SIDEBAR_W - 4, 16).build());
            tabY += 18;
        }

        // ── Sidebar bottom utilities ──────────────────────────────────────────
        // Gear button (all users see it; dev-only actions are inside the screen)
        // Rendered as plain text '⚙' with hover tooltip — no invasive button chrome
        // The actual click is handled in mouseClicked() below

        // "New category" button + form (dev only)
        if (isDevMode) {
            int newCatY = height - (newCatFormOpen ? 38 : 22);
            addRenderableWidget(Button.builder(
                    Component.literal(newCatFormOpen ? "§8– Cancel" : "§a+ Category"),
                    b -> {
                        newCatFormOpen = !newCatFormOpen;
                        rebuild();
                    }).bounds(4, newCatY, SIDEBAR_W - 24, 14).build());

            if (newCatFormOpen) {
                newCatBox = new EditBox(font, 4, height - 22, SIDEBAR_W - 8, 14, Component.empty());
                newCatBox.setHint(Component.literal("§8Name, press Enter"));
                newCatBox.setMaxLength(32);
                addRenderableWidget(newCatBox);
            }
        }

        // Search is now handled by the Ctrl+F overlay — no persistent toolbar search box.

        // ── Quest node buttons ────────────────────────────────────────────────
        for (QuestNode root : QuestTreeRegistry.getRootChapters().values()) {
            if (!catMatches(root)) continue;
            placeNodeRecursive(root, cl, cr);
        }
        // Second pass: catch nodes whose parent is in a different category (cross-category links).
        // These nodes are never reached by the root traversal above when their category is selected.
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            if (catMatches(n)) placeNodeRecursive(n, cl, cr);
        }
        buildLineCache();
    }

    // ── Node placement (zoom-aware) ───────────────────────────────────────────

    private void placeNodeRecursive(QuestNode node, int cl, int cr) {
        if (nodeButtons.containsKey(node.getId())) return; // already placed (cross-category link)
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

    void onNodeClicked(QuestNode node) {
        ctxOpen = false;
        ctxMoveCatOpen = false;
        QuestState st = getState(node);
        if (st == QuestState.LOCKED && !isDevMode) return;
        if (minecraft != null) {
            Path mdPath = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles")
                    .resolve(node.getId().getPath() + ".md");
            FullQuestData fd = loadMarkdownContent(mdPath);
            minecraft.setScreen(new QuestTasksScreen(this, node, fd, playerData));
        }
    }

    /** Called by SearchOverlayScreen to pan the canvas to a quest and select it. */
    void navigateToNode(QuestNode node) {
        if (node.getCategory() != null && !node.getCategory().equals(selectedCategory)) {
            selectedCategory = node.getCategory();
            rebuild();
        }
        int canvasW = width - SIDEBAR_W;
        int canvasH = height - HEADER_H;
        viewOffX = (int) (canvasW / 2f - node.getCustomX() * zoom);
        viewOffY = (int) (canvasH / 2f - node.getCustomY() * zoom);
        onNodeClicked(node);
    }

    private void buildLineCache() {
        lineCache.clear();
        lineCacheNodes.clear();
        int sz = scaledNodeSize();
        for (Map.Entry<ResourceLocation, int[]> e : nodeScreenPos.entrySet()) {
            QuestNode parent = QuestTreeRegistry.getQuest(e.getKey());
            if (parent == null || !catMatches(parent)) continue;
            // Flag-disabled quests and their dep lines are always invisible (even in dev mode)
            if (parent.isFlagDisabled()) continue;
            // Per-quest dep-line visibility
            if (parent.isHideDepLine()) continue;
            int[] pPos = e.getValue();
            int px = pPos[0] + sz / 2, py = pPos[1] + sz / 2;
            QuestState ps = getState(parent);
            // style: 0=locked, 1=done, 2=active, 3=optional-locked, 4=optional-done
            // 5=forbidden-locked, 6=forbidden-done
            // 7=link-locked, 8=link-done, 9=link-active
            for (QuestNode child : parent.getChildren()) {
                if (!catMatches(child)) continue;
                if (child.isHideDepLine()) continue;
                boolean isForbidden = child.isPrereqForbidden(parent.getId());
                boolean isLinkEdge = child.isPrereqLink(parent.getId());
                boolean isOptionalPrereq = !isForbidden && child.hasPerPrereqFlags() &&
                        !child.isPrereqRequired(parent.getId());
                int[] cPos = nodeScreenPos.get(child.getId());
                if (cPos == null) continue;
                int cx2 = cPos[0] + sz / 2, cy2 = cPos[1] + sz / 2;
                int col, style;
                if (isForbidden) {
                    col = ps == QuestState.COMPLETED ? 0xFFAA2222 : 0xFF661111;
                    style = ps == QuestState.COMPLETED ? 6 : 5;
                } else if (isLinkEdge) {
                    col = ps == QuestState.COMPLETED ? 0x6600AA55 :
                            ps == QuestState.ACTIVE ? 0x66FFAA00 : 0x26FFFFFF;
                    style = ps == QuestState.ACTIVE ? 9 : (ps == QuestState.COMPLETED ? 8 : 7);
                } else if (isOptionalPrereq) {
                    col = ps == QuestState.COMPLETED ? 0xFF336644 : 0xFF2A2A3A;
                    style = ps == QuestState.COMPLETED ? 4 : 3;
                } else {
                    col = ps == QuestState.COMPLETED ? C_LINE_DONE :
                            ps == QuestState.ACTIVE ? C_LINE_ACTIVE : C_LINE_LOCKED;
                    style = ps == QuestState.ACTIVE ? 2 : (ps == QuestState.COMPLETED ? 1 : 0);
                }
                lineCache.add(new int[] { px, py, cx2, cy2, col, style });
                lineCacheNodes.add(new ResourceLocation[] { parent.getId(), child.getId() });
            }
            // Also emit lines for prerequisites that are NOT already covered by a child→parent link
            for (QuestNode prereq : parent.getPrerequisites()) {
                if (prereq.getChildren().contains(parent)) continue; // already drawn above
                if (!catMatches(prereq)) continue;
                if (prereq.isFlagDisabled()) continue;
                int[] prereqPos = nodeScreenPos.get(prereq.getId());
                if (prereqPos == null) continue;
                int prx = prereqPos[0] + sz / 2, pry = prereqPos[1] + sz / 2;
                QuestState prereqState = getState(prereq);
                boolean isForbidden = parent.isPrereqForbidden(prereq.getId());
                boolean isLinkEdge = parent.isPrereqLink(prereq.getId());
                boolean isOptional = !isForbidden && parent.hasPerPrereqFlags() &&
                        !parent.isPrereqRequired(prereq.getId());
                int col, style;
                if (isForbidden) {
                    col = prereqState == QuestState.COMPLETED ? 0xFFAA2222 : 0xFF661111;
                    style = prereqState == QuestState.COMPLETED ? 6 : 5;
                } else if (isLinkEdge) {
                    col = prereqState == QuestState.COMPLETED ? 0x6600AA55 :
                            prereqState == QuestState.ACTIVE ? 0x66FFAA00 : 0x26FFFFFF;
                    style = prereqState == QuestState.ACTIVE ? 9 : (prereqState == QuestState.COMPLETED ? 8 : 7);
                } else if (isOptional) {
                    col = prereqState == QuestState.COMPLETED ? 0xFF336644 : 0xFF2A2A3A;
                    style = prereqState == QuestState.COMPLETED ? 4 : 3;
                } else {
                    col = prereqState == QuestState.COMPLETED ? C_LINE_DONE :
                            prereqState == QuestState.ACTIVE ? C_LINE_ACTIVE : C_LINE_LOCKED;
                    style = prereqState == QuestState.ACTIVE ? 2 : (prereqState == QuestState.COMPLETED ? 1 : 0);
                }
                lineCache.add(new int[] { prx, pry, px, py, col, style });
                lineCacheNodes.add(new ResourceLocation[] { prereq.getId(), parent.getId() });
            }
        }
    }

    /**
     * Panning fast-path: shifts all existing node buttons by (dx,dy) without
     * tearing down and recreating every widget.  Much cheaper than rebuild().
     */
    private void panCanvas(int dx, int dy) {
        int cl = SIDEBAR_W, cr = width;
        int sz = scaledNodeSize();
        for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
            Button btn = e.getValue();
            int nx = btn.getX() + dx;
            int ny = btn.getY() + dy;
            btn.setX(nx);
            btn.setY(ny);
            int[] pos = nodeScreenPos.get(e.getKey());
            if (pos != null) { pos[0] = nx; pos[1] = ny; }
            btn.visible = nx + sz > cl && nx < cr && ny + sz > HEADER_H && ny < height;
        }
        buildLineCache();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 2) != 0;

        // ── Ctrl+F — open search overlay ─────────────────────────────────────
        if (key == 70 && ctrl) {
            openSearchOverlay();
            return true;
        }

        // ── L — toggle line style (spline ↔ straight) ────────────────────────
        if (key == 76 && !ctrl) {
            QuestChroniclesSettings s = QuestChroniclesSettings.get();
            boolean nowSpline = s.isSplineLines();
            s.setLineStyle(nowSpline ? QuestChroniclesSettings.LineStyle.STRAIGHT
                    : QuestChroniclesSettings.LineStyle.SPLINE);
            s.save();
            setFeedback("Line style: " + (nowSpline ? "Straight" : "Spline"));
            return true;
        }

        if (key == 257 && newCatFormOpen && newCatBox != null && newCatBox.isFocused()) {
            commitNewCategory();
            return true;
        }
        if (key == 256) {
            if (lineCtxOpen) {
                lineCtxOpen = false;
                return true;
            }
            if (!unlockPathHighlight.isEmpty()) {
                unlockPathHighlight.clear();
                return true;
            }
            if (validationOpen) {
                validationOpen = false;
                return true;
            }
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
        if (key == 256 && isDevMode && !multiSelection.isEmpty()) {
            multiSelection.clear();
            bulkMoveCatOpen = false;
            return true;
        }
        boolean shift = (mods & 1) != 0;
        if (ctrl && isDevMode) {
            if (key == 90 && !shift) {
                undo();
                return true;
            }
            if (key == 89 || (key == 90 && shift)) {
                redo();
                return true;
            }
        }
        if (key == 70 && !ctrl && !shift) {
            fitToCanvas();
            return true;
        }
        if (key == 47 && isDevMode) { // '?' (slash key with shift = ?)
            if (minecraft != null) minecraft.setScreen(new DevWikiScreen(this));
            return true;
        }
        if (key == 86 && !ctrl && !shift && isDevMode) {
            validationOpen = !validationOpen;
            return true;
        }
        if (key == 73 && !ctrl && !shift && isDevMode) {
            runFtbImport();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ── FTB Quests import ─────────────────────────────────────────────────────

    private void runFtbImport() {
        if (minecraft == null) return;
        Path base = minecraft.gameDirectory.toPath().resolve("config").resolve("phoenix_chronicles");
        Path importDir = base.resolve("ftb_import");
        try {
            java.nio.file.Files.createDirectories(importDir);
            FtbQuestsImporter.ImportResult r = FtbQuestsImporter.importDirectory(importDir, base);
            if (r.imported() == 0 && r.skipped() == 0) {
                setFeedback("§eNo .snbt files found in config/phoenix_chronicles/ftb_import/");
            } else {
                setFeedback("§aImported " + r.imported() + " quests" +
                        (r.skipped() > 0 ? " §c(" + r.skipped() + " skipped)" : "") +
                        (r.warnings().isEmpty() ? "" : " §8— " + r.warnings().size() + " warnings"));
                if (r.imported() > 0) {
                    QuestFileLoader.reloadAllQuestsFromDisk();
                    rebuild();
                }
            }
        } catch (Exception e) {
            setFeedback("§cFTB import error: " + e.getMessage());
        }
    }

    // ── Undo / redo ───────────────────────────────────────────────────────────

    private void pushUndo(Runnable action) {
        undoStack.push(action);
        if (undoStack.size() > MAX_UNDO) undoStack.pollLast();
        redoStack.clear(); // new action clears the redo branch
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            setFeedback("Nothing to undo");
            return;
        }
        Runnable action = undoStack.pop();
        action.run();
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            setFeedback("Nothing to redo");
            return;
        }
        Runnable action = redoStack.pop();
        action.run();
    }

    // ── Quest duplication ─────────────────────────────────────────────────────

    private void duplicateQuest(QuestNode source) {
        Path base = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");
        Path srcFile = questSnbt(source);
        if (!Files.exists(srcFile)) {
            setFeedback("Cannot duplicate — source file not found on disk");
            return;
        }
        try {
            String content = Files.readString(srcFile, StandardCharsets.UTF_8);

            // Generate a unique ID by appending _copy (then _copy2, _copy3…)
            String srcPath = source.getId().getPath();
            String newPath = srcPath + "_copy";
            for (int i = 2; Files.exists(base.resolve(newPath + ".snbt")); i++) {
                newPath = srcPath + "_copy" + i;
            }

            // Replace the id field in the SNBT content
            content = content.replaceFirst("id:\\s*\"[^\"]*\"", "id: \"" + newPath + "\"");
            // Offset position slightly so the duplicate doesn't sit exactly on top
            content = offsetSnbtCoord(content, "positionX", 48);
            content = offsetSnbtCoord(content, "positionY", 48);

            Path destFile = base.resolve(newPath + ".snbt");
            Files.writeString(destFile, content, StandardCharsets.UTF_8);

            // Inject into live registry
            QuestFileLoader.loadAdditiveFromDisk(base);
            rebuild();
            setFeedback("Duplicated → " + newPath);
        } catch (IOException e) {
            e.printStackTrace();
            setFeedback("Duplicate failed: " + e.getMessage());
        }
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
        int cl = SIDEBAR_W, cr = width;
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
        if (btn == 0 && handleTutorialClick(mx, my)) return true;

        int cl = SIDEBAR_W, cr = width;

        if (btn == 0) {
            // Inspector removed from overview — all quest detail interactions now in QuestTasksScreen

            // ── Toolbar right-side button clicks (Settings, Fit) ──────────────────
            if (my >= TOOLBAR_Y && my < HEADER_H) {
                int rx = cr - 4;
                int fitW = font.width("⊞ Fit") + 10;
                int settingsW = font.width("⚙") + 10;

                // Fit button
                rx -= fitW + 2;
                if (mx >= rx && mx < rx + fitW) {
                    fitToCanvas();
                    return true;
                }

                // Settings button
                rx -= settingsW + 2;
                if (mx >= rx && mx < rx + settingsW && minecraft != null) {
                    minecraft.setScreen(new SettingsScreen(this));
                    return true;
                }

                // Wiki button (dev only)
                if (isDevMode) {
                    int wikiW = font.width("?") + 10;
                    rx -= wikiW + 2;
                    if (mx >= rx && mx < rx + wikiW && minecraft != null) {
                        minecraft.setScreen(new DevWikiScreen(this));
                        return true;
                    }
                }
            }

            // ── Filter pill clicks ─────────────────────────────────────────────
            int[][] pills = filterPillBounds(cl, cr);
            for (int i = 0; i < FILTER_KEYS.length; i++) {
                int[] b = pills[i];
                if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                    stateFilter = FILTER_KEYS[i];
                    selectedNode = null;
                    rebuild();
                    return true;
                }
            }

            // ── Gear (utilities) click — left=open editor, right=export lang ──
            if (gearHovered((int) mx, (int) my) && minecraft != null) {
                minecraft.setScreen(new LangEditorScreen(this));
                return true;
            }
        }

        if (btn == 1 && gearHovered((int) mx, (int) my) && isDevMode) {
            Path base = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles");
            LangEditorScreen.writeEnUsJson(base);
            setFeedback("§aExported lang/en_us.json");
            return true;
        }

        // ── Bulk-ops panel clicks ─────────────────────────────────────────────
        if (btn == 0 && isDevMode && multiSelection.size() >= 2) {
            int bx = cl + 4, by = HEADER_H + 4;
            int bh = 38;
            if ((int) mx >= bx && (int) mx <= bx + 360 && (int) my >= by && (int) my <= by + bh) {
                // Shape picker row hit-test
                String[] shapeIds = { "SQUARE", "CIRCLE", "DIAMOND", "HEXAGON", "TRIANGLE", "STAR", "PENTAGON",
                        "SHIELD", "CROSS" };
                int slotW = 14, startX = bx + 6, slotY = by + 24;
                for (int i = 0; i < shapeIds.length; i++) {
                    int sx = startX + i * (slotW + 2);
                    if ((int) mx >= sx && (int) mx < sx + slotW && (int) my >= slotY && (int) my < slotY + 12) {
                        String newShape = shapeIds[i];
                        for (ResourceLocation id : multiSelection) {
                            QuestNode n = QuestTreeRegistry.getQuest(id);
                            if (n != null) {
                                n.setShapeType(newShape);
                                saveNodeShapeToDisk(n, newShape);
                            }
                        }
                        setFeedback("Shape → " + newShape + " for " + multiSelection.size() + " quests");
                        rebuild();
                        return true;
                    }
                }
                int actX = startX + shapeIds.length * (slotW + 2) + 8;
                // "Move cat" toggle
                if ((int) mx >= actX && (int) mx < actX + 58 && (int) my >= slotY && (int) my < slotY + 12) {
                    bulkMoveCatOpen = !bulkMoveCatOpen;
                    return true;
                }
                // Bulk move cat submenu
                if (bulkMoveCatOpen) {
                    List<String> moveCats = buildCategoryList();
                    moveCats.remove("ALL");
                    int subX = actX, subY = slotY + 13, subRH = 11;
                    for (int ci = 0; ci < moveCats.size(); ci++) {
                        int ry = subY + 2 + ci * subRH;
                        if ((int) mx >= subX + 2 && (int) mx < subX + 90 - 2 && (int) my >= ry &&
                                (int) my < ry + subRH) {
                            String newCat = moveCats.get(ci);
                            for (ResourceLocation sid : new ArrayList<>(multiSelection)) {
                                QuestNode sn = QuestTreeRegistry.getQuest(sid);
                                if (sn != null) {
                                    sn.setCategory(newCat);
                                    saveNodeCategoryToDisk(sn, newCat);
                                }
                            }
                            bulkMoveCatOpen = false;
                            setFeedback("Moved " + multiSelection.size() + " quests to " + friendly(newCat));
                            rebuild();
                            return true;
                        }
                    }
                }
                // Delete all selected
                int delX = actX + 62;
                if ((int) mx >= delX && (int) mx < delX + 44 && (int) my >= slotY && (int) my < slotY + 12) {
                    int count = multiSelection.size();
                    for (ResourceLocation id : new ArrayList<>(multiSelection)) {
                        QuestNode n = QuestTreeRegistry.getQuest(id);
                        if (n != null) {
                            QuestTreeRegistry.removeQuest(id);
                            deleteQuestFiles(n);
                        }
                    }
                    multiSelection.clear();
                    rebuild();
                    setFeedback("Deleted " + count + " quests");
                    return true;
                }
                return true; // absorb all clicks on the panel
            }
        }

        // ── Line context menu ─────────────────────────────────────────────────
        if (lineCtxOpen && btn == 0) {
            handleLineCtxClick((int) mx, (int) my);
            lineCtxOpen = false;
            return true;
        }
        if (lineCtxOpen) {
            lineCtxOpen = false;
            return true;
        }

        // ── Context menu ──────────────────────────────────────────────────────
        if (ctxOpen && btn == 0) {
            if (handleCtxClick((int) mx, (int) my)) return true;
            ctxOpen = false;
            ctxMoveCatOpen = false;
            return true;
        }

        // ── Ctrl + left-click = toggle multi-select (dev mode) ───────────────
        if (btn == 0 && isDevMode && hasControlDown() && !hasShiftDown()) {
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    if (multiSelection.contains(e.getKey())) multiSelection.remove(e.getKey());
                    else multiSelection.add(e.getKey());
                    return true;
                }
            }
            // Clicking empty canvas clears selection
            multiSelection.clear();
            return true;
        }

        // ── Alt + left-click = start prerequisite link drag (dev mode) ──────
        if (btn == 0 && isDevMode && hasAltDown() && !hasShiftDown()) {
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    linkDragSource = QuestTreeRegistry.getQuest(e.getKey());
                    linkDragX = (int) mx;
                    linkDragY = (int) my;
                    return true;
                }
            }
        }

        // ── Shift + left-click = dev node drag (or group drag) ───────────────
        if (btn == 0 && isDevMode && hasShiftDown()) {
            // Try node first
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    draggedNode = QuestTreeRegistry.getQuest(e.getKey());
                    if (draggedNode != null) {
                        // Capture position before drag so Ctrl+Z can restore it
                        final int preX = draggedNode.getCustomX(), preY = draggedNode.getCustomY();
                        final QuestNode capturedNode = draggedNode;
                        pushUndo(() -> {
                            capturedNode.setCustomPosition(preX, preY);
                            saveNodeToDisk(capturedNode);
                            rebuild();
                        });
                        dragGrabX = (int) mx - e.getValue().getX();
                        dragGrabY = (int) my - e.getValue().getY();
                        selectedNode = draggedNode;
                    }
                    return true;
                }
            }
            // Try group label bar
            QuestGroup hitGrp = groupAtLabelBar(mx, my, cl);
            if (hitGrp != null) {
                draggedGroup = hitGrp;
                int sx = (int) (hitGrp.getX() * zoom) + viewOffX + cl;
                int sy = (int) (hitGrp.getY() * zoom) + viewOffY + HEADER_H;
                groupDragGrabX = (int) mx - sx;
                groupDragGrabY = (int) my - sy;
                return true;
            }
        }

        // ── Shift + right-click = open quest directly ─────────────────────────
        if (btn == 1 && hasShiftDown() && mx > cl && mx < cr) {
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    QuestNode node = QuestTreeRegistry.getQuest(e.getKey());
                    if (node != null && (getState(node) != QuestState.LOCKED || isDevMode)) {
                        onNodeClicked(node);
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
            QuestGroup hitGrp = (hit == null) ? groupAtLabelBar(mx, my, cl) : null;
            // Check if near a line (higher priority than empty-canvas menu)
            if (hit == null && hitGrp == null) {
                for (int i = 0; i < lineCache.size() && i < lineCacheNodes.size(); i++) {
                    int[] ln = lineCache.get(i);
                    if (pointNearBezier((int) mx, (int) my, ln[0], ln[1], ln[2], ln[3], 6)) {
                        lineCtxOpen = true;
                        lineCtxX = (int) mx;
                        lineCtxY = (int) my;
                        lineCtxParentId = lineCacheNodes.get(i)[0];
                        lineCtxChildId = lineCacheNodes.get(i)[1];
                        ctxOpen = false;
                        return true;
                    }
                }
            }
            openCtx((int) mx, (int) my, hit, hitGrp);
            return true;
        }
        // ── Right-click non-dev: show unlock path for locked quests, dep lines on empty canvas ──
        if (btn == 1 && !isDevMode && mx > cl && mx < cr) {
            boolean hitNode = false;
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    QuestNode node = QuestTreeRegistry.getQuest(e.getKey());
                    if (node != null && getState(node) == QuestState.LOCKED) {
                        computeUnlockPath(node);
                        hitNode = true;
                    }
                    break;
                }
            }
            if (!hitNode) {
                unlockPathHighlight.clear();
                // Empty canvas right-click → open dep line settings
                if (minecraft != null) minecraft.setScreen(new DepLineSettingsScreen(this, selectedCategory));
            }
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
        if (ctxNode != null) y += CTX_ROW; // skip title row

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
        if (btn == 0 && linkDragSource != null) {
            linkDragX = (int) mx;
            linkDragY = (int) my;
            return true;
        }
        if (btn == 0) {
            if (draggedGroup != null) {
                int cl = SIDEBAR_W;
                int screenX = (int) mx - groupDragGrabX;
                int screenY = (int) my - groupDragGrabY;
                draggedGroup.setX((int) ((screenX - cl - viewOffX) / zoom));
                draggedGroup.setY((int) ((screenY - HEADER_H - viewOffY) / zoom));
                return true;
            }
            if (draggedNode != null) {
                int cl = SIDEBAR_W;
                int rawX = (int) mx - dragGrabX;
                int rawY = (int) my - dragGrabY;
                // Snap logical position to grid
                int logX = (int) ((rawX - cl - viewOffX) / zoom);
                int logY = (int) ((rawY - HEADER_H - viewOffY) / zoom);
                logX = Math.round((float) logX / GRID_SNAP) * GRID_SNAP;
                logY = Math.round((float) logY / GRID_SNAP) * GRID_SNAP;
                // Recompute screen position from snapped logical position
                int nx = (int) (logX * zoom) + cl + viewOffX;
                int ny = (int) (logY * zoom) + HEADER_H + viewOffY;
                Button b = nodeButtons.get(draggedNode.getId());
                if (b != null) {
                    b.setX(nx);
                    b.setY(ny);
                }
                nodeScreenPos.put(draggedNode.getId(), new int[] { nx, ny });
                draggedNode.setCustomPosition(logX, logY);
                buildLineCache();
                return true;
            }
            if (isPanning) {
                viewOffX += (int) dx;
                viewOffY += (int) dy;
                panCanvas((int) dx, (int) dy);
                return true;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0 && linkDragSource != null) {
            QuestNode src = linkDragSource;
            linkDragSource = null;
            for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
                if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                    QuestNode target = QuestTreeRegistry.getQuest(e.getKey());
                    if (target != null && target != src && !target.getPrerequisites().contains(src)) {
                        target.addPrerequisite(src);
                        target.setPrereqLink(src.getId(), true);
                        saveNodePrereqsToDisk(target);
                        setFeedback("§aLinked: " + src.getId().getPath() + " → prereq of " + target.getId().getPath());
                        buildLineCache();
                        rebuild();
                    }
                    return true;
                }
            }
            return true;
        }
        if (btn == 0) {
            if (draggedGroup != null) {
                QuestGroupManager.save(groupsConfigPath());
                draggedGroup = null;
                return true;
            }
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
        boolean hasGroup = (ctxGroup != null);

        // New quest (only on empty canvas, not on existing quest/group)
        if (!hasNode && !hasGroup) {
            items.add(new CtxItem("+ New quest", "§a", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new QuestCreatorScreen(this));
                    }));
        }

        // Dependency lines (empty canvas — always shown for all right-click contexts)
        if (!hasNode && !hasGroup) {
            final String cat = selectedCategory;
            items.add(new CtxItem("Dependency lines…", "§b", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new DepLineSettingsScreen(this, cat));
                    }));
        }

        // Group creation & theme (only when right-clicking empty canvas, not on a node/group)
        if (!hasNode && !hasGroup) {
            int cl = SIDEBAR_W;
            items.add(new CtxItem("+ New group here", "§b", false, false,
                    () -> {
                        ctxOpen = false;
                        int lx = (int) ((ctxX - cl - viewOffX) / zoom);
                        int ly = (int) ((ctxY - HEADER_H - viewOffY) / zoom);
                        minecraft.setScreen(new QuestGroupEditorScreen(this, selectedCategory, null, lx, ly));
                    }));
            items.add(new CtxItem("Edit chapter theme…", "§d", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(new CategoryThemeScreen(this, selectedCategory));
                    }));
        }

        // Group editing (when right-clicking a group label bar)
        if (hasGroup) {
            items.add(CtxItem.sep());
            QuestGroup grp = ctxGroup;
            items.add(new CtxItem("Edit group…", "§b", false, false,
                    () -> {
                        ctxOpen = false;
                        minecraft.setScreen(
                                new QuestGroupEditorScreen(this, selectedCategory, grp, grp.getX(), grp.getY()));
                    }));
            items.add(new CtxItem("Delete group", "§c", false, true,
                    () -> {
                        QuestGroupManager.remove(grp.getId());
                        QuestGroupManager.save(groupsConfigPath());
                        ctxOpen = false;
                        ctxGroup = null;
                        setFeedback("Group deleted");
                    }));
        }

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
            items.add(new CtxItem("Edit texts…", "§d", false, false,
                    () -> {
                        final QuestNode target = ctxNode;
                        ctxOpen = false;
                        minecraft.setScreen(new LangEditorScreen(this, target));
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
            items.add(new CtxItem("Dependency lines…", "§b", false, false,
                    () -> {
                        ctxOpen = false;
                        final String cat = selectedCategory;
                        minecraft.setScreen(new DepLineSettingsScreen(this, cat));
                    }));
            items.add(CtxItem.sep());
            items.add(new CtxItem("Duplicate quest", "§b", false, false,
                    () -> {
                        ctxOpen = false;
                        duplicateQuest(ctxNode);
                    }));
            items.add(new CtxItem("Force complete (dev)", "§e", false, false,
                    () -> {
                        final QuestNode target = ctxNode;
                        ctxOpen = false;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                                data.setQuestState(target.getId(), QuestState.COMPLETED);
                                data.recordCompletion(target.getId());
                            });
                            setFeedback("Force-completed: " + target.getTitle().getString() + " (client only)");
                            rebuild();
                        }
                    }));
            items.add(new CtxItem("Delete quest", "§c", false, true,
                    () -> {
                        final QuestNode deleted = ctxNode;
                        final Path snbtBackup = questSnbt(deleted);
                        // Read file content BEFORE deleting so we can restore it on undo
                        String snbtContent;
                        try {
                            snbtContent = Files.exists(snbtBackup) ?
                                    Files.readString(snbtBackup, StandardCharsets.UTF_8) : "";
                        } catch (IOException ex) {
                            snbtContent = "";
                        }
                        final String savedContent = snbtContent;
                        pushUndo(() -> {
                            // Restore file + re-inject into registry
                            if (!savedContent.isEmpty()) {
                                try {
                                    Files.createDirectories(snbtBackup.getParent());
                                    Files.writeString(snbtBackup, savedContent, StandardCharsets.UTF_8);
                                } catch (IOException ignored) {}
                            }
                            QuestFileLoader.loadAdditiveFromDisk(snbtBackup.getParent());
                            rebuild();
                            setFeedback("Undo: quest restored");
                        });
                        QuestTreeRegistry.removeQuest(deleted.getId());
                        deleteQuestFiles(deleted);
                        if (selectedNode == deleted) selectedNode = null;
                        ctxOpen = false;
                        rebuild();
                        setFeedback("Quest deleted  (Ctrl+Z to undo)");
                    }));
        }
        return items;
    }

    private void openCtx(int x, int y, QuestNode node) {
        openCtx(x, y, node, null);
    }

    private void openCtx(int x, int y, QuestNode node, @Nullable QuestGroup group) {
        ctxOpen = true;
        ctxMoveCatOpen = false;
        ctxX = x;
        ctxY = y;
        ctxNode = node;
        ctxGroup = group;
        List<CtxItem> items = buildCtxItems();
        int menuH = menuHeight(items);
        if (ctxY + menuH > height - 4) ctxY = height - menuH - 4;
        if (ctxX + CTX_W > width - 4) ctxX = width - CTX_W - 4;
    }

    private int menuHeight(List<CtxItem> items) {
        int h = 4;
        if (ctxNode != null) h += CTX_ROW; // title row
        for (CtxItem i : items) h += i.isSep ? CTX_SEP : CTX_ROW;
        return h;
    }

    private int ctxMoveCatY(List<CtxItem> items) {
        int y = ctxY + 2;
        if (ctxNode != null) y += CTX_ROW; // skip title row
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

        // Live drag update (same grid-snap logic as mouseDragged)
        if (draggedNode != null) {
            int cl2 = SIDEBAR_W;
            int logX = (int) ((mx - dragGrabX - cl2 - viewOffX) / zoom);
            int logY = (int) ((my - dragGrabY - HEADER_H - viewOffY) / zoom);
            logX = Math.round((float) logX / GRID_SNAP) * GRID_SNAP;
            logY = Math.round((float) logY / GRID_SNAP) * GRID_SNAP;
            int nx = (int) (logX * zoom) + cl2 + viewOffX;
            int ny = (int) (logY * zoom) + HEADER_H + viewOffY;
            Button b = nodeButtons.get(draggedNode.getId());
            if (b != null) {
                b.setX(nx);
                b.setY(ny);
            }
            nodeScreenPos.put(draggedNode.getId(), new int[] { nx, ny });
            draggedNode.setCustomPosition(logX, logY);
            buildLineCache();
        }

        int cl = SIDEBAR_W, cr = width;

        renderBackground(g);
        g.fill(0, 0, SIDEBAR_W, height, C_PANEL_DARK);
        g.fill(cl, 0, cr, height, C_BG);
        // Inspector / toggle strip background
        g.fill(cr, 0, width, height, C_PANEL_DARK);
        g.fill(cr, 0, cr + 1, height, C_BORDER);

        // Title bar (row 1)
        g.fill(0, 0, width, TOOLBAR_Y, C_HEADER);
        g.fill(0, TOOLBAR_Y - 1, width, TOOLBAR_Y, C_BORDER);
        g.drawString(font, "§8Chronicle  §8⟫  §7" + friendly(selectedCategory), cl + 8, 7, C_TEXT);

        // Zoom level (title bar right)
        String zoomStr = Math.round(zoom * 100) + "%";
        g.drawString(font, "§8" + zoomStr, cr - font.width(zoomStr) - 8, 7, C_TEXT_DIM);

        // Toolbar row (search + filter pills + zoom) — scissored to header band
        g.enableScissor(0, TOOLBAR_Y, width, HEADER_H);
        renderToolbar(g, mx, my, cl, cr);
        g.disableScissor();

        // Inspector removed — show only in quest detail screen
        PhantasiaCompat.tickPreview(phantasiaPreview);

        // ── Sidebar (clipped to below the header so it can't bleed into title/toolbar) ──
        g.enableScissor(0, HEADER_H, SIDEBAR_W - 1, height);

        g.fill(0, HEADER_H, SIDEBAR_W - 1, HEADER_H + 14, C_PANEL_DARK);
        g.drawCenteredString(font, "§8CHAPTERS", SIDEBAR_W / 2, HEADER_H + 3, C_TEXT_FAINT);
        g.fill(0, HEADER_H + 13, SIDEBAR_W - 1, HEADER_H + 14, C_BORDER);

        // Active tab accent + per-chapter progress bars
        List<String> cats = buildCategoryList();
        int tabY = HEADER_H + 16;
        int barW = SIDEBAR_W - 10;
        for (int ci = 0; ci < cats.size(); ci++) {
            String cat = cats.get(ci);
            int catAccent = CAT_ACCENTS[Math.abs(cat.hashCode()) % CAT_ACCENTS.length];
            boolean isSel = cat.equals(selectedCategory);
            if (isSel) {
                g.fill(0, tabY - 1, SIDEBAR_W - 1, tabY + 17, C_SEL_TAB);
                g.fill(0, tabY - 1, 3, tabY + 17, catAccent);
            } else {
                g.fill(0, tabY + 4, 2, tabY + 13, (catAccent & 0x00FFFFFF) | 0x55000000);
            }
            int[] p = progressCache.computeIfAbsent(cat, this::computeCategoryProgress);
            if (p[1] > 0) {
                String countStr = p[0] + "/" + p[1];
                int countColor = (p[0] == p[1]) ? C_PROG_FILL : (p[0] > 0 ? C_PROG_ACT : C_TEXT_FAINT);
                g.drawString(font, "§8" + countStr, SIDEBAR_W - font.width(countStr) - 5, tabY + 4, countColor);
                int fill = (int) ((float) p[0] / p[1] * barW);
                g.fill(5, tabY + 14, 5 + barW, tabY + 15, 0x22FFFFFF);
                int barColor = (p[0] == p[1]) ? C_PROG_FILL :
                        (isSel ? catAccent : (p[0] > 0 ? C_PROG_ACT : 0x22FFFFFF));
                if (fill > 0) g.fill(5, tabY + 14, 5 + fill, tabY + 15, barColor);
            }
            tabY += 18;
        }

        g.disableScissor();
        // Border between sidebar and canvas — drawn outside scissor so it paints on both edges
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, C_BORDER);

        // ── Canvas (clipped strictly to [cl, cr]) ────────────────────────────
        g.enableScissor(cl, HEADER_H, cr, height);

        drawBackground(g, cl, HEADER_H, cr, height);

        // Draw quest groups behind lines and nodes
        for (QuestGroup grp : QuestGroupManager.forCategory(selectedCategory)) {
            renderQuestGroup(g, grp, cl, cr);
        }

        long animTick = System.currentTimeMillis();
        for (int[] ln : lineCache) drawBezierLine(g, ln[0], ln[1], ln[2], ln[3], ln[4], ln[5], animTick);

        // Sparks: bright dots traveling along lines connected to ACTIVE nodes
        for (int i = 0; i < lineCache.size() && i < lineCacheNodes.size(); i++) {
            ResourceLocation[] pair = lineCacheNodes.get(i);
            QuestNode pn = QuestTreeRegistry.getQuest(pair[0]);
            QuestNode cn = QuestTreeRegistry.getQuest(pair[1]);
            if (pn == null || cn == null) continue;
            QuestState pst = getState(pn), cst = getState(cn);
            if (pst != QuestState.ACTIVE && cst != QuestState.ACTIVE) continue;
            int[] ln = lineCache.get(i);
            drawBezierSpark(g, ln[0], ln[1], ln[2], ln[3], animTick, i);
        }

        // Hover-highlight: find which node the mouse is over and redraw its connected lines brighter
        ResourceLocation hoveredNodeId = null;
        for (Map.Entry<ResourceLocation, Button> e : nodeButtons.entrySet()) {
            if (e.getValue().visible && e.getValue().isMouseOver(mx, my)) {
                hoveredNodeId = e.getKey();
                break;
            }
        }
        if (hoveredNodeId != null) {
            for (int i = 0; i < lineCache.size() && i < lineCacheNodes.size(); i++) {
                ResourceLocation[] pair = lineCacheNodes.get(i);
                if (pair[0].equals(hoveredNodeId) || pair[1].equals(hoveredNodeId)) {
                    int[] ln = lineCache.get(i);
                    drawBezierLine(g, ln[0], ln[1], ln[2], ln[3], boostedLineColor(ln[4]), ln[5], animTick);
                }
            }
        }

        // Link drag: draw dashed preview line from source node to cursor
        if (linkDragSource != null) {
            int[] srcPos = nodeScreenPos.get(linkDragSource.getId());
            if (srcPos != null) {
                int sz2 = scaledNodeSize();
                int sx = srcPos[0] + sz2 / 2, sy = srcPos[1] + sz2 / 2;
                drawBezierLine(g, sx, sy, linkDragX, linkDragY, 0xFFCC44FF, 3, animTick);
                g.drawString(font, "§dAlt+release on target to link", sx - 50, sy - 14, 0xFFAA66FF, false);
            }
        }

        // Widgets (search box, filter pills, node buttons) — rendered outside the canvas scissor
        // so the search EditBox in the header isn't accidentally clipped.
        g.disableScissor();
        if (!renderingAsBackdrop) super.render(g, mx, my, partial);
        g.enableScissor(cl, HEADER_H, cr, height);

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

        // Multi-selection outlines (dev mode) — cyan dashed border around each selected node
        if (isDevMode && !multiSelection.isEmpty()) {
            long dashPhase = (System.currentTimeMillis() / 80) % 6;
            for (ResourceLocation id : multiSelection) {
                int[] pos = nodeScreenPos.get(id);
                if (pos == null) continue;
                int x1 = pos[0] - 2, y1 = pos[1] - 2, x2 = pos[0] + sz + 2, y2 = pos[1] + sz + 2;
                // Draw dashed rectangular outline
                int selCol = 0xFF00DDFF;
                for (int px = x1; px < x2; px++) {
                    if ((px + dashPhase) % 6 < 3) {
                        g.fill(px, y1, px + 1, y1 + 1, selCol);
                        g.fill(px, y2, px + 1, y2 + 1, selCol);
                    }
                }
                for (int py = y1; py < y2; py++) {
                    if ((py + dashPhase) % 6 < 3) {
                        g.fill(x1, py, x1 + 1, py + 1, selCol);
                        g.fill(x2, py, x2 + 1, py + 1, selCol);
                    }
                }
            }
        }

        // Node labels + "NEW" badge (separate pass so labels render above arcs)
        for (Map.Entry<ResourceLocation, int[]> entry : nodeScreenPos.entrySet()) {
            QuestNode node = QuestTreeRegistry.getQuest(entry.getKey());
            if (node == null) continue;
            Button btn = nodeButtons.get(node.getId());
            if (btn == null || !btn.visible) continue;
            int[] pos = entry.getValue();
            QuestState st = getState(node);

            // "NEW" badge: UNLOCKED quests the player hasn't started yet
            if (st == QuestState.UNLOCKED && sz >= 16) {
                int badgeX = pos[0] + sz - 2;
                int badgeY = pos[1] - 1;
                g.fill(badgeX, badgeY, badgeX + font.width("NEW") + 4, badgeY + 8, 0xFF1144BB);
                g.drawString(font, "NEW", badgeX + 2, badgeY + 1, 0xFFAADDFF, false);
            }

            if (zoom >= 0.55f) {
                // Fade label alpha at low zoom; also dim if search active and node doesn't match
                int baseAlpha = zoom >= 0.75f ? 0xFF : (int) ((zoom - 0.55f) / 0.20f * 0xFF);
                if (!searchQuery.isEmpty() && !matchesSearch(node)) baseAlpha = baseAlpha * 30 / 100;
                int lc = st == QuestState.COMPLETED ? C_TEXT_DONE :
                        st == QuestState.ACTIVE ? C_TEXT_ACT : st == QuestState.LOCKED ? C_TEXT_FAINT : C_TEXT_DIM;
                if (baseAlpha < 0xFF) lc = (lc & 0x00FFFFFF) | (baseAlpha << 24);
                g.drawCenteredString(font, shortLabel(node), pos[0] + sz / 2, pos[1] + sz + 4, lc);
            }
        }

        // End canvas scissor — everything below is full-screen UI (overlays, menus, gear)
        g.disableScissor();

        if (feedbackTimer > 0 && !feedbackMsg.isEmpty()) {
            g.fill(cl, height - 13, cr, height, C_HEADER);
            g.fill(cl, height - 13, cl + 1, height, C_SEL_ACCENT);
            g.drawString(font, "§7" + feedbackMsg, cl + 6, height - 10, C_TEXT_DIM);
        }

        // ── Sidebar gear (utilities) icon ─────────────────────────────────────
        renderSidebarGear(g, mx, my);

        // Tutorial overlay — drawn after widgets, before tooltips
        if (!renderingAsBackdrop) renderTutorialOverlay(g, mx, my);

        // Hover tooltip — drawn last so it's always on top
        if (!renderingAsBackdrop && draggedNode == null && !ctxOpen) {
            for (Map.Entry<ResourceLocation, int[]> entry : nodeScreenPos.entrySet()) {
                QuestNode node = QuestTreeRegistry.getQuest(entry.getKey());
                if (node == null) continue;
                Button btn = nodeButtons.get(node.getId());
                if (btn == null || !btn.visible || !btn.isMouseOver(mx, my)) continue;
                renderNodeTooltip(g, node, mx, my);
                break;
            }
        }

        if (!renderingAsBackdrop && ctxOpen && isDevMode) renderCtxMenu(g, mx, my);

        // Unlock path highlight — bright cyan rings around all ancestors of target
        if (!unlockPathHighlight.isEmpty()) {
            long pulse = System.currentTimeMillis();
            float blink = (float) (Math.sin(pulse / 400.0) * 0.3 + 0.7);
            int ringAlpha = (int) (blink * 0xAA) & 0xFF;
            for (ResourceLocation uid : unlockPathHighlight) {
                int[] upos = nodeScreenPos.get(uid);
                if (upos == null) continue;
                int ux = upos[0], uy = upos[1];
                g.fill(ux - 3, uy - 3, ux + sz + 3, uy - 2, (ringAlpha << 24) | 0x0088FF);
                g.fill(ux - 3, uy + sz + 2, ux + sz + 3, uy + sz + 3, (ringAlpha << 24) | 0x0088FF);
                g.fill(ux - 3, uy - 2, ux - 2, uy + sz + 2, (ringAlpha << 24) | 0x0088FF);
                g.fill(ux + sz + 2, uy - 2, ux + sz + 3, uy + sz + 2, (ringAlpha << 24) | 0x0088FF);
            }
            g.drawString(font, "§bUnlock path — §8Esc to clear", cl + 6, height - 10, 0xFF4488FF, false);
        }

        // Line context menu (right-click on dep line)
        if (lineCtxOpen) renderLineCtxMenu(g, mx, my);

        // Validation panel (V key, dev mode)
        if (validationOpen && isDevMode) renderValidationPanel(g, cl, cr);

        // Bulk-ops panel — appears at top of canvas when 2+ nodes are selected
        if (isDevMode && multiSelection.size() >= 2) {
            renderBulkOpsPanel(g, mx, my, cl, cr);
        }

        // Open-fade: black overlay that fades out over OPEN_FADE_MS after the screen opens
        if (openTimeMs > 0) {
            long elapsed = System.currentTimeMillis() - openTimeMs;
            if (elapsed < OPEN_FADE_MS) {
                float t = 1f - (float) elapsed / OPEN_FADE_MS;
                int fadeAlpha = (int) (t * t * 0xFF) & 0xFF;
                if (fadeAlpha > 0) g.fill(0, 0, width, height, (fadeAlpha << 24) | 0x000000);
            }
        }
    }

    // ── Ctrl+F search overlay ─────────────────────────────────────────────────

    private void openSearchOverlay() {
        if (minecraft != null) minecraft.setScreen(new SearchOverlayScreen(this));
    }

    // ── Filter pills ──────────────────────────────────────────────────────────

    private static final String[] FILTER_KEYS = { "ALL", "AVAILABLE", "ACTIVE", "COMPLETE", "LOCKED" };
    private static final String[] FILTER_GLYPHS = { "◉", "○", "◑", "✔", "🔒" };
    private static final int[] FILTER_COLORS = {
            0xFFAAAAAA, // ALL — neutral
            0xFF55BBFF, // AVAILABLE — blue
            0xFFFFBB33, // ACTIVE — amber
            0xFF44CC88, // COMPLETE — green
            0xFF666688, // LOCKED — muted purple
    };

    /** Draws compact pill-style filter tabs in the toolbar row. */
    private void drawFilterPills(GuiGraphics g, int mx, int my, int cl, int cr) {
        int px = cl + 4;
        int py = TOOLBAR_Y + 2;
        int ph = TOOLBAR_H - 4;

        for (int i = 0; i < FILTER_KEYS.length; i++) {
            boolean sel = stateFilter.equals(FILTER_KEYS[i]);
            String label = FILTER_GLYPHS[i] + " " +
                    (FILTER_KEYS[i].charAt(0) + FILTER_KEYS[i].substring(1).toLowerCase());
            int pw = font.width(label) + 8;
            boolean hov = mx >= px && mx < px + pw && my >= py && my < py + ph;

            // Background
            int bg = sel ? (FILTER_COLORS[i] & 0x00FFFFFF | 0x33000000) : (hov ? 0x22FFFFFF : 0x00000000);
            if (bg != 0) g.fill(px, py, px + pw, py + ph, bg);

            // Accent underline when selected
            if (sel) g.fill(px, py + ph - 1, px + pw, py + ph, FILTER_COLORS[i]);

            // Label
            int col = sel ? FILTER_COLORS[i] : (hov ? 0xFFCCCCCC : 0xFF666677);
            g.drawString(font, label, px + 4, py + 2, col, false);

            px += pw + 4;
        }
    }

    /** Returns pill bounds for hit-testing in mouseClicked. [x0,y0,x1,y1] per filter. */
    private int[][] filterPillBounds(int cl, int cr) {
        int px = cl + 4;
        int py = TOOLBAR_Y + 2, ph = TOOLBAR_H - 4;
        int[][] bounds = new int[FILTER_KEYS.length][4];
        for (int i = 0; i < FILTER_KEYS.length; i++) {
            String label = FILTER_GLYPHS[i] + " " +
                    (FILTER_KEYS[i].charAt(0) + FILTER_KEYS[i].substring(1).toLowerCase());
            int pw = font.width(label) + 8;
            bounds[i] = new int[] { px, py, px + pw, py + ph };
            px += pw + 4;
        }
        return bounds;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void renderToolbar(GuiGraphics g, int mx, int my, int cl, int cr) {
        int ty = TOOLBAR_Y;
        g.fill(0, ty, width, ty + TOOLBAR_H, C_PANEL_DARK);
        g.fill(0, ty + TOOLBAR_H - 1, width, ty + TOOLBAR_H, C_BORDER);

        // Search box is a widget rendered by super.render() — just leave space for it.
        // Filter pills follow the search box (offset by SEARCH_BOX_W + gap).
        drawFilterPills(g, mx, my, cl, cr);

        // Settings + Fit controls (right side, before inspector edge)
        int rx = cr - 4;
        rx = drawToolbarBtnR(g, mx, my, rx, ty, "⊞ Fit");
        rx -= 2;
        rx = drawToolbarBtnR(g, mx, my, rx, ty, "⚙");
        if (isDevMode) {
            rx -= 2;
            rx = drawToolbarBtnR(g, mx, my, rx, ty, "?");
        }
        rx -= 2;

        // DEV badge (indicator only; actions are in right-click context menu)
        if (isDevMode) {
            String devLabel = "DEV";
            int dbx = rx - font.width(devLabel) - 12;
            g.fill(dbx, ty + 4, dbx + font.width(devLabel) + 8, ty + TOOLBAR_H - 4, 0x221a0d26);
            g.fill(dbx, ty + TOOLBAR_H - 4, dbx + font.width(devLabel) + 8, ty + TOOLBAR_H - 3, 0xFF9955CC);
            g.drawString(font, "§5" + devLabel, dbx + 4, ty + 4, 0xFF9955CC, false);
        }
    }

    private int drawToolbarBtnR(GuiGraphics g, int mx, int my, int rx, int ty, String label) {
        int tw = font.width(label) + 10;
        int th = TOOLBAR_H - 8;
        int bx = rx - tw, by = ty + 4;
        boolean hov = mx >= bx && mx < bx + tw && my >= by && my < by + th;
        if (hov) g.fill(bx, by, bx + tw, by + th, 0x22FFFFFF);
        g.drawString(font, label, bx + 5, by + 3, hov ? C_TEXT : C_TEXT_DIM, false);
        return bx - 2;
    }

    // ── Inspector removed from overview (shown only in QuestTasksScreen) ───────
    // All inspector rendering methods have been moved to QuestTasksScreen for a cleaner canvas.

    // ── Sidebar gear ──────────────────────────────────────────────────────────

    private static final int GEAR_SIZE = 14;

    private int gearY() {
        return height - GEAR_SIZE - 4;
    }

    private boolean gearHovered(int mx, int my) {
        int gy = gearY();
        return mx >= SIDEBAR_W - GEAR_SIZE - 4 && mx < SIDEBAR_W - 4 && my >= gy && my < gy + GEAR_SIZE;
    }

    private void renderSidebarGear(GuiGraphics g, int mx, int my) {
        int gx = SIDEBAR_W - GEAR_SIZE - 4;
        int gy = gearY();
        boolean hov = gearHovered(mx, my);

        // Subtle separator above utilities area
        g.fill(4, gy - 6, SIDEBAR_W - 4, gy - 5, C_BORDER);

        // Gear glyph
        int col = hov ? 0xFFDDDDE8 : 0xFF555566;
        g.drawString(font, "⚙", gx + 1, gy + 1, col, false);

        if (hov) {
            // Tooltip panel
            int ttW = 200;
            int ttH = isDevMode ? 54 : 30;
            int ttX = gx - ttW - 4;
            int ttY = gy - ttH - 2;
            if (ttX < 2) ttX = 2;
            g.fill(ttX, ttY, ttX + ttW, ttY + ttH, 0xFF1A1A24);
            g.fill(ttX, ttY, ttX + ttW, ttY + 1, C_BORDER);
            g.fill(ttX, ttY + ttH - 1, ttX + ttW, ttY + ttH, C_BORDER);
            g.fill(ttX, ttY, ttX + 1, ttY + ttH, C_BORDER);
            g.fill(ttX + ttW - 1, ttY, ttX + ttW, ttY + ttH, C_BORDER);
            g.drawString(font, "§dUtilities", ttX + 5, ttY + 4, C_TEXT, false);
            g.drawString(font, "§8§oLeft-click§r§8: Edit all quest texts", ttX + 5, ttY + 14, C_TEXT_DIM, false);
            if (isDevMode) {
                g.drawString(font, "§8§oRight-click§r§8: Export lang/en_us.json", ttX + 5, ttY + 24, C_TEXT_DIM, false);
                g.drawString(font, "§8§o[I]§r§8: Import FTB Quests chapter", ttX + 5, ttY + 34, C_TEXT_DIM, false);
                g.drawString(font, "§8(place .snbt in ftb_import/ folder)", ttX + 5, ttY + 44, C_TEXT_FAINT, false);
            }
        }
    }

    // ── Bulk-ops panel ────────────────────────────────────────────────────────

    private void renderBulkOpsPanel(GuiGraphics g, int mx, int my, int cl, int cr) {
        int n = multiSelection.size();
        int bx = cl + 4, by = HEADER_H + 4;
        int bw = 360, bh = 38;
        g.fill(bx, by, bx + bw, by + bh, 0xF0131319);
        g.fill(bx, by, bx + bw, by + 1, C_BORDER_LIT);
        g.fill(bx, by, bx + 1, by + bh, C_BORDER_LIT);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, C_BORDER_LIT);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, C_BORDER_LIT);
        g.fill(bx, by, bx + 2, by + bh, 0xFF00DDFF); // cyan left accent

        g.drawString(font, "§b" + n + " selected", bx + 6, by + 4, 0xFF00DDFF);
        g.drawString(font, "§8Ctrl+click to toggle  ·  Esc to clear", bx + 6, by + 14, C_TEXT_FAINT);

        // Shape picker row
        String[] glyphs = { "■", "●", "◆", "⬡", "▲", "★", "⬠", "❖", "✚" };
        String[] shapeIds = { "SQUARE", "CIRCLE", "DIAMOND", "HEXAGON", "TRIANGLE", "STAR", "PENTAGON", "SHIELD",
                "CROSS" };
        int slotW = 14, startX = bx + 6, slotY = by + 24;
        for (int i = 0; i < glyphs.length; i++) {
            int sx = startX + i * (slotW + 2);
            boolean hov = mx >= sx && mx < sx + slotW && my >= slotY && my < slotY + 12;
            if (hov) g.fill(sx, slotY, sx + slotW, slotY + 12, 0xFF222233);
            g.drawString(font, "§7" + glyphs[i], sx + 2, slotY + 2, hov ? 0xFFFFFFFF : 0xFF888899);
        }
        // "Move to cat ▸" and "Delete all" labels
        int actX = startX + glyphs.length * (slotW + 2) + 8;
        boolean catHov = mx >= actX && mx < actX + 58 && my >= slotY && my < slotY + 12;
        if (catHov || bulkMoveCatOpen) g.fill(actX, slotY, actX + 58, slotY + 12, 0xFF222233);
        g.drawString(font, "§7Move cat ▸", actX, slotY + 2, (catHov || bulkMoveCatOpen) ? 0xFFCCCCFF : C_TEXT_DIM);
        int delX = actX + 62;
        boolean delHov = mx >= delX && mx < delX + 44 && my >= slotY && my < slotY + 12;
        if (delHov) g.fill(delX, slotY, delX + 44, slotY + 12, 0xFF221212);
        g.drawString(font, "§cDel all", delX, slotY + 2, delHov ? 0xFFFF5555 : C_CTX_DANGER);

        // Bulk move submenu
        if (bulkMoveCatOpen) {
            List<String> moveCats = buildCategoryList();
            moveCats.remove("ALL");
            int subX = actX, subY = slotY + 13, subRH = 11, subW = 90;
            g.fill(subX, subY, subX + subW, subY + moveCats.size() * subRH + 4, 0xFF1A1A24);
            g.fill(subX, subY, subX + subW, subY + 1, C_BORDER_LIT);
            g.fill(subX, subY, subX + 1, subY + moveCats.size() * subRH + 4, C_BORDER_LIT);
            g.fill(subX + subW - 1, subY, subX + subW, subY + moveCats.size() * subRH + 4, C_BORDER_LIT);
            for (int ci = 0; ci < moveCats.size(); ci++) {
                int ry = subY + 2 + ci * subRH;
                boolean rHov = mx >= subX + 2 && mx < subX + subW - 2 && my >= ry && my < ry + subRH;
                if (rHov) g.fill(subX + 2, ry, subX + subW - 2, ry + subRH, 0xFF222233);
                g.drawString(font, "§7" + friendly(moveCats.get(ci)), subX + 4, ry + 2, rHov ? 0xFFCCCCFF : C_TEXT_DIM);
            }
        }
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

        // COMPLETED: soft green bloom — layered expanding fills, each softer
        if (st == QuestState.COMPLETED) {
            int bloomRgb = C_NBORD_DONE & 0x00FFFFFF;
            g.fill(x - 4, y - 4, x + sz + 4, y + sz + 4, 0x0C000000 | bloomRgb);
            g.fill(x - 3, y - 3, x + sz + 3, y + sz + 3, 0x18000000 | bloomRgb);
            g.fill(x - 2, y - 2, x + sz + 2, y + sz + 2, 0x28000000 | bloomRgb);
        }

        // ACTIVE: pulsing outer glow
        if (st == QuestState.ACTIVE) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.4 + 0.6);
            int baseColor = C_NBORD_ACTIVE & 0x00FFFFFF;
            for (int d = 3; d >= 1; d--) {
                int alpha = (int) (pulse * 0x50 * (1f - d * 0.28f)) & 0xFF;
                g.fill(x - d, y - d, x + sz + d, y + sz + d, (alpha << 24) | baseColor);
            }
        }

        String shape = node.getShapeType() != null ? node.getShapeType().toUpperCase() : "SQUARE";

        // Drop shadow — shape-matched so it doesn't bleed outside non-square nodes
        switch (shape) {
            case "CIRCLE"   -> fillCircle  (g, x + 2, y + 2, sz, 0x44000000);
            case "DIAMOND"  -> fillDiamond (g, x + 2, y + 2, sz, 0x44000000);
            case "HEXAGON"  -> fillHexagon (g, x + 2, y + 2, sz, 0x44000000);
            case "TRIANGLE" -> fillTriangle(g, x + 2, y + 2, sz, 0x44000000);
            case "STAR"     -> fillStar    (g, x + 2, y + 2, sz, 0x44000000);
            case "PENTAGON" -> fillPentagon(g, x + 2, y + 2, sz, 0x44000000);
            case "SHIELD"   -> fillShield  (g, x + 2, y + 2, sz, 0x44000000);
            case "CROSS"    -> fillCross   (g, x + 2, y + 2, sz, 0x44000000);
            default         -> g.fill(x + 2, y + 2, x + sz + 2, y + sz + 2, 0x44000000);
        }

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

        // DISABLED visibility: grayed-out overlay — visible but can't be completed
        if (node.getVisibility() == QuestNode.Visibility.DISABLED) {
            g.fill(x + 1, y + 1, x + sz - 1, y + sz - 1, 0xBB0B0B0F);
            g.drawCenteredString(font, "§8✕", x + sz / 2, y + sz / 2 - 4, 0xFF444444);
        }

        // Flag-disabled (enable_if = false): dev-only dashed purple border + "⚑" glyph
        if (isDevMode && node.isFlagDisabled()) {
            g.fill(x - 2, y - 2, x + sz + 2, y - 1, 0xBB7722BB);
            g.fill(x - 2, y + sz + 1, x + sz + 2, y + sz + 2, 0xBB7722BB);
            g.fill(x - 2, y - 1, x - 1, y + sz + 1, 0xBB7722BB);
            g.fill(x + sz + 1, y - 1, x + sz + 2, y + sz + 1, 0xBB7722BB);
            g.fill(x + 1, y + 1, x + sz - 1, y + sz - 1, 0xCC0B0B0F);
            g.drawCenteredString(font, "§5⚑", x + sz / 2, y + sz / 2 - 4, 0xFFAA44CC);
        }

        // Search dim: if search is active and this node doesn't match, fade it out
        if (!searchQuery.isEmpty() && !matchesSearch(node)) {
            g.fill(x - 1, y - 1, x + sz + 1, y + sz + 1, 0xCC0B0B0F);
        }


        // LOCKED: diagonal hatch overlay to make inaccessible nodes visually distinct
        if (st == QuestState.LOCKED && !isDevMode) {
            g.fill(x + 1, y + 1, x + sz - 1, y + sz - 1, 0x880B0B0F);
            // Diagonal hatch lines (every 4px, running top-left to bottom-right)
            for (int d = -(sz); d < sz; d += 4) {
                for (int i = 0; i < sz - 1; i++) {
                    int hx = x + 1 + i;
                    int hy = y + 1 + i + d;
                    if (hx < x + 1 || hx >= x + sz - 1 || hy < y + 1 || hy >= y + sz - 1) continue;
                    g.fill(hx, hy, hx + 1, hy + 1, 0x220B0B0F);
                }
            }
        }

        // Progress arc ring around the node (clockwise from top, proportional to tasks done)
        List<QuestTask> tasks = node.getTasks();
        if (!tasks.isEmpty() && sz >= 14) {
            int total = 0, done = 0;
            if (minecraft != null && minecraft.player != null) {
                for (QuestTask t : tasks) {
                    if (t.isOptional()) continue;
                    total++;
                    if (isTaskDone(t)) done++;
                }
            }
            if (total > 0) {
                float fraction = st == QuestState.COMPLETED ? 1f : (float) done / total;
                int arcColor = st == QuestState.COMPLETED ? C_NBORD_DONE :
                        st == QuestState.ACTIVE ? C_NBORD_ACTIVE : 0xFF4488BB;
                drawProgressArc(g, x + sz / 2, y + sz / 2, sz / 2 + 3, fraction, arcColor, 0x22FFFFFF);
            }
        }

        // UNLOCKED: small pulsing "ready" dot in top-right corner
        if (st == QuestState.UNLOCKED && sz >= 20) {
            float readyPulse = (float) (Math.sin(System.currentTimeMillis() / 700.0) * 0.35 + 0.65);
            int dotAlpha = (int) (readyPulse * 0xFF) & 0xFF;
            int dotColor = (dotAlpha << 24) | 0x004488FF;
            g.fill(x + sz - 6, y + 1, x + sz - 1, y + 6, dotColor);
        }

        // Icon: try custom PNG first, then scaled item, then state glyph
        String questPath = node.getId().getPath();
        ResourceLocation customIcon = QuestIconCache.get(questPath);
        if (customIcon != null && sz >= 8) {
            int[] dims = QuestIconCache.getDimensions(questPath);
            int pad = Math.max(2, sz / 8);
            int iconSz = sz - pad * 2;
            g.blit(customIcon, x + pad, y + pad, 0, 0, iconSz, iconSz, dims[0], dims[1]);
            if (sz >= 20) renderStateBadge(g, x, y, sz, st);
        } else {
            Item icon = node.getIconItem();
            if (icon != null && icon != Items.AIR && sz >= 12) {
                float scale = sz / 16f * 0.75f;
                float cx = x + sz / 2f, cy = y + sz / 2f;
                g.pose().pushPose();
                g.pose().translate(cx, cy, 100f);
                g.pose().scale(scale, scale, scale);
                g.renderItem(new ItemStack(icon), -8, -8);
                g.pose().popPose();
                if (sz >= 20) renderStateBadge(g, x, y, sz, st);
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

        // Dev-mode validation warning badge — orange ⚠ in bottom-left corner
        if (isDevMode && sz >= 14) {
            List<String> issues = getValidationIssues(node);
            if (!issues.isEmpty()) {
                int bx = x, by = y + sz - 7;
                g.fill(bx, by, bx + 8, by + 7, 0xEE331800);
                g.drawString(font, "§6!", bx + 2, by, 0xFFFFAA00, false);
            }
        }
    }

    // ── Quest group rendering ─────────────────────────────────────────────────

    /** Height of the label bar at the top of a group rectangle (in screen pixels). */
    private static final int GROUP_LABEL_BAR_H = 11;

    private void renderQuestGroup(GuiGraphics g, QuestGroup grp, int cl, int cr) {
        int sx = (int) (grp.getX() * zoom) + viewOffX + cl;
        int sy = (int) (grp.getY() * zoom) + viewOffY + HEADER_H;
        int sw = (int) (grp.getWidth() * zoom);
        int sh = (int) (grp.getHeight() * zoom);

        // Cull if entirely outside the canvas viewport
        if (sx + sw < cl || sx > cr || sy + sh < HEADER_H || sy > height) return;

        // Fill
        g.fill(sx, sy, sx + sw, sy + sh, grp.getColor());

        // 1-pixel border
        int bc = grp.getBorderColor();
        g.fill(sx, sy, sx + sw, sy + 1, bc);
        g.fill(sx, sy + sh - 1, sx + sw, sy + sh, bc);
        g.fill(sx, sy, sx + 1, sy + sh, bc);
        g.fill(sx + sw - 1, sy, sx + sw, sy + sh, bc);

        // Label bar at the top (slightly more opaque tint)
        g.fill(sx + 1, sy + 1, sx + sw - 1, sy + GROUP_LABEL_BAR_H, (grp.getBorderColor() & 0x00FFFFFF) | 0x55000000);

        // Label text (clipped to group width)
        if (sw > 20) {
            String label = grp.getLabel();
            int maxLabelW = sw - 8;
            if (font.width(label) > maxLabelW) {
                label = font.plainSubstrByWidth(label, maxLabelW - 6) + "…";
            }
            g.drawString(font, "§f" + label, sx + 4, sy + 2, 0xFFFFFFFF);
        }
    }

    /**
     * Returns the group whose label bar is under (mx, my), or null.
     * Used for context-menu detection.
     */
    @Nullable
    private QuestGroup groupAtLabelBar(double mx, double my, int cl) {
        for (QuestGroup grp : QuestGroupManager.forCategory(selectedCategory)) {
            int sx = (int) (grp.getX() * zoom) + viewOffX + cl;
            int sy = (int) (grp.getY() * zoom) + viewOffY + HEADER_H;
            int sw = (int) (grp.getWidth() * zoom);
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + GROUP_LABEL_BAR_H) {
                return grp;
            }
        }
        return null;
    }

    // ── Bezier connector lines ────────────────────────────────────────────────

    /**
     * Draws a cubic bezier S-curve between two node centers at physical pixel precision.
     *
     * By pushing scale(1/guiScale) and multiplying all coordinates by guiScale, we render at
     * 1 physical pixel per sample rather than the default 2×2 GUI-pixel blobs.
     * Soft edge pixels (4 cardinal neighbours at ~25% alpha) simulate anti-aliasing.
     *
     * style: 0 = locked (dotted, faint), 1 = done (solid), 2 = active (marching dashes)
     * 3 = optional-locked (long dashes, very dim), 4 = optional-done (long dashes, dim-green)
     */
    private void drawBezierLine(GuiGraphics g, int x1, int y1, int x2, int y2,
                                int color, int style, long animTick) {
        drawBezierLine(g, x1, y1, x2, y2, color, style, animTick,
                QuestChroniclesSettings.get().isSplineLines());
    }

    private void drawBezierLine(GuiGraphics g, int x1, int y1, int x2, int y2,
                                int color, int style, long animTick, boolean spline) {
        double gs = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float s = (float) (1.0 / gs);

        g.pose().pushPose();
        g.pose().scale(s, s, 1f);

        // All positions in physical pixels
        int px1 = (int) Math.round(x1 * gs), py1 = (int) Math.round(y1 * gs);
        int px2 = (int) Math.round(x2 * gs), py2 = (int) Math.round(y2 * gs);

        // Control points: S-curve (spline) or degenerate straight line
        float adx = Math.abs(px2 - px1), ady = Math.abs(py2 - py1);
        float cp1x, cp1y, cp2x, cp2y;
        if (!spline) {
            // Straight: degenerate bezier with control points at endpoints
            cp1x = px1; cp1y = py1;
            cp2x = px2; cp2y = py2;
        } else if (adx >= ady) {
            // Mostly horizontal: control points split at x-midpoint, keep each y endpoint
            float mx = (px1 + px2) / 2f;
            cp1x = mx;
            cp1y = py1;
            cp2x = mx;
            cp2y = py2;
        } else {
            // Mostly vertical: control points split at y-midpoint, keep each x endpoint
            float my = (py1 + py2) / 2f;
            cp1x = px1;
            cp1y = my;
            cp2x = px2;
            cp2y = my;
        }

        float dist = (float) Math.sqrt(adx * adx + ady * ady);
        int steps = Math.min(Math.max(20, (int) (dist / 1.5f)), 200);

        int alpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        // Soft-edge neighbours at ~22% of line alpha (simulates 1px anti-aliasing)
        int softA = Math.max(0, (alpha * 56) >> 8);
        int soft = (softA << 24) | rgb;

        // Dash params:
        // 0=locked(dots), 1=done(solid), 2=active(marching), 3=opt-locked(long dash), 4=opt-done(long dash)
        // 5=forbidden-locked(short dash), 6=forbidden-done(solid), 7=link-locked(long gap), 8=link-done, 9=link-active
        boolean isSolid = (style == 1 || style == 6 || style == 8);
        boolean isMarching = (style == 2 || style == 9);
        int dashPeriod, dashOn;
        if (style == 0) {
            dashPeriod = 10;
            dashOn = 3;
        } else if (style == 3) {
            dashPeriod = 14;
            dashOn = 6;
        } else if (style == 4) {
            dashPeriod = 14;
            dashOn = 6;
        } else if (style == 5) {
            dashPeriod = 8;
            dashOn = 3;
        } else if (style == 7) {
            dashPeriod = 20;
            dashOn = 5;
        } else if (style == 9) {
            dashPeriod = 20;
            dashOn = 5;
        } else {
            dashPeriod = 8;
            dashOn = 5;
        }
        long speedDiv = isMarching ? QuestChroniclesSettings.get().getLineAnimSpeed().divisor : 1L;
        int dashOffset = (int) ((animTick / speedDiv) % dashPeriod);

        QuestChroniclesSettings.LineVisualStyle vis = QuestChroniclesSettings.get().getLineVisualStyle();

        // Pre-compute glow halos for GLOW mode (two concentric alpha rings)
        int glow1 = 0, glow2 = 0;
        if (vis == QuestChroniclesSettings.LineVisualStyle.GLOW) {
            glow1 = (Math.max(0, (alpha * 38) >> 8) << 24) | rgb;
            glow2 = (Math.max(0, (alpha * 18) >> 8) << 24) | rgb;
        }

        for (int i = 0; i <= steps; i++) {
            if (!isSolid && ((i + dashOffset) % dashPeriod) >= dashOn) continue;

            float t = (float) i / steps;
            float mt = 1f - t;
            int bx = Math.round(mt * mt * mt * px1 + 3 * mt * mt * t * cp1x + 3 * mt * t * t * cp2x + t * t * t * px2);
            int by = Math.round(mt * mt * mt * py1 + 3 * mt * mt * t * cp1y + 3 * mt * t * t * cp2y + t * t * t * py2);

            switch (vis) {
                case THIN -> g.fill(bx, by, bx + 1, by + 1, color);
                case BOLD -> {
                    // 5px core with soft edge
                    g.fill(bx - 2, by - 2, bx + 3, by + 3, color);
                    if (softA > 0) {
                        g.fill(bx - 3, by - 2, bx - 2, by + 3, soft);
                        g.fill(bx + 3, by - 2, bx + 4, by + 3, soft);
                        g.fill(bx - 2, by - 3, bx + 3, by - 2, soft);
                        g.fill(bx - 2, by + 3, bx + 3, by + 4, soft);
                    }
                }
                case THICK -> {
                    // Outer halo (faint, extends to 9px total visual)
                    int haloA = Math.max(0, (alpha * 28) >> 8);
                    if (haloA > 0) {
                        int halo = (haloA << 24) | rgb;
                        g.fill(bx - 4, by - 4, bx + 5, by + 5, halo);
                    }
                    // Soft mid ring
                    if (softA > 0) {
                        g.fill(bx - 3, by - 3, bx + 4, by + 4, soft);
                    }
                    // Hard 7px core
                    g.fill(bx - 3, by - 3, bx + 4, by + 4, color);
                    // Bright centre highlight
                    int hiA = Math.min(0xFF, alpha + 40);
                    g.fill(bx - 1, by - 1, bx + 2, by + 2, (hiA << 24) | rgb);
                }
                case WIDE -> {
                    // Strong 13px halo fading inward, 9px hard core
                    int outerA = Math.max(0, (alpha * 15) >> 8);
                    if (outerA > 0) g.fill(bx - 6, by - 6, bx + 7, by + 7, (outerA << 24) | rgb);
                    int midA = Math.max(0, (alpha * 30) >> 8);
                    if (midA > 0) g.fill(bx - 5, by - 5, bx + 6, by + 6, (midA << 24) | rgb);
                    int innerA = Math.max(0, (alpha * 50) >> 8);
                    if (innerA > 0) g.fill(bx - 4, by - 4, bx + 5, by + 5, (innerA << 24) | rgb);
                    // 9px solid core
                    g.fill(bx - 4, by - 4, bx + 5, by + 5, color);
                    // Bright 5px highlight
                    int hiA = Math.min(0xFF, alpha + 50);
                    g.fill(bx - 2, by - 2, bx + 3, by + 3, (hiA << 24) | rgb);
                }
                case GLOW -> {
                    g.fill(bx - 3, by - 3, bx + 4, by + 4, glow2);
                    g.fill(bx - 2, by - 2, bx + 3, by + 3, glow1);
                    g.fill(bx - 1, by - 1, bx + 2, by + 2, color);
                }
                default -> { // NORMAL
                    g.fill(bx - 1, by - 1, bx + 2, by + 2, color);
                    if (softA > 0) {
                        g.fill(bx - 2, by - 1, bx - 1, by + 2, soft);
                        g.fill(bx + 2, by - 1, bx + 3, by + 2, soft);
                        g.fill(bx - 1, by - 2, bx + 2, by - 1, soft);
                        g.fill(bx - 1, by + 2, bx + 2, by + 3, soft);
                    }
                }
            }
        }

        g.pose().popPose();
    }

    /** Draws a single bright spark dot traveling from (x1,y1) to (x2,y2) along the S-bezier. */
    private void drawBezierSpark(GuiGraphics g, int x1, int y1, int x2, int y2, long animMs, int lineIdx) {
        float adx = Math.abs(x2 - x1), ady = Math.abs(y2 - y1);
        float cp1x, cp1y, cp2x, cp2y;
        if (adx >= ady) {
            float mx = (x1 + x2) / 2f;
            cp1x = mx;
            cp1y = y1;
            cp2x = mx;
            cp2y = y2;
        } else {
            float my = (y1 + y2) / 2f;
            cp1x = x1;
            cp1y = my;
            cp2x = x2;
            cp2y = my;
        }
        // Each line gets its own offset so sparks don't all sync
        float t = ((animMs / 1800f) + lineIdx * 0.37f) % 1f;
        float mt = 1f - t;
        int bx = Math.round(mt * mt * mt * x1 + 3 * mt * mt * t * cp1x + 3 * mt * t * t * cp2x + t * t * t * x2);
        int by = Math.round(mt * mt * mt * y1 + 3 * mt * mt * t * cp1y + 3 * mt * t * t * cp2y + t * t * t * y2);
        // Bright core + soft halo
        g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFFFFEE88);
        g.fill(bx - 2, by - 2, bx + 3, by + 3, 0x44FFCC44);
    }

    // ── Progress arc ─────────────────────────────────────────────────────────

    /**
     * Draws a clockwise progress arc ring at physical pixel resolution.
     * Renders a 2-physical-pixel-wide ring (inner radius r−1, outer radius r)
     * so the arc is visible and clean at any GUI scale.
     */
    private void drawProgressArc(GuiGraphics g, int cx, int cy, int r,
                                 float fraction, int fillColor, int bgColor) {
        double gs = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float s = (float) (1.0 / gs);

        g.pose().pushPose();
        g.pose().scale(s, s, 1f);

        int pcx = (int) Math.round(cx * gs);
        int pcy = (int) Math.round(cy * gs);

        // Draw at two radii for a 2-physical-pixel wide ring
        for (int dr = 0; dr <= 1; dr++) {
            int pr = (int) Math.round((r - dr * 0.5) * gs);
            if (pr <= 0) continue;
            int steps = Math.max(64, pr * 5); // enough steps to hit every pixel once
            for (int i = 0; i < steps; i++) {
                double angle = (i * 2.0 * Math.PI / steps) - Math.PI / 2.0;
                int px = (int) Math.round(pcx + pr * Math.cos(angle));
                int py = (int) Math.round(pcy + pr * Math.sin(angle));
                int col = (i < fraction * steps) ? fillColor : bgColor;
                if ((col >>> 24) == 0) continue;
                g.fill(px, py, px + 1, py + 1, col);
            }
        }

        g.pose().popPose();
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

    /** 1-pixel outline of a circle at physical-pixel precision (1px at any GUI scale). */
    private void outlineCircle(GuiGraphics g, int x, int y, int sz, int color) {
        double gs = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float s = (float) (1.0 / gs);
        g.pose().pushPose();
        g.pose().scale(s, s, 1f);

        float pcx = (float) ((x + sz / 2f) * gs);
        float pcy = (float) ((y + sz / 2f) * gs);
        float pr = (float) ((sz / 2f - 1f) * gs);
        int steps = Math.max(64, (int) (pr * 6.3f)); // ≥ 1 step per physical pixel around circumference
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int px = (int) Math.round(pcx + Math.cos(a) * pr);
            int py = (int) Math.round(pcy + Math.sin(a) * pr);
            g.fill(px, py, px + 1, py + 1, color);
        }
        g.pose().popPose();
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
            iy += CTX_ROW;
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

    /** Parses the first occurrence of {@code key: <number>} in SNBT text and adds {@code delta} to it. */
    private static String offsetSnbtCoord(String snbt, String key, int delta) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(key + ":\\s*(-?\\d+)").matcher(snbt);
        if (!m.find()) return snbt;
        int old = Integer.parseInt(m.group(1));
        return snbt.substring(0, m.start()) + key + ": " + (old + delta) + snbt.substring(m.end());
    }

    // ── Background rendering ──────────────────────────────────────────────────

    /**
     * Renders the full quest graph (background, nodes, sidebar) without interactive widgets or
     * tooltips. Safe to call from a child screen that overlays its own UI on top.
     * Flushes all batched renders and disables any scissor before returning.
     */
    public void renderForChildScreen(GuiGraphics g) {
        renderingAsBackdrop = true;
        try {
            render(g, -9999, -9999, 0f);
        } finally {
            renderingAsBackdrop = false;
        }
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    /** Renders only the static canvas backdrop — no widgets, no scissors, safe to call from child screens. */
    public void renderBackdrop(GuiGraphics g) {
        g.fill(0, 0, SIDEBAR_W, height, C_PANEL_DARK);
        g.fill(SIDEBAR_W, 0, width, height, C_BG);
        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, C_BORDER);
        drawBackground(g, SIDEBAR_W, HEADER_H, width, height);
    }

    private void drawBackground(GuiGraphics g, int x1, int y1, int x2, int y2) {
        CategoryConfig cfg = selectedCategory.isEmpty() ? new CategoryConfig() : CategoryConfig.get(selectedCategory);
        int tint = cfg.getColor();
        if (tint != 0) g.fill(x1, y1, x2, y2, 0xCC000000 | (tint & 0x00FFFFFF));
        switch (cfg.getStyle()) {
            case DOT_GRID -> drawDotGrid(g, x1, y1, x2, y2);
            case GRID_LINES -> drawGridLines(g, x1, y1, x2, y2);
            case HEX_GRID -> drawHexGrid(g, x1, y1, x2, y2);
            case DIAGONAL_LINES -> drawDiagonalLines(g, x1, y1, x2, y2);
            case SOLID -> {} // tint fill above is sufficient
            case CUSTOM -> drawCustomBg(g, x1, y1, x2, y2, cfg.getTexture());
        }
    }

    private void drawDotGrid(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int sp = Math.max(6, (int) (18 * zoom));
        int sx = x1 + ((viewOffX % sp + sp) % sp);
        int sy = y1 + ((viewOffY % sp + sp) % sp);
        for (int x = sx; x < x2; x += sp)
            for (int y = sy; y < y2; y += sp)
                g.fill(x, y, x + 1, y + 1, C_DOT);
    }

    private void drawGridLines(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int sp = Math.max(10, (int) (32 * zoom));
        int sx = x1 + ((viewOffX % sp + sp) % sp);
        int sy = y1 + ((viewOffY % sp + sp) % sp);
        for (int x = sx; x < x2; x += sp) g.fill(x, y1, x + 1, y2, 0x18FFFFFF);
        for (int y = sy; y < y2; y += sp) g.fill(x1, y, x2, y + 1, 0x18FFFFFF);
    }

    private void drawHexGrid(GuiGraphics g, int x1, int y1, int x2, int y2) {
        float r = Math.max(10f, 28f * zoom);
        float w = r * 1.732f; // sqrt(3) * r
        float h = r * 2f;
        float offX = viewOffX % (int) w;
        float offY = viewOffY % (int) (h * 0.75f);
        for (float gy = y1 + offY - h; gy < y2 + h; gy += h * 0.75f) {
            boolean odd = ((int) ((gy - y1 - offY + h) / (h * 0.75f)) % 2) == 1;
            float rowOffX = odd ? w / 2 : 0;
            for (float gx = x1 + offX + rowOffX - w; gx < x2 + w; gx += w) {
                drawHexOutline(g, (int) gx, (int) gy, (int) r, 0x1AFFFFFF);
            }
        }
    }

    private void drawHexOutline(GuiGraphics g, int cx, int cy, int r, int color) {
        int sides = 6;
        int px = 0, py = 0;
        for (int i = 0; i <= sides; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            int nx = cx + (int) (Math.cos(a) * r);
            int ny = cy + (int) (Math.sin(a) * r);
            if (i > 0) drawLine(g, px, py, nx, ny, color);
            px = nx;
            py = ny;
        }
    }

    private void drawDiagonalLines(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int sp = Math.max(10, (int) (24 * zoom));
        int total = (x2 - x1) + (y2 - y1);
        int startOff = ((viewOffX + viewOffY) % sp + sp) % sp;
        for (int d = -sp + startOff; d < total + sp; d += sp) {
            int ax = x1 + d, ay = y1;
            int bx = x1, by = y1 + d;
            // Clamp to canvas rect
            int cx0 = Math.max(x1, Math.min(x2, ax));
            int cy0 = ay + (cx0 - ax);
            int cx1 = Math.max(x1, Math.min(x2, bx));
            int cy1 = by + (cx1 - bx);
            if (cy0 < y1) {
                cx0 += y1 - cy0;
                cy0 = y1;
            }
            if (cy1 < y1) {
                cx1 += y1 - cy1;
                cy1 = y1;
            }
            if (cy0 > y2) {
                cx0 -= cy0 - y2;
                cy0 = y2;
            }
            if (cy1 > y2) {
                cx1 -= cy1 - y2;
                cy1 = y2;
            }
            if (cx0 >= x1 && cx0 <= x2 && cx1 >= x1 && cx1 <= x2)
                drawLine(g, cx0, cy0, cx1, cy1, 0x18FFFFFF);
        }
    }

    private void drawCustomBg(GuiGraphics g, int x1, int y1, int x2, int y2, String textureLoc) {
        if (textureLoc == null || textureLoc.isBlank()) return;
        try {
            ResourceLocation loc = new ResourceLocation(textureLoc);
            int w = x2 - x1, h = y2 - y1;
            g.blit(loc, x1, y1, 0, 0, w, h, w, h);
        } catch (Exception ignored) {} // malformed RL or missing texture
    }

    // ── State badge (small corner indicator when node has an icon) ────────────

    private void renderStateBadge(GuiGraphics g, int nx, int ny, int sz, QuestState st) {
        int badgeSz = Math.min(8, Math.max(4, sz / 5));
        int bx = nx + sz - badgeSz - 1, by = ny + sz - badgeSz - 1;
        int bc = switch (st) {
            case COMPLETED -> C_NBORD_DONE;
            case ACTIVE -> C_NBORD_ACTIVE;
            case LOCKED -> C_NBORD_LOCKED;
            default -> 0xFF4488FF;
        };
        g.fill(bx - 1, by - 1, bx + badgeSz + 1, by + badgeSz + 1, 0xAA0B0B0F);
        g.fill(bx, by, bx + badgeSz, by + badgeSz, bc);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean catMatches(QuestNode n) {
        QuestNode.Visibility vis = n.getVisibility();
        // Flag-disabled quests never appear (treated as nonexistent), even in dev mode
        if (n.isFlagDisabled()) return isDevMode; // dev can still see them with a faint marker
        // HIDDEN quests invisible to non-devs until prerequisites satisfied
        if (!isDevMode && vis == QuestNode.Visibility.HIDDEN) {
            if (getState(n) == QuestState.LOCKED) return false;
        }
        // DISABLED quests are always visible (shown grayed out); they are NOT hidden
        // Category filter
        if (!selectedCategory.equals(n.getCategory())) return false;
        // State filter (hard filter — search is soft/dim only via matchesSearch)
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

    /** Returns true if this node matches the current search query (any-word-order, title+id+desc). */
    private boolean matchesSearch(QuestNode n) {
        if (searchWords.length == 0) return true;
        String hay = searchCache.computeIfAbsent(n.getId(), id -> buildSearchHaystack(n));
        for (String word : searchWords) {
            if (!hay.contains(word)) return false;
        }
        return true;
    }

    String buildSearchHaystack(QuestNode n) {
        StringBuilder sb = new StringBuilder();

        // Core text fields (lowercased for case-insensitive search)
        sb.append(n.getTitle().getString().toLowerCase()).append(' ');
        sb.append(n.getId().getPath().replace('_', ' ').toLowerCase()).append(' ');
        sb.append(n.getId().toString().toLowerCase()).append(' ');
        if (!n.getDescription().getString().isEmpty())
            sb.append(n.getDescription().getString().toLowerCase()).append(' ');
        if (n.getSubtitle() != null && !n.getSubtitle().isEmpty()) sb.append(n.getSubtitle().toLowerCase()).append(' ');
        sb.append(n.getCategory().toLowerCase()).append(' ');

        // Tasks — description text + item name + item ID + item tags
        for (QuestTask task : n.getTasks()) {
            sb.append(task.getDescription().getString().toLowerCase()).append(' ');

            ResourceLocation displayId = task.getDisplayItemId();
            if (displayId != null) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(displayId);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    // Item display name
                    sb.append(item.getDescription().getString().toLowerCase()).append(' ');
                    // Item registry path (e.g. "iron_ingot" → "iron ingot")
                    sb.append(displayId.getPath().replace('_', ' ').toLowerCase()).append(' ');
                    sb.append(displayId.toString().toLowerCase()).append(' ');
                    // Item tags
                    try {
                        net.minecraft.core.Registry<net.minecraft.world.item.Item> itemReg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                        var holder = itemReg.getHolder(itemReg.getId(item));
                        if (holder.isPresent()) {
                            for (var tag : holder.get().tags().toList()) {
                                sb.append(tag.location().getPath().replace('/', ' ').replace('_', ' ').toLowerCase())
                                        .append(' ');
                                sb.append(tag.location().toString().toLowerCase()).append(' ');
                            }
                        }
                    } catch (Exception ignored) {}
                    // Tooltips — use the local player if available so mods see a real player
                    try {
                        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
                        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft
                                .getInstance().player;
                        var tooltipLines = stack.getTooltipLines(localPlayer,
                                net.minecraft.world.item.TooltipFlag.Default.NORMAL);
                        for (int ti = 1; ti < tooltipLines.size(); ti++) { // skip index 0 (display name — already in
                                                                           // haystack)
                            String txt = tooltipLines.get(ti).getString().trim().toLowerCase();
                            if (!txt.isEmpty()) sb.append(txt).append(' ');
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Rewards — item name + ID
        for (QuestReward reward : n.getRewards()) {
            sb.append(reward.getSummary().getString()).append(' ');
            if (reward instanceof QuestReward.ItemReward ir) {
                ResourceLocation rid = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ir.getItem());
                if (rid != null) {
                    sb.append(rid.getPath().replace('_', ' ')).append(' ');
                    sb.append(rid.toString()).append(' ');
                }
            }
        }

        return sb.toString().toLowerCase();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Returns a list of human-readable warnings about this quest node.
     * Only meaningful in dev mode — called at render time, kept cheap.
     */
    private List<String> getValidationIssues(QuestNode node) {
        List<String> issues = new ArrayList<>();
        // No tasks at all
        if (node.getTasks().isEmpty()) issues.add("No tasks defined");
        // No title
        if (node.getTitle().getString().isBlank()) issues.add("Missing title");
        // SNBT file doesn't exist on disk (unsaved / loaded from datapack)
        Path snbt = questSnbt(node);
        if (!Files.exists(snbt)) issues.add("No editable file on disk (datapack quest)");
        // Check that registered item IDs in item_check tasks are resolvable
        for (QuestTask task : node.getTasks()) {
            if (task instanceof net.phoenix.core.integration.phoenix_chronicles.tasks.ItemRequirementTask irt) {
                if (irt.getItem() == null || irt.getItem() == net.minecraft.world.item.Items.AIR) {
                    issues.add("Item task has missing/AIR item");
                }
            }
        }
        // Prerequisites that no longer exist
        for (QuestNode prereq : node.getPrerequisites()) {
            if (QuestTreeRegistry.getQuest(prereq.getId()) == null) {
                issues.add("Broken prerequisite: " + prereq.getId().getPath());
            }
        }
        return issues;
    }

    String friendly(String cat) {
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

    // ── Fit-to-canvas ─────────────────────────────────────────────────────────

    private void fitToCanvas() {
        if (nodeScreenPos.isEmpty()) return;
        int cl = SIDEBAR_W, cr = width;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            if (!catMatches(n)) continue;
            int nx = n.getCustomX() != 0 ? n.getCustomX() : 20;
            int ny = n.getCustomY() != 0 ? n.getCustomY() : 40;
            minX = Math.min(minX, nx);
            minY = Math.min(minY, ny);
            maxX = Math.max(maxX, nx + NODE_SIZE);
            maxY = Math.max(maxY, ny + NODE_SIZE);
        }
        if (minX == Integer.MAX_VALUE) return;
        int canvasW = cr - cl - 20, canvasH = height - HEADER_H - 20;
        int contentW = maxX - minX, contentH = maxY - minY;
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, Math.min(
                (float) canvasW / contentW,
                (float) canvasH / contentH)));
        viewOffX = (int) (canvasW / 2f - (minX + contentW / 2f) * zoom) + 10;
        viewOffY = (int) (canvasH / 2f - (minY + contentH / 2f) * zoom) + 10;
        rebuild();
    }

    // ── Hover tooltip ─────────────────────────────────────────────────────────

    private void renderNodeTooltip(GuiGraphics g, QuestNode node, int mx, int my) {
        QuestState st = getState(node);
        String title = node.getTitle().getString();
        String sub = node.getSubtitle() != null && !node.getSubtitle().isBlank() ? node.getSubtitle() : null;

        // Task progress summary
        List<QuestTask> tasks = node.getTasks();
        int taskDone = 0, taskTotal = 0;
        List<String> taskLines = new ArrayList<>();
        if (minecraft != null && minecraft.player != null) {
            for (QuestTask t : tasks) {
                if (t.isOptional()) continue;
                taskTotal++;
                boolean done = isTaskDone(t);
                if (done) taskDone++;
                String prog = t.getProgressString(minecraft.player);
                String check = done ? "§a✔ " : "§8✗ ";
                taskLines.add(check + "§7" + t.getDescription().getString() +
                        (prog != null && !prog.isBlank() ? " §8(" + prog + ")" : ""));
            }
        }

        // State line
        String stateLine = switch (st) {
            case COMPLETED -> "§a✔ Complete";
            case ACTIVE -> "§e▶ In progress — " + taskDone + "/" + taskTotal;
            case UNLOCKED -> "§b○ Ready to start";
            case LOCKED -> "§8✕ Locked";
        };

        // Prereqs
        List<String> prereqLines = new ArrayList<>();
        if (!node.getPrerequisites().isEmpty()) {
            for (QuestNode req : node.getPrerequisites()) {
                QuestState rs = getState(req);
                String mark = rs == QuestState.COMPLETED ? "§a✔" : "§8○";
                prereqLines.add("  " + mark + " §8" + req.getTitle().getString());
            }
        }

        // Build tooltip lines
        List<String> lines = new ArrayList<>();
        lines.add("§f" + title);
        if (sub != null) lines.add("§8" + sub);
        lines.add(stateLine);
        if (!taskLines.isEmpty()) {
            lines.add("§8─────────────");
            lines.addAll(taskLines.stream().limit(6).toList());
            if (taskLines.size() > 6) lines.add("§8  … +" + (taskLines.size() - 6) + " more");
        }
        if (!prereqLines.isEmpty() && st == QuestState.LOCKED) {
            lines.add("§8─────────────");
            lines.add("§8Requires:");
            lines.addAll(prereqLines.stream().limit(4).toList());
        }
        // Dev-mode validation warnings
        if (isDevMode) {
            List<String> issues = getValidationIssues(node);
            if (!issues.isEmpty()) {
                lines.add("§8─────────────");
                lines.add("§6⚠ Validation issues:");
                issues.forEach(i -> lines.add("  §e• §7" + i));
            }
        }

        int lineH = font.lineHeight + 2;
        int padH = 6, padW = 8;
        int tipW = lines.stream().mapToInt(font::width).max().orElse(60) + padW * 2;
        int tipH = lines.size() * lineH + padH * 2;

        int tx = mx + 10, ty = my + 12;
        if (tx + tipW > width - 4) tx = mx - tipW - 4;
        if (ty + tipH > height - 4) ty = my - tipH - 4;

        g.fill(tx, ty, tx + tipW, ty + tipH, 0xF00D0D14);
        g.fill(tx, ty, tx + tipW, ty + 1, C_BORDER_LIT);
        g.fill(tx, ty + tipH - 1, tx + tipW, ty + tipH, C_BORDER_LIT);
        g.fill(tx, ty, tx + 1, ty + tipH, C_BORDER_LIT);
        g.fill(tx + tipW - 1, ty, tx + tipW, ty + tipH, C_BORDER_LIT);
        g.fill(tx, ty, tx + 1, ty + tipH, 0xFF884499); // left accent bar

        int lx = tx + padW, ly = ty + padH;
        for (String line : lines) {
            g.drawString(font, line, lx, ly, 0xFFCCCCDD);
            ly += lineH;
        }
    }

    private int[] computeProgress() {
        return computeCategoryProgress(selectedCategory);
    }

    private int[] computeCategoryProgress(String cat) {
        int done = 0, total = 0;
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            if (!cat.equals("ALL") && !cat.equals(n.getCategory())) continue;
            if (n.isFlagDisabled()) continue; // flag-disabled = nonexistent, excluded from progress
            // DISABLED visibility still counts toward progress (visible but uncompletable)
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

    /** Brightens a line color for hover highlighting. */
    private static int boostedLineColor(int col) {
        int r = Math.min(255, ((col >> 16) & 0xFF) + 90);
        int g2 = Math.min(255, ((col >> 8) & 0xFF) + 90);
        int b = Math.min(255, (col & 0xFF) + 90);
        return 0xFF000000 | (r << 16) | (g2 << 8) | b;
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

    /** Called by DepLineSettingsScreen to trigger a line-cache rebuild after per-quest hide toggles. */
    void rebuildFromExternal() { rebuild(); }

    /** Saves the hide_dep_line flag for a single quest node to its SNBT file. */
    void saveNodeHideDepLineToDisk(QuestNode node) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            if (node.isHideDepLine()) {
                if (!s.contains("hide_dep_line:"))
                    s = s.substring(0, s.lastIndexOf('}')) + "  hide_dep_line: 1b\n" + s.substring(s.lastIndexOf('}'));
                else
                    s = s.replaceAll("hide_dep_line:\\s*\\S+", "hide_dep_line: 1b");
            } else {
                s = s.replaceAll(",?\\s*hide_dep_line:\\s*\\S+", "");
                s = s.replaceAll("hide_dep_line:\\s*\\S+,?\\s*", "");
            }
            Files.writeString(p, s, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void saveNodeShapeToDisk(QuestNode node, String shape) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            if (s.contains("shape:"))
                s = s.replaceAll("shape:\\s*\"[^\"]*\"", "shape: \"" + shape + "\"");
            else {
                int last = s.lastIndexOf('}');
                if (last >= 0) s = s.substring(0, last) + "  shape: \"" + shape + "\"\n" + s.substring(last);
            }
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

    private void saveNodePrereqsToDisk(QuestNode node) {
        try {
            Path p = questSnbt(node);
            if (!Files.exists(p)) return;
            net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(
                    Files.readString(p, StandardCharsets.UTF_8));
            net.minecraft.nbt.ListTag prereqList = new net.minecraft.nbt.ListTag();
            for (QuestNode req : node.getPrerequisites()) {
                net.minecraft.nbt.CompoundTag pTag = new net.minecraft.nbt.CompoundTag();
                pTag.putString("id", req.getId().getPath());
                pTag.putBoolean("required", node.isPrereqRequired(req.getId()));
                prereqList.add(pTag);
            }
            if (!prereqList.isEmpty()) tag.put("prerequisites", prereqList);
            else tag.remove("prerequisites");
            Files.writeString(p, tag.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
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

    // ── Bezier proximity ─────────────────────────────────────────────────────

    /** True when (mx,my) is within tol pixels of the cubic S-bezier from (x1,y1) to (x2,y2). */
    private boolean pointNearBezier(int mx, int my, int x1, int y1, int x2, int y2, int tol) {
        int dx = x2 - x1, dy = y2 - y1;
        int cx1 = x1 + dx / 3, cy1 = y1;
        int cx2 = x2 - dx / 3, cy2 = y2;
        int steps = Math.max(16, Math.abs(dx) / 4 + Math.abs(dy) / 4);
        int tolSq = tol * tol;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float u = 1 - t;
            float bx = u * u * u * x1 + 3 * u * u * t * cx1 + 3 * u * t * t * cx2 + t * t * t * x2;
            float by = u * u * u * y1 + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y2;
            int ddx = mx - (int) bx, ddy = my - (int) by;
            if (ddx * ddx + ddy * ddy <= tolSq) return true;
        }
        return false;
    }

    // ── Line context menu ─────────────────────────────────────────────────────

    private void renderLineCtxMenu(GuiGraphics g, double mx, double my) {
        QuestNode parentNode = lineCtxParentId == null ? null : QuestTreeRegistry.getQuest(lineCtxParentId);
        QuestNode childNode = lineCtxChildId == null ? null : QuestTreeRegistry.getQuest(lineCtxChildId);
        if (parentNode == null || childNode == null) {
            lineCtxOpen = false;
            return;
        }

        boolean isForbidden = childNode.isPrereqForbidden(lineCtxParentId);
        boolean isRequired = !isForbidden && childNode.isPrereqRequired(lineCtxParentId);
        boolean isLink = childNode.isPrereqLink(lineCtxParentId);

        // 3-way cycle label: required → optional → forbidden → required
        String cycleLabel;
        if (isForbidden) cycleLabel = "§aType: Forbidden  →  Required";
        else if (!isRequired) cycleLabel = "§cType: Optional  →  Forbidden";
        else cycleLabel = "§eType: Required  →  Optional";

        String linkLabel = isLink ? "§7Unmark as link" : "§dMark as link";
        boolean childHidesLines = childNode.isHideDepLine();
        String hideLabel = childHidesLines ? "§aShow dep lines: " + childNode.getTitle().getString()
                : "§8Hide dep lines: " + childNode.getTitle().getString();

        String[] labels = {
                "§cRemove connection",
                cycleLabel,
                linkLabel,
                hideLabel,
                "§bDependency line settings…"
        };
        int menuW = 210, itemH = 14, pad = 4;
        int menuH = pad + labels.length * itemH + pad;
        int lx = lineCtxX, ly = lineCtxY;
        if (lx + menuW > width) lx = width - menuW - 2;
        if (ly + menuH > height) ly = height - menuH - 2;

        g.fill(lx, ly, lx + menuW, ly + menuH, 0xEE0D0D12);
        g.fill(lx, ly, lx + menuW, ly + 1, 0xFF9900FF);
        g.fill(lx, ly, lx + 1, ly + menuH, 0xFF9900FF);
        g.fill(lx + menuW - 1, ly, lx + menuW, ly + menuH, 0xFF9900FF);
        g.fill(lx, ly + menuH - 1, lx + menuW, ly + menuH, 0xFF9900FF);

        for (int i = 0; i < labels.length; i++) {
            int iy = ly + pad + i * itemH;
            boolean hov = mx >= lx && mx < lx + menuW && my >= iy && my < iy + itemH;
            if (hov) g.fill(lx + 1, iy, lx + menuW - 1, iy + itemH, 0x44FFFFFF);
            g.drawString(font, labels[i], lx + 6, iy + 2, 0xFFDDDDDD, false);
        }
    }

    private void handleLineCtxClick(int mx, int my) {
        if (!lineCtxOpen) return;
        QuestNode parentNode = lineCtxParentId == null ? null : QuestTreeRegistry.getQuest(lineCtxParentId);
        QuestNode childNode = lineCtxChildId == null ? null : QuestTreeRegistry.getQuest(lineCtxChildId);
        if (parentNode == null || childNode == null) {
            lineCtxOpen = false;
            return;
        }

        boolean isForbidden = childNode.isPrereqForbidden(lineCtxParentId);
        boolean isRequired = !isForbidden && childNode.isPrereqRequired(lineCtxParentId);
        boolean isLink = childNode.isPrereqLink(lineCtxParentId);
        int menuW = 210, itemH = 14, pad = 4;
        int menuH = pad + 5 * itemH + pad;
        int lx = lineCtxX, ly = lineCtxY;
        if (lx + menuW > width) lx = width - menuW - 2;
        if (ly + menuH > height) ly = height - menuH - 2;

        if (mx < lx || mx >= lx + menuW || my < ly || my >= ly + menuH) {
            lineCtxOpen = false;
            return;
        }

        int idx = (my - ly - pad) / itemH;
        lineCtxOpen = false;

        if (idx == 0) {
            // Remove connection: child removes parentNode as a prereq; also remove parent→child edge
            childNode.removePrerequisite(parentNode);
            parentNode.removeChild(childNode);
            saveNodePrereqsToDisk(childNode);
            rebuild();
            setFeedback("Removed: " + parentNode.getTitle().getString() + " → " + childNode.getTitle().getString());
        } else if (idx == 1) {
            // 3-way cycle: required → optional → forbidden → required
            if (isForbidden) {
                childNode.setPrereqForbidden(lineCtxParentId, false);
                childNode.setPrereqRequired(lineCtxParentId, true);
                setFeedback("Prereq type: Required");
            } else if (!isRequired) {
                childNode.setPrereqForbidden(lineCtxParentId, true);
                setFeedback("Prereq type: Forbidden (must NOT be completed)");
            } else {
                childNode.setPrereqRequired(lineCtxParentId, false);
                setFeedback("Prereq type: Optional");
            }
            saveNodePrereqsToDisk(childNode);
            rebuild();
        } else if (idx == 2) {
            // Toggle link marker
            childNode.setPrereqLink(lineCtxParentId, !isLink);
            saveNodePrereqsToDisk(childNode);
            rebuild();
            setFeedback(isLink ? "Unmarked as link" : "Marked as link");
        } else if (idx == 3) {
            // Toggle dep line visibility for the child quest
            childNode.setHideDepLine(!childNode.isHideDepLine());
            saveNodeHideDepLineToDisk(childNode);
            rebuild();
            setFeedback(childNode.isHideDepLine()
                    ? "Dep lines hidden: " + childNode.getTitle().getString()
                    : "Dep lines shown: " + childNode.getTitle().getString());
        } else if (idx == 4) {
            // Open dep line settings screen
            final String cat = selectedCategory;
            if (minecraft != null) minecraft.setScreen(new DepLineSettingsScreen(this, cat));
        }
    }

    // ── Unlock path BFS ───────────────────────────────────────────────────────

    private void computeUnlockPath(QuestNode target) {
        unlockPathHighlight.clear();
        // BFS backwards through prerequisites until we hit completed/active nodes
        java.util.Queue<QuestNode> queue = new java.util.LinkedList<>();
        for (QuestNode prereq : target.getPrerequisites()) queue.add(prereq);
        java.util.Set<ResourceLocation> visited = new java.util.HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        PlayerQuestData data = mc.player == null ? null :
                mc.player.getCapability(
                        net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider.PLAYER_QUESTS)
                        .orElse(null);
        while (!queue.isEmpty()) {
            QuestNode n = queue.poll();
            if (!visited.add(n.getId())) continue;
            unlockPathHighlight.add(n.getId());
            // Stop following chain once we hit a completed/active node
            if (data != null) {
                QuestState st = data.getQuestState(n.getId(), QuestState.LOCKED);
                if (st == QuestState.COMPLETED || st == QuestState.ACTIVE) continue;
            }
            for (QuestNode req : n.getPrerequisites()) queue.add(req);
        }
    }

    // ── Validation panel ──────────────────────────────────────────────────────

    private void renderValidationPanel(GuiGraphics g, int cl, int cr) {
        int panW = Math.min(400, cr - cl - 20);
        int panX = cl + (cr - cl - panW) / 2;
        int panY = HEADER_H + 10;
        int panH = height - panY - 10;

        g.fill(panX, panY, panX + panW, panY + panH, 0xF00B0B12);
        g.fill(panX, panY, panX + panW, panY + 1, 0xFFFF4444);
        g.fill(panX, panY, panX + 1, panY + panH, 0xFFFF4444);
        g.fill(panX + panW - 1, panY, panX + panW, panY + panH, 0xFFFF4444);
        g.fill(panX, panY + panH - 1, panX + panW, panY + panH, 0xFFFF4444);

        g.drawString(font, "§cValidation Issues §8(V to close)", panX + 6, panY + 4, 0xFFFF6666, false);
        g.fill(panX + 4, panY + 14, panX + panW - 4, panY + 15, 0xFF333344);

        int vy = panY + 18;
        int maxY = panY + panH - 4;
        boolean any = false;
        g.enableScissor(panX + 2, panY + 16, panX + panW - 2, maxY);
        for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
            List<String> issues = getValidationIssues(node);
            if (issues.isEmpty()) continue;
            any = true;
            if (vy + 11 > maxY) break;
            g.drawString(font, "§e" + node.getTitle().getString() + " §8[" + node.getId().getPath() + "]",
                    panX + 6, vy, 0xFFFFCC44, false);
            vy += 10;
            for (String issue : issues) {
                if (vy + 9 > maxY) break;
                g.drawString(font, "§c  • " + issue, panX + 12, vy, 0xFFFF6666, false);
                vy += 9;
            }
        }
        g.disableScissor();
        if (!any) {
            g.drawString(font, "§aNo issues found!", panX + 6, panY + 20, 0xFF44CC88, false);
        }
    }

    @Override
    public void onClose() {
        PhantasiaCompat.closePreview(phantasiaPreview);
        phantasiaPreview = null;
        super.onClose();
    }

    public boolean isPauseScreen() {
        return false;
    }

    // ── Tutorial overlay ──────────────────────────────────────────────────────

    /** Finds the first ACTIVE/UNLOCKED quest that has tutorial steps and hasn't been dismissed. */
    private QuestNode findActiveTutorialQuest() {
        if (!TutorialProgressTracker.isInitialized()) {
            Path cfg = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles");
            TutorialProgressTracker.init(cfg);
        }
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            if (n.getTutorialSteps().isEmpty()) continue;
            String qid = n.getId().getPath();
            if (TutorialProgressTracker.isDismissed(qid)) continue;
            QuestState st = getState(n);
            if (st == QuestState.ACTIVE || st == QuestState.UNLOCKED) return n;
        }
        return null;
    }

    private void renderTutorialOverlay(GuiGraphics g, int mx, int my) {
        tutPrevBtn = null; tutNextBtn = null; tutSkipBtn = null;

        QuestNode tutQuest = findActiveTutorialQuest();
        if (tutQuest == null) return;

        java.util.List<TutorialStep> steps = tutQuest.getTutorialSteps();
        String qid = tutQuest.getId().getPath();
        int stepIdx = TutorialProgressTracker.getStep(qid);
        if (stepIdx < 0 || stepIdx >= steps.size()) return;
        TutorialStep step = steps.get(stepIdx);

        int cl = SIDEBAR_W, cr = width;

        // ── Spotlight dim ─────────────────────────────────────────────────────
        int hx = 0, hy = 0, hw = 0, hh = 0;
        if (step.hasHighlight()) {
            if (TutorialStep.HL_SIDEBAR.equals(step.highlight())) {
                hx = 0; hy = 0; hw = SIDEBAR_W; hh = height;
            } else if (TutorialStep.HL_CANVAS.equals(step.highlight())) {
                hx = cl; hy = HEADER_H; hw = cr - cl; hh = height - HEADER_H;
            } else if (TutorialStep.HL_TOOLBAR.equals(step.highlight())) {
                hx = 0; hy = TOOLBAR_Y; hw = width; hh = TOOLBAR_H;
            } else if (step.isNodeHighlight()) {
                String nid = step.nodeHighlightId();
                if (nid != null) {
                    ResourceLocation rid = new ResourceLocation("phoenixcore", nid);
                    int[] pos = nodeScreenPos.get(rid);
                    if (pos != null) {
                        int sz = scaledNodeSize();
                        hx = pos[0] - 4; hy = pos[1] - 4; hw = sz + 8; hh = sz + 8;
                    }
                }
            }
        }

        int dimColor = 0xBB000000;
        if (hw > 0) {
            // Four rectangles around the spotlight
            g.fill(0, 0, width, hy, dimColor);
            g.fill(0, hy + hh, width, height, dimColor);
            g.fill(0, hy, hx, hy + hh, dimColor);
            g.fill(hx + hw, hy, width, hy + hh, dimColor);
            // Glowing border around highlight
            g.fill(hx - 1, hy - 1, hx + hw + 1, hy, C_SEL_ACCENT);
            g.fill(hx - 1, hy + hh, hx + hw + 1, hy + hh + 1, C_SEL_ACCENT);
            g.fill(hx - 1, hy, hx, hy + hh, C_SEL_ACCENT);
            g.fill(hx + hw, hy, hx + hw + 1, hy + hh, C_SEL_ACCENT);
        } else {
            g.fill(0, 0, width, height, dimColor);
        }

        // ── Text box ──────────────────────────────────────────────────────────
        int boxW = Math.min(380, width - 40);
        int boxX = (width - boxW) / 2;

        // Word-wrap the step text
        java.util.List<String> wrappedLines = new java.util.ArrayList<>();
        String remaining = step.text();
        int maxLineW = boxW - 20;
        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= maxLineW) { wrappedLines.add(remaining); break; }
            String sub = font.plainSubstrByWidth(remaining, maxLineW);
            int sp = sub.lastIndexOf(' ');
            String lineOut = sp > 0 ? sub.substring(0, sp) : sub;
            wrappedLines.add(lineOut);
            remaining = remaining.substring(lineOut.length()).trim();
        }

        int textH = wrappedLines.size() * 11;
        int btnRowH = 18;
        int boxH = 14 + textH + 8 + btnRowH + 8;
        // Position above highlight if it's in the lower half, else below
        int boxY = (hw > 0 && hy + hh > height * 2 / 3)
                ? hy - boxH - 10
                : (hw > 0 ? hy + hh + 10 : height - boxH - 20);
        boxY = Math.max(HEADER_H + 4, Math.min(boxY, height - boxH - 4));

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF0E0E18);
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, C_SEL_ACCENT);
        g.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, C_BORDER);
        g.fill(boxX, boxY, boxX + 1, boxY + boxH, C_BORDER);
        g.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, C_BORDER);

        // Step counter
        String counter = "Step " + (stepIdx + 1) + " / " + steps.size();
        g.drawString(font, "§8" + counter, boxX + 10, boxY + 5, C_TEXT_FAINT, false);
        // Quest title
        g.drawString(font, "§7" + tutQuest.getTitle().getString(),
                boxX + boxW - font.width(tutQuest.getTitle().getString()) - 10, boxY + 5, C_TEXT_DIM, false);

        // Text lines
        int ty = boxY + 16;
        for (String line : wrappedLines) {
            g.drawString(font, "§f" + line, boxX + 10, ty, C_TEXT, false);
            ty += 11;
        }

        // ── Navigation buttons ────────────────────────────────────────────────
        int btnY = boxY + boxH - btnRowH - 5;
        int btnH = btnRowH - 2;

        // Skip (right-aligned)
        int skipW = font.width("Skip") + 12;
        int skipX = boxX + boxW - skipW - 6;
        tutSkipBtn = new int[]{skipX, btnY, skipX + skipW, btnY + btnH};
        boolean skipHov = mx >= skipX && mx < skipX + skipW && my >= btnY && my < btnY + btnH;
        g.fill(skipX, btnY, skipX + skipW, btnY + btnH, skipHov ? 0x33FFFFFF : 0x1AFFFFFF);
        g.drawCenteredString(font, "§8Skip", skipX + skipW / 2, btnY + 4, skipHov ? C_TEXT_DIM : C_TEXT_FAINT);

        // Next / Finish
        boolean isLast = stepIdx == steps.size() - 1;
        String nextLabel = isLast ? "§aFinish" : "§fNext →";
        int nextW = font.width(isLast ? "Finish" : "Next →") + 16;
        int nextX = skipX - nextW - 4;
        tutNextBtn = new int[]{nextX, btnY, nextX + nextW, btnY + btnH};
        boolean nextHov = mx >= nextX && mx < nextX + nextW && my >= btnY && my < btnY + btnH;
        g.fill(nextX, btnY, nextX + nextW, btnY + btnH, nextHov ? 0x55006633 : 0x2A006633);
        g.fill(nextX, btnY, nextX + nextW, btnY + 1, nextHov ? C_NBORD_DONE : 0xFF004422);
        g.drawCenteredString(font, nextLabel, nextX + nextW / 2, btnY + 4, nextHov ? C_NBORD_DONE : 0xFF55BB77);

        // Prev (only if not first step)
        if (stepIdx > 0) {
            int prevW = font.width("← Prev") + 12;
            int prevX = boxX + 6;
            tutPrevBtn = new int[]{prevX, btnY, prevX + prevW, btnY + btnH};
            boolean prevHov = mx >= prevX && mx < prevX + prevW && my >= btnY && my < btnY + btnH;
            g.fill(prevX, btnY, prevX + prevW, btnY + btnH, prevHov ? 0x33FFFFFF : 0x1AFFFFFF);
            g.drawCenteredString(font, "§8← Prev", prevX + prevW / 2, btnY + 4, prevHov ? C_TEXT_DIM : C_TEXT_FAINT);
        }
    }

    /** Called from mouseClicked — handles tutorial nav buttons before other handlers. */
    private boolean handleTutorialClick(double mx, double my) {
        if (tutNextBtn == null && tutPrevBtn == null && tutSkipBtn == null) return false;

        QuestNode tutQuest = findActiveTutorialQuest();
        if (tutQuest == null) return false;
        String qid = tutQuest.getId().getPath();

        if (tutNextBtn != null && mx >= tutNextBtn[0] && mx < tutNextBtn[2]
                && my >= tutNextBtn[1] && my < tutNextBtn[3]) {
            TutorialProgressTracker.advance(qid, tutQuest.getTutorialSteps().size());
            return true;
        }
        if (tutPrevBtn != null && mx >= tutPrevBtn[0] && mx < tutPrevBtn[2]
                && my >= tutPrevBtn[1] && my < tutPrevBtn[3]) {
            TutorialProgressTracker.back(qid);
            return true;
        }
        if (tutSkipBtn != null && mx >= tutSkipBtn[0] && mx < tutSkipBtn[2]
                && my >= tutSkipBtn[1] && my < tutSkipBtn[3]) {
            TutorialProgressTracker.dismiss(qid);
            return true;
        }

        return tutNextBtn != null;
    }
}
