package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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

        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getPos());
        if (data == null) return left;

        for (int i = 0; i < left.size(); i++) {
            SoundIngredient req = left.get(i);

            boolean nameMatch = !req.exactMatch() || req.soundName().equals(controller.selectedLibrarySound);

            boolean bassMatch = req.minBass() <= 0f || data.bass >= req.minBass();
            boolean midMatch = req.minMid() <= 0f || data.mid >= req.minMid();
            boolean trebleMatch = req.minTreble() <= 0f || data.treble >= req.minTreble();

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
