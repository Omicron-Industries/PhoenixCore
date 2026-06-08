package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes position changes back to a chapter's .yml file on disk.
 *
 * This replaces the old saveNodeCoordinatesToDisk() in ChronicleOverviewScreen,
 * which incorrectly wrote coordinates back to the quest's .md file.
 *
 * Now that position is scoped to a chapter, changes are persisted to:
 * config/phoenix_chronicles/chapters/<chapter_id>.yml
 */
public class ChapterPositionWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChapterPositionWriter.class);

    /**
     * Updates the "position:" line for the given quest inside the given chapter's
     * .yml file. Does nothing if the file or node entry cannot be found.
     *
     * @param chapterId The chapter whose file to update
     * @param questId   The quest whose position changed
     * @param x         New canvas X
     * @param y         New canvas Y
     */
    public static void savePosition(ResourceLocation chapterId, ResourceLocation questId, int x, int y) {
        Path chaptersFolder = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("phoenix_chronicles")
                .resolve("chapters");

        Path chapterFile = chaptersFolder.resolve(chapterId.getPath() + ".yml");

        if (!Files.exists(chapterFile)) {
            LOGGER.warn("[Chronicles] Cannot save position — chapter file not found: {}", chapterFile);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(chapterFile, StandardCharsets.UTF_8);
            List<String> updated = rewritePosition(lines, questId.getPath(), x, y);
            Files.write(chapterFile, updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[Chronicles] Failed to save position for quest {} in chapter {}", questId, chapterId, e);
        }
    }

    /**
     * Scans the YAML lines for the node block matching questId and replaces
     * its "position:" entry. If no position line exists in that block, one
     * is inserted after the "quest:" line.
     */
    private static List<String> rewritePosition(List<String> lines, String targetQuestPath, int x, int y) {
        List<String> out = new ArrayList<>(lines.size());

        boolean inTargetNode = false;
        boolean positionWritten = false;
        String newPositionLine = "    position: " + x + ", " + y;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.trim();

            // Detect the node entry that references our quest
            if (trimmed.startsWith("- quest:")) {
                String questVal = trimmed.substring("- quest:".length()).trim();
                inTargetNode = questVal.equals(targetQuestPath);
                positionWritten = false;
            }

            // Reset when we hit the next node entry (outside our block)
            if (inTargetNode && trimmed.startsWith("- ") && !trimmed.startsWith("- quest: " + targetQuestPath)) {
                inTargetNode = false;
            }

            if (inTargetNode && trimmed.startsWith("position:")) {
                // Replace existing position line
                out.add(newPositionLine);
                positionWritten = true;
                continue;
            }

            out.add(raw);

            // If we just wrote the "- quest:" marker and haven't inserted position yet,
            // insert it right after if the next line is not a position line
            if (inTargetNode && !positionWritten && trimmed.startsWith("- quest:")) {
                // Peek ahead — if the next non-empty line isn't "position:", insert one
                boolean nextIsPosition = false;
                for (int j = i + 1; j < lines.size(); j++) {
                    String peek = lines.get(j).trim();
                    if (peek.isEmpty()) continue;
                    nextIsPosition = peek.startsWith("position:");
                    break;
                }
                if (!nextIsPosition) {
                    out.add(newPositionLine);
                    positionWritten = true;
                }
            }
        }

        return out;
    }
}
