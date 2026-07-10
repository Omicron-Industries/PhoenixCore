package net.phoenix.core.conflux.pipe;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class ConfluxDataCapability {

    public static final Capability<IConfluxDataHandler> DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private ConfluxDataCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IConfluxDataHandler.class);
    }
}
