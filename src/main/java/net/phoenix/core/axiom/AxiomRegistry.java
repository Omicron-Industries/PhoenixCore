package net.phoenix.core.axiom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.phoenix.core.axiom.pipe.AxiomMultiHandlerCapability;
import net.phoenix.core.axiom.terminal.AxiomTerminalRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.axiom.pipe.AxiomDataCapability;
import net.phoenix.core.axiom.pipe.AxiomPipeBlock;
import net.phoenix.core.axiom.pipe.AxiomPipeBlockEntity;

import java.util.EnumMap;
import java.util.Map;

public final class AxiomRegistry {

    private static final DeferredRegister<Block>                     BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,           PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item>                      ITEMS  = DeferredRegister.create(ForgeRegistries.ITEMS,            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>>        BES    = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PhoenixCore.MOD_ID);

    // ── Pipe blocks ───────────────────────────────────────────────────────────

    public static final Map<AxiomDataType, RegistryObject<AxiomPipeBlock>>            PIPES    = new EnumMap<>(AxiomDataType.class);
    public static final Map<AxiomDataType, RegistryObject<BlockEntityType<AxiomPipeBlockEntity>>> PIPE_BES = new EnumMap<>(AxiomDataType.class);

    static {
        for (AxiomDataType type : AxiomDataType.values()) {
            // Skip soft-dep types if mod absent — registration still happens (so
            // resource packs can provide assets) but the block won't appear in
            // creative tabs or recipes when the dep is missing.
            String id = type.id() + "_data_pipe";

            // Forward-ref: BE type needs the block, block needs the BE type supplier.
            // Solve with a holder array to break the cycle.
            @SuppressWarnings("unchecked")
            RegistryObject<BlockEntityType<AxiomPipeBlockEntity>>[] beHolder = new RegistryObject[1];

            RegistryObject<AxiomPipeBlock> block = BLOCKS.register(id, () ->
                    new AxiomPipeBlock(pipeProps(type), type, () -> beHolder[0].get()));

            RegistryObject<BlockEntityType<AxiomPipeBlockEntity>> be = BES.register(id, () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new AxiomPipeBlockEntity(beHolder[0].get(), pos, state, type),
                            block.get()
                    ).build(null));

            beHolder[0] = be;

            PIPES.put(type, block);
            PIPE_BES.put(type, be);

            // BlockItem
            ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    // ── Registration entry point ──────────────────────────────────────────────

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        AxiomTerminalRegistry.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        AxiomDataCapability.register(event);
        AxiomMultiHandlerCapability.register(event);
    }

    // ── Block properties per type ─────────────────────────────────────────────

    private static BlockBehaviour.Properties pipeProps(AxiomDataType type) {
        return BlockBehaviour.Properties.of()
                .mapColor(typeColor(type))
                .strength(1.5f, 6f)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static MapColor typeColor(AxiomDataType type) {
        return switch (type) {
            case MATERIAL     -> MapColor.GOLD;
            case BIOLOGICAL   -> MapColor.GRASS;
            case ENERGETIC    -> MapColor.DIAMOND;
            case COMPUTATIONAL -> MapColor.COLOR_PURPLE;
            case ARCANE       -> MapColor.COLOR_MAGENTA;
        };
    }

    private AxiomRegistry() {}
}
