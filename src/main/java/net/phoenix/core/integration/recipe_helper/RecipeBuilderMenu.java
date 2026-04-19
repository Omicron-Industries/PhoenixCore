package net.phoenix.core.integration.recipe_helper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;

/**
 * Container/menu for the Recipe Builder screen.
 *
 * ─── Inventory slot binding ───────────────────────────────────────────────────
 *
 * AbstractContainerScreen places items and handles clicks at:
 * screenX = leftPos + slot.x
 * screenY = topPos + slot.y
 *
 * So slot.x / slot.y must be GUI-RELATIVE (offsets from the panel top-left),
 * NOT absolute screen coordinates.
 *
 * RecipeBuilderScreen#renderBg draws boxes at:
 * (x + INV_X + col*18, y + INV_Y + row*18) where x=leftPos, y=topPos
 *
 * GUI-relative draw offset = (INV_X + col*18, INV_Y + row*18)
 * → that is exactly what addSlot() registers below.
 *
 * Sharing the constants as public statics guarantees both sides are always in sync.
 *
 * ─── Layout intent ────────────────────────────────────────────────────────────
 * Content area ≈ rows 30–148
 * Action buttons row ≈ row 152
 * Visual divider at row 168
 * Inventory (3 rows) starts at INV_Y = 176
 * Hotbar at INV_Y + HOTBAR_OFFSET = 176 + 58 = 234
 * GUI_H = 264 (hotbar bottom 252 + 12 px padding)
 */
public class RecipeBuilderMenu extends AbstractContainerMenu {

    // ── Shared layout constants (used by RecipeBuilderScreen too) ─────────────

    public static final int GUI_W = 338;
    public static final int GUI_H = 264;

    /** GUI-relative X of the leftmost inventory column. Centers 162 px in 338 px. */
    public static final int INV_X = 88;

    /** GUI-relative Y of the first inventory row. Well below buttons. */
    public static final int INV_Y = 176;

    /** Pixel gap from INV_Y to the hotbar row (3 rows × 18 + 4 gap). */
    public static final int HOTBAR_OFFSET = 58;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RecipeBuilderMenu(int windowId, Inventory playerInv) {
        super(PhoenixCore.RECIPE_BUILDER_MENU.get(), windowId);

        // 3×9 player inventory (playerInv indices 9–35)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        INV_X + col * 18,
                        INV_Y + row * 18));
            }
        }

        // Hotbar (playerInv indices 0–8)
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

    /**
     * Shift-click: 0-26 = 3×9 inventory, 27-35 = hotbar.
     */
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
