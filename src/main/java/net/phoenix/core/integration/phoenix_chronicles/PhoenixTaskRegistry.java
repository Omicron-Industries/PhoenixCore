package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Central registry for quest task types.
 *
 * Java mod authors register a type id → deserializer mapping with optional editor metadata:
 * <pre>
 *   PhoenixTaskRegistry.register("mymod:eat_sun", EatSunTask::fromTag)
 *       .label("Eat the Sun").icon("§c☀").tooltip("Eat a star.\nTarget: star registry id.")
 *       .register();
 * </pre>
 *
 * KubeJS authors use the PhoenixEvents.registerTask() builder (see PhoenixKubeJSPlugin).
 *
 * Built-in types are registered via {@link #registerBuiltins()} at mod load time.
 */
public final class PhoenixTaskRegistry {

    public record FieldDef(String id, String label, FieldType type, @Nullable String hint) {
        public enum FieldType { TEXT, INTEGER, BOOLEAN, ITEM_ID, ENTITY_ID, FLUID_ID }

        public static FieldDef text(String id, String label) {
            return new FieldDef(id, label, FieldType.TEXT, null);
        }
        public static FieldDef text(String id, String label, String hint) {
            return new FieldDef(id, label, FieldType.TEXT, hint);
        }
        public static FieldDef integer(String id, String label) {
            return new FieldDef(id, label, FieldType.INTEGER, null);
        }
        public static FieldDef itemId(String id, String label) {
            return new FieldDef(id, label, FieldType.ITEM_ID, null);
        }
        public static FieldDef entityId(String id, String label) {
            return new FieldDef(id, label, FieldType.ENTITY_ID, null);
        }
        public static FieldDef fluidId(String id, String label) {
            return new FieldDef(id, label, FieldType.FLUID_ID, null);
        }
        public static FieldDef bool(String id, String label) {
            return new FieldDef(id, label, FieldType.BOOLEAN, null);
        }
    }

    public record TaskEntry(
            String typeId,
            Function<CompoundTag, QuestTask> deserializer,
            @Nullable String editorIcon,
            @Nullable String editorLabel,
            @Nullable String editorTooltip,
            List<FieldDef> fields
    ) {
        public boolean hasEditorMeta() {
            return editorLabel != null;
        }
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private static final Map<String, TaskEntry> REGISTRY = new LinkedHashMap<>();
    // Ordered list for editor dropdown (insertion-order, built-ins first)
    private static final List<TaskEntry> EDITOR_ORDER = new ArrayList<>();

    // ── Registration API ──────────────────────────────────────────────────────

    public static Builder register(String typeId, Function<CompoundTag, QuestTask> deserializer) {
        return new Builder(typeId, deserializer);
    }

    public static final class Builder {
        private final String typeId;
        private final Function<CompoundTag, QuestTask> deserializer;
        private String icon = null;
        private String label = null;
        private String tooltip = null;
        private final List<FieldDef> fields = new ArrayList<>();

        private Builder(String typeId, Function<CompoundTag, QuestTask> deserializer) {
            this.typeId = typeId;
            this.deserializer = deserializer;
        }

        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder tooltip(String tooltip) { this.tooltip = tooltip; return this; }
        public Builder field(FieldDef f) { this.fields.add(f); return this; }

        public void register() {
            TaskEntry entry = new TaskEntry(typeId, deserializer, icon, label, tooltip,
                    Collections.unmodifiableList(new ArrayList<>(fields)));
            REGISTRY.put(typeId, entry);
            if (entry.hasEditorMeta()) EDITOR_ORDER.add(entry);
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    @Nullable
    public static TaskEntry get(String typeId) {
        return REGISTRY.get(typeId);
    }

    /** All types with editor metadata, in registration order (built-ins first). */
    public static List<TaskEntry> getEditorTypes() {
        return Collections.unmodifiableList(EDITOR_ORDER);
    }

    /** Deserialize a task from NBT using the registry. Returns null if type is unknown. */
    @Nullable
    public static QuestTask deserialize(CompoundTag tag) {
        String typeId = tag.getString("type");
        TaskEntry entry = REGISTRY.get(typeId);
        if (entry == null) return null;
        try {
            return entry.deserializer().apply(tag);
        } catch (Exception e) {
            System.err.println("[PhoenixTaskRegistry] Failed to deserialize task type '" + typeId + "': " + e.getMessage());
            return null;
        }
    }

    // ── Built-in registration ─────────────────────────────────────────────────

    private static boolean builtinsRegistered = false;

    public static void registerBuiltins() {
        if (builtinsRegistered) return;
        builtinsRegistered = true;

        register("kill_entity", tag -> {
            KillEntityTask t = new KillEntityTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "pig"), 1, false);
            t.deserializeNBT(tag);
            return t;
        }).icon("§c☠").label("Kill Entity")
          .tooltip("Kill a number of a specific mob type.\nTarget: entity registry id (e.g. minecraft:zombie)")
          .field(FieldDef.entityId("entity_id", "Entity ID"))
          .field(FieldDef.integer("required", "Count"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("item_check", tag -> {
            ItemRequirementTask t = new ItemRequirementTask(taskId(tag), desc(tag),
                    net.minecraft.world.item.Items.DIRT, 1, false);
            t.deserializeNBT(tag);
            return t;
        }).icon("§e■").label("Collect Item")
          .tooltip("Have a specific item in your inventory.\nTarget: item registry id. Consume: remove items on complete.")
          .field(FieldDef.itemId("item_id", "Item ID"))
          .field(FieldDef.integer("count", "Count"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("craft_item", tag -> {
            CraftItemTask t = new CraftItemTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "dirt"), 1);
            t.deserializeNBT(tag);
            return t;
        }).icon("§6⚒").label("Craft Item")
          .tooltip("Craft a specific item the required number of times.\nTarget: item registry id.")
          .field(FieldDef.itemId("item_id", "Item ID"))
          .field(FieldDef.integer("count", "Count"))
          .register();

        register("experience", tag -> {
            ExperienceTask t = new ExperienceTask(taskId(tag), desc(tag), 1);
            t.deserializeNBT(tag);
            return t;
        }).icon("§a✦").label("XP Level")
          .tooltip("Reach a minimum XP level.\nNo target needed — just set the required level.")
          .field(FieldDef.integer("required_level", "Level"))
          .register();

        register("location_terminal", tag -> {
            LocationOrTerminalTask t = new LocationOrTerminalTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "air"), false);
            t.deserializeNBT(tag);
            return t;
        }).icon("§b◎").label("Terminal / Location")
          .tooltip("Interact with a specific terminal block or location.\nTarget: terminal registry id.")
          .field(FieldDef.text("terminal_id", "Terminal ID"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("advancement", tag -> {
            AdvancementTask t = new AdvancementTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "story/root"));
            t.deserializeNBT(tag);
            return t;
        }).icon("§d★").label("Advancement")
          .tooltip("Earn a specific Minecraft advancement.\nTarget: advancement id (e.g. minecraft:story/mine_diamond)")
          .field(FieldDef.text("advancement_id", "Advancement ID"))
          .register();

        register("block_interact", tag -> {
            BlockInteractTask t = new BlockInteractTask(taskId(tag), desc(tag),
                    net.minecraft.world.level.block.Blocks.STONE, "PLACE");
            t.deserializeNBT(tag);
            return t;
        }).icon("§7□").label("Block Interact")
          .tooltip("Place or right-click a specific block.\nTarget: block id. Secondary: PLACE or RIGHT_CLICK.")
          .field(FieldDef.text("block_id", "Block ID"))
          .field(FieldDef.text("mode", "Mode", "PLACE or RIGHT_CLICK"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("fluid_check", tag -> {
            FluidRequirementTask t = new FluidRequirementTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "water"), 1000, false);
            t.deserializeNBT(tag);
            return t;
        }).icon("§3≋").label("Fluid Check")
          .tooltip("Have a fluid amount in a tank.\nTarget: fluid id. Count: amount in mB.")
          .field(FieldDef.fluidId("fluid_id", "Fluid ID"))
          .field(FieldDef.integer("amount", "Amount (mB)"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("stat", tag -> {
            StatTrackerTask t = new StatTrackerTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "jump"), 1, false);
            t.deserializeNBT(tag);
            return t;
        }).icon("§9≡").label("Stat Tracker")
          .tooltip("Reach a value on a Minecraft statistic.\nTarget: stat id (e.g. minecraft:jump). Count: target value.")
          .field(FieldDef.text("stat_id", "Stat ID"))
          .field(FieldDef.integer("required", "Target Value"))
          .field(FieldDef.bool("consume", "Consume"))
          .register();

        register("dimension", tag -> {
            DimensionTask t = new DimensionTask(taskId(tag), desc(tag),
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            new net.minecraft.resources.ResourceLocation("minecraft", "overworld")));
            t.deserializeNBT(tag);
            return t;
        }).icon("§5⊕").label("Visit Dimension")
          .tooltip("Travel to a specific dimension.\nSecondary: dimension id (e.g. minecraft:the_nether)")
          .field(FieldDef.text("dimension_id", "Dimension ID"))
          .register();

        register("biome", tag -> {
            BiomeTask t = new BiomeTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "plains"));
            t.deserializeNBT(tag);
            return t;
        }).icon("§2⛰").label("Visit Biome")
          .tooltip("Stand inside a specific biome.\nTarget: biome registry id (e.g. minecraft:jungle)")
          .field(FieldDef.text("biome_id", "Biome ID"))
          .register();

        register("structure", tag -> {
            StructureTask t = new StructureTask(taskId(tag), desc(tag),
                    new net.minecraft.resources.ResourceLocation("minecraft", "village"));
            t.deserializeNBT(tag);
            return t;
        }).icon("§6⌂").label("Visit Structure")
          .tooltip("Enter a specific structure.\nTarget: structure registry id (e.g. minecraft:village)")
          .field(FieldDef.text("structure_id", "Structure ID"))
          .register();

        register("checkmark", tag -> {
            CheckmarkTask t = new CheckmarkTask(taskId(tag), desc(tag));
            t.deserializeNBT(tag);
            return t;
        }).icon("§a✓").label("Checkmark")
          .tooltip("Admin-completable manual task.\nNo target needed — complete with /chronicle task complete.")
          .register();

        register("tag_item", tag -> {
            TagItemTask t = new TagItemTask(taskId(tag), desc(tag),
                    net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("c", "gems")), 1);
            t.deserializeNBT(tag);
            return t;
        }).icon("§e◈").label("Tag Item")
          .tooltip("Have items matching an item tag in inventory.\nTarget: item tag (e.g. c:ores/iron). Count: required amount.")
          .field(FieldDef.text("tag", "Item Tag"))
          .field(FieldDef.integer("count", "Count"))
          .register();

        register("info", tag -> {
            InfoTask t = new InfoTask(taskId(tag), desc(tag), "");
            t.deserializeNBT(tag);
            return t;
        }).icon("§7§l!").label("Info / Text")
          .tooltip("A read-only information panel the player must acknowledge.\nNo target — body text is shown to the player.")
          .field(FieldDef.text("body", "Body Text"))
          .register();

        register("energy_check", tag -> {
            EnergyStorageTask t = new EnergyStorageTask(taskId(tag), desc(tag),
                    10000L, EnergyStorageTask.EnergyType.FE, EnergyStorageTask.Source.INVENTORY);
            t.deserializeNBT(tag);
            return t;
        }).icon("§6⚡").label("Energy Storage")
          .tooltip("Check stored energy (FE or GTM EU) in inventory items OR a right-clicked block.\nCount: amount required.\nTarget: FE / EU / ANY  (energy type).\nSecondary: INVENTORY / HELD / BLOCK  (source).")
          .field(FieldDef.text("energy_type", "Energy Type", "FE / EU / ANY"))
          .field(FieldDef.text("source", "Source", "INVENTORY / HELD / BLOCK"))
          .field(FieldDef.integer("required", "Amount (FE)"))
          .register();

        register("external_trigger", tag -> {
            ExternalTriggerTask t = new ExternalTriggerTask(taskId(tag), desc(tag), "", 1);
            t.deserializeNBT(tag);
            return t;
        }).icon("§d⚡").label("External Trigger")
          .tooltip("Completes when an external mod/script fires QuestAPI.fireExternalEvent() with the matching trigger ID.\nTarget: trigger id (e.g. mymod:sun_eaten).\nCount: how many times the event must fire.")
          .field(FieldDef.text("trigger_id", "Trigger ID", "e.g. mymod:sun_eaten"))
          .field(FieldDef.integer("required", "Count (times fired)"))
          .register();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static net.minecraft.resources.ResourceLocation taskId(CompoundTag tag) {
        try {
            return new net.minecraft.resources.ResourceLocation(tag.getString("task_id"));
        } catch (Exception e) {
            return new net.minecraft.resources.ResourceLocation("phoenixcore", "task_" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    private static net.minecraft.network.chat.Component desc(CompoundTag tag) {
        try {
            if (tag.contains("description")) {
                net.minecraft.network.chat.Component c = net.minecraft.network.chat.Component.Serializer.fromJson(tag.getString("description"));
                if (c != null) return c;
            }
        } catch (Exception ignored) {}
        return net.minecraft.network.chat.Component.literal(tag.getString("type"));
    }

    private PhoenixTaskRegistry() {}
}
