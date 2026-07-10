package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.conflux.ConfluxDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for all Conflux data pipes.
 *
 * Extends GTCEu's {@link PipeBlockEntity} to gain cover support, wrench-tool
 * interaction, and GTM pipe-network membership.
 *
 * Transport behaviour is kept as per-tick push propagation: when the buffer
 * has data the entity subscribes to {@code serverTick}, pushes to every
 * connected neighbour up to the per-tick throughput limit, and unsubscribes
 * when the buffer drains to zero.
 */
public class ConfluxPipeBlockEntity extends PipeBlockEntity<ConfluxPipeType, ConfluxPipeData> {

    public static final long THROUGHPUT = 64L;
    public static final long BUFFER     = 256L;

    @SaveField(nbtKey = "stored")
    private long stored = 0L;
    private final LazyOptional<IConfluxDataHandler> handlerOpt;
    @Nullable
    private TickableSubscription tickSub;

    public ConfluxPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.handlerOpt = LazyOptional.of(this::buildHandler);
    }

    // ── Capability ────────────────────────────────────────────────────────────

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ConfluxDataCapability.DATA) {
            if (side != null && !isConnected(side)) return LazyOptional.empty();
            return handlerOpt.cast();
        }
        if (cap == GTCapability.CAPABILITY_COVERABLE) {
            return GTCapability.CAPABILITY_COVERABLE.orEmpty(cap, LazyOptional.of(this::getCoverContainer));
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerOpt.invalidate();
    }

    private IConfluxDataHandler buildHandler() {
        return new IConfluxDataHandler() {
            @Override public ConfluxDataType getDataType() { return getPipeType().dataType(); }

            @Override public long insert(long amount) {
                long accepted = Math.min(amount, BUFFER - stored);
                if (accepted <= 0) return 0;
                stored += accepted;
                setChanged();
                scheduleTickIfNeeded();
                return accepted;
            }

            @Override public long extract(long amount) {
                long given = Math.min(amount, stored);
                stored -= given;
                if (given > 0) setChanged();
                return given;
            }

            @Override public long getStored()   { return stored; }
            @Override public long getCapacity() { return BUFFER; }
        };
    }

    // ── canAttachTo ───────────────────────────────────────────────────────────

    @Override
    public boolean canAttachTo(Direction side) {
        if (level == null) return false;
        BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(side));
        if (neighbor == null || neighbor instanceof ConfluxPipeBlockEntity) return false;

        ConfluxDataType dt = getPipeType().dataType();
        if (neighbor.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, side.getOpposite()).isPresent()) {
            return true;
        }
        return neighbor.getCapability(ConfluxDataCapability.DATA, side.getOpposite())
                .map(h -> h.getDataType() == dt)
                .orElse(false);
    }

    // ── Per-tick push ─────────────────────────────────────────────────────────

    private void scheduleTickIfNeeded() {
        if (tickSub == null || !tickSub.isStillSubscribed()) {
            tickSub = subscribeServerTick(this::doServerTick);
        }
    }

    private void doServerTick() {
        if (stored == 0 || level == null) {
            unsubscribeTick();
            return;
        }

        long budget = Math.min(stored, THROUGHPUT);
        ConfluxDataType dt = getPipeType().dataType();

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            if (!isConnected(dir)) continue;

            BlockPos npos = getBlockPos().relative(dir);
            BlockEntity be = level.getBlockEntity(npos);
            if (be == null) continue;

            // skip back-push into equally-full pipes to prevent oscillation
            if (be instanceof ConfluxPipeBlockEntity peer && peer.stored >= stored) continue;

            // single-type handler (another pipe or typed machine output)
            LazyOptional<IConfluxDataHandler> single =
                    be.getCapability(ConfluxDataCapability.DATA, dir.getOpposite());
            if (single.isPresent()) {
                IConfluxDataHandler h = single.orElseThrow(IllegalStateException::new);
                if (h.getDataType() != dt) continue;
                long accepted = h.insert(budget);
                if (accepted > 0) { stored -= accepted; budget -= accepted; setChanged(); }
                continue;
            }

            // multi-type handler (research terminal)
            LazyOptional<IConfluxMultiHandler> multi =
                    be.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, dir.getOpposite());
            if (multi.isPresent()) {
                long accepted = multi.orElseThrow(IllegalStateException::new).insert(dt, budget);
                if (accepted > 0) { stored -= accepted; budget -= accepted; setChanged(); }
            }
        }

        if (stored == 0) unsubscribeTick();
    }

    private void unsubscribeTick() {
        unsubscribe(tickSub);
        tickSub = null;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    // stored is persisted automatically via @SaveField.
    // We override load only to resume ticking after the field is restored.

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (stored > 0) scheduleTickIfNeeded();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public ConfluxDataType getDataType() { return getPipeType().dataType(); }
    public long getStored()             { return stored; }
}
