package net.phoenix.core.axiom.pipe;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class AxiomMultiHandlerCapability {

    public static final Capability<IAxiomMultiHandler> MULTI_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private AxiomMultiHandlerCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IAxiomMultiHandler.class);
    }
}
