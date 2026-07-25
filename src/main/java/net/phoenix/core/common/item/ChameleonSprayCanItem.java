package net.phoenix.core.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;
import net.phoenix.core.configs.PhoenixConfigs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChameleonSprayCanItem extends Item {

    private final ChameleonSprayCanBehaviour behaviour = new ChameleonSprayCanBehaviour();

    public ChameleonSprayCanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();

        var temporaryHandler = FluidUtil.getFluidHandler(level, pos, side);
        if (!temporaryHandler.isPresent()) {
            temporaryHandler = FluidUtil.getFluidHandler(level, pos, null);
        }

        if (temporaryHandler.isPresent()) {
            final var blockFluidHandler = temporaryHandler;
            boolean filledTank = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .map(itemCap -> blockFluidHandler.map(blockCap -> {
                        int emptySpace = itemCap.getTankCapacity(0) - itemCap.getFluidInTank(0).getAmount();
                        if (emptySpace <= 0) return false;

                        FluidStack transferable = blockCap.drain(emptySpace, IFluidHandler.FluidAction.SIMULATE);
                        if (!transferable.isEmpty() && itemCap.isFluidValid(0, transferable)) {
                            FluidStack drained = blockCap.drain(emptySpace, IFluidHandler.FluidAction.EXECUTE);
                            itemCap.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                            return true;
                        }
                        return false;
                    }).orElse(false)).orElse(false);

            if (filledTank) {
                level.playSound(null, context.getPlayer().getX(), context.getPlayer().getY(),
                        context.getPlayer().getZ(),
                        SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return this.behaviour.onItemUseFirst(stack, context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(level, player,
                net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof BucketPickup bucketPickup) {
                if (state.getFluidState().getType() == PhoenixProgressionMaterials.PRISMATIC_PAINT.getFluid()) {
                    boolean success = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(itemCap -> {
                        FluidStack fillStack = new FluidStack(PhoenixProgressionMaterials.PRISMATIC_PAINT.getFluid(),
                                FluidType.BUCKET_VOLUME);
                        int filled = itemCap.fill(fillStack, IFluidHandler.FluidAction.SIMULATE);

                        if (filled == FluidType.BUCKET_VOLUME) {
                            itemCap.fill(fillStack, IFluidHandler.FluidAction.EXECUTE);
                            return true;
                        }
                        return false;
                    }).orElse(false);

                    if (success) {
                        bucketPickup.pickupBlock(level, pos, state);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL,
                                SoundSource.PLAYERS, 1.0F, 1.0F);
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack heldItem, @NotNull Slot slot,
                                            @NotNull ClickAction action, @NotNull Player player,
                                            @NotNull net.minecraft.world.entity.SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY || heldItem.isEmpty()) return false;

        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(canCap -> FluidUtil.getFluidHandler(heldItem).map(heldCap -> {
                    var transferred = FluidUtil.tryFluidTransfer(canCap, heldCap, canCap.getTankCapacity(0), true);
                    if (!transferred.isEmpty()) {
                        slotAccess.set(heldCap.getContainer());
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    }
                    return false;
                }).orElse(false)).orElse(false);
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack stack, @NotNull Slot slot, @NotNull ClickAction action,
                                          @NotNull Player player) {
        if (action != ClickAction.SECONDARY) return false;

        ItemStack targetContainer = slot.getItem();
        if (targetContainer.isEmpty()) return false;

        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(canCap -> FluidUtil.getFluidHandler(targetContainer).map(heldCap -> {
                    var transferred = FluidUtil.tryFluidTransfer(canCap, heldCap, canCap.getTankCapacity(0), true);
                    if (!transferred.isEmpty()) {
                        slot.set(heldCap.getContainer());
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    }
                    return false;
                }).orElse(false)).orElse(false);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        return this.behaviour.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        String chromCode = ChameleonSprayCanBehaviour.getChromaticCode(stack);
        if (chromCode != null) {
            tooltipComponents.add(Component.literal("§7Color Mode: §" + chromCode + "Chromatic (" + chromCode + ")"));
        } else {
            DyeColor normalColor = ChameleonSprayCanBehaviour.getColor(stack);
            if (normalColor != null) {
                String rawName = normalColor.getSerializedName();
                String stylizedName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);
                tooltipComponents.add(Component.literal("§7Color Mode: §b" + stylizedName));
            } else {
                tooltipComponents.add(Component.literal("§7Color Mode: §fClear"));
            }
        }

        this.behaviour.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(Component.literal(""));

        tooltipComponents.add(Component.literal("§eControls:"));
        tooltipComponents.add(Component.literal(" §7• §fRight-Click Block §7- Paint a single block"));
        tooltipComponents.add(Component.literal(" §7• §fShift + Scroll §7- Quickly cycle standard colors"));
        tooltipComponents.add(
                Component.literal(" §7• §fPress Keybind or Shift Right-Click §7- Open Color Radial Selection Menu"));

        tooltipComponents.add(Component.literal(""));

        tooltipComponents.add(Component.literal("§bFeatures:"));
        tooltipComponents.add(Component.literal(" §7• §fMass Painting §7- Hold §eShift§7 while painting blocks to"));
        tooltipComponents.add(Component.literal("   §7chain-paint connected surfaces of the same type."));

        double discountPct = (1.0 - PhoenixConfigs.INSTANCE.features.chameleonSprayCanBulkMultiplier) * 100;
        if (discountPct > 0) {
            tooltipComponents.add(Component.literal(
                    String.format("   §aEnjoys a %d%% fluid discount during bulk operations.", (int) discountPct)));
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(fluidHandler -> {
                    int max = fluidHandler.getTankCapacity(0);
                    int current = fluidHandler.getFluidInTank(0).getAmount();
                    return max == 0 ? 0 : Math.round(13.0F * current / max);
                }).orElse(0);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0x00FFFF;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {

            private final int capacity = PhoenixConfigs.INSTANCE.features.chameleonSprayCanCapacity;
            private final LazyOptional<FluidHandlerItemStack> holder = LazyOptional
                    .of(() -> new FluidHandlerItemStack(stack, capacity) {

                        @Override
                        public boolean isFluidValid(int tank, @NotNull FluidStack fluidStack) {
                            return fluidStack.getFluid() == PhoenixProgressionMaterials.PRISMATIC_PAINT.getFluid();
                        }
                    });

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                    return holder.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack);
    }
}
