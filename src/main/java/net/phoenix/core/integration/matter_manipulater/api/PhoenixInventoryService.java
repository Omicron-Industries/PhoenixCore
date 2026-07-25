package net.phoenix.core.integration.matter_manipulater.api;

import com.gregtechceu.gtceu.api.item.PipeBlockItem;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class PhoenixInventoryService {

    public static boolean consumePipe(Player player, ItemStack reference) {
        if (player.getAbilities().instabuild) return true;

        Optional<ItemStack> stackOpt = findMatchingPipe(player, reference);

        if (stackOpt.isPresent()) {
            ItemStack foundStack = stackOpt.get();
            if (!foundStack.isEmpty()) {
                foundStack.shrink(1);

                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    public static Optional<ItemStack> findMatchingPipe(Player player, ItemStack reference) {
        if (reference.isEmpty()) return Optional.empty();

        Inventory inv = player.getInventory();

        if (isSamePipe(reference, player.getOffhandItem())) {
            return Optional.of(player.getOffhandItem());
        }

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isSamePipe(reference, stack)) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }

    public static boolean isSamePipe(ItemStack reference, ItemStack candidate) {
        if (candidate.isEmpty() || reference.isEmpty()) {
            return false;
        }

        if (!(candidate.getItem() instanceof PipeBlockItem) ||
                !(reference.getItem() instanceof PipeBlockItem)) {
            return false;
        }

        return reference.getItem() == candidate.getItem();
    }
}
