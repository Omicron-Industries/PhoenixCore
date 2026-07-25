package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.WireProperties;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PhoenixCapabilityBridge {

    public static boolean validateConnection(Level level, Player player, BlockPos posA, BlockPos posB) {
        var beA = level.getBlockEntity(posA);
        var beB = level.getBlockEntity(posB);

        if (beA instanceof IPipeNode<?, ?> pipeA && beB instanceof IPipeNode<?, ?> pipeB) {

            if (pipeA.getNodeData() instanceof WireProperties propsA &&
                    pipeB.getNodeData() instanceof WireProperties propsB) {

                long vA = propsA.getVoltage();
                long vB = propsB.getVoltage();

                if (vA != vB) {
                    player.displayClientMessage(Component.literal(
                            String.format("§cPhoenix Warning: Voltage Mismatch! (%dV vs %dV)", vA, vB)), true);
                    return false;
                }
            }
        }

        IEnergyContainer energyA = GTCapabilityHelper.getEnergyContainer(level, posA, null);
        IEnergyContainer energyB = GTCapabilityHelper.getEnergyContainer(level, posB, null);

        if (energyA != null && energyB != null) {

            if (Math.abs(energyA.getInputVoltage() - energyB.getOutputVoltage()) > energyA.getInputVoltage() * 4) {
                player.displayClientMessage(Component.literal("§4Phoenix: High Risk! Voltage Tier gap too large."),
                        true);
            }
        }

        return true;
    }
}
