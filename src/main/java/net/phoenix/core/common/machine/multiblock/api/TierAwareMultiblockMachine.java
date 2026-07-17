package net.phoenix.core.common.machine.multiblock.api;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Drop-in replacement for {@link WorkableElectricMultiblockMachine} that adds automatic
 * multi-tier formation state tracking with two complementary detection mechanisms:
 *
 * <ol>
 * <li><b>{@link IMultiblockTierProvider}</b> — implement on any custom part/hatch you own.</li>
 * <li><b>Tier conditions</b> — predicates registered in the controller's constructor for
 * standard GTM parts (input hatches, output buses, etc.) that you can't modify.</li>
 * </ol>
 *
 * <h2>Minimal custom-hatch usage</h2>
 * <pre>{@code
 * // Hatch:
 * public class MyHatch extends MultiblockPartMachine implements IMultiblockTierProvider {
 * @Override public int getFormationTier() { return 1; }
 * }
 *
 * // Controller:
 * public class MyMachine extends TierAwareMultiblockMachine {
 * public MyMachine(BlockEntityCreationInfo info) {
 * // Pass in the required RecipeLogic (e.g., MultiblockRecipeLogic) and the maxTier
 * super(info, new MultiblockRecipeLogic(this), 1);
 * }
 * }
 * }</pre>
 *
 * <h2>Standard-part detection via tier conditions</h2>
 * <pre>{@code
 * public class MyMachine extends TierAwareMultiblockMachine {
 * public MyMachine(BlockEntityCreationInfo info) {
 * super(info, new MultiblockRecipeLogic(this), 1);
 *
 * // Tier 1 if a PlasmaHatchPartMachine is present:
 * registerTierCondition(1, TierConditions.hasPartOfClass(PlasmaHatchPartMachine.class));
 *
 * // Tier 2 if a specific definition ID is present:
 * registerTierCondition(2, TierConditions.hasDefinition(MY_ULTRA_HATCH));
 *
 * // Compose with AND / OR:
 * registerTierCondition(2,
 * TierConditions.hasPartOfClass(PlasmaHatchPartMachine.class)
 * .and(TierConditions.hasPartAbility(MY_CUSTOM_ABILITY)));
 * }
 * }
 * }</pre>
 *
 * <h2>Builder wiring</h2>
 * <pre>{@code
 * REGISTRATE.multiblock("my_machine", MyMachine::new)
 * .modelProperty(PhoenixMultiblockProperties.FORMATION_TIER, 0)
 * .build();
 * }</pre>
 *
 * <h2>Blockstate JSON</h2>
 * <pre>{@code
 * {
 * "variants": {
 * "formation_tier=0": { "model": "phoenixcore:block/my_machine_formed" },
 * "formation_tier=1": { "model": "phoenixcore:block/my_machine_enhanced" }
 * }
 * }
 * }</pre>
 */
public abstract class TierAwareMultiblockMachine extends WorkableElectricMultiblockMachine {

    // ── State ─────────────────────────────────────────────────────────────────

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private int formationTier = 0;

    private final int maxTier;

    /**
     * Each entry is (requiredTier, condition). During {@link #scanTier()} all conditions
     * are evaluated and the highest tier whose condition passes is returned.
     */
    private final List<TierEntry> tierConditions = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public TierAwareMultiblockMachine(BlockEntityCreationInfo info, int maxTier) {
        super(info); // This is all WorkableElectricMultiblockMachine actually needs!
        this.maxTier = maxTier;
    }

    /**
     * Registers a predicate-based tier condition.
     * Call this in your constructor for standard GTM parts you can't annotate.
     *
     * <p>The predicate receives the full part list after formation and should return
     * {@code true} when the parts needed for {@code tier} are all present.
     *
     * <p>Multiple conditions may be registered for the same tier; any one passing is
     * sufficient. Conditions for different tiers compete; the highest passing tier wins.
     *
     * @param tier      the tier to activate when {@code condition} passes (must be ≥ 1)
     * @param condition predicate over the formed part list
     */
    protected void registerTierCondition(int tier, Predicate<Collection<MultiblockPartMachine>> condition) {
        tierConditions.add(new TierEntry(tier, condition));
    }

