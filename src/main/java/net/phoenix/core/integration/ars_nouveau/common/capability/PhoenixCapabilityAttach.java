package net.phoenix.core.integration.ars_nouveau.common.capability;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhoenixCapabilityAttach {

    private static final ResourceLocation KEY = new ResourceLocation(PhoenixCore.MOD_ID, "source_provider");

    private PhoenixCapabilityAttach() {}

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();

        if (!(be instanceof MetaMachine machine)) return;

        if (!(machine instanceof SourceHatchPartMachine hatch)) return;

        PhoenixCore.LOGGER.info("[SourceHatchCap] Attaching to {} machine={}",
                be.getBlockPos(), machine.getClass().getName());

        LazyOptional<ISourceProviderCapability> opt = LazyOptional.of(() -> hatch);

        event.addCapability(KEY, new ICapabilityProvider() {

            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ISourceProviderCapability.CAPABILITY ? opt.cast() : LazyOptional.empty();
            }
        });

        event.addListener(opt::invalidate);
    }
}
