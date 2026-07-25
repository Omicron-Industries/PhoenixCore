package net.phoenix.core.conflux.client.render.discipline;

import net.phoenix.core.conflux.client.render.DisciplineRenderer;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    public static DisciplineRenderer get(@Nullable String disciplineId) {
        if (disciplineId == null) return DEFAULT;
        return RENDERERS.getOrDefault(disciplineId, DEFAULT);
    }

    public static DisciplineRenderer getDefault() {
        return DEFAULT;
    }
}
