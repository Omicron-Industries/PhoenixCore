package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    /** true = ALL prereqs must be complete (AND); false = ANY one suffices (OR). */
    private boolean requireAllPrerequisites = true;

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
}
