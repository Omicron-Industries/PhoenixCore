package net.phoenix.core.integration.ae2.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.integration.ae2.METagInputHatchPartMachine;

import org.jetbrains.annotations.NotNull;

public class FluidPreviewWidget extends Widget {

    private final int index;
    private final METagInputHatchPartMachine machine;

    public FluidPreviewWidget(int x, int y, int index, METagInputHatchPartMachine machine) {
        super(new Position(x, y), new Size(18, 18));
        this.index = index;
        this.machine = machine;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Position pos = getPosition();
        GuiTextures.FLUID_SLOT.draw(graphics, mouseX, mouseY, pos.x, pos.y, 18, 18);

        if (machine.previewFluids == null || index < 0 || index >= machine.previewFluids.length) return;

        FluidStack stack = machine.previewFluids[index];
        long amount = machine.previewAmounts[index];

        if (stack == null || stack.isEmpty()) return;

        DrawerHelper.drawFluidForGui(
                graphics,
                FluidHelperImpl.toFluidStack(stack),
                amount,
                pos.x + 1,
                pos.y + 1,
                16,
                16);

        if (amount > 0) {
            String txt = TextFormattingUtil.formatLongToCompactStringBuckets(amount, 3) + "B";
            DrawerHelper.drawStringFixedCorner(
                    graphics, txt,
                    pos.x + 17, pos.y + 17,
                    0xFFFFFFFF, true, 0.45f);
        }
    }

    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isMouseOverElement(mouseX, mouseY)) return;

        FluidStack stack = machine.previewFluids[index];
        if (stack == null || stack.isEmpty()) return;

        graphics.renderTooltip(
                Minecraft.getInstance().font,
                stack.getDisplayName(),
                mouseX, mouseY);
    }
}
