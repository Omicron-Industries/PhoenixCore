package net.phoenix.core.common.machine.multiblock.api;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public abstract class TierAwareMultiblockMachine extends WorkableElectricMultiblockMachine {

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private int formationTier = 0;

    private final int maxTier;

    private final List<TierEntry> tierConditions = new ArrayList<>();

    public TierAwareMultiblockMachine(BlockEntityCreationInfo info, int maxTier) {
        super(info);
        this.maxTier = maxTier;
    }

    protected void registerTierCondition(int tier, Predicate<Collection<MultiblockPartMachine>> condition) {
        tierConditions.add(new TierEntry(tier, condition));
    }

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

    public boolean isAtLeastTier(int tier) {
        return isFormed() && formationTier >= tier;
    }

    public boolean isAtTier(int tier) {
        return isFormed() && formationTier == tier;
    }

    public void refreshFormationTier() {
        if (!isFormed()) return;
        applyTier(scanTier());
    }

    private record TierEntry(int tier, Predicate<Collection<MultiblockPartMachine>> condition) {}

    public static final class TierConditions {

        private TierConditions() {}

        public static Predicate<Collection<MultiblockPartMachine>> hasPartOfClass(Class<?> clazz) {
            return parts -> parts.stream().anyMatch(clazz::isInstance);
        }

        public static Predicate<Collection<MultiblockPartMachine>> hasPartOfClassAtLeast(int minCount, Class<?> clazz) {
            return parts -> parts.stream().filter(clazz::isInstance).count() >= minCount;
        }

        public static Predicate<Collection<MultiblockPartMachine>> hasDefinition(MachineDefinition definition) {
            return parts -> parts.stream()
                    .anyMatch(p -> p != null && p.getDefinition() == definition);
        }

        public static Predicate<Collection<MultiblockPartMachine>> hasPartAbility(PartAbility ability) {
            return parts -> {
                var abilityBlocks = ability.getAllBlocks();
                return parts.stream().anyMatch(p -> {

                    if (p == null || p.getLevel() == null || p.getBlockPos() == null) return false;
                    var block = p.getLevel().getBlockState(p.getBlockPos()).getBlock();
                    return abilityBlocks.contains(block);
                });
            };
        }

        public static Predicate<Collection<MultiblockPartMachine>> noneMatch(Predicate<MultiblockPartMachine> partPredicate) {
            return parts -> parts.stream().noneMatch(partPredicate);
        }
    }
}
