package net.phoenix.core.integration.phoenix_tesla_network.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

import com.mojang.brigadier.CommandDispatcher;

import java.util.UUID;

@Mod.EventBusSubscriber
public class TeslaDebugCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("tesla_debug")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> run(ctx.getSource())));
    }

    private static int run(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);

        source.sendSuccess(() -> Component.literal("=== TESLA NETWORK SYSTEM DEBUG ===").withStyle(ChatFormatting.GOLD,
                ChatFormatting.BOLD), false);

        var networks = data.getNetworksView();

        source.sendSuccess(() -> Component.literal("Global Machine Registry:").withStyle(ChatFormatting.YELLOW,
                ChatFormatting.UNDERLINE), false);

        int globalCount = 0;
        for (var entry : networks.entrySet()) {
            UUID teamId = entry.getKey();
            TeslaTeamEnergyData.TeamEnergy teamData = entry.getValue();

            for (BlockPos pos : teamData.soulLinkedMachines) {
                UUID lookupTeam = data.getOwnerTeam(pos);
                boolean verified = teamId.equals(lookupTeam);

                source.sendSuccess(() -> Component.literal("  [LINKED] ")
                        .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.WHITE))
                        .append(" -> Team: ")
                        .append(Component.literal(teamId.toString().substring(0, 8)).withStyle(ChatFormatting.AQUA))
                        .append(verified ? Component.literal(" [VERIFIED]").withStyle(ChatFormatting.GREEN) :
                                Component.literal(" [MAP MISSING]").withStyle(ChatFormatting.RED)),
                        false);
                globalCount++;
            }
        }

        if (globalCount == 0) {
            source.sendSuccess(() -> Component.literal("  No machines currently registered in Global Map.")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }

        source.sendSuccess(() -> Component.literal("--------------------------------"), false);

        if (networks.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active team networks found.").withStyle(ChatFormatting.RED),
                    false);
            return 1;
        }

        for (var entry : networks.entrySet()) {
            UUID team = entry.getKey();
            TeslaTeamEnergyData.TeamEnergy teamData = entry.getValue();
            boolean online = data.isOnline(team);

            MutableComponent teamHeader = Component.literal("Network: ")
                    .append(Component.literal(team.toString().substring(0, 8)).withStyle(ChatFormatting.AQUA))
                    .append(online ? Component.literal(" [ONLINE]").withStyle(ChatFormatting.GREEN) :
                            Component.literal(" [OFFLINE]").withStyle(ChatFormatting.RED));
            source.sendSuccess(() -> teamHeader, false);

            source.sendSuccess(() -> Component.literal("  EU: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(teamData.stored.toString()).withStyle(ChatFormatting.YELLOW))
                    .append(" / ")
                    .append(Component.literal(teamData.capacity.toString()).withStyle(ChatFormatting.GOLD)), false);

            long now = level.getGameTime();
            int liveCount = 0;
            for (long lastSeen : teamData.lastSeen.values()) {
                if (now - lastSeen < 40) liveCount++;
            }
            final int finalLiveCount = liveCount;
            source.sendSuccess(() -> Component.literal("  Live Heartbeats: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(finalLiveCount)).withStyle(ChatFormatting.LIGHT_PURPLE)),
                    false);

            var hatches = data.getHatches(team);
            if (!hatches.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  Connected Hardware:").withStyle(ChatFormatting.GRAY,
                        ChatFormatting.ITALIC), false);
                for (var info : hatches) {
                    String typePrefix = info.isSoulLinked ? "[S]" : (info.isPhysicalOutput ? "[I]" : "[O]");
                    ChatFormatting typeColor = info.isSoulLinked ? ChatFormatting.LIGHT_PURPLE :
                            (info.isPhysicalOutput ? ChatFormatting.GREEN : ChatFormatting.RED);

                    source.sendSuccess(() -> Component.literal("    - ")
                            .append(Component.literal(typePrefix).withStyle(typeColor))
                            .append(" " + info.pos.toShortString())
                            .append(Component.literal(" (" + info.dimension.location().getPath() + ")")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                            .append(" Flow: " + info.displayFlow + " EU/t"), false);
                }
            }

            if (!teamData.activeChargers.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  Active Chargers:").withStyle(ChatFormatting.BLUE), false);
                for (BlockPos cPos : teamData.activeChargers) {
                    source.sendSuccess(() -> Component.literal("    - " + cPos.toShortString())
                            .append(" Flow: " + teamData.machineDisplayFlow.getOrDefault(cPos, 0L) + " EU/t"), false);
                }
            }

            source.sendSuccess(() -> Component.literal(" "), false);
        }

        return 1;
    }
}
