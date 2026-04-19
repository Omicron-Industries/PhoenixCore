package net.phoenix.core.integration.phoenix_tesla_network.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

import com.mojang.brigadier.CommandDispatcher;

import java.util.UUID;

@Mod.EventBusSubscriber
public class TeslaUICommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("tesla_energy_debug")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> run(ctx.getSource())));
    }

    private static int run(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);

        source.sendSuccess(() -> Component.literal("§6=== Tesla Team Energy Data ==="), false);

        var networks = data.getNetworksView();

        if (networks.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cNo networks found."), false);
            return 1;
        }

        for (var entry : networks.entrySet()) {
            UUID team = entry.getKey();
            TeslaTeamEnergyData.TeamEnergy e = entry.getValue();

            boolean online = data.isOnline(team);
            String status = online ? "§aONLINE" : "§7OFFLINE";

            source.sendSuccess(() -> Component.literal("§eTeam: §f" + team + " (" + status + "§f)"), false);
            source.sendSuccess(() -> Component.literal("  §7Stored: §b" + e.stored), false);
            source.sendSuccess(() -> Component.literal("  §7Capacity: §b" + e.capacity), false);

            int hatchCount = e.energyBuffered.size();
            int soulLinkCount = e.soulLinkedMachines.size();

            source.sendSuccess(() -> Component.literal("  §7Physical Hatches: §f" + hatchCount), false);
            source.sendSuccess(() -> Component.literal("  §dSoul-Linked Machines: §f" + soulLinkCount), false);
        }

        return 1;
    }
}
