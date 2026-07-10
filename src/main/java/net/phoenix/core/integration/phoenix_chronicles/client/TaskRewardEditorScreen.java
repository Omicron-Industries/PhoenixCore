package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen task & reward editor — left column tasks, right column rewards.
 */
public class TaskRewardEditorScreen extends Screen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private int C_BG, C_PANEL, C_HEADER, C_BORDER, C_ACCENT, C_TEXT, C_TEXT_DIM, C_TEXT_FAINT, C_OK;
    private static final int C_ROW_HOVER = 0x22FFFFFF;
    private static final int C_FORM_BG = 0x33000000;
    private static final int C_SPLIT = 0xFF2A2A3A;
    private static final int C_TOOLTIP_BG = 0xFF0E0E16;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 28;
    private static final int MARGIN = 10;
    private static final int COL_GAP = 6;    // gap between the two columns
    private static final int ROW_H = 26;   // task/reward list row height (2 lines)
    private static final int FIELD_H = 15;   // form field height
    private static final int FIELD_GAP = 3;    // gap between fields
    private static final int FORM_ROWS = 4;    // max form field rows to reserve

    // Derived — set in init()
    private int splitX;        // x where right column begins
    private int colW;          // width of each column (they're equal)
    private int listTop;       // y where list area begins
    private int listBottom;    // y where list area ends (form starts)
    private int formTop;       // y where add-form begins
    private int formBottom;    // y where footer begins (== height - FOOTER_H)

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final QuestNode questNode;

    private final List<QuestTask> tasks = new ArrayList<>();
    private final List<QuestReward> rewards = new ArrayList<>();

    // Task form
    private String taskType = "kill_entity";
    private boolean taskConsume = true;
    private boolean taskOptional = false;
    private boolean taskTypeDropOpen = false;
    private EditBox taskDescBox, taskTargetBox, taskCountBox, taskSecondaryBox;

    // Reward form
    private String rewardType = "item";
    private boolean rewardTypeDropOpen = false;
    private ItemStack rewardPickedItem = null;
    private EditBox rewardCountBox, rewardCommandBox;

    // Hover tracking
    private int hoveredTaskRow = -1;
    private int hoveredRewardRow = -1;
    private int hoveredDropRow = -1;

    // Task clipboard
    private static CompoundTag copiedTaskNBT = null;

    // Undo history — each entry is a snapshot of [tasks, rewards] before a mutation
    private final java.util.Deque<Object[]> undoHistory = new java.util.ArrayDeque<>();
    private static final int MAX_UNDO = 30;

    private static final String[] REWARD_TYPES = { "item", "xp", "command", "loot_table", "script_event" };
    private EditBox rewardEventDataBox;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TaskRewardEditorScreen(Screen parent, QuestNode questNode) {
        super(Component.literal("Tasks & Rewards"));
        this.parent = parent;
        this.questNode = questNode;
        this.tasks.addAll(questNode.getTasks());
        this.rewards.addAll(questNode.getRewards());
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        ChroniclesTheme th = ChroniclesTheme.current();
        C_BG = th.bg.getColor();
        C_PANEL = th.panel.getColor();
        C_HEADER = th.header.getColor();
        C_BORDER = th.border.getColor();
        C_ACCENT = th.accent.getColor();
        C_TEXT = th.text.getColor();
        C_TEXT_DIM = th.textDim.getColor();
        C_TEXT_FAINT = th.textFaint.getColor();
        C_OK = th.done.getColor();

        // Geometry
        colW = (width - MARGIN * 2 - COL_GAP) / 2;
        splitX = MARGIN + colW + COL_GAP;
        formBottom = height - FOOTER_H;
        formTop = formBottom - MARGIN - FORM_ROWS * (FIELD_H + FIELD_GAP) - 8;
        listTop = HEADER_H + 22; // 22px for column sub-header
        listBottom = formTop - 22; // leave room for the form panel header

        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();

        // ── Done button ───────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("§7‹ Done"), b -> {
            flushToQuestNode();
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(width / 2 - 40, height - FOOTER_H + (FOOTER_H - 14) / 2, 80, 14)
                .tooltip(Tooltip.create(Component.literal("Save changes and return to quest editor"))).build());

        // ── Task form fields ──────────────────────────────────────────────────
        int tx = MARGIN;
        int fy = formTop + 8;

        // Type selector
        PhoenixTaskRegistry.TaskEntry curMeta = getTaskMeta(taskType);
        String typeTooltip = curMeta != null && curMeta.editorTooltip() != null ?
                curMeta.editorTooltip().split("\n")[0] : "Choose the type of task to add";
        addRenderableWidget(Button.builder(
                Component.literal("§8Type: §7" + (curMeta != null ? curMeta.editorLabel() : taskType) + " §8▾"),
                b -> {
                    taskTypeDropOpen = !taskTypeDropOpen;
                    rewardTypeDropOpen = false;
                })
                .bounds(tx, fy, colW, FIELD_H)
                .tooltip(Tooltip.create(Component.literal(typeTooltip))).build());
        fy += FIELD_H + FIELD_GAP;

        // Field visibility logic (unchanged from original)
        boolean isInfo = taskType.equals("info");
        boolean needsTarget = switch (taskType) {
            case "experience", "dimension", "checkmark" -> false;
            default -> {
                PhoenixTaskRegistry.TaskEntry re = PhoenixTaskRegistry.get(taskType);
                if (re != null && !re.fields().isEmpty())
                    yield re.fields().stream()
                            .anyMatch(f -> f.type() != PhoenixTaskRegistry.FieldDef.FieldType.INTEGER &&
                                    f.type() != PhoenixTaskRegistry.FieldDef.FieldType.BOOLEAN);
                yield true;
            }
        };
        boolean needsSecond = taskType.equals("block_interact") || taskType.equals("stat") ||
                taskType.equals("dimension") || taskType.equals("energy_check");
        boolean needsCount = switch (taskType) {
            case "kill_entity", "item_check", "craft_item", "experience", "fluid_check", "stat", "tag_item", "energy_check", "external_trigger" -> true;
            default -> {
                PhoenixTaskRegistry.TaskEntry re = PhoenixTaskRegistry.get(taskType);
                yield re != null &&
                        re.fields().stream().anyMatch(f -> f.type() == PhoenixTaskRegistry.FieldDef.FieldType.INTEGER);
            }
        };
        boolean showConsume = switch (taskType) {
            case "kill_entity", "item_check", "craft_item", "fluid_check", "location_terminal", "stat", "block_interact" -> true;
            default -> false;
        };

        // Description
        taskDescBox = new EditBox(font, tx, fy, colW, FIELD_H, Component.empty());
        taskDescBox.setHint(Component.literal("§8Task label shown to player"));
        taskDescBox.setMaxLength(128);
        addRenderableWidget(taskDescBox);
        fy += FIELD_H + FIELD_GAP;

        if (needsTarget) {
            String hint = isInfo ? "§8Body text shown to the player" : switch (taskType) {
                case "kill_entity" -> "§8Entity id  (e.g. minecraft:zombie)";
                case "item_check", "craft_item" -> "§8Item id  (e.g. minecraft:iron_ingot)";
                case "location_terminal" -> "§8Terminal id";
                case "advancement" -> "§8Advancement id";
                case "block_interact" -> "§8Block id";
                case "fluid_check" -> "§8Fluid id";
                case "stat" -> "§8Stat id  (e.g. minecraft:jump)";
                case "biome" -> "§8Biome id";
                case "structure" -> "§8Structure id";
                case "tag_item" -> "§8Item tag  (e.g. c:ores/iron)";
                case "energy_check" -> "§8FE / EU / ANY";
                case "external_trigger" -> "§8Trigger id";
                default -> {
                    PhoenixTaskRegistry.TaskEntry re = PhoenixTaskRegistry.get(taskType);
                    if (re != null) {
                        for (PhoenixTaskRegistry.FieldDef f : re.fields()) {
                            if (f.type() != PhoenixTaskRegistry.FieldDef.FieldType.INTEGER &&
                                    f.type() != PhoenixTaskRegistry.FieldDef.FieldType.BOOLEAN)
                                yield "§8" + f.label() + (f.hint() != null ? "  (" + f.hint() + ")" : "");
                        }
                    }
                    yield "§8Target id";
                }
            };
            boolean hasItemPicker = taskType.equals("item_check") || taskType.equals("craft_item");
            boolean hasFluidPicker = taskType.equals("fluid_check");
            int tw = (hasItemPicker || hasFluidPicker) ? colW - 18 : colW;
            int tmaxLen = isInfo ? 512 : 160;
            taskTargetBox = new EditBox(font, tx, fy, tw, FIELD_H, Component.empty());
            taskTargetBox.setHint(Component.literal(hint));
            taskTargetBox.setMaxLength(tmaxLen);
            addRenderableWidget(taskTargetBox);
            if (hasItemPicker) {
                addRenderableWidget(Button.builder(Component.literal("§7⊞"), b -> {
                    if (minecraft != null) minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (id != null && taskTargetBox != null) taskTargetBox.setValue(id.toString());
                    }));
                }).bounds(tx + tw, fy, 16, FIELD_H).build());
            } else if (hasFluidPicker) {
                addRenderableWidget(Button.builder(Component.literal("§3⊞"), b -> {
                    if (minecraft != null) minecraft.setScreen(new FluidPickerScreen(this, fluidId -> {
                        if (taskTargetBox != null) taskTargetBox.setValue(fluidId);
                    }));
                }).bounds(tx + tw, fy, 16, FIELD_H).build());
            }
            fy += FIELD_H + FIELD_GAP;
        }

        if (needsSecond) {
            String hint2 = switch (taskType) {
                case "block_interact" -> "§8PLACE or RIGHT_CLICK";
                case "dimension" -> "§8Dimension id  (e.g. minecraft:the_nether)";
                case "energy_check" -> "§8INVENTORY / HELD / BLOCK";
                default -> "§8Secondary value";
            };
            taskSecondaryBox = new EditBox(font, tx, fy, colW, FIELD_H, Component.empty());
            taskSecondaryBox.setHint(Component.literal(hint2));
            taskSecondaryBox.setMaxLength(128);
            addRenderableWidget(taskSecondaryBox);
            fy += FIELD_H + FIELD_GAP;
        }

        // Bottom row: count | consume | optional | add
        int rowY = formBottom - FIELD_H - 4;
        if (needsCount) {
            String countHint = switch (taskType) {
                case "experience" -> "§8XP level";
                case "fluid_check" -> "§8mB amount";
                case "stat" -> "§8Target value";
                case "energy_check" -> "§8FE required";
                case "external_trigger" -> "§8Times fired";
                default -> {
                    PhoenixTaskRegistry.TaskEntry re = PhoenixTaskRegistry.get(taskType);
                    if (re != null) for (PhoenixTaskRegistry.FieldDef f : re.fields())
                        if (f.type() == PhoenixTaskRegistry.FieldDef.FieldType.INTEGER) yield "§8" + f.label();
                    yield "§8Count";
                }
            };
            taskCountBox = new EditBox(font, tx, rowY, 52, FIELD_H, Component.empty());
            taskCountBox.setHint(Component.literal(countHint));
            taskCountBox.setMaxLength(8);
            addRenderableWidget(taskCountBox);
        }
        if (showConsume) {
            int cx2 = needsCount ? tx + 56 : tx;
            addRenderableWidget(Button.builder(
                    Component.literal(taskConsume ? "§aConsume" : "§8Consume"),
                    b -> {
                        taskConsume = !taskConsume;
                        rebuildWidgets();
                    })
                    .bounds(cx2, rowY, 54, FIELD_H)
                    .tooltip(Tooltip.create(
                            Component.literal("Remove the item/fluid from the player's inventory on completion")))
                    .build());
        }
        addRenderableWidget(Button.builder(
                Component.literal(taskOptional ? "§eOptional" : "§8Optional"),
                b -> {
                    taskOptional = !taskOptional;
                    rebuildWidgets();
                })
                .bounds(tx + colW - 100, rowY, 50, FIELD_H)
                .tooltip(Tooltip.create(Component.literal("Task is optional — won't block quest completion"))).build());
        addRenderableWidget(Button.builder(Component.literal("§a✔ Add"),
                b -> commitTaskFromForm())
                .bounds(tx + colW - 46, rowY, 46, FIELD_H)
                .tooltip(Tooltip.create(Component.literal("Add this task to the quest (Ctrl+Z to undo)"))).build());

        // ── Reward form fields ────────────────────────────────────────────────
        int rx = splitX;
        int rfy = formTop + 8;

        String rewardTypeTooltip = switch (rewardType) {
            case "item" -> "Give the player one or more items";
            case "xp" -> "Award experience levels";
            case "command" -> "Run a server command (%player% = player name)";
            case "loot_table" -> "Roll a loot table and give all resulting items";
            case "script_event" -> "Fire a Forge event for KubeJS or Java handlers";
            default -> "Choose a reward type";
        };
        addRenderableWidget(Button.builder(
                Component.literal("§8Type: §7" + rewardType + " §8▾"),
                b -> {
                    rewardTypeDropOpen = !rewardTypeDropOpen;
                    taskTypeDropOpen = false;
                })
                .bounds(rx, rfy, colW, FIELD_H)
                .tooltip(Tooltip.create(Component.literal(rewardTypeTooltip))).build());
        rfy += FIELD_H + FIELD_GAP;

        if (rewardType.equals("item")) {
            String itemLabel = rewardPickedItem != null ? "§f" + rewardPickedItem.getHoverName().getString() :
                    "§8Pick item…";
            addRenderableWidget(Button.builder(Component.literal(itemLabel), b -> {
                if (minecraft != null) minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                    rewardPickedItem = stack;
                    rebuildWidgets();
                }));
            }).bounds(rx, rfy, colW - 44, FIELD_H).build());
            rewardCountBox = new EditBox(font, rx + colW - 42, rfy, 42, FIELD_H, Component.empty());
            rewardCountBox.setHint(Component.literal("§8Qty"));
            rewardCountBox.setMaxLength(4);
            addRenderableWidget(rewardCountBox);
        } else if (rewardType.equals("xp")) {
            rewardCountBox = new EditBox(font, rx, rfy, colW, FIELD_H, Component.empty());
            rewardCountBox.setHint(Component.literal("§8XP levels to award"));
            rewardCountBox.setMaxLength(5);
            addRenderableWidget(rewardCountBox);
        } else if (rewardType.equals("script_event")) {
            rewardCommandBox = new EditBox(font, rx, rfy, colW, FIELD_H, Component.empty());
            rewardCommandBox.setHint(Component.literal("§8Event ID  (e.g. unlock_end)"));
            rewardCommandBox.setMaxLength(128);
            addRenderableWidget(rewardCommandBox);
            rfy += FIELD_H + FIELD_GAP;
            rewardEventDataBox = new EditBox(font, rx, rfy, colW, FIELD_H, Component.empty());
            rewardEventDataBox.setHint(Component.literal("§8NBT data  {key:\"val\"}  (optional)"));
            rewardEventDataBox.setMaxLength(256);
            addRenderableWidget(rewardEventDataBox);
        } else {
            // command / loot_table
            String hint = rewardType.equals("loot_table") ? "§8Loot table id  (e.g. minecraft:chests/simple_dungeon)" :
                    "§8/give %player% …";
            rewardCommandBox = new EditBox(font, rx, rfy, colW, FIELD_H, Component.empty());
            rewardCommandBox.setHint(Component.literal(hint));
            rewardCommandBox.setMaxLength(256);
            addRenderableWidget(rewardCommandBox);
        }

        addRenderableWidget(Button.builder(Component.literal("§a✔ Add reward"),
                b -> commitRewardFromForm())
                .bounds(rx + colW - 80, formBottom - FIELD_H - 4, 80, FIELD_H)
                .tooltip(Tooltip.create(Component.literal("Add this reward to the quest (Ctrl+Z to undo)"))).build());
    }

    // ── Undo ─────────────────────────────────────────────────────────────────

    private void pushUndo() {
        undoHistory.push(new Object[] { new ArrayList<>(tasks), new ArrayList<>(rewards) });
        if (undoHistory.size() > MAX_UNDO) undoHistory.pollLast();
    }

    @SuppressWarnings("unchecked")
    private void undoLastChange() {
        if (undoHistory.isEmpty()) return;
        Object[] snap = undoHistory.pop();
        tasks.clear();
        tasks.addAll((List<QuestTask>) snap[0]);
        rewards.clear();
        rewards.addAll((List<QuestReward>) snap[1]);
        rebuildWidgets();
    }

    // ── Commit ────────────────────────────────────────────────────────────────

    private void commitTaskFromForm() {
        String desc = taskDescBox != null ? taskDescBox.getValue().trim() : "";
        String target = taskTargetBox != null ? taskTargetBox.getValue().trim() : "";
        String second = taskSecondaryBox != null ? taskSecondaryBox.getValue().trim() : "";
        String countS = taskCountBox != null ? taskCountBox.getValue().trim() : "1";
        int count = 1;
        try {
            count = Math.max(1, Integer.parseInt(countS));
        } catch (NumberFormatException ignored) {}

        boolean needsTarget = !taskType.equals("experience") && !taskType.equals("dimension") &&
                !taskType.equals("checkmark");
        if (desc.isEmpty() || (needsTarget && !taskType.equals("info") && target.isEmpty())) return;

        ResourceLocation taskId = new ResourceLocation("phoenixcore",
                "task_" + taskType + "_" + System.currentTimeMillis());
        Component descComp = Component.literal(desc);
        QuestTask task = null;
        try {
            task = switch (taskType) {
                case "kill_entity" -> new KillEntityTask(taskId, descComp, new ResourceLocation(target), count,
                        taskConsume);
                case "item_check" -> {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));
                    yield item != null ? new ItemRequirementTask(taskId, descComp, item, count, taskConsume) : null;
                }
                case "craft_item" -> new CraftItemTask(taskId, descComp, new ResourceLocation(target), count);
                case "experience" -> new ExperienceTask(taskId, descComp, count);
                case "location_terminal" -> new LocationOrTerminalTask(taskId, descComp, new ResourceLocation(target),
                        taskConsume);
                case "advancement" -> new AdvancementTask(taskId, descComp, new ResourceLocation(target));
                case "block_interact" -> {
                    var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(target));
                    String mode = second.isEmpty() ? "PLACE" : second.toUpperCase();
                    yield block != null ? new BlockInteractTask(taskId, descComp, block, mode) : null;
                }
                case "fluid_check" -> new FluidRequirementTask(taskId, descComp, new ResourceLocation(target), count,
                        taskConsume);
                case "stat" -> new StatTrackerTask(taskId, descComp, new ResourceLocation(target), count, taskConsume);
                case "dimension" -> {
                    String dim = second.isEmpty() ? "minecraft:overworld" : second;
                    yield new DimensionTask(taskId, descComp,
                            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim)));
                }
                case "biome" -> new BiomeTask(taskId, descComp, new ResourceLocation(target));
                case "structure" -> new StructureTask(taskId, descComp, new ResourceLocation(target));
                case "checkmark" -> new CheckmarkTask(taskId, descComp);
                case "tag_item" -> new TagItemTask(taskId, descComp, ItemTags.create(new ResourceLocation(target)),
                        count);
                case "info" -> new InfoTask(taskId, descComp, target);
                case "external_trigger" -> new ExternalTriggerTask(taskId, descComp, target, count);
                case "energy_check" -> {
                    var eType = EnergyStorageTask.EnergyType.FE;
                    if (!target.isBlank()) {
                        try {
                            eType = EnergyStorageTask.EnergyType.valueOf(target.trim().toUpperCase());
                        } catch (Exception ignored2) {}
                    }
                    var eSrc = EnergyStorageTask.Source.INVENTORY;
                    if (!second.isBlank()) {
                        try {
                            eSrc = EnergyStorageTask.Source.valueOf(second.trim().toUpperCase());
                        } catch (Exception ignored2) {}
                    }
                    yield new EnergyStorageTask(taskId, descComp, (long) count, eType, eSrc);
                }
                default -> {
                    PhoenixTaskRegistry.TaskEntry re = PhoenixTaskRegistry.get(taskType);
                    if (re != null) {
                        ExternalTriggerTask ext = new ExternalTriggerTask(taskId, descComp, target, count);
                        ext.setKjsTypeId(taskType);
                        yield ext;
                    }
                    yield null;
                }
            };
        } catch (Exception ignored) {}

        if (task != null) {
            task.setOptional(taskOptional);
            pushUndo();
            tasks.add(task);
            taskTypeDropOpen = false;
            taskOptional = false;
            rebuildWidgets();
        }
    }

    private void commitRewardFromForm() {
        String countS = rewardCountBox != null ? rewardCountBox.getValue().trim() : "1";
        int count = 1;
        try {
            count = Math.max(1, Integer.parseInt(countS));
        } catch (NumberFormatException ignored) {}

        QuestReward reward = switch (rewardType) {
            case "item" -> rewardPickedItem != null ? new QuestReward.ItemReward(rewardPickedItem.getItem(), count) :
                    null;
            case "xp" -> new QuestReward.XPReward(count);
            case "command" -> {
                String cmd = rewardCommandBox != null ? rewardCommandBox.getValue().trim() : "";
                yield cmd.isEmpty() ? null : new QuestReward.CommandReward(cmd);
            }
            case "loot_table" -> {
                String lt = rewardCommandBox != null ? rewardCommandBox.getValue().trim() : "";
                yield lt.isEmpty() ? null : new QuestReward.LootTableReward(new ResourceLocation(lt));
            }
            case "script_event" -> {
                String eid = rewardCommandBox != null ? rewardCommandBox.getValue().trim() : "";
                if (eid.isEmpty()) yield null;
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                if (rewardEventDataBox != null && !rewardEventDataBox.getValue().isBlank()) {
                    try {
                        data = net.minecraft.nbt.TagParser.parseTag(rewardEventDataBox.getValue().trim());
                    } catch (Exception ignored) {}
                }
                yield new QuestReward.ScriptEventReward(eid, data);
            }
            default -> null;
        };

        if (reward != null) {
            pushUndo();
            rewards.add(reward);
            rewardPickedItem = null;
            rewardTypeDropOpen = false;
            rebuildWidgets();
        }
    }

    // ── Flush ─────────────────────────────────────────────────────────────────

    private void flushToQuestNode() {
        questNode.clearTasks();
        for (QuestTask t : tasks) questNode.addTask(t);
        questNode.clearRewards();
        for (QuestReward r : rewards) questNode.addReward(r);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(@NotNull GuiGraphics g) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        if (parent instanceof ChronicleOverviewScreen overview) {
            overview.renderForChildScreen(g);
            g.fill(0, 0, width, height, 0xAA000000);
        } else {
            g.fill(0, 0, width, height, C_BG);
        }

        // Header
        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        String repeatBadge = switch (questNode.getRepeatMode()) {
            case DAILY -> "  §b[Daily]";
            case COOLDOWN -> "  §e[Cooldown " + questNode.getRepeatCooldownHours() + "h]";
            case INFINITE -> "  §a[∞]";
            default -> "";
        };
        g.drawCenteredString(font, "§fTasks & Rewards  §8— §7" + questNode.getId().getPath() + repeatBadge,
                width / 2, (HEADER_H - 8) / 2, C_TEXT);

        // Column sub-headers
        g.fill(0, HEADER_H, width, listTop - 1, C_PANEL);
        g.fill(0, listTop - 1, width, listTop, C_BORDER);
        String taskSubHeader;
        if (tasks.isEmpty()) {
            taskSubHeader = "§c⚠ No tasks — quest auto-completes on unlock";
        } else {
            long optCount = tasks.stream().filter(QuestTask::isOptional).count();
            long reqCount = tasks.size() - optCount;
            taskSubHeader = "§8TASKS  §7" + reqCount + " req" + (optCount > 0 ? "  §8+  §e" + optCount + " opt" : "");
        }
        g.drawString(font, taskSubHeader, MARGIN + 4, HEADER_H + 6, C_TEXT_FAINT, false);
        if (copiedTaskNBT != null)
            g.drawString(font, "§b[Ctrl+V]", MARGIN + colW - font.width("[Ctrl+V]") - 4, HEADER_H + 6, 0xFF55BBFF,
                    false);
        g.drawString(font, "§8REWARDS  §7" + rewards.size(), splitX + 4, HEADER_H + 6, C_TEXT_FAINT, false);

        // Centre column divider
        g.fill(splitX - COL_GAP / 2, HEADER_H, splitX - COL_GAP / 2 + 1, height - FOOTER_H, C_SPLIT);

        // Form zone background + separator
        int formPanelTop = formTop - 20;
        g.fill(0, formPanelTop, width, formBottom, C_PANEL);
        g.fill(0, formPanelTop, width, formPanelTop + 1, C_BORDER);
        // Column form panels
        g.fill(MARGIN, formPanelTop + 2, MARGIN + colW, formBottom - 2, C_FORM_BG);
        drawBorder(g, MARGIN, formPanelTop + 2, colW, formBottom - 2 - (formPanelTop + 2), C_BORDER);
        g.fill(splitX, formPanelTop + 2, splitX + colW, formBottom - 2, C_FORM_BG);
        drawBorder(g, splitX, formPanelTop + 2, colW, formBottom - 2 - (formPanelTop + 2), C_BORDER);
        g.drawString(font, "§8ADD TASK", MARGIN + 6, formPanelTop + 6, C_TEXT_FAINT, false);
        g.drawString(font, "§8ADD REWARD", splitX + 6, formPanelTop + 6, C_TEXT_FAINT, false);

        // Footer
        g.fill(0, height - FOOTER_H, width, height, C_HEADER);
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, C_BORDER);

        // ── Task list ─────────────────────────────────────────────────────────
        g.enableScissor(0, listTop, splitX - COL_GAP / 2, listBottom);
        hoveredTaskRow = -1;
        int ty = listTop;
        for (int i = 0; i < tasks.size(); i++) {
            QuestTask task = tasks.get(i);
            if (ty + ROW_H > listBottom) break;
            boolean hov = mx >= MARGIN && mx < splitX - COL_GAP && my >= ty && my < ty + ROW_H;
            if (hov) {
                g.fill(MARGIN, ty, splitX - COL_GAP, ty + ROW_H, C_ROW_HOVER);
                hoveredTaskRow = i;
            }
            // Accent stripe: green = optional, accent = required
            g.fill(MARGIN, ty + 2, MARGIN + 2, ty + ROW_H - 2,
                    task.isOptional() ? 0xFF22AA55 : C_ACCENT);
            PhoenixTaskRegistry.TaskEntry meta = getTaskMetaByClass(task);
            ItemStack taskIcon = getTaskIconStack(task);
            int textX = MARGIN + 5;
            if (!taskIcon.isEmpty()) {
                g.renderItem(taskIcon, textX, ty + 4);
                textX += 18;
            } else if (meta != null && meta.editorIcon() != null) {
                g.drawString(font, meta.editorIcon(), textX, ty + 9, 0xFFFFFFFF, false);
                textX += 10;
            }
            int maxW = (splitX - COL_GAP) - textX - (hov ? 34 : 6);
            String rawLabel = task.getDescription().getString();
            if (task.isOptional()) rawLabel = "[opt] " + rawLabel;
            String detail = getTaskDetailString(task);
            String[] wrapped = wordWrap(rawLabel, maxW);
            String line1Color = task.isOptional() ? "§8" : "§7";
            g.drawString(font, line1Color + wrapped[0], textX, ty + 4, C_TEXT_DIM, false);
            if (wrapped[1] != null) {
                // description overflowed — second line continues it; no room for detail
                g.drawString(font, "§8" + wrapped[1], textX, ty + 15, C_TEXT_FAINT, false);
            } else if (detail != null) {
                String dl = detail;
                if (font.width(dl) > maxW) dl = font.plainSubstrByWidth(dl, maxW - 4) + "…";
                g.drawString(font, "§8" + dl, textX, ty + 15, C_TEXT_FAINT, false);
            }
            if (hov) {
                g.drawString(font, "§b⧉", splitX - COL_GAP - 26, ty + 9, 0xFF55BBFF, false);
                g.drawString(font, "§c×", splitX - COL_GAP - 12, ty + 9, 0xFFFF5555, false);
            }
            ty += ROW_H;
        }
        if (tasks.isEmpty())
            g.drawString(font, "§8No tasks yet — add one below.", MARGIN + 6, listTop + 5, C_TEXT_FAINT, false);
        g.disableScissor();

        // ── Reward list ───────────────────────────────────────────────────────
        g.enableScissor(splitX, listTop, width, listBottom);
        hoveredRewardRow = -1;
        int ry = listTop;
        for (int i = 0; i < rewards.size(); i++) {
            QuestReward reward = rewards.get(i);
            if (ry + ROW_H > listBottom) break;
            boolean hov = mx >= splitX && mx < width - MARGIN && my >= ry && my < ry + ROW_H;
            if (hov) {
                g.fill(splitX, ry, width - MARGIN, ry + ROW_H, C_ROW_HOVER);
                hoveredRewardRow = i;
            }
            int rewardTextX = splitX + 5;
            if (reward instanceof QuestReward.ItemReward ir) {
                ItemStack stack = new ItemStack(ir.getItem(), ir.getCount());
                g.renderItem(stack, rewardTextX, ry + 4);
                rewardTextX += 18;
                int rmaxW = (width - MARGIN - (hov ? 16 : 6)) - rewardTextX;
                String rl = "§f" + stack.getHoverName().getString();
                if (font.width(rl) > rmaxW) rl = font.plainSubstrByWidth(rl, rmaxW - 4) + "…";
                g.drawString(font, rl, rewardTextX, ry + 4, C_TEXT_DIM, false);
                g.drawString(font, "§8×" + ir.getCount(), rewardTextX, ry + 15, C_TEXT_FAINT, false);
            } else {
                String icon = switch (reward.getType()) {
                    case XP -> "§a✦";
                    case COMMAND -> "§b◆";
                    case LOOT_TABLE -> "§d❋";
                    case SCRIPT_EVENT -> "§e⚡";
                    default -> "§8?";
                };
                String typeLine = switch (reward.getType()) {
                    case XP -> "§8XP";
                    case COMMAND -> "§8command";
                    case LOOT_TABLE -> "§8loot table";
                    case SCRIPT_EVENT -> "§8script event";
                    default -> "§8reward";
                };
                int rmaxW = (width - MARGIN - (hov ? 16 : 6)) - rewardTextX - font.width(icon) - 4;
                String rl = reward.getSummary().getString();
                String[] rwrapped = wordWrap(rl, rmaxW);
                g.drawString(font, icon + " §7" + rwrapped[0], rewardTextX, ry + 4, C_TEXT_DIM, false);
                g.drawString(font, rwrapped[1] != null ? "§8" + rwrapped[1] : typeLine,
                        rewardTextX, ry + 15, C_TEXT_FAINT, false);
            }
            if (hov) g.drawString(font, "§c×", width - MARGIN - 12, ry + 9, 0xFFFF5555, false);
            ry += ROW_H;
        }
        if (rewards.isEmpty())
            g.drawString(font, "§8No rewards yet — add one below.", splitX + 6, listTop + 5, C_TEXT_FAINT, false);
        g.disableScissor();

        super.render(g, mx, my, partial);

        // ── Dropdowns ─────────────────────────────────────────────────────────
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        if (taskTypeDropOpen) {
            List<PhoenixTaskRegistry.TaskEntry> editorTypes = PhoenixTaskRegistry.getEditorTypes();
            int rowH = FIELD_H;
            int dropH = editorTypes.size() * rowH;
            int dy = Math.max(listTop, formTop - dropH - 2);
            g.fill(MARGIN, dy, MARGIN + colW, dy + dropH, C_PANEL);
            drawBorder(g, MARGIN, dy, colW, dropH, C_ACCENT);
            hoveredDropRow = -1;
            for (int i = 0; i < editorTypes.size(); i++) {
                PhoenixTaskRegistry.TaskEntry m = editorTypes.get(i);
                int dropRowY = dy + i * rowH;
                boolean hov = mx >= MARGIN && mx < MARGIN + colW && my >= dropRowY && my < dropRowY + rowH;
                if (hov) {
                    g.fill(MARGIN + 1, dropRowY, MARGIN + colW - 1, dropRowY + rowH, 0xFF1E1E2A);
                    hoveredDropRow = i;
                }
                g.drawString(font, m.editorIcon() + " §7" + m.editorLabel(), MARGIN + 5, dropRowY + 3,
                        hov ? C_TEXT : C_TEXT_DIM, false);
            }
            if (hoveredDropRow >= 0 && hoveredDropRow < editorTypes.size()) {
                PhoenixTaskRegistry.TaskEntry hm = editorTypes.get(hoveredDropRow);
                String tooltip = hm.editorTooltip() != null ? hm.editorTooltip() : hm.editorLabel();
                String[] lines = tooltip.split("\n");
                int maxLw = 0;
                for (String l : lines) maxLw = Math.max(maxLw, font.width(l));
                int tipW = maxLw + 10, tipH = lines.length * 10 + 6;
                int tipX = MARGIN + colW + 4;
                int tipY = Math.min(Math.max(dy + hoveredDropRow * rowH, 2), height - tipH - 2);
                if (tipX + tipW > width - 2) tipX = MARGIN - tipW - 4;
                g.fill(tipX, tipY, tipX + tipW, tipY + tipH, C_TOOLTIP_BG);
                drawBorder(g, tipX, tipY, tipW, tipH, C_ACCENT);
                for (int li = 0; li < lines.length; li++)
                    g.drawString(font, (li == 0 ? "§f" : "§8") + lines[li], tipX + 5, tipY + 3 + li * 10, 0xFFFFFFFF,
                            false);
            }
        }

        if (rewardTypeDropOpen) {
            int rowH = FIELD_H;
            int dropH = REWARD_TYPES.length * rowH;
            int dy = Math.max(listTop, formTop - dropH - 2);
            g.fill(splitX, dy, splitX + colW, dy + dropH, C_PANEL);
            drawBorder(g, splitX, dy, colW, dropH, C_ACCENT);
            for (int i = 0; i < REWARD_TYPES.length; i++) {
                int dropRowY = dy + i * rowH;
                boolean hov = mx >= splitX && mx < splitX + colW && my >= dropRowY && my < dropRowY + rowH;
                if (hov) g.fill(splitX + 1, dropRowY, splitX + colW - 1, dropRowY + rowH, 0xFF1E1E2A);
                g.drawString(font, "§7" + REWARD_TYPES[i], splitX + 5, dropRowY + 3,
                        hov ? C_TEXT : C_TEXT_DIM, false);
            }
        }

        g.pose().popPose();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 2) != 0;
        if (ctrl && key == 90) { // Ctrl+Z — undo
            undoLastChange();
            return true;
        }
        if (ctrl && key == 86 && copiedTaskNBT != null) {
            QuestTask pasted = deserializeTask(copiedTaskNBT.copy());
            if (pasted != null) {
                pasted = retaskId(pasted, "task_paste_" + System.currentTimeMillis());
                tasks.add(pasted);
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (taskTypeDropOpen) {
                List<PhoenixTaskRegistry.TaskEntry> edTypes = PhoenixTaskRegistry.getEditorTypes();
                int dropH = edTypes.size() * FIELD_H;
                int dy = Math.max(listTop, formTop - dropH - 2);
                for (int i = 0; i < edTypes.size(); i++) {
                    int ry2 = dy + i * FIELD_H;
                    if (mx >= MARGIN && mx < MARGIN + colW && my >= ry2 && my < ry2 + FIELD_H) {
                        taskType = edTypes.get(i).typeId();
                        taskTypeDropOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                taskTypeDropOpen = false;
                return true;
            }
            if (rewardTypeDropOpen) {
                int dropH = REWARD_TYPES.length * FIELD_H;
                int dy = Math.max(listTop, formTop - dropH - 2);
                for (int i = 0; i < REWARD_TYPES.length; i++) {
                    int ry2 = dy + i * FIELD_H;
                    if (mx >= splitX && mx < splitX + colW && my >= ry2 && my < ry2 + FIELD_H) {
                        rewardType = REWARD_TYPES[i];
                        rewardTypeDropOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                rewardTypeDropOpen = false;
                return true;
            }
            // Copy task
            if (hoveredTaskRow >= 0 && mx >= splitX - COL_GAP - 28 && mx < splitX - COL_GAP - 14) {
                copiedTaskNBT = tasks.get(hoveredTaskRow).serializeNBT();
                return true;
            }
            // Delete task
            if (hoveredTaskRow >= 0 && mx >= splitX - COL_GAP - 14 && mx < splitX - COL_GAP) {
                pushUndo();
                tasks.remove(hoveredTaskRow);
                hoveredTaskRow = -1;
                return true;
            }
            // Delete reward
            if (hoveredRewardRow >= 0 && mx >= width - MARGIN - 14 && mx < width - MARGIN) {
                pushUndo();
                rewards.remove(hoveredRewardRow);
                hoveredRewardRow = -1;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public void onClose() {
        flushToQuestNode();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static QuestTask deserializeTask(CompoundTag nbt) {
        QuestTask t = PhoenixTaskRegistry.deserialize(nbt);
        if (t != null) t.setOptional(nbt.getBoolean("optional"));
        return t;
    }

    private static QuestTask retaskId(QuestTask task, String newId) {
        CompoundTag nbt = task.serializeNBT();
        nbt.putString("task_id", "phoenixcore:" + newId);
        QuestTask copy = deserializeTask(nbt);
        return copy != null ? copy : task;
    }

    private PhoenixTaskRegistry.TaskEntry getTaskMeta(String typeId) {
        PhoenixTaskRegistry.TaskEntry e = PhoenixTaskRegistry.get(typeId);
        List<PhoenixTaskRegistry.TaskEntry> all = PhoenixTaskRegistry.getEditorTypes();
        return e != null ? e : (all.isEmpty() ? null : all.get(0));
    }

    private PhoenixTaskRegistry.TaskEntry getTaskMetaByClass(QuestTask task) {
        try {
            String typeId = task.serializeNBT().getString("type");
            return getTaskMeta(typeId);
        } catch (Exception ignored) {}
        List<PhoenixTaskRegistry.TaskEntry> all = PhoenixTaskRegistry.getEditorTypes();
        return all.isEmpty() ? null : all.get(0);
    }

    private ItemStack getTaskIconStack(QuestTask task) {
        ResourceLocation id = task.getDisplayItemId();
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return (item != null && item != Items.AIR) ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Nullable
    private String getTaskDetailString(QuestTask task) {
        if (task instanceof ItemRequirementTask t)
            return t.getItem() != null ?
                    t.getItem().getDefaultInstance().getHoverName().getString() + " ×" + t.getRequiredCount() : null;
        if (task instanceof CraftItemTask t) {
            Item item = ForgeRegistries.ITEMS.getValue(t.getItemId());
            return item != null ? item.getDefaultInstance().getHoverName().getString() + " ×" + t.getRequiredCount() :
                    t.getItemId().toString();
        }
        if (task instanceof KillEntityTask t)
            return t.getEntityId().getPath().replace('_', ' ') + " ×" + t.getRequiredCount();
        if (task instanceof FluidRequirementTask t)
            return t.getFluidId().getPath().replace('_', ' ') + "  " + t.getRequiredAmount() + " mB";
        if (task instanceof ExperienceTask t) return "Level " + t.getRequiredLevel();
        if (task instanceof TagItemTask t) return "#" + t.getTag().location().getPath() + " ×" + t.getRequired();
        return null;
    }

    /** Splits text at the last word boundary that fits within maxW pixels. Returns [line1, line2_or_null]. */
    private String[] wordWrap(String text, int maxW) {
        if (font.width(text) <= maxW) return new String[] { text, null };
        String sub = font.plainSubstrByWidth(text, maxW);
        int lastSpace = sub.lastIndexOf(' ');
        String line1 = lastSpace > 0 ? sub.substring(0, lastSpace) : sub;
        String rest = text.substring(line1.length()).trim();
        if (rest.isEmpty()) return new String[] { line1, null };
        if (font.width(rest) > maxW) rest = font.plainSubstrByWidth(rest, maxW - 4) + "…";
        return new String[] { line1, rest };
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
