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

public class ChapterPositionWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChapterPositionWriter.class);

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

    private static List<String> rewritePosition(List<String> lines, String targetQuestPath, int x, int y) {
        List<String> out = new ArrayList<>(lines.size());

        boolean inTargetNode = false;
        boolean positionWritten = false;
        String newPositionLine = "    position: " + x + ", " + y;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.trim();

            if (trimmed.startsWith("- quest:")) {
                String questVal = trimmed.substring("- quest:".length()).trim();
                inTargetNode = questVal.equals(targetQuestPath);
                positionWritten = false;
            }

            if (inTargetNode && trimmed.startsWith("- ") && !trimmed.startsWith("- quest: " + targetQuestPath)) {
                inTargetNode = false;
            }

            if (inTargetNode && trimmed.startsWith("position:")) {
                
                out.add(newPositionLine);
                positionWritten = true;
                continue;
            }

            out.add(raw);

            if (inTargetNode && !positionWritten && trimmed.startsWith("- quest:")) {
                
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
