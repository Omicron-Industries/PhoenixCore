package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTreeRegistry;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParentSelectorScreen extends Screen {

    private final Screen parentScreen;
    private final QuestNode editingNode;
    private final Consumer<QuestNode> onSelectionComplete;

    private EditBox searchBox;
    private final List<QuestNode> filteredNodes = new ArrayList<>();
    private final List<QuestNode> allAvailableNodes = new ArrayList<>();

    public ParentSelectorScreen(Screen parentScreen, QuestNode editingNode, Consumer<QuestNode> onSelectionComplete) {
        super(Component.literal("Select Parent Dependency Node"));
        this.parentScreen = parentScreen;
        this.editingNode = editingNode;
        this.onSelectionComplete = onSelectionComplete;

        // Cache all valid nodes (excluding the node currently being edited to prevent self-recursion loops)
        for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
            if (this.editingNode == null || !node.getId().equals(this.editingNode.getId())) {
                this.allAvailableNodes.add(node);
            }
        }
        this.filteredNodes.addAll(this.allAvailableNodes);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int midX = this.width / 2;

        // 1. Search Bar Field Input
        this.searchBox = new EditBox(this.font, midX - 100, 35, 200, 16, Component.literal("Search..."));
        this.searchBox.setHint(Component.literal("§8Type to filter nodes..."));
        this.searchBox.setResponder(this::updateSearchFilter);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // 2. Generate Clickable Select Rows dynamically based on the filtered results
        int startY = 60;
        int maxRows = Math.min(this.filteredNodes.size(), 8);

        for (int i = 0; i < maxRows; i++) {
            QuestNode targetNode = this.filteredNodes.get(i);
            String label = "§7" + targetNode.getId().getPath() + " (" + targetNode.getTitle().getString() + ")";

            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                this.onSelectionComplete.accept(targetNode);
                if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
            }).bounds(midX - 110, startY + (i * 22), 220, 18).build());
        }

        // 3. Abort / Return Back Controls
        this.addRenderableWidget(Button.builder(Component.literal("§c[ CANCEL ]"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
        }).bounds(midX - 50, this.height - 28, 100, 20).build());
    }

    private void updateSearchFilter(String query) {
        String cleanQuery = query.toLowerCase().trim();
        this.filteredNodes.clear();

        for (QuestNode node : this.allAvailableNodes) {
            boolean matchesPath = node.getId().getPath().toLowerCase().contains(cleanQuery);
            boolean matchesTitle = node.getTitle().getString().toLowerCase().contains(cleanQuery);

            if (cleanQuery.isEmpty() || matchesPath || matchesTitle) {
                this.filteredNodes.add(node);
            }
        }
        // Force workspace layout regeneration to redraw filtered button listings
        this.rebuildWidgets();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        int midX = this.width / 2;

        // Screen Frame Box Boundary Backdrop
        graphics.fill(midX - 130, 10, midX + 130, this.height - 6, 0xED050505);
        graphics.renderOutline(midX - 130, 10, 260, this.height - 16, 0xFF00AA00);

        graphics.drawCenteredString(this.font, "§aSEARCH CORE QUEST MATRIX", midX, 18, 0xFFFFFF);

        if (this.filteredNodes.isEmpty()) {
            graphics.drawCenteredString(this.font, "§8No matching nodes located.", midX, 90, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }
}
