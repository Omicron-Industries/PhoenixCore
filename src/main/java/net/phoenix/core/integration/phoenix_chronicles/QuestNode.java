package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import lombok.Getter;

import java.util.*;

/**
 * Runtime representation of a single quest blueprint.
 * Stateless — all per-player progress lives in PlayerQuestData.
 */
public class QuestNode {

    // ── Identity ──────────────────────────────────────────────────────────────
    private final ResourceLocation id;
    private final Component title;
    private final Component description;

    // ── Appearance ────────────────────────────────────────────────────────────
    private String category = "MAIN";
    private String shapeType = "SQUARE";
    private Item iconItem = null;
    private int customX = 0;
    private int customY = 0;

    // ── Extended metadata ─────────────────────────────────────────────────────
    private String subtitle = "";

    public enum Visibility {
        NORMAL,   // always shown; locked appearance if prereqs not met
        HIDDEN,   // invisible until prerequisites are complete
        MYSTERY,  // shown as ??? until prerequisites are complete
        DISABLED  // shown but grayed-out; excluded from progress counts;
                  // if also treated as hidden, its dep lines have no effect
    }

    private Visibility visibility = Visibility.NORMAL;

    /**
     * Comma-separated flag expression evaluated via {@link PhoenixQuestFlags}.
     * When the expression evaluates to {@code false}, this quest is treated as
     * HIDDEN + DISABLED regardless of its {@link #visibility} field.
     * Null / blank means always enabled.
     */
    @Getter
    private String enableIf = null;

    /** Returns true if this quest's flag condition is currently satisfied. */
    public boolean isFlagEnabled() {
        return PhoenixQuestFlags.evaluate(enableIf) && CategoryFlagRegistry.isCategoryEnabled(category);
    }

    public void setEnableIf(String expr) {
        this.enableIf = (expr == null || expr.isBlank()) ? null : expr.trim();
    }

    /**
     * True when the quest is completely inert: flag condition failed, meaning it
     * should be treated as if it doesn't exist (hidden, non-completable, no gating).
     * Distinct from {@link Visibility#DISABLED}, which still shows the quest.
     */
    public boolean isFlagDisabled() {
        return !isFlagEnabled();
    }

    /**
     * When true, all dependency lines touching this quest are hidden on the canvas.
     * Persisted as {@code hide_dep_line: 1b} in the quest SNBT.
     */
    private boolean hideDepLine = false;

    /**
     * Only meaningful when {@link #visibility} is {@link Visibility#DISABLED}.
     * When false (default): DISABLED quests are excluded from the prerequisite gate —
     * children unlock as if this quest were not a prerequisite at all.
     * When true: children remain locked until this quest is somehow completed
     * (useful for branching-narrative placeholders that still block progress).
     */
    private boolean disabledBlocksChildren = false;

    public boolean isHideDepLine() { return hideDepLine; }
    public void setHideDepLine(boolean hide) { this.hideDepLine = hide; }

    public boolean isDisabledBlocksChildren() { return disabledBlocksChildren; }
    public void setDisabledBlocksChildren(boolean v) { this.disabledBlocksChildren = v; }

    /**
     * When true, completing this quest on any player cascades the completion to all
     * online members of the same scoreboard team. Task progress remains per-player;
     * only the final COMPLETED state is shared. Uses Minecraft's built-in /team system.
     */
    private boolean shared = false;
    public boolean isShared() { return shared; }
    public void setShared(boolean s) { this.shared = s; }

    /** 0 = all non-optional tasks required; >0 = need exactly this many tasks. */
    private int taskMinCount = 0;

    // ── Repeat behaviour ──────────────────────────────────────────────────────
    /** How this quest can be repeated after first completion. */
    public enum RepeatMode {
        NONE,
        DAILY,
        COOLDOWN,
        INFINITE
    }

    private RepeatMode repeatMode = RepeatMode.NONE;
    private int repeatCooldownHours = 24; // used when mode == COOLDOWN

    // ── Prerequisite gate ─────────────────────────────────────────────────────
    /**
     * Legacy simple gate (still used when no per-prereq flags are set):
     * true = ALL prereqs must be complete (AND); false = ANY one suffices (OR).
     *
     * When per-prereq flags are set this field is ignored — use
     * {@link #isPrereqRequired(ResourceLocation)} + {@link #optionalPrereqMinCount}.
     */
    private boolean requireAllPrerequisites = true;

