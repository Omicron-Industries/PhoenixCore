package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class FissionScramHatchPart extends TieredPartMachine {

    @Getter
    private boolean isScrammed = false;

    public FissionScramHatchPart(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
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

    private void updateScramStatus() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        boolean newScrammed = level.getBestNeighborSignal(getPos()) > 0;
        if (newScrammed != isScrammed) {
            isScrammed = newScrammed;
            notifyController();
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
        WidgetGroup group = new WidgetGroup(0, 0, 200, 145);

        group.addWidget(new LabelWidget(10, 8, "§l§cFission SCRAM Hatch"));

        group.addWidget(new LabelWidget(10, 24,
                () -> isScrammed ? "§c● SCRAMMED — Reactor HALTED" : "§a● Standby — Reactor Permitted"));

        group.addWidget(new LabelWidget(10, 40, "§7─────────────────────────────"));
        group.addWidget(new LabelWidget(10, 52, "§eHow it works:"));
        group.addWidget(new LabelWidget(10, 63, "§7Any redstone signal on any face"));
        group.addWidget(new LabelWidget(10, 73, "§7halts the reactor immediately."));
        group.addWidget(new LabelWidget(10, 83, "§7Removing the signal resumes it."));
        group.addWidget(new LabelWidget(10, 99, "§7─────────────────────────────"));
        group.addWidget(new LabelWidget(10, 111, "§8§oHint: not every redstone source"));
        group.addWidget(new LabelWidget(10, 121, "§8§obehaves the way you expect."));
        group.addWidget(new LabelWidget(10, 131, "§8§oThink carefully about signal timing."));

        return group;
    }
}
