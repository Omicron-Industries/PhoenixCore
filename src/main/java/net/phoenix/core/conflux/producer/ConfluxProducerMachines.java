package net.phoenix.core.conflux.producer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.phoenix.core.conflux.ConfluxDataType;

import java.util.EnumMap;
import java.util.Map;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/**
 * Registers one tiered GTCEu machine per {@link ConfluxDataType}.
 *
 * Naming: {@code lv_material_data_producer}, {@code mv_material_data_producer}, …
 * Tiers: LV → UV (1–9), same as standard GT machines.
 * ARCANE producers are only added to creative tabs when Ars Nouveau is loaded.
 */
public final class ConfluxProducerMachines {

    /** [ConfluxDataType][tier] — index by {@link GTValues} tier constants. */
    public static final Map<ConfluxDataType, MachineDefinition[]> PRODUCERS = new EnumMap<>(ConfluxDataType.class);

    private static final int[] TIERS = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
            GTValues.LuV, GTValues.ZPM, GTValues.UV
    };

    static {
        for (ConfluxDataType type : ConfluxDataType.values()) {
            MachineDefinition[] defs = new MachineDefinition[GTValues.TIER_COUNT];
            for (int tier : TIERS) {
                String id = GTValues.VN[tier].toLowerCase() + "_" + type.id() + "_data_producer";
                String lang = GTValues.VN[tier] + " " + type.displayName + " Data Producer";

                defs[tier] = REGISTRATE
                        .machine(id, holder -> new ConfluxProducerMachine(holder, tier, type))
                        .langValue(lang)
                        .tier(tier)
                        .rotationState(RotationState.NON_Y_AXIS)
                        .tooltips(
                                net.minecraft.network.chat.Component.literal(
                                        "Produces §b" + type.displayName + "§r research data."),
                                net.minecraft.network.chat.Component.literal(
                                        "Output: §e" + (16L * tier) + "§r units/t | Fuel: §e1 item / 10s"))
                        .register();
            }
            PRODUCERS.put(type, defs);
        }
    }

    public static void init() { /* triggers static initializer */ }

    private ConfluxProducerMachines() {}
}
