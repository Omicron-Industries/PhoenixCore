package net.phoenix.core.common.machine.multiblock;

import net.phoenix.core.common.machine.multiblock.Shield.ShieldTypes;

public interface ShieldedMachine {

    ShieldTypes getShieldType();

    int getShieldHealth();

    default boolean isShieldActive() {
        return getShieldType().isActive && getShieldHealth() > 0;
    }
}
