package net.phoenix.core.axiom.research;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResearchTree {

    public final ResourceLocation id;
    public final String title;
    public final String description;

    private final Map<ResourceLocation, ResearchNode> nodes = new LinkedHashMap<>();

    public ResearchTree(ResourceLocation id, String title, String description, List<ResearchNode> nodeList) {
        this.id = id;
        this.title = title;
        this.description = description;
        nodeList.forEach(n -> nodes.put(n.id, n));
    }

    public Optional<ResearchNode> getNode(ResourceLocation nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Collection<ResearchNode> getNodes() {
        return nodes.values();
    }

    public List<ResearchNode> getRoots() {
        return nodes.values().stream().filter(ResearchNode::isRoot).toList();
    }

    public static ResearchTree fromJson(ResourceLocation id, JsonObject obj) {
        String title = obj.has("title") ? obj.get("title").getAsString() : id.getPath();
        String description = obj.has("description") ? obj.get("description").getAsString() : "";

        List<ResearchNode> nodes = obj.has("nodes") ? obj.getAsJsonArray("nodes").asList().stream()
                .map(e -> ResearchNode.fromJson(e.getAsJsonObject()))
                .toList() : List.of();

        return new ResearchTree(id, title, description, nodes);
    }
}
