package net.phoenix.core.mixin.gtceu;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.util.AEColor;
import com.gregtechceu.gtceu.common.item.ColorSprayBehaviour;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.phoenix.core.api.IColorSprayBehaviourMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ColorSprayBehaviour.class, remap = false)
public abstract class ColorSprayBehaviourMixin implements IColorSprayBehaviourMixin {

    @Final
    @Shadow
    private DyeColor color;

    @Override
    public void bridge$recolorAE2(IColorableBlockEntity colorable, Player player) {
        var ae2Color = this.color == null ? AEColor.TRANSPARENT : AEColor.values()[this.color.ordinal()];
        if (colorable.getColor() != ae2Color) {
            colorable.recolourBlock(null, ae2Color, player);
        }
    }
}
