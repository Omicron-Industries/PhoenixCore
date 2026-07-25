package net.phoenix.core.axiom.research;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerResearchCapability {

    public static final Capability<PlayerResearchData> RESEARCH = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID = new ResourceLocation(PhoenixCore.MOD_ID, "axiom_research");

    private PlayerResearchCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerResearchData.class);
    }

    public static PlayerResearchData get(Player player) {
        return player.getCapability(RESEARCH).orElseThrow(
                () -> new IllegalStateException(
                        "Player missing AxiomResearch capability: " + player.getName().getString()));
    }

    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        event.addCapability(ID, new Provider());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().isDeadOrDying() || event.isWasDeath()) {
            PlayerResearchData original = get(event.getOriginal());
            PlayerResearchData clone = get(event.getEntity());
            clone.copyFrom(original);
        }
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerResearchData data = new PlayerResearchData();
        private final LazyOptional<PlayerResearchData> opt = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == RESEARCH ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }
    }
}
