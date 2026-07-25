package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FluidPickerScreen extends Screen {

    private static final int COL_BG = 0xFF0F0F13;
    private static final int COL_PANEL = 0xFF16161C;
    private static final int COL_PANEL_DARK = 0xFF111115;
    private static final int COL_HEADER = 0xFF0C0C10;
    private static final int COL_BORDER = 0xFF2A2A36;
    private static final int COL_BORDER_LIT = 0xFF3A3A4C;
    private static final int COL_SEL = 0xFF0D1A2E;
    private static final int COL_SEL_BORDER = 0xFF0088FF;
    private static final int COL_HOVER = 0xFF1E1E2A;
    private static final int COL_TEXT = 0xFFDDDDE8;
    private static final int COL_TEXT_DIM = 0xFF888898;
    private static final int COL_TEXT_FAINT = 0xFF4A4A5A;

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 200;
    private static final int HEADER_H = 22;
    private static final int SEARCH_H = 16;
    private static final int FOOTER_H = 22;
    private static final int ROW_H = 20;

    private final Screen parent;
    private final Consumer<String> onPick; 

    private final List<Fluid> displayFluids = new ArrayList<>();
    private int scrollOffset = 0;
    private Fluid hoveredFluid = null;
    private Fluid selectedFluid = null;

    private EditBox searchBox;
    private String searchQuery = "";

    private int panelLeft, panelTop;

    public FluidPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.literal("Pick Fluid"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        clearWidgets();
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;

        int searchY = panelTop + HEADER_H + 2;
        searchBox = new EditBox(font, panelLeft + 4, searchY, PANEL_W - 8, SEARCH_H, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("§8Search fluids…"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(q -> {
            searchQuery = q;
            scrollOffset = 0;
            rebuildList();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("§aSelect"), b -> confirm())
                .bounds(panelLeft + PANEL_W - 56, panelTop + PANEL_H - FOOTER_H + 3, 52, 14).build());
        addRenderableWidget(Button.builder(Component.literal("§7Cancel"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(panelLeft + 4, panelTop + PANEL_H - FOOTER_H + 3, 52, 14).build());

        rebuildList();
    }

    private void rebuildList() {
        displayFluids.clear();
        String q = searchQuery.toLowerCase().trim();
        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            if (fluid == Fluids.EMPTY) continue;
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
            if (id == null) continue;
            
            if (id.getPath().contains("flowing")) continue;
            if (!q.isEmpty() && !id.toString().contains(q) &&
                    !fluid.getFluidType().getDescription().getString().toLowerCase().contains(q))
                continue;
            displayFluids.add(fluid);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x88000000);

        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL);
        drawBorder(g, panelLeft, panelTop, PANEL_W, PANEL_H, COL_BORDER_LIT);

        g.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + HEADER_H, COL_HEADER);
        g.fill(panelLeft, panelTop + HEADER_H - 1, panelLeft + PANEL_W, panelTop + HEADER_H, COL_BORDER);
        g.drawCenteredString(font, "§3Pick Fluid", panelLeft + PANEL_W / 2, panelTop + 7, COL_TEXT);

        int footerY = panelTop + PANEL_H - FOOTER_H;
        g.fill(panelLeft, footerY, panelLeft + PANEL_W, footerY + 1, COL_BORDER);
        g.fill(panelLeft, footerY, panelLeft + PANEL_W, panelTop + PANEL_H, COL_PANEL_DARK);
        g.drawString(font, "§8" + displayFluids.size() + " fluids", panelLeft + 62, footerY + 7, COL_TEXT_FAINT);

        super.render(g, mx, my, partial);

        int listTop = panelTop + HEADER_H + SEARCH_H + 4;
        int listBottom = footerY - (selectedFluid != null ? 20 : 2);
        int visRows = Math.max(1, (listBottom - listTop) / ROW_H);

        g.enableScissor(panelLeft, listTop, panelLeft + PANEL_W, listBottom);
        hoveredFluid = null;

        for (int i = 0; i < visRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= displayFluids.size()) break;
            Fluid fluid = displayFluids.get(idx);
            int ry = listTop + i * ROW_H;

            boolean hov = mx >= panelLeft + 2 && mx < panelLeft + PANEL_W - 2 && my >= ry && my < ry + ROW_H;
            boolean sel = fluid == selectedFluid;

            if (sel) {
                g.fill(panelLeft + 2, ry, panelLeft + PANEL_W - 2, ry + ROW_H, COL_SEL);
                g.fill(panelLeft + 2, ry, panelLeft + PANEL_W - 2, ry + 1, COL_SEL_BORDER);
                g.fill(panelLeft + 2, ry + ROW_H - 1, panelLeft + PANEL_W - 2, ry + ROW_H, COL_SEL_BORDER);
            } else if (hov) {
                g.fill(panelLeft + 2, ry, panelLeft + PANEL_W - 2, ry + ROW_H, COL_HOVER);
            }

            int swatchColor = getFluidColor(fluid);
            g.fill(panelLeft + 4, ry + 2, panelLeft + 20, ry + ROW_H - 2, swatchColor | 0xFF000000);
            drawBorder(g, panelLeft + 4, ry + 2, 16, ROW_H - 4, 0xFF444455);

            String name = fluid.getFluidType().getDescription().getString();
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
            String idStr = id != null ? "§8" + id : "";
            int labelX = panelLeft + 24;
            int maxW = PANEL_W - 28;
            if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - 4) + "…";
            g.drawString(font, (sel ? "§f" : "§7") + name, labelX, ry + 3, COL_TEXT_DIM);
            if (font.width(idStr) <= maxW)
                g.drawString(font, idStr, labelX, ry + 12, COL_TEXT_FAINT);

            if (hov) hoveredFluid = fluid;
        }
        g.disableScissor();

        if (selectedFluid != null) {
            int prevY = footerY - 18;
            g.fill(panelLeft, prevY - 1, panelLeft + PANEL_W, prevY, COL_BORDER);
            g.fill(panelLeft, prevY, panelLeft + PANEL_W, footerY, COL_PANEL_DARK);
            int sc = getFluidColor(selectedFluid) | 0xFF000000;
            g.fill(panelLeft + 4, prevY + 1, panelLeft + 20, prevY + 17, sc);
            drawBorder(g, panelLeft + 4, prevY + 1, 16, 16, 0xFF444455);
            ResourceLocation selId = ForgeRegistries.FLUIDS.getKey(selectedFluid);
            String selStr = selId != null ? selId.toString() : "?";
            g.drawString(font, "§f" + selStr, panelLeft + 24, prevY + 5, COL_TEXT);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && hoveredFluid != null) {
            selectedFluid = hoveredFluid;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int footerY = panelTop + PANEL_H - FOOTER_H;
        int listTop = panelTop + HEADER_H + SEARCH_H + 4;
        int listBottom = footerY - (selectedFluid != null ? 20 : 2);
        int visRows = Math.max(1, (listBottom - listTop) / ROW_H);
        int maxScroll = Math.max(0, displayFluids.size() - visRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(delta), maxScroll));
        return true;
    }

    private void confirm() {
        if (selectedFluid != null) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(selectedFluid);
            if (id != null) onPick.accept(id.toString());
        }
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private int getFluidColor(Fluid fluid) {
        try {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
            return ext.getTintColor();
        } catch (Exception e) {
            return 0x3355FF; 
        }
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
