package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.common.data.item.PhoenixTechSuite;

import java.util.function.Supplier;

public class C2STeslaDischargePacket {

    public C2STeslaDischargePacket() {}

    public C2STeslaDischargePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {
        // No extra payloads required; server queries the inventory safely
    }

    public static void handle(C2STeslaDischargePacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

            // 1. Maintain alignment with your clean client-side PhoenixArmorItem check
            if (!boots.isEmpty() && boots.getItem() instanceof PhoenixArmorItem) {
                CompoundTag data = boots.getOrCreateTag();

                // 2. Validate server-side cooldown state to maintain packet security
                if (data.getInt("dischargeCooldown") == 0) {

                    // 3. Since boots.getItem() is your custom GregTech armor definition,
                    // we can double-cast via (Object) to bypass standard class hierarchy limits safely.
                    PhoenixTechSuite suiteController = (PhoenixTechSuite) (Object) boots.getItem();
                    IElectricItem electricItemCapability = (IElectricItem) (Object) boots.getItem();

                    // 4. Fire the verified method on the server thread!
                    suiteController.doTeslaDischarge((ServerLevel) player.level(), player, electricItemCapability);

                    // 5. Establish the cooldown (5 seconds / 100 ticks)
                    data.putInt("dischargeCooldown", 100);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
