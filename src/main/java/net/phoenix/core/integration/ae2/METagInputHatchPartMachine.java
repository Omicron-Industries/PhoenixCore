package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
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
import net.phoenix.core.integration.ae2.widget.FluidPreviewWidget;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class METagInputHatchPartMachine extends MEHatchPartMachine
                                        implements IDataStickInteractable, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            METagInputHatchPartMachine.class,
            MEHatchPartMachine.MANAGED_FIELD_HOLDER);

    protected static final int CONFIG_SIZE = 32;

    @Persisted
    @DescSynced
    protected ExportOnlyAEFluidList aeFluidHandler;
    @Persisted
    private boolean nukeTriggered = false;

    @Persisted
    @DescSynced
    protected String whitelistExpr = "";
    @Persisted
    @DescSynced
    protected String blacklistExpr = "";

    @DescSynced
    public FluidStack[] previewFluids = new FluidStack[CONFIG_SIZE];

    @DescSynced
    public long[] previewAmounts = new long[CONFIG_SIZE];

    protected int refreshTimer = 0;

    public METagInputHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        clearPreview();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /*
     * =========================
     * == LIFECYCLE / TANK ==
     * =========================
     */

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        this.aeFluidHandler = new ExportOnlyAEFluidList(this, CONFIG_SIZE);
        return aeFluidHandler;
    }

    @Override
    public void onMachineRemoved() {
        flushInventory();
    }

    /*
     * =========================
     * == ME SYNC ==
     * =========================
     */

    @Override
    protected void autoIO() {
        if (!isWorkingEnabled()) return;
        if (!shouldSyncME()) return;

        if (updateMEStatus()) {

            if (!isRemote() && !nukeTriggered && containsNukeTag()) {
                nukeTriggered = true;
                triggerNuke();
                markDirty();
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

        double x = getPos().getX() + 0.5;
        double y = getPos().getY() + 0.5;
        double z = getPos().getZ() + 0.5;

        float power = 4.0f;

        world.explode(
                null,
                x, y, z,
                power,
                Level.ExplosionInteraction.BLOCK);

        int radius = Mth.clamp((int) Math.ceil(power / 4.0), 1, 3);
        BlockPos center = getPos();

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

    /*
     * =========================
     * == TAG LOGIC ==
     * =========================
     */

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
            markDirty();
        }
    }

    protected void clearPreview() {
        for (int i = 0; i < CONFIG_SIZE; i++) {
            previewFluids[i] = FluidStack.EMPTY;
            previewAmounts[i] = 0;
        }
    }

    /*
     * =========================
     * == GUI ==
     * =========================
     */

    @Override
    public Widget createUIWidget() {
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
        group.addWidget(new LabelWidget(5, y, "Fluid Preview (Read Only)"));

        y += 15;
        for (int i = 0; i < CONFIG_SIZE; i++) {
            int col = i % 8;
            int row = i / 8;
            group.addWidget(new FluidPreviewWidget(16 + col * 18, y + row * 18, i, this));
        }

        return group;
    }

    /*
     * =========================
     * == DATA STICK ==
     * =========================
     */

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
