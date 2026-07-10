package net.phoenix.core.conflux.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.research.DisciplineInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of the player's research state, populated by S2CResearchSyncPacket.
 *
 * Discipline state is included so the terminal GUI can display the discipline banner,
 * commitment status, and switch cost without querying the server.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientResearchCache {

    public static Set<ResourceLocation> unlocked  = Collections.emptySet();
    public static Set<ResourceLocation> lockedOut = Collections.emptySet();
    public static Set<String>           flags     = Collections.emptySet();

    // Discipline state synced from server
    private static @Nullable String disciplineId    = null;
    private static @Nullable String disciplineTitle = null;
    private static boolean          disciplineCommitted = false;
    private static Map<ConfluxDataType, Long> switchCost = Map.of();

    private ClientResearchCache() {}

    public static void update(Set<ResourceLocation> u, Set<ResourceLocation> lo, Set<String> f,
                              @Nullable String discId, @Nullable String discTitle,
                              boolean committed, Map<ConfluxDataType, Long> cost) {
        unlocked              = Set.copyOf(u);
        lockedOut             = Set.copyOf(lo);
        flags                 = Set.copyOf(f);
        disciplineId          = discId;
        disciplineTitle       = discTitle;
        disciplineCommitted   = committed;
        switchCost            = Map.copyOf(cost);
    }

    public static void clear() {
        unlocked            = Collections.emptySet();
        lockedOut           = Collections.emptySet();
        flags               = Collections.emptySet();
        disciplineId        = null;
        disciplineTitle     = null;
        disciplineCommitted = false;
        switchCost          = Map.of();
    }

    public static boolean isUnlocked(ResourceLocation id)  { return unlocked.contains(id); }
    public static boolean isLockedOut(ResourceLocation id) { return lockedOut.contains(id); }

    /** Returns the client-side discipline snapshot ready for GUI rendering. */
    public static DisciplineInfo getDisciplineInfo() {
        if (disciplineId == null) return DisciplineInfo.NONE;
        return new DisciplineInfo(disciplineId, disciplineTitle, disciplineCommitted, switchCost);
    }
}
