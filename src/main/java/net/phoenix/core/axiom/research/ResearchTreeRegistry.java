package net.phoenix.core.axiom.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ResearchTreeRegistry extends SimpleJsonResourceReloadListener {

    public static final ResearchTreeRegistry INSTANCE = new ResearchTreeRegistry();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<ResourceLocation, ResearchTree> trees = new LinkedHashMap<>();

    private ResearchTreeRegistry() {
        super(GSON, "axiom/research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager mgr, ProfilerFiller profiler) {
        trees.clear();
        data.forEach((id, element) -> {
            JsonObject json = element.getAsJsonObject();
            try {
                trees.put(id, ResearchTree.fromJson(id, json));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Axiom research tree: " + id, e);
            }
        });
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

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
