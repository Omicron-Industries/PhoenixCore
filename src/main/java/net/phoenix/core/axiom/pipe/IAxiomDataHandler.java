package net.phoenix.core.axiom.pipe;

import net.phoenix.core.axiom.AxiomDataType;

public interface IAxiomDataHandler {

    AxiomDataType getDataType();

    long insert(long amount);

    long extract(long amount);

    long getStored();

    long getCapacity();

    default boolean canInsert() {
        return getStored() < getCapacity();
    }

    default boolean isEmpty() {
        return getStored() == 0;
    }

    default boolean isFull() {
        return getStored() >= getCapacity();
    }
}
