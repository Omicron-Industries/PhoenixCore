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

/**
 * Checks that a player has sufficient energy stored, supporting both Forge Energy (FE/RF)
 * and GregTech EU as fully separate unit systems.
 *
 * Sources:
 * INVENTORY – sum FE across all IEnergyStorage items in player inventory (FE only)
 * HELD – FE in the currently-held item only (FE only)
 * BLOCK – energy stored in the last right-clicked block entity; reads BOTH FE and GTM EU,
 * matched against whichever energy type this task requires.
 * Populated once per right-click via {@link #onBlockRightClicked} — no per-tick polling.
 *
 * For BLOCK source the quest UI shows the cached reading from the most recent interaction.
 * Players just right-click their battery box / capacitor bank / energy hatch and then open the quest.
 */
public class EnergyStorageTask extends QuestTask {

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum EnergyType {
        FE,   // Forge Energy / RF
        EU,   // GregTech EU
        ANY   // whichever is non-zero (FE checked first)
    }

    public enum Source {
        INVENTORY,   // sum FE in all player inventory items
        HELD,        // FE in mainhand item only
        BLOCK        // cached from last right-clicked energy block
    }

    // ── Per-session block energy cache ────────────────────────────────────────
    // Key: player UUID → long[2] { fe_stored, eu_stored }
    // Populated by onBlockRightClicked(), never polled per-tick.
    private static final Map<UUID, long[]> blockCache = new HashMap<>();

    public static void onBlockRightClicked(Player player, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        long fe = 0L, eu = 0L;

        // Forge Energy
        IEnergyStorage feCap = be.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (feCap != null) fe = feCap.getEnergyStored();

        // GregTech EU (all faces — null = default direction)
        IEnergyContainer euCap = GTCapabilityHelper.getEnergyContainer(level, pos, null);
        if (euCap != null) eu = euCap.getEnergyStored();

        if (fe > 0 || eu > 0) {
            blockCache.put(player.getUUID(), new long[] { fe, eu });
        }
    }

    /** Call on player disconnect / world unload to avoid stale data. */
    public static void clearBlockCache(UUID playerId) {
        blockCache.remove(playerId);
    }

    // ── Task state ────────────────────────────────────────────────────────────

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

    // ── Logic ─────────────────────────────────────────────────────────────────

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

    /**
     * Returns a user-facing description of what the task is checking,
     * shown in the hover tooltip on the main quest screen.
     */
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

    // ── Serialization ─────────────────────────────────────────────────────────

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
                nbt.contains("required_fe") ? nbt.getLong("required_fe")   // legacy
                        : nbt.getLong("amount");

        if (nbt.contains("energy_type")) {
            try {
                this.energyType = EnergyType.valueOf(nbt.getString("energy_type").toUpperCase());
            } catch (Exception ignored) {
                this.energyType = EnergyType.FE;
            }
        } else if (nbt.contains("mode")) {
            // Legacy migration: old "INVENTORY"/"HELD" modes mapped to source, not type
            this.energyType = EnergyType.FE;
        }

        if (nbt.contains("source")) {
            try {
                this.source = Source.valueOf(nbt.getString("source").toUpperCase());
            } catch (Exception ignored) {
                this.source = Source.INVENTORY;
            }
        } else if (nbt.contains("mode")) {
            // Legacy migration from old single-enum approach
            String mode = nbt.getString("mode").toUpperCase();
            this.source = mode.equals("HELD") ? Source.HELD : Source.INVENTORY;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static String format(long energy, String unit) {
        if (energy >= 1_000_000_000L) return String.format("%.1fG%s", energy / 1_000_000_000.0, unit);
        if (energy >= 1_000_000L) return String.format("%.1fM%s", energy / 1_000_000.0, unit);
        if (energy >= 1_000L) return String.format("%.1fk%s", energy / 1_000.0, unit);
        return energy + " " + unit;
    }
}
