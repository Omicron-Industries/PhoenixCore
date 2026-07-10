/*
 * package net.phoenix.core.common.machine.multiblock.electric;
 * 
 * 
 * public class PhoenixCleanroomMachine extends WorkableElectricMultiblockMachine {
 * 
 * private static final BlockPattern PATTERN = FactoryBlockPattern.start()
 * .aisle("CCC", "CCC", "CCC")
 * .aisle("C C", "C C", "C C")
 * .aisle("CSC", "CCC", "CCC")
 * .where('S', Predicates.controller(Predicates.blocks(self())))
 * .where('C', Predicates.blocks(PhoenixBlocks.CLEANROOM_CASING.get())
 * .or(Predicates.abilities(GTCapabilities.ENERGY_CONTAINER).setExactLimit(1))
 * .or(Predicates.abilities(capability.GTCapabilities.FLUID_STORAGE).setExactLimit(1)))
 * .where(' ', Predicates.any())
 * .build();
 * 
 * private int cleanliness = 1000;
 * 
 * public PhoenixCleanroomMachine(BlockEntityCreationInfo holder, Object... args) {
 * super(holder, args);
 * }
 * 
 * @Override
 * public BlockPattern getPattern() {
 * return PATTERN;
 * }
 * 
 * @Override
 * public void onWorkingTick(long tick) {
 * super.onWorkingTick(tick);
 * 
 * if (isFormed()) {
 * PhoenixConfigs.CleanroomConfig config = PhoenixConfigs.INSTANCE.cleanroom;
 * 
 * // Check for sterilizing fluid
 * if (getInputFluidInventory().getFluidInTank(0).getFluid().isSame(GTFluids.STERILIZING_GAS.get()) &&
 * getInputFluidInventory().getFluidInTank(0).getAmount() >= config.fluidConsumption) {
 * 
 * getInputFluidInventory().drain(config.fluidConsumption, true);
 * 
 * AABB interior = new AABB(getBlockPos().offset(1, 1, 1), getBlockPos().offset(2, 2, 2));
 * List<Player> playersInside = getLevel().getEntitiesOfClass(Player.class, interior);
 * 
 * if (!playersInside.isEmpty()) {
 * cleanliness = Math.max(0, cleanliness - (config.playerPollution * playersInside.size()));
 * 
 * for (Player player : playersInside) {
 * if (config.lethal && this.isActive() && cleanliness >= config.maxCleanliness) {
 * player.hurt(PhoenixDamageSources.sterilized(getLevel()), Float.MAX_VALUE);
 * }
 * // Check for wireless terminals
 * for (ItemStack itemStack : player.getInventory().items) {
 * if (itemStack.getItem() instanceof WirelessTerminalItem) {
 * this.setWorkingEnabled(false);
 * return; // Stop processing to enforce shutdown
 * }
 * }
 * }
 * } else {
 * if (cleanliness < config.maxCleanliness) {
 * cleanliness = Math.min(config.maxCleanliness, cleanliness + config.regenRate);
 * }
 * }
 * } else {
 * // Not enough sterilizing fluid, so cleanliness degrades
 * cleanliness = Math.max(0, cleanliness - config.playerPollution);
 * }
 * }
 * }
 * 
 * @Override
 * public Component getMultiblockName() {
 * return Component.translatable("block.phoenixcore.cleanroom");
 * }
 * }
 * 
 */
