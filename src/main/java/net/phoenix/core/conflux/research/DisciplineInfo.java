package net.phoenix.core.conflux.research;

import net.phoenix.core.conflux.ConfluxDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Read-only snapshot of a team's current Discipline state, designed for GUI consumption.
 *
 * Obtain via {@link WorldResearchData#getDisciplineInfo(java.util.UUID, ResearchTreeRegistry)}
 * on the server, or {@link net.phoenix.core.conflux.client.ClientResearchCache#getDisciplineInfo()}
 * on the client (synced via S2CResearchSyncPacket).
 *
 * <ul>
 *   <li>{@code disciplineId} — the canonical discipline string (e.g. {@code "thermodynamics"}),
 *       or {@code null} if the team has not yet entered any Discipline tree.</li>
 *   <li>{@code disciplineTitle} — human-readable name of the tree, for display.</li>
 *   <li>{@code committed} — {@code true} once the commitment node has been unlocked;
 *       switching is permanently blocked.</li>
 *   <li>{@code switchCost} — resources required to abandon this Discipline before commitment.
 *       Empty if the team is uncommitted, has no Discipline, or the tree declares no switch cost.</li>
 * </ul>
 */
public record DisciplineInfo(
        @Nullable String disciplineId,
        @Nullable String disciplineTitle,
        boolean committed,
        Map<ConfluxDataType, Long> switchCost
) {

    /** Sentinel: no Discipline chosen yet. */
    public static final DisciplineInfo NONE = new DisciplineInfo(null, null, false, Map.of());

    public boolean hasDiscipline() { return disciplineId != null; }

    /** True if the team can still abandon their Discipline (has one, and not yet committed). */
    public boolean canSwitch() { return hasDiscipline() && !committed; }
}
