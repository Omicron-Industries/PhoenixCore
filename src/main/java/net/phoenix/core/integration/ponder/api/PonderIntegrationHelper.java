package net.phoenix.core.integration.ponder.api;

import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ponder.PonderStoriesManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Compatibility shim — delegates everything to {@link PhoenixCore}.
 * Prefer using {@link PhoenixCore} directly.
 */
public class PonderIntegrationHelper {

    // Removed: PONDER_MOD_ID reference (PhoenixCore.PONDER_MOD_ID no longer exists).
    public static final Logger LOGGER = LogManager.getLogger(PhoenixCore.MOD_ID);
    public static final Set<String> NAMESPACES = PhoenixCore.PONDER_NAMESPACES;
    public static final PonderStoriesManager STORIES_MANAGER = PhoenixCore.PONDER_STORIES_MANAGER;

    public static Optional<PonderTag> getTagByName(ResourceLocation res) {
        return PhoenixCore.getPonderTagByName(res);
    }

    public static Optional<PonderTag> getTagByName(String tag) {
        return PhoenixCore.getPonderTagByName(tag);
    }

    /**
     * Resolves a bare path or {@code namespace:path} string to a
     * {@code phoenixcore}-namespaced {@link ResourceLocation}.
     *
     * <p>Renamed from {@code appendPonderJSNamespaceToId} — PonderJS is no longer
     * a dependency and the old name was misleading.
     */
    public static ResourceLocation ponderIdOf(String id) {
        return PhoenixCore.ponderIdOf(id);
    }

    public static void reload() {
        PhoenixCore.reloadPonderIntegration();
    }

    public static boolean isInitialized() {
        return PhoenixCore.isPonderIntegrationInitialized();
    }
}