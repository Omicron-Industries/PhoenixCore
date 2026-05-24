package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

public class SoundRecipeCapability extends RecipeCapability<SoundIngredient> {

    public final static SoundRecipeCapability CAP = new SoundRecipeCapability();

    protected SoundRecipeCapability() {
        super("sound", 0x00FFFF, false, 1, SoundIngredient.Serializer.INSTANCE);
    }

    @Override
    public SoundIngredient copyInner(SoundIngredient content) {
        return content.copy();
    }

    @Override
    public void addXEIInfo(WidgetGroup group, int xOffset, GTRecipe recipe, List<Content> contents, boolean perTick,
                           boolean isInput, MutableInt yOffset) {
        for (var content : contents) {
            SoundIngredient sound = of(content);
            if (isInput) {
                String namePrefix = sound.exactMatch() ? "§bRequired: §f" : "§3Suggested: §f";
                String displayName = sound.soundName().isEmpty() ? "Any Sonic Wave" : sound.soundName();
                group.addWidget(new LabelWidget(xOffset, yOffset.addAndGet(10), namePrefix + displayName));

                if (sound.requiredBPM() > 0) {
                    String tolStr = String.format("%.0f%%", (1.0f - sound.tolerance()) * 100);
                    group.addWidget(new LabelWidget(xOffset, yOffset.addAndGet(10),
                            "§6Tempo: §e" + sound.requiredBPM() + " BPM §8(Sync: " + tolStr + ")"));
                }

                if (sound.targetCentroid() > 0) {
                    String tone = sound.targetCentroid() > 0.6f ? "§dHigh Treble" : "§5Sub-Bass";
                    group.addWidget(new LabelWidget(xOffset, yOffset.addAndGet(10), "§7Tone: " + tone));
                }
            }
        }
    }
}
