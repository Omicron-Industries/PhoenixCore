package net.phoenix.core.integration.jade;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.world.level.block.Block;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.jade.provider.*;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class PhoenixJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(common) called");

        registration.registerBlockDataProvider(new SourceMachineProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new SourceTankJadeProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new TeslaNetworkProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new HighPressurePlasmaArcFurnaceProvider(),
                MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new FissionMachineProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new ThreadedRecipeOutputProvider(), MetaMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(client) called");

        registration.registerBlockComponent(new SourceMachineProvider(), Block.class);
        registration.registerBlockComponent(new SourceTankJadeProvider(), Block.class);
        registration.registerBlockComponent(new TeslaNetworkProvider(), Block.class);
        registration.registerBlockComponent(new HighPressurePlasmaArcFurnaceProvider(), Block.class);
        registration.registerBlockComponent(new FissionMachineProvider(), Block.class);
        registration.registerBlockComponent(new ThreadedRecipeOutputProvider(), Block.class);

        // Phantasia — shows the hold-to-phantasize hint in the Jade HUD
        registration.registerBlockComponent(PhantasiaJadeProvider.INSTANCE, Block.class);
    }
}
