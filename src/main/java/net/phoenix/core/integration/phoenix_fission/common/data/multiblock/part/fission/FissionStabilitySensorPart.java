package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import org.jetbrains.annotations.Nullable;

public class FissionStabilitySensorPart extends SensorHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FissionStabilitySensorPart.class, SensorHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted @Getter @Setter private int minPercent = 0;
    @Persisted @Getter @Setter private int maxPercent = 95;
    @Persisted @Getter @Setter private boolean inverted = false;

    public FissionStabilitySensorPart(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 200, 145);

        group.addWidget(new LabelWidget(10, 8, "§lFission Stability Sensor"));

        // Live readout
        group.addWidget(new LabelWidget(10, 24, () -> {
            var controller = getController();
            if (controller instanceof FissionWorkableElectricMultiblockMachine fission) {
                double pct = (fission.getHeat() / FissionWorkableElectricMultiblockMachine.cfg().maxSafeHeat) * 100.0;
                int sig = getOutputSignal(getFrontFacing().getOpposite());
                String sigColor = sig > 0 ? "§a" : "§c";
                return String.format("§7Heat: §f%.1f%%  §7Signal: %s%d", pct, sigColor, sig);
            }
            return "§7Heat: §8N/A  §7Signal: §80";
        }));

        group.addWidget(new LabelWidget(10, 40, "§7─────────────────────────────"));

        // Min %
        group.addWidget(new LabelWidget(10, 52, "§fMin Heat %:"));
        group.addWidget(new IntInputWidget(90, 47, 90, 20,
                () -> minPercent,
                val -> minPercent = Mth.clamp(val, 0, 200)));

        // Max %
        group.addWidget(new LabelWidget(10, 76, "§fMax Heat %:"));
        group.addWidget(new IntInputWidget(90, 71, 90, 20,
                () -> maxPercent,
                val -> maxPercent = Mth.clamp(val, 0, 200)));

        // Invert toggle
        group.addWidget(new LabelWidget(10, 100, "§fInvert output:"));
        group.addWidget(new ToggleButtonWidget(
                100, 95, 18, 18,
                GuiTextures.INVERT_REDSTONE_BUTTON,
                this::isInverted,
                this::setInverted)
                .setTooltipText("Invert Signal Output"));

        group.addWidget(new LabelWidget(10, 122, "§7─────────────────────────────"));
        group.addWidget(new LabelWidget(10, 133, "§8Emits signal on back face only."));

        return group;
    }

    // ── Signal logic ──────────────────────────────────────────────────────────

    @Override
    public int getOutputSignal(@Nullable Direction direction) {
        if (direction != null && direction != getFrontFacing().getOpposite()) return 0;

        var controller = getController();
        if (!(controller instanceof FissionWorkableElectricMultiblockMachine fission)) return 0;

        double maxSafe = FissionWorkableElectricMultiblockMachine.cfg().maxSafeHeat;
        if (maxSafe <= 0) return 0;

        double heatPct = (fission.getHeat() / maxSafe) * 100.0;

        // Proportional signal (0-15) scaled to heat % out of 200 max input range.
        // This is what makes the advanced hatch's threshold setting meaningful —
        // a sensor hatch paired with an advanced scram hatch creates a real
        // comparator-strength puzzle rather than a simple on/off.
        int strength = (int) Math.round((heatPct / 200.0) * 15.0);
        strength = Mth.clamp(strength, 0, 15);

        boolean inRange = heatPct >= minPercent && heatPct <= maxPercent;
        boolean emit = inverted != inRange; // clean XOR

        return emit ? strength : 0;
    }
}