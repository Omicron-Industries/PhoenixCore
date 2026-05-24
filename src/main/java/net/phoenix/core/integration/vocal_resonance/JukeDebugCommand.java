package net.phoenix.core.integration.vocal_resonance;

public class JukeDebugCommand {
    /*
     * public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
     * dispatcher.register(Commands.literal("juketest")
     * .requires(src -> src.hasPermission(2)) // op-only
     * 
     * // 1. /juketest ping
     * .then(Commands.literal("ping")
     * .executes(ctx -> {
     * CommandSourceStack src = ctx.getSource();
     * BlockPos pos = BlockPos.containing(src.getPosition());
     * ServerLevel level = src.getLevel();
     * 
     * ResourceLocation soundLoc = new ResourceLocation("minecraft", "block.note_block.pling");
     * 
     * PhoenixNetwork.CHANNEL.send(
     * PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
     * new S2CPlaySoundPacket(pos, soundLoc, 1.0f, 1.0f));
     * 
     * src.sendSuccess(() -> Component.literal(
     * "§aSent S2CPlaySoundPacket for §fminecraft:block.note_block.pling §aat §f" + pos
     * + "§a to TRACKING_CHUNK. Did you hear it?"), false);
     * return 1;
     * }))
     * 
     * // 2. /juketest play <sound> [volume] [pitch]
     * .then(Commands.literal("play")
     * .then(Commands.argument("sound", ResourceLocationArgument.id()) // Changed to ResourceLocationArgument
     * .executes(ctx -> playSound(ctx.getSource(),
     * ResourceLocationArgument.getId(ctx, "sound"),
     * 1.0f, 1.0f))
     * .then(Commands.argument("volume", FloatArgumentType.floatArg(0.01f, 4.0f))
     * .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5f, 2.0f))
     * .executes(ctx -> playSound(ctx.getSource(),
     * ResourceLocationArgument.getId(ctx, "sound"),
     * FloatArgumentType.getFloat(ctx, "volume"),
     * FloatArgumentType.getFloat(ctx, "pitch")))))))
     * 
     * // 3. /juketest range <sound> <radius>
     * .then(Commands.literal("range")
     * .then(Commands.argument("sound", ResourceLocationArgument.id()) // Changed to ResourceLocationArgument
     * .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
     * .executes(ctx -> {
     * CommandSourceStack src = ctx.getSource();
     * BlockPos pos = BlockPos.containing(src.getPosition());
     * ServerLevel level = src.getLevel();
     * ResourceLocation soundLoc = ResourceLocationArgument.getId(ctx, "sound");
     * int radius = IntegerArgumentType.getInteger(ctx, "radius");
     * 
     * PhoenixNetwork.CHANNEL.send(
     * PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
     * pos.getX(), pos.getY(), pos.getZ(),
     * radius,
     * level.dimension())),
     * new S2CPlaySoundPacket(pos, soundLoc, 1.0f, 1.0f));
     * 
     * src.sendSuccess(() -> Component.literal(
     * "§aSent S2CPlaySoundPacket for §f" + soundLoc
     * + "§a with radius §f" + radius + "§a. Move away and check range."), false);
     * return 1;
     * }))))
     * );
     * }
     * 
     * private static int playSound(CommandSourceStack src, ResourceLocation soundLoc, float volume, float pitch) {
     * BlockPos pos = BlockPos.containing(src.getPosition());
     * ServerLevel level = src.getLevel();
     * 
     * PhoenixNetwork.CHANNEL.send(
     * PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
     * new S2CPlaySoundPacket(pos, soundLoc, volume, pitch));
     * 
     * src.sendSuccess(() -> Component.literal(
     * "§aSent S2CPlaySoundPacket: §f" + soundLoc
     * + " §7(vol=" + volume + ", pitch=" + pitch + ")"), false);
     * return 1;
     * }
     * 
     */
}
