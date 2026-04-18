package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Service to handle scanning and consuming GregTech pipes/cables from the player's inventory.
 */
public class PhoenixInventoryService {

    /**
     * Consumes 1 pipe from the inventory that matches the reference stack.
     * Returns true if successful (or if player is in creative), false if empty.
     */
    public static boolean consumePipe(Player player, ItemStack reference) {
        if (player.getAbilities().instabuild) return true;

        Optional<ItemStack> stackOpt = findMatchingPipe(player, reference);

        if (stackOpt.isPresent()) {
            ItemStack foundStack = stackOpt.get();
            if (!foundStack.isEmpty()) {
                foundStack.shrink(1);
                // On some versions, you may need to explicitly tell the inventory it changed
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Searches for a pipe/cable stack that matches the reference stack.
     * Priority: Off-hand -> Hotbar -> Main Inventory.
     */
    public static Optional<ItemStack> findMatchingPipe(Player player, ItemStack reference) {
        if (reference.isEmpty()) return Optional.empty();

        Inventory inv = player.getInventory();

        // 1. Check Off-hand (Primary placement source)
        if (isSamePipe(reference, player.getOffhandItem())) {
            return Optional.of(player.getOffhandItem());
        }

        // 2. Scan Hotbar and Main Inventory
        // We use getContainerSize to include all slots
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isSamePipe(reference, stack)) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }

    /**
     * Helper to determine if two stacks are the same type of GT pipe/cable.
     * Uses item equality which, for GTCEu pipes, covers Material and Type.
     */
    public static boolean isSamePipe(ItemStack reference, ItemStack candidate) {
        if (candidate.isEmpty() || reference.isEmpty()) {
            return false;
        }

        // Ensure both are actually GT Pipe items
        if (!(candidate.getItem() instanceof PipeBlockItem) ||
                !(reference.getItem() instanceof PipeBlockItem)) {
            return false;
        }

        // GTCEu uses unique Item instances for every Material/PipeType combination.
        // Therefore, checking if the Items are the same is sufficient.
        return reference.getItem() == candidate.getItem();
    }
}