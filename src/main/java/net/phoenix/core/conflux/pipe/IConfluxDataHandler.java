package net.phoenix.core.conflux.pipe;

import net.phoenix.core.conflux.ConfluxDataType;

public interface IConfluxDataHandler {

    ConfluxDataType getDataType();

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
