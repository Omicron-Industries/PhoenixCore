package net.phoenix.core.integration.ars_nouveau.client;

import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.Widget;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.api.gui.BorderDrawable;
import net.phoenix.core.api.gui.SolidColorDrawable;

/**
 * MUI2 port of the old MUI1 SoulMapWidget.
 *
 * The MUI1 version drew the soul-density grid itself in drawInBackground(GuiGraphics, ...) using
 * graphics.fill(...)/renderOutline(...). MUI2 widgets don't draw directly the same way -- custom
 * visuals go through IDrawable (GuiContext#getGraphics(), confirmed via SourceHatchBackground)
 * attached as a background/overlay, not drawn inline in a widget's own draw hook.
 *
 * Rebuilt as composition: one small child widget per chunk cell, each with a SolidColorDrawable
 * background colored by that cell's soul density, plus a BorderDrawable overlay on the center
 * cell (the player's current chunk) matching the original's white outline.
 */
public class SoulMapWidget extends ParentWidget<SoulMapWidget> {

    private static final int RADIUS = 8;
    private static final int CELL_SIZE = 6;
    private static final double VISIBLE_RADIUS = 8.5;

    private final ItemStack stack;

    public SoulMapWidget(ItemStack stack) {
        this.stack = stack;
        this.size((RADIUS * 2 + 1) * CELL_SIZE);
        rebuildGrid();
    }

    /**
     * Rebuilds the cell grid from the stack's current "MapData" NBT. Call this again (e.g. from an
     * onUpdateListener) if the map needs to refresh after the panel is already open -- this
     * implementation only builds the grid once at construction, matching how the rest of this
     * port avoids unconfirmed "reactive child replace" APIs (see TeslaBinderItem's notes on the
     * same limitation).
     */
    private void rebuildGrid() {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("MapData")) return;

        ListTag mapData = tag.getList("MapData", Tag.TAG_COMPOUND);

        for (int i = 0; i < mapData.size(); i++) {
            CompoundTag chunk = mapData.getCompound(i);
            int rx = chunk.getInt("relX");
            int rz = chunk.getInt("relZ");

            double dist = Math.sqrt((double) rx * rx + (double) rz * rz);
            if (dist > VISIBLE_RADIUS) continue;

            float density = chunk.getFloat("density");
            int color = getSoulColor(density);

            int drawX = (rx + RADIUS) * CELL_SIZE;
            int drawY = (rz + RADIUS) * CELL_SIZE;

            Cell cell = new Cell();
            cell.pos(drawX, drawY);
            cell.size(CELL_SIZE);
            cell.background(new SolidColorDrawable(color));

            boolean isCenterCell = rx == 0 && rz == 0;
            if (isCenterCell) {
                cell.overlay(new BorderDrawable(0xFFFFFFFF));
            }

            this.child(cell);
        }
    }

    private int getSoulColor(float density) {
        float factor = Math.min(density / 2.5f, 1.0f);
        int r = (int) (30 + (180 * factor));
        int g = (int) (10 * factor);
        int b = (int) (50 + (205 * factor));
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Plain leaf widget for a single grid cell. Named (not anonymous) because Java doesn't allow
     * diamond inference ('<>') on anonymous classes, and Widget<W>'s self-bounded generic
     * (W extends Widget<W>) needs a concrete type argument either way. Mirrors the confirmed
     * DrawableWidget extends Widget<DrawableWidget> pattern.
     */
    private static class Cell extends Widget<Cell> {}
}