package net.phoenix.core.conflux.terminal;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public final class ConfluxTerminalRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, PhoenixCore.MOD_ID);

    public static final RegistryObject<ResearchTerminalBlock> TERMINAL;
    public static final RegistryObject<BlockEntityType<ResearchTerminalBlockEntity>> TERMINAL_BE;

    static {
        @SuppressWarnings("unchecked")
        RegistryObject<BlockEntityType<ResearchTerminalBlockEntity>>[] beHolder = new RegistryObject[1];

        TERMINAL = BLOCKS.register("research_terminal",
                () -> new ResearchTerminalBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3f, 12f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));

        TERMINAL_BE = BES.register("research_terminal",
                () -> BlockEntityType.Builder.of(
                        (pos, state) -> new ResearchTerminalBlockEntity(beHolder[0].get(), pos, state),
                        TERMINAL.get()).build(null));

        beHolder[0] = TERMINAL_BE;

        ITEMS.register("research_terminal", () -> new BlockItem(TERMINAL.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }

    private ConfluxTerminalRegistry() {}
}
