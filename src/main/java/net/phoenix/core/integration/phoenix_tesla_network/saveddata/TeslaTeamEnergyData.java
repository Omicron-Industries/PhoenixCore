package net.phoenix.core.integration.phoenix_tesla_network.saveddata;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

public class TeslaTeamEnergyData extends SavedData {

    private static final String DATA_NAME = "phoenix_tesla_team_energy";
    private final Map<UUID, TeamEnergy> networks = new HashMap<>();
    private final Set<UUID> onlineNetworks = new HashSet<>();

    public final Map<BlockPos, UUID> machineToTeam = new HashMap<>();

    public static class HatchInfo {

        public final BlockPos pos;
        public final ResourceKey<Level> dimension;
        public BigInteger buffered = BigInteger.ZERO;
        public long displayFlow = 0;
        public boolean isPhysicalOutput;
        public boolean isSoulLinked = false;

        public HatchInfo(BlockPos pos, ResourceKey<Level> dimension) {
            this.pos = pos;
            this.dimension = dimension;
        }
    }

    public Collection<HatchInfo> getHatches(UUID team) {
        TeamEnergy e = networks.get(team);
        if (e == null) return List.of();

        Map<BlockPos, HatchInfo> map = new HashMap<>();

        e.energyBuffered.forEach((pos, buf) -> {
            HatchInfo info = map.computeIfAbsent(pos, p -> new HatchInfo(p, e.getMachineDimension(p)));
            info.buffered = buf;
            info.isPhysicalOutput = e.hatchIsOutput.getOrDefault(pos, false);
            info.displayFlow = e.machineDisplayFlow.getOrDefault(pos, 0L);
        });

        e.soulLinkedMachines.forEach(pos -> {
            HatchInfo info = map.computeIfAbsent(pos, p -> new HatchInfo(p, e.getMachineDimension(p)));
            info.isSoulLinked = true;
            info.displayFlow = e.machineDisplayFlow.getOrDefault(pos, 0L);
        });

        return map.values();
    }

    public boolean isOnline(UUID team) {
        if (!networks.containsKey(team)) return true;
        return onlineNetworks.contains(team);
    }

    public Map<UUID, TeamEnergy> getNetworksView() {
        return Collections.unmodifiableMap(networks);
    }

    public void markHatchActive(UUID team, BlockPos pos, long time) {
        TeamEnergy e = networks.get(team);
        if (e != null) {
            e.markHatchActive(pos, time);
        }
    }

    public static class TeamEnergy {

        public BigInteger stored = BigInteger.ZERO;
        public BigInteger capacity = BigInteger.ZERO;

        public final Set<BlockPos> soulLinkedMachines = new HashSet<>();
        public final Set<BlockPos> activeChargers = new HashSet<>();
        public final Map<BlockPos, BigInteger> energyInput = new HashMap<>();
        public final Map<BlockPos, BigInteger> energyOutput = new HashMap<>();
        public final Map<BlockPos, BigInteger> energyBuffered = new HashMap<>();
        public final Map<BlockPos, Boolean> hatchIsOutput = new HashMap<>();
        public final Map<BlockPos, ResourceKey<Level>> posToDimension = new HashMap<>();

        public final Map<BlockPos, Long> lastSeen = new HashMap<>();
        public final Map<BlockPos, Long> machineDisplayFlow = new HashMap<>();
        public final Map<BlockPos, Long> machineCurrentFlow = new HashMap<>();
        public long lastNetInput = 0;
        public long lastNetOutput = 0;

        public void addCharger(BlockPos pos) {
            activeChargers.add(pos.immutable());
        }

        public void removeCharger(BlockPos pos) {
            activeChargers.remove(pos);
            machineCurrentFlow.remove(pos);
            machineDisplayFlow.remove(pos);
        }

        public long calculateTotalNetworkFlow() {
            long total = 0;

            for (Long flow : machineDisplayFlow.values()) {
                if (flow != null) {
                    total += Math.abs(flow);
                }
            }
            return total;
        }

