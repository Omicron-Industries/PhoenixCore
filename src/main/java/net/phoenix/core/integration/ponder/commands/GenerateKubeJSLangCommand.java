package net.phoenix.core.integration.ponder.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.ponder.PhoenixPonderLang;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

public class GenerateKubeJSLangCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        String lang = context.getArgument("lang", String.class);
        PhoenixPonderLang ponderLang = new PhoenixPonderLang();

        CommandSourceStack source = context.getSource();
        if (ponderLang.generate(lang)) {
            source.sendSuccess(() -> Component.literal("Changes detected - New lang file created."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Lang file the same. Nothing created."), false);
        }

        return SINGLE_SUCCESS;
    }
}
