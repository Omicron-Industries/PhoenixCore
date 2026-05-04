package net.phoenix.core.mixin;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.integration.ponder.multiblocks.GTPonderMultiblocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PonderUI.class, remap = false)
public abstract class PonderUIMixin extends AbstractSimiScreen {

    @Inject(method = "mouseClicked", at = @At("HEAD"), remap = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (GTPonderMultiblocks.GT_CURRENT_SHAPE_DATA == null) return;

        PonderScene scene = phoenixcore$getSceneFromUI();
        if (scene == null) return;

        float pt = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        Vec3 start = scene.getTransform().screenToScene(mouseX, mouseY, 0, pt);
        Vec3 end = scene.getTransform().screenToScene(mouseX, mouseY, 1, pt);
        Vec3 ray = end.subtract(start).normalize();

        BlockHitResult hit = scene.getWorld().clip(new net.minecraft.world.level.ClipContext(
                start, start.add(ray.scale(100)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null));

        if (hit.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            var shapeData = GTPonderMultiblocks.GT_CURRENT_SHAPE_DATA;
            var entry = shapeData.getEntryAt(pos);
            if (entry != null && !entry.validBlocks().isEmpty()) {
                // Show info about valid blocks
                List<net.minecraft.world.item.ItemStack> items = entry.validBlocks();
                StringBuilder sb = new StringBuilder("§6Valid blocks for this position:§r");
                for (var stack : items) {
                    sb.append("\n §7• §f").append(stack.getHoverName().getString());
                }
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(sb.toString()), false);
                }
            }
        }
    }

    @Unique
    private PonderScene phoenixcore$getSceneFromUI() {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField("scene");
            field.setAccessible(true);
            return (PonderScene) field.get(this);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method method = this.getClass().getDeclaredMethod("getScene");
                method.setAccessible(true);
                return (PonderScene) method.invoke(this);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
