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
 * Compatibility shim — delegates everything to PhoenixCore.
 * Prefer using PhoenixCore directly. This class may be removed in the future.
 */
public class PonderIntegrationHelper {

    public static final String MOD_ID = PhoenixCore.PONDER_MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final Set<String> NAMESPACES = PhoenixCore.PONDER_NAMESPACES;
    public static final PonderStoriesManager STORIES_MANAGER = PhoenixCore.PONDER_STORIES_MANAGER;

    public static Optional<PonderTag> getTagByName(ResourceLocation res) {
        return PhoenixCore.getPonderTagByName(res);
    }

    public static Optional<PonderTag> getTagByName(String tag) {
        return PhoenixCore.getPonderTagByName(tag);
    }

    public static ResourceLocation appendPonderJSNamespaceToId(String id) {
        return PhoenixCore.appendPonderJSNamespaceToId(id);
    }

    public static void reload() {
        PhoenixCore.reloadPonderIntegration();
    }

    public static boolean isInitialized() {
        return PhoenixCore.isPonderIntegrationInitialized();
    }
}
