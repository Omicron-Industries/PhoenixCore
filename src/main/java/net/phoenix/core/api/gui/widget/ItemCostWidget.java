package net.phoenix.core.api.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandlerModifiable;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.slot.ItemSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class ItemCostWidget {

    private ItemCostWidget() {}

    public static List<IWidget> of(IItemHandlerModifiable inventory, int slotIndex, IntSupplier have,
                                   IntSupplier need) {
        List<IWidget> widgets = new ArrayList<>();

        widgets.add(new ItemSlot()
                .slot(SyncHandlers.itemSlot(inventory, slotIndex))
                .tooltip(new RichTooltip().addLine(Text.dynamic(() -> readout(have, need)))));

        widgets.add(new TextWidget<>(Text.dynamic(() -> readout(have, need))));

        return widgets;
    }

    private static Component readout(IntSupplier have, IntSupplier need) {
        int h = have.getAsInt();
        int n = need.getAsInt();
        return Component.literal(h + " / " + n).withStyle(h >= n ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
