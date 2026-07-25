package net.phoenix.core.conflux.research;

import net.phoenix.core.conflux.ConfluxDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record DisciplineInfo(
        @Nullable String disciplineId,
        @Nullable String disciplineTitle,
        boolean committed,
        Map<ConfluxDataType, Long> switchCost
) {

    public static final DisciplineInfo NONE = new DisciplineInfo(null, null, false, Map.of());

    public boolean hasDiscipline() { return disciplineId != null; }

    public boolean canSwitch() { return hasDiscipline() && !committed; }
}
