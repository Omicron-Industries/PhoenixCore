package net.phoenix.core.conflux.pipe;

import net.phoenix.core.conflux.ConfluxDataType;

public interface IConfluxMultiHandler {

    long insert(ConfluxDataType type, long amount);
    long extract(ConfluxDataType type, long amount);
    long getStored(ConfluxDataType type);
    long getCapacity(ConfluxDataType type);

    default boolean canInsert(ConfluxDataType type) { return getStored(type) < getCapacity(type); }
}
