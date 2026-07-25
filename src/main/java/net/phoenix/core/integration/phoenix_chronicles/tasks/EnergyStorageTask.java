package net.phoenix.core.integration.phoenix_chronicles.tasks;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyStorageTask extends QuestTask {

    public enum EnergyType {
        FE,
        EU,
        ANY
    }

    public enum Source {
        INVENTORY,
        HELD,
        BLOCK
    }

    private static final Map<UUID, long[]> blockCache = new HashMap<>();

    public static void onBlockRightClicked(Player player, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        long fe = 0L, eu = 0L;

        IEnergyStorage feCap = be.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (feCap != null) fe = feCap.getEnergyStored();

        IEnergyContainer euCap = GTCapabilityHelper.getEnergyContainer(level, pos, null);
        if (euCap != null) eu = euCap.getEnergyStored();

        if (fe > 0 || eu > 0) {
            blockCache.put(player.getUUID(), new long[] { fe, eu });
        }
    }

    public static void clearBlockCache(UUID playerId) {
        blockCache.remove(playerId);
    }

    private long requiredEnergy;
    private EnergyType energyType;
    private Source source;

    public EnergyStorageTask(ResourceLocation taskId, Component description,
                             long requiredEnergy, EnergyType energyType, Source source) {
        super(taskId, description);
        this.requiredEnergy = Math.max(1, requiredEnergy);
        this.energyType = energyType != null ? energyType : EnergyType.FE;
        this.source = source != null ? source : Source.INVENTORY;
    }

    public long getRequiredEnergy() {
        return requiredEnergy;
    }

    public EnergyType getEnergyType() {
        return energyType;
    }

    public Source getSource() {
        return source;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return getStored(player) >= requiredEnergy;
    }

    @Override
    public String getProgressString(Player player) {
        long stored = Math.min(getStored(player), requiredEnergy);
        String unit = (energyType == EnergyType.EU) ? "EU" : "FE";
        return format(stored, unit) + " / " + format(requiredEnergy, unit);
    }

    private long getStored(Player player) {
        return switch (source) {
            case INVENTORY -> inventoryFE(player);
            case HELD -> heldFE(player);
            case BLOCK -> blockStored(player);
        };
    }

    private long inventoryFE(Player player) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            total += stack.getCapability(ForgeCapabilities.ENERGY)
                    .map(IEnergyStorage::getEnergyStored).orElse(0);
        }
        return total;
    }

    private long heldFE(Player player) {
        return player.getMainHandItem()
                .getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::getEnergyStored).orElse(0);
    }

    private long blockStored(Player player) {
        long[] cache = blockCache.get(player.getUUID());
        if (cache == null) return 0L;
        return switch (energyType) {
            case FE -> cache[0];
            case EU -> cache[1];
            case ANY -> cache[0] > 0 ? cache[0] : cache[1];
        };
    }

    public String getSourceHint(Player player) {
        return switch (source) {
            case INVENTORY -> "in inventory";
            case HELD -> "in held item";
            case BLOCK -> {
                long[] cache = blockCache.get(player.getUUID());
                if (cache == null) yield "§8Right-click an energy block to link";
                String unit = energyType == EnergyType.EU ? "EU" : "FE";
                long val = blockStored(player);
                yield "in linked block  §8(currently " + format(val, unit) + ")";
            }
        };
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "energy_check");
        tag.putLong("required_energy", requiredEnergy);
        tag.putString("energy_type", energyType.name());
        tag.putString("source", source.name());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.requiredEnergy = nbt.contains("required_energy") ? nbt.getLong("required_energy") :
                nbt.contains("required_fe") ? nbt.getLong("required_fe") : nbt.getLong("amount");

        if (nbt.contains("energy_type")) {
            try {
                this.energyType = EnergyType.valueOf(nbt.getString("energy_type").toUpperCase());
            } catch (Exception ignored) {
                this.energyType = EnergyType.FE;
            }
        } else if (nbt.contains("mode")) {

            this.energyType = EnergyType.FE;
        }

        if (nbt.contains("source")) {
            try {
                this.source = Source.valueOf(nbt.getString("source").toUpperCase());
            } catch (Exception ignored) {
                this.source = Source.INVENTORY;
            }
        } else if (nbt.contains("mode")) {

            String mode = nbt.getString("mode").toUpperCase();
            this.source = mode.equals("HELD") ? Source.HELD : Source.INVENTORY;
        }
    }

    public static String format(long energy, String unit) {
        if (energy >= 1_000_000_000L) return String.format("%.1fG%s", energy / 1_000_000_000.0, unit);
        if (energy >= 1_000_000L) return String.format("%.1fM%s", energy / 1_000_000.0, unit);
        if (energy >= 1_000L) return String.format("%.1fk%s", energy / 1_000.0, unit);
        return energy + " " + unit;
    }
}
