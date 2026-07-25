package net.phoenix.core.conflux.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.ConfluxDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResearchTree {

    public final ResourceLocation id;
    public final String title;
    public final String description;
    
    @Nullable
    public final String discipline;
    
    public final Map<ConfluxDataType, Long> switchCost;

    private final Map<ResourceLocation, ResearchNode> nodes = new LinkedHashMap<>();

    public ResearchTree(ResourceLocation id, String title, String description,
                        @Nullable String discipline, Map<ConfluxDataType, Long> switchCost,
                        List<ResearchNode> nodeList) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.discipline  = discipline;
        this.switchCost  = Map.copyOf(switchCost);
        nodeList.forEach(n -> nodes.put(n.id, n));
    }

    public Optional<ResearchNode> getNode(ResourceLocation nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Collection<ResearchNode> getNodes() { return nodes.values(); }

    public List<ResearchNode> getRoots() {
        return nodes.values().stream().filter(ResearchNode::isRoot).toList();
    }

    public static ResearchTree fromJson(ResourceLocation id, JsonObject obj) {
        String title       = obj.has("title")       ? obj.get("title").getAsString()       : id.getPath();
        String description = obj.has("description") ? obj.get("description").getAsString() : "";
        String discipline  = obj.has("discipline")  ? obj.get("discipline").getAsString()  : null;

        Map<ConfluxDataType, Long> switchCost = new EnumMap<>(ConfluxDataType.class);
        if (obj.has("switch_cost")) {
            JsonObject sc = obj.getAsJsonObject("switch_cost");
            for (ConfluxDataType type : ConfluxDataType.values()) {
                if (sc.has(type.id())) switchCost.put(type, sc.get(type.id()).getAsLong());
            }
        }

        List<ResearchNode> nodes = obj.has("nodes")
                ? obj.getAsJsonArray("nodes").asList().stream()
                        .map(e -> ResearchNode.fromJson(e.getAsJsonObject()))
                        .toList()
                : List.of();

        return new ResearchTree(id, title, description, discipline, switchCost, nodes);
    }

    public Optional<ResearchNode> getCommitmentNode() {
        return nodes.values().stream().filter(n -> n.isCommitmentNode).findFirst();
    }

    public boolean isDisciplineTree() { return discipline != null; }
}
