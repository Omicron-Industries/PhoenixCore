package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchBackground;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.Rectangle;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.phoenix.core.utils.CompactCount.fmt;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SourceMultiblockTankMachine extends MultiblockControllerMachine implements IMuiMachine {

    @Getter
    @SaveField
    @SyncToClient
    protected final NotifiableSourceContainer sourceTank;

    private TickableSubscription transferSub;

    public SourceMultiblockTankMachine(BlockEntityCreationInfo info, int capacity, int maxConsumption) {
        super(info);
        this.sourceTank = attachTrait(new NotifiableSourceContainer(IO.BOTH, capacity, maxConsumption));
    }

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

        for (MultiblockPartMachine part : getParts()) {
            if (!(part instanceof SourceHatchPartMachine hatch)) continue;
            NotifiableSourceContainer hatchTank = hatch.getSourceContainer();

            if (hatch.getIo() == IO.IN) {
                int available = hatchTank.getSource();
                if (available > 0) {
                    int transfer = Math.min(available, (int) (sourceTank.getMaxSource() - sourceTank.getSource()));
                    if (transfer > 0) {
                        hatchTank.setSource(hatchTank.getSource() - transfer);
                        sourceTank.setSource(sourceTank.getSource() + transfer);
                    }
                }
            } else if (hatch.getIo() == IO.OUT) {
                int available = (int) sourceTank.getSource();
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
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData data, PanelSyncManager syncManager,
                            UISettings settings) {
        if (isRemote()) {
            mainWidget.background(new SourceHatchBackground(0xAA8F00FF));
        }

        mainWidget.child(Text.dynamic(() -> Component.literal("SOURCE TANK"))
                .asWidget()
                .pos(8, 6)
                .color(0xFF8F00FF));

        mainWidget.child(Text.dynamic(() -> {
            int cur = (int) sourceTank.getSource();
            int max = Math.max(1, sourceTank.getMaxSource());
            int pct = (int) ((cur * 100L) / max);
            return Component.literal("Capacity " + fmtSrc(cur) + "/" + fmtSrc(max) + " - " + pct + "%");
        }).asWidget()
                .pos(8, 24)
                .color(0xFFE6E6FF));

        mainWidget.child(new Rectangle()
                .color(0xFF120520)
                .asWidget()
                .pos(8, 38)
                .size(160, 5));

        mainWidget.child(Text.dynamic(() -> {
            int cur = (int) sourceTank.getSource();
            int max = Math.max(1, sourceTank.getMaxSource());
            int pct = (int) ((cur * 100L) / max);

            int barColor = pct < 30 ? 0xFF00E5FF : pct < 75 ? 0xFFD466FF : 0xFFBD00FF;

            int filled = (int) ((pct / 100.0) * 20);
            String bar = "|".repeat(filled);
            return Component.literal(bar).withStyle(style -> style.withColor(barColor));
        }).asWidget()
                .pos(8, 37)
                .height(10));

        mainWidget.child(new Rectangle()
                .color(0x448F00FF)
                .asWidget()
                .pos(8, 55)
                .size(160, 1));
    }

    private static String fmtSrc(long v) {
        if (v < 1_000) return String.valueOf(v);
        if (v < 10_000) return String.format("%.1fk", v / 1_000.0);
        if (v < 1_000_000) return (v / 1_000) + "k";
        if (v < 10_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v < 1_000_000_000) return (v / 1_000_000) + "M";
        return String.format("%.1fB", v / 1_000_000_000.0);
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
}
