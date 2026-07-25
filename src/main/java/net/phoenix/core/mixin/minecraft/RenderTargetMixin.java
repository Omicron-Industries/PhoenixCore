package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @Shadow(remap = false)
    private boolean stencilEnabled;

    @Shadow
    public int viewWidth;

    @Shadow
    public int viewHeight;

    @Shadow
    public abstract void resize(int width, int height, boolean clearError);

    @Overwrite(remap = false)
    public void enableStencil() {
        if (!this.stencilEnabled) {
            this.stencilEnabled = true;
            try {
                this.resize(this.viewWidth, this.viewHeight, Minecraft.ON_OSX);
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Cannot get config value")) {
                    this.stencilEnabled = false;
                    System.out.println("[PhoenixCore] Bypassed early stencil config access check successfully.");
                } else {
                    throw e;
                }
            }
        }
    }
}
