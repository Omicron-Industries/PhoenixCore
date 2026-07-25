package net.phoenix.core.conflux.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.terminal.ResearchTerminalBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldResearchData extends SavedData {

    private static final String ID = "conflux_team_research";

    private final Map<UUID, Set<ResourceLocation>> unlocked    = new HashMap<>();
    
    private final Map<UUID, Set<ResourceLocation>> lockedOut   = new HashMap<>();
    
    private final Map<UUID, Set<String>>           flags       = new HashMap<>();
    
    private final Map<UUID, Set<ResourceLocation>> multiblocks = new HashMap<>();
    
    private final Map<UUID, String>                discipline  = new HashMap<>();
    
    private final Map<UUID, Boolean>               committed   = new HashMap<>();

    public static WorldResearchData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(WorldResearchData::load, WorldResearchData::new, ID);
    }

    public boolean isUnlocked(UUID team, ResourceLocation nodeId) {
        return teamSet(unlocked, team).contains(nodeId);
    }

    public boolean isLockedOut(UUID team, ResourceLocation nodeId) {
        return teamSet(lockedOut, team).contains(nodeId);
    }

    public boolean hasFlag(UUID team, String flag) {
        return teamSet(flags, team).contains(flag);
    }

    public boolean canFormMultiblock(UUID team, ResourceLocation machineId) {
        Set<ResourceLocation> gated = ResearchTreeRegistry.INSTANCE.getGatedMultiblocks();
        if (!gated.contains(machineId)) return true;
        return teamSet(multiblocks, team).contains(machineId);
    }

    public @Nullable String getDiscipline(UUID team) { return discipline.get(team); }

    public boolean isCommitted(UUID team) { return Boolean.TRUE.equals(committed.get(team)); }

    public Set<ResourceLocation> getUnlocked(UUID team)  { return Collections.unmodifiableSet(teamSet(unlocked, team)); }
    public Set<ResourceLocation> getLockedOut(UUID team) { return Collections.unmodifiableSet(teamSet(lockedOut, team)); }
    public Set<String>           getFlags(UUID team)     { return Collections.unmodifiableSet(teamSet(flags, team)); }

    public DisciplineInfo getDisciplineInfo(UUID team, ResearchTreeRegistry registry) {
        String discId = discipline.get(team);
        if (discId == null) return DisciplineInfo.NONE;

        ResearchTree tree = registry.getAllTrees().stream()
                .filter(t -> discId.equals(t.discipline))
                .findFirst()
                .orElse(null);

        boolean isCommitted = isCommitted(team);
        Map<ConfluxDataType, Long> switchCost = (tree != null && !isCommitted)
                ? tree.switchCost : Map.of();
        String title = tree != null ? tree.title : discId;

        return new DisciplineInfo(discId, title, isCommitted, switchCost);
    }

    public boolean tryUnlock(UUID team, ResearchNode node,
                             ResearchTerminalBlockEntity terminal,
                             ResearchTreeRegistry registry) {
        if (!node.canUnlock(getUnlocked(team), getLockedOut(team))) return false;
        if (!terminal.trySpend(node.cost)) return false;

        teamSet(unlocked, team).add(node.id);

        registry.getAllTrees().stream()
                .filter(ResearchTree::isDisciplineTree)
                .filter(t -> t.getNode(node.id).isPresent())
                .findFirst()
                .ifPresent(tree -> {
                    discipline.putIfAbsent(team, tree.discipline);
                    if (node.isCommitmentNode) committed.put(team, true);
                });

        for (ResearchUnlock unlock : node.unlocks) {
            switch (unlock.type()) {
                case "flag"       -> teamSet(flags, team).add(unlock.value());
                case "multiblock" -> {
                    ResourceLocation machineId = ResourceLocation.tryParse(unlock.value());
                    if (machineId != null) teamSet(multiblocks, team).add(machineId);
                }
            }
        }

        if (node.exclusionGroup != null) {
            registry.getAllNodes().stream()
                    .filter(n -> node.exclusionGroup.equals(n.exclusionGroup))
                    .filter(n -> !n.id.equals(node.id))
                    .forEach(n -> teamSet(lockedOut, team).add(n.id));
        }

        setDirty();
        return true;
    }

    public boolean abandonDiscipline(UUID team, ResearchTerminalBlockEntity terminal,
                                     ResearchTreeRegistry registry) {
        if (isCommitted(team)) return false;

        String discId = discipline.get(team);
        if (discId == null) return false;

        ResearchTree tree = registry.getAllTrees().stream()
                .filter(t -> discId.equals(t.discipline))
                .findFirst()
                .orElse(null);

        if (tree != null && !tree.switchCost.isEmpty()) {
            if (!terminal.trySpend(tree.switchCost)) return false;
        }

        if (tree != null) {
            Set<ResourceLocation> disciplineNodeIds = new HashSet<>();
            tree.getNodes().forEach(n -> disciplineNodeIds.add(n.id));

            teamSet(unlocked, team).removeAll(disciplineNodeIds);
            teamSet(lockedOut, team).removeAll(disciplineNodeIds);

            for (ResearchNode node : tree.getNodes()) {
                for (ResearchUnlock unlock : node.unlocks) {
                    switch (unlock.type()) {
                        case "flag"       -> teamSet(flags, team).remove(unlock.value());
                        case "multiblock" -> {
                            ResourceLocation ml = ResourceLocation.tryParse(unlock.value());
                            if (ml != null) teamSet(multiblocks, team).remove(ml);
                        }
                    }
                }
            }
        }

        discipline.remove(team);
        committed.remove(team);
        setDirty();
        return true;
    }

    public boolean hasRecipeTag(UUID team, String recipeTag, ResearchTreeRegistry registry) {
        for (ResourceLocation nodeId : getUnlocked(team)) {
            ResearchNode node = registry.getNode(nodeId).orElse(null);
            if (node == null) continue;
            for (ResearchUnlock unlock : node.unlocks) {
                if ("recipe_tag".equals(unlock.type()) && recipeTag.equals(unlock.value())) return true;
            }
        }
        return false;
    }

    public static WorldResearchData load(CompoundTag tag) {
        WorldResearchData d = new WorldResearchData();
        if (tag.contains("teams", Tag.TAG_LIST)) {
            tag.getList("teams", Tag.TAG_COMPOUND).forEach(e -> {
                CompoundTag t = (CompoundTag) e;
                UUID id = t.getUUID("id");
                readRLSet(t, "unlocked").forEach(rl    -> d.teamSet(d.unlocked,    id).add(rl));
                readRLSet(t, "lockedOut").forEach(rl   -> d.teamSet(d.lockedOut,   id).add(rl));
                readRLSet(t, "multiblocks").forEach(rl -> d.teamSet(d.multiblocks, id).add(rl));
                readStringList(t, "flags").forEach(f   -> d.teamSet(d.flags,       id).add(f));
                if (t.contains("discipline", Tag.TAG_STRING)) d.discipline.put(id, t.getString("discipline"));
                if (t.contains("committed",  Tag.TAG_BYTE))   d.committed.put(id, t.getBoolean("committed"));
            });
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag teams = new ListTag();
        Set<UUID> allTeams = new HashSet<>();
        allTeams.addAll(unlocked.keySet());
        allTeams.addAll(flags.keySet());
        allTeams.addAll(multiblocks.keySet());
        allTeams.addAll(discipline.keySet());

        for (UUID id : allTeams) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            writeRLSet(t, "unlocked",    teamSet(unlocked,    id));
            writeRLSet(t, "lockedOut",   teamSet(lockedOut,   id));
            writeRLSet(t, "multiblocks", teamSet(multiblocks, id));
            writeStringList(t, "flags",  teamSet(flags,       id));
            String disc = discipline.get(id);
            if (disc != null) t.putString("discipline", disc);
            if (Boolean.TRUE.equals(committed.get(id))) t.putBoolean("committed", true);
            teams.add(t);
        }
        tag.put("teams", teams);
        return tag;
    }

    private <V> Set<V> teamSet(Map<UUID, Set<V>> map, UUID team) {
        return map.computeIfAbsent(team, k -> new HashSet<>());
    }

    private static void writeRLSet(CompoundTag tag, String key, Set<ResourceLocation> set) {
        ListTag list = new ListTag();
        set.forEach(rl -> list.add(StringTag.valueOf(rl.toString())));
        tag.put(key, list);
    }

    private static Set<ResourceLocation> readRLSet(CompoundTag tag, String key) {
        Set<ResourceLocation> set = new HashSet<>();
        if (tag.contains(key, Tag.TAG_LIST))
            tag.getList(key, Tag.TAG_STRING).forEach(t -> set.add(new ResourceLocation(t.getAsString())));
        return set;
    }

    private static void writeStringList(CompoundTag tag, String key, Set<String> set) {
        ListTag list = new ListTag();
        set.forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put(key, list);
    }

    private static Set<String> readStringList(CompoundTag tag, String key) {
        Set<String> set = new HashSet<>();
        if (tag.contains(key, Tag.TAG_LIST))
            tag.getList(key, Tag.TAG_STRING).forEach(t -> set.add(t.getAsString()));
        return set;
    }
}
