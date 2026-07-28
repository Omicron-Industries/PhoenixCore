package net.phoenix.core.conflux.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ConfluxWikiScreen extends Screen {

    private static final int HEADER_H = 24;
    private static final int SIDEBAR_W = 90;
    private static final int MARGIN = 10;
    private static final int LINE_H = 11;

    private static final int C_BG = 0xFF07070F;
    private static final int C_PANEL = 0xFF0F0F1C;
    private static final int C_HEADER = 0xFF0B0B18;
    private static final int C_BORDER = 0xFF252540;
    private static final int C_ACCENT = 0xFF5577FF;
    private static final int C_TEXT = 0xFFCCCCEE;
    private static final int C_DIM = 0xFF555577;

    private static final String[] PAGE_NAMES = {
            "Overview", "Disciplines", "Research Nodes", "Unlock Types", "JSON Format", "Terminal UI",
            "Java API", "Chronicles Bridge"
    };

    private final @Nullable Screen parent;
    private int activePage = 0;
    private int scrollY = 0;
    private int cachedContentH = 0;

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

    public ConfluxWikiScreen(@Nullable Screen parent) {
        super(Component.literal("Conflux Wiki"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scrollY = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, C_BG);

        g.fill(0, 0, width, HEADER_H, C_HEADER);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_BORDER);
        g.drawString(font, Component.literal("Conflux Wiki").withStyle(ChatFormatting.BOLD), MARGIN,
                (HEADER_H - 8) / 2, C_TEXT, false);
        String hint = "Esc to close";
        g.drawString(font, hint, width - font.width(hint) - MARGIN, (HEADER_H - 8) / 2, C_DIM, false);

        g.fill(0, HEADER_H, SIDEBAR_W, height, C_PANEL);
        g.fill(SIDEBAR_W - 1, HEADER_H, SIDEBAR_W, height, C_BORDER);
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            int rowY = HEADER_H + 8 + i * 16;
            boolean active = i == activePage;
            boolean hover = mx >= 0 && mx < SIDEBAR_W && my >= rowY - 1 && my < rowY + 15;
            if (active) g.fill(0, rowY - 1, SIDEBAR_W - 1, rowY + 15, 0xFF1A1A30);
            if (active) g.fill(0, rowY - 1, 2, rowY + 15, C_ACCENT);
            g.drawString(font, PAGE_NAMES[i], 8, rowY + 2, active ? C_TEXT : hover ? 0xFF9999BB : C_DIM, false);
        }

        List<WLine> lines = buildPage(activePage);
        int contentX = SIDEBAR_W + MARGIN;
        int contentTop = HEADER_H + MARGIN;
        int contentW = width - contentX - MARGIN;
        int contentBottom = height - MARGIN;

        g.enableScissor(SIDEBAR_W, HEADER_H, width, height);
        int y = contentTop - scrollY;
        for (WLine ln : lines) {
            y = renderLine(g, ln, contentX, y, contentW);
        }
        cachedContentH = y + scrollY - contentTop;
        g.disableScissor();

        int visibleH = contentBottom - contentTop;
        int maxScroll = Math.max(0, cachedContentH - visibleH);
        if (maxScroll > 0) {
            int trackX = width - 6;
            g.fill(trackX, contentTop, trackX + 3, contentBottom, 0x33FFFFFF);
            int thumbH = Math.max(16, visibleH * visibleH / cachedContentH);
            int thumbY = contentTop + (int) ((visibleH - thumbH) * (scrollY / (float) maxScroll));
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0x88FFFFFF);
        }

        super.render(g, mx, my, pt);
    }

    private int renderLine(GuiGraphics g, WLine ln, int x, int y, int w) {
        switch (ln.type()) {
            case HEADING -> {
                g.drawString(font, Component.literal(ln.a()).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), x,
                        y, C_TEXT, false);
                return y + LINE_H + 6;
            }
            case SUBHEADING -> {
                g.drawString(font, Component.literal(ln.a()).withStyle(ChatFormatting.YELLOW), x, y, C_TEXT, false);
                return y + LINE_H + 3;
            }
            case KV -> {
                g.drawString(font, Component.literal(ln.a()).withStyle(ChatFormatting.GOLD), x, y, C_TEXT, false);
                return y + LINE_H;
            }
            case TEXT -> {
                g.drawString(font, ln.a(), x, y, C_DIM, false);
                return y + LINE_H;
            }
            case INDENT -> {
                g.drawString(font, ln.a(), x + 8, y, 0xFF8888AA, false);
                return y + LINE_H;
            }
            case CODE -> {
                g.fill(x, y - 1, x + w, y + LINE_H - 1, 0xFF12121F);
                g.drawString(font, Component.literal(ln.a()).withStyle(ChatFormatting.GREEN), x + 3, y, C_TEXT,
                        false);
                return y + LINE_H;
            }
            case DIVIDER -> {
                g.fill(x, y + 2, x + w, y + 3, C_BORDER);
                return y + 7;
            }
            case SPACER -> {
                return y + 5;
            }
            default -> {
                return y;
            }
        }
    }

    private List<WLine> buildPage(int page) {
        return switch (page) {
            case 0 -> pageOverview();
            case 1 -> pageDisciplines();
            case 2 -> pageResearchNodes();
            case 3 -> pageUnlockTypes();
            case 4 -> pageJsonFormat();
            case 5 -> pageTerminalUi();
            case 6 -> pageJavaApi();
            case 7 -> pageChroniclesBridge();
            default -> List.of();
        };
    }

    private List<WLine> pageOverview() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Conflux: Research & Discipline System"));
        L.add(WLine.t("A datapack-driven research tree, browsed and unlocked at a Research"));
        L.add(WLine.t("Terminal. Progress is tracked per-team (or per-player if no team mod is"));
        L.add(WLine.t("installed), server-side, in WorldResearchData."));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("Core concepts"));
        L.add(WLine.kv("Research tree", "A JSON file: a set of nodes, optionally tied to a discipline"));
        L.add(WLine.kv("Research node", "One unlockable point in a tree: costs, prerequisites, unlocks"));
        L.add(WLine.kv("Unlock", "What a node grants when unlocked: a flag, a multiblock gate, ..."));
        L.add(WLine.kv("Flag", "An arbitrary string id other systems can check with hasFlag(...)"));
        L.add(WLine.kv("Discipline", "A named tree a team commits to (Phoenix, Sculk, ...)"));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("Where to go from here"));
        L.add(WLine.t("Disciplines/Research Nodes/Unlock Types explain the concepts; JSON Format"));
        L.add(WLine.t("is the actual file schema for pack authors; Java API is for other mods."));
        return L;
    }

    private List<WLine> pageDisciplines() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Disciplines"));
        L.add(WLine.t("A discipline is just a string id (the \"discipline\" field on a research"));
        L.add(WLine.t("tree JSON) - there's no separate discipline registry. A team \"commits\" to"));
        L.add(WLine.t("a discipline the first time they unlock a node flagged commitment: true in"));
        L.add(WLine.t("that tree; WorldResearchData then remembers which discipline id that team"));
        L.add(WLine.t("belongs to and whether they're committed."));
        L.add(WLine.sp());
        L.add(WLine.kv("Switching discipline", "Only possible before committing, or by paying the"));
        L.add(WLine.in("tree's switch_cost (a ConfluxDataType -> amount map) once committed."));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("Built-in discipline picker cards"));
        L.add(WLine.t("DisciplinePickerScreen shows a fixed set of cosmetic cards (Phoenix,"));
        L.add(WLine.t("Sculk, Void, Sealed) - these are UI presentation only and are separate"));
        L.add(WLine.t("from whatever discipline ids your actual tree JSON files declare."));
        return L;
    }

    private List<WLine> pageResearchNodes() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Research Nodes"));
        L.add(WLine.t("One unlockable point on the tree canvas. Fields (all in the node's JSON"));
        L.add(WLine.t("object under a tree's \"nodes\" map, keyed by node id):"));
        L.add(WLine.sp());
        L.add(WLine.kv("title / lore / hint", "Display text shown in the terminal detail panel"));
        L.add(WLine.kv("icon", "Item id rendered on the node"));
        L.add(WLine.kv("position", "[x, y] canvas coordinates"));
        L.add(WLine.kv("cost", "Map of ConfluxDataType -> amount spent to unlock"));
        L.add(WLine.kv("prerequisites", "Node ids that must ALL be unlocked first"));
        L.add(WLine.kv("prerequisites_any", "Node ids where unlocking ANY ONE is enough"));
        L.add(WLine.kv("exclusion_group", "Unlocking this node locks out every other node sharing it"));
        L.add(WLine.kv("commitment", "true = unlocking this commits the team to the tree's discipline"));
        L.add(WLine.kv("hidden", "true = invisible until its prerequisites are met"));
        L.add(WLine.kv("unlocks", "List of {type, value} - see the Unlock Types page"));
        return L;
    }

    private List<WLine> pageUnlockTypes() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Unlock Types"));
        L.add(WLine.t("Each entry in a node's \"unlocks\" list is {type: \"...\", value: \"...\"}."));
        L.add(WLine.t("Recognized types:"));
        L.add(WLine.sp());
        L.add(WLine.kv("flag", "value = an arbitrary string id, added to the team's flag set"));
        L.add(WLine.in("Check with WorldResearchData.hasFlag(team, value), or gate a GTCEU"));
        L.add(WLine.in("recipe with AxiomResearchCondition."));
        L.add(WLine.sp());
        L.add(WLine.kv("multiblock", "value = a machine registry id, added to that team's allowed set"));
        L.add(WLine.in("Checked automatically by MultiblockResearchGateMixin before letting a"));
        L.add(WLine.in("multiblock of that id form for a gated machine."));
        L.add(WLine.sp());
        L.add(WLine.kv("recipe_tag", "value = a tag string, checked via hasRecipeTag(team, tag, ...)"));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.t("Unknown unlock types are ignored (no crash, no effect) rather than"));
        L.add(WLine.t("rejected, so a typo doesn't break the whole tree's load."));
        return L;
    }

    private List<WLine> pageJsonFormat() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Research Tree JSON Format"));
        L.add(WLine.t("Datapack path: data/<namespace>/conflux/research/<file>.json - loaded as"));
        L.add(WLine.t("a SimpleJsonResourceReloadListener, so it's a normal datapack reload away."));
        L.add(WLine.sp());
        L.add(WLine.code("{"));
        L.add(WLine.code("  \"title\": \"My Discipline\","));
        L.add(WLine.code("  \"description\": \"...\","));
        L.add(WLine.code("  \"discipline\": \"mypack_discipline\","));
        L.add(WLine.code("  \"switch_cost\": { \"RESEARCH_POINTS\": 500 },"));
        L.add(WLine.code("  \"nodes\": {"));
        L.add(WLine.code("    \"first_node\": {"));
        L.add(WLine.code("      \"title\": \"...\", \"icon\": \"minecraft:diamond\","));
        L.add(WLine.code("      \"position\": [0, 0], \"cost\": { \"RESEARCH_POINTS\": 100 },"));
        L.add(WLine.code("      \"commitment\": true,"));
        L.add(WLine.code("      \"unlocks\": [{ \"type\": \"flag\", \"value\": \"mypack:started\" }]"));
        L.add(WLine.code("    },"));
        L.add(WLine.code("    \"second_node\": {"));
        L.add(WLine.code("      \"prerequisites\": [\"first_node\"], \"position\": [1, 0], ..."));
        L.add(WLine.code("    }"));
        L.add(WLine.code("  }"));
        L.add(WLine.code("}"));
        L.add(WLine.sp());
        L.add(WLine.t("Omit \"discipline\" for a non-discipline (utility/info-only) tree - those"));
        L.add(WLine.t("never trigger a commitment even if a node sets commitment: true."));
        return L;
    }

    private List<WLine> pageTerminalUi() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Research Terminal UI"));
        L.add(WLine.t("Right-click a Research Terminal block to open it. Tabs across the top"));
        L.add(WLine.t("switch between loaded trees; the canvas pans (drag) and zooms (scroll);"));
        L.add(WLine.t("click a node to select it and see its detail panel on the right."));
        L.add(WLine.sp());
        L.add(WLine.kv("Research button", "Spends the selected node's cost and unlocks it"));
        L.add(WLine.kv("Locked-out nodes", "Shown dimmed/crossed - member of an exclusion group"));
        L.add(WLine.in("whose sibling you already unlocked"));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("Commands"));
        L.add(WLine.kv("/conflux view", "Open a detached terminal view (no real block backing it)"));
        L.add(WLine.kv("/conflux editor", "Open the tree editor (build/export tree JSON in-game)"));
        L.add(WLine.kv("/conflux discipline", "Open the discipline picker screen"));
        L.add(WLine.kv("/conflux wiki", "Open this wiki"));
        return L;
    }

    private List<WLine> pageJavaApi() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Java API (net.phoenix.core.conflux.research)"));
        L.add(WLine.t("The live, server-authoritative store is WorldResearchData - a team-scoped"));
        L.add(WLine.t("SavedData singleton. Team id resolution goes through ResearchTeamHelper"));
        L.add(WLine.t("(FTB Teams-aware if installed, else falls back to the player's own UUID)."));
        L.add(WLine.sp());
        L.add(WLine.code("UUID team = ResearchTeamHelper.getTeamId(serverPlayer);"));
        L.add(WLine.code("WorldResearchData data = WorldResearchData.get(serverLevel);"));
        L.add(WLine.sp());
        L.add(WLine.kv("isUnlocked(team, nodeId)", "Has this specific node been unlocked"));
        L.add(WLine.kv("hasFlag(team, flag)", "Has this flag been granted"));
        L.add(WLine.kv("getDiscipline(team)", "Current committed/in-progress discipline id, or null"));
        L.add(WLine.kv("canFormMultiblock(team, id)", "Used by the multiblock-gate mixin"));
        L.add(WLine.sp());
        L.add(WLine.kv("grantUnlock(team, nodeId)", "Free unlock, no terminal/cost involved"));
        L.add(WLine.kv("grantFlag(team, flag)", "Free flag grant, no terminal/cost involved"));
        L.add(WLine.in("Both added specifically so external callers (e.g. a quest reward) can"));
        L.add(WLine.in("grant progress without needing a physical ResearchTerminalBlockEntity."));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.t("tryUnlock(team, node, terminal, registry) is the normal cost-spending path"));
        L.add(WLine.t("used by the terminal UI itself - requires a real terminal block entity."));
        return L;
    }

    private List<WLine> pageChroniclesBridge() {
        var L = new ArrayList<WLine>();
        L.add(WLine.h("Phoenix Chronicles Bridge"));
        L.add(WLine.t("Phoenix Chronicles (the quest mod) has optional Conflux support: a quest"));
        L.add(WLine.t("task type that requires a Conflux unlock/flag, and a reward type that"));
        L.add(WLine.t("grants one on claim. PhoenixCore is a compile-only, non-forced dependency"));
        L.add(WLine.t("there - Chronicles works fine with or without this mod installed."));
        L.add(WLine.sp());
        L.add(WLine.kv("Bridge class", "net.phoenixvine.chronicles.integration.conflux.ConfluxCompat"));
        L.add(WLine.kv("Quest task", "\"conflux_research\" - flag_mode + node_id/flag fields"));
        L.add(WLine.kv("Quest reward", "\"conflux_unlock\" - flag_mode + node_id/flag fields"));
        L.add(WLine.sp());
        L.add(WLine.t("See Phoenix Chronicles' own in-game wiki (\"Conflux\" page) for the full"));
        L.add(WLine.t("quest-authoring side of this - this page is just the PhoenixCore half."));
        return L;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int visibleH = height - MARGIN - (HEADER_H + MARGIN);
        int maxScroll = Math.max(0, cachedContentH - visibleH);
        scrollY = Math.max(0, Math.min(maxScroll, (int) (scrollY - delta * 14)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= 0 && mx < SIDEBAR_W && my >= HEADER_H) {
            for (int i = 0; i < PAGE_NAMES.length; i++) {
                int rowY = HEADER_H + 8 + i * 16;
                if (my >= rowY - 1 && my < rowY + 15) {
                    activePage = i;
                    scrollY = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            onClose();
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
