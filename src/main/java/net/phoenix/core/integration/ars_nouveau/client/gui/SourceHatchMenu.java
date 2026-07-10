package net.phoenix.core.integration.ars_nouveau.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class SourceHatchMenu extends AbstractContainerMenu {

    public static MenuType<SourceHatchMenu> TYPE;

    private final BlockPos pos;
    private final ContainerData data;

    public SourceHatchMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(TYPE, id);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    public static SourceHatchMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();

        ContainerData data = new ContainerData() {

            private final int[] a = new int[4];

            @Override
            public int get(int i) {
                return a[i];
            }

            @Override
            public void set(int i, int v) {
                a[i] = v;
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

        return new SourceHatchMenu(id, inv, pos, data);
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public int getCurrent() {
        return data.get(0);
    }

    public int getMax() {
        return data.get(1);
    }

    public int getRate() {
        return data.get(2);
    }

    public int getIoOrd() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isLoaded(pos) && player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
