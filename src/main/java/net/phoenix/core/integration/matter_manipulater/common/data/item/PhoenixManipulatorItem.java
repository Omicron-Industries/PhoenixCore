package net.phoenix.core.integration.matter_manipulater.common.data.item;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.item.GTToolActions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.common.ToolAction;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixManipulatorMode;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixPlacementEngine;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

public class PhoenixManipulatorItem extends Item implements IInteractionItem {

    public PhoenixManipulatorItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return IInteractionItem.super.onItemUseFirst(stack, context);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return IInteractionItem.super.onEntitySwing(stack, entity);
    }

    public PhoenixManipulatorMode getMode(ItemStack stack) {
        if (stack.hasTag()) {
            assert stack.getTag() != null;
            if (stack.getTag().contains("tool_mode")) {
                return PhoenixManipulatorMode.values()[stack.getTag().getInt("tool_mode")];
            }
        }
        return PhoenixManipulatorMode.LINE;
    }

    public void setMode(ItemStack stack, PhoenixManipulatorMode mode, Player player) {
        stack.getOrCreateTag().putInt("tool_mode", mode.ordinal());
        player.displayClientMessage(mode.getDisplayName(), true);
    }

    private int calculateVolume(BlockPos a, BlockPos b) {
        return (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) *
                (Math.abs(a.getZ() - b.getZ()) + 1);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        PhoenixManipulatorMode mode = getMode(stack);
        BlockPos start = getStartPos(stack);

        if (player.isShiftKeyDown()) {
            if (start != null && pos.getY() > start.getY()) {
                player.displayClientMessage(
                        Component.literal("§c⚠ Error: Point 2 (Shift-Click) cannot be higher than Point 1!"), true);
                player.playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 1.0f, 0.5f);
                return InteractionResult.FAIL;
            }
            setEndPos(stack, pos);
        } else {
            setStartPos(stack, pos);
        }

        BlockPos end = getEndPos(stack);
        start = getStartPos(stack);

        if (start != null && end != null) {
            int count = PhoenixPlacementEngine.getTargetPositions(start, end, mode).size();
            String color = player.isShiftKeyDown() ? "§e" : "§6";
            player.displayClientMessage(Component.literal(
                    color + "Point Set. §bTotal Area: §f" + count + " blocks §7(" + mode.getName() + ")"), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BlockPos start = getStartPos(stack);
            BlockPos end = getEndPos(stack);

            if (start != null && end != null) {
                executeMatterManipulation(level, player, stack);
            } else {
                player.displayClientMessage(Component.literal("§cPhoenix: Select both points first!"), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        PhoenixManipulatorMode mode = getMode(stack);
        BlockPos start = getStartPos(stack);
        BlockPos end = getEndPos(stack);

        tooltip.add(Component.literal("§7Mode: §6" + mode.getName()));
        tooltip.add(Component.literal("§8» §b" + getModeDescription(mode)));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§e§l⚠ PLACEMENT RULE:"));
        tooltip.add(Component.literal("§fAlways set §6Point 1 §fHIGHER than §ePoint 2§f."));
        tooltip.add(Component.literal("§7(Vertical building only works downwards)"));
        tooltip.add(Component.empty());

        if (start != null && end != null) {
            int dx = Math.abs(end.getX() - start.getX()) + 1;
            int dy = Math.abs(end.getY() - start.getY()) + 1;
            int dz = Math.abs(end.getZ() - start.getZ()) + 1;

            int count = PhoenixPlacementEngine.getTargetPositions(start, end, mode).size();

            tooltip.add(Component.literal("§7Selection: §a" + start.toShortString() + " §7→ §a" + end.toShortString()));
            tooltip.add(Component.literal("§8Volume: §f" + count + " blocks §7(" + dx + "x" + dy + "x" + dz + ")"));
        } else if (start != null) {
            tooltip.add(Component.literal("§7Selection: §a" + start.toShortString() + " §7→ §8[§cWaiting for End§8]"));
        } else {
            tooltip.add(Component.literal("§7Selection: §cNone"));
        }

        tooltip.add(Component.empty());

        tooltip.add(Component.literal("§6Controls:"));
        tooltip.add(Component.literal("§8[R-Click Block] §7Set §6Point 1 (Higher)"));
        tooltip.add(Component.literal("§8[Shift + R-Click Block] §7Set §ePoint 2 (Lower)"));
        tooltip.add(Component.literal("§8[R-Click Air] §7Execute §aAction"));
    }

    private String getModeDescription(PhoenixManipulatorMode mode) {
        return switch (mode) {
            case LINE -> "Builds in a single straight axis.";
            case WALL -> "Creates a flat 2D plane.";
            case GRID -> "Fills the 3D bounding box.";
            case CONNECT_ONLY -> "Bridges nodes without placing blocks.";
            case DISCONNECT -> "Severs all node connections.";
        };
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    public Set<GTToolType> getToolTypes(ItemStack stack) {
        return Collections.singleton(GTToolType.WRENCH);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return GTToolActions.DEFAULT_WRENCH_ACTIONS.contains(toolAction) ||
                GTToolActions.DEFAULT_WIRE_CUTTER_ACTIONS.contains(toolAction) ||
                super.canPerformAction(stack, toolAction);
    }

    private void setStartPos(ItemStack stack, @Nullable BlockPos pos) {
        if (pos == null) stack.getOrCreateTag().remove("start_pos");
        else stack.getOrCreateTag().putLong("start_pos", pos.asLong());
    }

    private void setEndPos(ItemStack stack, @Nullable BlockPos pos) {
        if (pos == null) stack.getOrCreateTag().remove("end_pos");
        else stack.getOrCreateTag().putLong("end_pos", pos.asLong());
    }

    @Nullable
    public BlockPos getStartPos(ItemStack stack) {
        return stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("start_pos") ?
                BlockPos.of(stack.getTag().getLong("start_pos")) : null;
    }

    @Nullable
    public BlockPos getEndPos(ItemStack stack) {
        return stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("end_pos") ?
                BlockPos.of(stack.getTag().getLong("end_pos")) : null;
    }

    private void executeMatterManipulation(Level level, Player player, ItemStack stack) {
        BlockPos start = getStartPos(stack);
        BlockPos end = getEndPos(stack);

        if (start == null || end == null) {
            player.displayClientMessage(Component.literal("§cPhoenix: Select start and end points first!"), true);
            return;
        }

        if (end.getY() > start.getY()) {
            player.displayClientMessage(Component.literal("§cCannot Build: Point 2 is higher than Point 1!"), true);
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 1.0f, 0.5f);
            return;
        }

        PhoenixManipulatorMode mode = getMode(stack);
        PhoenixPlacementEngine.fillPipeArea(level, player, start, end, stack, mode);
    }
}
