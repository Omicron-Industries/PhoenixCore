package net.phoenix.core.integration.jade.provider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;

public class SourceHatchProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("source_hatch_info");

    private static final String KEY_STORED = "SourceStored";
    private static final String KEY_CAP = "SourceCapacity";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SourceHatchPartMachine hatch)) return;

        ISourceTile source = hatch.getSource();
        if (source == null) return;

        tag.putInt(KEY_STORED, source.getSource());
        tag.putInt(KEY_CAP, source.getMaxSource());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof SourceHatchPartMachine)) return;

        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(KEY_STORED) || !data.contains(KEY_CAP)) return;

        int stored = data.getInt(KEY_STORED);
        int cap = data.getInt(KEY_CAP);
        if (cap <= 0) return;

        float pct = Math.min(1f, stored / (float) cap);

        var helper = tooltip.getElementHelper();

        tooltip.add(
                helper.progress(
                        pct,
                        Component.literal(stored + " / " + cap),
                        helper.progressStyle()
                                .color(0x8F00FF, 0x8F00FF)
                                .textColor(0xFFFFFFFF),
                        BoxStyle.DEFAULT,
                        true));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
