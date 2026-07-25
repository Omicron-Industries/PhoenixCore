package net.phoenix.core.common.machine.multiblock.api;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhoenixMultiblockProperties {

    public static final IntegerProperty FORMATION_TIER = IntegerProperty.create("formation_tier", 0, 7);
}
