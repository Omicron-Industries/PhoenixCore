package net.phoenix.core.integration.phantasia;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PhantasiaFakeLevel extends Level {

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final FakeChunkSource chunkSource;

    @SuppressWarnings("deprecation")
    public PhantasiaFakeLevel() {
        super(
                (WritableLevelData) Minecraft.getInstance().level.getLevelData(),
                Minecraft.getInstance().level.dimension(),
                Minecraft.getInstance().level.registryAccess(),
                Minecraft.getInstance().level.dimensionTypeRegistration(),
                () -> Minecraft.getInstance().getProfiler(),
                true,   // isClientSide — required for block renderers
                false,
                0L,
                0);
        this.chunkSource = new FakeChunkSource(this);
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void placeBlock(BlockPos pos, BlockState state) {
        setBlock(pos, state, 3);
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return true; // The matcher stops instantly if this is false
    }

    @Override
    public boolean hasChunkAt(BlockPos pos) {
        return true;
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean validate) {
        return this.getChunkSource().getChunk(x, z, status, validate);
    }

    /**
     * Called once after all blocks are placed. Runs onLoad() and a first
     * requestModelDataUpdate() on every BE so CTM neighbor bits are computed
     * while all blocks are already present in the map.
     */
    public void finalizeLoad() {
        System.out
                .println("[Phantasia] finalizeLoad() called. Processing " + blockEntities.size() + " block entities.");
        for (BlockEntity be : blockEntities.values()) {
            try {
                be.onLoad();
                System.out
                        .println("[Phantasia] onLoad() -> " + be.getClass().getSimpleName() + " @ " + be.getBlockPos());
            } catch (Exception e) {
                System.out.println(
                        "[Phantasia] onLoad() threw for " + be.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // First requestModelDataUpdate pass — CTM reads neighbor states here and
        // bakes connection bits into ModelData.
        for (BlockEntity be : blockEntities.values()) {
            try {
                be.requestModelDataUpdate();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Called after initializeMultiblock() has written IS_FORMED = true into the
     * controller's MachineRenderState. A second requestModelDataUpdate() pass is
     * needed so that the updated render state is reflected in the ModelData
     * snapshot the renderer reads at draw time.
     */
    public void refreshModelData() {
        for (BlockEntity be : blockEntities.values()) {
            try {
                be.requestModelDataUpdate();
            } catch (Exception ignored) {}
        }
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return Collections.unmodifiableMap(blocks);
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return Collections.unmodifiableMap(blockEntities);
    }

    // ── Level abstract methods ────────────────────────────────────────────────

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Override
    public void setBlockEntity(BlockEntity be) {
        if (be != null) blockEntities.put(be.getBlockPos().immutable(), be);
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        blockEntities.remove(pos);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        blocks.put(pos.immutable(), state);

        if (state.hasBlockEntity()) {
            BlockEntity be = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            if (be != null) {
                be.setLevel(this);
                blockEntities.put(pos.immutable(), be);
                be.requestModelDataUpdate();
            }
        }
        return true;
    }

    /**
     * MetaMachineBlock extends Block and implements IMachineBlock which extends EntityBlock,
     * so the vanilla EntityBlock cast always works for GTCEu machine blocks.
     * Forge registry is kept as a safety net for any other mods.
     */
    private @Nullable BlockEntity tryCreateBlockEntity(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof EntityBlock eb) {
            try {
                return eb.newBlockEntity(pos, state);
            } catch (Exception e) {
                System.out.println("[Phantasia] EntityBlock.newBlockEntity threw: " + e.getMessage());
            }
        }
        try {
            for (net.minecraft.world.level.block.entity.BlockEntityType<?> type : net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES) {
                if (type.isValid(state)) {
                    BlockEntity be = type.create(pos, state);
                    if (be != null) return be;
                }
            }
        } catch (Exception e) {
            System.out.println("[Phantasia] Forge BE registry fallback threw: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        blocks.remove(pos);
        removeBlockEntity(pos);
        return true;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity entity, int recursionLeft) {
        blocks.remove(pos);
        removeBlockEntity(pos);
        return true;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState now, int flags) {
        BlockEntity be = blockEntities.get(pos);
        if (be != null) {
            try {
                be.requestModelDataUpdate();
            } catch (Exception ignored) {}
        }
    }

    // ── Lighting — always full bright ─────────────────────────────────────────

    @Override
    public float getShade(Direction direction, boolean shade) {
        return switch (direction) {
            case DOWN, UP -> 0.9F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDark) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return true;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return Minecraft.getInstance().level.getLightEngine();
    }

    // ── Biome ─────────────────────────────────────────────────────────────────

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return Minecraft.getInstance().level.getUncachedNoiseBiome(x, y, z);
    }

    // ── Chunks ────────────────────────────────────────────────────────────────

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return 0;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    // ── Ticks ─────────────────────────────────────────────────────────────────

    @Override
    public LevelTickAccess<net.minecraft.world.level.block.Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public @NotNull ChunkSource getChunkSource() {
        return chunkSource;
    }

    // ── Entities — none ───────────────────────────────────────────────────────

    @Override
    public @NotNull List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> test, AABB b, Predicate<? super T> p) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity e, AABB b, @Nullable Predicate<? super Entity> p) {
        return Collections.emptyList();
    }

    // ── Sound / particles / events — no-ops ──────────────────────────────────

    @Override
    public void playSeededSound(@Nullable Player p, double x, double y, double z, Holder<SoundEvent> h, SoundSource s,
                                float v, float p2, long l) {}

    @Override
    public void playSeededSound(@Nullable Player p, Entity e, Holder<SoundEvent> h, SoundSource s, float v, float p2,
                                long l) {}

    @Override
    public void playSound(@Nullable Player p, double x, double y, double z, SoundEvent s, SoundSource src, float v,
                          float pitch) {}

    @Override
    public void playSound(@Nullable Player p, Entity e, SoundEvent s, SoundSource src, float v, float pitch) {}

    @Override
    public void addParticle(net.minecraft.core.particles.ParticleOptions o, double x, double y, double z, double sx,
                            double sy, double sz) {}

    @Override
    public void addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleOptions o, double x, double y, double z,
                                         double sx, double sy, double sz) {}

    @Override
    public void addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleOptions o, boolean ov, double x, double y,
                                         double z, double sx, double sy, double sz) {}

    @Override
    public void levelEvent(@Nullable Player p, int type, BlockPos pos, int data) {}

    @Override
    public void gameEvent(GameEvent e, net.minecraft.world.phys.Vec3 pos, GameEvent.Context ctx) {}

    // ── Misc ──────────────────────────────────────────────────────────────────

    @Override
    public String gatherChunkSourceStats() {
        return "PhantasiaFakeLevel";
    }

    @Override
    public @Nullable Entity getEntity(int id) {
        return null;
    }

    @Override
    public @Nullable MapItemSavedData getMapData(String n) {
        return null;
    }

    @Override
    public void setMapData(String id, MapItemSavedData d) {}

    @Override
    public int getFreeMapId() {
        return 0;
    }

    @Override
    public void destroyBlockProgress(int id, BlockPos pos, int p) {}

    @Override
    public Scoreboard getScoreboard() {
        return Minecraft.getInstance().level.getScoreboard();
    }

    @Override
    public RecipeManager getRecipeManager() {
        return Minecraft.getInstance().level.getRecipeManager();
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return null;
    }

    @Override
    public RegistryAccess registryAccess() {
        return Minecraft.getInstance().level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return Minecraft.getInstance().level.enabledFeatures();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver r) {
        return r.getColor(Minecraft.getInstance().level.getBiome(pos).value(), pos.getX(), pos.getZ());
    }
}
