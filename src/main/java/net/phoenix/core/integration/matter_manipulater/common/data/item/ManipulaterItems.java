package net.phoenix.core.integration.matter_manipulater.common.data.item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class ManipulaterItems {

    public static final ItemEntry<PhoenixManipulatorItem> PHOENIX_MANIPULATOR = REGISTRATE
            .item("phoenix_manipulator", PhoenixManipulatorItem::new)
            .lang("Phoenix Matter Manipulator")
            .onRegister(item -> {})
            .register();

    public static void init() {}
}
