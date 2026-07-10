package net.phoenix.core.common.machine.multiblock.unique;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;

import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.saveddata.CreativeEnergySavedData;

import java.util.UUID;

@SuppressWarnings("unused")
public class CreativeEnergyMultiMachine extends UniqueWorkableElectricMultiblockMachine {

    private final ConditionalSubscriptionHandler creativeEnergySubscription;

    public CreativeEnergyMultiMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, args);

        this.creativeEnergySubscription = new ConditionalSubscriptionHandler(this, this::tickEnableCreativeEnergy,
                this::isSubscriptionActive);
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        creativeEnergySubscription.updateSubscription();
    }

    public void enableCreativeEnergy(boolean enabled) {
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) {
            ownerUUID = new UUID(0L, 0L);
        }
        if (getLevel() instanceof ServerLevel serverLevel) {
            CreativeEnergySavedData savedData = CreativeEnergySavedData
                    .getOrCreate(serverLevel.getServer().overworld());
            savedData.setEnabled(ownerUUID, enabled);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        enableCreativeEnergy(false);
    }

    private void tickEnableCreativeEnergy() {
        enableCreativeEnergy(isActive() && getRecipeLogic().isWorkingEnabled());
    }

    private Boolean isSubscriptionActive() {
        return isFormed();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        super.setWorkingEnabled(isWorkingAllowed);
        if (!isWorkingAllowed) {
            enableCreativeEnergy(false);
        }
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        enableCreativeEnergy(false);
        creativeEnergySubscription.unsubscribe();
    }
}
