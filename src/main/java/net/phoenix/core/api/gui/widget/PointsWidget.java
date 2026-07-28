package net.phoenix.core.api.gui.widget;

import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class PointsWidget {

    private PointsWidget() {}

    public static IWidget display(PanelSyncManager syncManager, String key, IntSupplier getter,
                                  Function<Integer, Component> label) {
        IntSyncValue value = syncManager.getOrCreateSyncHandler(key, IntSyncValue.class,
                () -> new IntSyncValue(getter));
        return new TextWidget<>(Text.dynamic(() -> label.apply(value.getIntValue())));
    }

    public static List<IWidget> withSpendButton(PanelSyncManager syncManager, String key, IntSupplier getter,
                                                IntConsumer setter, int amount, Function<Integer, Component> label,
                                                IDrawable buttonOverlay, RichTooltip buttonTooltip) {
        IntSyncValue value = syncManager.getOrCreateSyncHandler(key, IntSyncValue.class,
                () -> new IntSyncValue(getter, setter));

        ButtonWidget<?> spend = ActionButtons.simple(syncManager, key + "_spend", () -> {
            int current = getter.getAsInt();
            if (current >= amount) setter.accept(current - amount);
        }, buttonOverlay, buttonTooltip);

        List<IWidget> widgets = new ArrayList<>();
        widgets.add(new TextWidget<>(Text.dynamic(() -> label.apply(value.getIntValue()))));
        widgets.add(spend);
        return widgets;
    }
}
