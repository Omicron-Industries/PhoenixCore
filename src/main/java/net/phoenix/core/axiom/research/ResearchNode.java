package net.phoenix.core.axiom.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.axiom.AxiomDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A single node in an Axiom research tree.
 *
 * JSON schema (in data/<namespace>/axiom/research/<tree_id>.json, under "nodes"):
 * <pre>
 * {
 *   "id": "phoenixcore:basic_alloys",
 *   "title": "Basic Alloys",
 *   "lore": "The foundation of material science.",
 *   "icon": "minecraft:iron_ingot",
 *   "position": [0, 0],
 *   "prerequisites": ["phoenixcore:root"],
 *   "exclusion_group": "material_path_1",   // optional — nodes sharing a group are mutually exclusive
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
    /** Item ID used as the node icon in the tree GUI. */
    public final String icon;
    /** GUI layout position: [x, y] in tree canvas units. */
    public final int posX;
    public final int posY;
    public final List<ResourceLocation> prerequisites;
    /** Nodes sharing the same non-null exclusion group are mutually exclusive. */
    public final String exclusionGroup;
    public final Map<AxiomDataType, Long> cost;
    public final List<ResearchUnlock> unlocks;

    public ResearchNode(ResourceLocation id, String title, String lore, String icon,
                        int posX, int posY, List<ResourceLocation> prerequisites,
                        String exclusionGroup, Map<AxiomDataType, Long> cost,
                        List<ResearchUnlock> unlocks) {
        this.id             = id;
        this.title          = title;
        this.lore           = lore;
        this.icon           = icon;
        this.posX           = posX;
        this.posY           = posY;
        this.prerequisites  = List.copyOf(prerequisites);
        this.exclusionGroup = exclusionGroup;
        this.cost           = Map.copyOf(cost);
        this.unlocks        = List.copyOf(unlocks);
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    public static ResearchNode fromJson(JsonObject obj) {
        ResourceLocation id    = new ResourceLocation(obj.get("id").getAsString());
        String title           = obj.get("title").getAsString();
        String lore            = obj.has("lore") ? obj.get("lore").getAsString() : "";
        String icon            = obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:book";
        int posX               = 0, posY = 0;
        if (obj.has("position")) {
            JsonArray pos = obj.getAsJsonArray("position");
            posX = pos.get(0).getAsInt();
            posY = pos.get(1).getAsInt();
        }
        String exclusionGroup  = obj.has("exclusion_group") ? obj.get("exclusion_group").getAsString() : null;

        List<ResourceLocation> prereqs = new ArrayList<>();
        if (obj.has("prerequisites")) {
            obj.getAsJsonArray("prerequisites")
                    .forEach(e -> prereqs.add(new ResourceLocation(e.getAsString())));
        }

        Map<AxiomDataType, Long> cost = new EnumMap<>(AxiomDataType.class);
        if (obj.has("cost")) {
            JsonObject costObj = obj.getAsJsonObject("cost");
            for (AxiomDataType type : AxiomDataType.values()) {
                if (costObj.has(type.id())) {
                    cost.put(type, costObj.get(type.id()).getAsLong());
                }
            }
        }

        List<ResearchUnlock> unlocks = new ArrayList<>();
        if (obj.has("unlocks")) {
            obj.getAsJsonArray("unlocks")
                    .forEach(e -> unlocks.add(ResearchUnlock.fromJson(e.getAsJsonObject())));
        }

        return new ResearchNode(id, title, lore, icon, posX, posY, prereqs, exclusionGroup, cost, unlocks);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True if this node has no prerequisites (i.e. it is a root node). */
    public boolean isRoot() { return prerequisites.isEmpty(); }

    /**
     * Whether a player can currently research this node given their unlocked set
     * and the set of nodes locked out by prior exclusion choices.
     */
    public boolean canUnlock(Set<ResourceLocation> unlocked, Set<ResourceLocation> lockedOut) {
        if (lockedOut.contains(id)) return false;
        if (unlocked.contains(id))  return false;
        return unlocked.containsAll(prerequisites);
    }
}
