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

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * Advanced Fission Stability Sensor.
 *
 * Like the basic sensor but the emitted signal strength is user-configured
 * rather than proportional to heat. When heat is in the configured range
 * this hatch always emits exactly the chosen strength (1–15).
 *
 * This lets the player precisely match the output to an Advanced SCRAM
 * Hatch's threshold setting, turning the redstone puzzle into a deliberate
 * tuning exercise rather than a variable-signal juggling act.
 */
public class AdvancedFissionStabilitySensorPart extends SensorHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedFissionStabilitySensorPart.class, SensorHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @Getter
    @Setter
    private int minPercent = 0;
    @Persisted
    @Getter
    @Setter
    private int maxPercent = 95;
    @Persisted
    @Getter
    @Setter
    private boolean inverted = false;

    /** Fixed signal strength emitted when heat is in range. */
    @Persisted
    @Getter
    @Setter
    private int emitStrength = 15;

    public AdvancedFissionStabilitySensorPart(IMachineBlockEntity holder, int tier) {
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
        WidgetGroup group = new WidgetGroup(0, 0, 200, 175);

        group.addWidget(new LabelWidget(10, 8, "§lAdvanced Fission Stability Sensor"));

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

        // Fixed emit strength
        group.addWidget(new LabelWidget(10, 100, "§fEmit Strength §7(1–15):"));
        group.addWidget(new IntInputWidget(90, 95, 90, 20,
                () -> emitStrength,
                val -> emitStrength = Mth.clamp(val, 1, 15)));

        // Invert toggle
        group.addWidget(new LabelWidget(10, 124, "§fInvert output:"));
        group.addWidget(new ToggleButtonWidget(
                100, 119, 18, 18,
                GuiTextures.INVERT_REDSTONE_BUTTON,
                this::isInverted,
                this::setInverted)
                .setTooltipText("Invert Signal Output"));

        group.addWidget(new LabelWidget(10, 147, "§7─────────────────────────────"));
        group.addWidget(new LabelWidget(10, 158, "§8Emits fixed strength on back face only."));
        group.addWidget(new LabelWidget(10, 168, "§8Pair with an Advanced SCRAM Hatch."));

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
        boolean inRange = heatPct >= minPercent && heatPct <= maxPercent;
        boolean emit = inverted != inRange; // clean XOR

        // Always emit the user-chosen fixed strength — never a variable value.
        return emit ? Mth.clamp(emitStrength, 1, 15) : 0;
    }
}
