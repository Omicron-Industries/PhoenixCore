package net.phoenix.core.integration.recipe_helper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;

public class RecipeBuilderMenu extends AbstractContainerMenu {

    public static final int GUI_W = 338;
    public static final int GUI_H = 264;

    public static final int INV_X = 88;

    public static final int INV_Y = 176;

    public static final int HOTBAR_OFFSET = 58;

    public RecipeBuilderMenu(int windowId, Inventory playerInv) {
        super(PhoenixCore.RECIPE_BUILDER_MENU.get(), windowId);

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        INV_X + col * 18,
                        INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    playerInv,
                    col,
                    INV_X + col * 18,
                    INV_Y + HOTBAR_OFFSET));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 27) {
            if (!this.moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return original;
    }
}
