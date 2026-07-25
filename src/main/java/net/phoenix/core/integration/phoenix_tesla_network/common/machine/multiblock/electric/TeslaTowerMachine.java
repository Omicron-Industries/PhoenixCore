package net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import com.gregtechceu.gtceu.api.machine.*;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;

import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GradientUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.machine.multiblock.unique.UniqueWorkableElectricMultiblockMachine;
import net.phoenix.core.integration.phoenix_tesla_network.api.machine.trait.ITeslaBattery;
import net.phoenix.core.integration.phoenix_tesla_network.common.block.TeslaBatteryBlock;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part.TeslaEnergyHatchPartMachine;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import static net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part.TeslaEnergyHatchPartMachine.TESLA_DEBUG;

public class TeslaTowerMachine extends UniqueWorkableElectricMultiblockMachine
                               implements IEnergyInfoProvider, IDataStickInteractable {

    public TeslaTowerMachine(BlockEntityCreationInfo holder) {
        super(holder);

        this.energyBank = new TeslaEnergyBank();

        this.attachPersistentTrait("tesla_energy_bank", this.energyBank);

        subscribeServerTick(this::transferEnergyTick);
    }

    public static final String TTB_BATTERY_HEADER = "TTBatteries_";

    private static final BigInteger BIG_INTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    @Override
    public boolean isActive() {
        return isFormed() && isWorkingEnabled() && recipeLogic.isActive();
    }

    @Getter
    private TeslaTowerMachine.TeslaEnergyBank energyBank;
    private EnergyContainerList inputHatches;
    private EnergyContainerList outputHatches;

    private long netInLastSec;
    @Getter
    private long inputPerSec;
    private long netOutLastSec;
    @Getter
    private long outputPerSec;

    private boolean introSequencePlayed = false;

    protected ConditionalSubscriptionHandler tickSubscription;

    public static TextColor nebulaColor(float speed) {
        float baseHue = 260.5f;
        float hueRange = 25f;

        float time = (GTValues.CLIENT_TIME & ((1 << 20) - 1)) * speed;

        float hue = baseHue + (float) (Math.sin(time * Math.PI / 180.0) * (hueRange / 2f));

        return TextColor.fromRgb(GradientUtil.toRGB(hue, 95f, 65f));
    }

    public static final UnaryOperator<Style> NEBULA_HSL = style -> style.withColor(nebulaColor(8.0f));

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);

        if (!getLevel().isClientSide) {
            ensureOwnerTeamUUID();
        }

        UUID ownerId = getOwnerUUID();

        if (!getLevel().isClientSide && ownerId != null && !introSequencePlayed) {
            if (getLevel() instanceof ServerLevel serverLevel) {
                Player player = serverLevel.getPlayerByUUID(ownerId);

                if (player != null) {
                    introSequencePlayed = true;

                    player.displayClientMessage(
                            Component.literal("We See You, We Know You.")
                                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                            true);

                    int messageDelay = 100;
                    serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                            serverLevel.getServer().getTickCount() + messageDelay,
                            () -> {
                                Player recheckPlayer = serverLevel.getPlayerByUUID(ownerId);
                                if (recheckPlayer != null) {
                                    recheckPlayer.sendSystemMessage(Component.literal("The Signal Has Begun.")
                                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                                }
                            }
                    ));
                }
            }
        }

        if (ownerTeamUUID != null) {
            registerTower(this);
        }
        TeslaWirelessRegistry.registerTower(this);

        MaintenanceHatchPartMachine maintenance = null;
        List<IEnergyContainer> inputs = new ArrayList<>();
        List<IEnergyContainer> outputs = new ArrayList<>();

        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof MaintenanceHatchPartMachine maintenanceMachine) {
                maintenance = maintenanceMachine;
            }

            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                IO io = handlerList.getHandlerIO();
                if (io == IO.NONE) continue;
                var containers = handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .toList();
                if (io.support(IO.IN)) inputs.addAll(containers);
                if (io.support(IO.OUT)) outputs.addAll(containers);
            }

            if (part instanceof TeslaEnergyHatchPartMachine hatch && !hatch.isWireless()) {
                if (hatch.getIO() == IO.IN) inputs.add(hatch.getEnergyContainer());
                else if (hatch.getIO() == IO.OUT) outputs.add(hatch.getEnergyContainer());
            }
        }

        this.inputHatches = new EnergyContainerList(inputs);
        this.outputHatches = new EnergyContainerList(outputs);

        List<ITeslaBattery> batteries = new ArrayList<>();
        var patternState = this.getPatternState(substructureName);

        if (patternState != null && patternState.getCache() != null) {
            
            for (var entry : patternState.getCache().long2ObjectEntrySet()) {
                BlockState state = entry.getValue().getBlockState();

                if (state.getBlock() instanceof TeslaBatteryBlock batteryBlock) {
                    
                    batteries.add(batteryBlock.getBatteryData());
                }
            }
        }

        if (batteries.isEmpty()) {
            invalidateStructure(substructureName);
            return;
        }

        if (this.energyBank == null) {
            this.energyBank = new TeslaEnergyBank();
        } else {
            this.energyBank = energyBank.rebuild(batteries);
        }

        updateBatteryTier();

        if (!getLevel().isClientSide && ownerTeamUUID != null) {
            syncToTeslaSavedData();
        }
    }

    private void pushToSoulLinkedMachines(ServerLevel level, TeslaTeamEnergyData.TeamEnergy team) {
        if (team.soulLinkedMachines.isEmpty()) return;

        for (BlockPos targetPos : team.soulLinkedMachines) {

            ResourceKey<Level> dimKey = team.getMachineDimension(targetPos);
            ServerLevel targetLevel = level.getServer().getLevel(dimKey);

            if (targetLevel == null || !targetLevel.isLoaded(targetPos)) continue;

            team.markHatchActive(targetPos, targetLevel.getGameTime());

            if (team.stored.signum() == 0) continue;

            MetaMachine machine = MetaMachine.getMachine(targetLevel, targetPos);
            if (machine == null) continue;

            long injectedThisTick = 0;

            if (machine instanceof BatteryBufferMachine charger) {
                var energy = charger.energyContainer;
                if (energy != null) {
                    long voltage = energy.getInputVoltage();
                    long available = team.stored.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
                    long maxTransfer = voltage * energy.getInputAmperage();
                    long toPush = Math.min(available, maxTransfer);

                    long accepted = energy.acceptEnergyFromNetwork(null, voltage,
                            (long) Math.ceil((double) toPush / voltage));

                    if (accepted > 0) {
                        injectedThisTick = accepted * voltage;
                        team.drain(BigInteger.valueOf(injectedThisTick));
                    }
                }
            } else if (machine instanceof TieredEnergyMachine tieredMachine) {
                var energy = tieredMachine.energyContainer;
                if (energy != null && energy.getInputVoltage() > 0) {
                    long demand = energy.getEnergyCanBeInserted();
                    if (demand > 0) {
                        long voltage = energy.getInputVoltage();
                        long transferLimit = voltage * energy.getInputAmperage();
                        long toInject = Math.min(demand, transferLimit);
                        long available = team.stored.min(BigInteger.valueOf(toInject)).longValue();

                        if (available > 0) {
                            BigInteger drained = team.drain(BigInteger.valueOf(available));
                            injectedThisTick = drained.longValue();
                            if (injectedThisTick > 0) {
                                energy.addEnergy(injectedThisTick);

                                if (targetLevel.getGameTime() % 10 == 0) {
                                    targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                            targetPos.getX() + 0.5, targetPos.getY() + 1.1, targetPos.getZ() + 0.5,
                                            5, 0.2, 0.2, 0.2, 0.05);
                                }
                            }
                        }
                    }
                }
            }

            if (injectedThisTick > 0) {
                team.machineCurrentFlow.merge(targetPos, injectedThisTick, Long::sum);
            }
        }
    }

    private void pullFromSoulLinkedGenerators(ServerLevel level, TeslaTeamEnergyData.TeamEnergy team) {
        if (team.soulLinkedMachines.isEmpty()) return;

        for (BlockPos targetPos : team.soulLinkedMachines) {

            ResourceKey<Level> dimKey = team.getMachineDimension(targetPos);
            ServerLevel targetLevel = level.getServer().getLevel(dimKey);

            if (targetLevel == null || !targetLevel.isLoaded(targetPos)) continue;

            team.markHatchActive(targetPos, targetLevel.getGameTime());

            MetaMachine machine = MetaMachine.getMachine(targetLevel, targetPos);
            if (machine == null) continue;

            long pulledThisTick = 0;

            if (machine instanceof TieredEnergyMachine generator) {
                var energy = generator.energyContainer;
                if (energy != null && energy.getEnergyStored() > 0) {
                    long voltage = energy.getOutputVoltage();
                    long maxAmperage = energy.getOutputAmperage();
                    long available = energy.getEnergyStored();
                    long maxTransfer = voltage * maxAmperage;
                    long toPull = Math.min(available, maxTransfer);

                    if (toPull > 0) {
                        BigInteger accepted = team.fill(BigInteger.valueOf(toPull));
                        long acceptedLong = accepted.longValue();
                        if (acceptedLong > 0) {
                            energy.removeEnergy(acceptedLong);
                            pulledThisTick = acceptedLong;
                        }
                    }
                }
            }

            if (pulledThisTick > 0) {
                team.machineCurrentFlow.merge(targetPos, -pulledThisTick, Long::sum);

                if (targetLevel.getGameTime() % 12 == 0) {
                    targetLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            targetPos.getX() + 0.5, targetPos.getY() + 1.2, targetPos.getZ() + 0.5,
                            3, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
    }

    protected void transferEnergyTick() {
        if (getLevel().isClientSide) return;
        if (!isWorkingEnabled() || !isFormed()) {
            if (recipeLogic.isActive()) recipeLogic.setStatus(RecipeLogic.Status.IDLE);
            return;
        }

        ServerLevel sl = (ServerLevel) getLevel();
        boolean isDoingWork = false;

        if (sl.getGameTime() % 20 == 0) {
            syncToTeslaSavedData();

            if (ownerTeamUUID != null) {
                TeslaTeamEnergyData data = TeslaTeamEnergyData.get(sl);
                TeslaTeamEnergyData.TeamEnergy team = data.getOrCreate(ownerTeamUUID);

                long totalWirelessInput = 0;
                long totalWirelessOutput = 0;

                for (var entry : team.energyOutput.entrySet()) {
                    totalWirelessInput += entry.getValue().longValue();
                    team.machineDisplayFlow.put(entry.getKey(), entry.getValue().longValue() / 20);
                }
                for (var entry : team.energyInput.entrySet()) {
                    totalWirelessOutput += entry.getValue().longValue();
                    team.machineDisplayFlow.put(entry.getKey(), -entry.getValue().longValue() / 20);
                }

                for (BlockPos mPos : new HashSet<>(team.soulLinkedMachines)) {
                    long accumulated = team.machineCurrentFlow.getOrDefault(mPos, 0L);

                    team.machineDisplayFlow.put(mPos, accumulated / 20);

                    if (accumulated < 0) {
                        totalWirelessInput += Math.abs(accumulated);
                    } else {
                        totalWirelessOutput += accumulated;
                    }

                    team.machineCurrentFlow.put(mPos, 0L);
                }

                team.lastNetInput = (netInLastSec + totalWirelessInput) / 20;
                team.lastNetOutput = (netOutLastSec + totalWirelessOutput) / 20;

                team.energyInput.clear();
                team.energyOutput.clear();
                data.setDirty();
            }

            inputPerSec = netInLastSec;
            outputPerSec = netOutLastSec;
            netInLastSec = 0;
            netOutLastSec = 0;
        }

        if (ownerTeamUUID != null) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(sl);
            TeslaTeamEnergyData.TeamEnergy team = data.getOrCreate(ownerTeamUUID);

            if (inputHatches != null) {
                long incoming = inputHatches.getEnergyStored();
                if (incoming > 0) {
                    BigInteger accepted = team.fill(BigInteger.valueOf(incoming));
                    if (accepted.signum() > 0) {
                        inputHatches.changeEnergy(-accepted.longValue());
                        netInLastSec += accepted.longValue();
                        isDoingWork = true;
                    }
                }
            }

            pullFromSoulLinkedGenerators(sl, team);

            if (!team.soulLinkedMachines.isEmpty() && team.stored.signum() > 0) {
                pushToSoulLinkedMachines(sl, team);

                isDoingWork = true;
            }

            data.setDirty();
        }

        recipeLogic.setStatus(isDoingWork ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
    }

    private static final Map<UUID, TeslaTowerMachine> TEAM_TOWER_MAP = new HashMap<>();

    public static void registerTower(TeslaTowerMachine tower) {
        if (tower.ownerTeamUUID != null) {
            TEAM_TOWER_MAP.put(tower.ownerTeamUUID, tower);
        }
    }

    public static void unregisterTower(TeslaTowerMachine tower) {
        if (tower.ownerTeamUUID != null) {
            TEAM_TOWER_MAP.remove(tower.ownerTeamUUID);
        }
    }

    public static TeslaTowerMachine getTowerByTeam(UUID team) {
        return TEAM_TOWER_MAP.get(team);
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        inputHatches = null;
        outputHatches = null;

        TeslaWirelessRegistry.unregisterTower(this);

        unregisterTower(this);

        netInLastSec = 0;
        inputPerSec = 0;
        netOutLastSec = 0;
        outputPerSec = 0;
        super.invalidateStructure(substructureName);
    }

    private static MutableComponent getTimeToFillDrainText(BigInteger timeToFillSeconds) {
        if (timeToFillSeconds.compareTo(BIG_INTEGER_MAX_LONG) > 0) {
            timeToFillSeconds = BIG_INTEGER_MAX_LONG;
        }

        Duration duration = Duration.ofSeconds(timeToFillSeconds.longValue());
        String key;
        long fillTime;
        if (duration.getSeconds() <= 180) {
            fillTime = duration.getSeconds();
            key = "gtceu.multiblock.power_substation.time_seconds";
        } else if (duration.toMinutes() <= 180) {
            fillTime = duration.toMinutes();
            key = "gtceu.multiblock.power_substation.time_minutes";
        } else if (duration.toHours() <= 72) {
            fillTime = duration.toHours();
            key = "gtceu.multiblock.power_substation.time_hours";
        } else if (duration.toDays() <= 730) {
            fillTime = duration.toDays();
            key = "gtceu.multiblock.power_substation.time_days";
        } else if (duration.toDays() / 365 < 1_000_000) {
            fillTime = duration.toDays() / 365;
            key = "gtceu.multiblock.power_substation.time_years";
        } else {
            return Component.translatable("gtceu.multiblock.power_substation.time_forever");
        }

        return Component.translatable(key, FormattingUtil.formatNumbers(fillTime));
    }

    public String getStored() {
        if (energyBank == null) {
            return "0";
        }
        return FormattingUtil.formatNumbers(energyBank.getStored());
    }

    public String getCapacity() {
        if (energyBank == null) {
            return "0";
        }
        return FormattingUtil.formatNumbers(energyBank.getCapacity());
    }

    @Override
    public EnergyInfo getEnergyInfo() {
        return new EnergyInfo(energyBank.getCapacity(), energyBank.getStored());
    }

    @Override
    public boolean supportsBigIntEnergyValues() {
        return true;
    }

    private BigInteger[] storage;
    private BigInteger[] maximums;
    @Setter
    private BigInteger capacity;
    private int index;

    @Override
    public void onLoad() {
        super.onLoad();
        
        updateBatteryTier();
    }

    private void updateBatteryTier() {
        int newTier = energyBank.getHighestTier();
        if (newTier != batteryTier) {
            batteryTier = newTier;
            markAsChanged();
        }
    }

    @SaveField
    private BlockPos boundTowerPos;

    @Nullable
    private TeslaTowerMachine getBoundTower() {
        if (boundTowerPos == null || !(getLevel() instanceof ServerLevel sl)) return null;

        if (getLevel().getBlockEntity(boundTowerPos) instanceof TeslaTowerMachine tower) {
            return tower;
        }

        return null;
    }

    private TickableSubscription energySyncSub;

    @Override
    public void onUnload() {
        super.onUnload();
        if (energySyncSub != null) {
            energySyncSub.unsubscribe();
            energySyncSub = null;
        }
    }

    public void bindToTower(TeslaTowerMachine tower) {
        if (tower == null) return;
        boundTowerPos = tower.self().getBlockPos();
        self().markAsChanged();
    }

    private void pushEnergyToSavedData() {
        if (this.getLevel() instanceof ServerLevel server && ownerTeamUUID != null) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(server);
            TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(ownerTeamUUID);

            teamData.stored = this.energyBank.getStored();
            teamData.capacity = this.energyBank.getCapacity();

            data.setDirty();
        }
    }

    private void syncToTeslaSavedData() {
        if (this.getLevel() instanceof ServerLevel serverLevel && ownerTeamUUID != null) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(serverLevel);
            TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(ownerTeamUUID);

            teamData.capacity = this.energyBank.getCapacity();

            this.energyBank.setStored(teamData.stored);

            data.setOnline(ownerTeamUUID, isWorkingEnabled() && isFormed());
            data.setDirty();
        }
    }

    public class TeslaEnergyBank extends MachineTrait {

        private static final MachineTraitType<TeslaEnergyBank> TRAIT_TYPE =
                new MachineTraitType<>(TeslaEnergyBank.class, true);

        @Override
        public MachineTraitType<?> getTraitType() {
            return TRAIT_TYPE;
        }

        @SaveField
        @SyncToClient
        private String[] storedStrings = new String[0];

        @SaveField
        @SyncToClient
        private String[] maxStrings = new String[0];

        private BigInteger[] storage = new BigInteger[0];
        private BigInteger[] maximums = new BigInteger[0];

        @Getter
        private BigInteger capacity = BigInteger.ZERO;
        private int index = 0;
        private List<ITeslaBattery> batteries = new ArrayList<>();

        public TeslaEnergyBank() {
            super();
        }

        public void initializeBatteries(List<ITeslaBattery> batteries) {
            this.batteries = new ArrayList<>(batteries);
            this.storage = new BigInteger[batteries.size()];
            this.maximums = new BigInteger[batteries.size()];
            this.storedStrings = new String[batteries.size()];
            this.maxStrings = new String[batteries.size()];
            this.capacity = BigInteger.ZERO;

            for (int i = 0; i < batteries.size(); i++) {
                this.maximums[i] = batteries.get(i).getCapacity();
                this.storage[i] = BigInteger.ZERO;
                this.storedStrings[i] = "0";
                this.maxStrings[i] = this.maximums[i].toString();
                this.capacity = this.capacity.add(this.maximums[i]);
            }
            updateNetworkArrays();
        }

        public TeslaEnergyBank rebuild(@NotNull List<ITeslaBattery> newBatteries) {
            TeslaEnergyBank newStorage = new TeslaEnergyBank();
            newStorage.initializeBatteries(newBatteries);

            for (BigInteger stored : this.storage) {
                newStorage.fill(stored);
            }
            return newStorage;
        }

        private void updateNetworkArrays() {
            if (storage == null) return;

            this.storedStrings = new String[storage.length];
            this.maxStrings = new String[maximums.length];

            for (int i = 0; i < storage.length; i++) {
                this.storedStrings[i] = storage[i] != null ? storage[i].toString() : "0";
                this.maxStrings[i] = maximums[i] != null ? maximums[i].toString() : "0";
            }

            if (this.getSyncDataHolder() != null) {
                this.getSyncDataHolder().markClientSyncFieldDirty("storedStrings");
                this.getSyncDataHolder().markClientSyncFieldDirty("maxStrings");
            }
        }

        public void setStored(BigInteger totalAmount) {
            if (totalAmount == null || storage == null || storage.length == 0) return;
            BigInteger remaining = totalAmount.max(BigInteger.ZERO).min(this.capacity);
            for (int i = 0; i < storage.length; i++) {
                BigInteger toPut = remaining.min(maximums[i]);
                storage[i] = toPut;
                remaining = remaining.subtract(toPut);
            }
            this.index = 0;
            while (index < storage.length - 1 && storage[index].equals(maximums[index])) {
                index++;
            }
            updateNetworkArrays();
        }

        public int getHighestTier() {
            if (batteries.isEmpty()) return 0;
            return batteries.stream().mapToInt(ITeslaBattery::getTier).max().orElse(0);
        }

        public long fill(long amount) {
            return fill(BigInteger.valueOf(amount)).longValue();
        }

        public BigInteger fill(BigInteger amount) {
            if (amount.signum() < 0 || storage.length == 0) return BigInteger.ZERO;

            if (index < storage.length && storage[index].equals(maximums[index])) {
                if (index < storage.length - 1) index++;
            }

            BigInteger space = maximums[index].subtract(storage[index]);
            BigInteger toFill = amount.min(space);

            if (toFill.equals(BigInteger.ZERO) && index == storage.length - 1) {
                return BigInteger.ZERO;
            }

            storage[index] = storage[index].add(toFill);
            BigInteger remaining = amount.subtract(toFill);

            updateNetworkArrays();

            if (remaining.signum() > 0 && index < storage.length - 1) {
                return toFill.add(fill(remaining));
            }

            return toFill;
        }

        public long drain(long amount) {
            return drain(BigInteger.valueOf(amount)).longValue();
        }

        public BigInteger drain(BigInteger amount) {
            if (amount.signum() < 0 || storage.length == 0) return BigInteger.ZERO;

            if (index >= 0 && storage[index].equals(BigInteger.ZERO)) {
                if (index > 0) index--;
            }

            BigInteger toDrain = storage[index].min(amount);

            if (toDrain.equals(BigInteger.ZERO) && index == 0) {
                return BigInteger.ZERO;
            }

            storage[index] = storage[index].subtract(toDrain);
            BigInteger remaining = amount.subtract(toDrain);

            updateNetworkArrays();

            if (remaining.signum() > 0 && index > 0) {
                return toDrain.add(drain(remaining));
            }

            return toDrain;
        }

        public BigInteger getStored() {
            BigInteger total = BigInteger.ZERO;
            for (BigInteger b : storage) {
                if (b != null) total = total.add(b);
            }
            return total;
        }

        public boolean hasEnergy() {
            return getStored().signum() > 0;
        }
    }

    @Getter
    public static class BatteryMatchWrapper {

        private final ITeslaBattery partType;
        private int amount;

        public BatteryMatchWrapper(ITeslaBattery partType) {
            this.partType = partType;
        }

        public TeslaTowerMachine.BatteryMatchWrapper increment() {
            amount++;
            return this;
        }
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack binder) {
        if (!binder.is(PhoenixItems.TESLA_BINDER.get())) return InteractionResult.PASS;

        var tag = binder.getTag();
        if (!getLevel().isClientSide && tag != null && tag.hasUUID("TargetTeam")) {

            this.ownerTeamUUID = tag.getUUID("TargetTeam");

            registerTower(this);

            if (isFormed()) {
                syncToTeslaSavedData();
                self().markAsChanged();
            }

            player.sendSystemMessage(
                    Component.literal("Tower frequency set to: " + ownerTeamUUID.toString().substring(0, 8))
                            .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(getLevel().isClientSide);
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack binder) {
        if (!binder.is(PhoenixItems.TESLA_BINDER.get())) return InteractionResult.PASS;

        if (!getLevel().isClientSide) {
            ensureOwnerTeamUUID();
            if (this.ownerTeamUUID != null) {
                var tag = binder.getOrCreateTag();
                tag.putUUID("TargetTeam", this.ownerTeamUUID);
                tag.putString("TeamName", player.getName().getString() + "'s Network");

                if (!tag.contains("OwnerName")) {
                    tag.putString("OwnerName", player.getName().getString());
                }

                syncToTeslaSavedData();
                player.sendSystemMessage(Component.literal("Tower frequency copied to Binder.")
                        .withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(getLevel().isClientSide);
    }

    @SaveField
    @SyncToClient
    private int batteryTier;
    @SaveField
    private UUID ownerTeamUUID;

    private void ensureOwnerTeamUUID() {
        if (!(getLevel() instanceof ServerLevel sl)) return;

        if (this.ownerTeamUUID != null) return;

        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null) {

            this.ownerTeamUUID = TeamUtils.getTeamIdOrPlayerFallback(ownerUUID);

            self().markAsChanged();

            if (TESLA_DEBUG) {
                PhoenixCore.LOGGER.info("Tesla Tower at {} auto-assigned to Team {}",
                        getBlockPos().toShortString(), ownerTeamUUID);
            }
        }
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);

        if (!isFormed()) {
            widgets.add(new TextWidget<>(Text.of(Component.literal("Tesla Network: Inactive").withStyle(ChatFormatting.RED))));
            return widgets;
        }

        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Tesla Network: ")
                .append(Component.literal(isWorkingEnabled() ? "ONLINE" : "OFFLINE")
                        .withStyle(isWorkingEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Team: ")
                .append(Component.literal(ownerTeamUUID == null ? "None" : TeamUtils.getTeamName(ownerTeamUUID))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))))));
        if (energyBank != null) {
            widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Stored: ")
                    .append(Component.literal(formatTeslaValue(FormattingUtil.formatNumbers(energyBank.getStored()), false) + " EU")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))))));
            widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Capacity: ")
                    .append(Component.literal(formatTeslaValue(FormattingUtil.formatNumbers(energyBank.getCapacity()), false) + " EU")
                            .withStyle(ChatFormatting.YELLOW)))));
        }
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            long inputVal = inputPerSec;
            return Component.literal("Total Input: ")
                    .append(Component.literal("+" + formatTeslaValue(FormattingUtil.formatNumbers(inputVal), false) + " EU/t")
                            .withStyle(ChatFormatting.GREEN));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            long outputVal = outputPerSec;
            return Component.literal("Total Output: ")
                    .append(Component.literal("-" + formatTeslaValue(FormattingUtil.formatNumbers(outputVal), false) + " EU/t")
                            .withStyle(ChatFormatting.RED));
        })));
        if (energyBank != null) {
            widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal("Battery Tier: ")
                    .append(Component.literal(GTValues.VN[energyBank.getHighestTier()])
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))))));
        }
        return widgets;
    }

    private String formatTeslaValue(String valueStr, boolean forceScientific) {
        if (valueStr == null || valueStr.isEmpty()) return "0";
        try {

            String cleanValue = valueStr.replaceAll("[§][0-9a-fk-or]", "")
                    .replace(",", "")
                    .replace("+", "")
                    .replace("-", "")
                    .replaceAll("[a-zA-Z]", "")
                    .trim();

            double value = Double.parseDouble(cleanValue);
            if (value == 0) return "0";

            if (forceScientific && value >= 1000) {
                return String.format("%.1e", value);
            }

            if (value >= 1_000_000_000_000L) {
                return String.format("%.3e", value);
            }

            return String.format("%,.0f", value);

        } catch (NumberFormatException ignored) {

            return valueStr.replaceAll("[§][0-9a-fk-or]", "");
        }
    }

}
