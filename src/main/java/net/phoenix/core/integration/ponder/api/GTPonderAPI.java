package net.phoenix.core.integration.ponder.api;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTPonderAPI {

    private static final Logger LOGGER = LogManager.getLogger("GTPonderAPI");

    public static final String GTPONDER_API_VERSION = "0.1.0";
    public static final ResourceLocation GTPONDER_DEFAULT_STRUCTURE_ID = new ResourceLocation(
            "phoenixcore:blank_48");
    public static final int GTPONDER_DEFAULT_BASE_PLATE_SIZE = 7;
    public static final int GTPONDER_DEFAULT_TEXT_DURATION = 56;
    public static final int GTPONDER_DEFAULT_CUE_DURATION = 36;
    public static final int GTPONDER_DEFAULT_FLOW_DURATION = 34;
    public static final int GTPONDER_DEFAULT_REVEAL_IDLE = 2;

    public static final Map<String, PonderPalette> GTPONDER_PALETTES = new HashMap<>();
    public static final Map<String, Direction> GTPONDER_DIRECTIONS = new HashMap<>();
    public static final Map<String, Pointing> GTPONDER_POINTING = new HashMap<>();

    static {
        // Initialize GTPONDER_PALETTES
        GTPONDER_PALETTES.put("white", PonderPalette.WHITE);
        GTPONDER_PALETTES.put("black", PonderPalette.BLACK);
        GTPONDER_PALETTES.put("red", PonderPalette.RED);
        GTPONDER_PALETTES.put("green", PonderPalette.GREEN);
        GTPONDER_PALETTES.put("blue", PonderPalette.BLUE);
        GTPONDER_PALETTES.put("slow", PonderPalette.SLOW);
        GTPONDER_PALETTES.put("medium", PonderPalette.MEDIUM);
        GTPONDER_PALETTES.put("fast", PonderPalette.FAST);
        GTPONDER_PALETTES.put("input", PonderPalette.INPUT);
        GTPONDER_PALETTES.put("output", PonderPalette.OUTPUT);

        // Initialize GTPONDER_DIRECTIONS
        GTPONDER_DIRECTIONS.put("up", Direction.UP);
        GTPONDER_DIRECTIONS.put("down", Direction.DOWN);
        GTPONDER_DIRECTIONS.put("north", Direction.NORTH);
        GTPONDER_DIRECTIONS.put("south", Direction.SOUTH);
        GTPONDER_DIRECTIONS.put("west", Direction.WEST);
        GTPONDER_DIRECTIONS.put("east", Direction.EAST);

        // Initialize GTPONDER_POINTING
        GTPONDER_POINTING.put("up", Pointing.UP);
        GTPONDER_POINTING.put("down", Pointing.DOWN);
        GTPONDER_POINTING.put("left", Pointing.LEFT);
        GTPONDER_POINTING.put("right", Pointing.RIGHT);
    }

    public static Map<String, Object> gtPonderOptions(Map<String, Object> options) {
        return options == null ? new HashMap<>() : options;
    }

    public static double gtPonderNumber(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            double numberValue = Double.parseDouble(String.valueOf(value));
            return Double.isNaN(numberValue) ? fallback : numberValue;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static ResourceLocation gtPonderResourceLocation(Object id) {
        if (id instanceof ResourceLocation) {
            return (ResourceLocation) id;
        }
        return new ResourceLocation(String.valueOf(id));
    }

    public static BlockPos gtPonderPos(Object x, Object y, Object z) {
        if (x instanceof List) {
            List<?> list = (List<?>) x;
            if (list.size() >= 3) {
                return new BlockPos(
                        (int) gtPonderNumber(list.get(0), 0),
                        (int) gtPonderNumber(list.get(1), 0),
                        (int) gtPonderNumber(list.get(2), 0));
            }
        }
        if (x instanceof BlockPos) {
            return (BlockPos) x;
        }
        if (x != null && x.getClass().getName().contains("BlockPos")) {
            try {
                int bx = (int) x.getClass().getMethod("getX").invoke(x);
                int by = (int) x.getClass().getMethod("getY").invoke(x);
                int bz = (int) x.getClass().getMethod("getZ").invoke(x);
                return new BlockPos(bx, by, bz);
            } catch (Exception e) {
                LOGGER.warn("Failed to convert KJS BlockPos to Java BlockPos: " + e.getMessage());
            }
        }
        return new BlockPos(
                (int) gtPonderNumber(x, 0),
                (int) gtPonderNumber(y, 0),
                (int) gtPonderNumber(z, 0));
    }

    public static BlockState gtPonderBlockState(Object id) {
        ResourceLocation rl = gtPonderResourceLocation(id);
        if (ForgeRegistries.BLOCKS.containsKey(rl)) {
            return ForgeRegistries.BLOCKS.getValue(rl).defaultBlockState();
        }
        LOGGER.warn("Unknown block in GTPonder scene: " + id);
        return Blocks.BARRIER.defaultBlockState();
    }

    public static ItemStack gtPonderItemStack(Object id) {
        if (id instanceof ItemStack) {
            return (ItemStack) id;
        }
        ResourceLocation rl = gtPonderResourceLocation(id);
        if (ForgeRegistries.ITEMS.containsKey(rl)) {
            return new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
        }
        LOGGER.warn("Unknown item in GTPonder scene: " + id);
        return new ItemStack(Blocks.BARRIER.asItem());
    }

    public static PonderPalette gtPonderPalette(Object value, PonderPalette fallback) {
        if (value == null) {
            return fallback != null ? fallback : PonderPalette.WHITE;
        }
        String key = String.valueOf(value).toLowerCase();
        return GTPONDER_PALETTES.getOrDefault(key, fallback != null ? fallback : PonderPalette.WHITE);
    }

    public static Direction gtPonderDirection(Object value, Direction fallback) {
        if (value == null) {
            return fallback != null ? fallback : Direction.DOWN;
        }
        String key = String.valueOf(value).toLowerCase();
        return GTPONDER_DIRECTIONS.getOrDefault(key, fallback != null ? fallback : Direction.DOWN);
    }

    public static Pointing gtPonderPointing(Object value, Pointing fallback) {
        if (value == null) {
            return fallback != null ? fallback : Pointing.DOWN;
        }
        String key = String.valueOf(value).toLowerCase();
        return GTPONDER_POINTING.getOrDefault(key, fallback != null ? fallback : Pointing.DOWN);
    }

    public static Vec3 gtPonderBlockCenter(SceneBuildingUtil util, BlockPos pos) {
        return util.vector().of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static Vec3 gtPonderBlockTop(SceneBuildingUtil util, BlockPos pos, Object xOffset, Object zOffset) {
        return util.vector().of(
                pos.getX() + 0.5 + gtPonderNumber(xOffset, 0),
                pos.getY() + 1.05,
                pos.getZ() + 0.5 + gtPonderNumber(zOffset, 0));
    }

    public static Selection gtPonderSelectionForPositions(SceneBuildingUtil util, List<BlockPos> positions) {
        Selection selection = null;
        for (BlockPos pos : positions) {
            Selection next = util.select().position(pos);
            selection = (selection == null) ? next : selection.add(next);
        }
        return selection;
    }

    public static BlockEntry gtPonderNormalizeBlockEntry(Object entry) {
        if (entry instanceof List) {
            List<?> list = (List<?>) entry;
            if (list.size() >= 4) {
                return new BlockEntry(gtPonderPos(list.get(0), list.get(1), list.get(2)), list.get(3));
            }
        } else if (entry instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) entry;
            Object posObj = map.get("pos");
            if (posObj == null) posObj = map.get("position");
            Object idObj = map.get("id");
            if (idObj == null) idObj = map.get("block");
            return new BlockEntry(gtPonderPos(posObj, null, null), idObj);
        }
        throw new IllegalArgumentException("Invalid block entry format: " + entry);
    }

    public static class BlockEntry {

        public final BlockPos pos;
        public final Object id; // Can be ResourceLocation or String

        public BlockEntry(BlockPos pos, Object id) {
            this.pos = pos;
            this.id = id;
        }
    }
}
