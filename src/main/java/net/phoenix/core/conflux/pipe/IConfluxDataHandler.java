package net.phoenix.core.conflux.pipe;

import net.phoenix.core.conflux.ConfluxDataType;

/**
 * Capability interface for blocks that participate in an Axiom data network.
 *
 * Producers (research machines) expose this on their output face.
 * The Research Terminal exposes it on all input faces.
 * Pipes read adjacent handlers to route data each tick.
 */
public interface IConfluxDataHandler {

    /** The data type this handler accepts/produces. Pipes only connect to matching types. */
    ConfluxDataType getDataType();

    /**
     * Insert up to {@code amount} units of data.
     * @return the amount actually accepted (maybe less if near capacity).
     */
    long insert(long amount);

    /**
     * Extract up to {@code amount} units of data.
     * @return the amount actually extracted.
     */
    long extract(long amount);

    /** Current stored amount. */
    long getStored();

    /** Maximum this handler can hold. */
    long getCapacity();

    default boolean canInsert() { return getStored() < getCapacity(); }
    default boolean isEmpty()   { return getStored() == 0; }
    default boolean isFull()    { return getStored() >= getCapacity(); }
}
