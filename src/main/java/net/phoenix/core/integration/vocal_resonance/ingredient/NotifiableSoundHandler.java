package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NotifiableSoundHandler extends NotifiableRecipeHandlerTrait<SoundIngredient> {

    private final ResonantJukeboxMachine controller;
    private final IO io;

    public NotifiableSoundHandler(ResonantJukeboxMachine controller, IO io) {
        super(controller.getHolder().getMetaMachine());
        this.controller = controller;
        this.io = io;
    }

    @Override
    public List<SoundIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SoundIngredient> left,
                                                   boolean simulate) {
        if (!controller.isActive() || controller.selectedLibrarySound.isEmpty()) return left;

        double distance = Math.sqrt(controller.getPos().distSqr(this.machine.getPos()));
        float soundReach = getPropagationFactor(controller.selectedLibrarySound);
        double effectivePower = ((controller.getFinalRange() / (distance + 1)) * soundReach) *
                controller.currentLiveBass;

        for (int i = 0; i < left.size(); i++) {
            SoundIngredient req = left.get(i);

            // 1. Name Check (Skipped if not exactMatch)
            boolean nameMatch = !req.exactMatch() || req.soundName().equals(controller.selectedLibrarySound);

            // 2. Power Check
            boolean powerMatch = effectivePower >= req.minLoudness();

            // 3. Spectral Check (Using tolerance)
            // If the recipe wants 0.8 brightness and tolerance is 0.1, we accept 0.7 to 0.9.
            float liveCentroid = controller.currentLiveBass; // Temporary until full analyzer integration
            boolean spectralMatch = req.targetCentroid() == 0 ||
                    (Math.abs(req.targetCentroid() - liveCentroid) <= req.tolerance());

            // 4. BPM Check (Using tolerance as a percentage of BPM)
            int liveBPM = 120; // Placeholder for live data
            boolean bpmMatch = req.requiredBPM() == 0 ||
                    (Math.abs(req.requiredBPM() - liveBPM) <= (req.requiredBPM() * req.tolerance()));

            if (nameMatch && powerMatch && spectralMatch && bpmMatch) {
                left.remove(i);
                break;
            }
        }
        return left.isEmpty() ? null : left;
    }

    public static float getPropagationFactor(String soundName) {
        if (soundName == null || soundName.isEmpty()) return 1.0f;
        String path = soundName.toLowerCase();
        if (path.contains("explosion") || path.contains("thunder")) return 8.0f;
        if (path.contains("music_disc")) return 2.5f;
        return 1.0f;
    }

    @Override
    public @NotNull List<Object> getContents() {
        return List.of(new SoundIngredient(controller.selectedLibrarySound, (int) controller.currentLiveBass));
    }

    @Override
    public double getTotalContentAmount() {
        return controller.isActive() ? 1.0 : 0.0;
    }

    @Override
    public RecipeCapability<SoundIngredient> getCapability() {
        return SoundRecipeCapability.CAP;
    }

    @Override
    public IO getHandlerIO() {
        return this.io;
    }
}
