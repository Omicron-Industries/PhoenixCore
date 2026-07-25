package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.saveddata.UniqueMultiblockSavedData;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;

import java.util.List;
import java.util.UUID;

public class UniqueWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine {

    public UniqueWorkableElectricMultiblockMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @SaveField
    @SyncToClient
    public boolean isDuplicate = false;

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);

        if (getLevel() instanceof ServerLevel serverLevel) {
            UUID owner = getOwnerUUID();
            String multiblockId = getDefinition().getId().toString();
            String dimension = getLevel().dimension().location().toString();
            BlockPos pos = getBlockPos();

            UniqueMultiblockSavedData uniqueMultiblockMapping = UniqueMultiblockSavedData.getOrCreate(serverLevel);

            handleUniqueRegistration(uniqueMultiblockMapping, owner, multiblockId, dimension, pos);
        }
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);

        if (getLevel() instanceof ServerLevel serverLevel) {
            UUID owner = getOwnerUUID();
            String multiblockId = getDefinition().getId().toString();
            String dimension = getLevel().dimension().location().toString();
            BlockPos pos = getBlockPos();

            UniqueMultiblockSavedData uniqueMultiblockMapping = UniqueMultiblockSavedData.getOrCreate(serverLevel);

            handleUniqueRemoval(uniqueMultiblockMapping, owner, multiblockId, dimension, pos);
        }
    }

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
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        if (this.isDuplicate) {
            return List.of(
                    new TextWidget<>(Text.of(Component.translatable("phoenixcore.multiblock.duplicate.1")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)))),
                    new TextWidget<>(Text.of(Component.translatable("phoenixcore.multiblock.duplicate.2")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)))));
        }
        return super.getWidgetsForDisplay(syncManager);
    }
}
