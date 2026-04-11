package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicThreadedMachine extends WorkableElectricMultiblockMachine {
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final int THREAD_COUNT = 8;

    // -------------------------------------------------------------------------
    // Per-thread state
    // -------------------------------------------------------------------------

    public static class RecipeThread {

        @Nullable
        public GTRecipe recipe;
        public long progress;
        public long duration;
        public final Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches = new HashMap<>();

        public boolean isActive() {
            return recipe != null;
        }

        public float getProgressPercent() {
            if (duration <= 0) return 0f;
            return (float) progress / (float) duration;
        }

        public void clear() {
            recipe = null;
            progress = 0;
            duration = 0;
            chanceCaches.clear();
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    @Getter
    private final BasicThreadedMachine.RecipeThread[] threads = new BasicThreadedMachine.RecipeThread[THREAD_COUNT];

    @Getter
    private float cachedFloraBoost = 0.0f;

    @Getter
    @Persisted
    private long totalWorkTicks = 0L;

    @Persisted
    private final long[] threadProgress = new long[THREAD_COUNT];

    @Persisted
    private final long[] threadDuration = new long[THREAD_COUNT];

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public BasicThreadedMachine(IMachineBlockEntity holder) {
        super(holder);

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new BasicThreadedMachine.RecipeThread();
        }

        // The base RecipeLogic handles the main recipe completely on its own.
        // We only need our own tick for the extra threads and bookkeeping.
        subscribeServerTick(this::threadedTick);
    }

    // -------------------------------------------------------------------------
    // Structure lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        restoreThreadsFromPersisted();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        for (BasicThreadedMachine.RecipeThread t : threads) t.clear();
    }

    // -------------------------------------------------------------------------
    // Main tick — only handles extra threads and bookkeeping.
    // The primary recipe runs via the normal RecipeLogic tick, untouched.
    // -------------------------------------------------------------------------

    private void threadedTick() {
        if (!isFormed()) return;

        // Extra threads only run while the main recipe is actively working.
        boolean mainRunning = recipeLogic.isWorking();

        // Tick all already-active threads every tick.
        for (int i = 0; i < THREAD_COUNT; i++) {
            if (threads[i].isActive() && mainRunning) {
                tickThread(threads[i]);
            }
        }

        // Start at most ONE new thread per tick, always filling from index 0 upward.
        // This ensures thread 0 is always filled before thread 1, etc.
        if (mainRunning && getOffsetTimer() % 5 == 0) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                if (!threads[i].isActive()) {
                    tryStartExtraRecipe(threads[i]);
                    break; // only start one per search tick
                }
            }
        }

        if (getOffsetTimer() % 5 == 0) {
            saveThreadsToPersisted();
            this.markDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Extra recipe search
    // -------------------------------------------------------------------------

    /**
     * Finds a recipe that is NOT the same as the currently running main recipe,
     * then atomically consumes its inputs and assigns it to the given thread.
     */
    private void tryStartExtraRecipe(BasicThreadedMachine.RecipeThread thread) {
        GTRecipe mainRecipe = recipeLogic.getLastRecipe();

        var iterator = recipeLogic.searchRecipe();
        if (iterator == null) return;

        while (iterator.hasNext()) {
            GTRecipe candidate = iterator.next();
            if (candidate == null) continue;

            // Skip the recipe that the main logic is already running.
            if (mainRecipe != null && candidate.id.equals(mainRecipe.id)) continue;

            // Skip if another thread is already running this recipe.
            if (isRecipeAlreadyThreaded(candidate)) continue;

            // Apply modifier.
            ModifierFunction modifier = recipeModifier(this, candidate);
            if (modifier == ModifierFunction.NULL) continue;
            GTRecipe modifiedRecipe = modifier.apply(candidate.copy());
            if (modifiedRecipe == null) continue;

            var chanceCaches = new HashMap<RecipeCapability<?>, Object2IntMap<?>>();

            // Dry-run check.
            if (!RecipeHelper.matchRecipe(this, modifiedRecipe).isSuccess()) continue;

            // Atomically consume inputs.
            var result = RecipeHelper.handleRecipeIO(this, modifiedRecipe, IO.IN, chanceCaches);
            if (!result.isSuccess()) continue;

            thread.recipe = modifiedRecipe;
            thread.progress = 0;
            thread.duration = modifiedRecipe.duration;
            thread.chanceCaches.clear();
            thread.chanceCaches.putAll(chanceCaches);
            return;
        }
    }

    private boolean isRecipeAlreadyThreaded(GTRecipe recipe) {
        for (BasicThreadedMachine.RecipeThread t : threads) {
            if (t.isActive() && t.recipe != null && t.recipe.id.equals(recipe.id)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Per-thread tick
    // -------------------------------------------------------------------------

    private void tickThread(BasicThreadedMachine.RecipeThread thread) {
        GTRecipe recipe = thread.recipe;

        var euResult = RecipeHelper.handleTickRecipeIO(this, recipe, IO.IN, thread.chanceCaches);
        if (!euResult.isSuccess()) {
            // Not enough energy — stall without losing progress.
            return;
        }

        thread.progress++;

        if (thread.progress >= thread.duration) {
            RecipeHelper.handleRecipeIO(this, recipe, IO.OUT, thread.chanceCaches);
            thread.clear();
        }
    }

    // -------------------------------------------------------------------------
    // Recipe modifier
    // -------------------------------------------------------------------------

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof BasicThreadedMachine imbuer)) {
            return RecipeModifier.nullWrongType(BasicThreadedMachine.class, machine);
        }

        return ModifierFunction.IDENTITY;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void restoreThreadsFromPersisted() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            if (threads[i].recipe != null) {
                threads[i].progress = threadProgress[i];
                threads[i].duration = threadDuration[i];
            } else {
                threads[i].clear();
            }
        }
    }

    private void saveThreadsToPersisted() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            threadProgress[i] = threads[i].progress;
            threadDuration[i] = threads[i].duration;
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public List<GTRecipe> getActiveRecipes() {
        List<GTRecipe> list = new ArrayList<>();
        for (BasicThreadedMachine.RecipeThread t : threads) {
            if (t.isActive()) list.add(t.recipe);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Display text
    // -------------------------------------------------------------------------

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (getLevel() instanceof ServerLevel serverLevel) {
            int activeThreads = 0;
            for (BasicThreadedMachine.RecipeThread t : threads) if (t.isActive()) activeThreads++;

            if (activeThreads > 0) {
                textList.add(Component.literal("§dActive Threads: §f" + activeThreads + " / " + THREAD_COUNT));
                for (int i = 0; i < THREAD_COUNT; i++) {
                    BasicThreadedMachine.RecipeThread t = threads[i];
                    if (t.isActive()) {
                        int pct = (int) (t.getProgressPercent() * 100);
                        textList.add(Component.literal("  §8Thread " + (i + 1) + ": §7" + pct + "%"));
                    }
                }
            } else {
                textList.add(Component.literal("§8Threads Idle"));
            }
        }
    }
}
