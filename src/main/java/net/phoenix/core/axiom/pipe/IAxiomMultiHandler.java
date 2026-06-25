package net.phoenix.core.axiom.pipe;

import net.phoenix.core.axiom.AxiomDataType;

/**
 * Capability for blocks that handle multiple Axiom data types simultaneously.
 * Used by the Research Terminal and multi-output producer machines.
 *
 * Pipes check for this after failing to find {@link IAxiomDataHandler}, allowing
 * a single terminal face to accept any incoming data type.
 */
public interface IAxiomMultiHandler {

    long insert(AxiomDataType type, long amount);
    long extract(AxiomDataType type, long amount);
    long getStored(AxiomDataType type);
    long getCapacity(AxiomDataType type);

    default boolean canInsert(AxiomDataType type) { return getStored(type) < getCapacity(type); }
}
