package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchBackground;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchTracker;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import lombok.Getter;

@Getter
public class SourceHatchPartMachine extends TieredIOPartMachine implements ISourceProviderCapability, IMuiMachine {

    private final IO io;
    private final NotifiableSourceContainer sourceContainer;

    public SourceHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.io = io;
        this.sourceContainer = attachTrait(new NotifiableSourceContainer(io, getMaxCapacity(tier), getMaxConsumption(tier)));
    }

    @Override
    public ISourceTile getSource() {
        return sourceContainer;
    }

    /**
     * MUI2 buildMainUI — recreates the purple source-hatch HUD that was previously in
     * SourceHatchFancyUIWidget.  The visual background (gradient + grid + animated mist)
     * is handled by {@link SourceHatchBackground}; the text lines mirror what the old
     * drawOverlayHUD() method rendered.
     */
    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        // Only set the fancy background on the client side to avoid touching render state
        // from server logic.
        if (isRemote()) {
            mainWidget.background(new SourceHatchBackground(getBorderColor()));
        }

        mainWidget.child(Flow.col()
                .coverChildren()
                .margin(8, 6)
                .child(Text.dynamic(() -> Component.literal("SOURCE HATCH")
                        .withStyle(style -> style.withColor(getAccentColor())))
                        .asWidget()
                        .marginBottom(8))
                .child(Text.dynamic(() -> Component.literal("MODE: " + io.name())
                        .withStyle(ChatFormatting.WHITE))
                        .asWidget())
                .child(Text.dynamic(() -> {
                    int cur = sourceContainer.getSource();
                    int max = Math.max(1, sourceContainer.getMaxSource());
                    int pct = (int) ((cur * 100L) / max);
                    return Component.literal("SOURCE: " + cur + " / " + max + " (" + pct + "%)")
                            .withStyle(ChatFormatting.WHITE);
                }).asWidget())
                .child(Text.dynamic(() -> Component.literal("RATE: " + sourceContainer.getTransferRate() + "/s")
                        .withStyle(ChatFormatting.WHITE))
                        .asWidget()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            // GTM 8.0 update: getBlockPos() replaced with getBlockPos()
            SourceHatchTracker.add(serverLevel.dimension(), getBlockPos());
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() instanceof ServerLevel serverLevel) {
            // GTM 8.0 update: getBlockPos() replaced with getBlockPos()
            SourceHatchTracker.remove(serverLevel.dimension(), getBlockPos());
        }
    }

    public static int getMaxCapacity(int tier) {
        return 2000 * tier;
    }

    public static int getMaxConsumption(int tier) {
        return 500 * tier;
    }

    /** ARGB accent colour for the "SOURCE HATCH" title, keyed on IO direction. */
    private int getAccentColor() {
        return switch (io) {
            case IN   -> 0xFF8F00FF;
            case OUT  -> 0xFFD466FF;
            case BOTH -> 0xFFBC66FF;
            default   -> 0xFF8080A0;
        };
    }

    /** ARGB border colour passed to {@link SourceHatchBackground}. */
    private int getBorderColor() {
        int accent = getAccentColor();
        // 0xAA alpha, then the RGB from accentColor
        return (0xAA << 24) | (accent & 0x00FFFFFF);
    }
}
