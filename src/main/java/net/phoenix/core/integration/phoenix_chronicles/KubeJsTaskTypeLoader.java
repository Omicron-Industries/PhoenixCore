package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.phoenix_chronicles.PhoenixTaskRegistry.FieldDef;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ExternalTriggerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code config/phoenix_chronicles/kjs_task_types.json} and registers any
 * task types defined there into {@link PhoenixTaskRegistry}.
 *
 * This lets KubeJS startup scripts define custom task types that appear in the
 * in-game editor dropdown and can be used in quest SNBT — without needing a
 * compile-time KubeJS dependency on the Java side.
 *
 * ── KubeJS startup_scripts/phoenix_tasks.js ─────────────────────────────────
 * 
 * <pre>
 * // Define custom task types visible in the in-game editor
 * const taskTypes = [
 *   {
 *     type_id: "mypack:sun_eaten",
 *     label: "Eat the Sun",
 *     icon: "§c☀",
 *     tooltip: "Complete after eating a star.\nTrigger: mypack:sun_eaten",
 *     // task_type: what underlying task class to use — "external_trigger" is the default
 *     task_type: "external_trigger",
 *     // For external_trigger: pre-fill the trigger_id in the editor
 *     default_trigger_id: "mypack:sun_eaten",
 *     fields: [
 *       {id: "required", label: "Times", type: "integer"}
 *     ]
 *   }
 * ]
 *
 * const file = new java.io.File("config/phoenix_chronicles/kjs_task_types.json")
 * file.getParentFile().mkdirs()
 * file.text = JSON.stringify(taskTypes, null, 2)
 * </pre>
 *
 * ── How KubeJS handles completion ────────────────────────────────────────────
 * 
 * <pre>
 * // server_scripts/quest_triggers.js
 * const QuestAPI = Java.loadClass('net.phoenix.core.integration.phoenix_chronicles.QuestAPI')
 *
 * ForgeEvents.onEvent('...LivingDeathEvent', event => {
 *   if (event.entity.type.registryName.equals('mypack:sun')) {
 *     QuestAPI.fireExternalEvent(event.source.entity, 'mypack:sun_eaten', null)
 *   }
 * })
 * </pre>
 *
 * The task in quest SNBT is just an {@code external_trigger} task:
 * 
 * <pre>
 * tasks: [{type: "external_trigger", trigger_id: "mypack:sun_eaten", required: 1,
 *          task_id: "mypack:eat_sun_task", description: '"Eat the Sun"'}]
 * </pre>
 */
public final class KubeJsTaskTypeLoader {

    private static final String FILE = "kjs_task_types.json";

    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE);
        if (!Files.exists(file)) return;

        List<JsonObject> entries = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) {
                    if (e.isJsonObject()) entries.add(e.getAsJsonObject());
                }
            }
        } catch (Exception e) {
            System.err.println("[Phoenix Chronicles] Failed to load kjs_task_types.json: " + e.getMessage());
            return;
        }

        int registered = 0;
        for (JsonObject obj : entries) {
            try {
                String typeId = obj.get("type_id").getAsString();
                if (typeId.isBlank()) continue;

                // Skip if already registered (built-in or already loaded)
                if (PhoenixTaskRegistry.get(typeId) != null) continue;

                String label = obj.has("label") ? obj.get("label").getAsString() : typeId;
                String icon = obj.has("icon") ? obj.get("icon").getAsString() : "§7◆";
                String tooltip = obj.has("tooltip") ? obj.get("tooltip").getAsString() : label;
                String defaultTriggerId = obj.has("default_trigger_id") ? obj.get("default_trigger_id").getAsString() :
                        typeId;

                // Build field defs
                List<FieldDef> fields = new ArrayList<>();
                if (obj.has("fields") && obj.get("fields").isJsonArray()) {
                    JsonArray farr = obj.getAsJsonArray("fields");
                    for (JsonElement fe : farr) {
                        if (!fe.isJsonObject()) continue;
                        JsonObject fo = fe.getAsJsonObject();
                        String fid = fo.has("id") ? fo.get("id").getAsString() : "field";
                        String flabel = fo.has("label") ? fo.get("label").getAsString() : fid;
                        String ftype = fo.has("type") ? fo.get("type").getAsString() : "text";
                        String fhint = fo.has("hint") ? fo.get("hint").getAsString() : null;
                        FieldDef.FieldType ft = switch (ftype.toLowerCase()) {
                            case "integer", "int" -> FieldDef.FieldType.INTEGER;
                            case "boolean", "bool" -> FieldDef.FieldType.BOOLEAN;
                            case "item_id", "item" -> FieldDef.FieldType.ITEM_ID;
                            case "entity_id", "entity" -> FieldDef.FieldType.ENTITY_ID;
                            case "fluid_id", "fluid" -> FieldDef.FieldType.FLUID_ID;
                            default -> FieldDef.FieldType.TEXT;
                        };
                        fields.add(new FieldDef(fid, flabel, ft, fhint));
                    }
                }

                // All KubeJS-defined task types are backed by ExternalTriggerTask
                final String tid = defaultTriggerId;
                var builder = PhoenixTaskRegistry.register(typeId, tag -> {
                    String trigger = tag.contains("trigger_id") ? tag.getString("trigger_id") : tid;
                    int required = tag.contains("required") ? tag.getInt("required") : 1;
                    ResourceLocation taskId;
                    try {
                        taskId = new ResourceLocation(tag.getString("task_id"));
                    } catch (Exception ex) {
                        taskId = new ResourceLocation("phoenixcore", "kjs_task");
                    }
                    Component desc;
                    try {
                        desc = Component.Serializer.fromJson(tag.getString("description"));
                    } catch (Exception ex) {
                        desc = Component.literal(label);
                    }
                    if (desc == null) desc = Component.literal(label);
                    ExternalTriggerTask t = new ExternalTriggerTask(taskId, desc, trigger, required);
                    // Store the original typeId so serializeNBT writes the right type string
                    t.setKjsTypeId(typeId);
                    return t;
                }).icon(icon).label(label).tooltip(tooltip);

                for (FieldDef f : fields) builder.field(f);
                // Always expose trigger_id and required if no fields specified
                if (fields.isEmpty()) {
                    builder.field(FieldDef.text("trigger_id", "Trigger ID", "e.g. " + defaultTriggerId));
                    builder.field(FieldDef.integer("required", "Count"));
                }

                builder.register();
                registered++;
            } catch (Exception e) {
                System.err.println("[Phoenix Chronicles] Failed to register KubeJS task type: " + e.getMessage());
            }
        }

        if (registered > 0) {
            System.out.println("[Phoenix Chronicles] Registered " + registered + " KubeJS task type(s).");
        }
    }

    private KubeJsTaskTypeLoader() {}
}
