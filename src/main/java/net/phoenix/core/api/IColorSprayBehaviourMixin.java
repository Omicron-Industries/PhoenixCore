package net.phoenix.core.api;

import net.minecraft.world.entity.player.Player;

import appeng.api.implementations.blockentities.IColorableBlockEntity;

public interface IColorSprayBehaviourMixin {

    void bridge$recolorAE2(IColorableBlockEntity colorable, Player player);
}
