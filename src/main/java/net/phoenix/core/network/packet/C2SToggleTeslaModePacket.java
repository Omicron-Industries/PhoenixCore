package net.phoenix.core.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import java.util.function.Supplier;

public class C2SToggleTeslaModePacket {

    public C2SToggleTeslaModePacket() {}

    public C2SToggleTeslaModePacket(FriendlyByteBuf buf) {}

    public static void encode(C2SToggleTeslaModePacket packet, FriendlyByteBuf buf) {}

    public static void handle(C2SToggleTeslaModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);

            if (!chestplate.isEmpty() && chestplate.getItem() instanceof PhoenixArmorItem) {
                CompoundTag data = chestplate.getOrCreateTag();

                long currentTime = player.level().getGameTime();
                long lastToggle = data.getLong("lastToggleTime");
                if (currentTime - lastToggle < 10) return;

                boolean nextMode = !data.getBoolean("teslaMode");
                data.putBoolean("teslaMode", nextMode);
                data.putLong("lastToggleTime", currentTime);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        nextMode ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 1.0F, 2.0F);
            }
        });
        context.setPacketHandled(true);
    }
}
