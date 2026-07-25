package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.phoenix_chronicles.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DevWikiScreen extends Screen {

    private int C_BG, C_PANEL, C_HEADER, C_BORDER, C_ACCENT, C_TEXT, C_TEXT_DIM, C_TEXT_FAINT, C_DONE, C_ACTIVE;

    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 28;
    private static final int SIDEBAR_W = 96;
    private static final int MARGIN = 10;
    private static final int LINE_H = 12;

    private static final String[] PAGE_NAMES = {
            "Overview", "Canvas", "Quest Fields", "Tasks", "Rewards", "SNBT Format", "Live Stats", "API Reference"
    };

    private final Screen parent;
    private int activePage = 0;
    private int scrollY = 0;
    private int cachedContentH = 0; 

    private final List<int[]> copyBtnBounds = new ArrayList<>(); 
    private final List<String> copyBtnTexts = new ArrayList<>();

    private enum LT {
        HEADING,
        SUBHEADING,
        KV,
        TEXT,
        INDENT,
        DIVIDER,
        SPACER,
        CODE
    }

    private record WLine(LT type, String a, String b) {

        static WLine h(String s) {
            return new WLine(LT.HEADING, s, "");
        }

        static WLine sh(String s) {
            return new WLine(LT.SUBHEADING, s, "");
        }

        static WLine kv(String k, String v) {
            return new WLine(LT.KV, k, v);
        }

        static WLine t(String s) {
            return new WLine(LT.TEXT, s, "");
        }

        static WLine in(String s) {
            return new WLine(LT.INDENT, s, "");
        }

        static WLine div() {
            return new WLine(LT.DIVIDER, "", "");
        }

        static WLine sp() {
            return new WLine(LT.SPACER, "", "");
        }

        static WLine code(String s) {
            return new WLine(LT.CODE, s, "");
        }
    }

    public DevWikiScreen(Screen parent) {
        super(Component.literal("Dev Wiki"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ChroniclesTheme t = ChroniclesTheme.current();
        C_BG = t.bg.getColor();
        C_PANEL = t.panel.getColor();
        C_HEADER = t.header.getColor();
        C_BORDER = t.border.getColor();
        C_ACCENT = t.accent.getColor();
        C_TEXT = t.text.getColor();
        C_TEXT_DIM = t.textDim.getColor();
        C_TEXT_FAINT = t.textFaint.getColor();
        C_DONE = t.done.getColor();
        C_ACTIVE = t.activeColor.getColor();

        clearWidgets();

        int tabY = HEADER_H + 8;
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            final int idx = i;
            boolean sel = i == activePage;
            addRenderableWidget(Button.builder(
                    Component.literal((sel ? "§f" : "§8") + PAGE_NAMES[i]),
                    b -> {
                        activePage = idx;
                        scrollY = 0;
                        rebuildWidgets();
                    })
                    .bounds(2, tabY, SIDEBAR_W - 4, 14).build());
            tabY += 16;
        }

        addRenderableWidget(Button.builder(Component.literal("§7✕ Close"),
                b -> {
                    if (minecraft != null) minecraft.setScreen(parent);
                })
                .bounds(width / 2 - 36, height - FOOTER_H + 6, 72, 16).build());
    }

    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, C_BG);

        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.drawCenteredString(font, "§fDev Wiki  §8— §7" + PAGE_NAMES[activePage], width / 2, 10, C_TEXT);

        g.fill(0, HEADER_H, SIDEBAR_W, height - FOOTER_H, C_PANEL);
        g.fill(SIDEBAR_W - 1, HEADER_H, SIDEBAR_W, height, C_BORDER);

        int tabY = HEADER_H + 8 + activePage * 16;
        g.fill(0, tabY - 1, 2, tabY + 15, C_ACCENT);

        g.fill(0, height - FOOTER_H, width, height, C_HEADER);
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, C_BORDER);

        int cx = SIDEBAR_W + MARGIN;
        int cw = width - cx - MARGIN;
        int contentTop = HEADER_H + MARGIN;
        int contentBot = height - FOOTER_H - MARGIN;

        g.enableScissor(cx, contentTop, cx + cw, contentBot);

        copyBtnBounds.clear();
        copyBtnTexts.clear();

        List<WLine> lines = buildPage(activePage);
        int y = contentTop - scrollY;
        for (WLine line : lines) {
            y = renderLine(g, line, cx, y, cw, contentTop, contentBot, mx, my);
        }

        g.disableScissor();

        int totalH = lines.stream().mapToInt(this::lineHeight).sum();
        cachedContentH = totalH;
        int visibleH = contentBot - contentTop;
        if (totalH > visibleH) {
            int trackH = visibleH;
            int thumbH = Math.max(16, trackH * visibleH / totalH);
            int thumbY = contentTop + (int) ((long) scrollY * (trackH - thumbH) / Math.max(1, totalH - visibleH));
            g.fill(width - MARGIN / 2 - 2, contentTop, width - MARGIN / 2, contentBot, 0x22FFFFFF);
            g.fill(width - MARGIN / 2 - 2, thumbY, width - MARGIN / 2, thumbY + thumbH, 0x88FFFFFF);
        }

        super.render(g, mx, my, partial);
    }

    private int renderLine(GuiGraphics g, WLine line, int x, int y, int w, int top, int bot, int mx, int my) {
        if (y > bot) return y + lineHeight(line);
        switch (line.type()) {
            case HEADING -> {
                if (y + LINE_H >= top) {
                    g.fill(x, y + LINE_H + 1, x + w, y + LINE_H + 2, C_ACCENT);
                    g.drawString(font, "§f" + line.a(), x, y + 2, C_TEXT, false);
                }
                return y + LINE_H + 6;
            }
            case SUBHEADING -> {
                if (y + LINE_H >= top)
                    g.drawString(font, line.a(), x, y + 2, C_TEXT_DIM, false);
                return y + LINE_H + 4;
            }
            case KV -> {
                if (y + LINE_H >= top) {
                    int kw = font.width(line.a() + "  ");
                    g.drawString(font, line.a(), x, y, C_ACCENT, false);
                    
                    String val = line.b();
                    int maxVW = w - kw;
                    if (font.width(val) <= maxVW) {
                        g.drawString(font, val, x + kw, y, C_TEXT_DIM, false);
                    } else {
                        String clipped = font.plainSubstrByWidth(val, maxVW - 6) + "…";
                        g.drawString(font, clipped, x + kw, y, C_TEXT_DIM, false);
                    }
                }
                return y + LINE_H + 1;
            }
            case TEXT -> {
                if (y + LINE_H >= top)
                    g.drawString(font, line.a(), x, y, C_TEXT_DIM, false);
                return y + LINE_H + 1;
            }
            case INDENT -> {
                if (y + LINE_H >= top)
                    g.drawString(font, "• " + line.a(), x + 8, y, C_TEXT_DIM, false);
                return y + LINE_H + 1;
            }
            case DIVIDER -> {
                if (y + 1 >= top)
                    g.fill(x, y + 3, x + w, y + 4, C_BORDER);
                return y + 8;
            }
            case SPACER -> {
                return y + 6;
            }
            case CODE -> {
                int lh = LINE_H + 4;
                if (y + lh >= top) {
                    
                    g.fill(x, y, x + w, y + lh, 0xFF0A0A12);
                    g.fill(x, y, x + 1, y + lh, C_BORDER);
                    
                    int btnW = font.width("⎘") + 8;
                    int btnX = x + w - btnW - 2;
                    int btnY2 = y + 1;
                    int btnH2 = lh - 2;
                    boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY2 && my < btnY2 + btnH2;
                    g.fill(btnX, btnY2, btnX + btnW, btnY2 + btnH2, hov ? 0x44FFFFFF : 0x22FFFFFF);
                    g.drawCenteredString(font, hov ? "§f⎘" : "§7⎘", btnX + btnW / 2, btnY2 + 2, C_TEXT_DIM);
                    
                    String code = line.a();
                    int maxCW = btnX - x - 6;
                    String display = font.width(code) <= maxCW ? code : font.plainSubstrByWidth(code, maxCW - 4) + "…";
                    g.drawString(font, display, x + 4, y + 3, C_TEXT, false);
                    
                    copyBtnBounds.add(new int[] { btnX, btnY2, btnX + btnW, btnY2 + btnH2 });
                    copyBtnTexts.add(line.a());
                }
                return y + lh + 1;
            }
        }
        return y + LINE_H;
    }

    private int lineHeight(WLine line) {
        return switch (line.type()) {
            case HEADING -> LINE_H + 6;
            case SUBHEADING -> LINE_H + 4;
            case KV, TEXT, INDENT -> LINE_H + 1;
            case DIVIDER -> 8;
            case SPACER -> 6;
            case CODE -> LINE_H + 5;
        };
    }

    private List<WLine> buildPage(int page) {
        return switch (page) {
            case 0 -> pageOverview();
            case 1 -> pageCanvas();
            case 2 -> pageQuestFields();
            case 3 -> pageTasks();
            case 4 -> pageRewards();
            case 5 -> pageSnbtFormat();
            case 6 -> pageLiveStats();
            case 7 -> pageApiReference();
            default -> List.of();
        };
    }

    private List<WLine> pageOverview() {
        int total = QuestTreeRegistry.getAllQuests().size();
        int cats = QuestTreeRegistry.getRootChapters().values().stream()
                .map(QuestNode::getCategory).distinct().mapToInt(c -> 1).sum();
        
        Set<String> catSet = new HashSet<>();
        QuestTreeRegistry.getAllQuests().values().forEach(n -> catSet.add(n.getCategory()));

        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Phoenix Chronicles — Dev Reference"));
        lines.add(WLine.t("In-game quest system for Minecraft Forge 1.20.1."));
        lines.add(WLine.t("Dev mode activates automatically in Creative or at op level ≥ 2."));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Live registry"));
        lines.add(WLine.kv("Quests loaded:", String.valueOf(total)));
        lines.add(WLine.kv("Categories:", String.valueOf(catSet.size())));
        lines.add(WLine.kv("Task types:", String.valueOf(PhoenixTaskRegistry.getEditorTypes().size()) + " registered"));
        lines.add(WLine.sp());
        lines.add(WLine.div());
        lines.add(WLine.sh("Quick navigation"));
        lines.add(WLine.kv("Canvas", "Pan, zoom, right-click, Alt+drag dep lines"));
        lines.add(WLine.kv("Quest Fields", "All SNBT keys and their meanings"));
        lines.add(WLine.kv("Tasks", "Every task type with expected fields"));
        lines.add(WLine.kv("Rewards", "All reward types and their parameters"));
        lines.add(WLine.kv("SNBT Format", "Full file format reference"));
        lines.add(WLine.kv("Live Stats", "Per-category quest counts and type breakdown"));
        lines.add(WLine.sp());
        lines.add(WLine.div());
        lines.add(WLine.sh("Tutorial quests"));
        lines.add(WLine.t("Add tutorial_steps to any quest SNBT to attach a guided overlay."));
        lines.add(WLine.t("Each step has a text field and an optional highlight target:"));
        lines.add(WLine.in("none, sidebar, canvas, toolbar, node:{quest_id}"));
        lines.add(WLine.t("Progress is stored client-side in config/phoenix_chronicles/tutorial_progress.dat."));
        return lines;
    }

    private List<WLine> pageCanvas() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Canvas Controls"));
        lines.add(WLine.sh("Mouse"));
        lines.add(WLine.kv("Left drag", "Pan the canvas"));
        lines.add(WLine.kv("Scroll wheel", "Zoom in / out"));
        lines.add(WLine.kv("Left click node", "Select / open quest detail"));
        lines.add(WLine.kv("Right click node", "Context menu (Edit, Delete, Move category…)"));
        lines.add(WLine.kv("Right click canvas", "Open dep-line settings (or unlock path for players)"));
        lines.add(WLine.kv("Alt + drag node", "Draw dependency line to another node"));
        lines.add(WLine.kv("Middle click", "Reset pan offset"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Keyboard"));
        lines.add(WLine.kv("F", "Fit all nodes to view"));
        lines.add(WLine.kv("Ctrl+F", "Open quest search overlay"));
        lines.add(WLine.kv("Ctrl+Z", "Undo last node move or dep-line change"));
        lines.add(WLine.kv("Ctrl+Y", "Redo"));
        lines.add(WLine.kv("L", "Toggle line style (Spline ↔ Straight)"));
        lines.add(WLine.kv("V", "Toggle validation panel (dev)"));
        lines.add(WLine.kv("I", "Run FTB import (dev)"));
        lines.add(WLine.kv("?", "Open this wiki"));
        lines.add(WLine.kv("ESC", "Close menus / deselect"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Multi-select (dev mode)"));
        lines.add(WLine.kv("Shift+click", "Add node to selection"));
        lines.add(WLine.kv("Ctrl+drag", "Box-select nodes"));
        lines.add(WLine.kv("Ctrl+G", "Group selected nodes"));
        lines.add(WLine.kv("Del", "Delete selected nodes"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Zoom"));
        lines.add(WLine.kv("Range", "35% – 250%  (scroll or pinch)"));
        lines.add(WLine.kv("Step", "12% per scroll tick"));
        return lines;
    }

    private List<WLine> pageQuestFields() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Quest SNBT Fields"));
        lines.add(WLine.t("All fields are optional except id and title."));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Identity"));
        lines.add(WLine.kv("id", "Path portion only — e.g. \"my_quest\" → phoenixcore:my_quest"));
        lines.add(WLine.kv("title", "Display name shown in quest headers and search"));
        lines.add(WLine.kv("description", "Lore / body text"));
        lines.add(WLine.kv("subtitle", "Smaller text below the title on the detail card"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Appearance"));
        lines.add(WLine.kv("category", "Chapter tab name — e.g. MAIN, MAGIC, COMBAT"));
        lines.add(
                WLine.kv("shape", "SQUARE · CIRCLE · DIAMOND · HEXAGON · TRIANGLE · STAR · PENTAGON · SHIELD · CROSS"));
        lines.add(WLine.kv("icon_item", "Item id for the node icon — e.g. minecraft:diamond"));
        lines.add(WLine.kv("positionX", "Canvas X coordinate (pixels from left edge)"));
        lines.add(WLine.kv("positionY", "Canvas Y coordinate (pixels from top edge)"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Visibility"));
        lines.add(WLine.kv("visibility", "NORMAL · HIDDEN · MYSTERY · DISABLED"));
        lines.add(WLine.kv("enable_if", "Flag expression — quest hidden+disabled when false"));
        lines.add(WLine.kv("hide_dep_line", "true / false — hides all dep lines on the canvas"));
        lines.add(WLine.kv("disabled_blocks_children", "true → DISABLED quest still gates children"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Completion"));
        lines.add(WLine.kv("task_min_count", "0 = all tasks required; N = complete any N tasks"));
        lines.add(WLine.kv("repeat_mode", "NONE · DAILY · COOLDOWN · INFINITE"));
        lines.add(WLine.kv("repeat_cooldown_hours", "Hours between repeats (COOLDOWN mode only)"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Prerequisites"));
        lines.add(WLine.kv("parent", "Single primary parent quest id path, or \"none\""));
        lines.add(WLine.kv("require_all_prereqs", "true = AND gate; false = OR gate (legacy)"));
        lines.add(WLine.kv("prerequisites", "List of {id, required, forbidden, link} tags"));
        lines.add(WLine.kv("optional_prereq_min_count", "Min optional prereqs needed (0 = all)"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Tutorial"));
        lines.add(WLine.kv("tutorial_steps", "List of {text, highlight} tags — see Overview page"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Multiplayer / Teams"));
        lines.add(WLine.kv("shared", "true → completing this quest cascades to all online teammates"));
        lines.add(WLine.t("Uses Minecraft's built-in scoreboard teams (/team add, /team join)."));
        lines.add(WLine.t("Task progress remains per-player; only the final COMPLETED state is shared."));
        return lines;
    }

    private List<WLine> pageTasks() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Task Types"));

        lines.add(WLine.sh("Built-in"));
        String[][] builtins = {
                { "kill_entity", "Kill mobs", "target: entity_id, count, consume" },
                { "item_check", "Have item(s) in inventory", "target: item_id, count, consume" },
                { "craft_item", "Craft an item", "target: item_id, count" },
                { "experience", "Reach an XP level", "count: level" },
                { "location_terminal", "Interact with a terminal", "target: terminal_id, consume" },
                { "advancement", "Earn an advancement", "target: advancement_id" },
                { "block_interact", "Place / right-click a block", "target: block_id, secondary: PLACE|RIGHT_CLICK" },
                { "fluid_check", "Have fluid in inventory", "target: fluid_id, count: mB, consume" },
                { "stat", "Reach a stat value", "target: stat_id (e.g. minecraft:jump), count" },
                { "dimension", "Enter a dimension", "secondary: dimension_id" },
                { "biome", "Visit a biome", "target: biome_id" },
                { "structure", "Enter a structure", "target: structure_id" },
                { "checkmark", "Manual checkbox", "(no fields)" },
                { "tag_item", "Have item matching tag", "target: tag (e.g. c:ores/iron), count" },
                { "info", "Display-only text panel", "target: body text" },
                { "external_trigger", "Fired by QuestAPI.fireExternalEvent()", "target: trigger_id, count: times" },
                { "energy_check", "Have stored energy",
                        "target: FE|EU|ANY, secondary: INVENTORY|HELD|BLOCK, count: FE" },
        };
        for (String[] row : builtins) {
            lines.add(WLine.kv(row[0], row[1]));
            lines.add(WLine.in(row[2]));
        }

        int kjsCount = PhoenixTaskRegistry.getEditorTypes().size() - builtins.length;
        if (kjsCount > 0) {
            lines.add(WLine.sp());
            lines.add(WLine.sh("KubeJS / mod-registered  (" + kjsCount + ")"));
            Set<String> builtinIds = new HashSet<>();
            for (String[] b : builtins) builtinIds.add(b[0]);
            for (PhoenixTaskRegistry.TaskEntry entry : PhoenixTaskRegistry.getEditorTypes()) {
                if (!builtinIds.contains(entry.typeId())) {
                    lines.add(WLine.kv(entry.typeId(),
                            entry.editorLabel() != null ? entry.editorLabel() : entry.typeId()));
                    if (entry.editorTooltip() != null)
                        lines.add(WLine.in(entry.editorTooltip().split("\n")[0]));
                }
            }
        }

        lines.add(WLine.sp());
        lines.add(WLine.div());
        lines.add(WLine.sh("SNBT task entry format"));
        lines.add(WLine.t("{type: \"kill_entity\", task_id: \"phoenixcore:task_…\", description: \"…\","));
        lines.add(WLine.t(" target: \"minecraft:zombie\", count: 5, consume: false, optional: false}"));
        return lines;
    }

    private List<WLine> pageRewards() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Reward Types"));
        lines.add(WLine.sp());
        lines.add(WLine.kv("item", "Give item(s) to the player"));
        lines.add(WLine.in("Fields: type, item (item_id), count"));
        lines.add(WLine.sp());
        lines.add(WLine.kv("xp", "Award experience levels"));
        lines.add(WLine.in("Fields: type, levels (integer)"));
        lines.add(WLine.sp());
        lines.add(WLine.kv("command", "Run a server command as console"));
        lines.add(WLine.in("Fields: type, command  (%player% replaced with player name)"));
        lines.add(WLine.sp());
        lines.add(WLine.kv("loot_table", "Roll a loot table, give all resulting items"));
        lines.add(WLine.in("Fields: type, loot_table (resource location)"));
        lines.add(WLine.sp());
        lines.add(WLine.kv("script_event", "Fire PhoenixQuestScriptRewardEvent on the Forge bus"));
        lines.add(WLine.in("Fields: type, event_id, data (optional CompoundTag NBT)"));
        lines.add(WLine.in("KubeJS: listen with ForgeEvents.onEvent('…ScriptRewardEvent', e => …)"));
        lines.add(WLine.sp());
        lines.add(WLine.div());
        lines.add(WLine.sh("SNBT reward entry format"));
        lines.add(WLine.t("{type: \"item\", item: \"minecraft:diamond\", count: 3}"));
        lines.add(WLine.t("{type: \"script_event\", event_id: \"my_event\", data: {key: 1}}"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Java event hook"));
        lines.add(WLine.t("@SubscribeEvent"));
        lines.add(WLine.t("public void onReward(PhoenixQuestScriptRewardEvent e) {"));
        lines.add(WLine.t("    e.getPlayer();  e.getEventId();  e.getData();"));
        lines.add(WLine.t("}"));
        return lines;
    }

    private List<WLine> pageSnbtFormat() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("SNBT File Format"));
        lines.add(WLine.t("Each quest is a single .snbt file in config/phoenix_chronicles/"));
        lines.add(WLine.t("The file name becomes the quest id path if no id field is present."));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Minimal quest"));
        lines.add(WLine.t("{id: \"my_quest\", title: \"My Quest\"}"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Full example"));
        lines.add(WLine.t("{"));
        lines.add(WLine.t("  id: \"magic/first_spell\","));
        lines.add(WLine.t("  title: \"First Spell\","));
        lines.add(WLine.t("  description: \"Cast your first spell.\","));
        lines.add(WLine.t("  subtitle: \"Chapter 1\","));
        lines.add(WLine.t("  category: \"MAGIC\","));
        lines.add(WLine.t("  shape: \"CIRCLE\","));
        lines.add(WLine.t("  icon_item: \"minecraft:book\","));
        lines.add(WLine.t("  positionX: 120,  positionY: 80,"));
        lines.add(WLine.t("  parent: \"magic/intro\","));
        lines.add(WLine.t("  visibility: \"NORMAL\","));
        lines.add(WLine.t("  repeat_mode: \"NONE\","));
        lines.add(WLine.t("  tasks: [{type: \"checkmark\", task_id: \"phoenixcore:task_1\","));
        lines.add(WLine.t("           description: \"Cast a spell\"}],"));
        lines.add(WLine.t("  rewards: [{type: \"xp\", levels: 5}],"));
        lines.add(WLine.t("  tutorial_steps: ["));
        lines.add(WLine.t("    {text: \"Welcome! Let's learn magic.\", highlight: \"none\"},"));
        lines.add(WLine.t("    {text: \"Check the canvas for your quests.\", highlight: \"canvas\"}"));
        lines.add(WLine.t("  ]"));
        lines.add(WLine.t("}"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Prerequisites list (extended format)"));
        lines.add(WLine.t("prerequisites: ["));
        lines.add(WLine.t("  {id: \"magic/intro\", required: true},"));
        lines.add(WLine.t("  {id: \"magic/side\",  required: false},"));
        lines.add(WLine.t("  {id: \"magic/bad\",   forbidden: true},"));
        lines.add(WLine.t("  {id: \"magic/link\",  required: true, link: true}"));
        lines.add(WLine.t("]"));
        lines.add(WLine.sp());
        lines.add(WLine.sh("File locations"));
        lines.add(WLine.kv("Quest SNBT", "config/phoenix_chronicles/*.snbt  (any depth)"));
        lines.add(WLine.kv("Quest markdown", "config/phoenix_chronicles/quests/{id}.md"));
        lines.add(WLine.kv("Groups", "config/phoenix_chronicles/quest_groups.json"));
        lines.add(WLine.kv("Settings", "config/phoenix_chronicles/settings.json"));
        lines.add(WLine.kv("Tutorial prog.", "config/phoenix_chronicles/tutorial_progress.dat"));
        return lines;
    }

    private List<WLine> pageLiveStats() {
        var lines = new ArrayList<WLine>();
        lines.add(WLine.h("Live Registry Stats"));
        lines.add(WLine.t("Data pulled from the in-memory registry at render time."));
        lines.add(WLine.sp());

        Map<String, List<QuestNode>> byCat = new LinkedHashMap<>();
        for (QuestNode n : QuestTreeRegistry.getAllQuests().values()) {
            byCat.computeIfAbsent(n.getCategory(), k -> new ArrayList<>()).add(n);
        }

        lines.add(WLine.kv("Total quests:", String.valueOf(QuestTreeRegistry.getAllQuests().size())));
        lines.add(WLine.kv("Categories:", String.valueOf(byCat.size())));
        lines.add(WLine.sp());
        lines.add(WLine.sh("Per-category"));
        byCat.entrySet().stream()
                .sorted(Map.Entry.<String, List<QuestNode>>comparingByValue(Comparator.comparingInt(l -> -l.size())))
                .forEach(e -> {
                    long repeatable = e.getValue().stream().filter(QuestNode::isRepeatable).count();
                    long hasTutorial = e.getValue().stream().filter(n -> !n.getTutorialSteps().isEmpty()).count();
                    String suffix = (repeatable > 0 ? "  " + repeatable + " repeatable" : "") +
                            (hasTutorial > 0 ? "  " + hasTutorial + " tutorial" : "");
                    lines.add(WLine.kv(e.getKey() + ":", e.getValue().size() + " quests" + suffix));
                });

        lines.add(WLine.sp());
        lines.add(WLine.sh("Task types"));
        lines.add(WLine.kv("Registered:", String.valueOf(PhoenixTaskRegistry.getEditorTypes().size())));

        lines.add(WLine.sp());
        lines.add(WLine.sh("Repeat modes in use"));
        Map<QuestNode.RepeatMode, Long> repeatCounts = new LinkedHashMap<>();
        for (QuestNode.RepeatMode m : QuestNode.RepeatMode.values()) repeatCounts.put(m, 0L);
        QuestTreeRegistry.getAllQuests().values()
                .forEach(n -> repeatCounts.merge(n.getRepeatMode(), 1L, Long::sum));
        repeatCounts.forEach((mode, count) -> {
            if (count > 0) lines.add(WLine.kv(mode.name() + ":", count + " quest" + (count == 1 ? "" : "s")));
        });

        lines.add(WLine.sp());
        lines.add(WLine.sh("Quests with tutorials"));
        long tutCount = QuestTreeRegistry.getAllQuests().values().stream()
                .filter(n -> !n.getTutorialSteps().isEmpty()).count();
        lines.add(WLine.kv("Total:", tutCount + " quest" + (tutCount == 1 ? "" : "s") + " have tutorial steps"));

        lines.add(WLine.sp());
        lines.add(WLine.div());
        List<String> errs = QuestFileLoader.LOAD_ERRORS;
        if (errs.isEmpty()) {
            lines.add(WLine.sh("§aLoad errors: none"));
        } else {
            lines.add(WLine.sh("§cLoad errors: " + errs.size()));
            lines.add(WLine.t("Run /chronicles validate in-game for full output."));
            for (String e : errs) lines.add(WLine.t("✗ " + e));
        }

        return lines;
    }

    private List<WLine> pageApiReference() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("API Reference"));
        L.add(WLine.t("Click ⎘ to copy any snippet to clipboard."));
        L.add(WLine.sp());

        L.add(WLine.sh("QuestAPI  (Java — net.phoenix.core.integration.phoenix_chronicles)"));
        L.add(WLine.kv("completeQuest", "Force-complete a quest for a player (server-side)"));
        L.add(WLine.code("QuestAPI.completeQuest(serverPlayer, new ResourceLocation(\"phoenixcore\", \"my_quest\"));"));
        L.add(WLine.kv("unlockQuest", "Bypass prerequisites and unlock a quest"));
        L.add(WLine.code("QuestAPI.unlockQuest(serverPlayer, new ResourceLocation(\"phoenixcore\", \"my_quest\"));"));
        L.add(WLine.kv("resetQuest", "Reset progress on a quest (repeatable use)"));
        L.add(WLine.code("QuestAPI.resetQuest(serverPlayer, new ResourceLocation(\"phoenixcore\", \"my_quest\"));"));
        L.add(WLine.kv("fireExternalEvent", "Trigger an external_trigger task by id"));
        L.add(WLine.code("QuestAPI.fireExternalEvent(serverPlayer, \"my_trigger_id\");"));
        L.add(WLine.kv("getState", "Read a player's current quest state"));
        L.add(WLine.code("QuestState state = QuestAPI.getState(serverPlayer, questId);"));
        L.add(WLine.kv("getProgress", "0.0–1.0 completion ratio for a quest"));
        L.add(WLine.code("float pct = QuestAPI.getProgress(serverPlayer, questId);"));
        L.add(WLine.sp());

        L.add(WLine.sh("Forge event hooks  (Java)"));
        L.add(WLine.kv("QuestCompletedEvent", "Fired on server bus when any quest completes"));
        L.add(WLine.code("@SubscribeEvent"));
        L.add(WLine.code("public void onComplete(QuestCompletedEvent e) {"));
        L.add(WLine.code("    ServerPlayer p = e.getPlayer();"));
        L.add(WLine.code("    ResourceLocation id = e.getQuest().getId();"));
        L.add(WLine.code("}"));
        L.add(WLine.sp());
        L.add(WLine.kv("QuestUnlockedEvent", "Fired on server bus when a quest becomes available"));
        L.add(WLine.code("@SubscribeEvent"));
        L.add(WLine.code("public void onUnlock(QuestUnlockedEvent e) { … }"));
        L.add(WLine.sp());
        L.add(WLine.kv("PhoenixQuestScriptRewardEvent", "Fired by a script_event reward — carry custom data"));
        L.add(WLine.code("@SubscribeEvent"));
        L.add(WLine.code("public void onReward(PhoenixQuestScriptRewardEvent e) {"));
        L.add(WLine.code("    String evtId = e.getEventId();   // matches event_id in SNBT"));
        L.add(WLine.code("    CompoundTag data = e.getData();  // optional NBT payload"));
        L.add(WLine.code("    ServerPlayer p = e.getPlayer();"));
        L.add(WLine.code("}"));
        L.add(WLine.sp());

        L.add(WLine.sh("Custom task type  (Java)"));
        L.add(WLine.t("Implement QuestTask, then register in your mod constructor or common setup:"));
        L.add(WLine.code("PhoenixTaskRegistry.register("));
        L.add(WLine.code("    \"my_task_type\",           // type id used in SNBT"));
        L.add(WLine.code("    MyTask::new,               // CompoundTag → QuestTask"));
        L.add(WLine.code("    \"My Task\",                // editor label"));
        L.add(WLine.code("    \"Does something custom\"   // editor tooltip"));
        L.add(WLine.code(");"));
        L.add(WLine.sp());

        L.add(WLine.sh("Custom enable_if flag  (Java)"));
        L.add(WLine.t("Flags are evaluated each render tick — keep the supplier cheap:"));
        L.add(WLine.code("PhoenixQuestFlags.register(\"my_flag\", () -> MyMod.isSomethingEnabled());"));
        L.add(WLine.t("Usage in SNBT:  enable_if: \"my_flag\"  or  enable_if: \"my_flag,other_flag\""));
        L.add(WLine.t("Prefix a flag with ! to negate:  enable_if: \"!my_flag\""));
        L.add(WLine.sp());
        L.add(WLine.div());

        L.add(WLine.sh("KubeJS — startup_scripts/chronicles.js"));
        L.add(WLine.sp());
        L.add(WLine.kv("Register a task type", ""));
        L.add(WLine.code("PhoenixTaskRegistry.registerKJS("));
        L.add(WLine.code("  'my_task_type',"));
        L.add(WLine.code("  tag => { return { check: player => true }; },"));
        L.add(WLine.code("  'My Task Label',"));
        L.add(WLine.code("  'Tooltip text'"));
        L.add(WLine.code(");"));
        L.add(WLine.sp());
        L.add(WLine.kv("Listen for quest complete", "client_scripts or server_scripts"));
        L.add(WLine.code("ForgeEvents.onEvent('net.phoenix.core.integration"));
        L.add(WLine.code("  .phoenix_chronicles.event.QuestCompletedEvent', e => {"));
        L.add(WLine.code("    let id = e.quest.id.path;  // e.g. 'magic/first_spell'"));
        L.add(WLine.code("    let player = e.player;"));
        L.add(WLine.code("});"));
        L.add(WLine.sp());
        L.add(WLine.kv("Listen for script_event reward", ""));
        L.add(WLine.code("ForgeEvents.onEvent('net.phoenix.core.integration"));
        L.add(WLine.code("  .phoenix_chronicles.QuestScriptRewardEvent', e => {"));
        L.add(WLine.code("    if (e.eventId === 'my_event') {"));
        L.add(WLine.code("        e.player.tell('Reward fired!');"));
        L.add(WLine.code("    }"));
        L.add(WLine.code("});"));
        L.add(WLine.sp());
        L.add(WLine.kv("Register a flag", ""));
        L.add(WLine.code("PhoenixQuestFlags.registerKJS('my_flag', () => someCondition);"));
        L.add(WLine.sp());
        L.add(WLine.kv("Fire external trigger", "server_scripts"));
        L.add(WLine.code("QuestAPI.fireExternalEvent(player, 'my_trigger_id');"));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("In-game commands"));
        L.add(WLine.kv("/chronicles status <id>", "Any player — check your own quest state"));
        L.add(WLine.kv("/chronicles emergency <id>", "Any player — get emergency items for active quest"));
        L.add(WLine.kv("/chronicles complete <id>", "Op — force-complete a quest"));
        L.add(WLine.kv("/chronicles unlock <id>", "Op — bypass prerequisites"));
        L.add(WLine.kv("/chronicles reset <id>", "Op — reset quest to LOCKED"));
        L.add(WLine.kv("/chronicles active <id>", "Op — force-start a quest"));
        L.add(WLine.kv("/chronicles validate", "Op — report load errors and config issues"));

        return L;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int visibleH = (height - FOOTER_H - MARGIN) - (HEADER_H + MARGIN);
        int maxScroll = Math.max(0, cachedContentH - visibleH);
        scrollY = Math.max(0, Math.min(maxScroll, (int) (scrollY - delta * 14)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            for (int i = 0; i < copyBtnBounds.size(); i++) {
                int[] b = copyBtnBounds.get(i);
                if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                    if (minecraft != null)
                        minecraft.keyboardHandler.setClipboard(copyBtnTexts.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
