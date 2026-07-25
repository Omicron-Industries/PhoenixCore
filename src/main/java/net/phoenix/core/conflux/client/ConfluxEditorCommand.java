package net.phoenix.core.conflux.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@OnlyIn(Dist.CLIENT)
public final class ConfluxEditorCommand {

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        SuggestionProvider<CommandSourceStack> treeSuggestions = (ctx, builder) ->
                SharedSuggestionProvider.suggest(
                        ResearchTreeRegistry.INSTANCE.getAllTrees().stream()
                                .map(t -> t.id.toString()),
                        builder);

        d.register(literal("conflux")
                .then(literal("editor")
                        .executes(ctx -> {
                            Minecraft.getInstance().setScreen(new ResearchTreeEditorScreen());
                            return 1;
                        }))
                .then(literal("view")
                        .executes(ctx -> {
                            Minecraft.getInstance().setScreen(new ResearchTerminalScreen());
                            return 1;
                        }))
                .then(literal("discipline")
                        .executes(ctx -> {
                            Minecraft.getInstance().setScreen(new DisciplinePickerScreen());
                            return 1;
                        }))
        );

        d.register(literal("confluxeditor")
                .executes(ctx -> {
                    Minecraft.getInstance().setScreen(new ResearchTreeEditorScreen());
                    return 1;
                }));
    }

    private ConfluxEditorCommand() {}
}
