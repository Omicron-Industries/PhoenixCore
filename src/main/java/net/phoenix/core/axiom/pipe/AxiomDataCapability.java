package net.phoenix.core.axiom.pipe;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class AxiomDataCapability {

    public static final Capability<IAxiomDataHandler> DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private AxiomDataCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IAxiomDataHandler.class);
    }
}