        public int getLiveHatchCount(long gameTime) {
            int count = 0;
            for (var entry : lastSeen.entrySet()) {
                if (!energyBuffered.containsKey(entry.getKey())) continue;
                if (gameTime - entry.getValue() < 40) {
                    count++;
                }
            }
            return count;
        }

        public ResourceKey<Level> getMachineDimension(BlockPos pos) {
            return posToDimension.getOrDefault(pos, Level.OVERWORLD);
        }

        public BigInteger fill(BigInteger amount) {
            BigInteger space = capacity.subtract(stored).max(BigInteger.ZERO);
            BigInteger toAdd = amount.min(space);
            stored = stored.add(toAdd);
            return toAdd;
        }

        public BigInteger drain(BigInteger amount) {
            BigInteger toTake = amount.min(stored).max(BigInteger.ZERO);
            stored = stored.subtract(toTake);
            return toTake;
        }

        public void markHatchActive(BlockPos pos, long gameTime) {
            lastSeen.put(pos, gameTime);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Stored", stored.toString());
            tag.putString("Capacity", capacity.toString());

            ListTag soulList = new ListTag();
            soulLinkedMachines.forEach(p -> soulList.add(net.minecraft.nbt.LongTag.valueOf(p.asLong())));
            tag.put("SoulLinks", soulList);

            ListTag chargerList = new ListTag();
            activeChargers.forEach(p -> chargerList.add(net.minecraft.nbt.LongTag.valueOf(p.asLong())));
            tag.put("Chargers", chargerList);

            ListTag dimList = new ListTag();
            posToDimension.forEach((pos, dim) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("p", pos.asLong());
                entry.putString("d", dim.location().toString());
                dimList.add(entry);
            });
            tag.put("PosDims", dimList);

            return tag;
        }

