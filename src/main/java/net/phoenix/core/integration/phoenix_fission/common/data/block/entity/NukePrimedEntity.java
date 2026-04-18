package net.phoenix.core.integration.phoenix_fission.common.data.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import net.phoenix.core.configs.PhoenixConfigs;

import org.jetbrains.annotations.Nullable;

public class NukePrimedEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(NukePrimedEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RADIUS = SynchedEntityData.defineId(NukePrimedEntity.class,
            EntityDataSerializers.INT);

    @Nullable
    private LivingEntity owner;

    private boolean wiping = false;
    private int wipeIndex = 0;
    private BlockPos wipeCenter = BlockPos.ZERO;

    public NukePrimedEntity(EntityType<? extends NukePrimedEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE, 80);
        this.entityData.define(DATA_RADIUS, 16);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    public void setFuse(int ticks) {
        this.entityData.set(DATA_FUSE, Math.max(1, ticks));
    }

    public void setRadius(int r) {
        var cfg = PhoenixConfigs.INSTANCE.fission;
        int cap = Math.max(1, cfg.nukeCubeRadiusCap);
        this.entityData.set(DATA_RADIUS, Mth.clamp(r, 1, cap));
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        var cfg = PhoenixConfigs.INSTANCE.fission;
        if (!cfg.nukeEnabled) {
            discard();
            return;
        }

        if (!wiping) {
            int fuse = entityData.get(DATA_FUSE) - 1;
            entityData.set(DATA_FUSE, fuse);
            if (fuse <= 0) beginWipe();
        } else {
            doWipeBatch();
        }
    }

    private void beginWipe() {
        wiping = true;
        wipeIndex = 0;
        wipeCenter = this.blockPosition();
    }

    private void doWipeBatch() {
        var cfg = PhoenixConfigs.INSTANCE.fission;

        int radius = entityData.get(DATA_RADIUS);
        int side = 2 * radius + 1;
        int plane = side * side;
        int volume = plane * side;

        int batch = Math.max(1, cfg.nukeBatchPerTick);
        boolean skipBE = cfg.nukeSkipBlockEntities;
        boolean skipUnloaded = cfg.nukeSkipUnloadedChunks;
        boolean fire = cfg.nukeReplaceWithFire;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        int processed = 0;
        while (processed < batch && wipeIndex < volume) {
            int i = wipeIndex++;

            int dx = (i % side) - radius;
            int dy = ((i / side) % side) - radius;
            int dz = (i / plane) - radius;

            mpos.set(wipeCenter.getX() + dx, wipeCenter.getY() + dy, wipeCenter.getZ() + dz);

            if (!level().isInWorldBounds(mpos)) continue;
            if (skipUnloaded && !level().hasChunkAt(mpos)) continue;

            BlockState st = level().getBlockState(mpos);
            if (st.isAir()) continue;

            if (st.is(Blocks.BEDROCK) || st.is(Blocks.END_PORTAL_FRAME)) continue;

            if (skipBE && level().getBlockEntity(mpos) != null) continue;

            level().setBlock(mpos, fire ? Blocks.FIRE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            processed++;
        }

        if (wipeIndex >= volume) discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_FUSE, tag.getInt("Fuse"));
        entityData.set(DATA_RADIUS, tag.getInt("Radius"));
        wiping = tag.getBoolean("Wiping");
        wipeIndex = tag.getInt("WipeIndex");
        wipeCenter = BlockPos.of(tag.getLong("WipeCenter"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuse", entityData.get(DATA_FUSE));
        tag.putInt("Radius", entityData.get(DATA_RADIUS));
        tag.putBoolean("Wiping", wiping);
        tag.putInt("WipeIndex", wipeIndex);
        tag.putLong("WipeCenter", wipeCenter.asLong());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