    // ── Formation lifecycle ───────────────────────────────────────────────────

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        applyTier(scanTier());
    }

    @Override
    public void invalidateStructure(@NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        applyTier(0);
    }

    // ── Tier computation ──────────────────────────────────────────────────────

    /**
     * Returns the best tier achievable by the current part list.
     *
     * <p>Checks both {@link IMultiblockTierProvider} implementations and any
     * {@link #registerTierCondition registered predicate conditions}, then caps at
     * {@link #maxTier}.
     *
     * <p>Override for fully custom logic (call {@code super.scanTier()} to keep the
     * automatic scanning and layer extra logic on top of it).
     */
    protected int scanTier() {
        Collection<MultiblockPartMachine> parts = getParts();
        int best = 0;

        for (MultiblockPartMachine part : parts) {
            if (part instanceof IMultiblockTierProvider provider) {
                best = Math.max(best, provider.getFormationTier());
            }
        }

        for (TierEntry entry : tierConditions) {
            if (entry.tier > best && entry.condition.test(parts)) {
                best = entry.tier;
            }
        }

        return Math.min(best, maxTier);
    }

    private void applyTier(int tier) {
        if (this.formationTier == tier) return;
        this.formationTier = tier;
        getSyncDataHolder().markClientSyncFieldDirty("formationTier");

        var rs = getRenderState();
        if (rs.hasProperty(PhoenixMultiblockProperties.FORMATION_TIER)) {
            setRenderState(rs.setValue(PhoenixMultiblockProperties.FORMATION_TIER, tier));
        }

        markAsChanged();
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    /** True if the current formation tier is ≥ {@code tier}. */
    public boolean isAtLeastTier(int tier) {
        return isFormed() && formationTier >= tier;
    }

    /** True if the machine is formed at exactly {@code tier}. */
    public boolean isAtTier(int tier) {
        return isFormed() && formationTier == tier;
    }

    /**
     * Re-scans the part list and updates the tier.
     * Call this if a part changes its reported tier mid-operation.
     */
    public void refreshFormationTier() {
        if (!isFormed()) return;
        applyTier(scanTier());
    }

    // ── Internal record ───────────────────────────────────────────────────────

    private record TierEntry(int tier, Predicate<Collection<MultiblockPartMachine>> condition) {}

    // ── Built-in condition factories ──────────────────────────────────────────

    /**
     * Static factory methods for the most common tier-condition patterns.
     * All methods return {@code Predicate<Collection<MultiblockPartMachine>>} so they compose
     * naturally with {@link Predicate#and}, {@link Predicate#or}, and {@link Predicate#negate}.
     *
     * <pre>{@code
     * registerTierCondition(1,
     * TierConditions.hasPartOfClass(PlasmaHatchPartMachine.class)
     * .and(TierConditions.hasPartAbility(MY_SPECIAL_ABILITY)));
     * }</pre>
     */
    public static final class TierConditions {

        private TierConditions() {}

        /**
         * Passes when at least one part is an instance of {@code clazz}.
         * Useful for detecting standard GTM hatches/buses by their Java type.
         *
         * <pre>{@code
         * registerTierCondition(1, TierConditions.hasPartOfClass(FluidHatchPartMachine.class));
         * }</pre>
         */
        public static Predicate<Collection<MultiblockPartMachine>> hasPartOfClass(Class<?> clazz) {
            return parts -> parts.stream().anyMatch(clazz::isInstance);
        }

        /**
         * Passes when at least {@code minCount} parts are instances of {@code clazz}.
         *
         * <pre>{@code
         * registerTierCondition(2, TierConditions.hasPartOfClassAtLeast(2, EnergyHatchPartMachine.class));
         * }</pre>
         */
        public static Predicate<Collection<MultiblockPartMachine>> hasPartOfClassAtLeast(int minCount, Class<?> clazz) {
            return parts -> parts.stream().filter(clazz::isInstance).count() >= minCount;
        }

        /**
         * Passes when at least one part has the given {@link MachineDefinition}.
         * The cleanest way to detect a specific registered machine type.
         *
         * <pre>{@code
         * registerTierCondition(1, TierConditions.hasDefinition(PhoenixMachines.PLASMA_HATCH[GTValues.IV]));
         * }</pre>
         */
        public static Predicate<Collection<MultiblockPartMachine>> hasDefinition(MachineDefinition definition) {
            return parts -> parts.stream()
                    .anyMatch(p -> p != null && p.getDefinition() == definition);
        }

        /**
         * Passes when at least one part's block is registered under the given {@link PartAbility}.
         * This uses GTM's own ability → block registry, so it matches exactly what
         * {@code Predicates.abilities()} matches in pattern definitions.
         *
         * <pre>{@code
         * registerTierCondition(1, TierConditions.hasPartAbility(PartAbility.IMPORT_FLUIDS));
         * }</pre>
         */
        public static Predicate<Collection<MultiblockPartMachine>> hasPartAbility(PartAbility ability) {
            return parts -> {
                var abilityBlocks = ability.getAllBlocks();
                return parts.stream().anyMatch(p -> {
                    // Safe, flattened null check and block retrieval without redundant instanceof
                    if (p == null || p.getLevel() == null || p.getBlockPos() == null) return false;
                    var block = p.getLevel().getBlockState(p.getBlockPos()).getBlock();
                    return abilityBlocks.contains(block);
                });
            };
        }

        /**
         * Passes when none of the parts match the given predicate.
         * Useful for "only upgrade if the forbidden part is absent."
         */
        public static Predicate<Collection<MultiblockPartMachine>> noneMatch(Predicate<MultiblockPartMachine> partPredicate) {
            return parts -> parts.stream().noneMatch(partPredicate);
        }
    }
}