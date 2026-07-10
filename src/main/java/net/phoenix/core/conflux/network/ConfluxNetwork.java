package net.phoenix.core.conflux.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.research.DisciplineInfo;
import net.phoenix.core.conflux.research.ResearchTeamHelper;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.conflux.research.WorldResearchData;

import java.util.UUID;

public final class ConfluxNetwork {

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PhoenixCore.MOD_ID, "conflux"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, C2SResearchUnlockPacket.class,
                C2SResearchUnlockPacket::encode,
                C2SResearchUnlockPacket::decode,
                C2SResearchUnlockPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SAbandonDisciplinePacket.class,
                C2SAbandonDisciplinePacket::encode,
                C2SAbandonDisciplinePacket::decode,
                C2SAbandonDisciplinePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, S2CResearchSyncPacket.class,
                S2CResearchSyncPacket::encode,
                S2CResearchSyncPacket::decode,
                S2CResearchSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Send the player's team research state (including discipline) to their client. */
    public static void syncResearchToPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        UUID teamId = ResearchTeamHelper.getTeamId(player);
        WorldResearchData data = WorldResearchData.get(level);
        DisciplineInfo disc = data.getDisciplineInfo(teamId, ResearchTreeRegistry.INSTANCE);
        CHANNEL.sendTo(
                new S2CResearchSyncPacket(
                        data.getUnlocked(teamId), data.getLockedOut(teamId), data.getFlags(teamId),
                        disc.disciplineId(), disc.disciplineTitle(), disc.committed(), disc.switchCost()),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }
}
