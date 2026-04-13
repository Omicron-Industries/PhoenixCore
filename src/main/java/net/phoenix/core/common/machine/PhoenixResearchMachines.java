package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.network.chat.Component;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.part.hpca.BasicPhoenixComputationPartMachine;
import net.phoenix.core.common.machine.multiblock.part.hpca.BasicPhoenixCoolerPartMachine;
import net.phoenix.core.common.machine.multiblock.part.hpca.PhoenixComputationPartMachine;
import net.phoenix.core.common.machine.multiblock.part.hpca.PhoenixCoolerPartMachine;

import java.util.function.Function;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.machines.GTResearchMachines.OVERHEAT_TOOLTIPS;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.*;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;
import static net.phoenix.core.configs.PhoenixConfigs.*;

public class PhoenixResearchMachines {

    public static MachineDefinition PHOENIX_COMPUTATION_COMPONENT = null;
    public static MachineDefinition ADVANCED_PHOENIX_COMPUTATION_COMPONENT = null;
    public static MachineDefinition PHOENIX_COOLER_COMPONENT = null;
    public static MachineDefinition ACTIVE_PHOENIX_COOLER_COMPONENT = null;

    static {
        if (INSTANCE.features.HPCAComponetsEnabled) {
            PHOENIX_COMPUTATION_COMPONENT = registerComputationHPCAPart(
                    "phoenix_computation_component", "Phoenix Computation Component",
                    BasicPhoenixComputationPartMachine::new, "reinforced_computation", false)
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.upkeep_eut",
                                    VA[INSTANCE.features.basicPCUEutUpkeep]),
                            Component.translatable("gtceu.machine.hpca.component_general.max_eut",
                                    VA[INSTANCE.features.basicPCUMaxEUt]),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cwut",
                                    INSTANCE.features.BasicPCUStrength),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cooling",
                                    INSTANCE.features.BasicPCUCoolantUsed),
                            Component.translatable("gtceu.part_sharing.disabled"))
                    .tooltipBuilder(OVERHEAT_TOOLTIPS)
                    .register();
            ADVANCED_PHOENIX_COMPUTATION_COMPONENT = registerComputationHPCAPart(
                    "advanced_phoenix_computation_component", "Advanced Phoenix Computation Component",
                    PhoenixComputationPartMachine::new, "advanced_phoenix_computation_component", true)
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.upkeep_eut",
                                    VA[INSTANCE.features.PCUEutUpkeep]),
                            Component.translatable("gtceu.machine.hpca.component_general.max_eut",
                                    VA[INSTANCE.features.PCUMaxEUt]),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cwut",
                                    INSTANCE.features.PCUStrength),
                            Component.translatable("gtceu.machine.hpca.component_type.computation_cooling",
                                    INSTANCE.features.PCUCoolantUsed),
                            Component.translatable("gtceu.part_sharing.disabled"))
                    .tooltipBuilder(OVERHEAT_TOOLTIPS)
                    .register();

            PHOENIX_COOLER_COMPONENT = registerCoolingHPCAPart(
                    "phoenix_heat_sink_component", "Phoenix Heat Sink Component",
                    holder -> new BasicPhoenixCoolerPartMachine(holder, false),
                    "advanced_heat_sink", false)
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.upkeep_eut",
                                    INSTANCE.features.HeatSinkEutUpkeep),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_passive"),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_cooling",
                                    INSTANCE.features.HeatSinkStrength),
                            Component.translatable("gtceu.part_sharing.disabled"))
                    .register();

            ACTIVE_PHOENIX_COOLER_COMPONENT = registerCoolingHPCAPart(
                    "active_phoenix_cooling_component", "Active Phoenix Cooling Component",

                    holder -> new PhoenixCoolerPartMachine(holder, true),
                    "advanced_active_cooler", true)
                    .tooltips(
                            Component.translatable("gtceu.machine.hpca.component_general.upkeep_eut",
                                    INSTANCE.features.ActiveCoolerEutUpkeep),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_active"),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_active_coolant",
                                    INSTANCE.features.ActiveCoolerCoolantUse,
                                    GTMaterials.get(INSTANCE.features.ActiveCoolerCoolantBase).getLocalizedName()),
                            Component.translatable("gtceu.tooltip.uses_custom_coolant",
                                    GTMaterials.get(INSTANCE.features.ActiveCoolerCoolantBase).getLocalizedName()),
                            Component.translatable("gtceu.machine.hpca.component_type.cooler_cooling",
                                    INSTANCE.features.ActiveCoolerStrength),
                            Component.translatable("gtceu.part_sharing.disabled"))
                    .register();
        }
    }

    private static MachineBuilder<MachineDefinition, ?> registerCoolingHPCAPart(String name, String displayName,
                                                                                Function<IMachineBlockEntity, MetaMachine> constructor,
                                                                                String texture, boolean isAdvanced) {
        return REGISTRATE.machine(name, constructor)
                .langValue(displayName)
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.HPCA_COMPONENT)
                .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                .modelProperty(GTMachineModelProperties.IS_HPCA_PART_DAMAGED, false)
                .modelProperty(GTMachineModelProperties.IS_ACTIVE, false)
                .model(createHPCAPartModel(isAdvanced,
                        PhoenixCore.id("block/overlay/machine/hpca/cooling/" + texture),
                        PhoenixCore.id("block/overlay/machine/hpca/damaged" + (isAdvanced ? "_advanced" : ""))));
    }

    private static MachineBuilder<MachineDefinition, ?> registerComputationHPCAPart(String name, String displayName,
                                                                                    Function<IMachineBlockEntity, MetaMachine> constructor,
                                                                                    String texture,
                                                                                    boolean isAdvanced) {
        return REGISTRATE.machine(name, constructor)
                .langValue(displayName)
                .rotationState(RotationState.ALL)
                .abilities(PartAbility.HPCA_COMPONENT)
                .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                .modelProperty(GTMachineModelProperties.IS_HPCA_PART_DAMAGED, false)
                .modelProperty(GTMachineModelProperties.IS_ACTIVE, false)
                .model(createHPCAPartModel(isAdvanced,
                        PhoenixCore.id("block/machine/part/hpca/computation/" + texture),
                        PhoenixCore
                                .id("block/machine/part/hpca/computation/damaged" + (isAdvanced ? "_advanced" : ""))));
    }

    public static void init() {}
}
