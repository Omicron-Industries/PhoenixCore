package net.phoenix.core.mixin.minecraft;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostPass;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.mixin.accessor.PostChainAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    private void phoenixcore$injectUniforms(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.currentEffect() instanceof PostChainAccessor accessor) {
            for (PostPass pass : accessor.getPasses()) {
                if (pass.getEffect().getName().equals("phoenixcore:soul_vision")) {
                    // --- ALWAYS-ON SHADER TEST ---
                    // Hardcode saturation to 0.0 for grayscale.
                    Uniform uniform = pass.getEffect().getUniform("Saturation");
                    if (uniform != null) {
                        uniform.set(0.0f);
                    }
                }
            }
        }
    }
}
