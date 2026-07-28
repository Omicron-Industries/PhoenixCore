package net.phoenix.core.conflux.producer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.ConfluxDataType;

import java.util.EnumMap;
import java.util.Map;

import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createOverlayCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class ConfluxProducerMachines {

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

                        .model(createOverlayCasingMachineModel(
                                PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                                com.gregtechceu.gtceu.GTCEu.id("block/overlay/2_layer/front_emissive")))
                        .register();
            }
            PRODUCERS.put(type, defs);
        }
    }

    public static void init() {}

    private ConfluxProducerMachines() {}
}
