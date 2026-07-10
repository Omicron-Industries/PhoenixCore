package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.common.machine.multiblock.Shield.ShieldTypes;

import com.mojang.serialization.Codec;

public class ShieldRecipeCapability extends RecipeCapability<ShieldTypes> {

    public static ShieldRecipeCapability CAP = new ShieldRecipeCapability();

    protected ShieldRecipeCapability() {
        super(new ResourceLocation("phoenixcore", "shield"), 0xFF00FFFF, false, 11, SerializerShield.INSTANCE);
    }

    @Override
    public ShieldTypes copyInner(ShieldTypes content) {
        return content;
    }

    @Override
    public boolean isRecipeSearchFilter() {
        return true;
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
