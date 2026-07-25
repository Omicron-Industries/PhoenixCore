package net.phoenix.core.axiom.research;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.axiom.AxiomDataType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResearchNode {

    public final ResourceLocation id;
    public final String title;
    public final String lore;
    
    public final String icon;
    
    public final int posX;
    public final int posY;
    public final List<ResourceLocation> prerequisites;
    
    public final String exclusionGroup;
    public final Map<AxiomDataType, Long> cost;
    public final List<ResearchUnlock> unlocks;

    public ResearchNode(ResourceLocation id, String title, String lore, String icon,
                        int posX, int posY, List<ResourceLocation> prerequisites,
                        String exclusionGroup, Map<AxiomDataType, Long> cost,
                        List<ResearchUnlock> unlocks) {
        this.id = id;
        this.title = title;
        this.lore = lore;
        this.icon = icon;
        this.posX = posX;
        this.posY = posY;
        this.prerequisites = List.copyOf(prerequisites);
        this.exclusionGroup = exclusionGroup;
        this.cost = Map.copyOf(cost);
        this.unlocks = List.copyOf(unlocks);
    }

    public static ResearchNode fromJson(JsonObject obj) {
        ResourceLocation id = new ResourceLocation(obj.get("id").getAsString());
        String title = obj.get("title").getAsString();
        String lore = obj.has("lore") ? obj.get("lore").getAsString() : "";
        String icon = obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:book";
        int posX = 0, posY = 0;
        if (obj.has("position")) {
            JsonArray pos = obj.getAsJsonArray("position");
            posX = pos.get(0).getAsInt();
            posY = pos.get(1).getAsInt();
        }
        String exclusionGroup = obj.has("exclusion_group") ? obj.get("exclusion_group").getAsString() : null;

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

    public boolean isRoot() {
        return prerequisites.isEmpty();
    }

    public boolean canUnlock(Set<ResourceLocation> unlocked, Set<ResourceLocation> lockedOut) {
        if (lockedOut.contains(id)) return false;
        if (unlocked.contains(id)) return false;
        return unlocked.containsAll(prerequisites);
    }
}
