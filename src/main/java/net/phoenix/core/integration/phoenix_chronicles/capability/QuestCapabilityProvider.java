package net.phoenix.core.integration.phoenix_chronicles.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerQuestData> PLAYER_QUESTS = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = new ResourceLocation(PhoenixCore.MOD_ID, "player_quests");

    private final PlayerQuestData instance = new PlayerQuestData();
    private final LazyOptional<PlayerQuestData> optional = LazyOptional.of(() -> instance);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_QUESTS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    // Attach capability instantly whenever a player spawns into the world
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new QuestCapabilityProvider());
        }
    }

    // Register the capability to Forge on startup (call this in your Mod constructor or CommonSetup)
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerQuestData.class);
    }
}
