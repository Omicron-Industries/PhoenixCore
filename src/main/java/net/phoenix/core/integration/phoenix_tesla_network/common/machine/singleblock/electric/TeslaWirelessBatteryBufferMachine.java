package net.phoenix.core.integration.phoenix_tesla_network.common.machine.singleblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine.State;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import top.theillusivec4.curios.api.CuriosApi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeslaWirelessBatteryBufferMachine extends TieredEnergyMachine
                                               implements IDataStickInteractable, IMuiMachine {

    @SaveField
    @SyncToClient
    private UUID boundTeam;

    @SaveField
    @SyncToClient
    private long lastTransferred = 0L;

    @SaveField
    @SyncToClient
    private State state = State.IDLE;

    private final List<UUID> playersInRange = new ArrayList<>();
    private TickableSubscription tickSubs;

    public TeslaWirelessBatteryBufferMachine(BlockEntityCreationInfo holder, int tier) {
        super(holder, tier, NotifiableEnergyContainer.receiverContainer(
                com.gregtechceu.gtceu.api.GTValues.V[tier] * 64L, com.gregtechceu.gtceu.api.GTValues.V[tier], 1L));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            tickSubs = subscribeServerTick(tickSubs, this::tickCharge);
            registerToNetwork();
        }
    }

    private void changeState(State newState) {
        if (this.state != newState) {
            this.state = newState;
            setRenderState(getRenderState().setValue(GTMachineModelProperties.CHARGER_STATE, newState));
        }
    }

    private void tickCharge() {
        if (!(getLevel() instanceof ServerLevel level) || boundTeam == null) {
            changeState(State.IDLE);
            return;
        }

        TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);
        TeslaTeamEnergyData.TeamEnergy network = data.getOrCreate(boundTeam);

        if (getOffsetTimer() % 10 == 0) {
            if (network.stored.signum() <= 0) {
                this.lastTransferred = 0L;
                network.machineDisplayFlow.put(getBlockPos(), 0L);
                changeState(State.IDLE);
                return;
            }

            List<Player> playersToCharge = new ArrayList<>();
            for (Player player : level.getServer().getPlayerList().getPlayers()) {
                if (TeamUtils.isPlayerOnTeam(player, boundTeam)) {
                    playersToCharge.add(player);
                }
            }

            handleRangeNotifications(playersToCharge);

            long movedInThisCycle = 0L;
            long voltage = GTValues.V[getTier()];
            long amps = 4;
            long ticksInBatch = 10;

            long totalBatchBudget = voltage * amps * ticksInBatch;

            for (Player player : playersToCharge) {
                List<IItemHandler> inventories = new ArrayList<>();
                inventories.add(new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()));
                CuriosApi.getCuriosInventory(player).ifPresent(h -> inventories.add(h.getEquippedCurios()));

                for (IItemHandler handler : inventories) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (totalBatchBudget <= 0) break;

                        ItemStack stack = handler.getStackInSlot(i);
                        if (stack.isEmpty()) continue;

                        IElectricItem electric = GTCapabilityHelper.getElectricItem(stack);
                        if (electric != null && electric.chargeable()) {

                            long itemNeeded = electric.getMaxCharge() - electric.getCharge();
                            if (itemNeeded <= 0) continue;

                            long networkAvailable = network.stored.min(BigInteger.valueOf(totalBatchBudget))
                                    .longValue();
                            long offer = Math.min(itemNeeded, Math.min(totalBatchBudget, networkAvailable));

                            if (offer > 0) {
                                long accepted = electric.charge(offer, getTier(), true, false);
                                if (accepted > 0) {
                                    network.drain(BigInteger.valueOf(accepted));
                                    movedInThisCycle += accepted;
                                    totalBatchBudget -= accepted;
                                }
                            }
                        }
                    }
                }
            }

            this.lastTransferred = movedInThisCycle / ticksInBatch;
            network.machineDisplayFlow.put(getBlockPos(), this.lastTransferred);

            if (movedInThisCycle > 0) {
                network.machineCurrentFlow.merge(getBlockPos(), movedInThisCycle, Long::sum);
                network.markHatchActive(getBlockPos(), level.getGameTime());
                data.setDirty();
                changeState(State.RUNNING);
            } else {
                changeState(playersToCharge.isEmpty() ? State.IDLE : State.FINISHED);
            }
        }
    }

    private void handleRangeNotifications(List<Player> nearby) {
        List<UUID> nearbyUUIDs = nearby.stream().map(Player::getUUID).toList();
        for (Player p : nearby) {
            if (!playersInRange.contains(p.getUUID())) {
                p.displayClientMessage(Component.literal("Tesla Field Connected").withStyle(ChatFormatting.AQUA), true);
                playersInRange.add(p.getUUID());
            }
        }
        playersInRange.removeIf(uuid -> {
            if (!nearbyUUIDs.contains(uuid)) {
                Player p = getLevel().getPlayerByUUID(uuid);
                if (p != null)
                    p.displayClientMessage(Component.literal("Tesla Field Disconnected").withStyle(ChatFormatting.GRAY),
                            true);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (boundTeam != null && getLevel() instanceof ServerLevel level) {
            TeslaTeamEnergyData.get(level).getOrCreate(boundTeam).machineDisplayFlow.remove(getBlockPos());
        }
    }

    private void registerToNetwork() {
        if (!isRemote() && boundTeam != null && getLevel() instanceof ServerLevel level) {
            TeslaTeamEnergyData.get(level).getOrCreate(boundTeam).addCharger(getBlockPos());
        }
    }

    private void unregisterFromNetwork() {
        if (!isRemote() && boundTeam != null && getLevel() instanceof ServerLevel level) {
            TeslaTeamEnergyData.get(level).getOrCreate(boundTeam).removeCharger(getBlockPos());
        }
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack stick) {
        if (!stick.is(PhoenixItems.TESLA_BINDER.get())) return InteractionResult.PASS;
        if (isRemote()) return InteractionResult.SUCCESS;

        UUID stickTeam = stick.getOrCreateTag().getUUID("TargetTeam");

        if (this.boundTeam != null && this.boundTeam.equals(stickTeam)) {
            unregisterFromNetwork();
            this.boundTeam = null;
            player.sendSystemMessage(Component.literal("Charger Unbound").withStyle(ChatFormatting.YELLOW));
        } else {
            unregisterFromNetwork();
            this.boundTeam = stickTeam;
            registerToNetwork();
            player.sendSystemMessage(Component.literal("Charger Synchronized").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (this.getSyncDataHolder() != null) {
            this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        ModularPanel<?> panel = new ModularPanel<>("phoenix_core:tesla_wireless_charger_panel")
                .size(170, 95);

        Flow layout = Flow.column()
                .size(170, 95)
                .padding(10)
                .crossAxisAlignment(Alignment.CrossAxis.START);

        layout.child(new TextWidget<>(Component.literal("Tesla Field Generator")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).marginBottom(8));

        layout.child(new TextWidget<>(() -> {
            if (boundTeam == null) {
                return Component.literal("STATUS: ").append(Component.literal("UNBOUND").withStyle(ChatFormatting.RED));
            } else {
                return Component.literal("NETWORK: ")
                        .append(Component.literal(boundTeam.toString().substring(0, 8)).withStyle(ChatFormatting.AQUA));
            }
        }).marginBottom(4));

        layout.child(new TextWidget<>(() -> {
            if (boundTeam == null) return Component.empty();
            return Component.literal("RANGE: ")
                    .append(Component.literal("Omnipresent (Global)").withStyle(ChatFormatting.LIGHT_PURPLE));
        }).marginBottom(4));

        layout.child(new TextWidget<>(() -> {
            if (boundTeam == null) return Component.empty();
            String rate = com.gregtechceu.gtceu.utils.FormattingUtil.formatNumbers(lastTransferred);
            return Component.literal("OUTPUT: ")
                    .append(Component.literal(rate + " EU/t").withStyle(ChatFormatting.GREEN));
        }));

        panel.child(layout);
        return panel;
    }
}
