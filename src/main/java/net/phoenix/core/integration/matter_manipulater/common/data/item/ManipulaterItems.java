package net.phoenix.core.integration.matter_manipulater.common.data.item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class ManipulaterItems {

    // 1. Create a dedicated Registrate instance for your items if you haven't yet
    // Or use your coremod's existing one.
    public static final ItemEntry<PhoenixManipulatorItem> PHOENIX_MANIPULATOR = REGISTRATE
            .item("phoenix_manipulator", PhoenixManipulatorItem::new)
            .lang("Phoenix Matter Manipulator")
            .onRegister(item -> {})
            .register();

    /**
     * Call this in your Mod Constructor or Common Setup
     */
    public static void init() {
        // This triggers the static initializers above
    }
}