    /**
     * Per-prerequisite required flag.
     * Absent entry → defaults to true (required).
     * false → prereq is optional and contributes to the optional pool.
     */
    private final Map<ResourceLocation, Boolean> prereqRequired = new HashMap<>();

    /**
     * Forbidden prerequisites — these must NOT be completed for this quest to unlock.
     * Useful for mutually exclusive quest paths: "unlock if player did A but NOT B".
     *
     * SNBT: {id: "other_quest", forbidden: true}
     * Expression shorthand (display): shown as "!other_quest" in the dep list.
     */
    private final Set<ResourceLocation> prereqForbidden = new HashSet<>();

    /**
     * Link prerequisites — created via Alt+drag in the editor.
     * Functionally identical to normal prereqs but rendered and saved distinctly
     * so pack devs can visually distinguish "storyline" edges from "also requires" edges.
     *
     * SNBT: {id: "other_quest", link: true}
     */
    private final Set<ResourceLocation> prereqLink = new HashSet<>();

    /**
     * How many optional (non-required) prerequisites must be completed.
     * 0 = all optional prereqs must be done (same as required).
     * -1 = none of the optional prereqs need to be done.
     * N > 0 = exactly N (or more) from the optional pool.
     * Ignored when there are no optional prereqs.
     */
    private int optionalPrereqMinCount = 0;

    // ── Emergency items ───────────────────────────────────────────────────────
    /**
     * Items given to the player via /chronicle emergency <questId> when they
     * lose quest-required items mid-progress. Only granted while the quest is ACTIVE.
     */
    private final List<ItemStack> emergencyItems = new ArrayList<>();

    // ── Tutorial steps ────────────────────────────────────────────────────────
    private final List<TutorialStep> tutorialSteps = new ArrayList<>();

    public List<TutorialStep> getTutorialSteps() { return Collections.unmodifiableList(tutorialSteps); }
    public void addTutorialStep(TutorialStep step) { if (step != null) tutorialSteps.add(step); }
    public void clearTutorialSteps() { tutorialSteps.clear(); }

    // ── Relations ─────────────────────────────────────────────────────────────
    private final List<QuestNode> children = new ArrayList<>();
    private final List<QuestNode> prerequisites = new ArrayList<>();
    private final List<QuestTask> tasks = new ArrayList<>();
    private final List<QuestReward> rewards = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public QuestNode(ResourceLocation id, Component title, Component description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    // ── Identity accessors ────────────────────────────────────────────────────
    public ResourceLocation getId() {
        return id;
    }

    public Component getTitle() {
        return title;
    }

    public Component getDescription() {
        return description;
    }

    // ── Appearance accessors ──────────────────────────────────────────────────
    public String getCategory() {
        return category;
    }

    public void setCategory(String c) {
        this.category = c;
    }

    public String getShapeType() {
        return shapeType;
    }

    public void setShapeType(String t) {
        this.shapeType = t;
    }

    public Item getIconItem() {
        return iconItem;
    }

    public void setIconItem(Item item) {
        this.iconItem = item;
    }

    public void setIconItemById(String id) {
        if (id == null || id.isBlank()) {
            this.iconItem = null;
            return;
        }
        try {
            Item found = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            this.iconItem = (found != null && found != Items.AIR) ? found : null;
        } catch (Exception ignored) {
            this.iconItem = null;
        }
    }

    public String getIconItemId() {
        if (iconItem == null) return "";
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(iconItem);
        return key != null ? key.toString() : "";
    }

    public int getCustomX() {
        return customX;
    }

    public void setCustomX(int x) {
        this.customX = x;
    }

    public int getCustomY() {
        return customY;
    }

    public void setCustomY(int y) {
        this.customY = y;
    }

    public void setCustomPosition(int x, int y) {
        this.customX = x;
        this.customY = y;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String s) {
        this.subtitle = s != null ? s : "";
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility v) {
        this.visibility = v != null ? v : Visibility.NORMAL;
    }

    public int getTaskMinCount() {
        return taskMinCount;
    }

    public void setTaskMinCount(int n) {
        this.taskMinCount = Math.max(0, n);
    }

    // ── Repeat accessors ──────────────────────────────────────────────────────
    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode m) {
        this.repeatMode = m;
    }

    public int getRepeatCooldownHours() {
        return repeatCooldownHours;
    }

