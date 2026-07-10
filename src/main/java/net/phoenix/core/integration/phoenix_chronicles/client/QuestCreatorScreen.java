package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.ChroniclesTheme;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuestCreatorScreen extends Screen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private int C_BG, C_PANEL, C_HEADER, C_BORDER, C_ACCENT, C_TEXT, C_TEXT_DIM, C_TEXT_FAINT, C_OK;
    private static final int C_ERR = 0xFFCC4444;
    private static final int C_SHAPE_SEL = 0x775533AA;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 32;
    private static final int MARGIN = 14;
    private static final int MAX_W = 520;
    private static final int LABEL_H = 8;
    private static final int FIELD_H = 16;
    private static final int ROW_GAP = 10;
    private static final int STRIDE = LABEL_H + 3 + FIELD_H + ROW_GAP; // 37
    private static final int DIV_H = 14; // divider between sections
    private static final int EDIT_W = 20;
    private static final int COL_GAP = 8;
    private static final int SEC_PAD = 6;  // panel padding around section rows

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private static final String[] TAB_LABELS = { "Info", "Settings", "Advanced" };
    private static final int TAB_H = 20;

    // ── Shapes ───────────────────────────────────────────────────────────────
    private record ShapeMeta(String id, String glyph) {}

    private static final ShapeMeta[] SHAPES = {
            new ShapeMeta("SQUARE", "■"), new ShapeMeta("CIRCLE", "●"),
            new ShapeMeta("DIAMOND", "◆"), new ShapeMeta("HEXAGON", "⬡"),
            new ShapeMeta("TRIANGLE", "▲"), new ShapeMeta("STAR", "★"),
            new ShapeMeta("PENTAGON", "⬠"), new ShapeMeta("SHIELD", "❖"),
            new ShapeMeta("CROSS", "✚"),
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final QuestNode editingNode;

    private String cachedTitle = "";
    private String cachedDesc = "";
    private String cachedSubtitle = "";
    private String cachedCategory = "MAIN";
    private String cachedIconItemId = "";
    private String cachedShape = "SQUARE";
    private QuestNode.Visibility cachedVisibility = QuestNode.Visibility.NORMAL;
    private String cachedEnableIf = "";
    private boolean cachedRequireAll = true;
    private boolean cachedDisabledBlocksChildren = false;
    private QuestNode cachedParent = null;
    private int cachedTaskMinCount = 0;
    private String cachedId = "";
    private boolean idManuallySet = false;
    private boolean initialized = false;
    private QuestNode.RepeatMode cachedRepeatMode = QuestNode.RepeatMode.NONE;
    private int cachedRepeatCooldownHours = 24;
    private boolean cachedHideDepLine = false;
    private int cachedPosX = 40;
    private int cachedPosY = 70;

    // Widgets
    private EditBox titleBox, descBox, subtitleBox, categoryBox, idBox, posXBox, posYBox;

    // Dropdowns
    private boolean visibilityDropdownOpen = false;
    private boolean categoryDropdownOpen = false;
    private static final QuestNode.Visibility[] VISIBILITIES = QuestNode.Visibility.values();

    // Status
    private String statusMsg = "";
    private boolean statusIsErr = false;

    // Computed geometry (set in init, used in render + mouseClicked)
    private int cx, cw;                    // content x and width
    private int[] fieldY;                  // field top y for each row (0-8)
    private int secPanelTop, secPanelBot;  // active tab section panel bounds
    private int activeTab = 0;             // 0=Info 1=Settings 2=Advanced

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
        cachedSubtitle = editingNode.getSubtitle() != null ? editingNode.getSubtitle() : "";
        cachedCategory = editingNode.getCategory();
        cachedIconItemId = editingNode.getIconItemId();
        cachedShape = editingNode.getShapeType() != null ? editingNode.getShapeType() : "SQUARE";
        cachedVisibility = editingNode.getVisibility() != null ? editingNode.getVisibility() :
                QuestNode.Visibility.NORMAL;
        cachedEnableIf = editingNode.getEnableIf() != null ? editingNode.getEnableIf() : "";
        cachedRequireAll = editingNode.getRequireAllPrerequisites();
        cachedDisabledBlocksChildren = editingNode.isDisabledBlocksChildren();
        cachedTaskMinCount = editingNode.getTaskMinCount();
        cachedRepeatMode = editingNode.getRepeatMode() != null ? editingNode.getRepeatMode() :
                QuestNode.RepeatMode.NONE;
        cachedRepeatCooldownHours = editingNode.getRepeatCooldownHours();
        cachedHideDepLine = editingNode.isHideDepLine();
        cachedPosX = editingNode.getCustomX();
        cachedPosY = editingNode.getCustomY();
        if (!editingNode.getPrerequisites().isEmpty())
            cachedParent = editingNode.getPrerequisites().get(0);
        idManuallySet = true;
        initialized = true;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        ChroniclesTheme t = ChroniclesTheme.current();
        C_BG = t.bg.getColor();
        C_PANEL = t.panel.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_ACCENT = t.accent.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        C_OK = t.done.getColor();

        clearWidgets();
        if (!initialized) initialized = true;

        cw = Math.min(width - MARGIN * 2, MAX_W);
        cx = (width - cw) / 2;

        fieldY = new int[12];

        // ── Tab buttons ───────────────────────────────────────────────────────
        String[] tabTooltips = {
                "Title, description, category, icon and shape",
                "Visibility, prerequisites, completion gate and parent quest",
                "Quest ID and task/reward editor"
        };
        int tabW = cw / TAB_LABELS.length;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int ti = i;
            addRenderableWidget(Button.builder(
                    Component.literal((activeTab == i ? "§f" : "§8") + TAB_LABELS[i]),
                    b -> {
                        activeTab = ti;
                        rebuildWidgets();
                    })
                    .bounds(cx + i * tabW, HEADER_H + 3, tabW - 2, TAB_H - 6)
                    .tooltip(Tooltip.create(Component.literal(tabTooltips[i]))).build());
        }

        // ── Adaptive content geometry ─────────────────────────────────────────
        int contentTop = HEADER_H + TAB_H + 4;
        int contentBottom = height - FOOTER_H - 4;
        int tabRows = 4;
        int availH = contentBottom - contentTop - SEC_PAD * 2;
        int dynStride = Math.max(28, Math.min(STRIDE, availH / tabRows));

        int y = contentTop + SEC_PAD;
        secPanelTop = contentTop;

        // ── Tab 0: Basic Info ─────────────────────────────────────────────────
        if (activeTab == 0) {
            // Row 0: Title
            fieldY[0] = y + LABEL_H + 2;
            titleBox = new EditBox(font, cx, fieldY[0], cw - EDIT_W - 2, FIELD_H, Component.empty());
            titleBox.setMaxLength(64);
            titleBox.setHint(Component.literal("§8Quest title shown to players"));
            titleBox.setValue(cachedTitle);
            titleBox.setResponder(v -> {
                cachedTitle = v;
                if (!idManuallySet) {
                    cachedId = v.trim().toLowerCase().replaceAll("[^a-z0-9 /._-]", "").replaceAll("\\s+", "_");
                    if (idBox != null) idBox.setValue(cachedId);
                }
            });
            addRenderableWidget(titleBox);
            addRenderableWidget(Button.builder(Component.literal("§7✎"),
                    b -> Minecraft.getInstance().setScreen(new QuestTextInputScreen(this, "Title", cachedTitle, 64,
                            v -> {
                                cachedTitle = v;
                                if (titleBox != null) titleBox.setValue(v);
                            })))
                    .bounds(cx + cw - EDIT_W, fieldY[0], EDIT_W, FIELD_H).build());
            y += dynStride;

            // Row 1: Description
            fieldY[1] = y + LABEL_H + 2;
            descBox = new EditBox(font, cx, fieldY[1], cw - EDIT_W - 2, FIELD_H, Component.empty());
            descBox.setMaxLength(512);
            descBox.setHint(Component.literal("§8Short description / lore text"));
            descBox.setValue(cachedDesc);
            descBox.setResponder(v -> cachedDesc = v);
            addRenderableWidget(descBox);
            addRenderableWidget(Button.builder(Component.literal("§7✎"),
                    b -> Minecraft.getInstance()
                            .setScreen(new QuestTextInputScreen(this, "Description", cachedDesc, 512,
                                    v -> {
                                        cachedDesc = v;
                                        if (descBox != null) descBox.setValue(v);
                                    })))
                    .bounds(cx + cw - EDIT_W, fieldY[1], EDIT_W, FIELD_H).build());
            y += dynStride;

            // Row 2: Category (55%) | Subtitle (45%)
            fieldY[2] = y + LABEL_H + 2;
            int catW = (int) (cw * 0.55f);
            int subW = cw - catW - COL_GAP;
            int subX = cx + catW + COL_GAP;
            int catPickW = 16, newCatW = 32;
            int catBoxW = catW - catPickW - 2 - newCatW - 2;
            categoryBox = new EditBox(font, cx, fieldY[2], catBoxW, FIELD_H, Component.empty());
            categoryBox.setMaxLength(32);
            categoryBox.setHint(Component.literal("§8MAIN  CHAPTER_1  …"));
            categoryBox.setValue(cachedCategory);
            categoryBox.setResponder(v -> {
                cachedCategory = v;
                categoryDropdownOpen = false;
            });
            addRenderableWidget(categoryBox);
            addRenderableWidget(Button.builder(Component.literal("§7▾"), b -> {
                categoryDropdownOpen = !categoryDropdownOpen;
                visibilityDropdownOpen = false;
            }).bounds(cx + catBoxW + 2, fieldY[2], catPickW, FIELD_H).build());
            addRenderableWidget(Button.builder(Component.literal("§a+New"), b -> {
                categoryDropdownOpen = false;
                cachedCategory = "";
                if (categoryBox != null) {
                    categoryBox.setValue("");
                    categoryBox.setFocused(true);
                }
            }).bounds(cx + catBoxW + 2 + catPickW + 2, fieldY[2], newCatW, FIELD_H).build());
            subtitleBox = new EditBox(font, subX, fieldY[2], subW - EDIT_W - 2, FIELD_H, Component.empty());
            subtitleBox.setMaxLength(128);
            subtitleBox.setHint(Component.literal("§8Subtitle…"));
            subtitleBox.setValue(cachedSubtitle);
            subtitleBox.setResponder(v -> cachedSubtitle = v);
            addRenderableWidget(subtitleBox);
            addRenderableWidget(Button.builder(Component.literal("§7✎"),
                    b -> Minecraft.getInstance()
                            .setScreen(new QuestTextInputScreen(this, "Subtitle", cachedSubtitle, 128,
                                    v -> {
                                        cachedSubtitle = v;
                                        if (subtitleBox != null) subtitleBox.setValue(v);
                                    })))
                    .bounds(subX + subW - EDIT_W, fieldY[2], EDIT_W, FIELD_H).build());
            y += dynStride;

            // Row 3: Icon (35%) | Shape (65%)
            fieldY[3] = y + LABEL_H + 2;
            int iconColW = (int) (cw * 0.35f);
            int shapeColW = cw - iconColW - COL_GAP;
            int shapeX = cx + iconColW + COL_GAP;
            net.minecraft.world.item.Item iconItem = cachedIconItemId.isBlank() ? null :
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(cachedIconItemId));
            String iconBtnLabel = (iconItem != null && iconItem != net.minecraft.world.item.Items.AIR) ?
                    "§f" + new net.minecraft.world.item.ItemStack(iconItem).getHoverName().getString() : "§8Pick icon…";
            addRenderableWidget(Button.builder(Component.literal(iconBtnLabel), b -> {
                if (minecraft != null) minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    cachedIconItemId = id != null ? id.toString() : "";
                    rebuildWidgets();
                }));
            }).bounds(cx, fieldY[3], iconColW - EDIT_W - 2, FIELD_H).build());
            addRenderableWidget(Button.builder(Component.literal("§c×"), b -> {
                cachedIconItemId = "";
                rebuildWidgets();
            }).bounds(cx + iconColW - EDIT_W, fieldY[3], EDIT_W, FIELD_H).build());
            int shapeSlot = shapeColW / SHAPES.length;
            for (int i = 0; i < SHAPES.length; i++) {
                ShapeMeta sm = SHAPES[i];
                boolean sel = sm.id().equals(cachedShape);
                addRenderableWidget(Button.builder(
                        Component.literal((sel ? "§d" : "§7") + sm.glyph()),
                        b -> {
                            cachedShape = sm.id();
                            rebuildWidgets();
                        })
                        .bounds(shapeX + i * shapeSlot, fieldY[3], shapeSlot - 1, FIELD_H).build());
            }
            secPanelBot = fieldY[3] + FIELD_H + SEC_PAD;
        }

        // ── Tab 1: Quest Settings ──────────────────────────────────────────────
        if (activeTab == 1) {
            // Row 4: Visibility | Prereq gate | [Blocks children]
            fieldY[4] = y + LABEL_H + 2;
            int visW = 90;
            addRenderableWidget(Button.builder(
                    Component.literal("§7" + cachedVisibility.name() + " §8▾"),
                    b -> {
                        visibilityDropdownOpen = !visibilityDropdownOpen;
                        categoryDropdownOpen = false;
                    })
                    .bounds(cx, fieldY[4], visW, FIELD_H).build());
            boolean showBlock = cachedVisibility == QuestNode.Visibility.DISABLED;
            int blockW = showBlock ? 90 : 0;
            int prereqW = cw - visW - COL_GAP - (showBlock ? blockW + COL_GAP : 0);
            String prereqLabel = cachedRequireAll ? "§a✔ ALL prereqs required" : "§e◑ ANY prereq sufficient";
            addRenderableWidget(Button.builder(Component.literal(prereqLabel),
                    b -> {
                        cachedRequireAll = !cachedRequireAll;
                        rebuildWidgets();
                    })
                    .bounds(cx + visW + COL_GAP, fieldY[4], prereqW, FIELD_H).build());
            if (showBlock) {
                String blkLabel = cachedDisabledBlocksChildren ? "§eBlocks children" : "§8Blocks children";
                addRenderableWidget(Button.builder(Component.literal(blkLabel),
                        b -> {
                            cachedDisabledBlocksChildren = !cachedDisabledBlocksChildren;
                            rebuildWidgets();
                        })
                        .bounds(cx + visW + COL_GAP + prereqW + COL_GAP, fieldY[4], blockW, FIELD_H).build());
            }
            y += dynStride;

            // Row 5: Task completion gate
            fieldY[5] = y + LABEL_H + 2;
            boolean anyMode = cachedTaskMinCount > 0;
            String gateLabel = anyMode ? "§e◑ Complete any " + cachedTaskMinCount + " task(s)" :
                    "§a✔ Complete all tasks";
            addRenderableWidget(Button.builder(Component.literal(gateLabel), b -> {
                cachedTaskMinCount = cachedTaskMinCount == 0 ? 1 : 0;
                rebuildWidgets();
            }).bounds(cx, fieldY[5], anyMode ? cw - 50 : cw, FIELD_H).build());
            if (anyMode) {
                addRenderableWidget(Button.builder(Component.literal("§7−"), b -> {
                    if (cachedTaskMinCount > 1) cachedTaskMinCount--;
                    rebuildWidgets();
                }).bounds(cx + cw - 48, fieldY[5], 22, FIELD_H).build());
                addRenderableWidget(Button.builder(Component.literal("§7+"), b -> {
                    cachedTaskMinCount++;
                    rebuildWidgets();
                }).bounds(cx + cw - 24, fieldY[5], 22, FIELD_H).build());
            }
            y += dynStride;

            // Row 6: Parent (60%) | enable_if (40%)
            fieldY[6] = y + LABEL_H + 2;
            int parentW = (int) (cw * 0.60f);
            int enableIfW = cw - parentW - COL_GAP;
            int enableIfX = cx + parentW + COL_GAP;
            String parentLabel = cachedParent != null ? "§a" + cachedParent.getId().getPath() : "§8No parent quest";
            addRenderableWidget(Button.builder(Component.literal(parentLabel), b -> {
                categoryDropdownOpen = false;
                visibilityDropdownOpen = false;
                Minecraft.getInstance().setScreen(new ParentSelectorScreen(this, editingNode, node -> {
                    cachedParent = node;
                    if (node != null && (cachedCategory.equals("MAIN") || cachedCategory.isBlank()))
                        cachedCategory = node.getCategory();
                    rebuildWidgets();
                }));
            }).bounds(cx, fieldY[6], parentW - FIELD_H - 4, FIELD_H).build());
            addRenderableWidget(Button.builder(Component.literal("§c×"), b -> {
                cachedParent = null;
                rebuildWidgets();
            }).bounds(cx + parentW - FIELD_H, fieldY[6], FIELD_H, FIELD_H).build());
            EditBox enableIfBox = new EditBox(font, enableIfX, fieldY[6], enableIfW, FIELD_H, Component.empty());
            enableIfBox.setMaxLength(128);
            enableIfBox.setHint(Component.literal("§8enable_if…"));
            enableIfBox.setValue(cachedEnableIf);
            enableIfBox.setResponder(v -> cachedEnableIf = v);
            addRenderableWidget(enableIfBox);
            y += dynStride;

            // Row 7 (new): Repeat mode + cooldown hours
            fieldY[9] = y + LABEL_H + 2;
            boolean hasCooldown = cachedRepeatMode == QuestNode.RepeatMode.COOLDOWN;
            int repeatBtnW = hasCooldown ? (int) (cw * 0.50f) : cw;
            String repeatIcon = switch (cachedRepeatMode) {
                case NONE -> "§8⊘ One-time  §8▸";
                case DAILY -> "§b☀ Daily  §8▸";
                case COOLDOWN -> "§e⏱ Cooldown  §8▸";
                case INFINITE -> "§a∞ Infinite  §8▸";
            };
            addRenderableWidget(Button.builder(Component.literal(repeatIcon), b -> {
                QuestNode.RepeatMode[] modes = QuestNode.RepeatMode.values();
                cachedRepeatMode = modes[(cachedRepeatMode.ordinal() + 1) % modes.length];
                rebuildWidgets();
            }).bounds(cx, fieldY[9], repeatBtnW, FIELD_H)
                    .tooltip(Tooltip.create(Component.literal(
                            "NONE = one-time only  ·  DAILY = resets at midnight  ·  COOLDOWN = custom wait  ·  INFINITE = repeats immediately")))
                    .build());
            if (hasCooldown) {
                int coolW = cw - repeatBtnW - COL_GAP;
                int coolX = cx + repeatBtnW + COL_GAP;
                addRenderableWidget(Button.builder(Component.literal("§7−"), b -> {
                    if (cachedRepeatCooldownHours > 1) cachedRepeatCooldownHours--;
                    rebuildWidgets();
                }).bounds(coolX, fieldY[9], 18, FIELD_H).build());
                addRenderableWidget(Button.builder(Component.literal("§7+"), b -> {
                    cachedRepeatCooldownHours++;
                    rebuildWidgets();
                }).bounds(coolX + coolW - 18, fieldY[9], 18, FIELD_H).build());
            }
            secPanelBot = fieldY[9] + FIELD_H + SEC_PAD;
        }

        // ── Tab 2: Advanced ───────────────────────────────────────────────────
        if (activeTab == 2) {
            // Row 7: Quest ID
            fieldY[7] = y + LABEL_H + 2;
            int lockW = 36;
            idBox = new EditBox(font, cx, fieldY[7], cw - lockW - 2, FIELD_H, Component.empty());
            idBox.setMaxLength(128);
            idBox.setHint(Component.literal("§8auto-generated from title"));
            idBox.setValue(cachedId);
            idBox.setResponder(v -> {
                cachedId = v;
                idManuallySet = !v.isEmpty();
            });
            addRenderableWidget(idBox);
            addRenderableWidget(Button.builder(
                    Component.literal(idManuallySet ? "§cLocked" : "§aAuto"),
                    b -> {
                        idManuallySet = !idManuallySet;
                        if (!idManuallySet) {
                            cachedId = cachedTitle.trim().toLowerCase()
                                    .replaceAll("[^a-z0-9 /._-]", "").replaceAll("\\s+", "_");
                            if (idBox != null) idBox.setValue(cachedId);
                        }
                        rebuildWidgets();
                    }).bounds(cx + cw - lockW, fieldY[7], lockW, FIELD_H).build());
            y += dynStride;

            // Row 8: Tasks & Rewards
            fieldY[8] = y + LABEL_H + 2;
            addRenderableWidget(Button.builder(Component.literal("§7⊞ Tasks & Rewards…"), b -> {
                categoryDropdownOpen = false;
                visibilityDropdownOpen = false;
                QuestNode target = editingNode;
                if (target == null) {
                    String id = cachedId.trim().isEmpty() ? "_preview_" : cachedId.trim();
                    target = new net.phoenix.core.integration.phoenix_chronicles.QuestNode(
                            new ResourceLocation("phoenixcore", id),
                            Component.literal(cachedTitle), Component.literal(cachedDesc));
                }
                Minecraft.getInstance().setScreen(new TaskRewardEditorScreen(this, target));
            }).bounds(cx, fieldY[8], cw, FIELD_H).build());
            y += dynStride;

            // Row (new): Canvas position X / Y
            fieldY[10] = y + LABEL_H + 2;
            int halfPosW = (cw - COL_GAP) / 2;
            posXBox = new EditBox(font, cx, fieldY[10], halfPosW, FIELD_H, Component.empty());
            posXBox.setMaxLength(6);
            posXBox.setHint(Component.literal("§8X"));
            posXBox.setValue(String.valueOf(cachedPosX));
            posXBox.setResponder(v -> {
                try {
                    cachedPosX = Integer.parseInt(v.trim());
                } catch (Exception ignored) {}
            });
            addRenderableWidget(posXBox);
            posYBox = new EditBox(font, cx + halfPosW + COL_GAP, fieldY[10], halfPosW, FIELD_H, Component.empty());
            posYBox.setMaxLength(6);
            posYBox.setHint(Component.literal("§8Y"));
            posYBox.setValue(String.valueOf(cachedPosY));
            posYBox.setResponder(v -> {
                try {
                    cachedPosY = Integer.parseInt(v.trim());
                } catch (Exception ignored) {}
            });
            addRenderableWidget(posYBox);
            y += dynStride;

            // Row (new): Hide dep line + children count (read-only)
            fieldY[11] = y + LABEL_H + 2;
            int hdepW = (int) (cw * 0.48f);
            String depToggleLabel = cachedHideDepLine ? "§e⊖ Hide dep lines" : "§7⊕ Show dep lines";
            addRenderableWidget(Button.builder(Component.literal(depToggleLabel),
                    b -> {
                        cachedHideDepLine = !cachedHideDepLine;
                        rebuildWidgets();
                    })
                    .bounds(cx, fieldY[11], hdepW, FIELD_H)
                    .tooltip(Tooltip.create(
                            Component.literal("Hide all dependency lines connected to this node on the quest canvas")))
                    .build());
            secPanelBot = fieldY[11] + FIELD_H + SEC_PAD;
        }

        // ── Footer buttons ────────────────────────────────────────────────────
        int fbtnY = height - FOOTER_H + (FOOTER_H - 16) / 2;
        int halfW = (cw - COL_GAP) / 2;
        addRenderableWidget(Button.builder(Component.literal("§a✓ Save quest"),
                b -> save()).bounds(cx, fbtnY, halfW, 16)
                .tooltip(Tooltip.create(Component.literal("Write quest to disk and register it live"))).build());
        addRenderableWidget(Button.builder(Component.literal("§7< Done"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx + halfW + COL_GAP, fbtnY, halfW, 16)
                .tooltip(Tooltip.create(Component.literal("Discard unsaved changes and return"))).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(@NotNull GuiGraphics g) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, C_BG);

        // Active tab section panel
        int panelL = cx - SEC_PAD;
        int panelR = cx + cw + SEC_PAD;
        if (secPanelBot > secPanelTop) {
            g.fill(panelL, secPanelTop, panelR, secPanelBot, C_PANEL);
            drawBorder(g, panelL, secPanelTop, panelR - panelL, secPanelBot - secPanelTop, C_BORDER);
        }

        // Header
        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        String heading = editingNode != null ? "§fEdit Quest  §8— §7" + editingNode.getId().getPath() : "§fNew Quest";
        g.drawCenteredString(font, heading, width / 2, (HEADER_H - 8) / 2, C_TEXT);

        // Tab strip background + active underline
        g.fill(0, HEADER_H, width, HEADER_H + TAB_H, C_HEADER);
        g.fill(0, HEADER_H + TAB_H - 1, width, HEADER_H + TAB_H, C_BORDER);
        int tabW = cw / TAB_LABELS.length;
        g.fill(cx + activeTab * tabW, HEADER_H + TAB_H - 2,
                cx + activeTab * tabW + tabW - 2, HEADER_H + TAB_H - 1, C_ACCENT);

        // Footer
        g.fill(0, height - FOOTER_H, width, height, C_HEADER);
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, C_BORDER);

        // Row labels for active tab only
        int[] labelY = new int[12];
        for (int i = 0; i < 12; i++) labelY[i] = fieldY[i] > 0 ? fieldY[i] - LABEL_H - 2 : 0;

        if (activeTab == 0) {
            g.drawString(font, "§8Title", cx, labelY[0], C_TEXT_FAINT, false);
            g.drawString(font, "§8Description", cx, labelY[1], C_TEXT_FAINT, false);
            int catW2 = (int) (cw * 0.55f);
            g.drawString(font, "§8Category", cx, labelY[2], C_TEXT_FAINT, false);
            g.drawString(font, "§8Subtitle", cx + catW2 + COL_GAP, labelY[2], C_TEXT_FAINT, false);
            int iconColW2 = (int) (cw * 0.35f);
            g.drawString(font, "§8Icon", cx, labelY[3], C_TEXT_FAINT, false);
            g.drawString(font, "§8Shape  §7" + cachedShape, cx + iconColW2 + COL_GAP, labelY[3], C_TEXT_FAINT, false);
            // Shape highlight + icon preview
            int shapeColW2 = cw - iconColW2 - COL_GAP;
            int shapeX2 = cx + iconColW2 + COL_GAP;
            int slotW = shapeColW2 / SHAPES.length;
            for (int i = 0; i < SHAPES.length; i++) {
                if (SHAPES[i].id().equals(cachedShape))
                    g.fill(shapeX2 + i * slotW, fieldY[3], shapeX2 + i * slotW + slotW - 1, fieldY[3] + FIELD_H,
                            C_SHAPE_SEL);
            }
            if (!cachedIconItemId.isBlank()) {
                try {
                    net.minecraft.world.item.Item prev = ForgeRegistries.ITEMS
                            .getValue(new ResourceLocation(cachedIconItemId));
                    if (prev != null && prev != net.minecraft.world.item.Items.AIR)
                        g.renderItem(new net.minecraft.world.item.ItemStack(prev), cx + iconColW2 - 18, fieldY[3] - 1);
                } catch (Exception ignored) {}
            }
        } else if (activeTab == 1) {
            g.drawString(font, "§8Visibility  ·  Prerequisite gate", cx, labelY[4], C_TEXT_FAINT, false);
            g.drawString(font, "§8Task completion gate", cx, labelY[5], C_TEXT_FAINT, false);
            int parentW2 = (int) (cw * 0.60f);
            g.drawString(font, "§8Parent quest", cx, labelY[6], C_TEXT_FAINT, false);
            g.drawString(font, "§8enable_if", cx + parentW2 + COL_GAP, labelY[6], C_TEXT_FAINT, false);
            g.drawString(font, "§8Repeat mode", cx, labelY[9], C_TEXT_FAINT, false);
            if (cachedRepeatMode == QuestNode.RepeatMode.COOLDOWN && fieldY[9] > 0) {
                int repeatBtnW2 = (int) (cw * 0.50f);
                int coolW2 = cw - repeatBtnW2 - COL_GAP;
                int coolX2 = cx + repeatBtnW2 + COL_GAP;
                g.drawString(font, "§8Cooldown hours", coolX2 + 22, labelY[9], C_TEXT_FAINT, false);
                g.drawCenteredString(font, "§f" + cachedRepeatCooldownHours + " §8h",
                        coolX2 + coolW2 / 2, fieldY[9] + (FIELD_H - 8) / 2, C_TEXT_DIM);
            }
        } else {
            g.drawString(font, idManuallySet ? "§8Quest ID  §c(manual)" : "§8Quest ID  §a(auto)", cx, labelY[7],
                    C_TEXT_FAINT, false);
            g.drawString(font, "§8Tasks & rewards", cx, labelY[8], C_TEXT_FAINT, false);
            if (fieldY[10] > 0) {
                g.drawString(font, "§8Canvas position  X / Y", cx, labelY[10], C_TEXT_FAINT, false);
            }
            if (fieldY[11] > 0) {
                int hdepW2 = (int) (cw * 0.48f);
                g.drawString(font, "§8Dep line  ·  Dependents", cx, labelY[11], C_TEXT_FAINT, false);
                if (editingNode != null) {
                    int childCount = editingNode.getChildren().size();
                    String childStr = childCount == 0 ? "§8No dependents" :
                            "§7" + childCount + " quest" + (childCount == 1 ? "" : "s") + " unlock after this";
                    g.drawString(font, childStr, cx + hdepW2 + COL_GAP, fieldY[11] + (FIELD_H - 8) / 2, C_TEXT_DIM,
                            false);
                }
            }
        }

        // Status
        if (!statusMsg.isEmpty()) {
            g.drawCenteredString(font, (statusIsErr ? "§c" : "§a") + statusMsg,
                    width / 2, height - FOOTER_H - 12, statusIsErr ? C_ERR : C_OK);
        }

        super.render(g, mx, my, partial);

        // Dropdowns — elevated z
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        if (visibilityDropdownOpen && activeTab == 1) {
            int visW = 90;
            int dropH = VISIBILITIES.length * (FIELD_H + 1);
            int dropY = fieldY[4] + FIELD_H + 1;
            g.fill(cx, dropY, cx + visW, dropY + dropH, C_PANEL);
            drawBorder(g, cx, dropY, visW, dropH, C_ACCENT);
            for (int i = 0; i < VISIBILITIES.length; i++) {
                int ry = dropY + i * (FIELD_H + 1);
                boolean hov = mx >= cx && mx < cx + visW && my >= ry && my < ry + FIELD_H + 1;
                if (hov) g.fill(cx + 1, ry, cx + visW - 1, ry + FIELD_H + 1, 0xFF1E1E2A);
                g.drawString(font, "§7" + VISIBILITIES[i].name(), cx + 5, ry + 3, hov ? C_TEXT : C_TEXT_DIM, false);
            }
        }

        if (categoryDropdownOpen && activeTab == 0) {
            List<String> cats = buildExistingCategories();
            int catW3 = (int) (cw * 0.55f);
            int catPickW = 16, newCatW = 32;
            int catBoxW = catW3 - catPickW - 2 - newCatW - 2;
            int dropW = catW3;
            int dropH = Math.max(FIELD_H + 1, cats.size() * (FIELD_H + 1));
            int dropY = fieldY[2] + FIELD_H + 1;
            g.fill(cx, dropY, cx + dropW, dropY + dropH, C_PANEL);
            drawBorder(g, cx, dropY, dropW, dropH, C_ACCENT);
            if (cats.isEmpty()) {
                g.drawString(font, "§8No categories yet", cx + 5, dropY + 3, C_TEXT_FAINT, false);
            } else {
                for (int i = 0; i < cats.size(); i++) {
                    int ry = dropY + i * (FIELD_H + 1);
                    boolean hov = mx >= cx && mx < cx + dropW && my >= ry && my < ry + FIELD_H + 1;
                    if (hov) g.fill(cx + 1, ry, cx + dropW - 1, ry + FIELD_H + 1, 0xFF1E1E2A);
                    g.drawString(font, "§7" + cats.get(i), cx + 5, ry + 3, hov ? C_TEXT : C_TEXT_DIM, false);
                }
            }
        }

        g.pose().popPose();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (visibilityDropdownOpen && activeTab == 1) {
                int visW = 90;
                int dropY = fieldY[4] + FIELD_H + 1;
                for (int i = 0; i < VISIBILITIES.length; i++) {
                    int ry = dropY + i * (FIELD_H + 1);
                    if (mx >= cx && mx < cx + visW && my >= ry && my < ry + FIELD_H + 1) {
                        cachedVisibility = VISIBILITIES[i];
                        visibilityDropdownOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                visibilityDropdownOpen = false;
                rebuildWidgets();
                return true;
            }
            if (categoryDropdownOpen && activeTab == 0) {
                List<String> cats = buildExistingCategories();
                int catW3 = (int) (cw * 0.55f);
                int dropW = catW3;
                int dropY = fieldY[2] + FIELD_H + 1;
                for (int i = 0; i < cats.size(); i++) {
                    int ry = dropY + i * (FIELD_H + 1);
                    if (mx >= cx && mx < cx + dropW && my >= ry && my < ry + FIELD_H + 1) {
                        cachedCategory = cats.get(i);
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

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && !visibilityDropdownOpen && !categoryDropdownOpen) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        visibilityDropdownOpen = false;
        categoryDropdownOpen = false;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void save() {
        String id = cachedId.trim().toLowerCase().replaceAll("[^a-z0-9/._-]", "");
        String title = cachedTitle.trim();
        String desc = cachedDesc.trim();
        String category = cachedCategory.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
        if (category.isEmpty()) category = "MAIN";

        if (id.isEmpty() || title.isEmpty()) {
            statusMsg = id.isEmpty() ? "Title is required (ID auto-generates from it)" : "Title is required";
            statusIsErr = true;
            return;
        }

        Path baseDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");

        try {
            CompoundTag snbt = new CompoundTag();
            snbt.putString("id", id);
            snbt.putString("title", title);
            snbt.putString("description", desc);
            if (!cachedSubtitle.isBlank()) snbt.putString("subtitle", cachedSubtitle.trim());
            snbt.putString("category", category);
            snbt.putString("shape", cachedShape);
            snbt.putString("visibility", cachedVisibility.name());
            if (cachedDisabledBlocksChildren) snbt.putBoolean("disabled_blocks_children", true);
            if (!cachedEnableIf.isBlank()) snbt.putString("enable_if", cachedEnableIf.trim());
            snbt.putString("parent", cachedParent != null ? cachedParent.getId().getPath() : "none");
            snbt.putBoolean("require_all_prereqs", cachedRequireAll);
            if (cachedTaskMinCount > 0) snbt.putInt("task_min_count", cachedTaskMinCount);
            snbt.putInt("positionX", cachedPosX);
            snbt.putInt("positionY", cachedPosY);
            if (cachedRepeatMode != QuestNode.RepeatMode.NONE) {
                snbt.putString("repeat_mode", cachedRepeatMode.name());
                if (cachedRepeatMode == QuestNode.RepeatMode.COOLDOWN)
                    snbt.putInt("repeat_cooldown_hours", cachedRepeatCooldownHours);
            }
            if (cachedHideDepLine) snbt.putBoolean("hide_dep_line", true);
            if (!cachedIconItemId.isBlank()) snbt.putString("icon_item", cachedIconItemId.trim());

            Path snbtPath = baseDir.resolve(id + ".snbt");
            Files.createDirectories(snbtPath.getParent());
            Files.writeString(snbtPath, snbt.toString(), StandardCharsets.UTF_8);

            Path mdPath = baseDir.resolve("quests").resolve(id + ".md");
            Files.createDirectories(mdPath.getParent());
            if (!Files.exists(mdPath)) {
                Files.writeString(mdPath,
                        "---\ntitle: \"" + title.replace("\"", "\\\"") + "\"\n---\n\n" + desc + "\n",
                        StandardCharsets.UTF_8);
            } else {
                String existing = Files.readString(mdPath, StandardCharsets.UTF_8);
                Files.writeString(mdPath, LangEditorScreen.patchMdFile(existing, title, desc), StandardCharsets.UTF_8);
            }

            ResourceLocation questId = new ResourceLocation("phoenixcore", id);
            ResourceLocation parentLoc = cachedParent != null ? cachedParent.getId() : null;

            QuestNode node = new QuestNode(questId, Component.literal(title), Component.literal(desc));
            node.setCategory(category);
            node.setShapeType(cachedShape);
            node.setSubtitle(cachedSubtitle.trim());
            node.setVisibility(cachedVisibility);
            node.setDisabledBlocksChildren(cachedDisabledBlocksChildren);
            node.setRequireAllPrerequisites(cachedRequireAll);
            node.setTaskMinCount(cachedTaskMinCount);
            node.setRepeatMode(cachedRepeatMode);
            if (cachedRepeatMode == QuestNode.RepeatMode.COOLDOWN)
                node.setRepeatCooldownHours(cachedRepeatCooldownHours);
            node.setHideDepLine(cachedHideDepLine);
            node.setCustomPosition(cachedPosX, cachedPosY);
            if (!cachedIconItemId.isBlank()) node.setIconItemById(cachedIconItemId.trim());

            QuestTreeRegistry.injectDynamicQuestNode(node, parentLoc);

            Path base = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("phoenix_chronicles");
            LangEditorScreen.writeEnUsJson(base);

            statusMsg = "Saved!";
            statusIsErr = false;

        } catch (IOException e) {
            statusMsg = "IO error: " + e.getMessage();
            statusIsErr = true;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> buildExistingCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("MAIN");
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            String c = n.getCategory();
            if (c != null && !cats.contains(c)) cats.add(c);
        }
        return cats;
    }

    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
