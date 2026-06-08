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
 * Writes or updates a node entry block in a chapter .yml file.
 *
 * Used by QuestCreatorScreen when saving Chapter Node tab edits.
 *
 * If the quest is already listed in the chapter, its block is replaced.
 * If not, a new node block is appended to the nodes: list.
 */
public class ChapterYamlWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChapterYamlWriter.class);

    public static void writeNodeEntry(
                                      ResourceLocation chapterId,
                                      ResourceLocation questId,
                                      String shape,
                                      int x, int y,
                                      boolean visible,
                                      List<ResourceLocation> dependsOn) {
        Path chapterFile = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("phoenix_chronicles")
                .resolve("chapters")
                .resolve(chapterId.getPath() + ".yml");

        try {
            List<String> lines;
            if (Files.exists(chapterFile)) {
                lines = new ArrayList<>(Files.readAllLines(chapterFile, StandardCharsets.UTF_8));
            } else {
                // Create a minimal chapter file if none exists
                lines = new ArrayList<>();
                lines.add("id: " + chapterId.getPath());
                lines.add("display_name: \"" + chapterId.getPath() + "\"");
                lines.add("category: MAIN");
                lines.add("");
                lines.add("nodes:");
                Files.createDirectories(chapterFile.getParent());
            }

            String questPath = questId.getPath();

            // Build the replacement block
            List<String> block = buildNodeBlock(questPath, shape, x, y, visible, dependsOn);

            // Check if the quest already has a block in the file
            int existingStart = findNodeBlockStart(lines, questPath);
            if (existingStart >= 0) {
                // Replace existing block
                int existingEnd = findNodeBlockEnd(lines, existingStart);
                lines.subList(existingStart, existingEnd).clear();
                lines.addAll(existingStart, block);
            } else {
                // Ensure "nodes:" key exists
                if (!lines.contains("nodes:")) {
                    lines.add("");
                    lines.add("nodes:");
                }
                // Append after the last line
                lines.add("");
                lines.addAll(block);
            }

            Files.write(chapterFile, lines, StandardCharsets.UTF_8);
            LOGGER.info("[Chronicles] Written node entry for {} in chapter {}", questPath, chapterId.getPath());

        } catch (IOException e) {
            LOGGER.error("[Chronicles] Failed to write chapter node for {} in {}", questId, chapterId, e);
        }
    }

    // -------------------------------------------------------------------------

    private static List<String> buildNodeBlock(
                                               String questPath, String shape, int x, int y, boolean visible,
                                               List<ResourceLocation> deps) {
        List<String> block = new ArrayList<>();
        block.add("  - quest: " + questPath);
        block.add("    shape: " + shape);
        block.add("    position: " + x + ", " + y);
        block.add("    visible: " + visible);

        if (!deps.isEmpty()) {
            block.add("    depends_on:");
            for (ResourceLocation dep : deps) {
                block.add("      - " + dep.getPath());
            }
        }

        return block;
    }

    /**
     * Returns the line index of " - quest: <questPath>" or -1 if not found.
     */
    private static int findNodeBlockStart(List<String> lines, String questPath) {
        String target = "  - quest: " + questPath;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("- quest: " + questPath) || lines.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the exclusive end index of the node block starting at startIdx.
     * A block ends when we hit another " - quest:" line, a top-level key, or EOF.
     */
    private static int findNodeBlockEnd(List<String> lines, int startIdx) {
        for (int i = startIdx + 1; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) continue;
            // Another node entry, or a top-level key — block is over
            if (trimmed.startsWith("- quest:") || (leadingSpaces(lines.get(i)) == 0 && !trimmed.startsWith("-"))) {
                return i;
            }
        }
        return lines.size();
    }

    private static int leadingSpaces(String line) {
        int c = 0;
        for (char ch : line.toCharArray()) {
            if (ch == ' ') c++;
            else break;
        }
        return c;
    }
}
