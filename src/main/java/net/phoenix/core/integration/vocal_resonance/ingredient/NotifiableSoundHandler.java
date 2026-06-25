package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Recipe handler that checks world-ambient audio conditions against a {@link SoundIngredient}.
 *
 * Data source: {@link WorldAcousticSensor} — populated server-side from packets sent by
 * every client player whose game is analysing sounds near this machine. Any sound in the
 * world (not just the jukebox) can satisfy the ingredient provided it propagates within
 * the sensor's listen radius.
 *
 * Registered as a field on {@link ResonantJukeboxMachine} so GTCEu's recipe pipeline
 * finds it automatically via {@code getRecipeHandlers()} → trait scan.
 */
public class NotifiableSoundHandler extends NotifiableRecipeHandlerTrait<SoundIngredient> {

    private final ResonantJukeboxMachine controller;
    private final IO io;

    public NotifiableSoundHandler(ResonantJukeboxMachine controller, IO io) {
        super(controller);
        this.controller = controller;
        this.io = io;
    }

    @Override
    public List<SoundIngredient> handleRecipeInner(IO io, GTRecipe recipe,
                                                   List<SoundIngredient> left,
                                                   boolean simulate) {
        if (!controller.isActive()) return left;

        // Pull the latest acoustic snapshot from the world sensor at this machine's position.
        // Returns null if nobody has registered this machine yet (shouldn't happen if
        // ResonantJukeboxMachine.onStructureFormed registers it, but guard anyway).
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getPos());
        if (data == null) return left;

        for (int i = 0; i < left.size(); i++) {
            SoundIngredient req = left.get(i);

            // 1. Optional exact sound-name match (only for jukebox-sourced audio)
            boolean nameMatch = !req.exactMatch() || req.soundName().equals(controller.selectedLibrarySound);

            // 2. Frequency band minimums — all three are independently optional (0 = skip)
            boolean bassMatch = req.minBass() <= 0f || data.bass >= req.minBass();
            boolean midMatch = req.minMid() <= 0f || data.mid >= req.minMid();
            boolean trebleMatch = req.minTreble() <= 0f || data.treble >= req.minTreble();

            // 3. BPM with tolerance window
            boolean bpmMatch = req.requiredBPM() == 0 ||
                    Math.abs(req.requiredBPM() - data.bpm) <= (req.requiredBPM() * req.tolerance());

            if (nameMatch && bassMatch && midMatch && trebleMatch && bpmMatch) {
                left.remove(i);
                break;
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public @NotNull List<Object> getContents() {
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getPos());
        float bass = data != null ? data.bass : 0f;
        return List.of(new SoundIngredient(controller.selectedLibrarySound, bass));
    }

    @Override
    public double getTotalContentAmount() {
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getPos());
        return (data != null && data.bpm > 0) ? 1.0 : 0.0;
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
