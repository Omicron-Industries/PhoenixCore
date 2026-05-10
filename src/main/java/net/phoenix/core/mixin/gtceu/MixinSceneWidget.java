package net.phoenix.core.mixin.gtceu;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SceneWidget.class, remap = false)
public abstract class MixinSceneWidget {

    @Inject(
            method = "drawInBackground",
            at = @At(value = "INVOKE",
                     target = "Lcom/lowdragmc/lowdraglib/client/scene/WorldSceneRenderer;isCompiling()Z"),
            cancellable = true)
    private void phantasia$silenceCompilingText(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks,
                                                CallbackInfo ci) {
        // 1. Check if the current screen is Phantasia
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof net.phoenix.core.integration.phantasia.PhantasiaSceneScreen) {

            // 2. We manually call RenderSystem fixes that happen AFTER the text block
            // because cancelling the method now would skip the rest of drawInBackground
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            // 3. CANCEL the rest of the method execution
            // This prevents the 'if (isCompiling)' block from ever running.
            ci.cancel();
        }
    }
}
