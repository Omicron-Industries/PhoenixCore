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

/**
 * A named collection of {@link ResearchNode}s forming one discipline's tree.
 *
 * Trees are loaded from datapacks at {@code data/<ns>/conflux/research/<id>.json}.
 * Multiple trees can coexist (e.g. one per ConfluxDataType, or cross-discipline trees).
 *
 * JSON schema:
 * <pre>
 * {
 *   "title": "Material Science",
 *   "description": "The study of matter and alloys.",
 *   "discipline": "thermodynamics",          // optional — omit for neutral/shared trees
 *   "switch_cost": { "material": 2000 },     // cost to abandon before commitment; omit = free
 *   "nodes": [ { ...ResearchNode... }, ... ]
 * }
 * </pre>
 */
public class ResearchTree {

    public final ResourceLocation id;
    public final String title;
    public final String description;
    /**
     * Canonical Discipline ID this tree belongs to.
     * {@code null} for neutral/shared trees that don't lock the player to a path.
     */
    @Nullable
    public final String discipline;
    /**
     * Cost to abandon this Discipline before the commitment node is reached.
     * Empty map means abandoning is free.
     */
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

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Optional<ResearchNode> getNode(ResourceLocation nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Collection<ResearchNode> getNodes() { return nodes.values(); }

    public List<ResearchNode> getRoots() {
        return nodes.values().stream().filter(ResearchNode::isRoot).toList();
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

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

    /** The node in this tree marked {@code "commitment": true}, if any. */
    public Optional<ResearchNode> getCommitmentNode() {
        return nodes.values().stream().filter(n -> n.isCommitmentNode).findFirst();
    }

    /** True if this tree belongs to a Discipline (non-null discipline field). */
    public boolean isDisciplineTree() { return discipline != null; }
}
