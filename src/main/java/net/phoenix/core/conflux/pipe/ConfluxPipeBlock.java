package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.conflux.ConfluxDataType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfluxPipeBlock extends PipeBlock<ConfluxPipeType, ConfluxPipeData, LevelConfluxPipeNet> {

    private final Supplier<BlockEntityType<ConfluxPipeBlockEntity>> beTypeSupplier;

    public ConfluxPipeBlock(Properties props, ConfluxPipeType pipeType,
                            Supplier<BlockEntityType<ConfluxPipeBlockEntity>> beTypeSupplier) {
        super(props, pipeType);
        this.beTypeSupplier = beTypeSupplier;
    }

    @Override
    public LevelConfluxPipeNet getWorldPipeNet(ServerLevel level) {
        return LevelConfluxPipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<ConfluxPipeType, ConfluxPipeData>> getBlockEntityType() {
        return beTypeSupplier.get();
    }

    @Override
    public ConfluxPipeData createRawData(BlockState state, @Nullable ItemStack stack) {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public ConfluxPipeData createProperties(IPipeNode<ConfluxPipeType, ConfluxPipeData> pipeTile) {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public ConfluxPipeData getFallbackType() {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public boolean canPipesConnect(IPipeNode<ConfluxPipeType, ConfluxPipeData> selfTile, Direction side,
                                   IPipeNode<ConfluxPipeType, ConfluxPipeData> sideTile) {
        
        return selfTile.getPipeType() == sideTile.getPipeType();
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<ConfluxPipeType, ConfluxPipeData> selfTile, Direction side,
                                         @Nullable BlockEntity tile) {
        if (tile == null) return false;
        ConfluxDataType dt = pipeType.dataType();

        if (tile.getCapability(ConfluxMultiHandlerCapability.MULTI_DATA, side.getOpposite()).isPresent()) {
            return true;
        }
        
        return tile.getCapability(ConfluxDataCapability.DATA, side.getOpposite())
                .map(h -> h.getDataType() == dt)
                .orElse(false);
    }

    @Override
    public PipeModel createPipeModel(GTBlockstateProvider provider) {

        return new PipeModel(this, provider, pipeType.getThickness(),
                GTCEu.id("block/pipe/pipe_side"),
                GTCEu.id("block/pipe/pipe_normal_in"));
    }

    public ConfluxDataType getDataType() {
        return pipeType.dataType();
    }
}
