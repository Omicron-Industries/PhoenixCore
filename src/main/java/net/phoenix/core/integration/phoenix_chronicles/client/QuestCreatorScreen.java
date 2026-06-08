package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Quest creator / editor screen.
 *
 * Saves two files per quest:
 * - <id>.snbt — structural data (parent, position, shape, category, tasks, state)
 * - <id>.md — human content (# Title heading + description body)
 *
 * Changes from original:
 * - Taller panel with proper ROW_GAP so inputs never feel smushed
 * - Dropdowns rendered at z=300 so they always appear above all widgets
 * - Category field has an inline "+ New" button to create new categories
 * - Shape and category dropdowns are properly separated visually
 */
public class QuestCreatorScreen extends Screen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COL_BG = 0xFF0F0F13;
    private static final int COL_PANEL = 0xFF16161C;
    private static final int COL_HEADER = 0xFF0C0C10;
    private static final int COL_BORDER = 0xFF2A2A36;
    private static final int COL_BORDER_LIT = 0xFF884499;
    private static final int COL_TEXT = 0xFFDDDDE8;
    private static final int COL_TEXT_DIM = 0xFF888898;
    private static final int COL_TEXT_FAINT = 0xFF4A4A5A;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_W = 280;
    private static final int HEADER_H = 22;
    // Each field row: label + edit box. FIELD_H is the edit box height.
    // ROW_STRIDE is the vertical distance between the tops of consecutive rows.
    private static final int FIELD_H = 18;    // taller than vanilla 14 — easier to click
    private static final int LABEL_H = 9;
    private static final int ROW_STRIDE = LABEL_H + FIELD_H + 8; // 35px between row tops
    private static final int MARGIN = 14;

    // Rows: ID, Title, Desc, Category+Shape, Parent, Tasks button
    private static final int NUM_ROWS = 6;
    // Panel height = header + top margin + rows + bottom button area
    private static final int PANEL_H = HEADER_H + 10 + NUM_ROWS * ROW_STRIDE + 6           // gap before save/cancel
            + 18          // save/cancel button height
            + 10;         // bottom padding

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final QuestNode editingNode;

    private String cachedId = "";
    private String cachedTitle = "";
    private String cachedDesc = "";
    private String cachedCategory = "MAIN";
    private String cachedShape = "SQUARE";
    private QuestNode cachedParent = null;

    private boolean initialized = false;

    // Widgets
    private EditBox idBox, titleBox, descBox, categoryBox;

    // Dropdowns
    private boolean shapeDropdownOpen = false;
    private boolean categoryDropdownOpen = false;
    private static final String[] SHAPES = {
            "SQUARE", "CIRCLE", "DIAMOND", "HEXAGON",
            "TRIANGLE", "STAR", "PENTAGON", "SHIELD", "CROSS"
    };

    // Status
    private String statusMsg = "";
    private boolean statusIsError = false;

    // Panel geometry (computed in init)
    private int panelLeft, panelTop;

    // ── Constructors ──────────────────────────────────────────────────────────

    public QuestCreatorScreen(Screen parent) {
        super(Component.literal("New Quest"));
        this.parent = parent;
        this.editingNode = null;
    }

    public QuestCreatorScreen(Screen parent, QuestNode editingNode) {
        super(Component.literal("Edit Quest"));
        this.parent = parent;
        this.editingNode = editingNode;

        cachedId = editingNode.getId().getPath();
        cachedTitle = editingNode.getTitle().getString();
        cachedDesc = editingNode.getDescription().getString();
        cachedCategory = editingNode.getCategory();
        cachedShape = editingNode.getShapeType() != null ? editingNode.getShapeType() : "SQUARE";
        if (!editingNode.getPrerequisites().isEmpty())
            cachedParent = editingNode.getPrerequisites().get(0);

        initialized = true;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();

        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;

        if (!initialized) {
            initialized = true;
        }

        int fx = panelLeft + MARGIN;
        int fw = PANEL_W - MARGIN * 2;
        // y0 = top of first label
        int y0 = panelTop + HEADER_H + 10;

        // ── Row 0: Quest ID ───────────────────────────────────────────────────
        int fieldY0 = y0 + LABEL_H + 2;
        idBox = new EditBox(font, fx, fieldY0, fw, FIELD_H, Component.empty());
        idBox.setMaxLength(128);
        idBox.setHint(Component.literal("§8e.g. chapter_1/signal_lost"));
        idBox.setValue(cachedId);
        idBox.setResponder(v -> cachedId = v);
        addRenderableWidget(idBox);

        // ── Row 1: Title ──────────────────────────────────────────────────────
        int fieldY1 = fieldY0 + ROW_STRIDE;
        titleBox = new EditBox(font, fx, fieldY1, fw, FIELD_H, Component.empty());
        titleBox.setMaxLength(64);
        titleBox.setHint(Component.literal("§8Display title"));
        titleBox.setValue(cachedTitle);
        titleBox.setResponder(v -> cachedTitle = v);
        addRenderableWidget(titleBox);

        // ── Row 2: Description ────────────────────────────────────────────────
        int fieldY2 = fieldY1 + ROW_STRIDE;
        descBox = new EditBox(font, fx, fieldY2, fw, FIELD_H, Component.empty());
        descBox.setMaxLength(512);
        descBox.setHint(Component.literal("§8Short description"));
        descBox.setValue(cachedDesc);
        descBox.setResponder(v -> cachedDesc = v);
        addRenderableWidget(descBox);

        // ── Row 3: Category + Shape ─────────────────────────────────────────
        // Layout: [category box][▾ picker][+ New] [shape dropdown trigger]
        // shape trigger is right-aligned at fixed width 80
        int fieldY3 = fieldY2 + ROW_STRIDE;
        int shapeW = 80;
        int shapeGap = 6;
        int newCatW = 36;
        int catPickW = 18;
        int catBoxW = fw - shapeW - shapeGap - catPickW - 2 - newCatW - 2;

        categoryBox = new EditBox(font, fx, fieldY3, catBoxW, FIELD_H, Component.empty());
        categoryBox.setMaxLength(32);
        categoryBox.setHint(Component.literal("§8MAIN, CHAPTER_1…"));
        categoryBox.setValue(cachedCategory);
        categoryBox.setResponder(v -> {
            cachedCategory = v;
            categoryDropdownOpen = false;
        });
        addRenderableWidget(categoryBox);

        // Category picker "▾"
        addRenderableWidget(Button.builder(Component.literal("§7▾"), b -> {
            categoryDropdownOpen = !categoryDropdownOpen;
            shapeDropdownOpen = false;
        }).bounds(fx + catBoxW + 2, fieldY3, catPickW, FIELD_H).build());

        // "+ New" category button — clears the box so you can type a fresh name
        addRenderableWidget(Button.builder(Component.literal("§a+New"), b -> {
            categoryDropdownOpen = false;
            shapeDropdownOpen = false;
            cachedCategory = "";
            if (categoryBox != null) {
                categoryBox.setValue("");
                categoryBox.setFocused(true);
            }
        }).bounds(fx + catBoxW + 2 + catPickW + 2, fieldY3, newCatW, FIELD_H).build());

        // Shape dropdown trigger
        addRenderableWidget(Button.builder(
                Component.literal("§7" + cachedShape + " ▾"),
                b -> {
                    shapeDropdownOpen = !shapeDropdownOpen;
                    categoryDropdownOpen = false;
                }).bounds(fx + fw - shapeW, fieldY3, shapeW, FIELD_H).build());

        // ── Row 4: Parent selector ────────────────────────────────────────────
        int fieldY4 = fieldY3 + ROW_STRIDE;
        String parentLabel = cachedParent != null ? "§a" + cachedParent.getId().getPath() : "§8No parent (root)";
        addRenderableWidget(Button.builder(Component.literal(parentLabel), b -> {
            shapeDropdownOpen = false;
            categoryDropdownOpen = false;
            Minecraft.getInstance().setScreen(new ParentSelectorScreen(this, editingNode, node -> {
                cachedParent = node;
                rebuildWidgets();
            }));
        }).bounds(fx, fieldY4, fw - FIELD_H - 4, FIELD_H).build());

        addRenderableWidget(Button.builder(Component.literal("§c×"), b -> {
            cachedParent = null;
            rebuildWidgets();
        }).bounds(fx + fw - FIELD_H, fieldY4, FIELD_H, FIELD_H).build());

        // ── Row 5: Tasks & Rewards ────────────────────────────────────────────
        int fieldY5 = fieldY4 + ROW_STRIDE;
        addRenderableWidget(Button.builder(
                Component.literal("§7⊞ Tasks & Rewards…"),
                b -> {
                    shapeDropdownOpen = false;
                    categoryDropdownOpen = false;
                    QuestNode targetNode = editingNode;
                    if (targetNode == null) {
                        String id = cachedId.trim().isEmpty() ? "_new_quest_" : cachedId.trim();
                        targetNode = new net.phoenix.core.integration.phoenix_chronicles.QuestNode(
                                new ResourceLocation("phoenixcore", id),
                                Component.literal(cachedTitle),
                                Component.literal(cachedDesc));
                    }
                    Minecraft.getInstance().setScreen(new TaskRewardEditorScreen(this, targetNode));
                }).bounds(fx, fieldY5, fw, FIELD_H).build());

        // ── Save / Cancel buttons ─────────────────────────────────────────────
        int btnY = panelTop + PANEL_H - 10 - 18;
        int halfW = (fw - 6) / 2;
        addRenderableWidget(Button.builder(
                Component.literal("§aSave quest"),
                b -> save()).bounds(fx, btnY, halfW, 18).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7Cancel"),
                b -> {
                    if (minecraft != null) minecraft.setScreen(parent);
                }).bounds(fx + halfW + 6, btnY, halfW, 18).build());
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        // dim background
        g.fill(0, 0, width, height, 0x88000000);

        // Panel background + border
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL);
        drawBorder(g, panelLeft, panelTop, PANEL_W, PANEL_H, COL_BORDER_LIT);

        // Header
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + HEADER_H, COL_HEADER);
        g.fill(panelLeft, panelTop + HEADER_H - 1, panelLeft + PANEL_W, panelTop + HEADER_H, COL_BORDER);
        String heading = editingNode != null ? "§dEdit Quest" : "§dNew Quest";
        g.drawCenteredString(font, heading, panelLeft + PANEL_W / 2, panelTop + 7, COL_TEXT);

        // Field labels
        int fx = panelLeft + MARGIN;
        int y0 = panelTop + HEADER_H + 10;
        g.drawString(font, "§8Quest ID", fx, y0, COL_TEXT_FAINT);
        g.drawString(font, "§8Title", fx, y0 + ROW_STRIDE, COL_TEXT_FAINT);
        g.drawString(font, "§8Description", fx, y0 + ROW_STRIDE * 2, COL_TEXT_FAINT);
        g.drawString(font, "§8Category  §8│  §8Shape", fx, y0 + ROW_STRIDE * 3, COL_TEXT_FAINT);
        g.drawString(font, "§8Parent node", fx, y0 + ROW_STRIDE * 4, COL_TEXT_FAINT);
        g.drawString(font, "§8Tasks & rewards", fx, y0 + ROW_STRIDE * 5, COL_TEXT_FAINT);

        // Status
        if (!statusMsg.isEmpty()) {
            g.drawCenteredString(font, (statusIsError ? "§c" : "§a") + statusMsg,
                    panelLeft + PANEL_W / 2, panelTop + PANEL_H - 32, 0xFFFFFFFF);
        }

        // Render widgets first, then overlays on top
        super.render(g, mx, my, partial);

        // ── Dropdowns rendered at elevated z so they are above all widgets ─────
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        // Shape dropdown
        if (shapeDropdownOpen) {
            int fw = PANEL_W - MARGIN * 2;
            int shapeW = 80;
            int dx = panelLeft + MARGIN + fw - shapeW;
            int dy = panelTop + HEADER_H + 10 + ROW_STRIDE * 3 + LABEL_H + 2 + FIELD_H + 1;
            int dropH = SHAPES.length * 16;
            g.fill(dx, dy, dx + shapeW, dy + dropH, COL_PANEL);
            drawBorder(g, dx, dy, shapeW, dropH, COL_BORDER_LIT);
            for (int i = 0; i < SHAPES.length; i++) {
                int rowY = dy + i * 16;
                boolean hovered = mx >= dx && mx <= dx + shapeW && my >= rowY && my <= rowY + 16;
                if (hovered) g.fill(dx + 1, rowY, dx + shapeW - 1, rowY + 16, 0xFF1E1E2A);
                g.drawString(font, "§7" + SHAPES[i], dx + 6, rowY + 4, hovered ? COL_TEXT : COL_TEXT_DIM);
            }
        }

        // Category dropdown
        if (categoryDropdownOpen) {
            List<String> existingCats = buildExistingCategories();
            int fw = PANEL_W - MARGIN * 2;
            int shapeW = 80;
            int shapeGap = 6;
            int newCatW = 36;
            int catPickW = 18;
            int catBoxW = fw - shapeW - shapeGap - catPickW - 2 - newCatW - 2;
            int dx = panelLeft + MARGIN;
            int dy = panelTop + HEADER_H + 10 + ROW_STRIDE * 3 + LABEL_H + 2 + FIELD_H + 1;
            int dropW = catBoxW + catPickW + 4;
            int dropH = existingCats.size() * 16;
            if (dropH == 0) dropH = 16; // show at least empty state
            g.fill(dx, dy, dx + dropW, dy + dropH, COL_PANEL);
            drawBorder(g, dx, dy, dropW, dropH, COL_BORDER_LIT);
            if (existingCats.isEmpty()) {
                g.drawString(font, "§8No categories yet", dx + 5, dy + 4, COL_TEXT_FAINT);
            } else {
                for (int i = 0; i < existingCats.size(); i++) {
                    int rowY = dy + i * 16;
                    boolean hovered = mx >= dx && mx <= dx + dropW && my >= rowY && my <= rowY + 16;
                    if (hovered) g.fill(dx + 1, rowY, dx + dropW - 1, rowY + 16, 0xFF1E1E2A);
                    g.drawString(font, "§7" + existingCats.get(i), dx + 5, rowY + 4,
                            hovered ? COL_TEXT : COL_TEXT_DIM);
                }
            }
        }

        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            // Shape dropdown click
            if (shapeDropdownOpen) {
                int fw = PANEL_W - MARGIN * 2;
                int shapeW = 80;
                int dx = panelLeft + MARGIN + fw - shapeW;
                int dy = panelTop + HEADER_H + 10 + ROW_STRIDE * 3 + LABEL_H + 2 + FIELD_H + 1;
                for (int i = 0; i < SHAPES.length; i++) {
                    int rowY = dy + i * 16;
                    if (mx >= dx && mx <= dx + shapeW && my >= rowY && my <= rowY + 16) {
                        cachedShape = SHAPES[i];
                        shapeDropdownOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                shapeDropdownOpen = false;
                rebuildWidgets();
                return true;
            }

            // Category dropdown click
            if (categoryDropdownOpen) {
                List<String> existingCats = buildExistingCategories();
                int fw = PANEL_W - MARGIN * 2;
                int shapeW = 80;
                int shapeGap = 6;
                int newCatW = 36;
                int catPickW = 18;
                int catBoxW = fw - shapeW - shapeGap - catPickW - 2 - newCatW - 2;
                int dx = panelLeft + MARGIN;
                int dy = panelTop + HEADER_H + 10 + ROW_STRIDE * 3 + LABEL_H + 2 + FIELD_H + 1;
                int dropW = catBoxW + catPickW + 4;
                for (int i = 0; i < existingCats.size(); i++) {
                    int rowY = dy + i * 16;
                    if (mx >= dx && mx <= dx + dropW && my >= rowY && my <= rowY + 16) {
                        cachedCategory = existingCats.get(i);
                        if (categoryBox != null) categoryBox.setValue(cachedCategory);
                        categoryDropdownOpen = false;
                        return true;
                    }
                }
                categoryDropdownOpen = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void save() {
        String id = cachedId.trim().toLowerCase().replaceAll("[^a-z0-9/._-]", "");
        String title = cachedTitle.trim();
        String desc = cachedDesc.trim();
        String category = cachedCategory.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
        if (category.isEmpty()) category = "MAIN";

        if (id.isEmpty() || title.isEmpty()) {
            statusMsg = "ID and Title are required";
            statusIsError = true;
            return;
        }

        Path baseDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");

        try {
            // ── Write .snbt ───────────────────────────────────────────────────
            // Include ALL fields that QuestFileLoader reads so the quest
            // fully survives a world restart without any data loss.
            CompoundTag snbt = new CompoundTag();
            snbt.putString("id", id);
            snbt.putString("title", title);           // ← was missing
            snbt.putString("description", desc);            // ← was missing
            snbt.putString("category", category);
            snbt.putString("shape", cachedShape);
            snbt.putString("parent", cachedParent != null ? cachedParent.getId().getPath() : "none");
            snbt.putInt("positionX", editingNode != null ? editingNode.getCustomX() : 40);
            snbt.putInt("positionY", editingNode != null ? editingNode.getCustomY() : 70);

            // Preserve existing icon_item if we're editing and none was explicitly changed
            if (editingNode != null && !editingNode.getIconItemId().isEmpty()) {
                snbt.putString("icon_item", editingNode.getIconItemId());
            }

            // Use SNBT pretty format so the file is human-readable
            String snbtText = snbt.toString();
            Path snbtPath = baseDir.resolve(id + ".snbt");
            Files.createDirectories(snbtPath.getParent());
            Files.writeString(snbtPath, snbtText, StandardCharsets.UTF_8);

            // ── Write .md (human-readable content companion) ──────────────────
            Files.writeString(baseDir.resolve(id + ".md"),
                    "# " + title + "\n\n" + desc + "\n", StandardCharsets.UTF_8);

            // ── Reload this specific quest into the live registry ─────────────
            // Rather than manually constructing and injecting, we do a targeted
            // reload so the in-memory state always matches what's on disk.
            ResourceLocation questId = new ResourceLocation("phoenixcore", id);
            ResourceLocation parentLoc = cachedParent != null ? cachedParent.getId() : null;

            QuestNode node = new QuestNode(questId, Component.literal(title), Component.literal(desc));
            node.setCategory(category);
            node.setShapeType(cachedShape);
            node.setCustomPosition(
                    editingNode != null ? editingNode.getCustomX() : 40,
                    editingNode != null ? editingNode.getCustomY() : 70);
            if (editingNode != null && editingNode.getIconItem() != null)
                node.setIconItem(editingNode.getIconItem());

            QuestTreeRegistry.injectDynamicQuestNode(node, parentLoc);

            statusMsg = "Saved!";
            statusIsError = false;

        } catch (IOException e) {
            statusMsg = "IO error: " + e.getMessage();
            statusIsError = true;
            e.printStackTrace();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Collects all unique categories already in the registry (excluding ALL). */
    private List<String> buildExistingCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("MAIN");
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            String c = n.getCategory();
            if (c != null && !cats.contains(c)) cats.add(c);
        }
        return cats;
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
