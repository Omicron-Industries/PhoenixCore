package net.phoenix.core.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import java.util.Set;
import java.util.function.Supplier;

public class UpdateWingSettingsPacket {

    private static final Set<String> VALID_MODES = Set.of(
            "basic",
            "powered",
            "creative",
            "creative+wings");

    private final String flightMode;
    private final int flightSpeed;
    private final int flightDrift;

    public UpdateWingSettingsPacket(String flightMode, int flightSpeed, int flightDrift) {
        this.flightMode = flightMode;
        this.flightSpeed = flightSpeed;
        this.flightDrift = flightDrift;
    }

    public UpdateWingSettingsPacket(FriendlyByteBuf buf) {
        this.flightMode = buf.readUtf();
        this.flightSpeed = buf.readInt();
        this.flightDrift = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(flightMode);
        buf.writeInt(flightSpeed);
        buf.writeInt(flightDrift);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof PhoenixArmorItem)) return;

            String mode = VALID_MODES.contains(flightMode) ? flightMode : "basic";
            int speed = Math.max(0, Math.min(10, flightSpeed));
            int drift = Math.max(0, Math.min(10, flightDrift));

            CompoundTag tag = chest.getOrCreateTag();
            tag.putString("FlightMode", mode);
            tag.putInt("FlightSpeed", speed);
            tag.putInt("FlightDrift", drift);

            player.inventoryMenu.sendAllDataToRemote();
        });
        ctx.get().setPacketHandled(true);
    }
}
