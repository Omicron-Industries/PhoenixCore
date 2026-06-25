package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * FTB-style item picker screen.
 *
 * Three source tabs:
 * REGISTRY — browses every item registered in ForgeRegistries.ITEMS,
 * paginated and filterable by name or registry id.
 * INVENTORY — shows the player's current inventory slots for quick access.
 * EMI / JEI — if EMI or JEI is loaded, delegates to their item list APIs
 * so modded items get proper display names and recipe links.
 * Falls back to REGISTRY mode if neither is present.
 *
 * The caller provides a Consumer<ItemStack> callback. When the player clicks
 * an item the callback fires and this screen closes, returning to the parent.
 */
public class ItemPickerScreen extends Screen {

    // ── Colours (shared with the rest of the UI) ───────────────────────────────
    private static final int COL_BG = 0xFF0F0F13;
    private static final int COL_PANEL = 0xFF16161C;
    private static final int COL_PANEL_DARK = 0xFF111115;
    private static final int COL_HEADER = 0xFF0C0C10;
    private static final int COL_BORDER = 0xFF2A2A36;
    private static final int COL_BORDER_LIT = 0xFF3A3A4C;
    private static final int COL_SEL = 0xFF1E2A1E;
    private static final int COL_SEL_BORDER = 0xFF00AA55;
    private static final int COL_HOVER = 0xFF1E1E2A;
    private static final int COL_TEXT = 0xFFDDDDE8;
    private static final int COL_TEXT_DIM = 0xFF888898;
    private static final int COL_TEXT_FAINT = 0xFF4A4A5A;
    private static final int COL_TAB_ACTIVE = 0xFF00AA55;

    // ── Layout ─────────────────────────────────────────────────────────────────
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 220;
    private static final int HEADER_H = 22;
    private static final int SEARCH_H = 16;
    private static final int FOOTER_H = 22;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 12;

    // ── State ──────────────────────────────────────────────────────────────────

    public enum SourceTab {
        REGISTRY,
        INVENTORY,
        EMI_JEI
    }

    private SourceTab activeTab = SourceTab.REGISTRY;

    private final Screen parent;
    private final Consumer<ItemStack> onPick;

    // Populated based on active tab + search filter
    private final List<ItemStack> displayItems = new ArrayList<>();
    private int scrollOffset = 0; // in rows
    private ItemStack hoveredStack = null;
    private ItemStack selectedStack = null;

    private EditBox searchBox;
    private String searchQuery = "";

    // Panel geometry (computed in init)
    private int panelLeft, panelTop;

    // EMI/JEI availability — detected once at construction
    private final boolean hasEmi;
    private final boolean hasJei;

