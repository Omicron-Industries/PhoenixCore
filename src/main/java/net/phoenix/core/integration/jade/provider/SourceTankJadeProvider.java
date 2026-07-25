package net.phoenix.core.integration.jade.provider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.SourceMultiblockTankMachine;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;

import javax.annotation.Nonnull;

public class SourceTankJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("source_tank_info");

    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_PURPLE = 0xFFD466FF;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof SourceMultiblockTankMachine tank) {
            tag.putInt("TankStored", tank.getSourceTank().getSource());
            tag.putInt("TankCap", tank.getSourceTank().getMaxSource());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(UID)) return;

        if (!(accessor.getBlockEntity() instanceof SourceMultiblockTankMachine)) return;

        CompoundTag data = accessor.getServerData();
        if (data == null || data.isEmpty()) return;

        if (data.contains("TankStored") && data.contains("TankCap")) {
            int stored = data.getInt("TankStored");
            int cap = data.getInt("TankCap");
            float pct = cap > 0 ? (float) stored / cap : 0;

            tooltip.add(Component.translatable("jade.phoenixcore.source_tank_header")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            int barColor = pct < 0.3f ? COLOR_CYAN : COLOR_PURPLE;

            Component barText = Component.translatable(
                    "jade.phoenixcore.source_tank_format",
                    fmt(stored),
                    fmt(cap),
                    (int) (pct * 100));

            tooltip.add(tooltip.getElementHelper().progress(
                    pct,
                    barText,
                    tooltip.getElementHelper().progressStyle()
                            .color(barColor, barColor)
                            .textColor(0xFFFFFFFF),
                    BoxStyle.DEFAULT,
                    true));
        }
    }

    private static String fmt(long v) {
        if (v < 1_000) return String.valueOf(v);
        if (v < 10_000) return String.format("%.1fk", v / 1_000.0);
        if (v < 1_000_000) return (v / 1_000) + "k";
        if (v < 10_000_000) return String.format("%.1fM", v / 1_000_000.0);
        return (v / 1_000_000) + "M";
    }

    @Override
    @Nonnull
    public ResourceLocation getUid() {
        return UID;
    }
}
