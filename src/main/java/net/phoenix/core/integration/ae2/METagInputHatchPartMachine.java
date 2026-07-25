package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.mui.widgets.textfield.TextEditorWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.utils.GTMath;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.phoenix.core.integration.ae2.utils.TagMatcher;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class METagInputHatchPartMachine extends MEHatchPartMachine
                                        implements IDataStickInteractable {

    protected static final int CONFIG_SIZE = 32;

    @SaveField
    @SyncToClient
    protected ExportOnlyAEFluidList aeFluidHandler;
    @SaveField
    private boolean nukeTriggered = false;

    @SaveField
    @SyncToClient
    protected String whitelistExpr = "";
    @SaveField
    @SyncToClient
    protected String blacklistExpr = "";

    @SyncToClient
    public FluidStack[] previewFluids = new FluidStack[CONFIG_SIZE];

    @SyncToClient
    public long[] previewAmounts = new long[CONFIG_SIZE];

    protected int refreshTimer = 0;

    public METagInputHatchPartMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, IO.IN);
        clearPreview();
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots) {
        this.aeFluidHandler = new ExportOnlyAEFluidList(this, CONFIG_SIZE);
        return aeFluidHandler;
    }

    @Override
    public void removedFromController(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller) {
        flushInventory();
        super.removedFromController(controller);
    }

    @Override
    public void onUnload() {
        flushInventory();
        super.onUnload();
    }

    @Override
    protected void autoIO() {
        if (!isWorkingEnabled()) return;
        if (!shouldSyncME()) return;

        if (updateMEStatus()) {

            if (!isRemote() && !nukeTriggered && containsNukeTag()) {
                nukeTriggered = true;
                triggerNuke();
                this.markAsChanged();
                return;
            }

            if (!isRemote() && ++refreshTimer >= 20) {
                refreshTimer = 0;
                updateConfigurationFromTags();
            }

            syncME();
            updateTankSubscription();
        }
    }

    private void triggerNuke() {
        if (!(getLevel() instanceof ServerLevel world)) return;

        double x = getBlockPos().getX() + 0.5;
        double y = getBlockPos().getY() + 0.5;
        double z = getBlockPos().getZ() + 0.5;

        float power = 4.0f;

        world.explode(
                null,
                x, y, z,
                power,
                Level.ExplosionInteraction.BLOCK);

        int radius = Mth.clamp((int) Math.ceil(power / 4.0), 1, 3);
        BlockPos center = getBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) continue;

                    BlockPos targetPos = center.offset(dx, dy, dz);
                    if (!world.isLoaded(targetPos)) continue;

                    BlockState state = world.getBlockState(targetPos);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(world, targetPos) < 0) continue;

                    world.removeBlockEntity(targetPos);
                    world.setBlock(
                            targetPos,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | 64 | 128);
                }
            }
        }
    }

    private boolean containsNukeTag() {
        return whitelistExpr.contains("forge:nuclear_bombs") || blacklistExpr.contains("forge:nuclear_bombs");
    }

    protected void syncME() {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            clearPreview();
            return;
        }

        MEStorage network = grid.getStorageService().getInventory();

        for (int i = 0; i < CONFIG_SIZE; i++) {
            ExportOnlyAEFluidSlot slot = aeFluidHandler.getInventory()[i];

            GenericStack overflow = slot.exceedStack();
            if (overflow != null) {
                long inserted = network.insert(overflow.what(), overflow.amount(), Actionable.MODULATE, actionSource);
                if (inserted > 0) {
                    slot.drain(GTMath.saturatedCast(inserted), IFluidHandler.FluidAction.EXECUTE);
                }
            }

            GenericStack req = slot.requestStack();
            if (req != null && req.what() instanceof AEFluidKey key && isAllowed(key)) {
                long extracted = network.extract(req.what(), req.amount(), Actionable.MODULATE, actionSource);
                if (extracted > 0) {
                    slot.addStack(new GenericStack(key, extracted));
                }
            }

            GenericStack stock = slot.getStock();
            GenericStack config = slot.getConfig();

            if (stock != null && stock.what() instanceof AEFluidKey key) {
                previewFluids[i] = key.toStack(1000);
                previewAmounts[i] = stock.amount();
            } else if (config != null && config.what() instanceof AEFluidKey key) {
                previewFluids[i] = key.toStack(1000);
                previewAmounts[i] = 0;
            } else {
                previewFluids[i] = FluidStack.EMPTY;
                previewAmounts[i] = 0;
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid == null) return;

        MEStorage storage = grid.getStorageService().getInventory();
        for (var slot : aeFluidHandler.getInventory()) {
            GenericStack stock = slot.getStock();
            if (stock != null) {
                storage.insert(stock.what(), stock.amount(), Actionable.MODULATE, actionSource);
            }
        }
    }

    protected boolean isAllowed(AEFluidKey key) {
        if (whitelistExpr.isBlank() && blacklistExpr.isBlank()) return false;
        if (!blacklistExpr.isBlank() && TagMatcher.doesFluidMatch(key, blacklistExpr)) return false;
        if (!whitelistExpr.isBlank()) return TagMatcher.doesFluidMatch(key, whitelistExpr);
        return true;
    }

    protected void updateConfigurationFromTags() {
        if (isRemote()) return;

        var grid = getMainNode().getGrid();
        if (grid == null) return;

        var storage = grid.getStorageService().getInventory();
        var available = storage.getAvailableStacks();

        Set<AEFluidKey> configured = new HashSet<>();
        boolean changed = false;

        for (int i = 0; i < CONFIG_SIZE; i++) {
            var slot = aeFluidHandler.getInventory()[i];
            var config = slot.getConfig();

            if (config != null && config.what() instanceof AEFluidKey key) {
                if (available.get(key) > 0 && isAllowed(key)) {
                    configured.add(key);
                } else {
                    slot.setConfig(null);
                    previewFluids[i] = FluidStack.EMPTY;
                    previewAmounts[i] = 0;
                    changed = true;
                }
            }
        }

        for (var entry : available) {
            if (entry.getKey() instanceof AEFluidKey key && isAllowed(key)) {
                if (!configured.contains(key)) {
                    for (var slot : aeFluidHandler.getInventory()) {
                        if (slot.getConfig() == null) {
                            slot.setConfig(new GenericStack(key, Integer.MAX_VALUE));
                            configured.add(key);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        if (changed) {
            updateTankSubscription();
            this.markAsChanged();
        }
    }

    protected void clearPreview() {
        for (int i = 0; i < CONFIG_SIZE; i++) {
            previewFluids[i] = FluidStack.EMPTY;
            previewAmounts[i] = 0;
        }
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
                        Text.dynamic(() -> Component.literal("Matched Fluids: " + java.util.Arrays.stream(previewFluids)
                                .filter(f -> f != null && !f.isEmpty()).count())))));
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack stick) {
        if (!isRemote()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("WhitelistExpr", whitelistExpr);
            tag.putString("BlacklistExpr", blacklistExpr);
            stick.getOrCreateTag().put("METagInputHatch", tag);
            player.sendSystemMessage(Component.literal("Tag Fluid Hatch settings copied"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack stick) {
        if (!stick.hasTag() || !stick.getTag().contains("METagInputHatch"))
            return InteractionResult.PASS;

        if (!isRemote()) {
            CompoundTag tag = stick.getTag().getCompound("METagInputHatch");
            whitelistExpr = tag.getString("WhitelistExpr");
            blacklistExpr = tag.getString("BlacklistExpr");
            updateConfigurationFromTags();
            player.sendSystemMessage(Component.literal("Tag Fluid Hatch settings pasted"));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }
}
