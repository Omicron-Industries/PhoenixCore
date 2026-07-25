package net.phoenix.core.integration.jade.provider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class HighPressurePlasmaArcFurnaceProvider implements IBlockComponentProvider,
                                                  IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("plasma_furnace_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(UID)) return;

        if (!(accessor.getBlockEntity() instanceof HighPressurePlasmaArcFurnaceMachine)) {
            return;
        }

        CompoundTag data = accessor.getServerData();
        if (data == null || data.isEmpty()) return;

        Shield.ShieldTypes shieldType = null;

        if (data.contains("shieldKey")) {
            int shieldKey = data.getInt("shieldKey");
            shieldType = Shield.ShieldTypes.getShieldFromKey(shieldKey);

            if (shieldType != null) {
                tooltip.add(Component.translatable(
                        "jade.phoenixcore.shield_state",
                        Component.translatable(shieldType.langKey)));
            }
        }

        if (shieldType == null) return;

        if (shieldType == Shield.ShieldTypes.NORMAL && data.contains("shieldHealth")) {
            int health = data.getInt("shieldHealth");
            tooltip.add(Component.translatable("jade.phoenixcore.shield_health", health));
        }

        if (shieldType == Shield.ShieldTypes.DECAYED && data.contains("shieldCooldown")) {
            int cooldownTicks = data.getInt("shieldCooldown");
            if (cooldownTicks > 0) {
                int seconds = cooldownTicks / 20;
                tooltip.add(Component.translatable("jade.phoenixcore.shield_cooldown", seconds));
            }
        }

        if (shieldType == Shield.ShieldTypes.NORMAL) {
            if (data.contains("isBoosted") && data.getBoolean("isBoosted")) {

                String boostName = data.contains("boostName") ? data.getString("boostName") : "Unknown";
                float durationMultiplier = data.contains("boostDuration") ? data.getFloat("boostDuration") : 0.0f;

                tooltip.add(Component.translatable(
                        "jade.phoenixcore.plasma_boost_active",
                        Component.literal("§b" + boostName)));

                tooltip.add(Component.translatable(
                        "jade.phoenixcore.plasma_boost_duration",
                        (int) (durationMultiplier * 100) + "%").withStyle(style -> style.withColor(0x00FFFF)));

            } else {
                tooltip.add(Component.translatable("jade.phoenixcore.no_plasma_boost")
                        .withStyle(style -> style.withColor(0x777777)));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof HighPressurePlasmaArcFurnaceMachine machine &&
                machine.isFormed()) {

            compoundTag.putInt("shieldKey", machine.getShieldType().key);

            compoundTag.putInt("shieldHealth", machine.getShieldHealth());
            compoundTag.putInt("shieldCooldown", machine.getShieldCooldownTimer());

            boolean isBoostApplied = machine.recipeLogic.isActive() && machine.isPlasmaBoosted;

            compoundTag.putBoolean("isBoosted", isBoostApplied);

            if (isBoostApplied && machine.activeBoost != null) {
                compoundTag.putString("boostName", machine.activeBoost.name());
                compoundTag.putFloat("boostDuration", (float) machine.activeBoost.durationMultiplier());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
