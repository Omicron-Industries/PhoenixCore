package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.client.renderer.RenderType;
import net.phoenix.core.common.block.CrystalRoseBlock;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_DENSE;

public class PhoenixMaterialFlags {

    public static final MaterialFlag GENERATE_NANITES = new MaterialFlag.Builder("generate_nanites")
            .requireFlags(GENERATE_DENSE)
            .requireProps(PropertyKey.DUST)
            .build();

    public static final MaterialFlag GENERATE_CRYSTAL_ROSE = new MaterialFlag.Builder("generate_crystal_rose")
            .requireProps(PropertyKey.DUST)
            .build();

    public static final MaterialFlag GENERATE_TIER_ONE_BEE = new MaterialFlag.Builder("generate_tier_one_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_TIER_TWO_BEE = new MaterialFlag.Builder("generate_tier_two_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_TIER_THREE_BEE = new MaterialFlag.Builder("generate_tier_three_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_BEE_COMB = new MaterialFlag.Builder("generate_bee_comb")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_BEE_HONEYCOMB_BLOCK = new MaterialFlag.Builder(
            "generate_bee_honeycomb_block")
            .requireProps(PropertyKey.DUST)
            .build();

    public static final TagPrefix nanites = new TagPrefix("nanites")
            .idPattern("%s_nanites")
            .defaultTagPath("nanites/%s")
            .unformattedTagPath("nanites")
            .langValue("%s Nanites")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.NANITES)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_NANITES));
    public static final TagPrefix honeycomb_block = new TagPrefix("honeycomb_block")
            .idPattern("%s_honeycomb_block")
            .defaultTagPath("honeycomb_block/%s")
            .unformattedTagPath("honeycomb_block")
            .langValue("%s HoneyComb Block")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateBlock(true)
            .materialIconType(PhoenixMaterialSet.HONEYCOMB_BLOCK)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_BEE_HONEYCOMB_BLOCK));

    public static final TagPrefix crystal_rose = new TagPrefix("crystal_rose")
            .idPattern("%s_crystal_rose")
            .defaultTagPath("crystal_roses/%s")
            .unformattedTagPath("crystal_rose")
            .langValue("%s Crystal Rose")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateBlock(true)
            .blockConstructor(CrystalRoseBlock::new)

            .blockProperties(new TagPrefix.BlockProperties(
                    () -> net.minecraft.client.renderer.RenderType::cutoutMipped,
                    p -> p.noCollission().noOcclusion().instabreak()))
            .materialIconType(PhoenixMaterialSet.CRYSTAL_ROSE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE));

    public static final TagPrefix tier_one_bee = new TagPrefix("tier_one_bee")
            .idPattern("%s_tier_one_bee")
            .defaultTagPath("tier_one_bee/%s")
            .unformattedTagPath("tier_one_bee")
            .langValue("%s Lively Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_ONE_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_ONE_BEE));

    public static final TagPrefix tier_two_bee = new TagPrefix("tier_two_bee")
            .idPattern("%s_tier_two_bee")
            .defaultTagPath("tier_two_bee/%s")
            .unformattedTagPath("tier_two_bee")
            .langValue("%s Energetic Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_TWO_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_TWO_BEE));

    public static final TagPrefix tier_three_bee = new TagPrefix("tier_three_bee")
            .idPattern("%s_tier_three_bee")
            .defaultTagPath("tier_three_bee/%s")
            .unformattedTagPath("tier_three_bee")
            .langValue("%s Stronk Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_THREE_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_THREE_BEE));
    public static final TagPrefix honeycomb = new TagPrefix("honeycomb")
            .idPattern("%s_honeycomb")
            .defaultTagPath("honeycombs/%s")
            .unformattedTagPath("bee_comb")
            .langValue("%s Honeycomb")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.BEE_COMB)
            .generationCondition(mat -> mat.hasFlag(GENERATE_BEE_COMB));

    public static void init() {}
}
