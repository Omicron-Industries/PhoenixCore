package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.client.renderer.gui.SourceTankFancyUIWidget;
import net.phoenix.core.common.machine.multiblock.part.special.SourceHatchPartMachine;

import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.phoenix.core.utils.CompactCount.fmt;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class SourceMultiblockTankMachine extends MultiblockControllerMachine implements IFancyUIMachine {

    // Use your custom trait
    @Getter
    @DescSynced
    @Persisted
    protected final NotifiableSourceContainer sourceTank;

    public SourceMultiblockTankMachine(IMachineBlockEntity holder, int capacity, int maxConsumption) {
        super(holder);
        this.sourceTank = new NotifiableSourceContainer(this, IO.BOTH, capacity, maxConsumption);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        if (!isFormed()) return InteractionResult.FAIL;
        return super.onUse(state, world, pos, player, hand, hit);
    }

    private TickableSubscription transferSub;

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            transferSub = subscribeServerTick(transferSub, this::transferSource);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (transferSub != null) {
            transferSub.unsubscribe();
            transferSub = null;
        }
    }

    private void transferSource() {
        if (!isFormed()) return;

        for (IMultiPart part : getParts()) {
            if (!(part instanceof SourceHatchPartMachine hatch)) continue;
            NotifiableSourceContainer hatchTank = hatch.getSourceContainer();

            if (hatch.getIo() == IO.IN) {
                int available = hatchTank.getSource();
                if (available > 0) {
                    int space = sourceTank.getMaxSource() - sourceTank.getSource();
                    int transfer = Math.min(available, space);
                    if (transfer > 0) {
                        hatchTank.setSource(hatchTank.getSource() - transfer);
                        sourceTank.setSource(sourceTank.getSource() + transfer);
                    }
                }
            } else if (hatch.getIo() == IO.OUT) {
                int available = sourceTank.getSource();
                if (available > 0) {
                    int space = hatchTank.getMaxSource() - hatchTank.getSource();
                    int transfer = Math.min(available, space);
                    if (transfer > 0) {
                        sourceTank.setSource(sourceTank.getSource() - transfer);
                        hatchTank.setSource(hatchTank.getSource() + transfer);
                    }
                }
            }
        }
    }

    @Override
    public ModularUI createUI(Player player) {
        final int w = 176;
        final int h = 166;
        return new ModularUI(w, h, this, player)
                .widget(new SourceTankFancyUIWidget(this, w, h));
    }

    @Override
    public Widget createUIWidget() {
        return new WidgetGroup(0, 0, 176, 74);
    }

    public static String compactIfNumeric(String s) {
        if (s == null || s.isEmpty()) return s;

        String cleaned = s.replace(",", "").replace("_", "").trim();
        if (cleaned.isEmpty()) return s;

        for (int i = 0; i < cleaned.length(); i++) {
            if (!Character.isDigit(cleaned.charAt(i))) return s;
        }

        long v;
        try {
            v = Long.parseLong(cleaned);
        } catch (Throwable t) {
            return s;
        }

        if (v < 10_000) return s;

        if (v >= 1_000_000_000L) return fmt(v, 1_000_000_000L, "B");
        if (v >= 1_000_000L) return fmt(v, 1_000_000L, "M");
        return fmt(v, 1_000L, "k");
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SourceMultiblockTankMachine.class, MultiblockControllerMachine.MANAGED_FIELD_HOLDER);
}
