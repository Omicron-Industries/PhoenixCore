package net.phoenix.core.api;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public class PhoenixArmorEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity();

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof PhoenixArmorItem) {
            if (!player.onGround()) {
                event.setNewSpeed(event.getNewSpeed() * 5.0f);
            }

            if (player.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) &&
                    !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player)) {
                event.setNewSpeed(event.getNewSpeed() * 5.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

            if (chest.getItem() instanceof PhoenixArmorItem) {
                DamageSource source = event.getSource();

                if (source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.FALL)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof PhoenixArmorItem)) return;

        ServerLevel level = (ServerLevel) player.level();
        var source = event.getSource();
        var nbt = chest.getOrCreateTag();

        if (source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL) ||
                source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            event.setCanceled(true);
            return;
        }

        if (nbt.getBoolean("teslaMode")) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);
            var network = data.getOrCreate(player.getUUID());

            float damage = event.getAmount();
            java.math.BigInteger cost = java.math.BigInteger.valueOf((long) (damage * 10000L));

            if (network.stored.compareTo(cost) >= 0) {
                network.drain(cost);

                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.3, 0.5, 0.3, 0.02);

                if (player.getRandom().nextFloat() < 0.3f) {
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 1.5f);
                }

                event.setCanceled(true);
            }
        }
    }
}
