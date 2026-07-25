package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.phoenix.core.utils.CompactCount;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsCountSuffixMixin {

    @Unique
    private boolean phoenix$isInsideTextField() {
        return StackWalker.getInstance()
                .walk(frames -> frames.anyMatch(
                        frame -> frame.getClassName().contains("com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget") ||
                                frame.getClassName().contains("net.minecraft.client.gui.components.EditBox") ||
                                frame.getClassName().contains("appeng.client.gui.widgets") ||
                                frame.getClassName().contains("appeng.client.gui.me.crafting.CraftingCPUScreen")));
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$compactString(Font font, String text, int x, int y, int color, boolean dropShadow,
                                       CallbackInfoReturnable<Integer> cir) {
        if (text == null || phoenix$isInsideTextField()) return;

        String compacted = CompactCount.compactIfNumeric(text);

        if (compacted != null && !compacted.equals(text)) {
            cir.setReturnValue(this.phoenix$renderScaled(font, compacted, x, y, color, dropShadow));
        }
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$compactSequence(Font font, FormattedCharSequence text, int x, int y, int color,
                                         boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || phoenix$isInsideTextField()) return;

        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        String original = sb.toString();
        String compacted = CompactCount.compactIfNumeric(original);

        if (compacted != null && !compacted.equals(original)) {
            cir.setReturnValue(this.phoenix$renderScaled(font, compacted, x, y, color, dropShadow));
        }
    }

    @Unique
    private int phoenix$renderScaled(Font font, String text, int x, int y, int color, boolean shadow) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        float scale = 0.70f;
        float originalWidth = font.width(text);
        float scaledWidth = originalWidth * scale;
        float xOffset = originalWidth - scaledWidth;

        graphics.pose().pushPose();
        graphics.pose().translate(x + xOffset, y + 2, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        int result = graphics.drawString(font, text, 0, 0, color, shadow);
        graphics.pose().popPose();
        return result;
    }
}
