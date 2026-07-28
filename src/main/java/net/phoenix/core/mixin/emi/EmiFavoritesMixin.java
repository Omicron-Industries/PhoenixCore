package net.phoenix.core.mixin.emi;

import net.phoenix.core.client.emi.PhoenixFavoriteSets;

import dev.emi.emi.runtime.EmiFavorites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiFavorites.class, remap = false)
public abstract class EmiFavoritesMixin {

    @Inject(method = "addFavorite(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V",
            at = @At("TAIL"))
    private static void phoenixcore$onAddFavorite(CallbackInfo ci) {
        PhoenixFavoriteSets.onFavoritesMutated();
    }

    @Inject(method = "addFavoriteAt", at = @At("TAIL"))
    private static void phoenixcore$onAddFavoriteAt(CallbackInfo ci) {
        PhoenixFavoriteSets.onFavoritesMutated();
    }

    @Inject(method = "removeFavorite", at = @At("TAIL"))
    private static void phoenixcore$onRemoveFavorite(CallbackInfoReturnable<Boolean> cir) {
        PhoenixFavoriteSets.onFavoritesMutated();
    }
}
