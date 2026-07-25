package net.phoenix.core.integration.recipe_helper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class RecipeBuilderState {

    public static final RecipeBuilderState INSTANCE = new RecipeBuilderState();

    private RecipeBuilderState() {}

    public int recipeTypeIdx = 0;
    public String recipeId = "", duration = "", eut = "", sourceIn = "", sourceOut = "";

    public static class SavedItem {

        public String key = "";
        public int count = 1;
        public boolean notConsumable = false;
    }

    public final List<SavedItem> itemInputs = new ArrayList<>(), itemOutputs = new ArrayList<>();

    public static class SavedFluid {

        public String fluidExpr = null;
        public int amount = 1000;
    }

    public final List<SavedFluid> fluidInputs = new ArrayList<>(), fluidOutputs = new ArrayList<>();

    public final List<String> conditions = new ArrayList<>();

    public void save(RecipeBuilderScreen s) {
        recipeTypeIdx = s.recipeTypeDropdown.getSelectedIdx();
        recipeId = s.recipeIdBox.getValue();
        duration = s.durationBox.getValue();
        eut = s.eutBox.getValue();
        sourceIn = s.sourceInBox.getValue();
        sourceOut = s.sourceOutBox.getValue();
        snapItems(itemInputs, s.itemInputPanel);
        snapItems(itemOutputs, s.itemOutputPanel);
        snapFluids(fluidInputs, s.fluidInputPanel);
        snapFluids(fluidOutputs, s.fluidOutputPanel);
        conditions.clear();
        for (ConditionsScreen.ConditionEntry e : s.getConditions()) {
            StringBuilder sb = new StringBuilder(e.type().name());
            for (String v : e.values()) sb.append("|").append(v == null ? "" : v);
            conditions.add(sb.toString());
        }
    }

    private void snapItems(List<SavedItem> out, SlotPanel p) {
        out.clear();
        for (SlotPanel.SlotEntry e : p.getEntries()) {
            SavedItem s = new SavedItem();
            if (!e.stack.isEmpty()) {
                s.key = ForgeRegistries.ITEMS.getKey(e.stack.getItem()).toString();
                s.count = e.count;
                s.notConsumable = e.notConsumable;
            }
            out.add(s);
        }
    }

    private void snapFluids(List<SavedFluid> out, FluidSlotPanel p) {
        out.clear();
        for (FluidSlotPanel.FluidEntry e : p.getEntries()) {
            SavedFluid f = new SavedFluid();
            f.fluidExpr = e.fluidExpr;
            f.amount = e.amount;
            out.add(f);
        }
    }

    public void restore(RecipeBuilderScreen s) {
        s.recipeTypeDropdown.setSelectedIdx(recipeTypeIdx);
        s.recipeIdBox.setValue(recipeId);
        s.durationBox.setValue(duration);
        s.eutBox.setValue(eut);
        s.sourceInBox.setValue(sourceIn);
        s.sourceOutBox.setValue(sourceOut);
        applyItems(itemInputs, s.itemInputPanel);
        applyItems(itemOutputs, s.itemOutputPanel);
        applyFluids(fluidInputs, s.fluidInputPanel);
        applyFluids(fluidOutputs, s.fluidOutputPanel);
        List<ConditionsScreen.ConditionEntry> list = new ArrayList<>();
        for (String enc : conditions) {
            String[] parts = enc.split("\\|", -1);
            if (parts.length < 1) continue;
            try {
                ConditionsScreen.ConditionType t = ConditionsScreen.ConditionType.valueOf(parts[0]);
                String[] vals = new String[parts.length - 1];
                for (int i = 1; i < parts.length; i++) vals[i - 1] = parts[i];
                list.add(new ConditionsScreen.ConditionEntry(t, vals));
            } catch (IllegalArgumentException ignored) {}
        }
        s.setConditions(list);
    }

    private void applyItems(List<SavedItem> saved, SlotPanel panel) {
        List<SlotPanel.SlotEntry> entries = panel.getEntries();
        for (int i = 0; i < Math.min(saved.size(), entries.size()); i++) {
            SavedItem s = saved.get(i);
            if (s.key == null || s.key.isEmpty()) continue;
            try {
                var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.key));
                if (item != null) {
                    SlotPanel.SlotEntry e = entries.get(i);
                    e.stack = new ItemStack(item, s.count);
                    e.count = s.count;
                    e.notConsumable = s.notConsumable;
                }
            } catch (Exception ignored) {}
        }
    }

    private void applyFluids(List<SavedFluid> saved, FluidSlotPanel panel) {
        List<FluidSlotPanel.FluidEntry> entries = panel.getEntries();
        for (int i = 0; i < Math.min(saved.size(), entries.size()); i++) {
            FluidSlotPanel.FluidEntry e = entries.get(i);
            e.fluidExpr = saved.get(i).fluidExpr;
            e.amount = saved.get(i).amount;
        }
    }

    public void clear() {
        recipeTypeIdx = 0;
        recipeId = duration = eut = sourceIn = sourceOut = "";
        itemInputs.clear();
        itemOutputs.clear();
        fluidInputs.clear();
        fluidOutputs.clear();
        conditions.clear();
    }
}
