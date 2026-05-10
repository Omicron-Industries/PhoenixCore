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
            GLFW.GLFW_KEY_V,
            "key.categories.phoenixcore");

    public static final KeyMapping OPEN_RECIPE_BUILDER = new KeyMapping(
            "key.phoenixcore.recipe_builder",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_8,
            "key.categories.phoenixcore");

    public static final KeyMapping SPRAY_CAN_MENU = new KeyMapping(
            "key.phoenixcore.spray_can_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.phoenixcore");

    /** Hold W (or rebind) while hovering a multiblock controller item to open its Ponder. */
    public static final KeyMapping OPEN_PONDER = new KeyMapping(
            "key.phoenixcore.open_ponder",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W,
            "key.categories.phoenixcore");

    public static final KeyMapping OPEN_PHANTASIA_MENU = new KeyMapping(
            "key.phoenixcore.phantasia_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.phoenixcore");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WING_GUI);
        event.register(TESLA_MODE);
        event.register(TESLA_DISCHARGE);
        event.register(MANIPULATOR_MENU);
        event.register(OPEN_RECIPE_BUILDER);
        event.register(SPRAY_CAN_MENU);
        event.register(OPEN_PONDER);
        event.register(OPEN_PHANTASIA_MENU);
    }
}
