package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * PhantasiaScriptLoader
 *
 * Owns the entire lifecycle of JSON scripts on disk:
 *
 * DISCOVERY — scans GTCEuAPI.MACHINE_REGISTRY for every MultiblockMachineDefinition,
 * registers it in PhantasiaSceneSelectionScreen, and creates a default
 * JSON on disk if none exists yet.
 *
 * LOAD — reads every .json file under the script directory and compiles it
 * into a PhantasiaScript via PhantasiaScripts.registerJson().
 *
 * SAVE — serialises a PhantasiaScriptData to disk and hot-reloads the
 * compiled script in the registry, no restart required.
 *
 * RELOAD — clears the JSON registry and re-runs LOAD; callable from
 * /phantasia reload or the in-game editor.
 *
 * Directory layout:
 * <gamedir>/kubejs/data/phantasia/scripts/<namespace>/<path>.json
 *
 * The namespace/path pair mirrors the machine's registry key, so
 * gtceu:electric_blast_furnace → .../scripts/gtceu/electric_blast_furnace.json
 */
public class PhantasiaScriptLoader {

    /**
     * Canonical script directory going forward.
     * Layout: <gamedir>/data/phoenixcore/phantasia/scripts/<namespace>/<path>.json
     */
    private static final Path SCRIPT_DIR = FMLPaths.GAMEDIR.get()
            .resolve("data/phoenixcore/phantasia/scripts");

    /**
     * Legacy directory (old kubejs location). Files here are migrated to SCRIPT_DIR
     * on first load and the originals are left in place (not deleted) so nothing is lost.
     */
    private static final Path LEGACY_SCRIPT_DIR = FMLPaths.GAMEDIR.get()
            .resolve("kubejs/data/phantasia/scripts");

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called once during FMLClientSetupEvent (from PhoenixClient).
     * Migrates any legacy kubejs scripts, discovers all multiblocks,
     * writes missing default JSONs, then loads everything.
     *
     * NOTE: if GTRegistries.MACHINES is not yet fully populated at call time
     * (e.g. some addon registers late), scripts for those machines will be
     * silently skipped here. Call {@link #reload()} later (e.g. on first screen
     * open) to pick them up. PhantasiaScripts.get() returns a show-all fallback
     * for any machine whose script hasn't been registered yet, so nothing breaks.
     */
    public static void discoverAndLoad() {
        migrateLegacyScripts();
        ensureDir(SCRIPT_DIR);
        discoverAllMultiblocks();
        loadAll();
    }

    /**
     * Clears the JSON script registry and re-reads all files from disk.
     * Safe to call at any time from any thread; the scene screen picks up new
     * scripts on next open. Also called lazily by PhantasiaSceneScreen on first
     * open to catch machines whose definitions weren't ready during client setup.
     */
    public static void reload() {
        PhantasiaScripts.clearAllJson();
        loadAll();
    }

