package net.phoenix.core.axiom.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.axiom.AxiomDataType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tile entity for all Axiom data pipes.
 *
 * Each pipe holds a small buffer and pushes its contents downstream each tick.
 * "Downstream" means any adjacent block exposing {@link IAxiomDataHandler} of the
 * same {@link AxiomDataType}, prioritising handlers that still have capacity.
 * Pipes do NOT form an explicit network graph — they rely on per-tick propagation,
 * which is simpler and avoids CWU-style network rebuild bugs.
 */
public class AxiomPipeBlockEntity extends BlockEntity {

    /** Throughput per tick, per pipe. Tune this to control network speed. */
    public static final long THROUGHPUT = 64L;
    /** How much data a single pipe segment can buffer. */
    public static final long BUFFER = 256L;

    private long stored = 0L;

    private final AxiomDataType dataType;
    private final LazyOptional<IAxiomDataHandler> handlerOpt;

    public AxiomPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, AxiomDataType dataType) {
        super(type, pos, state);
        this.dataType = dataType;
        this.handlerOpt = LazyOptional.of(this::buildHandler);
    }

    private IAxiomDataHandler buildHandler() {
        return new IAxiomDataHandler() {

            @Override
            public AxiomDataType getDataType() {
                return dataType;
            }

            @Override
            public long insert(long amount) {
                long accepted = Math.min(amount, BUFFER - stored);
                stored += accepted;
                setChanged();
                return accepted;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, stored);
                stored -= given;
                setChanged();
                return given;
            }

            @Override
            public long getStored() {
                return stored;
            }

            @Override
            public long getCapacity() {
                return BUFFER;
            }
        };
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void serverTick() {
        if (stored == 0 || level == null) return;

        long budget = Math.min(stored, THROUGHPUT);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighbourPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighbourPos);
            if (be == null) continue;

            // Don't push back toward a pipe that has equal or more data (prevents loops)
            if (be instanceof AxiomPipeBlockEntity peer && peer.stored >= stored) continue;

            // Single-type neighbour (another pipe)
            LazyOptional<IAxiomDataHandler> single = be.getCapability(AxiomDataCapability.DATA, dir.getOpposite());
            if (single.isPresent()) {
                IAxiomDataHandler handler = single.orElseThrow(IllegalStateException::new);
                if (handler.getDataType() != dataType) continue;
                long accepted = handler.insert(budget);
                if (accepted > 0) {
                    stored -= accepted;
                    budget -= accepted;
                    setChanged();
                }
                continue;
            }

            // Multi-type neighbour (terminal, machine)
            LazyOptional<IAxiomMultiHandler> multi = be.getCapability(AxiomMultiHandlerCapability.MULTI_DATA,
                    dir.getOpposite());
            if (multi.isPresent()) {
                long accepted = multi.orElseThrow(IllegalStateException::new).insert(dataType, budget);
                if (accepted > 0) {
                    stored -= accepted;
                    budget -= accepted;
                    setChanged();
                }
            }
        }
    }

    // ── Capability ────────────────────────────────────────────────────────────

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == AxiomDataCapability.DATA) return handlerOpt.cast();
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
        tag.putLong("stored", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.getLong("stored");
    }

    public AxiomDataType getDataType() {
        return dataType;
    }

    public long getStored() {
        return stored;
    }
}
