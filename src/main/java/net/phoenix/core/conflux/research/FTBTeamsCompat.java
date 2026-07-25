package net.phoenix.core.conflux.research;

import net.minecraft.server.level.ServerPlayer;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

final class FTBTeamsCompat {

    static UUID getTeamId(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager()
                .getTeamForPlayer(player)
                .map(t -> t.getId())
                .orElse(player.getUUID());
    }

    private FTBTeamsCompat() {}
}
