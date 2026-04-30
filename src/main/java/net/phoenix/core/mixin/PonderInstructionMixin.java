package net.phoenix.core.mixin;

import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.phoenix.core.integration.ponder.util.PonderErrorHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin into the simple PonderInstruction to catch Exceptions, so we can delegate them to the user.
 */
@Mixin(targets = "net.createmod.ponder.foundation.instruction.PonderInstruction$Simple")
public class PonderInstructionMixin {

    @Shadow(remap = false)
    private Consumer<PonderScene> callback;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void init(Consumer<PonderScene> argCallback, CallbackInfo ci) {
        callback = ponderScene -> {
            try {
                argCallback.accept(ponderScene);
            } catch (Throwable e) {
                PonderErrorHelper.yeet(e);
                if (Minecraft.getInstance() != null) {
                    Minecraft.getInstance().setScreen(null);
                }
            }
        };
    }
}
