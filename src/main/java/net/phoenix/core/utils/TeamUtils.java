package net.phoenix.core.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

public final class TeamUtils {

    private TeamUtils() {}

    public static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;

        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerUUID)
                .map(team -> {
                    if (team.isPartyTeam() || team.isServerTeam()) {
                        return team.getTeamId();
                    }
                    return playerUUID;
                })
                .orElse(playerUUID);
    }

    public static String getTeamName(UUID teamId) {
        if (teamId == null) return "Unknown";

        return FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                .map(team -> team.getShortName())
                .orElse("Player: " + teamId.toString().substring(0, 8));
    }

    public static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        if (player instanceof ServerPlayer) {
            return FTBTeamsAPI.api().getManager().getTeamByID(teamUUID)
                    .map(team -> team.getMembers().contains(player.getUUID()))
                    .orElse(player.getUUID().equals(teamUUID));
        }
        return false;
    }
}
