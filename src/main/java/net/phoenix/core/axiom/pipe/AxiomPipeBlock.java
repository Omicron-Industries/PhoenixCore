package net.phoenix.core.axiom.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.phoenix.core.axiom.AxiomDataType;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A typed data-pipe block. One block class serves all five disciplines;
 * the {@link AxiomDataType} is baked in at registration time.
 *
 * Connection state is stored as six {@link BooleanProperty} flags (one per direction).
 * A side is "connected" when the neighbour exposes {@link IAxiomDataHandler} of the
 * same type, OR is another pipe of the same type.
 */
public class AxiomPipeBlock extends BaseEntityBlock {

    // ── Connection properties ─────────────────────────────────────────────────

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final Map<Direction, BooleanProperty> DIR_TO_PROP = new EnumMap<>(Map.of(
            Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH,
            Direction.EAST, EAST,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN));

    // ── Shapes ────────────────────────────────────────────────────────────────

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARM = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.EAST, Block.box(11, 5, 5, 16, 11, 11),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11),
            Direction.UP, Block.box(5, 11, 5, 11, 16, 11),
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11)));

    // ── Type ──────────────────────────────────────────────────────────────────

    private final AxiomDataType dataType;
    private final java.util.function.Supplier<BlockEntityType<AxiomPipeBlockEntity>> beTypeSupplier;

    public AxiomPipeBlock(Properties props, AxiomDataType dataType,
                          java.util.function.Supplier<BlockEntityType<AxiomPipeBlockEntity>> beTypeSupplier) {
        super(props);
        this.dataType = dataType;
        this.beTypeSupplier = beTypeSupplier;
        registerDefaultState(buildDefaultState());
    }

    private BlockState buildDefaultState() {
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : DIR_TO_PROP.values()) s = s.setValue(p, false);
        return s;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // ── Connection logic ──────────────────────────────────────────────────────

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbourState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        boolean connects = canConnect(level, neighbourPos, dir.getOpposite());
        return state.setValue(DIR_TO_PROP.get(dir), connects);
    }

    private BlockState computeConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(DIR_TO_PROP.get(dir), canConnect(level, pos.relative(dir), dir.getOpposite()));
        }
        return state;
    }

    private boolean canConnect(LevelAccessor level, BlockPos neighbourPos, Direction fromSide) {
        BlockState ns = level.getBlockState(neighbourPos);
        // Same pipe type — always connect
        if (ns.getBlock() instanceof AxiomPipeBlock other && other.dataType == dataType) return true;
        // Any block exposing our data handler capability on the face toward us
        if (level instanceof net.minecraft.world.level.Level lvl) {
            BlockEntity be = lvl.getBlockEntity(neighbourPos);
            if (be != null) {
                return be.getCapability(AxiomDataCapability.DATA, fromSide)
                        .map(h -> h.getDataType() == dataType)
                        .orElse(false);
            }
        }
        return false;
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(DIR_TO_PROP.get(dir))) {
                shape = Shapes.or(shape, ARM.get(dir));
            }
        }
        return shape;
    }

    // ── Block entity ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AxiomPipeBlockEntity(beTypeSupplier.get(), pos, state, dataType);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                                                                  net.minecraft.world.level.Level level,
                                                                  BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, beTypeSupplier.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public AxiomDataType getDataType() {
        return dataType;
    }
}
