package net.phoenix.core.integration.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PonderRegistrationManager {

    private static PonderSceneRegistrationHelper<ResourceLocation> helperInstance;

    private static final List<Consumer<PonderBuilder>> sceneRegistrars = new ArrayList<>();
    private static final List<Consumer<PonderTagBuilder>> tagRegistrars = new ArrayList<>();

    // This now matches the field above
    public static void setHelper(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helperInstance = helper;
    }

    public static void register(Consumer<PonderBuilder> registrar) {
        sceneRegistrars.add(registrar);
    }

    public static void registerTags(Consumer<PonderTagBuilder> registrar) {
        tagRegistrars.add(registrar);
    }

    public static void load() {
        // Safety check: Don't proceed if the helper isn't ready
        if (helperInstance == null) {
            PhoenixCore.LOGGER.error("PonderRegistrationManager.load() called before helper was set!");
            return;
        }

        PonderBuilder ponderBuilder = new PonderBuilder(helperInstance);

        // Execute all registered scene consumers (registered via GTPonderRegistrar.registerAllGTPonderScenes()).
        // These already include GTPonderMultiblocks.register() and GTPonderProcesses.register(),
        // so we must NOT call them again below — doing so causes every machine to receive
        // every other machine's scenes and every process scene to be registered twice.
        for (Consumer<PonderBuilder> registrar : sceneRegistrars) {
            registrar.accept(ponderBuilder);
        }

        // DO NOT add additional calls to GTPonderMultiblocks or GTPonderProcesses here.
        // PhoenixPonderPlugin.registerScenes() drives everything through GTPonderRegistrar
        // → registerAllGTPonderScenes() → sceneRegistrars, which already covers both.

        PonderTagBuilder tagBuilder = new PonderTagBuilder();
        for (Consumer<PonderTagBuilder> registrar : tagRegistrars) {
            registrar.accept(tagBuilder);
        }
        tagBuilder.register();
    }
}
