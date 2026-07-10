package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.network.chat.Component;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.ae2.METagInputBusPartMachine;
import net.phoenix.core.integration.ae2.METagInputHatchPartMachine;

import static com.gregtechceu.gtceu.api.GTValues.UHV;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixAEMachines {

    public static MachineDefinition ME_TAG_INPUT_BUS = null;
    public static MachineDefinition ME_TAG_INPUT_HATCH = null;

    static {
        if (PhoenixConfigs.INSTANCE.features.tagInputsEnabled && GTCEu.Mods.isAE2Loaded() || GTCEu.isDataGen()) {

            ME_TAG_INPUT_BUS = REGISTRATE
                    .machine("me_tag_input_bus", (info) -> new METagInputBusPartMachine(info, IO.IN))
                    .tier(UHV)
                    .rotationState(RotationState.ALL)
                    .abilities(
                            PartAbility.IMPORT_ITEMS,
                            PartAbility.EXPORT_ITEMS)
                    .colorOverlayTieredHullModel(PhoenixCore.id("block/machine/ae2/me_tag_input_bus"))
                    .langValue("ME Tag Input Bus")
                    .tooltips(
                            Component.literal("§6Tag‑based AE2 Item Import"),
                            Component.literal("§7Matches items via tag expressions"),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();

            ME_TAG_INPUT_HATCH = REGISTRATE
                    .machine("me_tag_input_hatch", METagInputHatchPartMachine::new)
                    .tier(UHV)
                    .rotationState(RotationState.ALL)
                    .abilities(
                            PartAbility.IMPORT_FLUIDS,
                            PartAbility.EXPORT_FLUIDS)
                    .colorOverlayTieredHullModel(PhoenixCore.id("block/machine/ae2/me_tag_input_hatch"))
                    .langValue("ME Tag Input Hatch")
                    .tooltips(
                            Component.literal("§6Tag‑based AE2 Fluid Import"),
                            Component.literal("§7Matches fluids via tag expressions"),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();
        }
    }

    public static void init() {}
}
