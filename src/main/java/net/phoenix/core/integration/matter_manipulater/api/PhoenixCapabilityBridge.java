package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.WireProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PhoenixCapabilityBridge {

    /**
     * Checks if two nodes are safe to connect based on WireProperties.
     */
    public static boolean validateConnection(Level level, Player player, BlockPos posA, BlockPos posB) {
        var beA = level.getBlockEntity(posA);
        var beB = level.getBlockEntity(posB);

        // 1. Validation for Cables (using WireProperties from the nodes)
        if (beA instanceof IPipeNode<?, ?> pipeA && beB instanceof IPipeNode<?, ?> pipeB) {
            // Check if both nodes contain WireProperties (Energy-related data)
            if (pipeA.getNodeData() instanceof WireProperties propsA &&
                    pipeB.getNodeData() instanceof WireProperties propsB) {

                long vA = propsA.getVoltage();
                long vB = propsB.getVoltage();

                // Check for Voltage Mismatch
                if (vA != vB) {
                    player.displayClientMessage(Component.literal(
                            String.format("§cPhoenix Warning: Voltage Mismatch! (%dV vs %dV)", vA, vB)), true);
                    return false;
                }
            }
        }

        // 2. Machine Energy Container Validation
        IEnergyContainer energyA = GTCapabilityHelper.getEnergyContainer(level, posA, null);
        IEnergyContainer energyB = GTCapabilityHelper.getEnergyContainer(level, posB, null);

        if (energyA != null && energyB != null) {
            // Basic tier-checking safety (machine compatibility)
            if (Math.abs(energyA.getInputVoltage() - energyB.getOutputVoltage()) > energyA.getInputVoltage() * 4) {
                player.displayClientMessage(Component.literal("§4Phoenix: High Risk! Voltage Tier gap too large."), true);
            }
        }

        return true;
    }
}