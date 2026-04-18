package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchFancyUIWidget;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchTracker;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SourceHatchPartMachine extends TieredIOPartMachine implements ISourceProviderCapability, IFancyUIMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SourceHatchPartMachine.class,
            TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    private final IO io;

    @Persisted
    @DescSynced
    private final NotifiableSourceContainer sourceContainer;

    public SourceHatchPartMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier, io);
        this.io = io;
        this.sourceContainer = new NotifiableSourceContainer(
                this, io, getMaxCapacity(tier), getMaxConsumption(tier));
    }

    @Override
    public ISourceTile getSource() {
        return sourceContainer;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /**
     * IMPORTANT FIX:
     * Returning a simple page widget prevents GTCEu from generating its default fancy "scene/preview" page.
     * Do NOT put inventory widgets here; FancyMachineUIWidget handles the player inventory panel.
     *
     * Also: keep this reasonably sized. FancyMachineUIWidget will center it inside its pageContainer.
     */
    @Override
    public Widget createUIWidget() {
        return new WidgetGroup(0, 0, 176, 74);
    }

    /**
     * IMPORTANT:
     * The height here is just a starting container size; FancyMachineUIWidget will recompute its final size
     * from the page widget + border + optional inventory. Keep it close to your page widget height.
     */
    @Override
    public ModularUI createUI(Player player) {
        final int w = 176;
        final int h = 74;
        return new ModularUI(w, h, this, player)
                .widget(new SourceHatchFancyUIWidget(this, w, h));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            SourceHatchTracker.add(serverLevel.dimension(), getPos());
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() instanceof ServerLevel serverLevel) {
            SourceHatchTracker.remove(serverLevel.dimension(), getPos());
        }
    }

    public static int getMaxCapacity(int tier) {
        return 2000 * tier;
    }

    public static int getMaxConsumption(int tier) {
        return 500 * tier;
    }
}
