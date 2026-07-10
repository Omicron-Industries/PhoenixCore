package net.phoenix.core.conflux.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/**
 * Resolves a player → team UUID for research scoping.
 *
 * When FTB Teams is loaded, uses the player's current party team (or their personal
 * team if they have no party), so all teammates share the same research state.
 * Without FTB Teams each player's own UUID is used — effectively solo research.
 */
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
