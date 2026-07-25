package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission;

import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class AdvancedFissionScramHatchPart extends TieredPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedFissionScramHatchPart.class, TieredPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private int signalThreshold = 8;

    @Persisted
    private int sustainTicks = 5;

    private int sustainCounter = 0;

    @Getter
    private boolean isScrammed = false;

    public AdvancedFissionScramHatchPart(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateScramStatus();
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        updateScramStatus();
    }

    @Override
    public boolean canConnectRedstone(@NotNull Direction side) {
        return true;
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateScramStatus();
    }

    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        int signal = level.getBestNeighborSignal(getPos());

        if (signal >= signalThreshold) {
            sustainCounter = Math.min(sustainCounter + 1, sustainTicks);
            if (sustainCounter >= sustainTicks && !isScrammed) {
                isScrammed = true;
                notifyController();
            }
        } else {
            
            if (sustainCounter > 0 || isScrammed) {
                sustainCounter = 0;
                isScrammed = false;
                notifyController();
            }
        }
    }

    private void updateScramStatus() {

        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (level.getBestNeighborSignal(getPos()) < signalThreshold) {
            sustainCounter = 0;
            if (isScrammed) {
                isScrammed = false;
                notifyController();
            }
        }

    }

    private void notifyController() {
        for (var controller : getControllers()) {
            if (controller instanceof FissionWorkableElectricMultiblockMachine fission) {
                fission.markDirty();
            }
        }
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 200, 175);

        group.addWidget(new LabelWidget(10, 8, "§l§6Advanced Fission SCRAM Hatch"));

        group.addWidget(new LabelWidget(10, 24,
                () -> isScrammed ? "§c● SCRAMMED — Reactor HALTED" : "§a● Standby — Reactor Permitted"));

        group.addWidget(new LabelWidget(10, 36, () -> {
            if (sustainCounter > 0 && !isScrammed) {
                return String.format("§eArming: %d / %d ticks", sustainCounter, sustainTicks);
            } else if (isScrammed) {
                return "§cArmed and triggered.";
            }
            return "§7Waiting for signal...";
        }));

        group.addWidget(new LabelWidget(10, 52, "§7─────────────────────────────"));

        group.addWidget(new LabelWidget(10, 64, "§fMin Signal Strength §7(1–15):"));
        group.addWidget(new IntInputWidget(10, 76, 80, 20,
                () -> signalThreshold,
                val -> signalThreshold = Mth.clamp(val, 1, 15)));

        group.addWidget(new LabelWidget(10, 104, "§fSustain Ticks §7(1–100):"));
        group.addWidget(new IntInputWidget(10, 116, 80, 20,
                () -> sustainTicks,
                val -> sustainTicks = Mth.clamp(val, 1, 100)));

        group.addWidget(new LabelWidget(10, 144, "§7─────────────────────────────"));
        group.addWidget(new LabelWidget(10, 156, "§8§oSignal must meet strength threshold"));
        group.addWidget(new LabelWidget(10, 166, "§8§ofor the full sustain duration to SCRAM."));

        return group;
    }
}
