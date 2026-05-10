package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

/**
 * PhantasiaKubeJS
 *
 * KubeJS binding injected as "Phantasia" in startup scripts.
 * Registered in PhoenixKubeJSPlugin via:
 * event.add("Phantasia", PhantasiaKubeJS.class); // startup only
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * KJS USAGE (kubejs/startup_scripts/my_phantasia.js):
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * // Add a machine to the Phantasia selection screen:
 * Phantasia.addToSelectionScreen("gtceu:electric_blast_furnace");
 *
 * // Register a full animated script:
 * Phantasia.register("gtceu:electric_blast_furnace", script => {
 * script
 * .step(0, "The EBF smelts at extreme temperatures.")
 * .showAll()
 * .step(60, "The bottom layer is Heat-Proof Casings.")
 * .showLayer(0)
 * .step(120, "The controller sits center-front.")
 * .showPos(1, 0, 0)
 * .mistake(1, 3, 1, "Muffler hatch must be on top")
 * .build();
 * });
 *
 * // Or register a simple one-liner that just shows everything:
 * Phantasia.registerSimple("gtceu:large_chemical_reactor",
 * "The Large Chemical Reactor handles complex multi-fluid reactions.");
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * POSITIONS are LOCAL (shape-origin relative, 0-based). Y=0 = bottom layer.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class PhantasiaKubeJS {

    // ──────────────────────────────────────────────────────────────────────────
    // Selection screen registration
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Add a machine to the Phantasia selection screen (the card grid).
     * Must be called from a startup script — resolves the block immediately.
     *
     * @param registryPath e.g. "gtceu:electric_blast_furnace" or "phoenixcore:alchemical_imbuer"
     */
    public static void addToSelectionScreen(String registryPath) {
        MultiblockMachineDefinition def = resolveDefinition(registryPath);
        if (def == null) return;
        if (!PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(def)) {
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(def);
            System.out.println("[Phantasia] Added to selection screen: " + registryPath);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Script registration
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Register a full animated Phantasia script via a fluent builder lambda.
     *
     * @param registryPath Registry path of the multiblock controller block.
     * @param configurator Lambda receiving a {@link ScriptBuilder}; must call .build() at the end.
     */
    public static void register(String registryPath, Consumer<ScriptBuilder> configurator) {
        MultiblockMachineDefinition def = resolveDefinition(registryPath);
        if (def == null) return;

        ScriptBuilder sb = new ScriptBuilder();
        configurator.accept(sb);
        PhantasiaScript script = sb.build();

        PhantasiaScripts.register(def, script);
        System.out.println(
                "[Phantasia] Registered script for " + registryPath + " (" + script.getSteps().size() + " steps)");
    }

    /**
     * Register a simple single-step script that shows the entire machine
     * with a single intro caption. Useful for machines that don't need
     * a full animation.
     *
     * @param registryPath Registry path of the multiblock controller block.
     * @param caption      Intro text shown in the caption bar.
     */
    public static void registerSimple(String registryPath, String caption) {
        MultiblockMachineDefinition def = resolveDefinition(registryPath);
        if (def == null) return;
        PhantasiaScripts.register(def, PhantasiaScript.simple(caption));
        System.out.println("[Phantasia] Registered simple script for " + registryPath);
    }

    /**
     * Register a pre-built PhantasiaScript directly.
     * Useful when building the script in Java and exposing it to KJS.
     */
    public static void registerScript(String registryPath, PhantasiaScript script) {
        MultiblockMachineDefinition def = resolveDefinition(registryPath);
        if (def == null) return;
        PhantasiaScripts.register(def, script);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fluent script builder — JS-friendly (no BlockPos imports needed)
    // ──────────────────────────────────────────────────────────────────────────

    public static class ScriptBuilder {

        private final PhantasiaScript.Builder builder = PhantasiaScript.builder();

        // Step
        public ScriptBuilder step(int tickOffset, String caption) {
            builder.step(tickOffset, caption);
            return this;
        }

        // show*
        public ScriptBuilder showAll() {
            builder.showAll();
            return this;
        }

        public ScriptBuilder showLayer(int y) {
            builder.showLayer(y);
            return this;
        }

        public ScriptBuilder showLayers(int minY, int maxY) {
            builder.showLayers(minY, maxY);
            return this;
        }

        public ScriptBuilder showPos(int x, int y, int z) {
            builder.showPos(new BlockPos(x, y, z));
            return this;
        }

        // hide*
        public ScriptBuilder hidePos(int x, int y, int z) {
            builder.hidePos(new BlockPos(x, y, z));
            return this;
        }

        public ScriptBuilder hideLayer(int y) {
            builder.hideLayer(y);
            return this;
        }

        // Common mistakes
        public ScriptBuilder mistake(int x, int y, int z, String label) {
            builder.mistake(x, y, z, label);
            return this;
        }

        public ScriptBuilder mistake(int x, int y, int z, String label, int argbColor) {
            builder.mistake(x, y, z, label, argbColor);
            return this;
        }

        /** Must be called at the end of the lambda to finalise the script. */
        public PhantasiaScript build() {
            return builder.build();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal
    // ──────────────────────────────────────────────────────────────────────────

    private static MultiblockMachineDefinition resolveDefinition(String registryPath) {
        ResourceLocation rl = registryPath.contains(":") ? new ResourceLocation(registryPath) :
                new ResourceLocation("gtceu", registryPath);

        var block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null) {
            System.err.println("[Phantasia] WARNING: No block found for '" + rl + "'.");
            return null;
        }
        if (!(block instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock mb)) {
            System.err.println("[Phantasia] WARNING: '" + rl + "' is not a MetaMachineBlock.");
            return null;
        }
        if (!(mb.getDefinition() instanceof MultiblockMachineDefinition def)) {
            System.err.println("[Phantasia] WARNING: '" + rl + "' is not a multiblock.");
            return null;
        }
        return def;
    }
}
