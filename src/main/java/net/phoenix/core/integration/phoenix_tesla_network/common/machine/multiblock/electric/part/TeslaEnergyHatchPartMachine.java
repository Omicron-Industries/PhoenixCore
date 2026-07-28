package net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.gui.TeslaBackground;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaWirelessRegistry;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.UUID;

public class TeslaEnergyHatchPartMachine extends EnergyHatchPartMachine implements IDataStickInteractable, IMuiMachine {

    public static final boolean TESLA_DEBUG = false;

    @SaveField
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
    @SaveField
    @SyncToClient
    private String customName = "";

    public void setCustomName(String name) {
        this.customName = name;
        this.markAsChanged();
    }

    public TeslaEnergyHatchPartMachine(BlockEntityCreationInfo holder, int tier, IO io, int amperage) {
        super(holder, tier, io, amperage);
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
                        getBlockPos(),
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
            if (this.isRemoved()) {
                TeslaTeamEnergyData.get(server).removeMachineFromAllTeams(getBlockPos());
            } else if (ownerTeamUUID != null) {
                TeslaTeamEnergyData.get(server).setEnergyBuffered(
                        ownerTeamUUID,
                        getLevel(),
                        getBlockPos(),
                        BigInteger.valueOf(energyContainer.getEnergyStored()),
                        getIO() == IO.OUT);
            }
            TeslaWirelessRegistry.unregisterHatch(this);
        }
        unsubscribeFromTick();
    }

    @Override
    public void addedToController(@NotNull com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller,
                                  String substructureName) {
        super.addedToController(controller, substructureName);

        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] addedToController: {} at {}, isTeslaTower={}",
                controller.getClass().getSimpleName(), getBlockPos(), controller instanceof TeslaTowerMachine);

        if (controller instanceof TeslaTowerMachine) {
            if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Unsubscribing from tick (Tesla Tower)");
            unsubscribeFromTick();
        } else {
            if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] Updating tick subscription (Other multiblock)");
            updateTickSubscription();
        }
    }

    @Override
    public void removedFromController(@NotNull com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        updateTickSubscription();
    }

    private void updateTickSubscription() {
        boolean shouldTick = false;

        if (TESLA_DEBUG) PhoenixCore.LOGGER.info("[TESLA DEBUG] updateTickSubscription called at {}", getBlockPos());
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
        this.markAsChanged();
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

        teamData.markHatchActive(getBlockPos(), sl.getGameTime());

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
                    teamData.energyInput.merge(getBlockPos(), moved, BigInteger::add);
                }
            }
        } else {
            long stored = energyContainer.getEnergyStored();
            if (stored > 0) {
                BigInteger toPush = BigInteger.valueOf(Math.min(transferLimit, stored));
                moved = teamData.fill(toPush);
                if (moved.signum() > 0) {
                    energyContainer.changeEnergy(-moved.longValue());
                    teamData.energyOutput.merge(getBlockPos(), moved, BigInteger::add);
                }
            }
        }

        data.setEnergyBuffered(ownerTeamUUID, getLevel(), getBlockPos(),
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
            this.markAsChanged();

            TeslaWirelessRegistry.unregisterHatch(this);
            TeslaWirelessRegistry.registerHatch(this);
            updateTickSubscription();

            TeslaTeamEnergyData.get(sl).setEnergyBuffered(
                    ownerTeamUUID,
                    getLevel(),
                    getBlockPos(),
                    BigInteger.valueOf(energyContainer.getEnergyStored()),
                    getIO() == IO.OUT);
        }
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack binder) {
        return onDataStickUse(player, binder);
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
                        TeslaTeamEnergyData.get(server).removeEndpoint(ownerTeamUUID, getBlockPos());
                    }

                    this.ownerTeamUUID = newTeamUUID;
                    this.boundTower = null;
                    this.markAsChanged();

                    TeslaWirelessRegistry.unregisterHatch(this);
                    TeslaWirelessRegistry.registerHatch(this);
                    updateTickSubscription();

                    TeslaTeamEnergyData.get((ServerLevel) getLevel()).setEnergyBuffered(
                            ownerTeamUUID,
                            getLevel(),
                            getBlockPos(),
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

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        if (isRemote()) {
            mainWidget.background(new TeslaBackground(getBorderColor()));
        }

        Flow titleRow = Flow.row()
                .coverChildren()
                .marginBottom(8);

        var titleWidget = Text.dynamic(() -> Component.literal("TESLA HATCH")
                .withStyle(ChatFormatting.AQUA))
                .asWidget();
        titleWidget.marginRight(6);
        titleRow.child(titleWidget);

        var badgeWidget = Text.dynamic(this::getDirectionBadge).asWidget();
        titleRow.child(badgeWidget);

        mainWidget.child(Flow.col()
                .coverChildren()
                .margin(8, 6)
                .child(titleRow)
                .child(Text.dynamic(() -> Component.literal("Direction: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(isUplink() ? "Uplink" : "Downlink")
                                .withStyle(isUplink() ? ChatFormatting.GREEN : ChatFormatting.DARK_AQUA)))
                        .asWidget())
                .child(Text.dynamic(() -> {
                    long stored = energyContainer != null ? energyContainer.getEnergyStored() : 0L;
                    long capacity = energyContainer != null ? energyContainer.getEnergyCapacity() : 0L;
                    return Component.literal("Buffered: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(stored + " / " + capacity + " EU")
                                    .withStyle(ChatFormatting.WHITE));
                }).asWidget())
                .child(Text.dynamic(this::getLinkStatusComponent).asWidget())
                .child(Text.dynamic(() -> Component.literal("Network: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(isWireless() ? "Active" : "Inactive")
                                .withStyle(isWireless() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)))
                        .asWidget()));
    }

    private Component getDirectionBadge() {
        boolean isOut = getIO() == IO.OUT;
        return Component.literal(isOut ? "[OUT]" : "[IN]")
                .withStyle(isOut ? ChatFormatting.AQUA : ChatFormatting.GREEN);
    }

    private Component getLinkStatusComponent() {
        UUID team = ownerTeamUUID;
        if (team == null) {
            return Component.literal("Link: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Unlinked").withStyle(ChatFormatting.RED));
        }
        String teamName = TeamUtils.getTeamName(team);
        return Component.literal("Link: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(teamName != null ? teamName : "Unknown Team")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private int getBorderColor() {
        return 0xAA00C0FF;
    }
}
