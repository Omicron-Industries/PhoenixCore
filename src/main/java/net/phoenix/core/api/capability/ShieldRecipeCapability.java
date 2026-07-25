package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.resources.language.I18n;
import net.phoenix.core.common.machine.multiblock.Shield.ShieldTypes;

import com.mojang.serialization.Codec;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

public class ShieldRecipeCapability extends RecipeCapability<ShieldTypes> {

    public static ShieldRecipeCapability CAP = new ShieldRecipeCapability();

    protected ShieldRecipeCapability() {
        super("Shield", 0xFF00FFFF, false, 11, SerializerShield.INSTANCE);
    }

    @Override
    public ShieldTypes copyInner(ShieldTypes content) {
        return content;
    }

    @Override
    public boolean isRecipeSearchFilter() {
        return true;
    }

    @Override
    public void addXEIInfo(WidgetGroup group, int xOffset, GTRecipe recipe, List<Content> contents, boolean perTick,
                           boolean isInput, MutableInt yOffset) {
        if (isInput) {
            ShieldTypes shieldType = (ShieldTypes) contents.get(0).getContent();
            group.addWidget(
                    new LabelWidget(xOffset + 3, yOffset.addAndGet(10),
                            I18n.get("emi_info.phoenixcore.required_shield", I18n.get(shieldType.langKey))));
        }

        if (recipe.data.contains("shield_health_change")) {
            int healthChange = recipe.data.getInt("shield_health_change");

            if (healthChange != 0) {
                String langKey = healthChange > 0 ? "emi_info.phoenixcore.shield_heal" :
                        "emi_info.phoenixcore.shield_damage";
                String colorCode = healthChange > 0 ? "§a" : "§c";
                int amount = Math.abs(healthChange);

                group.addWidget(
                        new LabelWidget(xOffset + 3, yOffset.addAndGet(10),
                                I18n.get(langKey, colorCode + amount)));
            }
        }
    }

    private static class SerializerShield implements IContentSerializer<ShieldTypes> {

        public static SerializerShield INSTANCE = new SerializerShield();

        public static Codec<ShieldTypes> CODEC = Codec.INT.xmap(ShieldTypes::getShieldFromKey, ShieldTypes::getKey);

        @Override
        public ShieldTypes of(Object o) {
            if (!(o instanceof ShieldTypes shieldType)) {
                return null;
            }
            return shieldType;
        }

        @Override
        public ShieldTypes defaultValue() {
            return ShieldTypes.INACTIVE;
        }

        @Override
        public Class<ShieldTypes> contentClass() {
            return ShieldTypes.class;
        }

        @Override
        public Codec<ShieldTypes> codec() {
            return CODEC;
        }
    }
}
