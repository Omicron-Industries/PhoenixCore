package net.phoenix.core.client.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityInteractionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();

        // 1. Intercept immediately if they are holding our spray can tool
        if (stack.getItem() instanceof ChameleonSprayCanItem) {

            // 2. Check if the targeted entity is a Wolf or Cat
            if (event.getTarget() instanceof Wolf wolf) {
                // Only allow color changes if the wolf is actually tamed!
                if (wolf.isTame()) {
                    DyeColor currentColor = ChameleonSprayCanBehaviour.getColor(stack);
                    if (currentColor != null) {
                        if (!event.getLevel().isClientSide()) {
                            wolf.setCollarColor(currentColor);
                        }
                        // Successfully intercepted! Cancel the event so they don't sit down.
                        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                        event.setCanceled(true);
                    }
                }
            } else if (event.getTarget() instanceof Cat cat) {
                // Only allow color changes if the cat is actually tamed!
                if (cat.isTame()) {
                    DyeColor currentColor = ChameleonSprayCanBehaviour.getColor(stack);
                    if (currentColor != null) {
                        if (!event.getLevel().isClientSide()) {
                            cat.setCollarColor(currentColor);
                        }
                        // Successfully intercepted! Cancel the event so they don't stand up/sit down.
                        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
