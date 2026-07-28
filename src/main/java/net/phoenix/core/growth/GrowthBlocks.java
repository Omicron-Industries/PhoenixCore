package net.phoenix.core.growth;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.growth.tendril.GrowthWardBlock;

import com.tterrag.registrate.util.entry.BlockEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GrowthBlocks {

    public static final BlockEntry<GrowthWardBlock> GROWTH_WARD = REGISTRATE
            .block("growth_ward", GrowthWardBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(),
                            PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"))))
            .lang("Growth Ward")
            .item(BlockItem::new)
            .build()
            .register();

    public static void init() {}
}
