package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part.TeslaEnergyHatchPartMachine;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.UUID;

public class TeslaNetworkProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("tesla_network_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        MetaMachine machine = MetaMachine.getMachine(accessor.getLevel(), accessor.getPosition());
        if (machine == null) return;

        BlockPos pos = accessor.getPosition();
        UUID team = null;
        long transferRate = 0;
        int mode = -1;

        if (!(accessor.getLevel() instanceof ServerLevel sl)) return;

        MinecraftServer server = sl.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        TeslaTeamEnergyData data = TeslaTeamEnergyData.get(overworld);

        if (machine instanceof TeslaEnergyHatchPartMachine hatch) {
            team = hatch.getOwnerTeamUUID();
            if (team != null) {
                mode = hatch.isUplink() ? 0 : 1;
                transferRate = data.getOrCreate(team).machineDisplayFlow.getOrDefault(pos, 0L);
            }
        } else if (machine instanceof TeslaTowerMachine tower) {
            team = tower.getOwnerUUID();
            mode = -1;
        } else {

            team = data.getOwnerTeam(pos);
            if (team != null) {
                TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(team);
                transferRate = teamData.machineDisplayFlow.getOrDefault(pos, 0L);

                if (teamData.soulLinkedMachines.contains(pos)) {
                    mode = transferRate < 0 ? 3 : 1;
                } else if (teamData.activeChargers.contains(pos)) {
                    mode = 2;
                } else {

                    mode = 1;
                }
            }
        }

        if (team != null) {
            TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(team);
            tag.putUUID("TeslaTeam", team);
            tag.putString("TeamName", TeamUtils.getTeamName(team));
            tag.putString("Stored", FormattingUtil.formatNumbers(teamData.stored));
            tag.putString("Capacity", FormattingUtil.formatNumbers(teamData.capacity));
            tag.putLong("LocalTransfer", transferRate);
            tag.putInt("TransferMode", mode);

            int physicalHatches = teamData.getLiveHatchCount(sl.getGameTime());
            int wiredMachines = teamData.soulLinkedMachines.size();
            int wirelessChargers = teamData.activeChargers.size();

            tag.putInt("TotalConnections", physicalHatches + wiredMachines + wirelessChargers);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(UID)) return;

        CompoundTag data = accessor.getServerData();
        if (!data.contains("TeslaTeam")) return;

        tooltip.add(Component.literal("Network: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.getString("TeamName")).withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.literal("Cloud: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.getString("Stored")).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" / " + data.getString("Capacity") + " EU")
                        .withStyle(ChatFormatting.YELLOW)));

        int connections = data.getInt("TotalConnections");
        tooltip.add(Component.literal("Connections: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(connections)).withStyle(ChatFormatting.WHITE)));

        if (data.contains("TransferMode") && data.getInt("TransferMode") != -1) {
            long rate = data.getLong("LocalTransfer");
            int mode = data.getInt("TransferMode");

            MutableComponent label;
            ChatFormatting color;
            String icon = "";

            switch (mode) {
                case 0 -> {
                    label = Component.literal("Providing: ");
                    color = ChatFormatting.GREEN;
                }
                case 2 -> {
                    label = Component.literal("Broadcasting: ");
                    color = ChatFormatting.AQUA;
                    icon = "§3波 ";
                }
                case 3 -> {
                    label = Component.literal("Generating: ");
                    color = ChatFormatting.GOLD;
                    icon = "§6⚡ ";
                }
                default -> {
                    label = Component.literal("Taking: ");
                    color = ChatFormatting.RED;
                }
            }

            long displayRate = Math.abs(rate);

            if (displayRate > 0) {
                tooltip.add(label.withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(icon + FormattingUtil.formatNumbers(displayRate) + " EU/t")
                                .withStyle(color)));
            } else {
                tooltip.add(label.withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("IDLE").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
