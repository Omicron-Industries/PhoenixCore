package net.phoenix.core.integration.ponder.api;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GTPonderContext {

    public final ExtendedSceneBuilder scene;
    public final SceneBuildingUtil util;
    private final Map<String, Object> sceneOptions;

    private final int defaultTextDuration;
    private final int defaultCueDuration;
    private final int defaultFlowDuration;

    public GTPonderContext(ExtendedSceneBuilder scene, SceneBuildingUtil util, Map<String, Object> sceneOptions) {
        this.scene = scene;
        this.util = util;
        this.sceneOptions = GTPonderAPI.gtPonderOptions(sceneOptions);

        this.defaultTextDuration = (int) GTPonderAPI.gtPonderNumber(this.sceneOptions.get("textDuration"),
                GTPonderAPI.GTPONDER_DEFAULT_TEXT_DURATION);
        this.defaultCueDuration = (int) GTPonderAPI.gtPonderNumber(this.sceneOptions.get("cueDuration"),
                GTPonderAPI.GTPONDER_DEFAULT_CUE_DURATION);
        this.defaultFlowDuration = (int) GTPonderAPI.gtPonderNumber(this.sceneOptions.get("flowDuration"),
                GTPonderAPI.GTPONDER_DEFAULT_FLOW_DURATION);
    }

    public BlockPos block(Object pos, Object id, Map<String, Object> blockOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(blockOptions);
        BlockPos blockPos = GTPonderAPI.gtPonderPos(pos, null, null);
        scene.world().setBlock(blockPos, GTPonderAPI.gtPonderBlockState(id), false);

        if (!Boolean.FALSE.equals(localOptions.get("show"))) {
            scene.world().showSection(
                    util.select().position(blockPos),
                    GTPonderAPI.gtPonderDirection(localOptions.get("direction"), Direction.DOWN));
        }

        return blockPos;
    }

    public List<BlockPos> blocks(List<Object> blocks, Map<String, Object> blockOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(blockOptions);
        List<BlockPos> positions = new ArrayList<>();

        for (Object entry : blocks) {
            GTPonderAPI.BlockEntry block = GTPonderAPI.gtPonderNormalizeBlockEntry(entry);
            scene.world().setBlock(block.pos, GTPonderAPI.gtPonderBlockState(block.id), false);
            positions.add(block.pos);
        }

        Selection selection = GTPonderAPI.gtPonderSelectionForPositions(util, positions);
        if (selection != null && !Boolean.FALSE.equals(localOptions.get("show"))) {
            scene.world().showSection(
                    selection,
                    GTPonderAPI.gtPonderDirection(localOptions.get("direction"), Direction.DOWN));
        }

        return positions;
    }

    public TextElementBuilder text(String text, BlockPos targetPos, Map<String, Object> textOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(textOptions);
        int duration = (int) GTPonderAPI.gtPonderNumber(localOptions.get("duration"), defaultTextDuration);
        PonderPalette palette = GTPonderAPI.gtPonderPalette(
                localOptions.get("palette"),
                GTPonderAPI.gtPonderPalette(localOptions.get("color"), PonderPalette.WHITE));

        // Use ExtendedSceneBuilder.text() which returns TextElementBuilder
        TextElementBuilder overlay = scene.text(duration, text)
                .colored(palette);

        if (targetPos != null) {
            overlay.pointAt(GTPonderAPI.gtPonderBlockCenter(util, targetPos));
            overlay.placeNearTarget();
        }

        return overlay;
    }

    public void outline(String key, BlockPos targetPos, Map<String, Object> outlineOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(outlineOptions);
        int duration = (int) GTPonderAPI.gtPonderNumber(localOptions.get("duration"), defaultTextDuration);
        PonderPalette palette = GTPonderAPI.gtPonderPalette(
                localOptions.get("palette"),
                GTPonderAPI.gtPonderPalette(localOptions.get("color"), PonderPalette.BLUE));

        scene.overlay().showOutline(
                palette,
                key,
                util.select().position(targetPos),
                duration);
    }

    public void line(BlockPos fromPos, BlockPos toPos, Map<String, Object> lineOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(lineOptions);
        int duration = (int) GTPonderAPI.gtPonderNumber(localOptions.get("duration"), defaultFlowDuration);
        PonderPalette palette = GTPonderAPI.gtPonderPalette(
                localOptions.get("palette"),
                GTPonderAPI.gtPonderPalette(localOptions.get("color"), PonderPalette.MEDIUM));

        if (Boolean.FALSE.equals(localOptions.get("big"))) {
            scene.overlay().showLine(
                    palette,
                    GTPonderAPI.gtPonderBlockCenter(util, fromPos),
                    GTPonderAPI.gtPonderBlockCenter(util, toPos),
                    duration);
        } else {
            scene.overlay().showBigLine(
                    palette,
                    GTPonderAPI.gtPonderBlockCenter(util, fromPos),
                    GTPonderAPI.gtPonderBlockCenter(util, toPos),
                    duration);
        }
    }

    public void itemCue(Object itemId, BlockPos targetPos, Map<String, Object> cueOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(cueOptions);
        int duration = (int) GTPonderAPI.gtPonderNumber(localOptions.get("duration"), defaultCueDuration);

        // 1. Use the new showControls from ExtendedSceneBuilder
        // It returns the InputWindowElement directly
        InputWindowElement cue = scene.showControls(
                duration,
                GTPonderAPI.gtPonderBlockTop(util, targetPos, localOptions.get("xOffset"), localOptions.get("zOffset")),
                GTPonderAPI.gtPonderPointing(localOptions.get("pointing"), Pointing.DOWN));

        // 2. Use the .builder() fix to configure the element
        var builder = cue.builder();

        if (Boolean.TRUE.equals(localOptions.get("rightClick"))) {
            builder.rightClick();
        } else {
            builder.leftClick();
        }

        // 3. Attach the item stack
        builder.withItem(GTPonderAPI.gtPonderItemStack(itemId));
    }

    public void itemEntity(Object itemId, BlockPos targetPos, Map<String, Object> entityOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(entityOptions);
        scene.world().createItemEntity(
                GTPonderAPI.gtPonderBlockTop(util, targetPos, localOptions.get("xOffset"), localOptions.get("zOffset")),
                util.vector().of(
                        GTPonderAPI.gtPonderNumber(localOptions.get("velocityX"), 0),
                        GTPonderAPI.gtPonderNumber(localOptions.get("velocityY"), 0.02),
                        GTPonderAPI.gtPonderNumber(localOptions.get("velocityZ"), 0)),
                GTPonderAPI.gtPonderItemStack(itemId));
    }

    public void reveal(List<Object> blocks, Map<String, Object> revealOptions) {
        Map<String, Object> localOptions = GTPonderAPI.gtPonderOptions(revealOptions);
        int idleTime = (int) GTPonderAPI.gtPonderNumber(localOptions.get("idle"),
                GTPonderAPI.GTPONDER_DEFAULT_REVEAL_IDLE);
        Direction direction = GTPonderAPI.gtPonderDirection(localOptions.get("direction"), Direction.DOWN);

        for (Object entry : blocks) {
            blocks(List.of(entry), Map.of("direction", direction));
            scene.idle(idleTime);
        }
    }

    public void idle(int ticks) {
        scene.idle(ticks);
    }
}
