package net.phoenix.core.conflux.pipe;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class ConfluxMultiHandlerCapability {

    public static final Capability<IConfluxMultiHandler> MULTI_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private ConfluxMultiHandlerCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IConfluxMultiHandler.class);
    }
}
