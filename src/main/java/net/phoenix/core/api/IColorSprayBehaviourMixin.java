package net.phoenix.core.api;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import net.minecraft.world.entity.player.Player;

public interface IColorSprayBehaviourMixin {
    void bridge$recolorAE2(IColorableBlockEntity colorable, Player player);
}
