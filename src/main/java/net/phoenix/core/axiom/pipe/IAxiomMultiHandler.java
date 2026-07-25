package net.phoenix.core.axiom.pipe;

import net.phoenix.core.axiom.AxiomDataType;

public interface IAxiomMultiHandler {

    long insert(AxiomDataType type, long amount);

    long extract(AxiomDataType type, long amount);

    long getStored(AxiomDataType type);

    long getCapacity(AxiomDataType type);

    default boolean canInsert(AxiomDataType type) {
        return getStored(type) < getCapacity(type);
    }
}
