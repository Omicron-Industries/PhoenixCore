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

public class QuestNode {

    private final ResourceLocation id;
    private final Component title;
    private final Component description;

    private String category = "MAIN";
    private String shapeType = "SQUARE";
    private Item iconItem = null;
    private int customX = 0;
    private int customY = 0;

    private String subtitle = "";

    public enum Visibility {
        NORMAL,   
        HIDDEN,   
        MYSTERY,  
        DISABLED  
                  
    }

    private Visibility visibility = Visibility.NORMAL;

    @Getter
    private String enableIf = null;

    public boolean isFlagEnabled() {
        return PhoenixQuestFlags.evaluate(enableIf) && CategoryFlagRegistry.isCategoryEnabled(category);
    }

    public void setEnableIf(String expr) {
        this.enableIf = (expr == null || expr.isBlank()) ? null : expr.trim();
    }

    public boolean isFlagDisabled() {
        return !isFlagEnabled();
    }

    private boolean hideDepLine = false;

    private boolean disabledBlocksChildren = false;

    public boolean isHideDepLine() {
        return hideDepLine;
    }

    public void setHideDepLine(boolean hide) {
        this.hideDepLine = hide;
    }

    public boolean isDisabledBlocksChildren() {
        return disabledBlocksChildren;
    }

    public void setDisabledBlocksChildren(boolean v) {
        this.disabledBlocksChildren = v;
    }

    private boolean shared = false;

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean s) {
        this.shared = s;
    }

    private int taskMinCount = 0;

    public enum RepeatMode {
        NONE,
        DAILY,
        COOLDOWN,
        INFINITE
    }

    private RepeatMode repeatMode = RepeatMode.NONE;
    private int repeatCooldownHours = 24; 

    private boolean requireAllPrerequisites = true;

    private final Map<ResourceLocation, Boolean> prereqRequired = new HashMap<>();

    private final Set<ResourceLocation> prereqForbidden = new HashSet<>();

    private final Set<ResourceLocation> prereqLink = new HashSet<>();

    private int optionalPrereqMinCount = 0;

    private final List<ItemStack> emergencyItems = new ArrayList<>();

    private final List<TutorialStep> tutorialSteps = new ArrayList<>();

    public List<TutorialStep> getTutorialSteps() {
        return Collections.unmodifiableList(tutorialSteps);
    }

    public void addTutorialStep(TutorialStep step) {
        if (step != null) tutorialSteps.add(step);
    }

    public void clearTutorialSteps() {
        tutorialSteps.clear();
    }

    private final List<QuestNode> children = new ArrayList<>();
    private final List<QuestNode> prerequisites = new ArrayList<>();
    private final List<QuestTask> tasks = new ArrayList<>();
    private final List<QuestReward> rewards = new ArrayList<>();

    public QuestNode(ResourceLocation id, Component title, Component description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getTitle() {
        return title;
    }

    public Component getDescription() {
        return description;
    }

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

    public boolean getRequireAllPrerequisites() {
        return requireAllPrerequisites;
    }

    public void setRequireAllPrerequisites(boolean v) {
        this.requireAllPrerequisites = v;
    }

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

    public boolean isPrereqForbidden(ResourceLocation prereqId) {
        return prereqForbidden.contains(prereqId);
    }

    public void setPrereqForbidden(ResourceLocation prereqId, boolean forbidden) {
        if (forbidden) {
            prereqForbidden.add(prereqId);
            prereqRequired.remove(prereqId); 
        } else {
            prereqForbidden.remove(prereqId);
        }
    }

    public Set<ResourceLocation> getPrereqForbidden() {
        return Collections.unmodifiableSet(prereqForbidden);
    }

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
