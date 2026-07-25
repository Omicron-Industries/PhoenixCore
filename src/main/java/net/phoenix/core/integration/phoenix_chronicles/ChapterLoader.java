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
import java.util.stream.Stream;

public class ChapterLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChapterLoader.class);

    public static void reloadAllChaptersFromDisk() {
        ChapterRegistry.clear();

        Path chaptersFolder = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("phoenix_chronicles")
                .resolve("chapters");

        if (!Files.exists(chaptersFolder)) {
            LOGGER.info("[Chronicles] No chapters folder found at {}", chaptersFolder);
            return;
        }

        try (Stream<Path> walk = Files.walk(chaptersFolder)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(ChapterLoader::loadChapterFile);
        } catch (IOException e) {
            LOGGER.error("[Chronicles] Failed to walk chapters directory", e);
        }
    }

    private static void loadChapterFile(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            ChapterDefinition chapter = parseChapter(lines, file.getFileName().toString());
            if (chapter != null) {
                ChapterRegistry.register(chapter);
                LOGGER.info("[Chronicles] Loaded chapter: {}", chapter.getId());
            }
        } catch (IOException e) {
            LOGGER.error("[Chronicles] Failed to read chapter file: {}", file.getFileName(), e);
        }
    }

    private static ChapterDefinition parseChapter(List<String> lines, String fileName) {
        String id = null;
        String displayName = null;
        String category = "MAIN";

        List<ChapterDefinition.ChapterNodeEntry> nodes = new ArrayList<>();

        String currentQuestId = null;
        String currentShape = "SQUARE";
        int currentX = 0, currentY = 0;
        boolean currentVisible = true;
        List<ResourceLocation> currentDeps = new ArrayList<>();

        boolean inNodes = false;
        boolean inDependsOn = false;

        for (String rawLine : lines) {

            int indent = leadingSpaces(rawLine);
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) continue;

            if (indent == 0) {
                inNodes = false;
                inDependsOn = false;

                if (line.equals("nodes:")) {

                    if (currentQuestId != null) {
                        nodes.add(buildEntry(currentQuestId, currentShape, currentX, currentY, currentVisible,
                                currentDeps));
                        currentQuestId = null;
                        currentDeps = new ArrayList<>();
                    }
                    inNodes = true;
                    continue;
                }

                String[] kv = splitKV(line);
                if (kv == null) continue;
                switch (kv[0]) {
                    case "id" -> id = kv[1];
                    case "display_name" -> displayName = unquote(kv[1]);
                    case "category" -> category = kv[1].toUpperCase();
                }
                continue;
            }

            if (inNodes && indent == 2 && line.startsWith("- ")) {

                if (currentQuestId != null) {
                    nodes.add(
                            buildEntry(currentQuestId, currentShape, currentX, currentY, currentVisible, currentDeps));
                }

                currentQuestId = null;
                currentShape = "SQUARE";
                currentX = 0;
                currentY = 0;
                currentVisible = true;
                currentDeps = new ArrayList<>();
                inDependsOn = false;

                String afterMarker = line.substring(2).trim();
                String[] kv = splitKV(afterMarker);
                if (kv != null && kv[0].equals("quest")) {
                    currentQuestId = kv[1];
                }
                continue;
            }

            if (inNodes && indent == 4 && currentQuestId != null) {
                if (line.equals("depends_on:")) {
                    inDependsOn = true;
                    continue;
                }
                inDependsOn = false;

                String[] kv = splitKV(line);
                if (kv == null) continue;
                switch (kv[0]) {
                    case "quest" -> currentQuestId = kv[1];
                    case "shape" -> currentShape = kv[1].toUpperCase();
                    case "visible" -> currentVisible = kv[1].equalsIgnoreCase("true");
                    case "position" -> {
                        String[] coords = kv[1].split(",");
                        if (coords.length == 2) {
                            currentX = parseInt(coords[0].trim(), 0);
                            currentY = parseInt(coords[1].trim(), 0);
                        }
                    }
                }
                continue;
            }

            if (inNodes && inDependsOn && indent == 6 && line.startsWith("- ")) {
                String depId = line.substring(2).trim();
                currentDeps.add(new ResourceLocation("phoenixcore", depId));
            }
        }

        if (currentQuestId != null) {
            nodes.add(buildEntry(currentQuestId, currentShape, currentX, currentY, currentVisible, currentDeps));
        }

        if (id == null) {

            id = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        }
        if (displayName == null) displayName = id;

        return new ChapterDefinition(
                new ResourceLocation("phoenixcore", id.toLowerCase()),
                displayName,
                category,
                nodes);
    }

    private static ChapterDefinition.ChapterNodeEntry buildEntry(
                                                                 String questId, String shape, int x, int y,
                                                                 boolean visible, List<ResourceLocation> deps) {
        return new ChapterDefinition.ChapterNodeEntry(
                new ResourceLocation("phoenixcore", questId.toLowerCase()),
                shape, x, y, visible,
                List.copyOf(deps));
    }

    private static String[] splitKV(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) return null;
        String key = line.substring(0, colon).trim().toLowerCase();
        String val = line.substring(colon + 1).trim();
        if (key.isEmpty()) return null;
        return new String[] { key, val };
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
