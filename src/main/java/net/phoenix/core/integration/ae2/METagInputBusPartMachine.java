package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait;
import com.gregtechceu.gtceu.common.mui.widgets.textfield.TextEditorWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.utils.GTMath;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class METagInputBusPartMachine extends MEBusPartMachine implements IDataStickInteractable {

    protected static final int CONFIG_SIZE = 32;

    protected final ProgrammableCircuitSlotTrait circuitTrait;

    @SaveField
    @SyncToClient
    protected ExportOnlyAEItemList aeItemHandler;

    @SaveField
    @SyncToClient
    protected String whitelistExpr = "";
    @SaveField
    @SyncToClient
    protected String blacklistExpr = "";

    protected int refreshTimer = 0;

    @SyncToClient
    public ItemStack[] previewStacks = new ItemStack[CONFIG_SIZE];

    @SyncToClient
    public long[] previewAmounts = new long[CONFIG_SIZE];

    public METagInputBusPartMachine(BlockEntityCreationInfo info, IO io) {
        super(info, io, new ExportOnlyAEItemList(CONFIG_SIZE));

        this.aeItemHandler = (ExportOnlyAEItemList) this.getInventory();

        this.circuitTrait = new ProgrammableCircuitSlotTrait();
        this.attachPersistentTrait("programmable_circuit", this.circuitTrait);

        for (int i = 0; i < CONFIG_SIZE; i++) {
            previewStacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public NotifiableItemStackHandler getInventory() {
        return this.aeItemHandler != null ? (NotifiableItemStackHandler) (Object) this.aeItemHandler :
                super.getInventory();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) updateConfigurationFromTags();
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        if (this.circuitTrait != null) {
            this.circuitTrait.addedToController(controller);
        }
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        flushInventory();
        super.removedFromController(controller);
        if (this.circuitTrait != null) {
            this.circuitTrait.removedFromController(controller);
        }
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
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        StringSyncValue whitelistSync = new StringSyncValue(() -> whitelistExpr, val -> {
            whitelistExpr = val;
            updateConfigurationFromTags();
        });
        syncManager.syncValue("whitelist", whitelistSync);

        StringSyncValue blacklistSync = new StringSyncValue(() -> blacklistExpr, val -> {
            blacklistExpr = val;
            updateConfigurationFromTags();
        });
        syncManager.syncValue("blacklist", blacklistSync);

        mainWidget.child(Flow.column()
                .coverChildren()
                .child(new TextWidget<>(
                        Text.dynamic(() -> isOnline ? Component.translatable("gtceu.gui.me_network.online") :
                                Component.translatable("gtceu.gui.me_network.offline"))))
                .child(new TextWidget<>(Text.of(Component.literal("Whitelist Tags:"))))
                .child(new TextEditorWidget<>().value(whitelistSync).size(166, 20))
                .child(new TextWidget<>(Text.of(Component.literal("Blacklist Tags:"))))
                .child(new TextEditorWidget<>().value(blacklistSync).size(166, 20))
                .child(new TextWidget<>(
                        Text.dynamic(() -> Component.literal("Matched Items: " + java.util.Arrays.stream(previewStacks)
                                .filter(s -> s != null && !s.isEmpty()).count())))));
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

        if (this.getSyncDataHolder() != null) {
            this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
        }

        this.markAsChanged();
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

    public ProgrammableCircuitSlotTrait getCircuitTrait() {
        return this.circuitTrait;
    }
}
