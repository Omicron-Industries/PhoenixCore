package net.phoenix.core.conflux.research;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;

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
