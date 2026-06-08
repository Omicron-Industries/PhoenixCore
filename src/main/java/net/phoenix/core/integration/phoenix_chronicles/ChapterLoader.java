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

/**
 * Loads chapter definition files from:
 * config/phoenix_chronicles/chapters/<chapter_id>.yml
 *
 * YAML is parsed with a minimal hand-written reader to avoid adding a
 * library dependency. The format is intentionally simple (no nesting
 * beyond the nodes list).
 *
 * Expected file shape:
 * 
 * <pre>
 * id: chapter_1
 * display_name: "Chapter I — Awakening"
 * category: CHAPTER_1
 *
 * nodes:
 *   - quest: signal_lost
 *     shape: SQUARE
 *     position: 120, 80
 *     visible: true
 *
 *   - quest: restore_power
 *     shape: CIRCLE
 *     position: 200, 80
 *     visible: true
 *     depends_on:
 *       - signal_lost
 * </pre>
 */
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
                    .sorted() // deterministic load order
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

    // -------------------------------------------------------------------------
    // Minimal YAML parser
    // -------------------------------------------------------------------------

    private static ChapterDefinition parseChapter(List<String> lines, String fileName) {
        String id = null;
        String displayName = null;
        String category = "MAIN";

        List<ChapterDefinition.ChapterNodeEntry> nodes = new ArrayList<>();

        // State for the current node block being parsed
        String currentQuestId = null;
        String currentShape = "SQUARE";
        int currentX = 0, currentY = 0;
        boolean currentVisible = true;
        List<ResourceLocation> currentDeps = new ArrayList<>();

        boolean inNodes = false;
        boolean inDependsOn = false;

        for (String rawLine : lines) {
            // Preserve indent level before trimming
            int indent = leadingSpaces(rawLine);
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) continue;

            // Top-level keys (indent 0)
            if (indent == 0) {
                inNodes = false;
                inDependsOn = false;

                if (line.equals("nodes:")) {
                    // Flush any in-progress node before switching sections
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

            // Node list entries (indent 2 — list item marker "- ")
            if (inNodes && indent == 2 && line.startsWith("- ")) {
                // Flush the previous node
                if (currentQuestId != null) {
                    nodes.add(
                            buildEntry(currentQuestId, currentShape, currentX, currentY, currentVisible, currentDeps));
                }
                // Start fresh
                currentQuestId = null;
                currentShape = "SQUARE";
                currentX = 0;
                currentY = 0;
                currentVisible = true;
                currentDeps = new ArrayList<>();
                inDependsOn = false;

                // The "- " marker may carry the first key inline: "- quest: signal_lost"
                String afterMarker = line.substring(2).trim();
                String[] kv = splitKV(afterMarker);
                if (kv != null && kv[0].equals("quest")) {
                    currentQuestId = kv[1];
                }
                continue;
            }

            // Node properties (indent 4)
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

            // depends_on list entries (indent 6)
            if (inNodes && inDependsOn && indent == 6 && line.startsWith("- ")) {
                String depId = line.substring(2).trim();
                currentDeps.add(new ResourceLocation("phoenixcore", depId));
            }
        }

        // Flush last node
        if (currentQuestId != null) {
            nodes.add(buildEntry(currentQuestId, currentShape, currentX, currentY, currentVisible, currentDeps));
        }

        if (id == null) {
            // Fall back to filename without extension
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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Splits "key: value" into ["key", "value"], or null if not a valid pair. */
    private static String[] splitKV(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) return null;
        String key = line.substring(0, colon).trim().toLowerCase();
        String val = line.substring(colon + 1).trim();
        if (key.isEmpty()) return null;
        return new String[] { key, val };
    }

    /** Strips surrounding double-quotes if present. */
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