    /**
     * Write a script to disk and immediately hot-reload it into the registry.
     * Called by the in-game editor on "Save".
     *
     * @param machineId the registry key, e.g. "gtceu:electric_blast_furnace"
     * @param data      the script data to persist
     */
    public static void save(String machineId, PhantasiaScriptData data) {
        Path path = pathFor(machineId);
        try {
            ensureDir(path.getParent());
            Files.writeString(path, data.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logErr("Failed to save script for " + machineId + ": " + e.getMessage());
            return;
        }

        // Hot-reload this one file
        MultiblockMachineDefinition def = resolveDefinition(machineId);
        if (def != null) {
            PhantasiaScripts.registerJson(def, PhantasiaScript.fromData(data));
            log("Saved and hot-reloaded script for " + machineId);
        } else {
            logErr("Saved script for " + machineId + " but could not resolve definition — " +
                    "it will load on next reload.");
        }
    }

    /**
     * Returns the canonical path on disk for a given machine ID.
     * Useful for the editor to show the file path to the user.
     */
    public static Path pathFor(String machineId) {
        ResourceLocation rl = parseId(machineId);
        return SCRIPT_DIR.resolve(rl.getNamespace()).resolve(rl.getPath() + ".json");
    }

    /**
     * Called by PhantasiaSceneScreen on first open to catch machines whose
     * definitions weren't ready during FMLClientSetupEvent.
     * Only runs a full reload once per game session after the initial load.
     */
    private static boolean hasLazyReloaded = false;

    public static void reloadIfNeeded() {
        if (!hasLazyReloaded) {
            hasLazyReloaded = true;
            log("Performing lazy reload to catch late-registered machine definitions...");
            reload();
        }
    }

    /** Reset the lazy-reload flag (e.g. on world unload). */
    public static void resetLazyReload() {
        hasLazyReloaded = false;
    }

    /**
     * Copies any .json files from the old kubejs/data/phantasia/scripts tree into
     * the new data/phoenixcore/phantasia/scripts tree. Original files are NOT
     * deleted — this is purely additive, so nothing is ever lost.
     * Files that already exist in the new location are skipped (new wins).
     */
    private static void migrateLegacyScripts() {
        if (!Files.exists(LEGACY_SCRIPT_DIR)) return;
        int migrated = 0;
        try (var stream = Files.walk(LEGACY_SCRIPT_DIR)) {
            for (Path src : (Iterable<Path>) stream::iterator) {
                if (!src.toString().endsWith(".json")) continue;
                Path rel = LEGACY_SCRIPT_DIR.relativize(src);
                Path dest = SCRIPT_DIR.resolve(rel);
                if (Files.exists(dest)) continue; // new location wins
                try {
                    ensureDir(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    migrated++;
                } catch (IOException e) {
                    logErr("Migration copy failed for " + src + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logErr("Error walking legacy script directory during migration: " + e.getMessage());
        }
        if (migrated > 0) log("Migrated " + migrated + " script(s) from legacy kubejs location.");
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    /**
     * Walks GTCEuAPI.MACHINE_REGISTRY and registers every MultiblockMachineDefinition
     * in the Phantasia selection screen. Creates a default JSON for any machine that
     * doesn't already have one. This covers PhoenixCore machines, GTCEu machines,
     * GCYM machines, and any other loaded GTCEu addon.
     */
    private static void discoverAllMultiblocks() {
        var scenes = net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen.PHANTASIA_SCENES;

        // Use GTRegistries.MACHINES to get the registry
        for (MachineDefinition def : GTRegistries.MACHINES) {
            if (!(def instanceof MultiblockMachineDefinition multi)) continue;

            if (!scenes.contains(multi)) {
                scenes.add(multi);
            }

            String machineId = def.getId().toString();
            Path path = pathFor(machineId);
            if (!Files.exists(path)) {
                writeDefaultScript(machineId, path);
            }
        }

        log("Discovered " + scenes.size() + " multiblock machines.");
    }

    private static void writeDefaultScript(String machineId, Path path) {
        try {
            ensureDir(path.getParent());
            PhantasiaScriptData data = PhantasiaScriptData.defaultFor(machineId);
            Files.writeString(path, data.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logErr("Could not write default script for " + machineId + ": " + e.getMessage());
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private static void loadAll() {
        if (!Files.exists(SCRIPT_DIR)) return;
        int loaded = 0, failed = 0;
        try (var stream = Files.walk(SCRIPT_DIR)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!path.toString().endsWith(".json")) continue;
                if (loadOne(path)) loaded++;
                else failed++;
            }
        } catch (IOException e) {
            logErr("Error walking script directory: " + e.getMessage());
        }
        log("Loaded " + loaded + " scripts" + (failed > 0 ? ", " + failed + " failed." : "."));
    }

    /** Returns true on success, false on failure. */
    private static boolean loadOne(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            PhantasiaScriptData data = PhantasiaScriptData.fromJson(reader);
            if (data.getMachine() == null || data.getMachine().isBlank()) {
                data = inferMachine(data, path);
            }
            if (data.getMachine() == null || data.getMachine().isBlank()) {
                logErr("Could not determine machine ID for " + path + " — skipping.");
                return false;
            }

            MultiblockMachineDefinition def = resolveDefinition(data.getMachine());
            if (def == null) {
                // This is the most common silent failure: the machine definition isn't
                // registered yet (addon registered late, or typo in the JSON).
                // Log at warn level so it's visible in the console.
                logErr("No MultiblockMachineDefinition found for \"" + data.getMachine() +
                        "\" (from " + path.getFileName() + ") — script will not apply. " +
                        "Check the machine ID, or call /phantasia reload after world load.");
                return false;
            }

            PhantasiaScripts.registerJson(def, PhantasiaScript.fromData(data));
            return true;
        } catch (Exception e) {
            logErr("Failed to load " + path + ": " + e.getMessage());
            return false;
        }
    }

    private static PhantasiaScriptData inferMachine(PhantasiaScriptData original, Path path) {
        // Reconstruct "<namespace>:<path>" from the file's location inside SCRIPT_DIR
        try {
            Path relative = SCRIPT_DIR.relativize(path);
            // relative = namespace/machine_name.json (or deeper)
            String namespace = relative.getName(0).toString();
            String rest = relative.subpath(1, relative.getNameCount()).toString()
                    .replace(File.separatorChar, '/').replaceAll("\\.json$", "");
            String machineId = namespace + ":" + rest;
            // Rebuild data with the inferred machine id
            PhantasiaScriptData fixed = original.copy();
            // Use reflection-free approach: serialise, patch, deserialise
            String json = fixed.toJson()
                    .replaceFirst("\"machine\"\\s*:\\s*\"[^\"]*\"",
                            "\"machine\": \"" + machineId + "\"");
            return PhantasiaScriptData.fromJson(json);
        } catch (Exception e) {
            return original;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static MultiblockMachineDefinition resolveDefinition(String machineId) {
        // Ensure you are using the correct ResourceLocation parser
        ResourceLocation rl = ResourceLocation.parse(machineId);

        // Access the machine through the modern GTRegistries class
        MachineDefinition def = com.gregtechceu.gtceu.api.registry.GTRegistries.MACHINES.get(rl);

        // Pattern matching handles the null check and the type check safely
        if (def instanceof MultiblockMachineDefinition multi) {
            return multi;
        }

        return null;
    }

    private static ResourceLocation parseId(String machineId) {
        return machineId.contains(":") ? new ResourceLocation(machineId) : new ResourceLocation("gtceu", machineId);
    }

    private static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
    }

    private static void log(String msg) {
        System.out.println("[Phantasia] " + msg);
    }

    private static void logErr(String msg) {
        System.err.println("[Phantasia] ERROR: " + msg);
    }
}
