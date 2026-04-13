package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public interface MEPatternBufferInvoker {

    @Invoker("setCustomName")
    void phoenix$invokeSetCustomName(String name);
}
