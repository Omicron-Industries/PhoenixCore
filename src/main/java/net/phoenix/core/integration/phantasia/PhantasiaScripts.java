package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * PhantasiaScripts — central registry mapping definitions to their scripts.
 *
 * Register from your client-side init (FMLClientSetupEvent or similar):
 * 
 * <pre>{@code
 * PhantasiaScripts.register(MyMachines.MY_MULTI, PhantasiaScript.builder()
 *         .step(0, "This is My Machine.").showAll()
 *         .step(60, "The bottom layer is the foundation.").showLayer(0)
 *         .build());
 * }</pre>
 *
 * KubeJS support: scripts can also be registered from KJS startup scripts.
 * See PhantasiaKubeJS for the binding class.
 */
public class PhantasiaScripts {

    private static final Map<MultiblockMachineDefinition, PhantasiaScript> REGISTRY = new HashMap<>();

    public static void register(MultiblockMachineDefinition def, PhantasiaScript script) {
        REGISTRY.put(def, script);
    }

    /** Returns the registered script, or a fallback that just shows everything. */
    public static PhantasiaScript get(MultiblockMachineDefinition def) {
        return REGISTRY.getOrDefault(def, PhantasiaScript.showAll());
    }

    public static boolean has(MultiblockMachineDefinition def) {
        return REGISTRY.containsKey(def);
    }

    /** Call on world unload to clear cached world state (not the scripts themselves). */
    public static void invalidateWorldCache() {
        PhantasiaSceneScreen.invalidateSharedLevel();
    }
}
