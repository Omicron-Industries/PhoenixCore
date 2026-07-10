package net.phoenix.core.conflux.client.render.discipline;

import net.phoenix.core.conflux.client.render.DisciplineRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps discipline ID strings to their {@link DisciplineRenderer} implementations.
 *
 * The two Sealed renderers share an implementation; each receives its own
 * discipline ID so they can have distinct lore and shader variations later.
 */
public final class DisciplineRendererRegistry {

    private static final Map<String, DisciplineRenderer> RENDERERS = new HashMap<>();
    private static final DisciplineRenderer DEFAULT = new DefaultDisciplineRenderer();

    static {
        register(new PhoenixDisciplineRenderer());
        register(new VoidDisciplineRenderer());
        register(new SculkDisciplineRenderer());
        register(new SealedDisciplineRenderer("sealed_a"));
        register(new SealedDisciplineRenderer("sealed_b"));
    }

    private DisciplineRendererRegistry() {}

    public static void register(DisciplineRenderer renderer) {
        String id = renderer.disciplineId();
        if (id != null) RENDERERS.put(id, renderer);
    }

    /**
     * Returns the renderer for the given discipline ID, or the default fallback
     * if {@code disciplineId} is null or unregistered.
     */
    public static DisciplineRenderer get(@Nullable String disciplineId) {
        if (disciplineId == null) return DEFAULT;
        return RENDERERS.getOrDefault(disciplineId, DEFAULT);
    }

    public static DisciplineRenderer getDefault() {
        return DEFAULT;
    }
}
