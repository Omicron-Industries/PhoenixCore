package net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaWirelessRegistry;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.UUID;

public class TeslaEnergyHatchPartMachine extends EnergyHatchPartMachine implements IDataStickInteractable {

    public static final boolean TESLA_DEBUG = false;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            TeslaEnergyHatchPartMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private UUID ownerTeamUUID;

    public boolean isUplink() {
        return getIO() == IO.OUT;
    }

    public boolean isDownlink() {
        return getIO() == IO.IN;
    }

    private TeslaTowerMachine boundTower;

    private TickableSubscription tickSubscription;

    @Getter
    @Persisted
    @DescSynced
    private String customName = "";

    public void setCustomName(String name) {
        this.customName = name;
        self().markDirty();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!getLevel().isClientSide && getLevel() instanceof ServerLevel server) {
            autoLinkTeamIfNeeded();
            if (ownerTeamUUID != null) {
                TeslaTeamEnergyData.get(server).setEnergyBuffered(
                        ownerTeamUUID,
                        getLevel(),
                        getPos(),
                        BigInteger.valueOf(energyContainer.getEnergyStored()),
                        getIO() == IO.OUT);
            }
            updateTickSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!getLevel().isClientSide && getLevel() instanceof ServerLevel server) {
            if (self().getHolder().self().isRemoved()) {
                TeslaTeamEnergyData.get(server).removeMachineFromAllTeams(getPos());
            } else if (ownerTeamUUID != null) {
                TeslaTeamEnergyData.get(server).setEnergyBuffered(
                        ownerTeamUUID,
                        getLevel(),
                        getPos(),
                        BigInteger.valueOf(energyContainer.getEnergyStored()),
                        getIO() == IO.OUT);
            }
            TeslaWirelessRegistry.unregisterHatch(this);
        }
        unsubscribeFromTick();
    }

    public TeslaEnergyHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int amperage, Object... args) {
        super(holder, tier, io, amperage, args);
    }

    @Override
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] addedToController: {} at {}, isTeslaTower={}",
                controller.getClass().getSimpleName(), getPos(), controller instanceof TeslaTowerMachine);

        if (controller instanceof TeslaTowerMachine) {
            if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Unsubscribing from tick (Tesla Tower)");
            unsubscribeFromTick();
        } else {
            if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Updating tick subscription (Other multiblock)");

            updateTickSubscription();
        }
    }

    @Override
    public void removedFromController(@NotNull IMultiController controller) {
        super.removedFromController(controller);
        updateTickSubscription();
    }

    /**
     * Updates tick subscription based on wireless status and multiblock membership.
     * Subscribes if wireless AND (not in multiblock OR in non-Tesla multiblock).
     */
    private void updateTickSubscription() {
        boolean shouldTick = false;

        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] updateTickSubscription called at {}", getPos());
        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] isWireless={}, controllers={}",
                isWireless(), getControllers().size());

        if (isWireless()) {
            if (getControllers().isEmpty()) {

                shouldTick = true;
                if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Not in multiblock, should tick");
            } else {
                shouldTick = getControllers().stream()
                        .noneMatch(ctrl -> ctrl instanceof TeslaTowerMachine);
                if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] In multiblock, shouldTick={}", shouldTick);
            }
        }

        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] shouldTick={}, currentSubscription={}",
                shouldTick, tickSubscription != null);

        if (shouldTick) {
            if (tickSubscription == null) {
                tickSubscription = subscribeServerTick(this::tickWireless);
                if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Subscribed to tick!");
            }
        } else {
            if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Unsubscribing from tick");
            unsubscribeFromTick();
        }
    }

    private void unsubscribeFromTick() {
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public IEnergyContainer getEnergyContainer() {
        return energyContainer;
    }

    public IO getIO() {
        return io;
    }

    public @Nullable TeslaTowerMachine getBoundTower() {
        return boundTower;
    }

    public void bindToTower(TeslaTowerMachine tower) {
        this.boundTower = tower;
        self().markDirty();
    }

    public boolean isWireless() {
        if (ownerTeamUUID == null) return false;

        PhoenixConfigs.FeatureConfigs.TeslaConnectionMode mode = PhoenixConfigs.INSTANCE.features.teslaConnectionMode;

        return mode == PhoenixConfigs.FeatureConfigs.TeslaConnectionMode.TEAM_AUTO ||
                mode == PhoenixConfigs.FeatureConfigs.TeslaConnectionMode.DATA_STICK;
    }

    @Getter
    private long lastTransferRate = 0;
    @Getter
    private long lastTransferAmount = 0;

    public void tickWireless() {
        if (getLevel() == null || getLevel().isClientSide || ownerTeamUUID == null) return;
        if (!isWireless()) return;

        ServerLevel sl = (ServerLevel) getLevel();
        TeslaTeamEnergyData data = TeslaTeamEnergyData.get(sl);
        TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(ownerTeamUUID);
        if (!data.isOnline(ownerTeamUUID)) return;

        teamData.markHatchActive(getPos(), sl.getGameTime());

        long voltage = com.gregtechceu.gtceu.api.GTValues.V[getTier()];
        long transferLimit = voltage * getAmperage();

        BigInteger moved = BigInteger.ZERO;

        if (getIO() == IO.IN) {
            long space = energyContainer.getEnergyCapacity() - energyContainer.getEnergyStored();
            if (space > 0) {
                BigInteger toPull = BigInteger.valueOf(Math.min(transferLimit, space));
                moved = teamData.drain(toPull);
                if (moved.signum() > 0) {
                    energyContainer.changeEnergy(moved.longValue());

                    teamData.energyInput.merge(getPos(), moved, BigInteger::add);
                }
            }
        } else {
            long stored = energyContainer.getEnergyStored();
            if (stored > 0) {
                BigInteger toPush = BigInteger.valueOf(Math.min(transferLimit, stored));
                moved = teamData.fill(toPush);
                if (moved.signum() > 0) {
                    energyContainer.changeEnergy(-moved.longValue());

                    teamData.energyOutput.merge(getPos(), moved, BigInteger::add);
                }
            }
        }

        data.setEnergyBuffered(ownerTeamUUID, getLevel(), getPos(),
                BigInteger.valueOf(energyContainer.getEnergyStored()), getIO() == IO.OUT);
    }

    public @Nullable UUID getOwnerTeamUUID() {
        autoLinkTeamIfNeeded();
        return ownerTeamUUID;
    }

    private void autoLinkTeamIfNeeded() {
        if (!(getLevel() instanceof ServerLevel sl)) return;

        if (PhoenixConfigs.INSTANCE.features.teslaConnectionMode ==
                PhoenixConfigs.FeatureConfigs.TeslaConnectionMode.DATA_STICK)
            return;

        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) return;

        UUID team = TeamUtils.getTeamIdOrPlayerFallback(ownerUUID);

        if (this.ownerTeamUUID == null || !team.equals(this.ownerTeamUUID)) {
            this.ownerTeamUUID = team;
            self().markDirty();

            TeslaWirelessRegistry.unregisterHatch(this);
            TeslaWirelessRegistry.registerHatch(this);
            updateTickSubscription();

            TeslaTeamEnergyData.get(sl).setEnergyBuffered(
                    ownerTeamUUID,
                    getLevel(),
                    getPos(),
                    BigInteger.valueOf(energyContainer.getEnergyStored()),
                    getIO() == IO.OUT);

        }
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack binder) {
        if (!binder.is(PhoenixItems.TESLA_BINDER.get())) return InteractionResult.PASS;

        var tag = binder.getTag();
        if (tag != null && tag.hasUUID("TargetTeam")) {
            UUID newTeamUUID = tag.getUUID("TargetTeam");

            if (!getLevel().isClientSide && getLevel() instanceof ServerLevel server) {
                if (!newTeamUUID.equals(ownerTeamUUID)) {

                    if (ownerTeamUUID != null) {
                        TeslaTeamEnergyData.get(server).removeEndpoint(ownerTeamUUID, getPos());
                    }

                    this.ownerTeamUUID = newTeamUUID;
                    this.boundTower = null;
                    self().markDirty();

                    TeslaWirelessRegistry.unregisterHatch(this);
                    TeslaWirelessRegistry.registerHatch(this);
                    updateTickSubscription();

                    TeslaTeamEnergyData.get((ServerLevel) getLevel()).setEnergyBuffered(
                            ownerTeamUUID,
                            getLevel(),
                            getPos(),
                            java.math.BigInteger.valueOf(energyContainer.getEnergyStored()),
                            getIO() == IO.OUT);

                    player.sendSystemMessage(Component
                            .literal("Tesla Hatch: Connected to frequency " + ownerTeamUUID.toString().substring(0, 8) +
                                    "...")
                            .withStyle(ChatFormatting.AQUA));
                } else {
                    player.sendSystemMessage(Component.literal("Tesla Hatch: Already synced to this frequency.")
                            .withStyle(ChatFormatting.GRAY));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
