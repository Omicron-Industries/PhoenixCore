package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChronicleDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, QuestNode> loadedNodes = new HashMap<>();
    private final Map<ResourceLocation, ResourceLocation> parentRelations = new HashMap<>();

    public ChronicleDataLoader() {
        super(GSON, "phoenix_chronicles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        LOGGER.info("Loading Phoenix Chronicles quests from datapacks...");

        QuestTreeRegistry.clear();
        loadedNodes.clear();
        parentRelations.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation questId = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();

                Component title = Component.Serializer.fromJson(json.get("title"));
                Component desc = Component.Serializer.fromJson(json.get("description"));

                QuestNode node = new QuestNode(questId, title, desc);

                String category = "MAIN";
                if (json.has("category")) {
                    category = json.get("category").getAsString().trim().toUpperCase();
                }
                node.setCategory(category);

                if (json.has("layout_x") && json.has("layout_y")) {
                    node.setCustomX(json.get("layout_x").getAsInt());
                    node.setCustomY(json.get("layout_y").getAsInt());
                }

                String shape = "SQUARE";
                if (json.has("shape")) {
                    shape = json.get("shape").getAsString().trim().toUpperCase();
                }
                node.setShapeType(shape);

                if (json.has("subtitle")) node.setSubtitle(json.get("subtitle").getAsString());
                if (json.has("visibility")) {
                    try {
                        node.setVisibility(QuestNode.Visibility.valueOf(
                                json.get("visibility").getAsString().toUpperCase()));
                    } catch (Exception ignored) {}
                }
                if (json.has("task_min_count")) node.setTaskMinCount(json.get("task_min_count").getAsInt());
                if (json.has("require_all_prerequisites"))
                    node.setRequireAllPrerequisites(json.get("require_all_prerequisites").getAsBoolean());

                if (json.has("repeat_mode")) {
                    try {
                        node.setRepeatMode(QuestNode.RepeatMode.valueOf(
                                json.get("repeat_mode").getAsString().toUpperCase()));
                    } catch (Exception ignored) {}
                }
                if (json.has("repeat_cooldown_hours"))
                    node.setRepeatCooldownHours(json.get("repeat_cooldown_hours").getAsInt());

                if (json.has("parent")) {
                    ResourceLocation parentId = new ResourceLocation(json.get("parent").getAsString());
                    parentRelations.put(questId, parentId);
                }

                if (json.has("tasks")) {
                    json.getAsJsonArray("tasks").forEach(element -> {
                        JsonObject taskJson = element.getAsJsonObject();
                        ResourceLocation taskId = new ResourceLocation(taskJson.get("id").getAsString());
                        Component taskDesc = Component.Serializer.fromJson(taskJson.get("description"));

                        String type = taskJson.get("type").getAsString();
                        boolean isOptional = taskJson.has("optional") && taskJson.get("optional").getAsBoolean();

                        boolean defaultConsume = !taskJson.has("consume") || taskJson.get("consume").getAsBoolean();

                        QuestTask addedTask = null;
                        switch (type) {
                            case "terminal_check" -> {
                                ResourceLocation terminalId = new ResourceLocation(
                                        taskJson.get("target_terminal").getAsString());
                                boolean consume = taskJson.has("consume") && taskJson.get("consume").getAsBoolean();
                                addedTask = new LocationOrTerminalTask(taskId, taskDesc, terminalId, consume);
                            }
                            case "item_check" -> {
                                ResourceLocation itemId = new ResourceLocation(taskJson.get("item_id").getAsString());
                                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                                int amount = taskJson.has("amount") ? taskJson.get("amount").getAsInt() : 1;
                                if (item != null) {
                                    addedTask = new ItemRequirementTask(taskId, taskDesc, item, amount, defaultConsume);
                                } else {
                                    LOGGER.error(
                                            "Skipping item_check task '{}' - Item '{}' cannot be found in Registry!",
                                            taskId, itemId);
                                }
                            }
                            case "fluid_check" -> {
                                ResourceLocation fluidId = new ResourceLocation(taskJson.get("fluid_id").getAsString());
                                int amount = taskJson.has("amount") ? taskJson.get("amount").getAsInt() : 1000;
                                addedTask = new FluidRequirementTask(taskId, taskDesc, fluidId, amount, defaultConsume);
                            }
                            case "craft_item" -> {
                                ResourceLocation itemId = new ResourceLocation(taskJson.get("item_id").getAsString());
                                int count = taskJson.has("count") ? taskJson.get("count").getAsInt() : 1;
                                addedTask = new CraftItemTask(taskId, taskDesc, itemId, count);
                            }
                            case "kill_entity" -> {
                                ResourceLocation entityId = new ResourceLocation(
                                        taskJson.get("entity_id").getAsString());
                                int required = taskJson.has("required") ? taskJson.get("required").getAsInt() : 1;
                                boolean consume = taskJson.has("consume") && taskJson.get("consume").getAsBoolean();
                                addedTask = new KillEntityTask(taskId, taskDesc, entityId, required, consume);
                            }
                            case "stat" -> {
                                ResourceLocation statId = new ResourceLocation(taskJson.get("stat_id").getAsString());
                                int target = taskJson.has("target_value") ? taskJson.get("target_value").getAsInt() :
                                        1000;
                                boolean consume = taskJson.has("consume") && taskJson.get("consume").getAsBoolean();
                                addedTask = new StatTrackerTask(taskId, taskDesc, statId, target, consume);
                            }
                            case "xp_check" -> {
                                int level = taskJson.get("level").getAsInt();
                                addedTask = new ExperienceTask(taskId, taskDesc, level);
                            }
                            case "biome" -> {
                                ResourceLocation biomeId = new ResourceLocation(taskJson.get("biome_id").getAsString());
                                addedTask = new BiomeTask(taskId, taskDesc, biomeId);
                            }
                            case "structure" -> {
                                ResourceLocation structureId = new ResourceLocation(
                                        taskJson.get("structure_id").getAsString());
                                addedTask = new StructureTask(taskId, taskDesc, structureId);
                            }
                            case "checkmark" -> addedTask = new CheckmarkTask(taskId, taskDesc);
                            case "tag_item" -> {
                                net.minecraft.tags.TagKey<Item> tag = net.minecraft.tags.ItemTags.create(
                                        new ResourceLocation(taskJson.get("tag").getAsString()));
                                int required = taskJson.has("required") ? taskJson.get("required").getAsInt() : 1;
                                addedTask = new TagItemTask(taskId, taskDesc, tag, required);
                            }
                            case "info" -> {
                                String body = taskJson.has("body") ? taskJson.get("body").getAsString() : "";
                                addedTask = new net.phoenix.core.integration.phoenix_chronicles.tasks.InfoTask(taskId,
                                        taskDesc, body);
                            }
                            case "energy_check" -> {
                                long required = taskJson.has("required_energy") ?
                                        taskJson.get("required_energy").getAsLong() :
                                        taskJson.has("amount") ? taskJson.get("amount").getAsLong() : 10000L;
                                String typeStr = taskJson.has("energy_type") ?
                                        taskJson.get("energy_type").getAsString() : "FE";
                                String srcStr = taskJson.has("source") ? taskJson.get("source").getAsString() :
                                        "INVENTORY";
                                net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.EnergyType eType;
                                net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.Source eSrc;
                                try {
                                    eType = net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.EnergyType
                                            .valueOf(typeStr.toUpperCase());
                                } catch (Exception ignored) {
                                    eType = net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.EnergyType.FE;
                                }
                                try {
                                    eSrc = net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.Source
                                            .valueOf(srcStr.toUpperCase());
                                } catch (Exception ignored) {
                                    eSrc = net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask.Source.INVENTORY;
                                }
                                addedTask = new net.phoenix.core.integration.phoenix_chronicles.tasks.EnergyStorageTask(
                                        taskId, taskDesc, required, eType, eSrc);
                            }
                            default -> LOGGER.warn("Unknown chronicle task type '{}' discovered on quest '{}'", type,
                                    questId);
                        }
                        if (addedTask != null) {
                            addedTask.setOptional(isOptional);
                            node.addTask(addedTask);
                        }
                    });
                }

                loadedNodes.put(questId, node);
            } catch (Exception e) {
                LOGGER.error("Failed to parse chronicle quest entry JSON for {}", questId, e);
            }
        }

        List<QuestNode> rootNodes = new ArrayList<>();

        for (QuestNode node : loadedNodes.values()) {
            ResourceLocation parentId = parentRelations.get(node.getId());
            if (parentId != null) {
                QuestNode parentNode = loadedNodes.get(parentId);
                if (parentNode != null) {
                    parentNode.addChild(node);
                } else {
                    LOGGER.warn("Quest '{}' defined parent '{}' but that parent does not exist!", node.getId(),
                            parentId);
                    rootNodes.add(node);
                }
            } else {
                rootNodes.add(node);
            }
        }

        for (QuestNode root : rootNodes) {
            QuestTreeRegistry.registerRootChapter(root);
        }

        LOGGER.info("Successfully loaded {} chronicle branches into memory.", rootNodes.size());

        MinecraftServer server = ChronicleEvents.getCachedServer();
        if (server != null) {
            java.nio.file.Path configDir = server.getServerDirectory().toPath()
                    .resolve("config").resolve("phoenix_chronicles");
            QuestFileLoader.loadAdditiveFromDisk(configDir);
        }
    }
}
