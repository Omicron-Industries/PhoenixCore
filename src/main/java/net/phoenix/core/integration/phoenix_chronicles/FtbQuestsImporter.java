package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FtbQuestsImporter {

    private static final float COORD_SCALE = 80f;

    public record ImportResult(int imported, int skipped, String category, List<String> warnings) {}

    public static ImportResult importDirectory(Path importDir, Path outputDir) {
        if (!Files.exists(importDir)) return new ImportResult(0, 0, "", List.of("Import dir not found: " + importDir));
        int totalImported = 0, totalSkipped = 0;
        List<String> warnings = new ArrayList<>();
        String lastCat = "";
        try (var stream = Files.list(importDir)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (!file.toString().endsWith(".snbt")) continue;
                try {
                    String snbt = Files.readString(file, StandardCharsets.UTF_8);
                    ImportResult r = importChapter(snbt, outputDir);
                    totalImported += r.imported();
                    totalSkipped += r.skipped();
                    warnings.addAll(r.warnings());
                    if (!r.category().isEmpty()) lastCat = r.category();
                } catch (Exception e) {
                    warnings.add("Failed to read " + file.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            warnings.add("Failed to list import dir: " + e.getMessage());
        }
        return new ImportResult(totalImported, totalSkipped, lastCat, warnings);
    }

    public static ImportResult importChapter(String snbt, Path outputDir) throws Exception {
        List<String> warnings = new ArrayList<>();
        CompoundTag chapter = TagParser.parseTag(snbt);

        String rawTitle = chapter.contains("title") ? chapter.getString("title") : "Imported";
        String category = slugify(stripFormatting(rawTitle)).toUpperCase();
        if (category.isEmpty()) category = "IMPORTED";

        ListTag quests = chapter.getList("quests", Tag.TAG_COMPOUND);
        if (quests.isEmpty()) return new ImportResult(0, 0, category, warnings);

        Map<String, String> idToPath = new LinkedHashMap<>();
        Set<String> usedPaths = new HashSet<>();
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            String ftbId = q.getString("id");
            String title = stripFormatting(q.getString("title"));
            String path = uniquePath(ftbId, title, usedPaths);
            idToPath.put(ftbId, path);
            usedPaths.add(path);
        }

        Files.createDirectories(outputDir);
        int imported = 0, skipped = 0;
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            String ftbId = q.getString("id");
            String path = idToPath.get(ftbId);
            try {
                String nodeSnbt = convertQuest(q, idToPath, category);
                Path outFile = outputDir.resolve(path + ".snbt");
                Files.writeString(outFile, nodeSnbt, StandardCharsets.UTF_8);
                imported++;
            } catch (Exception e) {
                warnings.add("Quest " + ftbId + " (" + path + "): " + e.getMessage());
                skipped++;
            }
        }

        return new ImportResult(imported, skipped, category, warnings);
    }

    private static String convertQuest(CompoundTag q, Map<String, String> idToPath, String category) {
        StringBuilder sb = new StringBuilder("{\n");

        String path = idToPath.get(q.getString("id"));

        append(sb, "id", path);

        String title = stripFormatting(q.getString("title"));
        if (!title.isEmpty()) append(sb, "title", escape(title));

        String subtitle = stripFormatting(q.getString("subtitle"));
        if (!subtitle.isEmpty()) append(sb, "subtitle", escape(subtitle));

        String desc = buildDescription(q.getList("description", Tag.TAG_STRING));
        if (!desc.isEmpty()) append(sb, "description", escape(desc));

        append(sb, "category", category);

        append(sb, "shape", mapShape(q.getString("shape")));

        int px = q.contains("x") ? (int) Math.round(q.getDouble("x") * COORD_SCALE) : 40;
        int py = q.contains("y") ? (int) Math.round(q.getDouble("y") * COORD_SCALE) : 70;
        sb.append("    positionX: ").append(px).append("\n");
        sb.append("    positionY: ").append(py).append("\n");

        if (q.contains("icon")) {
            String iconId = extractItemId(q.get("icon"));
            if (!iconId.isEmpty() && !iconId.equals("minecraft:air")) {
                append(sb, "icon_item", iconId);
            }
        }

        ListTag deps = q.getList("dependencies", Tag.TAG_STRING);
        if (!deps.isEmpty()) {
            sb.append("    prerequisites: [\n");
            for (int i = 0; i < deps.size(); i++) {
                String depFtbId = deps.getString(i);
                String depPath = idToPath.get(depFtbId);
                if (depPath == null) continue;
                sb.append("        {id: \"").append(depPath).append("\", required: true}\n");
            }
            sb.append("    ]\n");
        }

        boolean oneCompleted = "one_completed".equals(q.getString("dependency_requirement"));
        boolean hasMinDeps = q.contains("min_required_dependencies");
        boolean requireAll = !oneCompleted && !hasMinDeps;
        sb.append("    require_all_prereqs: ").append(requireAll).append("\n");

        if (hasMinDeps) {
            sb.append("    optional_prereq_min_count: ").append(q.getInt("min_required_dependencies")).append("\n");
        }

        ListTag ftbTasks = q.getList("tasks", Tag.TAG_COMPOUND);
        if (!ftbTasks.isEmpty()) {
            sb.append("    tasks: [\n");
            for (int i = 0; i < ftbTasks.size(); i++) {
                String taskSnbt = convertTask(ftbTasks.getCompound(i), path, i);
                if (taskSnbt != null) sb.append("        ").append(taskSnbt).append("\n");
            }
            sb.append("    ]\n");
        }

        ListTag ftbRewards = q.getList("rewards", Tag.TAG_COMPOUND);
        if (!ftbRewards.isEmpty()) {
            sb.append("    rewards: [\n");
            for (int i = 0; i < ftbRewards.size(); i++) {
                String rewardSnbt = convertReward(ftbRewards.getCompound(i));
                if (rewardSnbt != null) sb.append("        ").append(rewardSnbt).append("\n");
            }
            sb.append("    ]\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String convertTask(CompoundTag t, String questPath, int idx) {
        String type = t.getString("type");
        String taskId = "phoenixcore:" + questPath + "_task_" + idx;
        boolean optional = t.getBoolean("optional_task");

        return switch (type) {
            case "item" -> {
                Tag itemTag = t.get("item");
                String itemId = itemTag != null ? extractItemId(itemTag) : "minecraft:air";
                long count = t.contains("count") ? t.getLong("count") : 1L;
                if (count <= 0) count = 1;
                
                String desc = t.contains("title") ? stripFormatting(t.getString("title")) :
                        itemId.substring(itemId.lastIndexOf(':') + 1).replace('_', ' ');
                yield "{type: \"item_check\", task_id: \"" + taskId + "\"" + ", item_id: \"" + itemId + "\"" +
                        ", count: " + count + ", consume: false" + (optional ? ", optional: true" : "") +
                        ", description: \"" + escape(desc) + "\"}";
            }
            case "checkmark" -> {
                String desc = t.contains("title") ? stripFormatting(t.getString("title")) : "Complete";
                yield "{type: \"checkmark\", task_id: \"" + taskId + "\"" + ", description: \"" + escape(desc) + "\"}";
            }
            default -> null; 
        };
    }

    private static String convertReward(CompoundTag r) {
        String type = r.getString("type");
        if (!"item".equals(type)) return null; 
        String itemId = r.getString("item");
        if (itemId.isEmpty()) return null;
        int count = r.contains("count") ? r.getInt("count") : 1;
        if (count <= 0) count = 1;
        return "{type: \"item\", item_id: \"" + itemId + "\", count: " + count + "}";
    }

    private static String extractItemId(Tag tag) {
        if (tag == null) return "minecraft:air";
        if (tag.getId() == Tag.TAG_STRING) return tag.getAsString();
        if (tag.getId() != Tag.TAG_COMPOUND) return "minecraft:air";

        CompoundTag ct = (CompoundTag) tag;
        String id = ct.getString("id");

        if ("itemfilters:or".equals(id)) {
            ListTag items = ct.getCompound("tag").getList("items", Tag.TAG_COMPOUND);
            if (!items.isEmpty()) return items.getCompound(0).getString("id");
        }
        
        if ("itemfilters:tag".equals(id)) {
            String tagValue = ct.getCompound("tag").getString("value");
            return tagValue.isEmpty() ? "minecraft:air" : "minecraft:air"; 
        }
        return id.isEmpty() ? "minecraft:air" : id;
    }

    private static String buildDescription(ListTag lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.getString(i);
            
            if (line.startsWith("{@pagebreak}") || line.startsWith("{image:")) continue;
            line = stripFormatting(line).trim();
            if (line.isEmpty()) {
                if (sb.length() > 0 && !sb.toString().endsWith(" ")) sb.append(" ");
            } else {
                if (sb.length() > 0 && !sb.toString().endsWith(" ")) sb.append(" ");
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    private static String mapShape(String ftb) {
        return switch (ftb == null ? "" : ftb.toLowerCase()) {
            case "gear" -> "STAR";
            case "circle" -> "CIRCLE";
            case "diamond" -> "DIAMOND";
            case "hexagon" -> "HEXAGON";
            case "pentagon" -> "PENTAGON";
            default -> "SQUARE"; 
        };
    }

    private static String uniquePath(String hexId, String title, Set<String> usedPaths) {
        String base = titleSlug(title);
        if (base.isEmpty()) base = "q_" + hexId.toLowerCase();
        if (!usedPaths.contains(base)) return base;
        
        String candidate = base + "_" + hexId.substring(0, 4).toLowerCase();
        int n = 2;
        while (usedPaths.contains(candidate)) candidate = base + "_" + (n++);
        return candidate;
    }

    private static String titleSlug(String title) {
        if (title == null || title.isBlank()) return "";
        String s = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "_");
        return s.length() > 48 ? s.substring(0, 48).replaceAll("_+$", "") : s;
    }

    private static String slugify(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    public static String stripFormatting(String s) {
        if (s == null) return "";
        return s.replaceAll("&#[0-9A-Fa-f]{6}", "")
                .replaceAll("&[0-9a-fklmnorA-FKLMNORxX]", "")
                .replaceAll(">\\?", "") 
                .trim();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void append(StringBuilder sb, String key, String value) {
        sb.append("    ").append(key).append(": \"").append(value).append("\"\n");
    }
}
