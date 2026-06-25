package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.QuestGroup;
import net.phoenix.core.integration.phoenix_chronicles.QuestGroupManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Popup screen for creating or editing a {@link QuestGroup}.
 *
 * <ul>
 * <li>If {@code existing} is {@code null}, a new group is created at the given canvas position.</li>
 * <li>If {@code existing} is provided, that group is edited in-place.</li>
 * </ul>
 */
public class QuestGroupEditorScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int DIALOG_W = 220;
    private static final int DIALOG_H = 180;
    private static final int ROW_H = 22;
    private static final int FIELD_H = 14;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int C_BG = 0xFF14141A;
    private static final int C_BORDER = 0xFF8844AA;
    private static final int C_HEADER = 0xFF09090D;
    private static final int C_TEXT = 0xFFD8D8E4;
    private static final int C_TEXT_DIM = 0xFF7A7A8A;
    private static final int C_LABEL = 0xFF5A5A7A;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final String category;
    @Nullable
    private final QuestGroup existing;
    private final int canvasX;
    private final int canvasY;

    private EditBox labelBox;
    private EditBox colorBox;
    private EditBox borderColorBox;
    private String errorMsg = "";

    public QuestGroupEditorScreen(Screen parent, String category, @Nullable QuestGroup existing,
                                  int canvasX, int canvasY) {
        super(Component.literal(existing == null ? "New Quest Group" : "Edit Quest Group"));
        this.parent = parent;
        this.category = category;
        this.existing = existing;
        this.canvasX = canvasX;
        this.canvasY = canvasY;
    }

    @Override
    protected void init() {
        int dx = (width - DIALOG_W) / 2;
        int dy = (height - DIALOG_H) / 2;

        int fieldX = dx + 10;
        int fieldW = DIALOG_W - 20;
        int row = dy + 30;

        // Label field
        labelBox = new EditBox(font, fieldX, row, fieldW, FIELD_H, Component.empty());
        labelBox.setHint(Component.literal("§8Group label…"));
        labelBox.setMaxLength(48);
        if (existing != null) labelBox.setValue(existing.getLabel());
        addRenderableWidget(labelBox);
        row += ROW_H;

        // Color (fill) field
        colorBox = new EditBox(font, fieldX, row, fieldW, FIELD_H, Component.empty());
        colorBox.setHint(Component.literal("§8#AARRGGBB fill color"));
        colorBox.setMaxLength(10);
        colorBox.setValue(existing != null ? QuestGroupManager.formatColor(existing.getColor()) : "#22FFFFFF");
        addRenderableWidget(colorBox);
        row += ROW_H;

        // Border color field
        borderColorBox = new EditBox(font, fieldX, row, fieldW, FIELD_H, Component.empty());
        borderColorBox.setHint(Component.literal("§8#AARRGGBB border color"));
        borderColorBox.setMaxLength(10);
        borderColorBox
                .setValue(existing != null ? QuestGroupManager.formatColor(existing.getBorderColor()) : "#44FFFFFF");
        addRenderableWidget(borderColorBox);
        row += ROW_H;

        // Category label (read-only info row — no editable field)
        row += 6; // a little spacer

        // Buttons
        int btnY = dy + DIALOG_H - 24;
        int btnW = existing != null ? (DIALOG_W - 30) / 3 : (DIALOG_W - 20) / 2;

        // Save
        addRenderableWidget(Button.builder(Component.literal("§aSave"), b -> onSave())
                .bounds(dx + 10, btnY, btnW, 14).build());

        // Delete (only when editing an existing group)
        if (existing != null) {
            addRenderableWidget(Button.builder(Component.literal("§cDelete"), b -> onDelete())
                    .bounds(dx + 10 + btnW + 5, btnY, btnW, 14).build());
            addRenderableWidget(Button.builder(Component.literal("§8Cancel"), b -> close())
                    .bounds(dx + 10 + (btnW + 5) * 2, btnY, btnW, 14).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("§8Cancel"), b -> close())
                    .bounds(dx + 10 + btnW + 5, btnY, btnW, 14).build());
        }
    }

    private void onSave() {
        String label = labelBox.getValue().trim();
        if (label.isEmpty()) {
            errorMsg = "Label cannot be empty.";
            return;
        }

        String colorStr = colorBox.getValue().trim();
        String borderStr = borderColorBox.getValue().trim();

        if (!isValidColor(colorStr)) {
            errorMsg = "Invalid fill color (use #AARRGGBB).";
            return;
        }
        if (!isValidColor(borderStr)) {
            errorMsg = "Invalid border color (use #AARRGGBB).";
            return;
        }

        QuestGroup group;
        if (existing != null) {
            group = existing;
        } else {
            group = new QuestGroup(QuestGroupManager.generateId(), label, category);
            group.setX(canvasX);
            group.setY(canvasY);
            group.setSize(120, 80);
        }

        group.setLabel(label);
        group.setColor(QuestGroupManager.parseColor(colorStr));
        group.setBorderColor(QuestGroupManager.parseColor(borderStr));
        group.setCategory(category);

        QuestGroupManager.put(group);
        QuestGroupManager.save(configPath());

        close();
    }

    private void onDelete() {
        if (existing != null) {
            QuestGroupManager.remove(existing.getId());
            QuestGroupManager.save(configPath());
        }
        close();
    }

    private void close() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private boolean isValidColor(String s) {
        if (s == null) return false;
        String hex = s.startsWith("#") ? s.substring(1) : s;
        return (hex.length() == 6 || hex.length() == 8) && hex.matches("[0-9A-Fa-f]+");
    }

    private Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles");
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);

        int dx = (width - DIALOG_W) / 2;
        int dy = (height - DIALOG_H) / 2;

        // Dialog background
        g.fill(dx, dy, dx + DIALOG_W, dy + DIALOG_H, C_BG);
        // Border
        g.fill(dx, dy, dx + DIALOG_W, dy + 1, C_BORDER);
        g.fill(dx, dy + DIALOG_H - 1, dx + DIALOG_W, dy + DIALOG_H, C_BORDER);
        g.fill(dx, dy, dx + 1, dy + DIALOG_H, C_BORDER);
        g.fill(dx + DIALOG_W - 1, dy, dx + DIALOG_W, dy + DIALOG_H, C_BORDER);
        // Header bar
        g.fill(dx + 1, dy + 1, dx + DIALOG_W - 1, dy + 16, C_HEADER);
        g.drawCenteredString(font, "§d" + this.title.getString(), dx + DIALOG_W / 2, dy + 4, C_TEXT);

        int row = dy + 30;

        // Field labels
        g.drawString(font, "§8Label", dx + 10, row - 11, C_LABEL);
        row += ROW_H;
        g.drawString(font, "§8Fill color  §7(#AARRGGBB)", dx + 10, row - 11, C_LABEL);
        row += ROW_H;
        g.drawString(font, "§8Border color  §7(#AARRGGBB)", dx + 10, row - 11, C_LABEL);
        row += ROW_H;

        // Category display
        g.drawString(font, "§8Chapter: §7" + friendlyCategory(), dx + 10, row + 2, C_TEXT_DIM);

        // Error message
        if (!errorMsg.isEmpty()) {
            g.drawCenteredString(font, "§c" + errorMsg, dx + DIALOG_W / 2, dy + DIALOG_H - 36, 0xFFCC4444);
        }

        super.render(g, mx, my, partial);
    }

    private String friendlyCategory() {
        if (category == null || category.equals("ALL")) return "All Chapters";
        StringBuilder sb = new StringBuilder();
        for (String w : category.toLowerCase().replace("_", " ").split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
