package net.phoenix.core.axiom.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.axiom.terminal.ResearchTerminalBlockEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-player Axiom research state: which nodes are unlocked, which are locked out
 * by prior exclusion choices, and which string flags have been granted.
 *
 * Attached as a Forge capability via {@link PlayerResearchCapability}.
 */
public class PlayerResearchData {

    private final Set<ResourceLocation> unlocked = new HashSet<>();
    private final Set<ResourceLocation> lockedOut = new HashSet<>();
    /** String flags granted by "flag" unlock entries. Queryable by external systems. */
    private final Set<String> flags = new HashSet<>();

    // ── Queries ───────────────────────────────────────────────────────────────

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

    public boolean canUnlock(ResearchNode node) {
        return node.canUnlock(unlocked, lockedOut);
    }

    // ── Unlock ────────────────────────────────────────────────────────────────

    /**
     * Attempt to unlock a node, spending data from the given terminal.
     *
     * @return true if the unlock succeeded (prerequisites met, not locked out, costs paid)
     */
    public boolean tryUnlock(ResearchNode node, ResearchTerminalBlockEntity terminal,
                             ResearchTreeRegistry registry) {
        if (!canUnlock(node)) return false;
        if (!terminal.trySpend(node.cost)) return false;

        unlocked.add(node.id);

        // Apply unlocks
        for (ResearchUnlock unlock : node.unlocks) {
            if (unlock.type().equals("flag")) flags.add(unlock.value());
            // "recipe_tag" unlocks are checked dynamically against this data — no action needed here
        }

        // Apply mutual exclusion: lock out siblings in the same exclusion group
        if (node.exclusionGroup != null) {
            registry.getAllNodes().stream()
                    .filter(n -> node.exclusionGroup.equals(n.exclusionGroup))
                    .filter(n -> !n.id.equals(node.id))
                    .forEach(n -> lockedOut.add(n.id));
        }

        return true;
    }

    // ── Recipe unlock check ───────────────────────────────────────────────────

    /**
     * Returns true if the player has any unlock of type "recipe_tag" matching the given tag.
     * Used by AxiomRecipeCondition to gate GT recipes behind research.
     */
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

    // ── NBT ───────────────────────────────────────────────────────────────────

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

    // ── Sync helper ───────────────────────────────────────────────────────────

    /** Copies state from another instance (used when syncing server→client). */
    public void copyFrom(PlayerResearchData other) {
        unlocked.clear();
        unlocked.addAll(other.unlocked);
        lockedOut.clear();
        lockedOut.addAll(other.lockedOut);
        flags.clear();
        flags.addAll(other.flags);
    }
}
