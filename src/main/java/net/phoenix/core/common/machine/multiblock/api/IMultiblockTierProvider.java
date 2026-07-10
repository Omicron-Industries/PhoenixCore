package net.phoenix.core.common.machine.multiblock.api;

/**
 * Implement on any {@link com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine}
 * (hatch, bus, etc.) to make it contribute a formation tier to its controller.
 *
 * <p>When the controller finishes a structure check it scans all parts for this interface
 * and picks the highest {@link #getFormationTier()} value it finds. That value is then
 * stored on the controller as its <em>active formation tier</em> and is synced to the
 * client so the block model can change accordingly.
 *
 * <p>Tier 0 is the "normally formed" baseline — you never need to return 0 here because
 * any formed structure is already at tier 0. Return 1 or higher to upgrade the controller
 * to a more advanced appearance.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MySpecialHatch extends MultiblockPartMachine implements IMultiblockTierProvider {
 *
 *     @Override
 *     public int getFormationTier() { return 1; }
 * }
 * }</pre>
 */
public interface IMultiblockTierProvider {

    /**
     * The formation tier this part contributes to the controller when it is present in a
     * fully formed structure. Must be ≥ 1 (tier 0 is implicit for any formed structure).
     */
    int getFormationTier();
}
