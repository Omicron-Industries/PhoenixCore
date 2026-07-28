package net.phoenix.core.client.emi;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmiFavoritePagesScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PADDING = 14;
    private static final int ROW_HEIGHT = 22;
    private static final int TITLE_HEIGHT = 26;
    private static final int ARROW_W = 16;
    private static final int ICON_W = 20;

    private final @Nullable Screen parent;
    private EditBox newPageBox;

    private @Nullable String renaming;
    private EditBox renameBox;

    public EmiFavoritePagesScreen(@Nullable Screen parent) {
        super(Component.literal("EMI Favorite Pages"));
        this.parent = parent;
    }

    private int contentHeight(int pageCount) {
        return TITLE_HEIGHT + pageCount * ROW_HEIGHT + 10 + ROW_HEIGHT + 12 + ROW_HEIGHT;
    }

    @Override
    protected void init() {
        if (renaming != null) {
            initRenameView();
        } else {
            initListView();
        }
    }

    private void initListView() {
        List<String> pageNames = PhoenixFavoriteSets.getSetNames();
        String active = PhoenixFavoriteSets.getActiveSet();

        int panelHeight = contentHeight(pageNames.size()) + PADDING * 2;
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = (this.height - panelHeight) / 2;
        int contentWidth = PANEL_WIDTH - PADDING * 2;

        int y = top + PADDING + TITLE_HEIGHT;

        for (int i = 0; i < pageNames.size(); i++) {
            String page = pageNames.get(i);
            boolean isActive = page.equals(active);
            boolean canDelete = pageNames.size() > 1;

            int x = left + PADDING;

            int rowY = y;
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                PhoenixFavoriteSets.moveUp(page);
                rebuild();
            }).bounds(x, rowY, ARROW_W, 20).build()).active = i > 0;
            x += ARROW_W + 2;

            addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                PhoenixFavoriteSets.moveDown(page);
                rebuild();
            }).bounds(x, rowY, ARROW_W, 20).build()).active = i < pageNames.size() - 1;
            x += ARROW_W + 4;

            int nameWidth = contentWidth - (x - (left + PADDING)) - ICON_W * 2 - 4;
            String label = isActive ? "★ " + page : page;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                PhoenixFavoriteSets.switchTo(page);
                onClose();
            }).bounds(x, rowY, nameWidth, 20).build());
            x += nameWidth + 2;

            addRenderableWidget(Button.builder(Component.literal("✎"), b -> {
                renaming = page;
                renameBox = null;
                rebuild();
            }).bounds(x, rowY, ICON_W, 20).build());
            x += ICON_W + 2;

            Button deleteBtn = addRenderableWidget(Button.builder(Component.literal("🗑"), b -> {
                if (PhoenixFavoriteSets.deleteSet(page)) rebuild();
            }).bounds(x, rowY, ICON_W, 20).build());
            deleteBtn.active = canDelete;

            y += ROW_HEIGHT;
        }

        y += 10;
        int boxWidth = contentWidth - 40;
        newPageBox = new EditBox(this.font, left + PADDING, y, boxWidth, 20, Component.literal("New page name"));
        newPageBox.setMaxLength(32);
        addRenderableWidget(newPageBox);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            String name = newPageBox.getValue().trim();
            if (!name.isEmpty()) {
                PhoenixFavoriteSets.createSet(name);
                onClose();
            }
        }).bounds(left + PADDING + boxWidth + 4, y, 36, 20).build());

        y += ROW_HEIGHT + 12;

        PhoenixFavoriteSets.Scope scope = PhoenixFavoriteSets.getScope();
        String scopeLabel = "Pages: " +
                (scope == PhoenixFavoriteSets.Scope.GLOBAL ? "Shared across worlds" : "Private per world");
        addRenderableWidget(Button.builder(Component.literal(scopeLabel), b -> {
            PhoenixFavoriteSets
                    .setScope(scope == PhoenixFavoriteSets.Scope.GLOBAL ? PhoenixFavoriteSets.Scope.PER_WORLD :
                            PhoenixFavoriteSets.Scope.GLOBAL);
            rebuild();
        }).bounds(left + PADDING, y, contentWidth, 20).build());
    }

    private void initRenameView() {
        int panelHeight = renamePanelHeight();
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = (this.height - panelHeight) / 2;
        int contentWidth = PANEL_WIDTH - PADDING * 2;
        int y = top + PADDING + TITLE_HEIGHT;

        renameBox = new EditBox(this.font, left + PADDING, y, contentWidth, 20, Component.literal("New name"));
        renameBox.setMaxLength(32);
        renameBox.setValue(renaming);
        addRenderableWidget(renameBox);
        y += ROW_HEIGHT + 8;

        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> {
            String newName = renameBox.getValue().trim();
            if (!newName.isEmpty()) {
                PhoenixFavoriteSets.renameSet(renaming, newName);
            }
            renaming = null;
            rebuild();
        }).bounds(left + PADDING, y, contentWidth / 2 - 4, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            renaming = null;
            rebuild();
        }).bounds(left + PADDING + contentWidth / 2 + 4, y, contentWidth / 2 - 4, 20).build());
    }

    private int renamePanelHeight() {
        return TITLE_HEIGHT + ROW_HEIGHT + 8 + ROW_HEIGHT + PADDING * 2;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        boolean isRename = renaming != null;
        int panelHeight = isRename ? renamePanelHeight() :
                contentHeight(PhoenixFavoriteSets.getSetNames().size()) + PADDING * 2;
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, 0xF0101014);
        graphics.fill(left, top, right, top + 1, 0xFF6A6A6A);
        graphics.fill(left, top, left + 1, bottom, 0xFF6A6A6A);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF262626);
        graphics.fill(right - 1, top, right, bottom, 0xFF262626);

        graphics.fill(left + 1, top + 1, right - 1, top + TITLE_HEIGHT, 0x30FFFFFF);

        String title = isRename ? "Rename \"" + renaming + "\"" : "EMI Favorite Pages";
        graphics.drawCenteredString(this.font, title, this.width / 2, top + (TITLE_HEIGHT - 8) / 2, 0xFFE8E8E8);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
