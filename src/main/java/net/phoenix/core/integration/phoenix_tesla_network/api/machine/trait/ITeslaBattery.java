package net.phoenix.core.integration.phoenix_tesla_network.api.machine.trait;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public interface ITeslaBattery {

    BigInteger getMaxInput();

    BigInteger getMaxOutput();

    BigInteger getCapacity();

    int getTier();

    @NotNull
    String getBatteryName();

    BigInteger getStored();

    void setStored(BigInteger amount);
}
