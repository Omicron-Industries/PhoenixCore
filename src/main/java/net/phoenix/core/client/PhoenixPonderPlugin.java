package net.phoenix.core.client;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.ponder.PonderRegistrationManager;
import net.phoenix.core.integration.ponder.api.GTPonderRegistrar;
import net.phoenix.core.integration.ponder.multiblocks.GTPonderProcesses;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PhoenixPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return "phoenixcore";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderRegistrationManager.setHelper(helper);

        // Call the registrar here to fill the event queue
        GTPonderRegistrar.registerAllGTPonderScenes();

        // Then load the manager to execute the registrations
        PonderRegistrationManager.load();
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        GTRegistries.MACHINES.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .filter(def -> {
                    try {
                        return def.isRenderXEIPreview();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(def -> {
                    String path = def.getId().getPath();
                    // Scene id path used in addGeneratedMultiblockScene() — must use "/" separator
                    // to match the id.getPath() that ForItemsBuilder.scene() registers with Ponder.
                    String sceneIdPath = "gregtech_multiblocks/" + path;
                    String title = toTitleCase(path);
                    // Register the header (displayed in the Ponder scene title bar).
                    // Ponder resolves shared text by scene id path (slash-separated, no namespace).
                    helper.registerSharedText(sceneIdPath + ".header", title);
                    // Register a description tooltip shown in the Ponder index / item hover.
                    helper.registerSharedText(sceneIdPath + ".description",
                            "Shows the assembled structure, part locations, and operating mechanics for the " + title +
                                    ".");
                });

        // --- Process scenes (and any future hand-authored scenes) ---
        // GTPonderProcesses.SCENE_TITLES tracks every scene registered via registerScene()
        GTPonderProcesses.SCENE_TITLES.forEach((sceneIdPath, title) -> {
            helper.registerSharedText(sceneIdPath + ".header", title);
            helper.registerSharedText(sceneIdPath + ".description",
                    "Explains the " + title + " process.");
        });
    }

    private static String toTitleCase(String snake) {
        return Arrays.stream(snake.split("_"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}