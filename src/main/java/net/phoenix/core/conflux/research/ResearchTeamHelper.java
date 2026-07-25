package net.phoenix.core.conflux.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

public final class ResearchTeamHelper {

    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    public static UUID getTeamId(ServerPlayer player) {
        if (FTB_TEAMS) {
            return FTBTeamsCompat.getTeamId(player);
        }
        return player.getUUID();
    }

    private ResearchTeamHelper() {}
}
