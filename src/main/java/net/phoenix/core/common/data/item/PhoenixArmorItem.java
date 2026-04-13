package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.phoenix.core.client.renderer.PhoenixArmorRenderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class PhoenixArmorItem extends ArmorComponentItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final PhoenixTechSuite logic;

    public PhoenixArmorItem(ArmorMaterial material, Type type, Properties properties, PhoenixTechSuite logic) {
        super(material, type, properties);
        this.logic = logic;
        this.setArmorLogic(logic);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        this.logic.onArmorTick(world, player, stack);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            CompoundTag nbt = stack.getTag();
            return nbt != null && nbt.getBoolean("teslaMode");
        }
        return false;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {

            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        this.logic.addInfo(stack, tooltip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "WingController", 5, event -> {
            Entity entity = event.getData(DataTickets.ENTITY);

            if (entity instanceof Player player) {

                if (player.isFallFlying()) {
                    if (player.getDeltaMovement().length() > 1.1) {
                        return event.setAndContinue(RawAnimation.begin().thenPlay("animation.phoenix.sonic"));
                    }
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.phoenix.fly"));
                }

                if (player.getAbilities().flying) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.phoenix.creative_fly"));
                }

                ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                CompoundTag chestNBT = chestplate.getTag();
                if (chestNBT != null && chestNBT.getInt("wingFlapTick") > 0) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.phoenix.flap"));
                }

                return event.setAndContinue(RawAnimation.begin()
                        .thenPlay("animation.phoenix.fold"));
            }
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.phoenix.idle"));
        }));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private PhoenixArmorRenderer renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot,
                                                                   HumanoidModel<?> original) {
                if (itemStack.getItem() instanceof PhoenixArmorItem phoenixItem) {

                    if (this.renderer == null) {
                        this.renderer = new PhoenixArmorRenderer(phoenixItem);
                    }

                    this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                    return this.renderer;
                }
                return original;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
