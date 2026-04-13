package net.phoenix.core.mixin.accessor;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.common.data.GTOres;

import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GTOres.class)
public interface GTOresAccessor {

    @Accessor("toReRegister")
    static Map<ResourceLocation, GTOreDefinition> getToReRegister() {
        throw new UnsupportedOperationException();
    }
}
