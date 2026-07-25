package net.phoenix.core.conflux.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ResearchTreeRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ResearchTreeRegistry INSTANCE = new ResearchTreeRegistry();

    private final Map<ResourceLocation, ResearchTree> trees = new LinkedHashMap<>();
    
    private final Set<ResourceLocation> gatedMultiblocks = new LinkedHashSet<>();
    
    private final Set<String> gatedRecipeTags = new LinkedHashSet<>();

    private ResearchTreeRegistry() {
        super(GSON, "conflux/research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager mgr, ProfilerFiller profiler) {
        trees.clear();
        gatedMultiblocks.clear();
        gatedRecipeTags.clear();

        data.forEach((id, element) -> {
            JsonObject json = element.getAsJsonObject();
            try {
                trees.put(id, ResearchTree.fromJson(id, json));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Axiom research tree: " + id, e);
            }
        });

        for (ResearchTree tree : trees.values()) {
            for (ResearchNode node : tree.getNodes()) {
                for (ResearchUnlock unlock : node.unlocks) {
                    switch (unlock.type()) {
                        case "multiblock" -> {
                            ResourceLocation id = ResourceLocation.tryParse(unlock.value());
                            if (id != null) gatedMultiblocks.add(id);
                        }
                        case "recipe_tag" -> gatedRecipeTags.add(unlock.value());
                    }
                }
            }
        }
    }

    public Optional<ResearchTree> getTree(ResourceLocation id) {
        return Optional.ofNullable(trees.get(id));
    }

    public Collection<ResearchTree> getAllTrees() {
        return Collections.unmodifiableCollection(trees.values());
    }

    public Optional<ResearchNode> getNode(ResourceLocation nodeId) {
        for (ResearchTree tree : trees.values()) {
            Optional<ResearchNode> n = tree.getNode(nodeId);
            if (n.isPresent()) return n;
        }
        return Optional.empty();
    }

    public Collection<ResearchNode> getAllNodes() {
        return trees.values().stream()
                .flatMap(t -> t.getNodes().stream())
                .toList();
    }

    public Set<ResourceLocation> getGatedMultiblocks() { return Collections.unmodifiableSet(gatedMultiblocks); }

    public Set<String> getGatedRecipeTags() { return Collections.unmodifiableSet(gatedRecipeTags); }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