    public ItemPickerScreen(Screen parent, Consumer<ItemStack> onPick) {
        super(Component.literal("Pick Item"));
        this.parent = parent;
        this.onPick = onPick;
        this.hasEmi = isModLoaded("emi");
        this.hasJei = isModLoaded("jei");
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;

        int tabY = panelTop + HEADER_H + 2;
        int tabW = 60;

        // Source tabs
        addRenderableWidget(Button.builder(Component.literal("Registry"), b -> switchTab(SourceTab.REGISTRY))
                .bounds(panelLeft + 2, tabY, tabW, 12).build());
        addRenderableWidget(Button.builder(Component.literal("Inventory"), b -> switchTab(SourceTab.INVENTORY))
                .bounds(panelLeft + 4 + tabW, tabY, tabW, 12).build());

        String eiLabel = hasEmi ? "EMI" : hasJei ? "JEI" : "EMI/JEI";
        addRenderableWidget(Button.builder(Component.literal(eiLabel), b -> switchTab(SourceTab.EMI_JEI))
                .bounds(panelLeft + 6 + tabW * 2, tabY, tabW, 12).build());

        // Search bar (only for REGISTRY and EMI_JEI tabs)
        int searchY = panelTop + HEADER_H + 16;
        searchBox = new EditBox(font, panelLeft + 4, searchY, PANEL_W - 8, SEARCH_H, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("§8Search items…"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(q -> {
            searchQuery = q;
            scrollOffset = 0;
            rebuildDisplayList();
        });
        searchBox.visible = (activeTab != SourceTab.INVENTORY);
        addRenderableWidget(searchBox);

        // Confirm button
        addRenderableWidget(Button.builder(Component.literal("§aSelect"), b -> confirmSelection())
                .bounds(panelLeft + PANEL_W - 56, panelTop + PANEL_H - FOOTER_H + 3, 52, 14).build());

        // Cancel
        addRenderableWidget(Button.builder(Component.literal("§7Cancel"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(panelLeft + 4, panelTop + PANEL_H - FOOTER_H + 3, 52, 14).build());

        rebuildDisplayList();
    }

    private void switchTab(SourceTab tab) {
        activeTab = tab;
        scrollOffset = 0;
        selectedStack = null;
        if (searchBox != null) searchBox.visible = (tab != SourceTab.INVENTORY);
        rebuildDisplayList();
    }

    // ── Display list population ────────────────────────────────────────────────

    private void rebuildDisplayList() {
        displayItems.clear();
        switch (activeTab) {
            case REGISTRY -> populateFromRegistry();
            case INVENTORY -> populateFromInventory();
            case EMI_JEI -> populateFromEmiOrJei();
        }
    }

    private void populateFromRegistry() {
        String q = searchQuery.toLowerCase().trim();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (!q.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                boolean matchesId = id != null && id.toString().contains(q);
                boolean matchesName = stack.getHoverName().getString().toLowerCase().contains(q);
                if (!matchesId && !matchesName) continue;
            }
            displayItems.add(stack);
            if (displayItems.size() >= 2000) break; // safety cap
        }
    }

    private void populateFromInventory() {
        if (minecraft == null || minecraft.player == null) return;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (!stack.isEmpty()) displayItems.add(stack.copy());
        }
    }

    private void populateFromEmiOrJei() {
        if (hasEmi) {
            populateFromEmiApi();
        } else if (hasJei) {
            populateFromJeiApi();
        } else {
            // Neither is loaded — fall back to registry with a notice
            populateFromRegistry();
        }
    }

    /**
     * EMI integration.
     * EMI exposes its item list via EmiApi.getSearchEntries() which returns
     * EmiIngredient instances. We unwrap those to ItemStacks here.
     *
     * The try/catch means this silently falls back if EMI's API signature
     * ever changes or if the class isn't present at runtime.
     */
    private void populateFromEmiApi() {
        try {
            Class<?> emiApi = Class.forName("dev.emi.emi.api.EmiApi");
            Object searchList = emiApi.getMethod("getSearchEntries").invoke(null);

            // searchList is Iterable<EmiIngredient>
            for (Object entry : (Iterable<?>) searchList) {
                Class<?> ingredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
                Object stacks = ingredientClass.getMethod("getEmiStacks").invoke(entry);

                for (Object emiStack : (Iterable<?>) stacks) {
                    Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
                    Object mcStack = emiStackClass.getMethod("getItemStack").invoke(emiStack);
                    if (mcStack instanceof ItemStack s && !s.isEmpty()) {
                        String q = searchQuery.toLowerCase().trim();
                        if (q.isEmpty() || s.getHoverName().getString().toLowerCase().contains(q)) {
                            displayItems.add(s.copy());
                        }
                        if (displayItems.size() >= 2000) return;
                    }
                }
            }
        } catch (Exception e) {
            // EMI API unavailable or changed — fall back
            populateFromRegistry();
        }
    }

    /**
     * JEI integration.
     * JEI exposes its ingredient list via IJeiRuntime → IIngredientManager.
     * We access it reflectively so we don't need a hard compile-time dependency.
     */
    private void populateFromJeiApi() {
        try {
            Class<?> internalClass = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            // JEI stores the current runtime in InternalJeiHelpers
            Class<?> helpersClass = Class.forName("mezz.jei.common.Internal");
            Object runtime = helpersClass.getMethod("getJeiRuntime").invoke(null);
            if (runtime == null) {
                populateFromRegistry();
                return;
            }

            Object ingredientManager = internalClass.getMethod("getIngredientManager").invoke(runtime);
            Class<?> mgr = Class.forName("mezz.jei.api.ingredients.IIngredientManager");

            Class<?> itemType = Class.forName("mezz.jei.api.ingredients.vanilla.IVanillaIngredientTypes");
            Object itemIngredientType = itemType.getField("ITEM").get(null);

            Iterable<?> allItems = (Iterable<?>) mgr.getMethod("getAllItemStacks").invoke(ingredientManager);
            String q = searchQuery.toLowerCase().trim();
            for (Object obj : allItems) {
                if (obj instanceof ItemStack s && !s.isEmpty()) {
                    if (q.isEmpty() || s.getHoverName().getString().toLowerCase().contains(q)) {
                        displayItems.add(s.copy());
                    }
                    if (displayItems.size() >= 2000) break;
                }
            }
        } catch (Exception e) {
            populateFromRegistry();
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x88000000);

        // Panel
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL);
        drawBorder(g, panelLeft, panelTop, PANEL_W, PANEL_H, COL_BORDER_LIT);

        // Header
        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + HEADER_H, COL_HEADER);
        g.fill(panelLeft, panelTop + HEADER_H - 1, panelLeft + PANEL_W, panelTop + HEADER_H, COL_BORDER);
        g.drawCenteredString(font, "§fPick Item", panelLeft + PANEL_W / 2, panelTop + 7, COL_TEXT);

        // Tab underlines
        int tabY = panelTop + HEADER_H + 2;
        int tabW = 60;
        int[] tabXs = { panelLeft + 2, panelLeft + 4 + tabW, panelLeft + 6 + tabW * 2 };
        SourceTab[] tabs = { SourceTab.REGISTRY, SourceTab.INVENTORY, SourceTab.EMI_JEI };
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == activeTab)
                g.fill(tabXs[i], tabY + 12, tabXs[i] + tabW, tabY + 13, COL_TAB_ACTIVE);
        }

        // Footer separator
        int footerY = panelTop + PANEL_H - FOOTER_H;
        g.fill(panelLeft, footerY, panelLeft + PANEL_W, footerY + 1, COL_BORDER);
        g.fill(panelLeft, footerY, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL_DARK);

        // Item count hint
        g.drawString(font, "§8" + displayItems.size() + " items", panelLeft + 62, footerY + 7, COL_TEXT_FAINT);

        // Render widgets (tabs, search, buttons)
        super.render(g, mx, my, partial);

        // ── Item grid ─────────────────────────────────────────────────────────
        int gridTop = panelTop + HEADER_H + (activeTab == SourceTab.INVENTORY ? 16 : 34);
        int gridBottom = footerY - (selectedStack != null ? 20 : 2);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / SLOT_SIZE);

        g.enableScissor(panelLeft, gridTop, panelLeft + PANEL_W, gridBottom);

        hoveredStack = null;
        int startIdx = scrollOffset * SLOTS_PER_ROW;

        for (int i = 0; i < visibleRows * SLOTS_PER_ROW; i++) {
            int itemIdx = startIdx + i;
            if (itemIdx >= displayItems.size()) break;

            ItemStack stack = displayItems.get(itemIdx);
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int sx = panelLeft + 4 + col * SLOT_SIZE;
            int sy = gridTop + row * SLOT_SIZE;

            // Slot background
            boolean isSelected = stack.getItem() == (selectedStack != null ? selectedStack.getItem() : Items.AIR);
            boolean isHovered = mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;

            if (isSelected) {
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, COL_SEL);
                g.fill(sx, sy, sx + SLOT_SIZE, sy + 1, COL_SEL_BORDER);
                g.fill(sx, sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE, COL_SEL_BORDER);
                g.fill(sx, sy, sx + 1, sy + SLOT_SIZE, COL_SEL_BORDER);
                g.fill(sx + SLOT_SIZE - 1, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, COL_SEL_BORDER);
            } else if (isHovered) {
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, COL_HOVER);
            }

            // Item icon
            g.renderItem(stack, sx + 1, sy + 1);

            // Count label for stacks > 1
            if (stack.getCount() > 1) {
                g.renderItemDecorations(font, stack, sx + 1, sy + 1);
            }

            if (isHovered) hoveredStack = stack;
        }

        g.disableScissor();

        // ── Tooltip ───────────────────────────────────────────────────────────
        if (hoveredStack != null) {
            g.renderTooltip(font, hoveredStack, mx, my);
        }

        // ── Selection preview (rendered above footer, never overlapping buttons) ──
        if (selectedStack != null) {
            int prevY = footerY - 18;
            g.fill(panelLeft, prevY - 1, panelLeft + PANEL_W, prevY, COL_BORDER);
            g.fill(panelLeft, prevY, panelLeft + PANEL_W, footerY, COL_PANEL_DARK);
            g.renderItem(selectedStack, panelLeft + 4, prevY + 1);
            String selName = selectedStack.getHoverName().getString();
            int maxW = PANEL_W - 30;
            if (font.width(selName) > maxW) selName = font.plainSubstrByWidth(selName, maxW - 6) + "…";
            g.drawString(font, "§f" + selName, panelLeft + 22, prevY + 5, COL_TEXT);
        }
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && hoveredStack != null) {
            selectedStack = hoveredStack.copy();
            return true;
        }
        if (btn == 0 && hoveredStack == null) {
            // Double-click anywhere with a selection confirms immediately
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxRows = (int) Math.ceil(displayItems.size() / (double) SLOTS_PER_ROW);
        int gridTop = panelTop + HEADER_H + (activeTab == SourceTab.INVENTORY ? 16 : 34);
        int gridBottom = panelTop + PANEL_H - FOOTER_H - (selectedStack != null ? 20 : 2);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / SLOT_SIZE);

        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(delta), maxRows - visibleRows));
        return true;
    }

    private void confirmSelection() {
        if (selectedStack != null) {
            onPick.accept(selectedStack.copy());
        }
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static boolean isModLoaded(String modId) {
        try {
            return net.minecraftforge.fml.ModList.get().isLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }
}
