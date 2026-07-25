package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicThreadedMachine extends WorkableElectricMultiblockMachine {

    private static final int THREAD_COUNT = 8;

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

    @Getter
    private final BasicThreadedMachine.RecipeThread[] threads = new BasicThreadedMachine.RecipeThread[THREAD_COUNT];

    @Getter
    private float cachedFloraBoost = 0.0f;

    @Getter
    @SaveField
    private long totalWorkTicks = 0L;

    @SaveField
    private final long[] threadProgress = new long[THREAD_COUNT];

    @SaveField
    private final long[] threadDuration = new long[THREAD_COUNT];

    public BasicThreadedMachine(BlockEntityCreationInfo holder) {
        super(holder);

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new BasicThreadedMachine.RecipeThread();
        }

        subscribeServerTick(this::threadedTick);
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        restoreThreadsFromPersisted();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        for (BasicThreadedMachine.RecipeThread t : threads) t.clear();
    }

    private void threadedTick() {
        if (!isFormed()) return;

        boolean mainRunning = recipeLogic.isWorking();

        for (int i = 0; i < THREAD_COUNT; i++) {
            if (threads[i].isActive() && mainRunning) {
                tickThread(threads[i]);
            }
        }

        if (mainRunning && getOffsetTimer() % 5 == 0) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                if (!threads[i].isActive()) {
                    tryStartExtraRecipe(threads[i]);
                    break;
                }
            }
        }

        if (getOffsetTimer() % 5 == 0) {
            saveThreadsToPersisted();
            if (this.getSyncDataHolder() != null) {
                this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
            }
        }
    }

    private void tryStartExtraRecipe(BasicThreadedMachine.RecipeThread thread) {
        GTRecipe mainRecipe = recipeLogic.getLastRecipe();

        var iterator = recipeLogic.searchRecipe();
        if (iterator == null) return;

        while (iterator.hasNext()) {
            GTRecipe candidate = iterator.next();
            if (candidate == null) continue;

            if (mainRecipe != null && candidate.id.equals(mainRecipe.id)) continue;

            if (isRecipeAlreadyThreaded(candidate)) continue;

            ModifierFunction modifier = recipeModifier(this, candidate);
            if (modifier == ModifierFunction.NULL) continue;
            GTRecipe modifiedRecipe = modifier.apply(candidate.copy());
            if (modifiedRecipe == null) continue;

            var chanceCaches = new HashMap<RecipeCapability<?>, Object2IntMap<?>>();

            if (!RecipeHelper.matchRecipe(this, modifiedRecipe).isSuccess()) continue;

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

    private void tickThread(BasicThreadedMachine.RecipeThread thread) {
        GTRecipe recipe = thread.recipe;

        var euResult = RecipeHelper.handleTickRecipeIO(this, recipe, IO.IN, thread.chanceCaches);
        if (!euResult.isSuccess()) {

            return;
        }

        thread.progress++;

        if (thread.progress >= thread.duration) {
            RecipeHelper.handleRecipeIO(this, recipe, IO.OUT, thread.chanceCaches);
            thread.clear();
        }
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof BasicThreadedMachine imbuer)) {
            return RecipeModifier.nullWrongType(BasicThreadedMachine.class, machine);
        }

        return ModifierFunction.IDENTITY;
    }

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

    public List<GTRecipe> getActiveRecipes() {
        List<GTRecipe> list = new ArrayList<>();
        for (BasicThreadedMachine.RecipeThread t : threads) {
            if (t.isActive()) list.add(t.recipe);
        }
        return list;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;

        if (getLevel() instanceof ServerLevel) {
            int activeThreads = 0;
            for (BasicThreadedMachine.RecipeThread t : threads) if (t.isActive()) activeThreads++;

            if (activeThreads > 0) {
                final int active = activeThreads;
                widgets.add(new TextWidget<>(
                        Text.dynamic(() -> Component.literal("§dActive Threads: §f" + active + " / " + THREAD_COUNT))));
                for (int i = 0; i < THREAD_COUNT; i++) {
                    final int idx = i;
                    widgets.add(new TextWidget<>(Text.dynamic(() -> {
                        BasicThreadedMachine.RecipeThread t = threads[idx];
                        if (!t.isActive()) return Component.empty();
                        int pct = (int) (t.getProgressPercent() * 100);
                        return Component.literal("  §8Thread " + (idx + 1) + ": §7" + pct + "%");
                    })));
                }
            } else {
                widgets.add(new TextWidget<>(Text.of(Component.literal("§8Threads Idle"))));
            }
        }
        return widgets;
    }
}
