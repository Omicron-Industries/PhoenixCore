package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public interface MEPatternBufferAccessor {

    @Accessor("customName")
    String getCustomName();
}
