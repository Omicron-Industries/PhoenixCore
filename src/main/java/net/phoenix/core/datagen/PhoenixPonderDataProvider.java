package net.phoenix.core.datagen;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.phoenix.core.integration.ponder.PhoenixPonderSceneDefinitions;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class PhoenixPonderDataProvider implements DataProvider {

    private final PackOutput packOutput;

    public PhoenixPonderDataProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // We point the generator to the datagen output folder
        Path outputFolder = this.packOutput.getOutputFolder().resolve("kubejs_export/client_scripts/generated/");

        return CompletableFuture.runAsync(() -> {
            // Call your existing logic, but modified to take a Path
            PhoenixPonderSceneDefinitions.generateAllScenes(outputFolder);
        });
    }

    @Override
    public String getName() {
        return "Phoenix Core Ponder Scenes";
    }
}
