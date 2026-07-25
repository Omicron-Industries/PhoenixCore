package net.phoenix.core.conflux.tools.capture;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SpriteCaptureRegistry {

    private static final Map<String, CaptureBakeable> REGISTRY = new LinkedHashMap<>();

    private SpriteCaptureRegistry() {}

    public static void register(CaptureBakeable b) {
        REGISTRY.put(b.id(), b);
    }

    public static Optional<CaptureBakeable> get(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<CaptureBakeable> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
