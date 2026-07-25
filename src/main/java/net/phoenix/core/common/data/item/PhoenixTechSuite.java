package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorLogicSuite;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.common.item.armor.IStepAssist;
import com.gregtechceu.gtceu.core.IFireImmuneEntity;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.mixin.accessor.AbilitiesAccessor;
import net.phoenix.core.utils.TeamUtils;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
public class PhoenixTechSuite extends ArmorLogicSuite implements IStepAssist, GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final Reference2IntMap<MobEffect> potionRemovalCost = new Reference2IntOpenHashMap<>();
    private float charge = 0.0F;
    private static final byte RUNNING_TIMER = 10;
    private static final byte JUMPING_TIMER = 10;
    private static final double LEGGING_ACCEL = 0.085D;

    @OnlyIn(Dist.CLIENT)
    protected ArmorUtils.ModularHUD HUD;

    public PhoenixTechSuite(ArmorItem.Type slot, int energyPerUse, long capacity, int tier) {
        super(energyPerUse, capacity, tier, slot);
        potionRemovalCost.put(MobEffects.POISON, 10000);
        potionRemovalCost.put(MobEffects.WITHER, 25000);
        potionRemovalCost.put(MobEffects.CONFUSION, 8000);
        potionRemovalCost.put(MobEffects.DIG_SLOWDOWN, 12500);
        potionRemovalCost.put(MobEffects.MOVEMENT_SLOWDOWN, 9000);
        potionRemovalCost.put(MobEffects.UNLUCK, 5000);
        if (GTCEu.isClientSide() && this.shouldDrawHUD()) {
            HUD = new ArmorUtils.ModularHUD();
        }
    }

    @Override
    public void onArmorTick(Level world, Player player, ItemStack itemStack) {
        IElectricItem item = GTCapabilityHelper.getElectricItem(itemStack);
        if (item == null) return;

        CompoundTag data = itemStack.getOrCreateTag();
        UUID teamID = net.phoenix.core.utils.TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        if (teamID == null) {
            teamID = player.getUUID();
        }

        ServerLevel serverLevel = !world.isClientSide && world instanceof ServerLevel sl ? sl : null;
        TeslaTeamEnergyData teslaData = serverLevel != null ? TeslaTeamEnergyData.get(serverLevel) : null;

        boolean networkOnline;
        if (serverLevel != null) {

            networkOnline = teslaData != null && teslaData.isOnline(teamID);
            data.putBoolean("TeslaNetworkOnline", networkOnline);
        } else {

            networkOnline = data.getBoolean("TeslaNetworkOnline");
        }

        boolean containerOpen = !world.isClientSide && player.containerMenu != player.inventoryMenu;
        boolean currentTeslaMode = data.getBoolean("teslaMode");

        if (!containerOpen && serverLevel != null && networkOnline && currentTeslaMode) {
            long room = item.getMaxCharge() - item.getCharge();
            if (room > 0) {
                long request = Math.min(room, item.getTransferLimit());
                java.math.BigInteger drained = teslaData.getOrCreate(teamID)
                        .drain(java.math.BigInteger.valueOf(request));
                if (drained.compareTo(java.math.BigInteger.ZERO) > 0) {
                    item.charge(drained.longValue(), item.getTier(), true, false);
                    data.putInt("TeslaChargingTick", 10);
                    if (world.getGameTime() % 5 == 0) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 1, player.getZ(),
                                3, 0.2, 0.4, 0.2, 0.05);
                    }
                }
            }
        }

        if (type == ArmorItem.Type.CHESTPLATE) {
            handleChestplateLogic(item, player, data, world, currentTeslaMode, teamID, teslaData);

            if (serverLevel != null && networkOnline) {
                TeslaTeamEnergyData.TeamEnergy network = teslaData.getOrCreate(teamID);
                handlePhoenixRebirth(player, serverLevel, data, network);

                if (!containerOpen && world.getGameTime() % 20 == 0 && currentTeslaMode) {
                    performInventoryCharging(player, network, this.tier);
                }

                if (currentTeslaMode) handleGlobalTeslaEffects(player, serverLevel, network);
            }
        } else if (type == ArmorItem.Type.HELMET) {
            handleHelmetLogic(item, player, data, world);
        } else if (type == ArmorItem.Type.LEGGINGS) {
            handleLeggingsLogic(item, player, data);
        } else if (type == ArmorItem.Type.BOOTS) {
            handleBootsLogic(item, player, data, serverLevel, world.isClientSide);
        }
    }

    private void handleLeggingsLogic(IElectricItem item, Player player, CompoundTag data) {
        if (player.getAbilities().flying || player.isFallFlying()) {
            return;
        }

        boolean sprinting = SyncedKeyMappings.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
        if (item.canUse(energyPerUse / 100) && sprinting && player.onGround()) {

            float speedModifier = player.isInWater() ? 0.02F : (float) LEGGING_ACCEL;

            player.moveRelative(speedModifier, new Vec3(0, 0, 1));

            if (player.level().getGameTime() % 10 == 0) {
                item.discharge(energyPerUse / 100, item.getTier(), true, false, false);
            }
        }
    }

    private void handleChestplateLogic(IElectricItem item, Player player, CompoundTag data,
                                       Level world, boolean currentTeslaMode,
                                       UUID teamID, TeslaTeamEnergyData teslaData) {
        ((IFireImmuneEntity) player).gtceu$setFireImmune(true);
        if (player.isOnFire()) player.extinguishFire();

        boolean networkOnline = teslaData != null && teslaData.isOnline(teamID);
        ServerLevel serverLevel = world instanceof ServerLevel sl ? sl : null;

        if (currentTeslaMode && (world.isClientSide || networkOnline)) {
            handleFlightSystem(player, data, world, networkOnline, teslaData, teamID);
        } else {
            disableFlight(player, data);
        }

        if (serverLevel != null) handleTeslaVisuals(player, serverLevel, data);

        if (!world.isClientSide && world.getGameTime() % 10 == 0 && networkOnline) {
            var teamData = teslaData.getOrCreate(teamID);

            data.putString("netStored", teamData.stored.toString());
            data.putString("netCapacity", teamData.capacity.toString());
            data.putLong("netDrain", teamData.calculateTotalNetworkFlow());
            data.putLong("chargingDrain", Math.abs(
                    teamData.machineDisplayFlow.getOrDefault(player.blockPosition(), 0L)));

            if (!player.isCreative() && player.containerMenu == player.inventoryMenu) {
                player.inventoryMenu.sendAllDataToRemote();
            }
        }

        int wingTick = data.getInt("wingFlapTick");
        if (wingTick > 0) {
            data.putInt("wingFlapTick", wingTick - 1);
        }
    }

    private void handleHelmetLogic(IElectricItem item, Player player, CompoundTag data, Level world) {
        if (!world.isClientSide) {
            supplyAir(item, player);
            supplyFood(item, player);
            removeNegativeEffects(item, player);
        }

        boolean nightVision = data.getBoolean("nightVision");
        int nightVisionTimer = data.contains("nightVisionTimer") ? data.getInt("nightVisionTimer") :
                ArmorUtils.NIGHTVISION_DURATION;

        if (data.getInt("toggleTimer") == 0 && SyncedKeyMappings.ARMOR_MODE_SWITCH.isKeyDown(player)) {
            nightVision = !nightVision;
            if (item.getCharge() < ArmorUtils.MIN_NIGHTVISION_CHARGE) {
                nightVision = false;
                player.displayClientMessage(Component.translatable("metaarmor.qts.nightvision.error"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("metaarmor.qts.nightvision." + (nightVision ? "enabled" : "disabled")),
                        true);
            }
            data.putBoolean("nightVision", nightVision);
        }

        if (nightVision) {
            player.removeEffect(MobEffects.BLINDNESS);
            if (nightVisionTimer <= ArmorUtils.NIGHT_VISION_RESET) {
                nightVisionTimer = ArmorUtils.NIGHTVISION_DURATION;
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ArmorUtils.NIGHTVISION_DURATION, 0,
                        true, false));
                item.discharge(4, this.tier, true, false, false);
            }
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }

        if (nightVisionTimer > 0) nightVisionTimer--;
        data.putInt("nightVisionTimer", nightVisionTimer);
    }

    private void handleBootsLogic(IElectricItem item, Player player, CompoundTag data, ServerLevel serverLevel,
                                  boolean isClientSide) {
        boolean jumping = player.getDeltaMovement().y > 0 && !player.onGround();
        boolean sneaking = player.isShiftKeyDown();
        boolean boostedJump = data.getBoolean("boostedJump");
        boolean stepAssist = data.getBoolean("stepAssist");
        int toggleBootsTimer = data.getInt("toggleBootsTimer");

        if (serverLevel != null) {
            int dischargeCooldown = data.getInt("dischargeCooldown");
            if (dischargeCooldown > 0) data.putInt("dischargeCooldown", dischargeCooldown - 1);
        }

        if (toggleBootsTimer == 0) {
            if (SyncedKeyMappings.BOOTS_ENABLE.isKeyDown(player)) {
                boostedJump = !boostedJump;
                data.putBoolean("boostedJump", boostedJump);
                player.displayClientMessage(
                        Component.translatable("metaarmor.qts.boosted_jump." + (boostedJump ? "enabled" : "disabled")),
                        true);
                data.putInt("toggleBootsTimer", 10);
            } else if (SyncedKeyMappings.STEP_ASSIST_ENABLE.isKeyDown(player)) {
                stepAssist = !stepAssist;
                data.putBoolean("stepAssist", stepAssist);
                player.displayClientMessage(
                        Component.translatable("metaarmor.qts.step_assist." + (stepAssist ? "enabled" : "disabled")),
                        true);
                data.putInt("toggleBootsTimer", 10);
            }
        }
        if (toggleBootsTimer > 0) data.putInt("toggleBootsTimer", toggleBootsTimer - 1);

        if (boostedJump) {
            if (serverLevel == null) {
                if (item.canUse(energyPerUse / 100) && player.onGround()) {
                    this.charge = 1.0F;
                }
                Vec3 delta = player.getDeltaMovement();

                if (delta.y >= 0.0D && this.charge > 0.0F && !player.isInWater()) {
                    if (player.getDeltaMovement().y > 0.05) {
                        if (this.charge == 1.0F) player.setDeltaMovement(delta.x * 3.6D, delta.y, delta.z * 3.6D);
                        player.addDeltaMovement(new Vec3(0.0, this.charge * 0.32, 0.0));
                        this.charge *= 0.7F;
                    } else if (this.charge < 1.0F) {
                        this.charge = 0.0F;
                    }
                }
            } else {
                boolean prevOnGround = data.getBoolean("onGround");
                if (prevOnGround && !player.onGround() && jumping && !sneaking) {
                    item.discharge(energyPerUse / 100, item.getTier(), true, false, false);
                    ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                    if (!chest.isEmpty()) chest.getOrCreateTag().putInt("wingFlapTick", 15);
                }
                data.putBoolean("onGround", player.onGround());
            }
        }
    }

    private void handleGlobalTeslaEffects(Player player, ServerLevel level, TeslaTeamEnergyData.TeamEnergy network) {
        if (player.hurtTime == 10 && player.getLastHurtByMob() != null) {
            LivingEntity attacker = player.getLastHurtByMob();
            attacker.hurt(level.damageSources().lightningBolt(), 5.0F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 10,
                    0.1, 0.1, 0.1, 0.1);
        }

        if (level.getGameTime() % 20 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 2, 0.2,
                    0.5, 0.2, 0.02);
        }
    }

    private void handleFlightSystem(Player player, CompoundTag data, Level world,
                                    boolean networkOnline, TeslaTeamEnergyData teslaData,
                                    UUID teamID) {
        String flightMode = data.contains("FlightMode") ? data.getString("FlightMode") : "basic";

        PhoenixConfigs.WingFlightConfigs cfg = PhoenixConfigs.wingFlight;

        int rawSpeed = data.contains("FlightSpeed") ? data.getInt("FlightSpeed") : 5;
        int rawDrift = data.contains("FlightDrift") ? data.getInt("FlightDrift") : 5;

        float speedPercent = (Math.max(1, rawSpeed) - 1) / 9.0f;
        float driftPercent = (Math.max(1, rawDrift) - 1) / 9.0f;

        if (flightMode.startsWith("creative")) {
            handleCreativeFlight(player, data, world, cfg, speedPercent, driftPercent, networkOnline, teslaData,
                    teamID);
        } else {
            handleElytraFlight(player, data, world, cfg, speedPercent, driftPercent, networkOnline, teslaData, teamID);
        }
    }

    private void performInventoryCharging(Player player, TeslaTeamEnergyData.TeamEnergy network, int tier) {
        if (player.isCreative()) return;

        if (player.containerMenu != player.inventoryMenu) return;

        if (network == null) return;

        var chargeableItems = ArmorUtils.getChargeableItem(player, tier);
        for (var pair : chargeableItems) {
            for (int slotIndex : pair.getSecond()) {
                ItemStack toolStack = pair.getFirst().get(slotIndex);
                IElectricItem toolCap = GTCapabilityHelper.getElectricItem(toolStack);

                if (toolCap != null && toolCap.getCharge() < toolCap.getMaxCharge()) {
                    long chargeMissing = toolCap.getMaxCharge() - toolCap.getCharge();
                    long request = Math.min(chargeMissing, toolCap.getTransferLimit());

                    java.math.BigInteger drained = network.drain(java.math.BigInteger.valueOf(request));

                    if (drained.compareTo(java.math.BigInteger.ZERO) > 0) {
                        toolCap.charge(drained.longValue(), toolCap.getTier(), true, false);
                    }
                }
            }
        }
    }

    private void applyWingThrust(Player player, Level world, CompoundTag data,
                                 PhoenixConfigs.WingFlightConfigs cfg,
                                 float speedMult, float driftMult,
                                 TeslaTeamEnergyData teslaData, UUID teamID) {
        Vec3 look = player.getLookAngle();
        Vec3 cur = player.getDeltaMovement();

        double euScale = cfg.poweredFlightEUt / 5_000.0;
        double baseThrust = cfg.poweredBoostMin + (speedMult * (cfg.poweredBoostMax - cfg.poweredBoostMin));
        double sigmoid = sigmoidAcceleration(player.tickCount, 5.0, baseThrust, baseThrust * 0.3);
        double thrust = sigmoid * euScale;

        double newX = cur.x + look.x * thrust;
        double newZ = cur.z + look.z * thrust;

        double newY;
        if (look.y > 0) {

            newY = Math.max(cur.y, look.y * thrust * 8.0);
        } else {

            newY = cur.y + look.y * thrust;
        }

        double maxSpeed = cfg.poweredDriftMin + (driftMult * (cfg.poweredDriftMax - cfg.poweredDriftMin));
        Vec3 newVel = new Vec3(newX, newY, newZ);
        if (newVel.length() > maxSpeed) {
            newVel = newVel.scale(maxSpeed / newVel.length());
        }

        player.setDeltaMovement(newVel);
        player.fallDistance = 0;
        player.hurtMarked = true;

        if (!world.isClientSide) {
            data.putBoolean("IsSonicFlight", true);

            teslaData.getOrCreate(player.getUUID())
                    .drain(java.math.BigInteger.valueOf(cfg.poweredFlightEUt));
        }
    }

    private static double sigmoidAcceleration(double t, double peakTime,
                                              double peakAcceleration,
                                              double initialAcceleration) {
        return ((2 * peakAcceleration) / (1 + Math.exp(-t / peakTime)) - peakAcceleration) + initialAcceleration;
    }

    private void handleElytraFlight(Player player, CompoundTag data, Level world,
                                    PhoenixConfigs.WingFlightConfigs cfg,
                                    float speedMult, float driftMult,
                                    boolean networkOnline, TeslaTeamEnergyData teslaData,
                                    UUID teamID) {
        if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        if (!player.onGround() && !player.isFallFlying() && world.isClientSide) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

            if (mc.options.keyJump.consumeClick() && player.getDeltaMovement().y < 0.0) {
                player.startFallFlying();
            }
        }

        if (player.isFallFlying()) {
            player.fallDistance = 0;

            boolean isSneaking = SyncedKeyMappings.VANILLA_SNEAK.isKeyDown(player);
            boolean isPowered = "powered".equals(data.getString("FlightMode"));

            if (isPowered && isSneaking) {
                long cost = (long) cfg.poweredFlightEUt;
                java.math.BigInteger requiredBI = java.math.BigInteger.valueOf(cost);
                boolean hasPower = false;

                if (networkOnline && teslaData != null) {
                    if (teslaData.getOrCreate(teamID).drain(requiredBI).compareTo(requiredBI) >= 0) {
                        hasPower = true;
                    }
                }

                if (!hasPower) {
                    IElectricItem item = GTCapabilityHelper.getElectricItem(player.getItemBySlot(EquipmentSlot.CHEST));
                    if (item != null && item.canUse(cost)) {
                        item.discharge(cost, item.getTier(), true, false, false);
                        hasPower = true;
                    }
                }

                if (hasPower) {

                    applyWingThrust(player, world, data, cfg, speedMult, driftMult, teslaData, teamID);

                    if (!world.isClientSide && world instanceof ServerLevel sl) {
                        Vec3 look = player.getLookAngle();
                        double tx = player.getX() - look.x * 0.8;
                        double ty = player.getY() + 0.5;
                        double tz = player.getZ() - look.z * 0.8;
                        sl.sendParticles(ParticleTypes.FLAME, tx, ty, tz, 3, 0.15, 0.15, 0.15, 0.02);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, tx, ty, tz, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                } else {
                    if (!world.isClientSide) data.putBoolean("IsSonicFlight", false);
                }
            } else {
                if (!world.isClientSide) data.putBoolean("IsSonicFlight", false);
            }
        }
    }

    private void handleCreativeFlight(Player player, CompoundTag data, Level world,
                                      PhoenixConfigs.WingFlightConfigs cfg,
                                      float speedMult, float driftMult,
                                      boolean networkOnline, TeslaTeamEnergyData teslaData,
                                      UUID teamID) {
        String flightMode = data.getString("FlightMode");
        IElectricItem item = GTCapabilityHelper.getElectricItem(player.getItemBySlot(EquipmentSlot.CHEST));

        long cost = (long) (cfg.creativeFlightEUt * (1.0 + (speedMult * 0.5)));
        java.math.BigInteger requiredBI = java.math.BigInteger.valueOf(cost);

        boolean canAfford = player.isCreative() || player.isSpectator();

        if (!canAfford) {

            if (networkOnline && teslaData != null && teslaData.getOrCreate(teamID).stored.compareTo(requiredBI) >= 0) {
                canAfford = true;
            }

            else if (item != null && item.canUse(cost)) {
                canAfford = true;
            }
        }

        if (canAfford) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {

            disableFlight(player, data);
            return;
        }

        if (flightMode.equals("creative+wings")) {
            if (!player.getAbilities().flying && !player.isFallFlying() && !player.onGround() && world.isClientSide) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

                if (mc.options.keyJump.consumeClick() && player.getDeltaMovement().y < 0.0) {
                    player.startFallFlying();
                }
            }

            if (player.isFallFlying()) {
                player.fallDistance = 0;
                boolean isSprinting = SyncedKeyMappings.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();

                if (isSprinting && canAfford) {
                    applyWingThrust(player, world, data, cfg, speedMult, driftMult, teslaData, teamID);
                } else {
                    if (!world.isClientSide) data.putBoolean("IsSonicFlight", false);
                }

                if (!world.isClientSide && isSprinting) {
                    consumeFlightEnergy(item, teslaData, teamID, networkOnline, requiredBI, cost);
                }
                return;
            }
        } else {
            if (player.isFallFlying()) player.stopFallFlying();
        }

        if (player.getAbilities().flying) {
            if (world.isClientSide) {
                float flySpeed = (float) (cfg.creativeSpeedMin +
                        (speedMult * (cfg.creativeSpeedMax - cfg.creativeSpeedMin)));
                ((AbilitiesAccessor) player.getAbilities()).setFlyingSpeed(flySpeed);
                player.onUpdateAbilities();
            } else {
                consumeFlightEnergy(item, teslaData, teamID, networkOnline, requiredBI, cost);
            }
        }

        if (!world.isClientSide) data.putBoolean("IsSonicFlight", false);
    }

    private void consumeFlightEnergy(IElectricItem item, TeslaTeamEnergyData teslaData, UUID teamID,
                                     boolean networkOnline, java.math.BigInteger requiredBI, long cost) {
        boolean success = false;
        if (networkOnline && teslaData != null) {
            if (teslaData.getOrCreate(teamID).drain(requiredBI).compareTo(requiredBI) >= 0) {
                success = true;
            }
        }
        if (!success && item != null) {
            item.discharge(cost, item.getTier(), true, false, false);
        }
    }

    private void handlePhoenixRebirth(Player player, ServerLevel serverLevel, CompoundTag data,
                                      TeslaTeamEnergyData.TeamEnergy network) {
        if (player.getHealth() <= 1.0f) {
            int rebirthCooldown = data.getInt("rebirthCooldown");

            if (rebirthCooldown > 0) {
                data.putInt("rebirthCooldown", rebirthCooldown - 1);
                if (rebirthCooldown % 20 == 0) {
                    int secondsLeft = rebirthCooldown / 20;
                    player.displayClientMessage(Component.literal("§c⚡ REBIRTH RECHARGING: §e" + secondsLeft + "s")
                            .withStyle(ChatFormatting.BOLD), true);
                }
            } else {

                java.math.BigInteger rebirthCost = java.math.BigInteger.valueOf(10_000_000L);
                if (network.stored.compareTo(rebirthCost) >= 0) {
                    network.drain(rebirthCost);

                    player.setHealth(player.getMaxHealth());
                    player.removeAllEffects();
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));

                    data.putInt("rebirthCooldown", 1200);

                    serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1, player.getZ(), 5,
                            0.2, 0.2, 0.2, 0);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1,
                            player.getZ(), 1, 0, 0, 0, 0);

                    player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS,
                            1.5f, 0.8f);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                            SoundSource.PLAYERS, 0.5f, 1.2f);

                    player.displayClientMessage(Component.literal("§6§l⚡ PHOENIX REBIRTH ACTIVATED ⚡"), true);
                } else {
                    player.displayClientMessage(Component.literal("§c⚡ REBIRTH FAILED: §7Insufficient network energy!")
                            .withStyle(ChatFormatting.BOLD), true);
                }
            }
        } else {
            int rebirthCooldown = data.getInt("rebirthCooldown");
            if (rebirthCooldown > 0) data.putInt("rebirthCooldown", rebirthCooldown - 1);
        }
    }

    private void handleTeslaVisuals(Player player, ServerLevel serverLevel, CompoundTag data) {
        if (data.contains("TeslaChargingTick")) {
            int timer = data.getInt("TeslaChargingTick");
            if (timer > 0) data.putInt("TeslaChargingTick", timer - 1);
            else data.remove("TeslaChargingTick");
        }

        if (data.getInt("TeslaChargingTick") > 0) {
            double time = serverLevel.getGameTime() * 0.2;
            for (int i = 0; i < 2; i++) {
                double xOffset = Math.cos(time + (i * Math.PI)) * 0.8;
                double zOffset = Math.sin(time + (i * Math.PI)) * 0.8;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX() + xOffset, player.getY() + 0.5,
                        player.getZ() + zOffset, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME, player.getX() - xOffset, player.getY() + 0.2,
                        player.getZ() - zOffset, 1, 0, 0.02, 0, 0.01);
            }
            if (serverLevel.random.nextFloat() < 0.05f) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 0.2f, 2.0f);
            }
        }
    }

    private void disableFlight(Player player, CompoundTag data) {
        if (player.getAbilities().mayfly && !player.isCreative()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        data.putBoolean("IsSonicFlight", false);
    }

    private String formatTeslaEnergy(java.math.BigInteger energy) {
        String[] units = new String[] { "", "K", "M", "G", "T", "P", "E" };
        java.math.BigDecimal display = new java.math.BigDecimal(energy);
        int unitIndex = 0;

        while (display.compareTo(new java.math.BigDecimal(1000)) >= 0 && unitIndex < units.length - 1) {
            display = display.divide(new java.math.BigDecimal(1000), 2, java.math.RoundingMode.HALF_UP);
            unitIndex++;
        }
        return String.format("%.2f %sEU", display.floatValue(), units[unitIndex]);
    }

    private String getModeDisplayName(String mode) {
        return switch (mode) {
            case "basic" -> "Vanilla Elytra";
            case "powered" -> "Powered Elytra";
            case "creative" -> "Creative";
            case "creative+wings" -> "Creative + Wings";
            default -> "Offline";
        };
    }

    private ChatFormatting getChatColorForMode(String mode) {
        return switch (mode) {
            case "basic" -> ChatFormatting.GREEN;
            case "powered" -> ChatFormatting.GOLD;
            case "creative" -> ChatFormatting.LIGHT_PURPLE;
            case "creative+wings" -> ChatFormatting.AQUA;
            default -> ChatFormatting.DARK_GRAY;
        };
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawHUD(ItemStack item, GuiGraphics guiGraphics) {
        addCapacityHUD(item, this.HUD);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            this.HUD.draw(guiGraphics);
            this.HUD.reset();
            return;
        }

        ItemStack chestplate = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag nbt = chestplate.getTag();

        if (nbt == null || !nbt.getBoolean("teslaMode")) {
            this.HUD.draw(guiGraphics);
            this.HUD.reset();
            return;
        }

        String fMode = nbt.getString("FlightMode");
        if (fMode.isEmpty()) fMode = "basic";

        int speed = nbt.getInt("FlightSpeed");
        int drift = nbt.getInt("FlightDrift");

        this.HUD.newString(Component.literal("✈ " + getModeDisplayName(fMode))
                .withStyle(getChatColorForMode(fMode), ChatFormatting.BOLD));

        if (fMode.equals("powered") || fMode.startsWith("creative")) {
            this.HUD.newString(Component.literal("  » SPEED: " + speed + "/10").withStyle(ChatFormatting.GRAY));
        }
        if (fMode.startsWith("creative")) {
            this.HUD.newString(Component.literal("  » DRIFT: " + drift + "/10").withStyle(ChatFormatting.GRAY));
        }

        if (nbt.getInt("TeslaChargingTick") > 0) {
            this.HUD.newString(Component.literal("ᗯ WIRELESS CHARGING").withStyle(ChatFormatting.YELLOW));
        }

        int rebirthCooldown = nbt.getInt("rebirthCooldown");
        if (rebirthCooldown > 0) {
            this.HUD.newString(Component.literal("☠ REBIRTH: " + ((rebirthCooldown / 20) + 1) + "s")
                    .withStyle(ChatFormatting.RED));
        } else {
            this.HUD.newString(Component.literal("❤ REBIRTH: READY").withStyle(ChatFormatting.GREEN));
        }

        boolean networkIsOnline = nbt.getBoolean("TeslaNetworkOnline");

        if (!networkIsOnline) {

            this.HUD.newString(
                    Component.literal("⚡ NETWORK: OFFLINE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        } else {
            String storedStr = nbt.getString("netStored");
            String capacityStr = nbt.getString("netCapacity");

            if (storedStr.isEmpty() || capacityStr.isEmpty()) {
                this.HUD.newString(
                        Component.literal("⚡ Syncing Network...").withStyle(ChatFormatting.GRAY,
                                ChatFormatting.ITALIC));
            } else {
                try {
                    java.math.BigInteger storedBI = new java.math.BigInteger(storedStr);
                    java.math.BigInteger capacityBI = new java.math.BigInteger(capacityStr);
                    long netLoad = nbt.getLong("netDrain");

                    var cfg = PhoenixConfigs.wingFlight;
                    float speedPercent = (Math.max(1, speed) - 1) / 9.0f;

                    long baseFlightDrain = 0;
                    if (fMode.equals("powered")) {
                        baseFlightDrain = cfg.poweredFlightEUt;
                    } else if (fMode.startsWith("creative")) {
                        baseFlightDrain = cfg.creativeFlightEUt;
                    } else {
                        baseFlightDrain = 0;
                    }

                    long currentFlightCost = 0;

                    if (baseFlightDrain > 0) {
                        if (mc.player.getAbilities().flying || mc.player.isFallFlying()) {
                            currentFlightCost = (long) (baseFlightDrain * (0.5 + (speedPercent * 1.5)));
                        }
                    }

                    long totalSuitLoad = currentFlightCost + nbt.getLong("chargingDrain");

                    int x = 2;
                    int y = guiGraphics.guiHeight() - 20;

                    if (totalSuitLoad > 0) {
                        guiGraphics.drawString(mc.font,
                                "§c-" + formatTeslaEnergy(java.math.BigInteger.valueOf(totalSuitLoad)) + " EU/t (Suit)",
                                x + 1, y - 27, 0xFFFFFFFF, false);
                    }
                    if (netLoad > 0) {
                        guiGraphics.drawString(mc.font,
                                "§4-" + formatTeslaEnergy(java.math.BigInteger.valueOf(netLoad)) + " EU/t (Net)", x + 1,
                                y - 18, 0xFFFFFFFF, false);
                    }

                    guiGraphics.drawString(mc.font,
                            "⚡ " + formatTeslaEnergy(storedBI) + " / " + formatTeslaEnergy(capacityBI), x + 1, y - 9,
                            0xFFAAAAAA, false);

                    float fill = capacityBI.signum() > 0 ? new java.math.BigDecimal(storedBI)
                            .divide(new java.math.BigDecimal(capacityBI), 4, java.math.RoundingMode.HALF_UP)
                            .floatValue() :
                            0f;
                    fill = Math.max(0f, Math.min(1f, fill));

                    renderEnergyBar(guiGraphics, x, y, 80, fill);

                } catch (NumberFormatException ignored) {}
            }
        }

        this.HUD.draw(guiGraphics);
        this.HUD.reset();
    }

    private void renderEnergyBar(GuiGraphics gfx, int x, int y, int width, float fill) {
        gfx.fill(x, y, x + width, y + 5, 0xFF222222);
        int color = fill > 0.6f ? 0xFF00FFAA : (fill > 0.25f ? 0xFFFFAA00 : 0xFFFF4444);
        if (fill > 0) gfx.fill(x, y, x + (int) (width * fill), y + 5, color);
        gfx.fill(x, y, x + width, y + 1, 0xFF555555);
        gfx.fill(x, y + 4, x + width, y + 5, 0xFF555555);
        gfx.fill(x, y, x + 1, y + 5, 0xFF555555);
        gfx.fill(x + width - 1, y, x + width, y + 5, 0xFF555555);
    }

    private static long getTotalPersonalLoad(Minecraft mc, String flightMode, long chargingDrain) {
        long flightDrain = 0;
        if (mc.player.getAbilities().flying || mc.player.isFallFlying()) {
            var cfg = PhoenixConfigs.INSTANCE.wingFlight;
            flightDrain = switch (flightMode) {
                case "powered" -> cfg.poweredFlightEUt;
                case "creative", "creative+wings" -> cfg.creativeFlightEUt;
                default -> 0L;
            };
        }

        long totalPersonalLoad = flightDrain + chargingDrain;
        return totalPersonalLoad;
    }

    public void supplyAir(@NotNull IElectricItem item, Player player) {
        int air = player.getAirSupply();
        if (item.canUse(energyPerUse / 100) && air < 100) {
            player.setAirSupply(air + 200);
            item.discharge(energyPerUse / 100, item.getTier(), true, false, false);
        }
    }

    public void doTeslaDischarge(ServerLevel level, Player player, IElectricItem armorItem) {
        java.math.BigInteger networkCost = BigInteger.valueOf(5000);
        long armorCost = 5000L;

        TeslaTeamEnergyData teslaData = TeslaTeamEnergyData.get(level);
        TeslaTeamEnergyData.TeamEnergy network = teslaData.getOrCreate(player.getUUID());

        boolean hasEnergy = false;
        if (network.stored.compareTo(networkCost) >= 0) {
            network.drain(networkCost);
            hasEnergy = true;
        } else if (armorItem.canUse(armorCost)) {
            armorItem.discharge(armorCost, armorItem.getTier(), true, false, false);
            hasEnergy = true;
        }

        if (!hasEnergy) {
            player.displayClientMessage(
                    Component.literal("§c⚡ DISCHARGE FAILED: §7No energy available!"),
                    true);
            return;
        }

        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(5.0));
        int hitCount = 0;
        for (Entity target : targets) {
            if (target instanceof LivingEntity living && !(target instanceof Player)) {
                living.hurt(level.damageSources().lightningBolt(), 20.0f);
                living.setSecondsOnFire(3);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        living.getX(), living.getY() + 1, living.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
                hitCount++;
            }
        }

        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);

        String msg = hitCount > 0 ? "§b⚡ DISCHARGE: §eHit " + hitCount + " target" + (hitCount > 1 ? "s" : "") + "!" :
                "§b⚡ DISCHARGE: §7No targets in range.";
        player.displayClientMessage(Component.literal(msg), true);
    }

    public void supplyFood(@NotNull IElectricItem item, Player player) {
        if (item.canUse(energyPerUse / 10) && player.getFoodData().needsFood()) {
            IItemHandler playerInv = player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            if (!(playerInv instanceof IItemHandlerModifiable items)) return;

            int bestSlot = -1;
            float bestSaturation = -1f;

            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack current = items.getStackInSlot(i);
                var foodProps = current.getFoodProperties(player);
                if (foodProps == null) continue;

                float saturation = foodProps.getSaturationModifier();
                boolean isHotbar = i < 9;
                float priority = saturation + (isHotbar ? 0.1f : 0f);

                if (priority > bestSaturation) {
                    bestSaturation = priority;
                    bestSlot = i;
                }
            }

            if (bestSlot > -1) {
                ItemStack stack = items.getStackInSlot(bestSlot);
                InteractionResultHolder<ItemStack> result = ArmorUtils.eat(player, stack);
                stack = result.getObject();
                if (stack.isEmpty())
                    items.setStackInSlot(bestSlot, ItemStack.EMPTY);

                if (result.getResult() == InteractionResult.SUCCESS)
                    item.discharge(energyPerUse / 10, item.getTier(), true, false, false);

            }
        }
    }

    public static void removeNegativeEffects(@NotNull IElectricItem item, Player player) {
        for (MobEffectInstance effect : new LinkedList<>(player.getActiveEffects())) {
            MobEffect potion = effect.getEffect();
            int cost = potionRemovalCost.getOrDefault(potion, -1);
            if (cost != -1) {
                cost = cost * (effect.getAmplifier() + 1);
                if (item.canUse(cost)) {
                    item.discharge(cost, item.getTier(), true, false, false);
                    player.removeEffect(potion);
                }
            }
        }
    }

    @Override
    public int damageArmor(LivingEntity entity, ItemStack itemStack, DamageSource source, int damage,
                           EquipmentSlot equipmentSlot) {
        if (source == null) {
            return super.damageArmor(entity, itemStack, source, damage, equipmentSlot);
        }

        if (entity instanceof Player player && !player.level().isClientSide &&
                player.level() instanceof ServerLevel serverLevel) {

            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(serverLevel);
            UUID teamID = net.phoenix.core.utils.TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());

            if (data.isOnline(teamID)) {
                CompoundTag nbt = itemStack.getOrCreateTag();
                if (source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.FALL) ||
                        nbt.getBoolean("teslaMode")) {
                    return 0;
                }

                if (player.getHealth() <= (float) damage) {
                    TeslaTeamEnergyData.TeamEnergy network = data.getOrCreate(teamID);
                    java.math.BigInteger rebirthCost = java.math.BigInteger.valueOf(10_000_000L);

                    ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                    CompoundTag chestNBT = chestplate.getOrCreateTag();
                    int rebirthCooldown = chestNBT.getInt("rebirthCooldown");

                    if (rebirthCooldown <= 0 && network.stored.compareTo(rebirthCost) >= 0) {
                        network.drain(rebirthCost);

                        player.setHealth(player.getMaxHealth());
                        player.removeAllEffects();
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0));
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));

                        chestNBT.putInt("rebirthCooldown", 600);

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
                        serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1, player.getZ(),
                                1, 0, 0, 0, 0);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(),
                                player.getZ(), 2, 0.1, 0.1, 0.1, 0);

                        player.displayClientMessage(Component.literal("§6§l⚡ PHOENIX REBIRTH ACTIVATED ⚡"), true);
                        return 0;
                    }
                }
            }
        }
        return super.damageArmor(entity, itemStack, source, damage, equipmentSlot);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String name = (slot == EquipmentSlot.LEGS) ? "phoenix_tech_suite_2" : "phoenix_tech_suite_1";
        return new ResourceLocation("phoenixcore", "textures/armor/" + name + ".png");
    }

    @Override
    public double getDamageAbsorption() {
        return type == ArmorItem.Type.CHESTPLATE ? 1.2D : 1.0D;
    }

    @Override
    public float getHeatResistance() {
        return 0.5f;
    }

    @Override
    public void addInfo(ItemStack itemStack, List<Component> lines) {
        super.addInfo(itemStack, lines);

        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            var level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
            var data = TeslaTeamEnergyData.get(level);

            assert Minecraft.getInstance().player != null;
            java.util.UUID teamID = net.minecraft.client.Minecraft.getInstance().player.getUUID();
            var network = data.getOrCreate(teamID);

            lines.add(Component.literal("Tesla Network Storage: ")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(Component.literal(formatTeslaEnergy(network.stored))
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));

            lines.add(Component.literal("Network Capacity: ")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW)
                    .append(Component.literal(formatTeslaEnergy(network.capacity))
                            .withStyle(net.minecraft.ChatFormatting.GRAY)));

            lines.add(Component.literal("────────────────────").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        if (type == ArmorItem.Type.HELMET) {
            CompoundTag nbtData = itemStack.getOrCreateTag();
            boolean nv = nbtData.getBoolean("nightVision");

            lines.add(Component.translatable("metaarmor.message.nightvision." + (nv ? "enabled" : "disabled")));

            lines.add(Component.translatable("metaarmor.tooltip.potions"));
            lines.add(Component.translatable("metaarmor.tooltip.breath"));
            lines.add(Component.translatable("metaarmor.tooltip.autoeat"));

        } else if (type == ArmorItem.Type.CHESTPLATE) {
            lines.add(Component.translatable("metaarmor.tooltip.burning"));
            lines.add(Component.translatable("metaarmor.tooltip.freezing"));
            lines.add(Component.translatable("metaarmor.tooltip.wings"));
            lines.add(Component.translatable("metaarmor.tooltip.tesla_connection"));

            if (itemStack.hasTag()) {
                assert itemStack.getTag() != null;
                if (itemStack.getTag().getInt("TeslaChargingTick") > 0) {
                    lines.add(Component.literal("⚡ Wireless Charging Active")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                }
            }

        } else if (type == ArmorItem.Type.LEGGINGS) {
            lines.add(Component.translatable("metaarmor.tooltip.speed"));

        } else if (type == ArmorItem.Type.BOOTS) {
            CompoundTag nbtData = itemStack.getOrCreateTag();
            boolean sa = nbtData.getBoolean("stepAssist");

            lines.add(Component.translatable("metaarmor.message.step_assist." + (sa ? "enabled" : "disabled")));
            lines.add(Component.translatable("metaarmor.tooltip.falldamage"));
            lines.add(Component.translatable("metaarmor.tooltip.jump"));
        }
    }

    @Override
    public boolean isPPE() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "WingController", 5, event -> {
            Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);

            if (entity instanceof Player player) {

                if (player.isFallFlying()) {
                    if (player.getDeltaMovement().length() > 0.6) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.phoenix.sonic"));
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.phoenix.fly"));
                }

                ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                CompoundTag chestNBT = chestplate.getTag();
                if (chestNBT != null && chestNBT.getInt("wingFlapTick") > 0) {
                    return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.phoenix.flap"));
                }
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.phoenix.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
