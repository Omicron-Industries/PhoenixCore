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

public class ResearchNode {

    public final ResourceLocation id;
    public final String title;
    public final String lore;
    
    public final String hint;
    public final String icon;
    public final int posX;
    public final int posY;
    
    public final List<ResourceLocation> prerequisites;
    
    public final List<ResourceLocation> prerequisitesAny;
    public final String exclusionGroup;
    public final Map<ConfluxDataType, Long> cost;
    public final List<ResearchUnlock> unlocks;
    
    public final boolean isCommitmentNode;
    
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

    public boolean isRoot() { return prerequisites.isEmpty() && prerequisitesAny.isEmpty(); }

    public boolean canUnlock(Set<ResourceLocation> unlocked, Set<ResourceLocation> lockedOut) {
        if (lockedOut.contains(id))          return false;
        if (unlocked.contains(id))           return false;
        if (!unlocked.containsAll(prerequisites)) return false;
        if (!prerequisitesAny.isEmpty() && prerequisitesAny.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }

    public boolean isVisible(Set<ResourceLocation> unlocked) {
        if (!hidden) return true;
        if (!unlocked.containsAll(prerequisites)) return false;
        if (!prerequisitesAny.isEmpty() && prerequisitesAny.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }
}
