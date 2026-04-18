package net.phoenix.core.client.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PhoenixKeybinds {

    public static final KeyMapping OPEN_WING_GUI = new KeyMapping(
            "key.phoenixcore.wing_flight_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            "key.categories.phoenixcore");
    public static final KeyMapping TESLA_MODE = new KeyMapping(
            "key.phoenixcore.tesla_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.phoenixcore");
    public static final KeyMapping TESLA_DISCHARGE = new KeyMapping(
            "key.phoenixcore.tesla_discharge",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_7,
            "key.categories.phoenixcore");

    public static final KeyMapping MANIPULATOR_MENU = new KeyMapping(
            "key.phoenixcore.manipulator_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // Defaulting to 'V', common for tool menus
            "key.categories.phoenixcore");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WING_GUI);
        event.register(TESLA_MODE);
        event.register(TESLA_DISCHARGE);
        event.register(MANIPULATOR_MENU);
    }
}
