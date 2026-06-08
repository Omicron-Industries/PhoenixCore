package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.*;
import net.phoenix.core.integration.phoenix_chronicles.tasks.*;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Task & reward editor for a single quest.
 *
 * Layout (fixed, never overlapping):
 *
 * ┌──────────────────────────┬──────────────────────────┐
 * │ TASKS [+Task] │ REWARDS [+Reward] │ ← header 22px
 * ├──────────────────────────┼──────────────────────────┤
 * │ ☠ Kill 10 Zombies [×] │ ■ 3× Diamond [×] │
 * │ ■ Collect 5 Ingots [×] │ │ ← scrollable list
 * │ │ │ (LIST_H px tall)
 * ├──────────────────────────┼──────────────────────────┤
 * │ ─── ADD TASK ──────── │ ─── ADD REWARD ──────── │ ← always-visible
 * │ [Type ▾] [desc______] │ [Type▾] [item____] [qty]│ form zone
 * │ [target__________][⊞] │ │ (FORM_H px tall)
 * │ [cnt][consume][✔ Add] │ [✔ Add reward] │
 * ├──────────────────────────┴──────────────────────────┤
 * │ [‹ Done] │ ← footer 22px
 * └─────────────────────────────────────────────────────┘
 */
public class TaskRewardEditorScreen extends Screen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_PANEL = 0xFF16161C;
    private static final int C_HEADER = 0xFF0C0C10;
    private static final int C_BORDER = 0xFF2A2A36;
    private static final int C_ACCENT = 0xFF884499;
    private static final int C_SPLIT = 0xFF2A2A36;
    private static final int C_ROW_HOVER = 0xFF1E1E2A;
    private static final int C_FORM_BG = 0xFF101016;
    private static final int C_TEXT = 0xFFDDDDE8;
    private static final int C_TEXT_DIM = 0xFF888898;
    private static final int C_TEXT_FAINT = 0xFF4A4A5A;
    private static final int C_TOOLTIP_BG = 0xFF0E0E16;

    // ── Fixed layout constants ────────────────────────────────────────────────
    private static final int HEADER_H = 22;
    private static final int FOOTER_H = 22;
    private static final int FORM_H = 82;  // fixed form zone height at bottom of each column
    private static final int ROW_H = 17;
    private static final int FIELD_H = 15;
    private static final int MARGIN = 8;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final QuestNode questNode;

    // Working copies — flushed back to questNode on Done
    private final List<QuestTask> tasks = new ArrayList<>();
    private final List<QuestReward> rewards = new ArrayList<>();

    // Task form
    private String taskType = "kill_entity";
    private boolean taskConsume = true;
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

    // Tooltip tracking for task-type dropdown
    private int hoveredDropRow = -1; // index into TASK_TYPES while dropdown is open

    // Panel geometry
    private int panelLeft, panelTop, panelW, panelH, splitX;
    private int listH; // height of the scrollable list area

    // ── Task type metadata ────────────────────────────────────────────────────

    private record TaskTypeMeta(String id, String icon, String label, String tooltip) {}

    private static final TaskTypeMeta[] TASK_TYPES = {
            new TaskTypeMeta("kill_entity", "§c☠", "Kill Entity",
                    "Kill a number of a specific mob type.\nTarget: entity registry id (e.g. minecraft:zombie)"),
            new TaskTypeMeta("item_check", "§e■", "Collect Item",
                    "Have a specific item in your inventory.\nTarget: item registry id. Consume: remove items on complete."),
            new TaskTypeMeta("craft_item", "§6⚒", "Craft Item",
                    "Craft a specific item the required number of times.\nTarget: item registry id."),
            new TaskTypeMeta("xp_check", "§a✦", "XP Level",
                    "Reach a minimum XP level.\nNo target needed — just set the required level."),
            new TaskTypeMeta("terminal_check", "§b◎", "Terminal / Location",
                    "Interact with a specific terminal block or location.\nTarget: terminal registry id."),
            new TaskTypeMeta("advancement", "§d★", "Advancement",
                    "Earn a specific Minecraft advancement.\nTarget: advancement id (e.g. minecraft:story/mine_diamond)"),
            new TaskTypeMeta("block_interact", "§7□", "Block Interact",
                    "Place or right-click a specific block.\nTarget: block id. Secondary: PLACE or RIGHT_CLICK."),
            new TaskTypeMeta("fluid_check", "§3≋", "Fluid Check",
                    "Have a fluid amount in a tank.\nTarget: fluid id. Count: amount in mB."),
            new TaskTypeMeta("stat_tracker", "§9≡", "Stat Tracker",
                    "Reach a value on a Minecraft statistic.\nTarget: stat id (e.g. minecraft:jump). Count: target value."),
            new TaskTypeMeta("dimension", "§5⊕", "Visit Dimension",
                    "Travel to a specific dimension.\nSecondary: dimension id (e.g. minecraft:the_nether)"),
    };

    private static final String[] REWARD_TYPES = { "item", "xp", "command" };

    // ── Constructor ───────────────────────────────────────────────────────────

    public TaskRewardEditorScreen(Screen parent, QuestNode questNode) {
        super(Component.literal("Tasks & Rewards"));
        this.parent = parent;
        this.questNode = questNode;
        this.tasks.addAll(questNode.getTasks());
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 560);
        panelH = Math.min(height - 20, 320);
        panelLeft = (width - panelW) / 2;
        panelTop = (height - panelH) / 2;
        splitX = panelLeft + panelW / 2;

        // List area: everything between header and the fixed form + footer zones
        listH = panelH - HEADER_H - FOOTER_H - FORM_H - 4;

        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();

        // ── Done ──────────────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("§7‹ Done"), b -> {
            flushToQuestNode();
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(panelLeft + MARGIN, panelTop + panelH - FOOTER_H + (FOOTER_H - 14) / 2, 56, 14).build());

        // ── Task form (anchored to the fixed form zone) ───────────────────────
        int taskFormX = panelLeft + MARGIN;
        int taskColW = splitX - panelLeft - MARGIN * 2;
        int formTop = panelTop + HEADER_H + listH + 6;  // always below list area

        // Type selector
        addRenderableWidget(Button.builder(
                Component.literal("§8Type: §7" + getTaskMeta(taskType).label() + " §8▾"),
                b -> {
                    taskTypeDropOpen = !taskTypeDropOpen;
                    rewardTypeDropOpen = false;
                }).bounds(taskFormX, formTop, taskColW, FIELD_H).build());

        int fy = formTop + FIELD_H + 3;

        // Description
        taskDescBox = new EditBox(font, taskFormX, fy, taskColW, FIELD_H, Component.empty());
        taskDescBox.setHint(Component.literal("§8Task description shown to player"));
        taskDescBox.setMaxLength(128);
        addRenderableWidget(taskDescBox);
        fy += FIELD_H + 3;

        // Target field (hidden for xp_check and dimension)
        boolean needsTarget = !taskType.equals("xp_check") && !taskType.equals("dimension");
        boolean needsSecond = taskType.equals("block_interact") || taskType.equals("stat_tracker") ||
                taskType.equals("dimension");
        boolean needsCount = switch (taskType) {
            case "kill_entity", "item_check", "craft_item", "xp_check", "fluid_check", "stat_tracker" -> true;
            default -> false;
        };
        boolean showConsume = switch (taskType) {
            case "kill_entity", "item_check", "craft_item", "fluid_check", "terminal_check", "stat_tracker", "block_interact" -> true;
            default -> false;
        };

        if (needsTarget) {
            String hint = switch (taskType) {
                case "kill_entity" -> "§8Entity id  (e.g. minecraft:zombie)";
                case "item_check", "craft_item" -> "§8Item id  (e.g. minecraft:iron_ingot)";
                case "terminal_check" -> "§8Terminal id";
                case "advancement" -> "§8Advancement id  (e.g. minecraft:story/root)";
                case "block_interact" -> "§8Block id  (e.g. minecraft:furnace)";
                case "fluid_check" -> "§8Fluid id  (e.g. minecraft:water)";
                case "stat_tracker" -> "§8Stat id  (e.g. minecraft:jump)";
                default -> "§8Target id";
            };
            boolean hasItemPicker = taskType.equals("item_check") || taskType.equals("craft_item");
            int tw = hasItemPicker ? taskColW - 18 : taskColW;
            taskTargetBox = new EditBox(font, taskFormX, fy, tw, FIELD_H, Component.empty());
            taskTargetBox.setHint(Component.literal(hint));
            taskTargetBox.setMaxLength(160);
            addRenderableWidget(taskTargetBox);
            if (hasItemPicker) {
                addRenderableWidget(Button.builder(Component.literal("§7⊞"), b -> {
                    if (minecraft != null) minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (id != null && taskTargetBox != null) taskTargetBox.setValue(id.toString());
                    }));
                }).bounds(taskFormX + tw, fy, 16, FIELD_H).build());
            }
            fy += FIELD_H + 3;
        }

        if (needsSecond) {
            String hint2 = switch (taskType) {
                case "block_interact" -> "§8PLACE or RIGHT_CLICK";
                case "dimension" -> "§8Dimension id  (e.g. minecraft:the_nether)";
                default -> "§8Secondary value";
            };
            taskSecondaryBox = new EditBox(font, taskFormX, fy, taskColW, FIELD_H, Component.empty());
            taskSecondaryBox.setHint(Component.literal(hint2));
            taskSecondaryBox.setMaxLength(128);
            addRenderableWidget(taskSecondaryBox);
            fy += FIELD_H + 3;
        }

        // Count + Consume + Add on the last row
        int rowY = formTop + FORM_H - FIELD_H - 4;
        if (needsCount) {
            String countHint = switch (taskType) {
                case "xp_check" -> "§8XP level";
                case "fluid_check" -> "§8mB amount";
                case "stat_tracker" -> "§8Target value";
                default -> "§8Count";
            };
            taskCountBox = new EditBox(font, taskFormX, rowY, 52, FIELD_H, Component.empty());
            taskCountBox.setHint(Component.literal(countHint));
            taskCountBox.setMaxLength(8);
            addRenderableWidget(taskCountBox);
        }
        if (showConsume) {
            int cx = needsCount ? taskFormX + 56 : taskFormX;
            addRenderableWidget(Button.builder(
                    Component.literal(taskConsume ? "§aConsume" : "§8Consume"),
                    b -> {
                        taskConsume = !taskConsume;
                        rebuildWidgets();
                    }).bounds(cx, rowY, 58, FIELD_H).build());
        }
        addRenderableWidget(Button.builder(Component.literal("§a✔ Add"),
                b -> commitTaskFromForm()).bounds(taskFormX + taskColW - 46, rowY, 46, FIELD_H).build());

        // ── Reward form ───────────────────────────────────────────────────────
        int rewardFormX = splitX + MARGIN;
        int rewardColW = panelLeft + panelW - splitX - MARGIN * 2;

        addRenderableWidget(Button.builder(
                Component.literal("§8Type: §7" + rewardType + " §8▾"),
                b -> {
                    rewardTypeDropOpen = !rewardTypeDropOpen;
                    taskTypeDropOpen = false;
                }).bounds(rewardFormX, formTop, rewardColW, FIELD_H).build());

        int rfy = formTop + FIELD_H + 3;
        if (rewardType.equals("item")) {
            String itemLabel = rewardPickedItem != null ? "§f" + rewardPickedItem.getHoverName().getString() :
                    "§8Pick item…";
            addRenderableWidget(Button.builder(Component.literal(itemLabel), b -> {
                if (minecraft != null) minecraft.setScreen(new ItemPickerScreen(this, stack -> {
                    rewardPickedItem = stack;
                    rebuildWidgets();
                }));
            }).bounds(rewardFormX, rfy, rewardColW - 44, FIELD_H).build());
            rewardCountBox = new EditBox(font, rewardFormX + rewardColW - 42, rfy, 42, FIELD_H, Component.empty());
            rewardCountBox.setHint(Component.literal("§8Qty"));
            rewardCountBox.setMaxLength(4);
            addRenderableWidget(rewardCountBox);
        } else if (rewardType.equals("xp")) {
            rewardCountBox = new EditBox(font, rewardFormX, rfy, rewardColW, FIELD_H, Component.empty());
            rewardCountBox.setHint(Component.literal("§8XP levels to award"));
            rewardCountBox.setMaxLength(5);
            addRenderableWidget(rewardCountBox);
        } else {
            rewardCommandBox = new EditBox(font, rewardFormX, rfy, rewardColW, FIELD_H, Component.empty());
            rewardCommandBox.setHint(Component.literal("§8/give %player% …"));
            rewardCommandBox.setMaxLength(256);
            addRenderableWidget(rewardCommandBox);
        }

        addRenderableWidget(Button.builder(Component.literal("§a✔ Add reward"),
                b -> commitRewardFromForm())
                .bounds(rewardFormX + rewardColW - 80, formTop + FORM_H - FIELD_H - 4, 80, FIELD_H).build());
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

        boolean needsTarget = !taskType.equals("xp_check") && !taskType.equals("dimension");
        if (desc.isEmpty() || (needsTarget && target.isEmpty())) return;

        ResourceLocation taskId = new ResourceLocation("phoenixcore",
                "task_" + taskType + "_" + System.currentTimeMillis());
        Component descComp = Component.literal(desc);

        QuestTask task = null;
        try {
            task = switch (taskType) {
                case "kill_entity" -> new KillEntityTask(taskId, descComp,
                        new ResourceLocation(target), count, taskConsume);
                case "item_check" -> {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));
                    yield item != null ? new ItemRequirementTask(taskId, descComp, item, count, taskConsume) : null;
                }
                case "craft_item" -> new CraftItemTask(taskId, descComp, new ResourceLocation(target), count);
                case "xp_check" -> new ExperienceTask(taskId, descComp, count);
                case "terminal_check" -> new LocationOrTerminalTask(taskId, descComp, new ResourceLocation(target),
                        taskConsume);
                case "advancement" -> new AdvancementTask(taskId, descComp, new ResourceLocation(target));
                case "block_interact" -> {
                    var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(target));
                    String mode = second.isEmpty() ? "PLACE" : second.toUpperCase();
                    yield block != null ? new BlockInteractTask(taskId, descComp, block, mode) : null;
                }
                case "fluid_check" -> new FluidRequirementTask(taskId, descComp, new ResourceLocation(target), count,
                        taskConsume);
                case "stat_tracker" -> new StatTrackerTask(taskId, descComp, new ResourceLocation(target), count,
                        taskConsume);
                case "dimension" -> {
                    String dim = second.isEmpty() ? "minecraft:overworld" : second;
                    yield new DimensionTask(taskId, descComp,
                            net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DIMENSION,
                                    new ResourceLocation(dim)));
                }
                default -> null;
            };
        } catch (Exception ignored) {}

        if (task != null) {
            tasks.add(task);
            // Clear form fields
            taskTypeDropOpen = false;
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
            default -> null;
        };

        if (reward != null) {
            rewards.add(reward);
            rewardPickedItem = null;
            rewardTypeDropOpen = false;
            rebuildWidgets();
        }
    }

    // ── Flush to QuestNode ────────────────────────────────────────────────────

    private void flushToQuestNode() {
        // Replace ALL tasks — not just append — so editing is non-destructive
        questNode.clearTasks();
        for (QuestTask t : tasks) questNode.addTask(t);
        // TODO: questNode.setRewards(rewards) once QuestNode exposes a rewards list
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x88000000);

        // Panel
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, C_PANEL);
        drawBorder(g, panelLeft, panelTop, panelW, panelH, C_ACCENT);

        // Header
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + HEADER_H, C_HEADER);
        g.fill(panelLeft, panelTop + HEADER_H - 1, panelLeft + panelW, panelTop + HEADER_H, C_BORDER);
        g.drawCenteredString(font, "§dTasks & Rewards  §8— §7" + questNode.getId().getPath(),
                panelLeft + panelW / 2, panelTop + 7, C_TEXT);

        // Column sub-headers
        g.drawString(font, "§8TASKS (" + tasks.size() + ")", panelLeft + MARGIN, panelTop + HEADER_H + 4, C_TEXT_FAINT);
        g.drawString(font, "§8REWARDS (" + rewards.size() + ")", splitX + MARGIN, panelTop + HEADER_H + 4,
                C_TEXT_FAINT);

        // Centre divider
        g.fill(splitX, panelTop + HEADER_H, splitX + 1, panelTop + panelH - FOOTER_H, C_SPLIT);

        // Form zone separator
        int formTop = panelTop + HEADER_H + listH + 6;
        g.fill(panelLeft + MARGIN, formTop - 4, splitX - MARGIN, formTop - 3, C_BORDER);
        g.drawString(font, "§8ADD TASK", panelLeft + MARGIN, formTop - 12, C_TEXT_FAINT);
        g.fill(splitX + MARGIN, formTop - 4, panelLeft + panelW - MARGIN, formTop - 3, C_BORDER);
        g.drawString(font, "§8ADD REWARD", splitX + MARGIN, formTop - 12, C_TEXT_FAINT);

        // Form background
        g.fill(panelLeft, formTop - 14, splitX, panelTop + panelH - FOOTER_H, C_FORM_BG);
        g.fill(splitX + 1, formTop - 14, panelLeft + panelW, panelTop + panelH - FOOTER_H, C_FORM_BG);

        // Footer
        g.fill(panelLeft, panelTop + panelH - FOOTER_H, panelLeft + panelW, panelTop + panelH - FOOTER_H + 1, C_BORDER);

        // ── Task list ─────────────────────────────────────────────────────────
        int listTop = panelTop + HEADER_H + 16;
        int listBottom = formTop - 14;
        g.enableScissor(panelLeft, listTop, splitX, listBottom);
        hoveredTaskRow = -1;
        int ty = listTop;
        for (int i = 0; i < tasks.size(); i++) {
            QuestTask task = tasks.get(i);
            if (ty + ROW_H > listBottom) break; // clipped
            boolean hov = mx >= panelLeft + MARGIN && mx < splitX - 2 && my >= ty && my < ty + ROW_H;
            if (hov) {
                g.fill(panelLeft + MARGIN, ty, splitX - 2, ty + ROW_H, C_ROW_HOVER);
                hoveredTaskRow = i;
            }
            TaskTypeMeta meta = getTaskMetaByClass(task);
            g.drawString(font, meta.icon(), panelLeft + MARGIN + 2, ty + 4, 0xFFFFFFFF);
            String label = task.getDescription().getString();
            int maxW = (splitX - panelLeft) - MARGIN * 2 - 16 - 10;
            if (font.width(label) > maxW) label = font.plainSubstrByWidth(label, maxW - 4) + "…";
            g.drawString(font, "§7" + label, panelLeft + MARGIN + 14, ty + 4, C_TEXT_DIM);
            if (hov) g.drawString(font, "§c×", splitX - 11, ty + 4, 0xFFFF5555);
            ty += ROW_H;
        }
        if (tasks.isEmpty())
            g.drawString(font, "§8No tasks yet. Add one below.", panelLeft + MARGIN + 4, listTop + 4, C_TEXT_FAINT);
        g.disableScissor();

        // ── Reward list ───────────────────────────────────────────────────────
        g.enableScissor(splitX + 1, listTop, panelLeft + panelW, listBottom);
        hoveredRewardRow = -1;
        int ry = listTop;
        for (int i = 0; i < rewards.size(); i++) {
            QuestReward reward = rewards.get(i);
            if (ry + ROW_H > listBottom) break;
            boolean hov = mx >= splitX + MARGIN && mx < panelLeft + panelW - 2 && my >= ry && my < ry + ROW_H;
            if (hov) {
                g.fill(splitX + MARGIN, ry, panelLeft + panelW - 2, ry + ROW_H, C_ROW_HOVER);
                hoveredRewardRow = i;
            }
            String icon = switch (reward.getType()) {
                case ITEM -> "§e■";
                case XP -> "§a✦";
                case COMMAND -> "§b>";
            };
            String rl = reward.getSummary().getString();
            int rmaxW = (panelLeft + panelW - splitX) - MARGIN * 2 - 14;
            if (font.width(rl) > rmaxW) rl = font.plainSubstrByWidth(rl, rmaxW - 4) + "…";
            g.drawString(font, icon + " §7" + rl, splitX + MARGIN + 2, ry + 4, C_TEXT_DIM);
            if (hov) g.drawString(font, "§c×", panelLeft + panelW - 12, ry + 4, 0xFFFF5555);
            ry += ROW_H;
        }
        if (rewards.isEmpty()) g.drawString(font, "§8No rewards yet.", splitX + MARGIN + 4, listTop + 4, C_TEXT_FAINT);
        g.disableScissor();

        // Widgets
        super.render(g, mx, my, partial);

        // ── Dropdowns + tooltips rendered at elevated z ───────────────────────
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        if (taskTypeDropOpen) {
            int dx = panelLeft + MARGIN;
            int dw = splitX - panelLeft - MARGIN * 2;
            int rowH = FIELD_H;
            int dropH = TASK_TYPES.length * rowH;
            // Anchor: button top is formTop. Open ABOVE the button if dropdown would clip below screen.
            int buttonTop = formTop;
            int dy = (buttonTop + FIELD_H + dropH > panelTop + panelH - FOOTER_H - 2) ? buttonTop - dropH          // open
                                                                                                                   // upward
                    : buttonTop + FIELD_H;       // open downward
            // Clamp so top never goes above panel
            dy = Math.max(panelTop + HEADER_H, dy);

            g.fill(dx, dy, dx + dw, dy + dropH, C_PANEL);
            drawBorder(g, dx, dy, dw, dropH, C_ACCENT);
            hoveredDropRow = -1;
            for (int i = 0; i < TASK_TYPES.length; i++) {
                TaskTypeMeta m = TASK_TYPES[i];
                int rowY = dy + i * rowH;
                boolean hov = mx >= dx && mx < dx + dw && my >= rowY && my < rowY + rowH;
                if (hov) {
                    g.fill(dx + 1, rowY, dx + dw - 1, rowY + rowH, C_ROW_HOVER);
                    hoveredDropRow = i;
                }
                g.drawString(font, m.icon() + " §7" + m.label(), dx + 5, rowY + 3,
                        hov ? C_TEXT : C_TEXT_DIM);
            }

            // Tooltip for hovered row — always on the right, clamped to screen
            if (hoveredDropRow >= 0) {
                TaskTypeMeta hm = TASK_TYPES[hoveredDropRow];
                String[] lines = hm.tooltip().split("\n");
                int maxLw = 0;
                for (String l : lines) maxLw = Math.max(maxLw, font.width(l));
                int tipW = maxLw + 10;
                int tipH = lines.length * 10 + 6;
                int tipX = dx + dw + 4;
                int tipY = dy + hoveredDropRow * rowH;
                // Clamp right
                if (tipX + tipW > width - 2) tipX = dx - tipW - 4;
                // Clamp bottom
                if (tipY + tipH > height - 2) tipY = height - tipH - 2;
                // Clamp top
                if (tipY < 2) tipY = 2;
                g.fill(tipX, tipY, tipX + tipW, tipY + tipH, C_TOOLTIP_BG);
                drawBorder(g, tipX, tipY, tipW, tipH, C_ACCENT);
                for (int li = 0; li < lines.length; li++) {
                    g.drawString(font, (li == 0 ? "§f" : "§8") + lines[li],
                            tipX + 5, tipY + 3 + li * 10, 0xFFFFFFFF);
                }
            }
        }

        if (rewardTypeDropOpen) {
            int dx = splitX + MARGIN;
            int dw = panelLeft + panelW - splitX - MARGIN * 2;
            int rowH = FIELD_H;
            int dropH = REWARD_TYPES.length * rowH;
            int buttonTop = formTop;
            int dy = (buttonTop + FIELD_H + dropH > panelTop + panelH - FOOTER_H - 2) ? buttonTop - dropH :
                    buttonTop + FIELD_H;
            dy = Math.max(panelTop + HEADER_H, dy);

            g.fill(dx, dy, dx + dw, dy + dropH, C_PANEL);
            drawBorder(g, dx, dy, dw, dropH, C_ACCENT);
            for (int i = 0; i < REWARD_TYPES.length; i++) {
                int rowY = dy + i * rowH;
                boolean hov = mx >= dx && mx < dx + dw && my >= rowY && my < rowY + rowH;
                if (hov) g.fill(dx + 1, rowY, dx + dw - 1, rowY + rowH, C_ROW_HOVER);
                g.drawString(font, "§7" + REWARD_TYPES[i], dx + 5, rowY + 3, hov ? C_TEXT : C_TEXT_DIM);
            }
        }

        g.pose().popPose();
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (taskTypeDropOpen) {
                int dx = panelLeft + MARGIN;
                int dw = splitX - panelLeft - MARGIN * 2;
                int dropH = TASK_TYPES.length * FIELD_H;
                int buttonTop = panelTop + HEADER_H + listH + 6;
                int dy = (buttonTop + FIELD_H + dropH > panelTop + panelH - FOOTER_H - 2) ? buttonTop - dropH :
                        buttonTop + FIELD_H;
                dy = Math.max(panelTop + HEADER_H, dy);
                for (int i = 0; i < TASK_TYPES.length; i++) {
                    int rowY = dy + i * FIELD_H;
                    if (mx >= dx && mx < dx + dw && my >= rowY && my < rowY + FIELD_H) {
                        taskType = TASK_TYPES[i].id();
                        taskTypeDropOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                taskTypeDropOpen = false;
                return true;
            }
            if (rewardTypeDropOpen) {
                int dx = splitX + MARGIN;
                int dw = panelLeft + panelW - splitX - MARGIN * 2;
                int dropH = REWARD_TYPES.length * FIELD_H;
                int buttonTop = panelTop + HEADER_H + listH + 6;
                int dy = (buttonTop + FIELD_H + dropH > panelTop + panelH - FOOTER_H - 2) ? buttonTop - dropH :
                        buttonTop + FIELD_H;
                dy = Math.max(panelTop + HEADER_H, dy);
                for (int i = 0; i < REWARD_TYPES.length; i++) {
                    int rowY = dy + i * FIELD_H;
                    if (mx >= dx && mx < dx + dw && my >= rowY && my < rowY + FIELD_H) {
                        rewardType = REWARD_TYPES[i];
                        rewardTypeDropOpen = false;
                        rebuildWidgets();
                        return true;
                    }
                }
                rewardTypeDropOpen = false;
                return true;
            }
            if (hoveredTaskRow >= 0 && mx >= splitX - 13 && mx < splitX - 1) {
                tasks.remove(hoveredTaskRow);
                hoveredTaskRow = -1;
                return true;
            }
            if (hoveredRewardRow >= 0 && mx >= panelLeft + panelW - 14 && mx < panelLeft + panelW - 2) {
                rewards.remove(hoveredRewardRow);
                hoveredRewardRow = -1;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TaskTypeMeta getTaskMeta(String typeId) {
        for (TaskTypeMeta m : TASK_TYPES) if (m.id().equals(typeId)) return m;
        return TASK_TYPES[0];
    }

    private TaskTypeMeta getTaskMetaByClass(QuestTask task) {
        String typeId = switch (task.getClass().getSimpleName()) {
            case "KillEntityTask" -> "kill_entity";
            case "ItemRequirementTask" -> "item_check";
            case "CraftItemTask" -> "craft_item";
            case "ExperienceTask" -> "xp_check";
            case "LocationOrTerminalTask" -> "terminal_check";
            case "AdvancementTask" -> "advancement";
            case "BlockInteractTask" -> "block_interact";
            case "FluidRequirementTask" -> "fluid_check";
            case "StatTrackerTask" -> "stat_tracker";
            case "DimensionTask" -> "dimension";
            default -> "kill_entity";
        };
        return getTaskMeta(typeId);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
