package net.phoenix.core.conflux.pipe;

import net.phoenix.core.conflux.ConfluxDataType;

/**
 * Capability for blocks that handle multiple Axiom data types simultaneously.
 * Used by the Research Terminal and multi-output producer machines.
 *
 * Pipes check for this after failing to find {@link IConfluxDataHandler}, allowing
 * a single terminal face to accept any incoming data type.
 */
public interface IConfluxMultiHandler {

    long insert(ConfluxDataType type, long amount);
    long extract(ConfluxDataType type, long amount);
    long getStored(ConfluxDataType type);
    long getCapacity(ConfluxDataType type);

    default boolean canInsert(ConfluxDataType type) { return getStored(type) < getCapacity(type); }
}
