package net.phoenix.core.mixin.emi;

import net.minecraft.client.Minecraft;
import net.phoenix.core.client.emi.EmiFavoritePagesScreen;

import dev.emi.emi.config.SidebarType;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiScreenManager.class, remap = false)
public abstract class EmiScreenManagerMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private static void phoenixcore$onMouseClicked(double mouseX, double mouseY, int button,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (EmiScreenBase.getCurrent().isEmpty()) return;

        for (EmiScreenManager.SidebarPanel panel : EmiScreenManagerAccessor.phoenixcore$getPanels()) {
            if (panel.getType() != SidebarType.FAVORITES || !panel.isVisible() || panel.space == null) continue;
            if (!panel.cycle.isMouseOver(mouseX, mouseY)) continue;

            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new EmiFavoritePagesScreen(mc.screen));
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
    }
}
