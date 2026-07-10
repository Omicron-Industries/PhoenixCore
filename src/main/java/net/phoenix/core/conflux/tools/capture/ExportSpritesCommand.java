package net.phoenix.core.conflux.tools.capture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * /exportsprites                  — reload hot bakers, bake everything
 * /exportsprites reload           — reload hot bakers only (no export)
 * /exportsprites <id>             — bake one specific sprite
 * /exportsprites <id> <path>      — bake to a custom output directory
 *
 * Hot bakers are .java files dropped in {@code run/sprite_bakers/} — they get
 * compiled and registered automatically before every export run.
 */
public final class ExportSpritesCommand {

    public static void register(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("exportsprites")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Reloading hot bakers + exporting all…"), false);
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        int loaded = safeReload(ctx.getSource());
                        if (loaded > 0)
                            sendOverlay("§aHot bakers loaded: " + loaded);
                        InGameSpriteExporter.exportAll(InGameSpriteExporter.defaultOutputDir());
                    });
                    return 1;
                })
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                                int loaded = safeReload(ctx.getSource());
                                String msg = loaded > 0
                                        ? "§aLoaded " + loaded + " hot baker(s). Available: " + String.join(", ", SpriteCaptureRegistry.ids())
                                        : "§7No hot bakers found in " + HotBakerLoader.BAKERS_DIR;
                                sendOverlay(msg);
                            });
                            return 1;
                        })
                )
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            List<String> ids = new ArrayList<>(SpriteCaptureRegistry.ids());
                            ids.add("reload");
                            return SharedSuggestionProvider.suggest(ids, b);
                        })
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                                safeReload(null);
                                SpriteCaptureRegistry.get(id).ifPresentOrElse(
                                        b -> InGameSpriteExporter.export(b, InGameSpriteExporter.defaultOutputDir()),
                                        () -> sendOverlay("§cUnknown baker: '" + id + "'. Available: " + SpriteCaptureRegistry.ids())
                                );
                            });
                            return 1;
                        })
                        .then(Commands.argument("outputPath", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String id   = StringArgumentType.getString(ctx, "id");
                                    String path = StringArgumentType.getString(ctx, "outputPath");
                                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                                        safeReload(null);
                                        SpriteCaptureRegistry.get(id).ifPresentOrElse(
                                                b -> InGameSpriteExporter.export(b, Paths.get(path)),
                                                () -> sendOverlay("§cUnknown baker: '" + id + "'")
                                        );
                                    });
                                    return 1;
                                })
                        )
                )
        );
    }

    /** Runs HotBakerLoader.reload(), catching errors so a bad baker file doesn't crash export. */
    private static int safeReload(CommandSourceStack source) {
        try {
            return HotBakerLoader.reload();
        } catch (Exception e) {
            String msg = "§cHot baker compile error: " + e.getMessage();
            sendOverlay(msg);
            if (source != null) source.sendFailure(Component.literal(msg));
            return 0;
        }
    }

    private static void sendOverlay(String msg) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gui != null) mc.gui.setOverlayMessage(Component.literal(msg), false);
    }
}
