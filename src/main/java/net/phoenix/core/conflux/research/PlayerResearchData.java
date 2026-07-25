package net.phoenix.core.conflux.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.terminal.ResearchTerminalBlockEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayerResearchData {

    private final Set<ResourceLocation> unlocked = new HashSet<>();
    private final Set<ResourceLocation> lockedOut = new HashSet<>();

    private final Set<String> flags = new HashSet<>();

    public boolean isUnlocked(ResourceLocation nodeId) {
        return unlocked.contains(nodeId);
    }

    public boolean isLockedOut(ResourceLocation nodeId) {
        return lockedOut.contains(nodeId);
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public Set<ResourceLocation> getUnlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    public Set<ResourceLocation> getLockedOut() {
        return Collections.unmodifiableSet(lockedOut);
    }

    public int getUnlockedCount() {
        return unlocked.size();
    }

    public boolean canUnlock(ResearchNode node) {
        return node.canUnlock(unlocked, lockedOut);
    }

    public boolean tryUnlock(ResearchNode node, ResearchTerminalBlockEntity terminal,
                             ResearchTreeRegistry registry) {
        if (!canUnlock(node)) return false;
        if (!terminal.trySpend(node.cost)) return false;

        unlocked.add(node.id);

        for (ResearchUnlock unlock : node.unlocks) {
            if (unlock.type().equals("flag")) flags.add(unlock.value());

        }

        if (node.exclusionGroup != null) {
            registry.getAllNodes().stream()
                    .filter(n -> node.exclusionGroup.equals(n.exclusionGroup))
                    .filter(n -> !n.id.equals(node.id))
                    .forEach(n -> lockedOut.add(n.id));
        }

        return true;
    }

    public boolean hasRecipeTag(String recipeTag, ResearchTreeRegistry registry) {
        for (ResourceLocation nodeId : unlocked) {
            ResearchNode node = registry.getNode(nodeId).orElse(null);
            if (node == null) continue;
            for (ResearchUnlock unlock : node.unlocks) {
                if ("recipe_tag".equals(unlock.type()) && recipeTag.equals(unlock.value())) return true;
            }
        }
        return false;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag unlockedList = new ListTag();
        unlocked.forEach(id -> unlockedList.add(StringTag.valueOf(id.toString())));
        tag.put("unlocked", unlockedList);

        ListTag lockedList = new ListTag();
        lockedOut.forEach(id -> lockedList.add(StringTag.valueOf(id.toString())));
        tag.put("locked_out", lockedList);

        ListTag flagList = new ListTag();
        flags.forEach(f -> flagList.add(StringTag.valueOf(f)));
        tag.put("flags", flagList);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        unlocked.clear();
        lockedOut.clear();
        flags.clear();

        if (tag.contains("unlocked", Tag.TAG_LIST))
            tag.getList("unlocked", Tag.TAG_STRING)
                    .forEach(t -> unlocked.add(new ResourceLocation(t.getAsString())));

        if (tag.contains("locked_out", Tag.TAG_LIST))
            tag.getList("locked_out", Tag.TAG_STRING)
                    .forEach(t -> lockedOut.add(new ResourceLocation(t.getAsString())));

        if (tag.contains("flags", Tag.TAG_LIST))
            tag.getList("flags", Tag.TAG_STRING)
                    .forEach(t -> flags.add(t.getAsString()));
    }

    public void copyFrom(PlayerResearchData other) {
        unlocked.clear();
        unlocked.addAll(other.unlocked);
        lockedOut.clear();
        lockedOut.addAll(other.lockedOut);
        flags.clear();
        flags.addAll(other.flags);
    }
}
