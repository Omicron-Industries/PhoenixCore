package net.phoenix.core.growth;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.phoenix.core.api.gui.widget.ActionButtons;
import net.phoenix.core.api.gui.widget.StageTrackerWidget;
import net.phoenix.core.client.worldfx.IWorldFXEmitter;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.builtin.NebulaSkyLayer;
import net.phoenix.core.common.machine.multiblock.unique.UniqueWorkableElectricMultiblockMachine;
import net.phoenix.core.growth.tendril.GrowthWardBlock;
import net.phoenix.core.growth.tendril.TendrilShape;
import net.phoenix.core.growth.tendril.TendrilShapeRegistry;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.PanelSyncManager;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class GrowthMultiblockMachine extends UniqueWorkableElectricMultiblockMachine
                                              implements IWorldFXEmitter {

    public enum GrowthResult {
        SUCCESS,
        PROGRESSED,
        MAX_STAGE,
        OBSTRUCTED,
        INSUFFICIENT_COST
    }

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    protected int growthStage = 0;

    @Getter
    @SaveField
    @SyncToClient
    protected float growthProgress = 0f;

    protected GrowthMultiblockMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    protected abstract List<GrowthStage> getGrowthStages();

    protected abstract BlockState getShellBlockState();

    @Override
    public void afterWorking() {
        super.afterWorking();
        if (!getLevel().isClientSide) {
            tryGrow();
            tryGrowTendril();
        }
    }

    protected float getGrowthPerRecipe() {
        return 1.0f;
    }

    public GrowthResult tryGrow() {
        return addGrowthProgress(getGrowthPerRecipe());
    }

    private GrowthResult addGrowthProgress(float amount) {
        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (next >= stages.size()) return GrowthResult.MAX_STAGE;
        if (amount <= 0f) return GrowthResult.PROGRESSED;

        growthProgress += amount;
        if (growthProgress < 1f) {
            getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
            setChanged();
            return GrowthResult.PROGRESSED;
        }

        growthProgress -= 1f;
        GrowthResult result = advanceStage();
        if (result != GrowthResult.SUCCESS) {

            growthProgress = Math.min(growthProgress + 1f, 0.999f);
        }
        getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
        setChanged();
        return result;
    }

    public GrowthResult tryManualGrow() {
        return tryManualGrow(1.0f);
    }

    public GrowthResult tryManualGrow(float amount) {
        if (!(getLevel() instanceof ServerLevel)) return GrowthResult.MAX_STAGE;
        if (amount <= 0f) return GrowthResult.PROGRESSED;

        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (next >= stages.size()) return GrowthResult.MAX_STAGE;

        List<ItemStack> cost = scaleCost(stages.get(next).cost(), amount);
        if (!cost.isEmpty() && !consumeCost(cost)) return GrowthResult.INSUFFICIENT_COST;

        return addGrowthProgress(amount);
    }

    private List<ItemStack> scaleCost(List<ItemStack> cost, float amount) {
        if (amount >= 1f || cost.isEmpty()) return cost;
        List<ItemStack> scaled = new ArrayList<>(cost.size());
        for (ItemStack required : cost) {
            ItemStack copy = required.copy();
            copy.setCount(Math.max(1, (int) Math.ceil(required.getCount() * amount)));
            scaled.add(copy);
        }
        return scaled;
    }

    private GrowthResult advanceStage() {
        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (next >= stages.size()) return GrowthResult.MAX_STAGE;
        if (!(getLevel() instanceof ServerLevel level)) return GrowthResult.MAX_STAGE;

        IntList from = stages.get(growthStage).bounds();
        IntList to = stages.get(next).bounds();

        List<BlockPos> newPositions = GrowthPatternHelper.diffShellPositions(
                getBlockPos(), getFrontFacing(), getUpwardsFacing(), from, to);

        for (BlockPos pos : newPositions) {
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                onGrowthObstructed(pos);
                return GrowthResult.OBSTRUCTED;
            }
        }

        BlockState shell = getShellBlockState();
        for (BlockPos pos : newPositions) {
            level.setBlock(pos, shell, 3);
        }

        growthStage = next;
        getSyncDataHolder().markClientSyncFieldDirty("growthStage");
        setChanged();

        invalidateStructureCaches();
        checkAndFormStructure();
        return GrowthResult.SUCCESS;
    }

    private boolean consumeCost(List<ItemStack> cost) {
        IItemHandlerModifiable[] handlers = getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).stream()
                .filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast)
                .toArray(IItemHandlerModifiable[]::new);
        CombinedInvWrapper inv = new CombinedInvWrapper(handlers);

        for (ItemStack required : cost) {
            if (countAvailable(inv, required) < required.getCount()) return false;
        }
        for (ItemStack required : cost) {
            extractExact(inv, required);
        }
        return true;
    }

    private int countAvailable(CombinedInvWrapper inv, ItemStack required) {
        int found = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(slot, required)) found += slot.getCount();
        }
        return found;
    }

    private void extractExact(CombinedInvWrapper inv, ItemStack required) {
        int remaining = required.getCount();
        for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(slot, required)) {
                int extracted = inv.extractItem(i, remaining, false).getCount();
                remaining -= extracted;
            }
        }
    }

    protected void onGrowthObstructed(BlockPos pos) {}

    private static final class TendrilInstance {

        final BlockPos anchor;
        final TendrilShape shape;
        int progress;
        boolean blocked;

        TendrilInstance(BlockPos anchor, TendrilShape shape) {
            this.anchor = anchor;
            this.shape = shape;
        }
    }

    private final List<TendrilInstance> activeTendrils = new ArrayList<>();

    protected List<BlockPos> getTendrilAnchors() {
        return List.of();
    }

    protected int getMaxTendrils() {
        return 3;
    }

    protected int getMaxTendrilLength() {
        return 12;
    }

    protected BlockState getTendrilBlockState() {
        return getShellBlockState();
    }

    public void tryGrowTendril() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        List<BlockPos> anchors = getTendrilAnchors();
        if (anchors.isEmpty()) return;

        for (TendrilInstance tendril : activeTendrils) {
            if (tendril.blocked || tendril.progress >= tendril.shape.steps().size()) continue;
            extendTendril(level, tendril);
            return;
        }

        if (activeTendrils.size() >= getMaxTendrils()) return;

        RandomSource random = level.getRandom();
        TendrilShape shape = TendrilShapeRegistry.pickWeighted(this, random);
        if (shape == null) return;

        BlockPos anchorOffset = anchors.get(random.nextInt(anchors.size()));
        BlockPos anchor = GrowthPatternHelper.rotateToWorld(anchorOffset, getFrontFacing(), getUpwardsFacing())
                .offset(getBlockPos());
        activeTendrils.add(new TendrilInstance(anchor, shape));
    }

    private void extendTendril(ServerLevel level, TendrilInstance tendril) {
        if (tendril.progress >= tendril.shape.steps().size() || tendril.progress >= getMaxTendrilLength()) {
            tendril.blocked = true;
            return;
        }

        BlockPos localStep = tendril.shape.steps().get(tendril.progress);
        BlockPos target = GrowthPatternHelper.rotateToWorld(localStep, getFrontFacing(), getUpwardsFacing())
                .offset(tendril.anchor);

        if (isWarded(level, target)) {
            tendril.blocked = true;
            return;
        }

        BlockState existing = level.getBlockState(target);
        if (!existing.isAir()) {
            if (existing.getDestroySpeed(level, target) < 0) {
                tendril.blocked = true;
                return;
            }
            level.destroyBlock(target, true);
        }

        level.setBlock(target, getTendrilBlockState(), 3);
        tendril.progress++;
    }

    private boolean isWarded(ServerLevel level, BlockPos pos) {
        int r = GrowthWardBlock.PROTECTION_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            if (level.getBlockState(p).getBlock() instanceof GrowthWardBlock) return true;
        }
        return false;
    }

    @ClientFieldChangeListener(fieldName = "isFormed")
    protected void onFormedChangedClient() {
        if (isFormed()) {
            WorldFXManager.register(getBlockPos(), this);
        } else {
            WorldFXManager.unregister(getBlockPos());
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() != null && getLevel().isClientSide) {
            WorldFXManager.unregister(getBlockPos());
        }
    }

    @Override
    public @Nullable PhoenixSkyLayer createSkyLayer() {
        float t = getGrowthStages().isEmpty() ? 0f : growthStage / (float) Math.max(1, getGrowthStages().size() - 1);
        return new NebulaSkyLayer(
                new float[] { 0.35f, 0.65f, 1.0f },
                new float[] { 0.85f, 0.35f, 0.95f },
                0.35f + 0.35f * t,
                1.0f + 0.5f * t,
                getBlockPos().asLong() % 1000);
    }

    @Override
    public @Nullable PhoenixScreenEffect createScreenEffect() {
        return null;
    }

    @Override
    public float getEffectRadius() {
        return 32f + growthStage * 8f;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = new ArrayList<>(super.getWidgetsForDisplay(syncManager));

        widgets.addAll(StageTrackerWidget.of(syncManager, "growthStagePips", this::getGrowthStage,
                getGrowthStages().size() - 1));

        brachy.modularui.value.sync.DoubleSyncValue progressSync = syncManager.getOrCreateSyncHandler(
                "growthProgressPct", brachy.modularui.value.sync.DoubleSyncValue.class,
                () -> new brachy.modularui.value.sync.DoubleSyncValue(() -> (double) getGrowthProgress()));
        widgets.add(new brachy.modularui.widgets.TextWidget<>(Text.dynamic(() -> net.minecraft.network.chat.Component
                .literal("Growth: " + Math.round(progressSync.getDoubleValue() * 100) + "%"))));

        widgets.add(ActionButtons.simple(syncManager, "growthTriggerPressed", () -> tryManualGrow(),
                new ItemDrawable(new ItemStack(getShellBlockState().getBlock())).asIcon().size(16),
                new RichTooltip().addLine(Text.of(net.minecraft.network.chat.Component
                        .translatable("phoenixcore.growth.trigger_button")))));

        return widgets;
    }
}
