package net.phoenix.core.client.emi;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Popup for picking which named EMI favorites page is active, creating a new one, and switching
 * whether pages are shared across every world or private to each one. Reopens {@link #parent} on
 * close instead of kicking the player all the way back to the main game screen.
 */
public class EmiFavoritePagesScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PADDING = 14;
    private static final int ROW_HEIGHT = 22;
    private static final int TITLE_HEIGHT = 26;

    private final @Nullable Screen parent;
    private EditBox newPageBox;

    public EmiFavoritePagesScreen(@Nullable Screen parent) {
        super(Component.literal("EMI Favorite Pages"));
        this.parent = parent;
    }

    private int contentHeight(int pageCount) {
        return TITLE_HEIGHT + pageCount * ROW_HEIGHT + 10 + ROW_HEIGHT + 12 + ROW_HEIGHT;
    }

    @Override
    protected void init() {
        List<String> pageNames = PhoenixFavoriteSets.getSetNames();
        String active = PhoenixFavoriteSets.getActiveSet();

        int panelHeight = contentHeight(pageNames.size()) + PADDING * 2;
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = (this.height - panelHeight) / 2;
        int contentWidth = PANEL_WIDTH - PADDING * 2;

        int y = top + PADDING + TITLE_HEIGHT;

        for (String page : pageNames) {
            String label = page.equals(active) ? "★ " + page : page;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                PhoenixFavoriteSets.switchTo(page);
                onClose();
            }).bounds(left + PADDING, y, contentWidth, 20).build());
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
            PhoenixFavoriteSets.setScope(scope == PhoenixFavoriteSets.Scope.GLOBAL ?
                    PhoenixFavoriteSets.Scope.PER_WORLD : PhoenixFavoriteSets.Scope.GLOBAL);
            if (this.minecraft != null) {
                this.minecraft.setScreen(new EmiFavoritePagesScreen(parent));
            }
        }).bounds(left + PADDING, y, contentWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelHeight = contentHeight(PhoenixFavoriteSets.getSetNames().size()) + PADDING * 2;
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

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + (TITLE_HEIGHT - 8) / 2, 0xFFE8E8E8);

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
