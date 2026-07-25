package net.phoenix.core.integration.ars_nouveau.api.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;

public interface ISourceProviderCapability {

    Capability<ISourceProviderCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    ISourceTile getSource();
}