        public static TeamEnergy load(CompoundTag tag) {
            TeamEnergy e = new TeamEnergy();
            e.stored = tag.contains("Stored") ? new BigInteger(tag.getString("Stored")) : BigInteger.ZERO;
            e.capacity = tag.contains("Capacity") ? new BigInteger(tag.getString("Capacity")) : BigInteger.ZERO;

            ListTag soulList = tag.getList("SoulLinks", Tag.TAG_LONG);
            for (Tag value : soulList) {
                e.soulLinkedMachines.add(BlockPos.of(((net.minecraft.nbt.LongTag) value).getAsLong()));
            }

            ListTag chargerList = tag.getList("Chargers", Tag.TAG_LONG);
            for (Tag value : chargerList) {
                e.activeChargers.add(BlockPos.of(((net.minecraft.nbt.LongTag) value).getAsLong()));
            }

            if (tag.contains("PosDims")) {
                ListTag dimList = tag.getList("PosDims", Tag.TAG_COMPOUND);
                for (int i = 0; i < dimList.size(); i++) {
                    CompoundTag entry = dimList.getCompound(i);
                    BlockPos pos = BlockPos.of(entry.getLong("p"));
                    ResourceLocation dimLoc = new ResourceLocation(entry.getString("d"));
                    e.posToDimension.put(pos, ResourceKey.create(Registries.DIMENSION, dimLoc));
                }
            }
            return e;
        }
    }

    public boolean toggleSoulLink(UUID team, Level level, BlockPos pos) {
        TeamEnergy e = getOrCreate(team);
        BlockPos p = pos.immutable();
        boolean removed = e.soulLinkedMachines.remove(p);

        if (!removed) {
            e.soulLinkedMachines.add(p);
            machineToTeam.put(p, team);
            e.posToDimension.put(p, level.dimension());
        } else {
            machineToTeam.remove(p);
            e.posToDimension.remove(p);
        }
        setDirty();
        return !removed;
    }

    public void setEnergyBuffered(UUID team, Level level, BlockPos pos, BigInteger amount, boolean isOutput) {
        TeamEnergy e = getOrCreate(team);
        BlockPos p = pos.immutable();
        e.energyBuffered.put(p, amount);
        e.hatchIsOutput.put(p, isOutput);
        e.posToDimension.put(p, level.dimension());
        machineToTeam.put(p, team);
        setDirty();
    }

    public void removeEndpoint(UUID team, BlockPos pos) {
        TeamEnergy e = networks.get(team);
        if (e != null) {
            e.soulLinkedMachines.remove(pos);
            e.activeChargers.remove(pos);
            e.energyInput.remove(pos);
            e.energyOutput.remove(pos);
            e.energyBuffered.remove(pos);
            e.hatchIsOutput.remove(pos);
            e.posToDimension.remove(pos);
            e.lastSeen.remove(pos);
            machineToTeam.remove(pos);
            setDirty();
        }
    }

    public void removeMachineFromAllTeams(BlockPos pos) {
        UUID team = machineToTeam.remove(pos);
        if (team != null) removeEndpoint(team, pos);
    }

    @Nullable
    public UUID getOwnerTeam(BlockPos pos) {
        return machineToTeam.get(pos);
    }

    public TeamEnergy getOrCreate(UUID team) {
        return networks.computeIfAbsent(team, k -> {
            TeamEnergy e = new TeamEnergy();
            onlineNetworks.add(k);
            return e;
        });
    }

    public void setOnline(UUID team, boolean online) {
        if (online) onlineNetworks.add(team);
        else onlineNetworks.remove(team);
        setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag netList = new ListTag();
        networks.forEach((uuid, energy) -> {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", uuid);
            teamTag.put("Energy", energy.save());
            teamTag.putBoolean("Online", onlineNetworks.contains(uuid));
            netList.add(teamTag);
        });
        tag.put("Networks", netList);

        ListTag lookupList = new ListTag();
        machineToTeam.forEach((pos, uuid) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("p", pos.asLong());
            entry.putUUID("t", uuid);
            lookupList.add(entry);
        });
        tag.put("GlobalLookup", lookupList);

        return tag;
    }

    public static TeslaTeamEnergyData load(CompoundTag tag) {
        TeslaTeamEnergyData data = new TeslaTeamEnergyData();
        ListTag netList = tag.getList("Networks", Tag.TAG_COMPOUND);

        for (int i = 0; i < netList.size(); i++) {
            CompoundTag teamTag = netList.getCompound(i);
            UUID teamUUID = teamTag.getUUID("Team");
            TeamEnergy teamData = TeamEnergy.load(teamTag.getCompound("Energy"));

            data.networks.put(teamUUID, teamData);
            if (teamTag.getBoolean("Online")) data.onlineNetworks.add(teamUUID);

            for (BlockPos p : teamData.soulLinkedMachines) data.machineToTeam.put(p, teamUUID);
            for (BlockPos p : teamData.activeChargers) data.machineToTeam.put(p, teamUUID);
            for (BlockPos p : teamData.energyBuffered.keySet()) data.machineToTeam.put(p, teamUUID);

            for (BlockPos p : teamData.soulLinkedMachines) {
                teamData.machineCurrentFlow.put(p, 0L);
                teamData.machineDisplayFlow.put(p, 0L);
            }
            for (BlockPos p : teamData.activeChargers) {
                teamData.machineCurrentFlow.put(p, 0L);
                teamData.machineDisplayFlow.put(p, 0L);
            }
        }

        if (tag.contains("GlobalLookup")) {
            ListTag lookupList = tag.getList("GlobalLookup", Tag.TAG_COMPOUND);
            for (int i = 0; i < lookupList.size(); i++) {
                CompoundTag entry = lookupList.getCompound(i);
                BlockPos pos = BlockPos.of(entry.getLong("p"));
                UUID uuid = entry.getUUID("t");
                data.machineToTeam.putIfAbsent(pos, uuid);
            }
        }

        return data;
    }

    public static TeslaTeamEnergyData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return (overworld == null ? level : overworld).getDataStorage().computeIfAbsent(
                TeslaTeamEnergyData::load, TeslaTeamEnergyData::new, DATA_NAME);
    }
}
