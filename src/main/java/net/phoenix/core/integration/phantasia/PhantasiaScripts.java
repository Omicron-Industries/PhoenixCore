package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * PhantasiaScripts — central runtime registry of compiled PhantasiaScript objects.
 *
 * Scripts are loaded exclusively by {@link PhantasiaScriptLoader} from JSON files on disk.
 * Every discovered multiblock gets at minimum a default (show-all) JSON.
 * Custom scripts are authored in-game via PhantasiaScriptEditorScreen, saved as JSON,
 * and hot-reloaded here without a world restart.
 *
 * To seed a script from Java code, call:
 * PhantasiaScriptLoader.save(machineId, scriptData);
 * This writes the JSON file and immediately registers the compiled script here.
 */
public class PhantasiaScripts {

    private static final Map<MultiblockMachineDefinition, PhantasiaScript> REGISTRY = new HashMap<>();

    // ── Called by PhantasiaScriptLoader ──────────────────────────────────────

    public static void registerJson(MultiblockMachineDefinition def, PhantasiaScript script) {
        REGISTRY.put(def, script);
    }

    public static void clearJson(MultiblockMachineDefinition def) {
        REGISTRY.remove(def);
    }

    /** Wipe everything before a reload pass. */
    public static void clearAllJson() {
        REGISTRY.clear();
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /** Returns the compiled script, or a show-all fallback if not yet loaded. */
    public static PhantasiaScript get(MultiblockMachineDefinition def) {
        return REGISTRY.getOrDefault(def, PhantasiaScript.showAll());
    }

    public static boolean has(MultiblockMachineDefinition def) {
        return REGISTRY.containsKey(def);
    }

    // ── World cache ───────────────────────────────────────────────────────────

    public static void invalidateWorldCache() {
        PhantasiaSceneScreen.invalidateSharedLevel();
    }
}
