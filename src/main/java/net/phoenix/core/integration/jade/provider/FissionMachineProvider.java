package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.BreederWorkableElectricMultiblockMachine;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class FissionMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("fission_machine_info");

    private static final String NBT_HEAT = "pf_heat";
    private static final String NBT_NET_HEAT = "pf_net_heat";
    private static final String NBT_MELTDOWN_SECONDS = "pf_meltdown_seconds";
    private static final String NBT_HAS_COOLANT = "pf_has_coolant";
    private static final String NBT_IS_SCRAMMED = "pf_is_scrammed";
    private static final String NBT_IS_BREEDER = "pf_is_breeder";
    private static final String NBT_BREEDING_PRODUCT = "pf_breeding_product";
    private static final String NBT_RUNNING = "pf_running";
    private static final String NBT_PARALLELS = "pf_parallels";
    private static final String NBT_EUT = "pf_eut";
    private static final String NBT_RODS = "pf_rods";
    private static final String NBT_COOLERS = "pf_coolers";
    private static final String NBT_MODS = "pf_mods";
    private static final String NBT_BLANKETS = "pf_blankets";
    private static final String NBT_COOLING_POWER = "pf_cooling_power";
    private static final String NBT_GATE_FAIL = "pf_gate_fail";
    private static final String NBT_BLANKET_INPUT = "pf_blanket_input";
    private static final String NBT_BLANKET_OUTPUT = "pf_blanket_output";
    private static final String NBT_BLANKET_OUTPUTS = "pf_blanket_outputs";
    private static final String NBT_BLANKET_AMOUNT = "pf_blanket_amount";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(UID)) return;

        if (!(accessor.getBlockEntity() instanceof MetaMachineBlockEntity metaBE &&
                metaBE.getMetaMachine() instanceof FissionWorkableElectricMultiblockMachine)) {
            return;
        }


        CompoundTag data = accessor.getServerData();

        double heat = data.getDouble(NBT_HEAT);
        double netHeat = data.getDouble(NBT_NET_HEAT);
        boolean isRunning = data.getBoolean(NBT_RUNNING);
        boolean isScrammed = data.getBoolean(NBT_IS_SCRAMMED);

        // Status logic
        if (isScrammed) {
            tooltip.add(Component.literal("SCRAMMED")
                    .withStyle(s -> s.withColor(0xFF0000).withBold(true)));
        } else if (!isRunning && heat > 0) {
            tooltip.add(Component.literal("Cooling Down")
                    .withStyle(s -> s.withColor(0x55FFFF).withItalic(true)));
        }

        // Meltdown Timer
        int meltdownSeconds = data.getInt(NBT_MELTDOWN_SECONDS);
        if (meltdownSeconds > 0) {
            tooltip.add(Component.translatable("jade.phoenixcore.fission_meltdown_timer", meltdownSeconds)
                    .withStyle(s -> s.withColor(0xFFAA00)));
            // Note: Using a literal here since we are showing raw heat value
            tooltip.add(Component.literal("Current Heat: " + (long)heat));
        } else {
            // Only show "Safe" if it's actually not scrammed or cooling
            if (heat <= 0) {
                tooltip.add(Component.translatable("jade.phoenixcore.fission_safe")
                        .withStyle(s -> s.withColor(0x33FF33)));
            }
        }

        if (data == null || data.isEmpty()) return;


        if (!data.getBoolean(NBT_HAS_COOLANT)) {
            tooltip.add(Component.translatable("jade.phoenixcore.fission_no_coolant")
                    .withStyle(s -> s.withColor(0xFF3333)));
        }

        if (netHeat > 0.0001) {
            tooltip.add(Component.translatable("jade.phoenixcore.fission_heating")
                    .withStyle(s -> s.withColor(0xFF5555)));
        }

        int parallels = data.getInt(NBT_PARALLELS);
        long eut = data.getLong(NBT_EUT);

        tooltip.add(Component.literal("Parallels: " + parallels));
        tooltip.add(Component.literal("EU/t: " + eut));

        int rods = data.getInt(NBT_RODS);
        int coolers = data.getInt(NBT_COOLERS);
        int mods = data.getInt(NBT_MODS);

        tooltip.add(Component.literal("Rods: " + rods + "  Mods: " + mods + "  Coolers: " + coolers));

        int coolingPower = data.getInt(NBT_COOLING_POWER);
        tooltip.add(Component.literal("Max Cooling Power: " + coolingPower + " HU/t")
                .withStyle(s -> s.withColor(0x55FFFF)));

        if (data.getBoolean(NBT_IS_BREEDER) && data.getInt(NBT_BLANKETS) > 0 && data.contains(NBT_BLANKET_INPUT)) {
            String inKey = data.getString(NBT_BLANKET_INPUT);
            Component inName = resolveKeyToDisplayName(inKey);

            tooltip.add(Component.translatable("jade.phoenixcore.blanket_input", inName)
                    .withStyle(s -> s.withColor(0xAAAAFF)));

            if (data.contains(NBT_BLANKET_OUTPUT)) {
                String outKey = data.getString(NBT_BLANKET_OUTPUT);
                if (!outKey.isEmpty()) {
                    Component outName = resolveKeyToDisplayName(outKey);
                    tooltip.add(Component.translatable("jade.phoenixcore.blanket_output", outName)
                            .withStyle(s -> s.withColor(0xFFBBFF)));
                }
            }

            if (data.contains(NBT_BLANKET_OUTPUTS, Tag.TAG_LIST)) {
                ListTag list = data.getList(NBT_BLANKET_OUTPUTS, Tag.TAG_STRING);
                if (!list.isEmpty()) {
                    tooltip.add(Component.translatable("phoenixcore.blanket_outputs")
                            .withStyle(s -> s.withColor(0xFFDD88)));

                    int shown = 0;
                    for (int i = 0; i < list.size(); i++) {
                        if (shown++ >= 5) break;

                        String entry = list.getString(i);
                        String[] parts = entry.split("\\|");
                        String key = parts.length > 0 ? parts[0] : entry;
                        String w = parts.length > 1 ? parts[1] : "?";
                        String inst = parts.length > 2 ? parts[2] : "?";

                        Component outName = resolveKeyToDisplayName(key);
                        tooltip.add(Component.literal("• ")
                                .append(outName)
                                .append(Component.literal("  w=" + w + "  inst=" + inst)
                                        .withStyle(s -> s.withColor(0x888888)))
                                .withStyle(s -> s.withColor(0xCCCCCC)));
                    }
                }
            }

            if (data.contains(NBT_BLANKET_AMOUNT)) {
                int amt = data.getInt(NBT_BLANKET_AMOUNT);
                tooltip.add(Component.translatable("jade.phoenixcore.blanket_amount", amt)
                        .withStyle(s -> s.withColor(0xBBBBBB)));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MetaMachineBlockEntity metaBE &&
                metaBE.getMetaMachine() instanceof FissionWorkableElectricMultiblockMachine machine)) {
            return;
        }

        tag.putInt(NBT_RODS, machine.getActiveFuelRods().size());
        tag.putInt(NBT_COOLERS, machine.getActiveCoolers().size());
        tag.putInt(NBT_MODS, machine.getActiveModerators().size());
        tag.putInt(NBT_BLANKETS, machine.getActiveBlankets().size());

        tag.putBoolean(NBT_IS_SCRAMMED, machine.isScramActive()); // Add this line
        tag.putBoolean(NBT_RUNNING, machine.wasRunningLastTick());

        tag.putDouble(NBT_HEAT, machine.getHeat());
        tag.putDouble(NBT_NET_HEAT, machine.getNetHeatPerTick());

        tag.putBoolean(NBT_HAS_COOLANT, machine.lastHasCoolant);
        tag.putBoolean(NBT_RUNNING, machine.wasRunningLastTick());

        tag.putInt(NBT_PARALLELS, machine.lastParallels);
        tag.putLong(NBT_EUT, machine.lastGeneratedEUt);

        tag.putInt(NBT_COOLING_POWER, machine.lastProvidedCooling);

        tag.putInt(NBT_MELTDOWN_SECONDS, machine.getMeltdownSecondsRemaining());

        boolean breeder = machine instanceof BreederWorkableElectricMultiblockMachine;
        tag.putBoolean(NBT_IS_BREEDER, breeder);

        if (breeder) {
            BreederWorkableElectricMultiblockMachine b = (BreederWorkableElectricMultiblockMachine) machine;

            boolean hasBlankets = b.getActiveBlankets() != null && !b.getActiveBlankets().isEmpty() &&
                    b.getPrimaryBlanket() != null;

            if (hasBlankets) {
                IFissionBlanketType primary = b.getPrimaryBlanket();

                tag.putInt(NBT_BLANKETS, b.getActiveBlankets().size());
                tag.putString(NBT_BLANKET_INPUT, primary.getInputKey());
                tag.putInt(NBT_BLANKET_AMOUNT, Math.max(0, primary.getAmountPerCycle()));

                String primaryOut = "";
                if (primary.getOutputs() != null && !primary.getOutputs().isEmpty() &&
                        primary.getOutputs().get(0) != null) {
                    primaryOut = primary.getOutputs().get(0).key();
                }
                tag.putString(NBT_BLANKET_OUTPUT, primaryOut);

                ListTag outs = new ListTag();
                if (primary.getOutputs() != null) {
                    for (var o : primary.getOutputs()) {
                        if (o == null) continue;

                        outs.add(StringTag.valueOf(o.key() + "|" + o.weight() + "|" + o.instability()));
                    }
                }
                tag.put(NBT_BLANKET_OUTPUTS, outs);

            } else {
                tag.remove(NBT_BLANKET_INPUT);
                tag.remove(NBT_BLANKET_OUTPUT);
                tag.remove(NBT_BLANKET_OUTPUTS);
                tag.remove(NBT_BLANKET_AMOUNT);
                tag.putInt(NBT_BLANKETS, 0);
            }
        } else {
            tag.putInt(NBT_BLANKETS, 0);
            tag.remove(NBT_BLANKET_INPUT);
            tag.remove(NBT_BLANKET_OUTPUT);
            tag.remove(NBT_BLANKET_OUTPUTS);
            tag.remove(NBT_BLANKET_AMOUNT);
        }
    }

    private static Component resolveKeyToDisplayName(String key) {
        if (key == null || key.isEmpty() || "none".equalsIgnoreCase(key)) {
            return Component.literal("None");
        }

        Material mat = GTMaterials.get(key);
        if (mat != null && mat != GTMaterials.NULL) {
            try {
                String transKey = mat.getDefaultTranslation();
                if (transKey != null && !transKey.isEmpty()) {
                    return Component.translatable(transKey);
                }
            } catch (Throwable ignored) {}
            return Component.literal(key);
        }

        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl != null) {
            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item, 1).getHoverName();
            }

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
            if (fluid != null && fluid != Fluids.EMPTY) {
                return Component.translatable(fluid.getFluidType().getDescriptionId());
            }
        }

        return Component.literal(key);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
