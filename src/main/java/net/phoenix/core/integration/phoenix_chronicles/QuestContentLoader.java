package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Loads quest content files from:
 * config/phoenix_chronicles/quests/<id>.md
 *
 * Each file contains ONLY human-readable content:
 *
 * <pre>
 * ---
 * title: "Signal Lost"
 * ---
 *
 * # Signal Lost
 *
 * The outpost went dark three cycles ago...
 * </pre>
 *
 * No structural data (parent, position, shape, dependencies) belongs here.
 * That all lives in the chapter definition files.
 *
 * Localization: place translated files at
 * config/phoenix_chronicles/quests/lang/<locale>/<id>.md
 * The loader tries the locale file first, falls back to the default.
 */
public class QuestContentLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestContentLoader.class);

    /** Locale used for content resolution. Defaults to en_us. */
    private static String activeLocale = "en_us";

    public static void setActiveLocale(String locale) {
        activeLocale = locale != null ? locale.toLowerCase() : "en_us";
    }

    // -------------------------------------------------------------------------

    public static void reloadAllQuestsFromDisk() {
        QuestTreeRegistry.clear();

        Path questsFolder = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("phoenix_chronicles")
                .resolve("quests");

        if (!Files.exists(questsFolder)) {
            LOGGER.info("[Chronicles] No quests folder found at {}", questsFolder);
            return;
        }

        try (Stream<Path> walk = Files.walk(questsFolder)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    // Skip locale sub-files here; they're resolved on demand in loadQuestContent()
                    .filter(p -> !p.toString().contains("/lang/") && !p.toString().contains("\\lang\\"))
                    .sorted()
                    .forEach(QuestContentLoader::loadQuestFile);
        } catch (IOException e) {
            LOGGER.error("[Chronicles] Failed to walk quests directory", e);
        }
    }

    private static void loadQuestFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            String id = fileName.substring(0, fileName.lastIndexOf('.'));
            ResourceLocation questId = new ResourceLocation("phoenixcore", id.toLowerCase());

            // Resolve locale override if one exists
            Path resolvedFile = resolveLocaleFile(file, id);

            QuestContent content = parseQuestFile(resolvedFile);
            if (content == null) {
                LOGGER.warn("[Chronicles] Skipping quest file with no parseable content: {}", file);
                return;
            }

            // Register a lightweight QuestNode with just id + title + description.
            // Position, shape, dependencies are NOT set here — the chapter loader handles those.
            QuestNode node = new QuestNode(questId, content.title(), content.description());
            QuestTreeRegistry.registerBareQuestNode(node);

        } catch (Exception e) {
            LOGGER.error("[Chronicles] Failed to load quest file: {}", file.getFileName(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Public JIT loader — used by ChronicleOverviewScreen when opening a node
    // -------------------------------------------------------------------------

    /**
     * Reads the full content for a quest by ID on demand (just-in-time).
     * Returns null if the file doesn't exist or fails to parse.
     */
    public static FullQuestData loadFullQuestDetails(ResourceLocation questId) {
        Path questsFolder = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("phoenix_chronicles")
                .resolve("quests");

        // Build the path from the quest id path component
        // e.g. "chapter_1/signal_lost" → quests/chapter_1/signal_lost.md
        Path file = questsFolder.resolve(questId.getPath() + ".md");

        if (!Files.exists(file)) {
            LOGGER.warn("[Chronicles] Quest content file not found: {}", file);
            return null;
        }

        Path resolved = resolveLocaleFile(file, questId.getPath());
        QuestContent content = parseQuestFile(resolved);

        if (content == null) return null;

        // Tasks are still populated from the QuestNode's task list (set by datapacks/config)
        // so we pass an empty list here; QuestTasksScreen reads from the node directly.
        return new FullQuestData(content.title(), content.description(), java.util.List.of());
    }

    // -------------------------------------------------------------------------
    // Parser
    // -------------------------------------------------------------------------

    /**
     * Parses a single quest .md file.
     *
     * Front matter (between --- delimiters) is scanned for "title:".
     * Everything after the second --- delimiter is the body/description.
     * The body may use full markdown including # H1 headings.
     */
    public static QuestContent parseQuestFile(Path file) {
        Component title = null;
        StringBuilder bodyBuilder = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int frontMatterCount = 0; // counts how many "---" lines we've seen
            boolean pastFrontMatter = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.equals("---")) {
                    frontMatterCount++;
                    if (frontMatterCount == 2) pastFrontMatter = true;
                    continue;
                }

                if (!pastFrontMatter) {
                    // Inside front matter — only look for "title:"
                    if (trimmed.startsWith("title:")) {
                        String raw = trimmed.substring("title:".length()).trim();
                        // Strip surrounding quotes if present
                        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
                            raw = raw.substring(1, raw.length() - 1);
                        }
                        title = Component.literal(raw);
                    }
                    // All other front matter keys are intentionally ignored here.
                    // Structure belongs in the chapter file.
                    continue;
                }

                // Body content
                bodyBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.error("[Chronicles] IO error reading quest file: {}", file, e);
            return null;
        }

        if (title == null) {
            // Fall back to filename if front matter had no title
            String name = file.getFileName().toString();
            title = Component.literal(name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name);
        }

        return new QuestContent(title, Component.literal(bodyBuilder.toString().trim()));
    }

    // -------------------------------------------------------------------------
    // Locale resolution
    // -------------------------------------------------------------------------

    private static Path resolveLocaleFile(Path defaultFile, String id) {
        if (activeLocale.equals("en_us")) return defaultFile;

        // e.g. quests/lang/fr_fr/signal_lost.md
        Path langDir = defaultFile.getParent().resolve("lang").resolve(activeLocale);
        String fileName = defaultFile.getFileName().toString();
        Path localeFile = langDir.resolve(fileName);

        if (Files.exists(localeFile)) {
            LOGGER.debug("[Chronicles] Using locale override for {}: {}", id, localeFile);
            return localeFile;
        }
        return defaultFile;
    }

    // -------------------------------------------------------------------------
    // Internal data carriers
    // -------------------------------------------------------------------------

    public record QuestContent(Component title, Component description) {}
}
