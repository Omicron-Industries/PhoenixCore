package net.phoenix.core.integration.ars_nouveau.client;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public class SoulMapWidget extends Widget {

    private final ItemStack stack;

    public SoulMapWidget(int x, int y, ItemStack stack) {
        super(x, y, 102, 102);
        this.stack = stack;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("MapData")) return;

        ListTag mapData = tag.getList("MapData", Tag.TAG_COMPOUND);

        int renderX = getPosition().x;
        int renderY = getPosition().y;
        int cellSize = 6;

        for (int i = 0; i < mapData.size(); i++) {
            CompoundTag chunk = mapData.getCompound(i);
            int rx = chunk.getInt("relX");
            int rz = chunk.getInt("relZ");

            double dist = Math.sqrt(rx * rx + rz * rz);
            if (dist > 8.5) continue;

            int drawX = renderX + ((rx + 8) * cellSize);
            int drawY = renderY + ((rz + 8) * cellSize);

            float density = chunk.getFloat("density");
            int color = getSoulColor(density);

            graphics.fill(drawX, drawY, drawX + cellSize - 1, drawY + cellSize - 1, color);
        }

        int centerX = renderX + (8 * cellSize);
        int centerY = renderY + (8 * cellSize);
        graphics.renderOutline(centerX, centerY, cellSize, cellSize, 0xFFFFFFFF);
    }

    private int getSoulColor(float density) {
        float factor = Math.min(density / 2.5f, 1.0f);
        int r = (int) (30 + (180 * factor));
        int g = (int) (10 * factor);
        int b = (int) (50 + (205 * factor));
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
}
