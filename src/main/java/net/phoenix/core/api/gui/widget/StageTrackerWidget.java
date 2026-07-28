package net.phoenix.core.api.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class StageTrackerWidget {

    private StageTrackerWidget() {}

    private static final int PIP_SPACING = 10;

    public static List<IWidget> of(PanelSyncManager syncManager, String key, IntSupplier stage, int maxStage) {
        IntSyncValue value = syncManager.getOrCreateSyncHandler(key, IntSyncValue.class,
                () -> new IntSyncValue(stage));

        List<IWidget> pips = new ArrayList<>();
        for (int i = 0; i <= maxStage; i++) {
            int index = i;
            pips.add(new TextWidget<>(Text.dynamic(() -> Component
                    .literal(index <= value.getIntValue() ? "■" : "□")
                    .withStyle(index <= value.getIntValue() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)))
                    .left(index * PIP_SPACING));
        }
        return pips;
    }
}
