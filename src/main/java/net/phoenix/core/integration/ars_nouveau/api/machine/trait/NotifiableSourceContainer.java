package net.phoenix.core.integration.ars_nouveau.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NotifiableSourceContainer extends NotifiableRecipeHandlerTrait<SourceIngredient> implements ISourceTile {

    public static final MachineTraitType<NotifiableSourceContainer> TRAIT_TYPE = new MachineTraitType<>(
            NotifiableSourceContainer.class);

    @SaveField
    @SyncToClient
    private long currentSource;

    @SaveField
    private long maxSource;

    @SaveField
    private int maxConsumption;

    @SaveField
    @SyncToClient
    private int currentFlow;

    @SaveField
    private long lastFlowUpdate;

    @SaveField
    private GTRecipe lastRecipe;

    private final IO handlerIO;

    public NotifiableSourceContainer(IO io, int maxCapacity, int maxConsumption) {
        super();
        this.setMaxSource(maxCapacity);
        this.maxConsumption = maxConsumption;
        this.handlerIO = io;
    }

    public int getCurrentFlow() {
        if (getLevel() != null && !(getMachine() instanceof WorkableElectricMultiblockMachine)) {

            long time = getLevel().getGameTime();
            if (time - lastFlowUpdate > 20) {
                this.currentFlow = 0;
                this.getSyncDataHolder().markClientSyncFieldDirty("currentFlow");
            }
        }
        return currentFlow;
    }

    public void updateFlow(int delta) {
        if (getLevel() != null) {
            this.lastFlowUpdate = getLevel().getGameTime();
        }
        this.currentFlow += delta;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentFlow");
    }

    public void resetFlow() {
        this.currentFlow = 0;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentFlow");
    }

    @Override
    public IO getHandlerIO() {
        return handlerIO;
    }

    @Override
    public List<SourceIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SourceIngredient> left,
                                                    boolean simulate) {
        if (io != this.handlerIO) return left;

        if (!simulate && recipe != null) {
            if (this.lastRecipe != recipe) {
                this.currentFlow = 0;
                this.lastRecipe = recipe;
            }
        }

        for (int i = 0; i < left.size(); i++) {
            SourceIngredient ingredient = left.get(i);
            int amountNeeded = ingredient.getSource();

            if (io == IO.IN) {
                if (currentSource >= amountNeeded) {
                    if (!simulate) {
                        removeSource(amountNeeded);
                        updateFlow(-amountNeeded);
                    }
                    left.remove(i);
                    break;
                }
            } else {
                if (maxSource - currentSource >= amountNeeded) {
                    if (!simulate) {
                        addSource(amountNeeded);
                        updateFlow(amountNeeded);
                    }
                    left.remove(i);
                    break;
                }
            }
        }

        return left;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public @NotNull List getContents() {
        return List.of(new SourceIngredient((int) currentSource));
    }

    @Override
    public RecipeCapability<SourceIngredient> getCapability() {
        return SourceRecipeCapability.CAP;
    }

    @Override
    public double getTotalContentAmount() {
        return currentSource;
    }

    @Override
    public int getTransferRate() {
        return maxConsumption;
    }

    @Override
    public boolean canAcceptSource() {
        return currentSource < maxSource;
    }

    @Override
    public int getSource() {
        return Math.toIntExact(currentSource);
    }

    @Override
    public int getMaxSource() {
        return Math.toIntExact(maxSource);
    }

    @Override
    public void setMaxSource(int max) {
        this.maxSource = max;
    }

    @Override
    public int setSource(int source) {
        this.currentSource = Math.min(source, maxSource);
        this.getSyncDataHolder().markClientSyncFieldDirty("currentSource");
        return Math.toIntExact(this.currentSource);
    }

    @Override
    public int addSource(int amount) {
        int inserted = Math.toIntExact(Math.min(amount, maxSource - currentSource));
        currentSource += inserted;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentSource");
        return inserted;
    }

    @Override
    public int removeSource(int amount) {
        int extracted = Math.toIntExact(Math.min(amount, currentSource));
        currentSource -= extracted;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentSource");
        return extracted;
    }

    @Override
    public MachineTraitType<?> getTraitType() {
        return TRAIT_TYPE;
    }
}
