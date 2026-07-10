package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.api.CustomNameAccess;

import com.glodblock.github.extendedae.container.ContainerRenamer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = ContainerRenamer.class, remap = false)
public abstract class MixinContainerRenamer {

    @Shadow
    @Final
    @Mutable
    private Consumer<String> setter;

    @Shadow
    @Final
    @Mutable
    private Supplier<Component> getter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void phoenixcore$init(
            int id,
            net.minecraft.world.entity.player.Inventory inv,
            Object host,
            CallbackInfo ci) {
        Object target = phoenixCore$resolveTarget(host, inv.player);

        // FIXED FOR 8.0.0: MetaMachine IS a BlockEntity now.
        // Cast directly to MetaMachine instead of looking for an MMBE wrapper.
        if (!(target instanceof MetaMachine machine)) {
            return;
        }

        if (!(machine instanceof CustomNameAccess access)) {
            return;
        }

        // FIXED FOR 8.0.0: Bind directly to the CustomNameAccess methods on the machine instance
        Consumer<String> setterFn = access::phoenix$setCustomName;

        phoenixCore$apply(setterFn, access::phoenix$getCustomName);
    }

    @Unique
    private void phoenixCore$apply(Consumer<String> setter, Supplier<String> getter) {
        this.setter = setter;
        this.getter = () -> Component.literal(getter.get());
        ((ContainerRenamer) (Object) this).setValidMenu(true);
    }

    @Unique
    private Object phoenixCore$resolveTarget(Object host, net.minecraft.world.entity.player.Player player) {
        if (!host.getClass().getSimpleName().contains("Locator")) {
            return host;
        }

        try {
            // FIXED FOR 8.0.0: Query the locator context using the standard BlockEntity class filter
            var locate = host.getClass()
                    .getMethod("locate", net.minecraft.world.entity.player.Player.class, Class.class);
            Object result = locate.invoke(host, player, BlockEntity.class);
            if (result == null) return host;

            try {
                return result.getClass().getMethod("getTarget").invoke(result);
            } catch (NoSuchMethodException e) {
                return result.getClass().getMethod("host").invoke(result);
            }
        } catch (Exception e) {
            return host;
        }
    }
}