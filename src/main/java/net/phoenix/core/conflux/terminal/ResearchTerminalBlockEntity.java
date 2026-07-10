package net.phoenix.core.conflux.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.conflux.pipe.IConfluxMultiHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Research Terminal accumulates Axiom data delivered by pipes and exposes it
 * to the player for spending on research nodes.
 *
 * Accepts all five data types on any face via {@link IConfluxMultiHandler}.
 * Storage is per-type; capacity is intentionally large so the terminal acts as a
 * proper reservoir rather than a bottleneck.
 */
public class ResearchTerminalBlockEntity extends BlockEntity {

    public static final long CAPACITY_PER_TYPE = 1_000_000L;

    private final Map<ConfluxDataType, Long> stored = new EnumMap<>(ConfluxDataType.class);
    private final LazyOptional<IConfluxMultiHandler> handlerOpt;

    public ResearchTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (ConfluxDataType dt : ConfluxDataType.values()) stored.put(dt, 0L);
        handlerOpt = LazyOptional.of(this::buildHandler);
    }

    private IConfluxMultiHandler buildHandler() {
        return new IConfluxMultiHandler() {
            @Override
            public long insert(ConfluxDataType type, long amount) {
                long have     = stored.getOrDefault(type, 0L);
                long accepted = Math.min(amount, CAPACITY_PER_TYPE - have);
                if (accepted <= 0) return 0;
                stored.put(type, have + accepted);
                setChanged();
                return accepted;
            }
            @Override
            public long extract(ConfluxDataType type, long amount) {
                long have  = stored.getOrDefault(type, 0L);
                long given = Math.min(amount, have);
                stored.put(type, have - given);
                setChanged();
                return given;
            }
            @Override public long getStored(ConfluxDataType type)   { return stored.getOrDefault(type, 0L); }
            @Override public long getCapacity(ConfluxDataType type) { return CAPACITY_PER_TYPE; }
        };
    }

    // ── Public accessors (used by GUI) ────────────────────────────────────────

    public long getStored(ConfluxDataType type)   { return stored.getOrDefault(type, 0L); }
    public long getCapacity(ConfluxDataType type) { return CAPACITY_PER_TYPE; }

    /**
     * Attempt to spend the given costs from stored data.
     * Either all costs are met or nothing is deducted — atomic check-then-spend.
     *
     * @param costs map of type → amount required
     * @return true if the spend succeeded
     */
    public boolean trySpend(Map<ConfluxDataType, Long> costs) {
        for (Map.Entry<ConfluxDataType, Long> e : costs.entrySet()) {
            if (stored.getOrDefault(e.getKey(), 0L) < e.getValue()) return false;
        }
        for (Map.Entry<ConfluxDataType, Long> e : costs.entrySet()) {
            stored.merge(e.getKey(), -e.getValue(), Long::sum);
        }
        setChanged();
        return true;
    }

    // ── Capability ────────────────────────────────────────────────────────────

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ConfluxMultiHandlerCapability.MULTI_DATA) return handlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerOpt.invalidate();
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag data = new CompoundTag();
        stored.forEach((type, amt) -> data.putLong(type.id(), amt));
        tag.put("data", data);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("data")) {
            CompoundTag data = tag.getCompound("data");
            for (ConfluxDataType type : ConfluxDataType.values()) {
                stored.put(type, data.getLong(type.id()));
            }
        }
    }
}
