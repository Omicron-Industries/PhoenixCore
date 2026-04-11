package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.saveddata.UniqueMultiblockSavedData;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

// Copied from CosmicCore with some minor changes (thank you Caitlynn!)
public class UniqueWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            UniqueWorkableElectricMultiblockMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public UniqueWorkableElectricMultiblockMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // Used to make sure you cannot have more than one of this multiblock per owner
    @Persisted
    public boolean isDuplicate = false;

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();

        if (getLevel() instanceof ServerLevel serverLevel) {
            UUID owner = getOwnerUUID();
            String multiblockId = getDefinition().getId().toString();
            String dimension = getLevel().dimension().location().toString();
            BlockPos pos = getPos();

            UniqueMultiblockSavedData uniqueMultiblockMapping = UniqueMultiblockSavedData.getOrCreate(serverLevel);

            handleUniqueRegistration(uniqueMultiblockMapping, owner, multiblockId, dimension, pos);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();

        if (getLevel() instanceof ServerLevel serverLevel) {
            UUID owner = getOwnerUUID();
            String multiblockId = getDefinition().getId().toString();
            String dimension = getLevel().dimension().location().toString();
            BlockPos pos = getPos();

            UniqueMultiblockSavedData uniqueMultiblockMapping = UniqueMultiblockSavedData.getOrCreate(serverLevel);

            handleUniqueRemoval(uniqueMultiblockMapping, owner, multiblockId, dimension, pos);
        }
    }

    /**
     * Default unique registration: one multiblock per player (owner UUID).
     * Tesla Tower will override this to use team UUID instead.
     */
    protected void handleUniqueRegistration(UniqueMultiblockSavedData data,
                                            UUID owner,
                                            String multiblockId,
                                            String dimension,
                                            BlockPos pos) {
        if (owner == null) {
            return;
        }

        if (data.hasData(owner, multiblockId)) {
            this.isDuplicate = !data.isUnique(owner, multiblockId, dimension, pos);
            if (isDuplicate) {
                recipeLogic.setStatus(RecipeLogic.Status.SUSPEND);
            }
        } else {
            data.addMultiblock(owner, multiblockId, dimension, pos);
        }
    }

    /**
     * Default unique removal for player‑owned machines.
     */
    protected void handleUniqueRemoval(UniqueMultiblockSavedData data,
                                       UUID owner,
                                       String multiblockId,
                                       String dimension,
                                       BlockPos pos) {
        if (owner == null) {
            return;
        }
        data.removeMultiblock(owner, multiblockId, dimension, pos);
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        if (this.isDuplicate) {
            textList.add(Component.translatable("phoenixcore.multiblock.duplicate.1")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
            textList.add(Component.translatable("phoenixcore.multiblock.duplicate.2")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        } else {
            super.addDisplayText(textList);
        }
    }
}
