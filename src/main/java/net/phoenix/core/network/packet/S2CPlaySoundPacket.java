package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CPlaySoundPacket {

    private final BlockPos pos;
    private final ResourceLocation soundLocation;
    private final float volume;
    private final float pitch;
    private final float range;

    public S2CPlaySoundPacket(BlockPos pos, ResourceLocation soundLocation, float volume, float pitch, float range) {
        this.pos = pos;
        this.soundLocation = soundLocation;
        this.volume = volume;
        this.pitch = pitch;
        this.range = range;
    }

    public S2CPlaySoundPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.soundLocation = buf.readResourceLocation();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.range = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(soundLocation);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(range);
    }

    public static void handle(S2CPlaySoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            net.phoenix.core.network.client.ClientSoundHandler.playSound(
                    msg.pos, msg.soundLocation, msg.volume, msg.pitch, msg.range);
        });
        ctx.get().setPacketHandled(true);
    }
}
