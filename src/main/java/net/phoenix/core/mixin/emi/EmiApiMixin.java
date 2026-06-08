package net.phoenix.core.mixin.emi;

import com.gregtechceu.gtceu.api.item.ComponentItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = EmiApi.class, remap = false)
public abstract class EmiApiMixin {

    @ModifyVariable(
                    method = "displayUses",
                    at = @At(
                             value = "INVOKE",
                             target = "Ldev/emi/emi/api/stack/EmiIngredient;isEmpty()Z"),
                    argsOnly = true)
    private static EmiIngredient modifyDisplayUses(EmiIngredient stack) {
        return stack.isEmpty() ? stack : gto$getBucketFluid(stack);
    }

    @ModifyVariable(
                    method = "displayRecipes",
                    at = @At(
                             value = "INVOKE",
                             target = "Ljava/util/List;size()I"),
                    argsOnly = true)
    private static EmiIngredient modifyDisplayRecipes(EmiIngredient stack) {
        return stack.getEmiStacks().size() != 1 ? stack : gto$getBucketFluid(stack);
    }

    @Unique
    private static EmiIngredient gto$getBucketFluid(EmiIngredient stack) {
        if (stack instanceof EmiStack emiStack) {
            Fluid fluid = Fluids.EMPTY;
            if (emiStack.getKey() instanceof BucketItem bucketItem) {
                fluid = bucketItem.getFluid();
            } else if (emiStack.getKey() instanceof ComponentItem && emiStack.hasNbt()) {
                CompoundTag nbt = emiStack.getNbt();
                if (nbt.contains("Fluid", Tag.TAG_COMPOUND)) {
                    var fluidTag = nbt.getCompound("Fluid");
                    var fluidName = fluidTag.getString("FluidName");
                    fluid = gto$getFluid(fluidName);
                }
            }

            if (fluid == Fluids.EMPTY || gto$isLiquidConcrete(fluid)) {
                return stack;
            }

            return EmiStack.of(fluid);
        }
        return stack;
    }

    @Unique
    private static Fluid gto$getFluid(String location) {
        var fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(location));
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Unique
    private static boolean gto$isLiquidConcrete(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        if (id == null) return false;

        return id.getNamespace().equals("gtceu") && id.getPath().equals("concrete");
    }
}
