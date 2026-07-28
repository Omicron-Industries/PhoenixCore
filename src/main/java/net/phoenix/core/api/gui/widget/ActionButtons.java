package net.phoenix.core.api.gui.widget;

import net.minecraft.network.FriendlyByteBuf;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ActionButtons {

    private ActionButtons() {}

    @FunctionalInterface
    public interface ServerAction {

        void run(FriendlyByteBuf payload);
    }

    public static ButtonWidget<?> simple(PanelSyncManager syncManager, String actionKey, Runnable onServer,
                                         IDrawable overlay, RichTooltip tooltip) {
        return payload(syncManager, actionKey, buf -> onServer.run(), buf -> {}, overlay, tooltip);
    }

    public static ButtonWidget<?> payload(PanelSyncManager syncManager, String actionKey, ServerAction onServer,
                                          Consumer<FriendlyByteBuf> clientPayload, IDrawable overlay,
                                          RichTooltip tooltip) {
        syncManager.registerServerSyncedAction(actionKey, onServer::run);
        return new ButtonWidget<>()
                .overlay(overlay)
                .tooltip(tooltip)
                .onMousePressed((context, button) -> {
                    syncManager.callSyncedAction(actionKey, clientPayload);
                    return true;
                });
    }

    public static ButtonWidget<?> gated(PanelSyncManager syncManager, String actionKey, Supplier<Boolean> enabled,
                                        Runnable onServer, IDrawable activeOverlay, IDrawable disabledOverlay,
                                        RichTooltip tooltip) {
        syncManager.registerServerSyncedAction(actionKey, buf -> {
            if (Boolean.TRUE.equals(enabled.get())) onServer.run();
        });
        return new ButtonWidget<>()
                .overlay(new DynamicDrawable(() -> Boolean.TRUE.equals(enabled.get()) ? activeOverlay :
                        disabledOverlay))
                .tooltip(tooltip)
                .onMousePressed((context, button) -> {
                    if (Boolean.TRUE.equals(enabled.get())) {
                        syncManager.callSyncedAction(actionKey);
                        return true;
                    }
                    return false;
                });
    }
}
