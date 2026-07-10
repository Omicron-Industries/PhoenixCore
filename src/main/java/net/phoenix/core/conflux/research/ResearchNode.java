package net.phoenix.core.conflux.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A single node in an Axiom research tree.
 *
 * JSON schema (in data/<namespace>/conflux/research/<tree_id>.json, under "nodes"):
 * <pre>
 * {
 *   "id": "phoenixcore:basic_alloys",
 *   "title": "Basic Alloys",
 *   "lore":  "The foundation of material science.",
 *   "hint":  "Something stirs in the lattice...",   // shown on mystery node before revealed; optional
 *   "icon":  "minecraft:iron_ingot",
 *   "position": [0, 0],
 *
 *   // ALL of these must be unlocked (classic linear prereq):
 *   "prerequisites": ["phoenixcore:root"],
 *
 *   // ANY ONE of these is enough (allows multiple paths to this node):
 *   "prerequisites_any": ["phoenixcore:path_a", "phoenixcore:path_b"],
 *
 *   // Hidden nodes are rendered as "???" until prerequisites are satisfied.
 *   // Once revealed they behave like normal nodes.
 *   "hidden": true,
 *
 *   "exclusion_group": "material_path_1",
 *   "commitment": true,
 *   "cost": { "material": 500, "energetic": 200 },
 *   "unlocks": [
 *     { "type": "recipe_tag", "value": "phoenixcore:basic_alloy_recipes" },
 *     { "type": "flag",       "value": "basic_metallurgy" }
 *   ]
 * }
 * </pre>
 */
public class ResearchNode {

    public final ResourceLocation id;
    public final String title;
    public final String lore;
    /**
     * Short cryptic text shown in the detail panel while the node is still hidden.
     * If empty, the panel shows nothing for mystery nodes.
     */
    public final String hint;
    public final String icon;
    public final int posX;
    public final int posY;
    /** All of these must be unlocked before this node is available. */
    public final List<ResourceLocation> prerequisites;
    /**
     * At least one of these must be unlocked before this node is available.
     * Combined with {@link #prerequisites}: both conditions must pass.
     * Empty list means this condition is ignored.
     */
    public final List<ResourceLocation> prerequisitesAny;
    public final String exclusionGroup;
    public final Map<ConfluxDataType, Long> cost;
    public final List<ResearchUnlock> unlocks;
    /** Point-of-no-return: committing to this Discipline once unlocked. */
    public final boolean isCommitmentNode;
    /**
     * Hidden nodes are not shown in the GUI until all prerequisites are met.
     * Before reveal they appear as dim {@code ???} nodes; edges are drawn faintly.
     * The {@link #hint} field is shown in the detail panel for the mystery node.
     */
    public final boolean hidden;

    public ResearchNode(ResourceLocation id, String title, String lore, String hint, String icon,
                        int posX, int posY,
                        List<ResourceLocation> prerequisites, List<ResourceLocation> prerequisitesAny,
                        String exclusionGroup, Map<ConfluxDataType, Long> cost,
                        List<ResearchUnlock> unlocks, boolean isCommitmentNode, boolean hidden) {
        this.id               = id;
        this.title            = title;
        this.lore             = lore;
        this.hint             = hint;
        this.icon             = icon;
        this.posX             = posX;
        this.posY             = posY;
        this.prerequisites    = List.copyOf(prerequisites);
        this.prerequisitesAny = List.copyOf(prerequisitesAny);
        this.exclusionGroup   = exclusionGroup;
        this.cost             = Map.copyOf(cost);
        this.unlocks          = List.copyOf(unlocks);
        this.isCommitmentNode = isCommitmentNode;
        this.hidden           = hidden;
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    public static ResearchNode fromJson(JsonObject obj) {
        ResourceLocation id    = new ResourceLocation(obj.get("id").getAsString());
        String title           = obj.has("title") ? obj.get("title").getAsString() : id.getPath();
        String lore            = obj.has("lore")  ? obj.get("lore").getAsString()  : "";
        String hint            = obj.has("hint")  ? obj.get("hint").getAsString()  : "";
        String icon            = obj.has("icon")  ? obj.get("icon").getAsString()  : "minecraft:book";
        boolean hidden         = obj.has("hidden")     && obj.get("hidden").getAsBoolean();
        boolean commitmentNode = obj.has("commitment") && obj.get("commitment").getAsBoolean();

        int posX = 0, posY = 0;
        if (obj.has("position")) {
            JsonArray pos = obj.getAsJsonArray("position");
            posX = pos.get(0).getAsInt();
            posY = pos.get(1).getAsInt();
        }

        String exclusionGroup = obj.has("exclusion_group") ? obj.get("exclusion_group").getAsString() : null;

        List<ResourceLocation> prereqs = new ArrayList<>();
        if (obj.has("prerequisites"))
            obj.getAsJsonArray("prerequisites").forEach(e -> prereqs.add(new ResourceLocation(e.getAsString())));

        List<ResourceLocation> prereqsAny = new ArrayList<>();
        if (obj.has("prerequisites_any"))
            obj.getAsJsonArray("prerequisites_any").forEach(e -> prereqsAny.add(new ResourceLocation(e.getAsString())));

        Map<ConfluxDataType, Long> cost = new EnumMap<>(ConfluxDataType.class);
        if (obj.has("cost")) {
            JsonObject costObj = obj.getAsJsonObject("cost");
            for (ConfluxDataType type : ConfluxDataType.values())
                if (costObj.has(type.id())) cost.put(type, costObj.get(type.id()).getAsLong());
        }

        List<ResearchUnlock> unlocks = new ArrayList<>();
        if (obj.has("unlocks"))
            obj.getAsJsonArray("unlocks").forEach(e -> unlocks.add(ResearchUnlock.fromJson(e.getAsJsonObject())));

        return new ResearchNode(id, title, lore, hint, icon, posX, posY,
                prereqs, prereqsAny, exclusionGroup, cost, unlocks, commitmentNode, hidden);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isRoot() { return prerequisites.isEmpty() && prerequisitesAny.isEmpty(); }

    /**
     * Whether this node is currently available to unlock.
     * Requires: not locked out, not already unlocked, all {@code prerequisites} met,
     * and (if {@code prerequisitesAny} is non-empty) at least one of them met.
     */
    public boolean canUnlock(Set<ResourceLocation> unlocked, Set<ResourceLocation> lockedOut) {
        if (lockedOut.contains(id))          return false;
        if (unlocked.contains(id))           return false;
        if (!unlocked.containsAll(prerequisites)) return false;
        if (!prerequisitesAny.isEmpty() && prerequisitesAny.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }

    /**
     * Whether this node should be visible in the GUI.
     * Hidden nodes are invisible until all prerequisites (both lists) are satisfied.
     */
    public boolean isVisible(Set<ResourceLocation> unlocked) {
        if (!hidden) return true;
        if (!unlocked.containsAll(prerequisites)) return false;
        if (!prerequisitesAny.isEmpty() && prerequisitesAny.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }
}
