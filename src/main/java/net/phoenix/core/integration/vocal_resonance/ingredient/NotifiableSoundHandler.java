package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;

import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe handler that checks world-ambient audio conditions against a {@link SoundIngredient}.
 */
public class NotifiableSoundHandler extends NotifiableRecipeHandlerTrait<SoundIngredient> {

    // Instantiating the clean GTM 8.0 trait type lookup matching your working class pattern
    public static final MachineTraitType<NotifiableSoundHandler> TRAIT_TYPE =
            new MachineTraitType<>(NotifiableSoundHandler.class);

    private final ResonantJukeboxMachine controller;
    private final IO handlerIO;

    public NotifiableSoundHandler(ResonantJukeboxMachine controller, IO io) {
        super(); // Avoid calling super(controller) to align with your working Ars container
        this.controller = controller;
        this.handlerIO = io;
    }

    @Override
    public IO getHandlerIO() {
        return this.handlerIO;
    }

    @Override
    public List<SoundIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SoundIngredient> left,
                                                   boolean simulate) {
        if (io != this.handlerIO || !controller.isActive()) return left;

        // Pull the latest acoustic snapshot from the world sensor at this machine's position.
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getBlockPos());
        if (data == null) return left;

        List<SoundIngredient> missingIngredients = new ArrayList<>();

        for (SoundIngredient req : left) {
            // 1. Optional exact sound-name match
            boolean nameMatch = !req.exactMatch() || req.soundName().equals(controller.selectedLibrarySound);

            // 2. Frequency band minimums
            boolean bassMatch = req.minBass() <= 0f || data.bass >= req.minBass();
            boolean midMatch = req.minMid() <= 0f || data.mid >= req.minMid();
            boolean trebleMatch = req.minTreble() <= 0f || data.treble >= req.minTreble();

            // 3. BPM with tolerance window
            boolean bpmMatch = req.requiredBPM() == 0 ||
                    Math.abs(req.requiredBPM() - data.bpm) <= (req.requiredBPM() * req.tolerance());

            // If any condition fails, it's missing
            if (!(nameMatch && bassMatch && midMatch && trebleMatch && bpmMatch)) {
                missingIngredients.add(req);
            }
        }

        return missingIngredients;
    }

    // FIXED CLASH: Using raw List type matching your working Ars Nouveau container
    @Override
    @SuppressWarnings("rawtypes")
    public @NotNull List getContents() {
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getBlockPos());
        float bass = data != null ? data.bass : 0f;
        float mid = data != null ? data.mid : 0f;
        float treble = data != null ? data.treble : 0f;
        int bpm = data != null ? data.bpm : 0;

        String sound = controller.selectedLibrarySound != null ? controller.selectedLibrarySound : "";

        return List.of(new SoundIngredient(
                sound,
                bass,
                mid,
                treble,
                bpm,
                !sound.isEmpty(),
                0.2f
        ));
    }

    @Override
    public RecipeCapability<SoundIngredient> getCapability() {
        return SoundRecipeCapability.CAP;
    }

    @Override
    public double getTotalContentAmount() {
        WorldAcousticSensor.SensorData data = WorldAcousticSensor.get(controller.getBlockPos());
        return (data != null && (data.bpm > 0 || data.bass > 0f || data.mid > 0f || data.treble > 0f)) ? 1.0 : 0.0;
    }

    // FIXED: Returning the static trait type instance required by the framework compilation layer
    @Override
    public MachineTraitType<?> getTraitType() {
        return TRAIT_TYPE;
    }
}