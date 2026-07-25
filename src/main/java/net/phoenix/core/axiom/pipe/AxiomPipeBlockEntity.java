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

public class AxiomPipeBlockEntity extends BlockEntity {

    public static final long THROUGHPUT = 64L;
    
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

    public void serverTick() {
        if (stored == 0 || level == null) return;

        long budget = Math.min(stored, THROUGHPUT);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighbourPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighbourPos);
            if (be == null) continue;

            if (be instanceof AxiomPipeBlockEntity peer && peer.stored >= stored) continue;

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
