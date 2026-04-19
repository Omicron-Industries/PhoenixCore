package net.phoenix.core.integration.phoenix_archive.common.data;

/*
 * 
 * public class DataDriveItem extends Item {
 * public DataDriveItem(Properties properties) {
 * super(properties);
 * }
 * 
 * @Override
 * public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
 * ItemStack stack = player.getItemInHand(hand);
 * 
 * if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
 * CompoundTag nbt = stack.getTag();
 * 
 * // Check if the drive has a specific lore identifier
 * if (nbt != null && nbt.contains("LoreKey")) {
 * String loreKey = nbt.getString("LoreKey"); // e.g., "secret_base_coords"
 * String loreValue = nbt.getString("LoreValue"); // e.g., "unlocked"
 * 
 * // 1. Fire the trigger (This saves it to the player's memory)
 * TriggerRegistry.fire(serverPlayer, loreKey, loreValue);
 * 
 * // 2. Visual/Audio feedback
 * level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
 * serverPlayer.sendSystemMessage(Component.literal("§a[SYSTEM] §fData successfully decrypted: §6" +
 * loreKey.toUpperCase()));
 * 
 * // 3. Consume the item
 * stack.shrink(1);
 * return InteractionResultHolder.success(stack);
 * } else {
 * serverPlayer.sendSystemMessage(Component.literal("§c[ERROR] §7Data Drive is corrupt or empty."));
 * }
 * }
 * return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
 * }
 * 
 * @Override
 * public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
 * if (stack.hasTag() && stack.getTag().contains("LoreKey")) {
 * tooltip.add(Component.literal("§dData ID: §f" + stack.getTag().getString("LoreKey")));
 * }
 * super.appendHoverText(stack, level, tooltip, flag);
 * }
 * }
 * 
 */
