package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.block.LampBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phantasia.PhantasiaLoadedPattern;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionCoolerBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;
import net.phoenix.core.integration.phoenix_tesla_network.common.block.TeslaBatteryBlock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class PhantasiaBlockInspectScreen extends Screen {

    private static final int C_BG = 0xFF020205; // Solid Black
    private static final int C_ACCENT = 0xFF4FC3F7; // Phantasia Cyan
    private static final int C_TEXT = 0xFFDDDDDD;
    private static final int C_DIM = 0xFF778899;

    private final BlockPos pos;
    private final PhantasiaLoadedPattern pattern;
    private final Screen parent;
    private final BlockState state;
    private final List<Component> infoLines = new ArrayList<>();
    private String machineRole = "Standard Component";

    public PhantasiaBlockInspectScreen(BlockPos pos, PhantasiaLoadedPattern pattern, Screen parent) {
        super(Component.literal("Block Inspector"));
        this.pos = pos;
        this.pattern = pattern;
        this.parent = parent;
        this.state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(pos);

        collectData();
    }

    private void collectData() {
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        if (!stack.isEmpty()) {
            // Force tooltips to always show "Shift" info automatically in the inspector
            infoLines.addAll(stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL));
        }

        var block = state.getBlock();

        // 1. Controller Verification
        if (pattern.controllerWorldPos != null && pos.equals(pattern.controllerWorldPos)) {
            machineRole = "MULTIBLOCK CONTROLLER";
        }
        // 2. Specialized Coils (using your CoilBlock code)
        else if (block instanceof CoilBlock coilBlock) {
            ICoilType type = coilBlock.coilType;
            machineRole = type.getName().toUpperCase() + " HEATING COIL";

            // Add technical specs directly to the info lines
            infoLines.add(Component.empty());
            infoLines.add(Component.literal("TECHNICAL SPECIFICATIONS:").withStyle(ChatFormatting.AQUA));
            infoLines.add(Component.literal(" - Max Heat: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getCoilTemperature() + "K").withStyle(ChatFormatting.GOLD)));
            infoLines.add(Component.literal(" - Material: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getMaterial().getName()).withStyle(ChatFormatting.WHITE)));
            infoLines.add(Component.literal(" - Energy Discount: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getEnergyDiscount() + "x").withStyle(ChatFormatting.GREEN)));
        }
        // 3. Fission Components (Phoenix)
        if (block instanceof FissionFuelRodBlock rodBlock) {
            var type = rodBlock.getFuelRodType();
            machineRole = "FISSION FUEL ROD [T" + type.getTier() + "]";

            infoLines.add(Component.empty());
            infoLines.add(Component.literal("CORE ANALYSIS:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            infoLines.add(Component.literal(" • Base Heat: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getBaseHeatProduction() + " HU/t").withStyle(ChatFormatting.RED)));
            infoLines.add(Component.literal(" • Neutron Bias: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal((type.getNeutronBias() >= 0 ? "+" : "") + type.getNeutronBias() + "%")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
            infoLines.add(Component.literal(" • Cycle: ").withStyle(ChatFormatting.GRAY)
                    .append(Component
                            .literal(type.getAmountPerCycle() + " pellets / " + (type.getDurationTicks() / 20.0) + "s")
                            .withStyle(ChatFormatting.WHITE)));
        }

        // 2. COOLERS
        else if (block instanceof FissionCoolerBlock coolerBlock) {
            var type = coolerBlock.getCoolerType();
            machineRole = "THERMAL COOLANT MODULE";

            infoLines.add(Component.empty());
            infoLines.add(Component.literal("COOLING DATA:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            infoLines.add(Component.literal(" • Cooling Power: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("-" + type.getCoolerTemperature() + " HU/t")
                            .withStyle(ChatFormatting.BLUE)));
            infoLines.add(Component.literal(" • Consumption: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getCoolantUsagePerTick() + " mB/t")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        // 3. MODERATORS
        else if (block instanceof FissionModeratorBlock moderatorBlock) {
            var type = moderatorBlock.getModeratorType();
            machineRole = "NEUTRON MODERATOR";

            infoLines.add(Component.empty());
            infoLines.add(Component.literal("MODERATION STATS:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            infoLines.add(Component.literal(" • Energy Boost: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("+" + type.getEUBoost() + "%").withStyle(ChatFormatting.GREEN)));
            infoLines.add(Component.literal(" • Fuel Discount: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type.getFuelDiscount() + "%").withStyle(ChatFormatting.YELLOW)));
        }

        // 4. BREEDER BLANKETS
        else if (block instanceof FissionBlanketBlock blanketBlock) {
            var type = blanketBlock.getBlanketType();
            machineRole = "BREEDER BLANKET";

            infoLines.add(Component.empty());
            infoLines
                    .add(Component.literal("TRANSFORMATION DATA:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            infoLines.add(Component.literal(" • Target Cycle: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal((type.getDurationTicks() / 20.0) + "s").withStyle(ChatFormatting.GOLD)));

            // List Top 2 Potential Outputs
            var outs = type.getOutputs();
            if (!outs.isEmpty()) {
                infoLines.add(Component.literal(" • Primary Byproducts:").withStyle(ChatFormatting.GRAY));
                for (int i = 0; i < Math.min(outs.size(), 2); i++) {
                    var out = outs.get(i);
                    infoLines.add(Component.literal("   - ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(FissionFuelRodBlock.getRegistryDisplayName(out.key()).copy()
                                    .withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" (W:" + out.weight() + ")")
                                    .withStyle(ChatFormatting.DARK_AQUA)));
                }
            }
        }
        // 4. Tesla Network
        else if (block instanceof TeslaBatteryBlock) {
            machineRole = "TESLA ENERGY STORAGE";
        }
        // 5. GregTech Lamps
        else if (block instanceof LampBlock) {
            machineRole = "INDICATOR LAMP";
        }
        // 6. GT Machine Parts (Buses/Hatches)
        else if (block instanceof MetaMachineBlock) {
            try {
                for (Field field : PartAbility.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == PartAbility.class) {
                        PartAbility ability = (PartAbility) field.get(null);
                        if (ability != null && ability.isApplicable(block)) {
                            machineRole = ability.getName().toUpperCase().replace("_", " ");
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Full black background
        g.fill(0, 0, this.width, this.height, C_BG);

        // Header Decoration
        g.fill(20, 35, this.width - 20, 36, C_ACCENT);
        g.drawString(font, "TECHNICAL BLOCK INSPECTOR", 25, 22, C_ACCENT, false);

        int leftCol = 30;
        int leftColWidth = 120;
        int rightCol = 170;
        int maxRightWidth = this.width - rightCol - 40;
        int y = 55;

        // --- LEFT COLUMN: IDENTITY ---

        // Block Icon
        g.pose().pushPose();
        g.pose().translate(leftCol, y, 100);
        g.pose().scale(4.0f, 4.0f, 1.0f);
        g.renderFakeItem(new ItemStack(state.getBlock()), 0, 0);
        g.pose().popPose();

        y += 75;

        // Wrapped Block Name
        String name = state.getBlock().getName().getString();
        for (FormattedCharSequence seq : font.split(Component.literal(name), leftColWidth)) {
            g.drawString(font, seq, leftCol, y, 0xFFFFFFFF, false);
            y += 10;
        }

        y += 4;

        // Wrapped Registry ID (Fixes bleeding)
        String id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        for (FormattedCharSequence seq : font.split(Component.literal(id).withStyle(ChatFormatting.ITALIC),
                leftColWidth)) {
            g.drawString(font, seq, leftCol, y, C_DIM, false);
            y += 10;
        }

        y += 20;
        g.drawString(font, "DESIGNATION", leftCol, y, C_ACCENT, false);
        y += 12;
        // Wrapped Role
        for (FormattedCharSequence seq : font.split(Component.literal(machineRole), leftColWidth)) {
            g.drawString(font, seq, leftCol, y, 0xFFFFB74D, false);
            y += 10;
        }

        y += 20;
        BlockPos lp = pattern.toLocal(pos);
        if (lp != null) {
            g.drawString(font, "LOCAL POS", leftCol, y, C_ACCENT, false);
            y += 12;
            g.drawString(font, "X: " + lp.getX(), leftCol, y, C_TEXT, false);
            g.drawString(font, "Y: " + lp.getY(), leftCol + 40, y, C_TEXT, false);
            g.drawString(font, "Z: " + lp.getZ(), leftCol + 80, y, C_TEXT, false);
        }

        // --- RIGHT COLUMN: DATA & TOOLTIPS ---
        y = 55;

        // Header for tooltips
        g.drawString(font, "SPECIFICATIONS & UTILITY", rightCol, y - 15, C_ACCENT, false);

        for (Component line : infoLines) {
            for (FormattedCharSequence sequence : font.split(line, maxRightWidth)) {
                g.drawString(font, sequence, rightCol, y, C_TEXT, false);
                y += 10;
            }
            y += 2;
            if (y > this.height - 80) break;
        }

        y += 10;

        // BlockState Data
        if (!state.getValues().isEmpty()) {
            g.fill(rightCol, y, rightCol + 120, y + 1, 0x22FFFFFF);
            y += 10;
            g.drawString(font, "BLOCKSTATE PROPERTIES", rightCol, y, C_DIM, false);
            y += 12;
            for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                String combined = entry.getKey().getName() + ": " + entry.getValue();
                for (FormattedCharSequence seq : font.split(Component.literal(combined), maxRightWidth)) {
                    g.drawString(font, seq, rightCol, y, C_ACCENT, false);
                    y += 10;
                }
                if (y > this.height - 60) break;
            }
        }

        // --- FOOTER BUTTONS (EMI Capability Added) ---
        int bw = 120, bh = 20;
        int bxClose = this.width - bw - 30;
        int by = this.height - 40;

        ItemStack itemStack = new ItemStack(state.getBlock().asItem()); // cite: 2
        if (!itemStack.isEmpty()) {
            // Shift Close button slightly left to make room for EMI button on the right
            bxClose = this.width - (bw * 2) - 40;
            int bxEmi = this.width - bw - 30;

            boolean emiHov = mx >= bxEmi && mx < bxEmi + bw && my >= by && my < by + bh;
            g.fill(bxEmi, by, bxEmi + bw, by + bh, emiHov ? 0xFF2A3A5A : 0xFF151A25);
            if (emiHov) {
                g.fill(bxEmi, by, bxEmi + bw, by + 1, C_ACCENT); // cite: 2
                g.fill(bxEmi, by + bh - 1, bxEmi + bw, by + bh, C_ACCENT); // cite: 2
            }
            g.drawCenteredString(font, "EMI RECIPES", bxEmi + bw / 2, by + 6, emiHov ? C_ACCENT : C_TEXT);
        }

        // --- CLOSE DATA BUTTON ---
        boolean hov = mx >= bxClose && mx < bxClose + bw && my >= by && my < by + bh; // cite: 2
        g.fill(bxClose, by, bxClose + bw, by + bh, hov ? 0xFF2A3A5A : 0xFF151A25); // cite: 2
        if (hov) {
            g.fill(bxClose, by, bxClose + bw, by + 1, C_ACCENT); // cite: 2
            g.fill(bxClose, by + bh - 1, bxClose + bw, by + bh, C_ACCENT); // cite: 2
        }
        g.drawCenteredString(font, "CLOSE DATA", bxClose + bw / 2, by + 6, hov ? C_ACCENT : C_TEXT); // cite: 2
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int bw = 120, bh = 20; // cite: 2
        int bxClose = this.width - bw - 30; // cite: 2
        int by = this.height - 40; // cite: 2

        ItemStack itemStack = new ItemStack(state.getBlock().asItem()); // cite: 2
        if (!itemStack.isEmpty()) {
            bxClose = this.width - (bw * 2) - 40;
            int bxEmi = this.width - bw - 30;

            // Check EMI Recipes Button Click
            if (mx >= bxEmi && mx < bxEmi + bw && my >= by && my < by + bh) {
                dev.emi.emi.api.EmiApi.displayRecipes(dev.emi.emi.api.stack.EmiStack.of(itemStack));
                return true;
            }
        }

        // Check Close Data Button Click
        if (mx >= bxClose && mx < bxClose + bw && my >= by && my < by + bh) { // cite: 2
            onClose(); // cite: 2
            return true; // cite: 2
        } // cite: 2
        return super.mouseClicked(mx, my, btn); // cite: 2
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
