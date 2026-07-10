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

/**
 * GTM 8.0 port of TeslaNetworkProvider.
 *
 * Two real bugs fixed here, neither of which was a MUI/8.0-API issue:
 *
 * 1. COMPILE ERROR: the original imported two different classes both named MetaMachine under
 *    the same simple name (com.gregtechceu.gtceu.api.blockentity.MetaMachine AND
 *    com.gregtechceu.gtceu.api.machine.MetaMachine) -- a duplicate single-type-import, which
 *    doesn't compile. Fixed by dropping the block-entity-side check entirely and resolving the
 *    machine directly via MetaMachine.getMachine(level, pos) -- the same confirmed pattern
 *    already used throughout TeslaBinderItem -- instead of guessing at the real block-entity
 *    class name.
 *
 * 2. THE "DOUBLE TRACKING / UNRELIABLE" BUG: the original's else-branch (for blocks that are
 *    neither a TeslaEnergyHatchPartMachine nor a TeslaTowerMachine) found the owning team by
 *    linearly scanning EVERY team's network and checking soulLinkedMachines.contains(pos) /
 *    activeChargers.contains(pos). This is exactly the kind of scan that produces unreliable
 *    results: if a position is ever stale in more than one team's tracking set (e.g. after an
 *    incomplete rebind/cleanup), the result silently depends on HashMap iteration order, picking
 *    whichever team happens to be visited first rather than the actually-correct owner. It also
 *    only checked 2 of the real tracking paths (soulLinkedMachines, activeChargers), silently
 *    missing energyBuffered-tracked positions in this branch.
 *
 *    TeslaTeamEnergyData already has the correct, authoritative, O(1) lookup for exactly this:
 *    machineToTeam (a single global BlockPos -> UUID map, kept up to date by toggleSoulLink,
 *    setEnergyBuffered, and removeEndpoint) via getOwnerTeam(pos). This rewrite resolves the
 *    team with getOwnerTeam(pos) directly -- no scanning, no ambiguity -- then determines the
 *    connection-type label (mode) by checking the membership sets ONLY on that single resolved
 *    team's TeamEnergy, not across all teams.
 */
public class TeslaNetworkProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("tesla_network_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        // The original checked `accessor.getBlockEntity() instanceof MetaMachine`, but that
        // can't have compiled -- MetaMachine here is the machine-logic class (returned by
        // .getMetaMachine() in the original), not a block-entity type, and the import collision
        // means the actual intended block-entity type is unknown. Sidestepped entirely by using
        // MetaMachine.getMachine(level, pos) directly -- the same confirmed pattern already used
        // throughout TeslaBinderItem -- instead of guessing at a block-entity class name.
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
            // Single authoritative O(1) lookup instead of scanning every team's network.
            team = data.getOwnerTeam(pos);
            if (team != null) {
                TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(team);
                transferRate = teamData.machineDisplayFlow.getOrDefault(pos, 0L);

                if (teamData.soulLinkedMachines.contains(pos)) {
                    mode = transferRate < 0 ? 3 : 1;
                } else if (teamData.activeChargers.contains(pos)) {
                    mode = 2;
                } else {
                    // Registered via machineToTeam (e.g. through energyBuffered) but not in
                    // either membership set on this team -- treat as a generic consumer/taker.
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
                case 0 -> { // Uplink Hatch
                    label = Component.literal("Providing: ");
                    color = ChatFormatting.GREEN;
                }
                case 2 -> { // Wireless Charger
                    label = Component.literal("Broadcasting: ");
                    color = ChatFormatting.AQUA;
                    icon = "§3波 ";
                }
                case 3 -> { // Soul-Linked Generator
                    label = Component.literal("Generating: ");
                    color = ChatFormatting.GOLD;
                    icon = "§6⚡ ";
                }
                default -> { // Downlink / Consumer Machine
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
