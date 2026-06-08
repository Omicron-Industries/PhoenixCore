package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class QuestRegistry {

    private static final Map<ResourceLocation, QuestStub> INDEX = new HashMap<>();

    public static void indexConfigFolder(Path configDir) throws IOException {
        INDEX.clear();
        try (var stream = Files.walk(configDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    ResourceLocation id = null;
                    String category = "MAIN";
                    ResourceLocation parent = null;
                    String shape = "SQUARE";
                    int x = 0, y = 0;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("=== DATA ===")) break; // STOP READING HERE! Save RAM.
                        if (line.isEmpty() || line.startsWith("#") || line.equals("=== META ===")) continue;

                        String[] parts = line.split(":", 2);
                        if (parts.length < 2) continue;
                        String key = parts[0].trim().toLowerCase();
                        String value = parts[1].trim();

                        switch (key) {
                            case "id" -> id = new ResourceLocation("phoenixcore", value);
                            case "category" -> category = value.toUpperCase();
                            case "parent" -> parent = value.equals("none") ? null :
                                    new ResourceLocation("phoenixcore", value);
                            case "shape" -> shape = value.toUpperCase();
                            case "position" -> {
                                String[] coords = value.split(",");
                                if (coords.length == 2) {
                                    x = Integer.parseInt(coords[0].trim());
                                    y = Integer.parseInt(coords[1].trim());
                                }
                            }
                        }
                    }

                    if (id != null) {
                        INDEX.put(id, new QuestStub(id, category, parent, shape, x, y, path));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