    public void setRepeatCooldownHours(int h) {
        this.repeatCooldownHours = Math.max(1, h);
    }

    public boolean isRepeatable() {
        return repeatMode != RepeatMode.NONE;
    }

    // ── Prerequisite gate accessors ───────────────────────────────────────────
    public boolean getRequireAllPrerequisites() {
        return requireAllPrerequisites;
    }

    public void setRequireAllPrerequisites(boolean v) {
        this.requireAllPrerequisites = v;
    }

    /** Returns true if this prereq is required (default), false if it's optional. */
    public boolean isPrereqRequired(ResourceLocation prereqId) {
        return prereqRequired.getOrDefault(prereqId, true);
    }

    public void setPrereqRequired(ResourceLocation prereqId, boolean required) {
        prereqRequired.put(prereqId, required);
    }

    public Map<ResourceLocation, Boolean> getPrereqRequired() {
        return Collections.unmodifiableMap(prereqRequired);
    }

    public boolean hasPerPrereqFlags() {
        return !prereqRequired.isEmpty() || !prereqForbidden.isEmpty();
    }

    // ── Forbidden prereqs ─────────────────────────────────────────────────────

    /** True if this prereq must NOT be completed for this quest to unlock. */
    public boolean isPrereqForbidden(ResourceLocation prereqId) {
        return prereqForbidden.contains(prereqId);
    }

    public void setPrereqForbidden(ResourceLocation prereqId, boolean forbidden) {
        if (forbidden) {
            prereqForbidden.add(prereqId);
            prereqRequired.remove(prereqId); // forbidden is mutually exclusive with required/optional
        } else {
            prereqForbidden.remove(prereqId);
        }
    }

    public Set<ResourceLocation> getPrereqForbidden() {
        return Collections.unmodifiableSet(prereqForbidden);
    }

    // ── Link prereqs ──────────────────────────────────────────────────────────

    /** True if this connection was created via Alt+drag (a "link" rather than a parent-child edge). */
    public boolean isPrereqLink(ResourceLocation prereqId) {
        return prereqLink.contains(prereqId);
    }

    public void setPrereqLink(ResourceLocation prereqId, boolean link) {
        if (link) prereqLink.add(prereqId);
        else prereqLink.remove(prereqId);
    }

    public Set<ResourceLocation> getPrereqLink() {
        return Collections.unmodifiableSet(prereqLink);
    }

    public int getOptionalPrereqMinCount() {
        return optionalPrereqMinCount;
    }

    public void setOptionalPrereqMinCount(int n) {
        this.optionalPrereqMinCount = n;
    }

    // ── Relations ─────────────────────────────────────────────────────────────
    public void addChild(QuestNode child) {
        if (child != null && !children.contains(child)) children.add(child);
    }

    public void removeChild(QuestNode child) {
        children.remove(child);
    }

    public List<QuestNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addPrerequisite(QuestNode p) {
        if (p != null && !prerequisites.contains(p)) prerequisites.add(p);
    }

    public void removePrerequisite(QuestNode p) {
        if (p != null) {
            prerequisites.remove(p);
            prereqRequired.remove(p.getId());
            prereqForbidden.remove(p.getId());
            prereqLink.remove(p.getId());
        }
    }

    public List<QuestNode> getPrerequisites() {
        return Collections.unmodifiableList(prerequisites);
    }

    public void addTask(QuestTask task) {
        if (task != null) tasks.add(task);
    }

    public void clearTasks() {
        tasks.clear();
    }

    public List<QuestTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addReward(QuestReward r) {
        if (r != null) rewards.add(r);
    }

    public void clearRewards() {
        rewards.clear();
    }

    public List<QuestReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    // ── Emergency items ───────────────────────────────────────────────────────

    public List<ItemStack> getEmergencyItems() {
        return Collections.unmodifiableList(emergencyItems);
    }

    public void addEmergencyItem(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) emergencyItems.add(stack.copy());
    }

    public void clearEmergencyItems() {
        emergencyItems.clear();
    }

    public ListTag serializeEmergencyItems() {
        ListTag list = new ListTag();
        for (ItemStack stack : emergencyItems) list.add(stack.save(new CompoundTag()));
        return list;
    }

    public void deserializeEmergencyItems(ListTag list) {
        emergencyItems.clear();
        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                ItemStack stack = ItemStack.of(ct);
                if (!stack.isEmpty()) emergencyItems.add(stack);
            }
        }
    }
}
