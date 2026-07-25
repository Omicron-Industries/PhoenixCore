package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.ae2.utils.TagMatcher;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class METagInputBusPartMachine extends MEBusPartMachine
                                      implements IDataStickInteractable, IMachineLife, IHasCircuitSlot {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            METagInputBusPartMachine.class, MEBusPartMachine.MANAGED_FIELD_HOLDER);

    protected static final int CONFIG_SIZE = 32;

    @Persisted
    @DescSynced
    protected ExportOnlyAEItemList aeItemHandler;

    @Persisted
    @DescSynced
    protected String whitelistExpr = "";
    @Persisted
    @DescSynced
    protected String blacklistExpr = "";

    protected int refreshTimer = 0;

    @DescSynced
    public ItemStack[] previewStacks = new ItemStack[CONFIG_SIZE];

    @DescSynced
    public long[] previewAmounts = new long[CONFIG_SIZE];

    public METagInputBusPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            previewStacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEItemList(this, CONFIG_SIZE);
        return this.aeItemHandler;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) updateConfigurationFromTags();
    }

    @Override
    public void onMachineRemoved() {
        flushInventory();
    }

    protected void flushInventory() {
        if (getMainNode().getGrid() == null) return;
        var storage = getMainNode().getGrid().getStorageService().getInventory();
        for (var aeSlot : aeItemHandler.getInventory()) {
            GenericStack stock = aeSlot.getStock();
            if (stock != null) {
                long inserted = storage.insert(stock.what(), stock.amount(), Actionable.MODULATE, actionSource);
                if (inserted > 0) aeSlot.extractItem(0, GTMath.saturatedCast(inserted), false);
            }
        }
    }

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled() || !this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            if (!isRemote() && ++refreshTimer >= 20) {
                refreshTimer = 0;
                updateConfigurationFromTags();
            }

            this.syncME();
            this.updateInventorySubscription();
        }
    }

    protected void syncME() {
        if (getMainNode().getGrid() == null) {
            for (int i = 0; i < CONFIG_SIZE; i++) {
                previewStacks[i] = ItemStack.EMPTY;
                previewAmounts[i] = 0L;
            }
            return;
        }

        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();

        for (int i = 0; i < CONFIG_SIZE; i++) {
            ExportOnlyAEItemSlot aeSlot = this.aeItemHandler.getInventory()[i];

            GenericStack exceedItem = aeSlot.exceedStack();
            if (exceedItem != null) {
                long inserted = networkInv.insert(exceedItem.what(), exceedItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (inserted > 0) aeSlot.extractItem(0, GTMath.saturatedCast(inserted), false);
            }

            GenericStack reqItem = aeSlot.requestStack();
            if (reqItem != null && reqItem.what() instanceof AEItemKey key) {
                if (isAllowed(key)) {
                    long extracted = networkInv.extract(reqItem.what(), reqItem.amount(), Actionable.MODULATE,
                            this.actionSource);
                    if (extracted != 0) aeSlot.addStack(new GenericStack(reqItem.what(), extracted));
                }
            }

            GenericStack stock = aeSlot.getStock();
            GenericStack config = aeSlot.getConfig();

            if (stock != null && stock.what() instanceof AEItemKey itemKey) {

                previewStacks[i] = itemKey.toStack(1);
                previewAmounts[i] = stock.amount();
            } else if (config != null && config.what() instanceof AEItemKey configKey) {

                previewStacks[i] = configKey.toStack(1);
                previewAmounts[i] = 0L;
            } else {

                previewStacks[i] = ItemStack.EMPTY;
                previewAmounts[i] = 0L;
            }
        }
    }

    protected boolean isAllowed(AEItemKey key) {
        if (whitelistExpr.isBlank() && blacklistExpr.isBlank()) return false;

        if (!blacklistExpr.isBlank() && TagMatcher.doesItemMatch(key, blacklistExpr)) return false;

        if (!whitelistExpr.isBlank()) return TagMatcher.doesItemMatch(key, whitelistExpr);

        return true;
    }

    @Override
    public @NotNull Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0), new Size(176, 220));

        group.addWidget(new LabelWidget(3, 0,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        group.addWidget(new ToggleButtonWidget(176 - 45, 0, 40, 16, () -> false, pressed -> {
            whitelistExpr = "";
            blacklistExpr = "";
            updateConfigurationFromTags();
        }).setTexture(new GuiTextureGroup(GuiTextures.VANILLA_BUTTON, new TextTexture("Clear")),
                new GuiTextureGroup(GuiTextures.VANILLA_BUTTON, new TextTexture("Clear"))));

        int y = 18;
        group.addWidget(new LabelWidget(5, y, "Whitelist Tags"));

        y += 12;

        group.addWidget(new MultilineTextFieldWidget(5, y, 166, 30,
                () -> whitelistExpr,
                val -> {
                    whitelistExpr = val;
                    updateConfigurationFromTags();
                },
                Component.literal("...")));

        y += 36;
        group.addWidget(new LabelWidget(5, y, "Blacklist Tags"));

        y += 12;
        group.addWidget(new MultilineTextFieldWidget(5, y, 166, 30,
                () -> blacklistExpr,
                val -> {
                    blacklistExpr = val;
                    updateConfigurationFromTags();
                },
                Component.literal("...")));

        y += 36;
        group.addWidget(new LabelWidget(5, y, "Item Preview (Read Only)"));

        y += 15;
        for (int i = 0; i < CONFIG_SIZE; i++) {
            int col = i % 8;
            int row = i / 8;

            group.addWidget(new LargeAmountPreviewWidget(16 + col * 18, y + row * 18, i, this));
        }

        return group;
    }

    private static class LargeAmountPreviewWidget extends Widget {

        private final int index;
        private final METagInputBusPartMachine machine;

        public LargeAmountPreviewWidget(int x, int y, int index, METagInputBusPartMachine machine) {
            super(new Position(x, y), new Size(18, 18));
            this.index = index;
            this.machine = machine;
        }

        @Override
        public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            Position pos = getPosition();
            GuiTextures.SLOT.draw(graphics, mouseX, mouseY, pos.x, pos.y, 18, 18);

            ItemStack stack = machine.previewStacks[index];
            if (stack != null && !stack.isEmpty()) {
                com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawItemStack(graphics, stack, pos.x + 1, pos.y + 1,
                        0xFFFFFFFF, null);

                long amount = machine.previewAmounts[index];
                if (amount > 0) {
                    String amountStr = com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil
                            .formatLongToCompactString(amount, 4);
                    com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawStringFixedCorner(graphics, amountStr,
                            pos.x + 17, pos.y + 17, 0xFFFFFFFF, true, 0.5f);
                }
            }
        }

        @Override
        public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (isMouseOverElement(mouseX, mouseY)) {
                ItemStack stack = machine.previewStacks[index];
                if (stack != null && !stack.isEmpty()) {
                    graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, stack, mouseX, mouseY);
                }
            }
        }
    }

    protected void updateConfigurationFromTags() {
        if (isRemote() || getMainNode().getGrid() == null) return;

        var storage = getMainNode().getGrid().getStorageService().getInventory();
        var availableInNetwork = storage.getAvailableStacks();
        boolean changed = false;

        System.out.println("[TagBus] Scanning... Whitelist: " + whitelistExpr);

        java.util.Set<AEItemKey> alreadyConfigured = new java.util.HashSet<>();
        for (int i = 0; i < CONFIG_SIZE; i++) {
            var slot = this.aeItemHandler.getInventory()[i];
            GenericStack config = slot.getConfig();

            if (config != null && config.what() instanceof AEItemKey key) {
                boolean stillExists = availableInNetwork.get(key) > 0;

                if (isAllowed(key) && stillExists) {
                    alreadyConfigured.add(key);
                } else {
                    System.out.println(
                            "[TagBus] Clearing Slot " + i + ": " + (stillExists ? "Disallowed" : "Gone from ME"));
                    slot.setConfig(null);

                    previewStacks[i] = ItemStack.EMPTY;
                    previewAmounts[i] = 0L;
                    changed = true;
                }
            }
        }

        for (var entry : availableInNetwork) {
            if (entry.getKey() instanceof AEItemKey itemKey && isAllowed(itemKey)) {
                if (!alreadyConfigured.contains(itemKey)) {
                    for (int i = 0; i < CONFIG_SIZE; i++) {
                        var slot = this.aeItemHandler.getInventory()[i];
                        if (slot.getConfig() == null) {
                            System.out.println(
                                    "[TagBus] Adding new match: " + itemKey.toStack().getDisplayName().getString());
                            slot.setConfig(new GenericStack(itemKey, Integer.MAX_VALUE));
                            alreadyConfigured.add(itemKey);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        if (changed) {
            notifyUpdate();
        }
    }

    private void notifyUpdate() {
        this.updateInventorySubscription();
        this.markDirty();
        if (self().getHolder() != null) {
            self().getHolder().self().setChanged();
        }
    }

    @Override
    public final InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!isRemote()) {
            CompoundTag tag = new CompoundTag();
            tag.put("METagInputBus", writeConfigToTag());
            dataStick.setTag(tag);

            String displayName = whitelistExpr.isBlank() ? "Empty Tag Bus" : "Tag Bus: " + whitelistExpr;
            dataStick.setHoverName(Component.literal("ME Tag Input Bus Configuration Data")
                    .withStyle(style -> style.withItalic(true)));

            player.sendSystemMessage(Component.literal("Settings Copied: " + whitelistExpr));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public final InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (!dataStick.hasTag() || !dataStick.getTag().contains("METagInputBus")) {
            return InteractionResult.PASS;
        }

        if (!isRemote()) {
            readConfigFromTag(dataStick.getTag().getCompound("METagInputBus"));

            updateConfigurationFromTags();

            player.sendSystemMessage(Component.literal("Settings Pasted successfully."));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("WhitelistExpr", whitelistExpr);
        tag.putString("BlacklistExpr", blacklistExpr);
        return tag;
    }

    protected void readConfigFromTag(CompoundTag tag) {
        this.whitelistExpr = tag.getString("WhitelistExpr");
        this.blacklistExpr = tag.getString("BlacklistExpr");
        updateConfigurationFromTags();
    }

    @Override
    public @NotNull NotifiableItemStackHandler getCircuitInventory() {
        return circuitInventory;
    }
}
