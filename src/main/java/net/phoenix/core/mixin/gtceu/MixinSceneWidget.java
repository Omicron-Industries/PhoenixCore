package net.phoenix.core.mixin.gtceu;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import net.minecraft.client.Minecraft;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SceneWidget.class, remap = false)
public abstract class MixinSceneWidget {
    @Shadow protected float rotationYaw;
    @Shadow protected float rotationPitch;

    @Unique
    public void phantasia$setRotation(float yaw, float pitch) {
        this.rotationYaw = yaw;
        this.rotationPitch = pitch;
    }


    @Redirect(
            method = "drawInBackground",
            at = @At(value = "INVOKE", target = "Lcom/lowdragmc/lowdraglib/client/scene/WorldSceneRenderer;isCompiling()Z")
    )
    private boolean phantasia$silenceCompilingMessage(WorldSceneRenderer instance) {
        // Only return false (hiding the text) if our specific screen is open
        if (Minecraft.getInstance().screen instanceof PhantasiaSceneScreen) {
            return false;
        }

        // Otherwise, let the default LDLib behavior happen
        return instance.isCompiling();
    }
}