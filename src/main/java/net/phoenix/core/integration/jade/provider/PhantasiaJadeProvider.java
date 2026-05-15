package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class PhantasiaJadeProvider implements IBlockComponentProvider {

    public static final PhantasiaJadeProvider INSTANCE = new PhantasiaJadeProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var mode = PhoenixConfigs.INSTANCE.phantasiaUI.displayMode;
        // Only show if the config allows Jade
        if (mode != PhoenixConfigs.PhantasiaUIConfig.DisplayMode.JADE_ONLY &&
                mode != PhoenixConfigs.PhantasiaUIConfig.DisplayMode.TOOLTIP_JADE)
            return;

        if (accessor.getBlock() instanceof MetaMachineBlock machineBlock) {
            if (machineBlock.getDefinition() instanceof MultiblockMachineDefinition multiDef &&
                    PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) {

                String keyName = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getTranslatedKeyMessage().getString();
                tooltip.add(Component.literal("§6§l» Hold §b[" + keyName + "] §7to Phantasize"));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return PhoenixCore.id("phantasia_jade");
    }
}
