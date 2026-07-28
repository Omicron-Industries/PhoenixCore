package net.phoenix.core.integration.phoenix_tesla_network.common.machine;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.pattern.PhoenixPredicates;
import net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;
import net.phoenix.core.common.registry.PhoenixRegistration;
import net.phoenix.core.datagen.models.PhoenixMachineModels;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.part.TeslaEnergyHatchPartMachine;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.singleblock.electric.TeslaWirelessBatteryBufferMachine;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.GTValues.VCF;
import static com.gregtechceu.gtceu.api.multiblock.Predicates.blocks;
import static com.gregtechceu.gtceu.api.multiblock.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.machine.PhoenixMachines.registerTieredMachines;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixTeslaMachines {

    public static final int[] MULTI_AMP_TESLA_HATCH = GTValues.tiersBetween(EV, GTCEuAPI.isHighTier() ? MAX : UHV);

    public static final BiConsumer<ItemStack, List<Component>> TELSA_TOWER_TOOLTIPS = (stack,
                                                                                       list) -> {
        list.add(
                Component.literal("The pulsating heart of your Tesla Network.")
                        .withStyle(TeslaTowerMachine.NEBULA_HSL));
        list.add(
                Component.literal("Transmits EU §7inter-dimensionally§f with §7infinite§f range."));
        list.add(
                Component.literal("Internal buffer is §7determined§f by the tier of §7Tesla Battery§f it has."));
    };
    public static final MultiblockMachineDefinition TESLA_TOWER = REGISTRATE
            .multiblock("tesla_tower", TeslaTowerMachine::new)
            .langValue("Tesla Tower")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.TESLA_TOWER)
            .appearanceBlock(PhoenixBlocks.INSANELY_SUPERCHARGED_TESLA_CASING)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .slice("                   ", "                   ", "                   ", "                   ",
                            "       CCCCC       ", "       DDCDD       ", "       DDCDD       ", "       DDCDD       ",
                            "       CCCCC       ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("                   ", "                   ", "                   ", "                   ",
                            "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     CCCHFHCCC     ", "       F   F       ", "       F   F       ", "       F   F       ",
                            "       F   F       ", "       CCCCC       ", "       DDCDD       ", "       DDCDD       ",
                            "       DDCDD       ", "       CCCCC       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("     IIIIIIIII     ", "                   ", "                   ", "                   ",
                            "    CCEEEFEEECC    ", "    DD       DD    ", "    DD       DD    ", "    DD       DD    ",
                            "    CCHHJFJHHCC    ", "                   ", "                   ", "                   ",
                            "                   ", "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     GDD   DDG     ", "     CCCHFHCCC     ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("    IIJJJJJJJII    ", "        FJF        ", "        FFF        ", "        FJF        ",
                            "   CCEEGJJJGEECC   ", "   GD         DG   ", "   GD         DG   ", "   GD         DG   ",
                            "   CCHHJIFIJHHCC   ", "                   ", "                   ", "                   ",
                            "                   ", "    CCEEEFEEECC    ", "    DD       DD    ", "    DD       DD    ",
                            "    DD       DD    ", "    CCHHJFJHHCC    ", "       F   F       ", "       F   F       ",
                            "       F   F       ", "       CCCCC       ", "       DDCDD       ", "       DDCDD       ",
                            "       DDCDD       ", "       CCCCC       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("   IIJJCCCCCJJII   ", "     J       J     ", "     J       J     ", "     J       J     ",
                            "  CCEJGGJCJGGJECC  ", "  DD           DD  ", "  DD           DD  ", "  DD           DD  ",
                            "  CCHHJIIFIIJHHCC  ", "                   ", "                   ", "                   ",
                            "                   ", "   CCEEIIIIIEECC   ", "   DD         DD   ", "   DD         DD   ",
                            "   DD         DD   ", "   CCHHJJFJJHHCC   ", "                   ", "                   ",
                            "                   ", "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     GDD   DDG     ", "     CCCJFJCCC     ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IIJJCCCCCCCJJII  ", "    J         J    ", "    J         J    ", "    J         J    ",
                            " CCEJGGJJCJJGGJECC ", " GD             DG ", " GD             DG ", " GD             DG ",
                            " CCHHJIIJJJIIJHHCC ", "         F         ", "         F         ", "         F         ",
                            "         C         ", "  CCEEIIJCJIIEECC  ", "  GD           DG  ", "  GD           DG  ",
                            "  GD           DG  ", "  CCHHJEEEEEJHHCC  ", "         F         ", "         F         ",
                            "         C         ", "    CCEEEFEEECC    ", "    GD       DG    ", "    GD       DG    ",
                            "    GD       DG    ", "    CCJJIFIJJCC    ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IJJCCCGGGCCCJJI  ", "                   ", "                   ", "                   ",
                            " CEEGGJJCCCJJGGEEC ", " D               D ", " D               D ", " D               D ",
                            " CHHJIICCGCCIIJHHC ", "        K K        ", "        K K        ", "        K K        ",
                            "        LCL        ", "  CEEIIJCCCJIIEEC  ", "  D             D  ", "  D             D  ",
                            "  D             D  ", "  CHHJIICCCIIJHHC  ", "        K K        ", "        K K        ",
                            "        LCL        ", "    CEEJJJJJEEC    ", "    D         D    ", "    D         D    ",
                            "    D         D    ", "    CJJICFCIJJC    ", "        I I        ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IJCCCGGEGGCCCJI  ", "                   ", "                   ", "                   ",
                            "CCEGGJJJCECJJJGGECC", "DD               DD", "DD               DD", "DD               DD",
                            "CCHJIICEEGEECIIJHCC", " F     D   D     F ", " F     D   D     F ", " F     D   D     F ",
                            " F     ILCLI     F ", " CCEIIJJCECJJIIECC ", " DD             DD ", " DD             DD ",
                            " DD             DD ", " CCHJEICJJJCIEJHCC ", "   F   D   D   F   ", "   F   D   D   F   ",
                            "   F   ILCLI   F   ", "   CCEJJIIIJJECC   ", "   DD         DD   ", "   DD         DD   ",
                            "   DD         DD   ", "   CCJICCCCCIJCC   ", "       IICII       ", "       JJ JJ       ",
                            "       JJ JJ       ", "       JJ JJ       ", "       JJ JJ       ", "       JJFJJ       ",
                            "       JJ JJ       ", "       JJ JJ       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IJCCGGEEEGGCCJI  ", "   F     C     F   ", "   F           F   ", "   F     C     F   ",
                            "CEEJJJCCEGECCJJJEEC", "D        F        D", "D        F        D", "D        F        D",
                            "CHJIIJCEFIFECJIIJHC", "      K GIG K      ", "      K GIG K      ", "      K GIGCK      ",
                            "      LLLCLLL      ", " CEEIJCCEGECCJIEEC ", " D       F       D ", " D       F       D ",
                            " D       F       D ", " CHJJECJJGJJCEJJHC ", "      K GIG K      ", "      K GIGCK      ",
                            "      LLLCLLL      ", "   CEEJIIGIIJEEC   ", "   D     F     D   ", "   D     F     D   ",
                            "   D     F     D   ", "   CJICCCICCCIJC   ", "      IICCCII      ", "       JFFFJ       ",
                            "       JFFFJ       ", "       JFFFJ       ", "       JFFFJ       ", "       JFFFJ       ",
                            "       JFFFJ       ", "       JFFFJ       ", "        FFF        ", "        FFF        ",
                            "        FFF        ", "        FFF        ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "                   ",
                            "                   ")
                    .slice("  IJCCGEEEEEGCCJI  ", "   J    CFC    J   ", "   J     M     J   ", "   J    CNC    J   ",
                            "CFFJCCCEGNGECCCJFFC", "C       FNF       C", "C       FNF       C", "C       FNF       C",
                            "CFFFFJGGINIGGJFFFFC", "     F  INI  F     ", "     F  INI  F     ", "     F  INI  F     ",
                            "     CCCCNCCCC     ", " CFFICCEGNGECCIFFC ", " C      FNF      C ", " C      FNF      C ",
                            " C      FNF      C ", " CFFFECJGNGJCEFFFC ", "     F  INI  F     ", "     F  INI  F     ",
                            "     CCCCNCCCC     ", "   CFFJIGNGIJFFC   ", "   C    FNF    C   ", "   C    FNF    C   ",
                            "   C    FNF    C   ", "   CFFFCIIICFFFC   ", "       CCNCC       ", "        FNF        ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "       FFNFF       ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "        FNF        ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "        FNF        ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "        FNF        ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "        FNF        ",
                            "        FNF        ", "        FNF        ", "        FNF        ", "         N         ",
                            "         N         ")
                    .slice("  IJCCGGEEEGGCCJI  ", "   F     C     F   ", "   F           F   ", "   F     C     F   ",
                            "CEEJJJCCEGECCJJJEEC", "D        F        D", "D        F        D", "D        F        D",
                            "CHJIIJCEFIFECJIIJHC", "      K GIG K      ", "      K GIG K      ", "      K GIG K      ",
                            "      LLLCLLL      ", " CEEIJCCEGECCJIEEC ", " D       F       D ", " D       F       D ",
                            " D       F       D ", " CHJJECJJGJJCEJJHC ", "      K GIG K      ", "      K GIG K      ",
                            "      LLLCLLL      ", "   CEEJIIGIIJEEC   ", "   D     F     D   ", "   D     F     D   ",
                            "   D     F     D   ", "   CJICCCICCCIJC   ", "      IICCCII      ", "       JFFFJ       ",
                            "       JFFFJ       ", "       JFFFJ       ", "       JFFFJ       ", "       JFFFJ       ",
                            "       JFFFJ       ", "       JFFFJ       ", "        FFF        ", "        FFF        ",
                            "        FFF        ", "        FFF        ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "         F         ",
                            "         F         ", "         F         ", "         F         ", "                   ",
                            "                   ")
                    .slice("  IJCCCGGEGGCCCJI  ", "                   ", "                   ", "                   ",
                            "CCEGGJJJCECJJJGGECC", "DD               DD", "DD               DD", "DD               DD",
                            "CCHJIICEEGEECIIJHCC", " F     D   D     F ", " F     D   D     F ", " F     D   D     F ",
                            " F     ILCLI     F ", " CCEIIJJCECJJIIECC ", " DD             DD ", " DD             DD ",
                            " DD             DD ", " CCHJEICJJJCIEJHCC ", "   F   D   D   F   ", "   F   D   D   F   ",
                            "   F   ILCLI   F   ", "   CCEJJIIIJJECC   ", "   DD         DD   ", "   DD         DD   ",
                            "   DD         DD   ", "   CCJICCCCCIJCC   ", "       IICII       ", "       JJ JJ       ",
                            "       JJ JJ       ", "       JJ JJ       ", "       JJ JJ       ", "       JJFJJ       ",
                            "       JJ JJ       ", "       JJ JJ       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IJJCCCGGGCCCJJI  ", "                   ", "                   ", "                   ",
                            " CEEGGJJCCCJJGGEEC ", " D               D ", " D               D ", " D               D ",
                            " CHHJIICCGCCIIJHHC ", "        K K        ", "        K K        ", "        K K        ",
                            "        LCL        ", "  CEEIIJCCCJIIEEC  ", "  D             D  ", "  D             D  ",
                            "  D             D  ", "  CHHJIICCCIIJHHC  ", "        K K        ", "        K K        ",
                            "        LCL        ", "    CEEJJJJJEEC    ", "    D         D    ", "    D         D    ",
                            "    D         D    ", "    CJJICFCIJJC    ", "        I I        ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("  IIJJCCCCCCCJJII  ", "    J         J    ", "    J         J    ", "    J         J    ",
                            " CCEJGGJJCJJGGJECC ", " GD             DG ", " GD             DG ", " GD             DG ",
                            " CCHHJIIJJJIIJHHCC ", "         F         ", "         F         ", "         F         ",
                            "         C         ", "  CCEEIIJCJIIEECC  ", "  GD           DG  ", "  GD           DG  ",
                            "  GD           DG  ", "  CCHHJEEEEEJHHCC  ", "         F         ", "         F         ",
                            "         C         ", "    CCEEEFEEECC    ", "    GD       DG    ", "    GD       DG    ",
                            "    GD       DG    ", "    CCJJIFIJJCC    ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("   IIJJCCCCCJJII   ", "     J       J     ", "     J       J     ", "     J       J     ",
                            "  CCEJGGJCJGGJECC  ", "  DD           DD  ", "  DD           DD  ", "  DD           DD  ",
                            "  CCHHJIIFIIJHHCC  ", "                   ", "                   ", "                   ",
                            "                   ", "   CCEEIIIIIEECC   ", "   DD         DD   ", "   DD         DD   ",
                            "   DD         DD   ", "   CCHHJJFJJHHCC   ", "                   ", "                   ",
                            "                   ", "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     GDD   DDG     ", "     CCCJFJCCC     ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("    IIJJJJJJJII    ", "        FJF        ", "        FOF        ", "        FJF        ",
                            "   CCEEGJJJGEECC   ", "   GD         DG   ", "   GD         DG   ", "   GD         DG   ",
                            "   CCHHJIFIJHHCC   ", "                   ", "                   ", "                   ",
                            "                   ", "    CCEEEFEEECC    ", "    DD       DD    ", "    DD       DD    ",
                            "    DD       DD    ", "    CCHHJFJHHCC    ", "       F   F       ", "       F   F       ",
                            "       F   F       ", "       CCCCC       ", "       DDCDD       ", "       DDCDD       ",
                            "       DDCDD       ", "       CCCCC       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("     IIIIIIIII     ", "                   ", "                   ", "                   ",
                            "    CCEEEFEEECC    ", "    DD       DD    ", "    DD       DD    ", "    DD       DD    ",
                            "    CCHHJFJHHCC    ", "                   ", "                   ", "                   ",
                            "                   ", "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     GDD   DDG     ", "     CCCHFHCCC     ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("                   ", "                   ", "                   ", "                   ",
                            "     CCCEFECCC     ", "     GDD   DDG     ", "     GDD   DDG     ", "     GDD   DDG     ",
                            "     CCCHFHCCC     ", "       F   F       ", "       F   F       ", "       F   F       ",
                            "       F   F       ", "       CCCCC       ", "       DDCDD       ", "       DDCDD       ",
                            "       DDCDD       ", "       CCCCC       ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .slice("                   ", "                   ", "                   ", "                   ",
                            "       CCCCC       ", "       DDCDD       ", "       DDCDD       ", "       DDCDD       ",
                            "       CCCCC       ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ", "                   ", "                   ", "                   ",
                            "                   ")
                    .where(' ', Predicates.any())
                    .where('C', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get()))
                    .where('D', blocks(GTBlocks.CASING_TEMPERED_GLASS.get())
                            .or(blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                            .or(blocks(GTBlocks.FUSION_GLASS.get())))
                    .where('E', PhoenixPredicates.lampsByColor(DyeColor.PURPLE))
                    .where('F',
                            blocks(ChemicalHelper.getBlock(TagPrefix.frameGt,
                                    PhoenixProgressionMaterials.ADVANCED_QUIN_NAQUADIAN_ALLOY)))
                    .where('G', Predicates.blocks(PhoenixBlocks.MACHINE_CASING_RHODIUM_PLATED_PALLADIUM.get()))
                    .where('H', PhoenixPredicates.lampsByColor(DyeColor.BLACK))
                    .where('I', blocks(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING.get()))
                    .where('J', blocks(PhoenixBlocks.INSANELY_SUPERCHARGED_TESLA_CASING.get())
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(10)
                                    .setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.SUBSTATION_INPUT_ENERGY).setMaxGlobalLimited(10)
                                    .setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.SUBSTATION_OUTPUT_ENERGY).setMaxGlobalLimited(10)
                                    .setPreviewCount(1))
                            .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(10)
                                    .setPreviewCount(1)))
                    .where('K', blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get()))
                    .where('L', blocks(PhoenixBlocks.RELIABLE_NAQUADAH_ALLOY_MACHINE_CASING.get()))
                    .where('M', PhoenixPredicates.teslaBatteries())
                    .where('N', blocks(GTBlocks.COIL_CUPRONICKEL.get()))
                    .where('O', controller(blocks(definition.get())))
                    .build())

            .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
            .model(
                    createWorkableCasingMachineModel(
                            PhoenixCore.id("block/casings/multiblock/tesla_casing"),
                            PhoenixCore.id("block/multiblock/tesla_tower"))
                            .andThen(d -> d
                                    .addDynamicRenderer(
                                            PhoenixDynamicRenderHelpers::getTeslaTowerRenderer)))
            .hasBER(true)
            .tooltipBuilder(TELSA_TOWER_TOOLTIPS)
            .register();

    private static String getTeslaOverlay(String iomode, int amperage) {
        if (amperage == 16) {
            return "tesla_hatches/tesla_" + iomode;
        }

        return "tesla_hatches/tesla_" + iomode + "_" + amperage + "a";
    }

    private static MachineDefinition[] registerTeslaHatch(String name, IO io, int amperage, PartAbility ability,
                                                          int... tiers) {
        String iomode = io == IO.OUT ? "input" : "output";

        return registerTieredMachines(
                name + "_" + amperage + "a",
                (holder, tier) -> new TeslaEnergyHatchPartMachine(holder, tier, io, amperage),
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + " Tesla Energy " + (io == IO.OUT ? "Uplink" : "Downlink") +
                                " Hatch " + amperage + "A")
                        .rotationState(RotationState.ALL)
                        .abilities(ability)
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .tooltips(
                                Component.translatable("gtceu.universal.tooltip.voltage_in",
                                        FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]),
                                Component.translatable(
                                        "gtceu.universal.tooltip.amperage_" + (io == IO.OUT ? "out" : "in"), amperage),
                                Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                                        FormattingUtil.formatNumbers(
                                                GTValues.V[tier] * (io == IO.OUT ? 16L : 64L) * amperage)),
                                Component.translatable("tooltip.phoenixcore.tesla_hatch." + iomode))
                        .overlayTieredHullModel(getTeslaOverlay(iomode, amperage))
                        .register(),
                tiers);
    }

    private static String getLaserName(IO io, int amperage) {
        if (io == IO.OUT) {
            if (amperage >= 4096) return "Phased Tesla Beam Matrix";
            if (amperage >= 256) return "Tesla Beam Collimator Array";
            return "Tesla Beam Collimator";
        } else {
            if (amperage >= 4096) return "Tesla Flux Coalescence Matrix";
            if (amperage >= 256) return "Tesla Flux Coalescence Array";
            return "Tesla Flux Coalescer";
        }
    }

    private static MachineDefinition[] registerTeslaLaserHatch(String name, IO io, int amperage, PartAbility ability) {
        String iomode = io == IO.OUT ? "input" : "output";
        String laserDisplayName = getLaserName(io, amperage);

        return registerTieredMachines(
                name + "_" + amperage + "a",
                (holder, tier) -> new TeslaEnergyHatchPartMachine(holder, tier, io, amperage),
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + " " + laserDisplayName + " " + amperage + "A")
                        .rotationState(RotationState.ALL)
                        .abilities(ability)
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .tooltips(
                                Component.translatable("gtceu.universal.tooltip.voltage_in",
                                        FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]),
                                Component.translatable(
                                        "gtceu.universal.tooltip.amperage_" + (io == IO.OUT ? "in" : "out"), amperage),
                                Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                                        FormattingUtil.formatNumbers(
                                                GTValues.V[tier] * (io == IO.OUT ? 16L : 64L) * amperage)),
                                Component.translatable("tooltip.phoenixcore.tesla_hatch." + iomode))
                        .overlayTieredHullModel(getTeslaLaserOverlay(iomode, amperage))
                        .register(),
                GTValues.ALL_TIERS);
    }

    private static String getTeslaLaserOverlay(String iomode, int amperage) {
        return "tesla_hatches/tesla_" + iomode + "_" + "laser" + "_" + amperage + "a";
    }

    public static final MachineDefinition[] TESLA_LASER_INPUT_256A = registerTeslaLaserHatch(
            "tesla_laser_input_hatch_256a", IO.OUT, 256,
            PartAbility.INPUT_LASER);
    public static final MachineDefinition[] TESLA_LASER_OUTPUT_256A = registerTeslaLaserHatch(
            "tesla_laser_output_hatch_256a", IO.IN, 256,
            PartAbility.OUTPUT_LASER);

    public static final MachineDefinition[] TESLA_LASER_INPUT_1024A = registerTeslaLaserHatch(
            "tesla_laser_input_hatch_1024a", IO.OUT, 1024,
            PartAbility.INPUT_LASER);
    public static final MachineDefinition[] TESLA_LASER_OUTPUT_1024A = registerTeslaLaserHatch(
            "tesla_laser_output_hatch_1024a", IO.IN, 1024,
            PartAbility.OUTPUT_LASER);

    public static final MachineDefinition[] TESLA_LASER_INPUT_4096A = registerTeslaLaserHatch(
            "tesla_laser_input_hatch_4096a", IO.OUT, 4096,
            PartAbility.INPUT_LASER);
    public static final MachineDefinition[] TESLA_LASER_OUTPUT_4096A = registerTeslaLaserHatch(
            "tesla_laser_output_hatch_4096a", IO.IN, 4096,
            PartAbility.OUTPUT_LASER);

    public static final MachineDefinition[] TESLA_INPUT_2A = registerTeslaHatch("tesla_energy_input_hatch", IO.OUT, 2,
            PartAbility.OUTPUT_ENERGY, GTValues.ALL_TIERS);

    public static final MachineDefinition[] TESLA_INPUT_4A = registerTeslaHatch("tesla_energy_input_hatch", IO.OUT, 4,
            PartAbility.OUTPUT_ENERGY, MULTI_AMP_TESLA_HATCH);

    public static final MachineDefinition[] TESLA_INPUT_16A = registerTeslaHatch("tesla_energy_input_hatch", IO.OUT, 16,
            PartAbility.OUTPUT_ENERGY, MULTI_AMP_TESLA_HATCH);

    public static final MachineDefinition[] TESLA_INPUT_64A = registerTeslaHatch("tesla_energy_input_hatch", IO.OUT, 64,
            PartAbility.SUBSTATION_OUTPUT_ENERGY,
            MULTI_AMP_TESLA_HATCH);

    public static final MachineDefinition[] TESLA_OUTPUT_2A = registerTeslaHatch("tesla_energy_output_hatch", IO.IN, 2,
            PartAbility.INPUT_ENERGY, GTValues.ALL_TIERS);

    public static final MachineDefinition[] TESLA_OUTPUT_4A = registerTeslaHatch("tesla_energy_output_hatch", IO.IN, 4,
            PartAbility.INPUT_ENERGY, MULTI_AMP_TESLA_HATCH);

    public static final MachineDefinition[] TESLA_OUTPUT_16A = registerTeslaHatch("tesla_energy_output_hatch", IO.IN,
            16, PartAbility.INPUT_ENERGY,
            MULTI_AMP_TESLA_HATCH);

    public static final MachineDefinition[] TESLA_OUTPUT_64A = registerTeslaHatch("tesla_energy_output_hatch", IO.IN,
            64, PartAbility.SUBSTATION_INPUT_ENERGY,
            MULTI_AMP_TESLA_HATCH);

    public static MachineDefinition[] registerWirelessCharger(
                                                              GTRegistrate registrate,
                                                              String name,
                                                              BiFunction<BlockEntityCreationInfo, Integer, TeslaWirelessBatteryBufferMachine> factory) {
        return registerChargerTieredMachines(
                registrate,
                name,
                (holder, tier) -> factory.apply(holder, tier),
                (tier, builder) -> builder
                        .rotationState(RotationState.ALL)
                        .modelProperty(GTMachineModelProperties.CHARGER_STATE, BatteryBufferMachine.State.IDLE)
                        .model(PhoenixMachineModels.createWirelessChargerModel())
                        .langValue("%s Tesla Wireless Charger".formatted(
                                VCF[tier] + GTValues.VOLTAGE_NAMES[tier] + ChatFormatting.RESET))
                        .tooltips(
                                Component.translatable("gtceu.universal.tooltip.voltage_in",
                                        FormattingUtil.formatNumbers(GTValues.V[tier]),
                                        GTValues.VNF[tier]),
                                Component.translatable("gtceu.universal.tooltip.amperage_in", 4),
                                Component.literal("Wireless Range: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(
                                                tier >= GTValues.LV ? "Global (Cross-Dimensional)" :
                                                        (8 * (tier + 1)) + "m")
                                                .withStyle(ChatFormatting.AQUA)),
                                Component.literal("Charges armor and tools from the Team Energy Cloud")
                                        .withStyle(ChatFormatting.GREEN))
                        .register(),
                GTValues.ALL_TIERS);
    }

    public static MachineDefinition[] registerChargerTieredMachines(
                                                                    GTRegistrate registrate,
                                                                    String name,
                                                                    BiFunction<BlockEntityCreationInfo, Integer, MetaMachine> machineFactory,
                                                                    BiFunction<Integer, MachineBuilder<MachineDefinition, ?, ?>, MachineDefinition> definitionBuilder,
                                                                    int... tiers) {
        MachineDefinition[] definitions = new MachineDefinition[GTValues.TIER_COUNT];

        for (int tier : tiers) {
            var builder = registrate
                    .machine(GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_" + name,
                            holder -> machineFactory.apply(holder, tier))
                    .tier(tier);

            definitions[tier] = definitionBuilder.apply(tier, builder);
        }

        return definitions;
    }

    public static final MachineDefinition[] TESLA_WIRELESS_CHARGER = registerWirelessCharger(
            PhoenixRegistration.REGISTRATE,
            "tesla_wireless_charger",
            TeslaWirelessBatteryBufferMachine::new);

    public static void init() {}
}
