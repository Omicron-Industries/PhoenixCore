package net.phoenix.core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import net.phoenix.core.network.packet.*;

import java.util.Optional;

@SuppressWarnings("removal")
public class PhoenixNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("phoenixcore", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        
        CHANNEL.registerMessage(id++,
                SelectColorPacket.class,
                SelectColorPacket::encode,
                SelectColorPacket::decode,
                SelectColorPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2STeslaDischargePacket.class,
                C2STeslaDischargePacket::encode,
                C2STeslaDischargePacket::new,
                C2STeslaDischargePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                UpdateWingSettingsPacket.class,
                UpdateWingSettingsPacket::encode,
                UpdateWingSettingsPacket::new,
                UpdateWingSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                PacketPhoenixModeSync.class,
                PacketPhoenixModeSync::encode,
                PacketPhoenixModeSync::decode,
                PacketPhoenixModeSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                CPacketChangeManipulatorMode.class,
                CPacketChangeManipulatorMode::encode,
                CPacketChangeManipulatorMode::new,
                CPacketChangeManipulatorMode::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                CPacketManipulatorAction.class,
                CPacketManipulatorAction::encode,
                CPacketManipulatorAction::new,
                CPacketManipulatorAction::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                PacketRecipeBuilderGenerate.class,
                PacketRecipeBuilderGenerate::encode,
                PacketRecipeBuilderGenerate::decode,
                PacketRecipeBuilderGenerate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                SelectChromaticCodePacket.class,
                SelectChromaticCodePacket::encode,
                SelectChromaticCodePacket::decode,
                SelectChromaticCodePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SSelectSoundPacket.class,
                C2SSelectSoundPacket::encode,
                C2SSelectSoundPacket::new,
                C2SSelectSoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SSoundMetadataPacket.class,
                C2SSoundMetadataPacket::encode,
                C2SSoundMetadataPacket::new,
                C2SSoundMetadataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                S2CPlaySoundPacket.class,
                S2CPlaySoundPacket::encode,
                S2CPlaySoundPacket::new,
                S2CPlaySoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                S2CPlayStreamPacket.class,
                S2CPlayStreamPacket::encode,
                S2CPlayStreamPacket::new,
                S2CPlayStreamPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                C2SToggleTeslaModePacket.class,
                C2SToggleTeslaModePacket::encode,
                C2SToggleTeslaModePacket::new, 
                C2SToggleTeslaModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

    }
}
