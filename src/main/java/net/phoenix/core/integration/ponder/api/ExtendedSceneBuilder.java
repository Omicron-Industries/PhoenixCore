package net.phoenix.core.integration.ponder.api;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.*;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.*;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.ShowInputInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.integration.ponder.particles.ParticleInstructions;
import net.phoenix.core.integration.ponder.util.PonderPlatform;
import net.phoenix.core.integration.ponder.util.SceneBuilderInternalAccess;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ExtendedSceneBuilder implements SceneBuilder {

    private final SceneBuilder delegate;
    private final PonderScene ponderScene;
    @Getter
    private final ParticleInstructions particles;
    private final ExtendedWorldInstructions extendedWorld;
    private final ExtendedSpecialInstructions extendedSpecial;

    public ExtendedSceneBuilder(SceneBuilder delegate) {
        this.delegate = delegate;

        // Check if our Mixin successfully attached the interface
        if (delegate instanceof SceneBuilderInternalAccess internal) {
            this.ponderScene = internal.phoenixcore$getPonderScene();
            this.extendedWorld = new ExtendedWorldInstructions(delegate.world());
            this.extendedSpecial = new ExtendedSpecialInstructions(delegate.special());
            internal.phoenixcore$setWorldInstructions(extendedWorld);
            internal.phoenixcore$setSpecialInstructions(extendedSpecial);
        } else {
            // Fallback for when Mixins/Classloaders act up
            this.ponderScene = delegate.getScene();
            this.extendedWorld = new ExtendedWorldInstructions(delegate.world());
            this.extendedSpecial = new ExtendedSpecialInstructions(delegate.special());
        }

        this.particles = new ParticleInstructions(this);
    }
    // --- SceneBuilder delegation ---

    @Override
    public @NotNull OverlayInstructions overlay() {
        return delegate.overlay();
    }

    @Override
    public @NotNull WorldInstructions world() {
        return extendedWorld;
    }

    @Override
    public @NotNull DebugInstructions debug() {
        return delegate.debug();
    }

    @Override
    public @NotNull EffectInstructions effects() {
        return delegate.effects();
    }

    @Override
    public SpecialInstructions special() {
        return extendedSpecial;
    }

    @Override
    public PonderScene getScene() {
        return delegate.getScene();
    }

    @Override
    public void title(String sceneId, String title) {
        delegate.title(sceneId, title);
    }

    @Override
    public void configureBasePlate(int x, int z, int size) {
        delegate.configureBasePlate(x, z, size);
    }

    @Override
    public void scaleSceneView(float factor) {
        delegate.scaleSceneView(factor);
    }

    @Override
    public void removeShadow() {
        delegate.removeShadow();
    }

    @Override
    public void setSceneOffsetY(float yOffset) {
        delegate.setSceneOffsetY(yOffset);
    }

    @Override
    public void showBasePlate() {
        delegate.showBasePlate();
    }

    @Override
    public void addInstruction(PonderInstruction instruction) {
        delegate.addInstruction(instruction);
    }

    @Override
    public void addInstruction(Consumer<PonderScene> callback) {
        delegate.addInstruction(callback);
    }

    @Override
    public void idle(int ticks) {
        delegate.idle(ticks);
    }

    @Override
    public void idleSeconds(int seconds) {
        delegate.idleSeconds(seconds);
    }

    @Override
    public void markAsFinished() {
        delegate.markAsFinished();
    }

    @Override
    public void setNextUpEnabled(boolean enabled) {
        delegate.setNextUpEnabled(enabled);
    }

    @Override
    public void rotateCameraY(float degrees) {
        delegate.rotateCameraY(degrees);
    }

    @Override
    public void addKeyframe() {
        delegate.addKeyframe();
    }

    @Override
    public void addLazyKeyframe() {
        delegate.addLazyKeyframe();
    }

    // --- Convenience accessors ---

    public ExtendedWorldInstructions getWorld() {
        return extendedWorld;
    }

    public ExtendedWorldInstructions getLevel() {
        return extendedWorld;
    }

    // --- Extended helpers ---

    public void showStructure() {
        showStructure(ponderScene.getBasePlateSize() * 2);
    }

    public void showStructure(int height) {
        BlockPos start = new BlockPos(ponderScene.getBasePlateOffsetX(), 0, ponderScene.getBasePlateOffsetZ());
        BlockPos size = new BlockPos(ponderScene.getBasePlateSize() - 1, height, ponderScene.getBasePlateSize() - 1);
        Selection selection = ponderScene.getSceneBuildingUtil().select().cuboid(start, size);
        encapsulateBounds(size);
        world().showSection(selection, Direction.UP);
    }

    public void encapsulateBounds(BlockPos size) {
        addInstruction(ps -> {
            PonderLevel w = ps.getWorld();
            if (w != null) {
                w.getBounds().encapsulate(size);
            }
        });
    }

    public void playSound(SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch) {
        Preconditions.checkArgument(soundEvent != null, "Given sound does not exist");
        if (Minecraft.getInstance().player != null) {
            addInstruction(ps -> {
                SimpleSoundInstance sound = new SimpleSoundInstance(soundEvent, soundSource, volume, pitch,
                        SoundInstance.createUnseededRandom(),
                        Minecraft.getInstance().player.blockPosition());
                Minecraft.getInstance().getSoundManager().play(sound);
            });
        }
    }

    public void playSound(SoundEvent soundEvent, float volume) {
        playSound(soundEvent, SoundSource.MASTER, volume, 1);
    }

    public void playSound(SoundEvent soundEvent) {
        playSound(soundEvent, SoundSource.MASTER, 1, 1);
    }

    public TextElementBuilder text(int duration, String text) {
        return overlay().showText(duration).text(text);
    }

    public TextElementBuilder text(int duration, String text, Vec3 position) {
        return overlay().showText(duration).text(text).pointAt(position);
    }

    public TextElementBuilder sharedText(int duration, ResourceLocation key) {
        return overlay().showText(duration).sharedText(key);
    }

    public TextElementBuilder sharedText(int duration, ResourceLocation key, Vec3 position) {
        return overlay().showText(duration).sharedText(key).pointAt(position);
    }

    // Fix 4: use net.createmod.catnip.math.Pointing
    public InputWindowElement showControls(int duration, Vec3 pos, Pointing pointing) {
        InputWindowElement element = new InputWindowElement(pos, pointing);
        addInstruction(new ShowInputInstruction(element, duration));
        return element;
    }

    // --- Inner classes ---

    public class ExtendedWorldInstructions implements WorldInstructions {

        private final WorldInstructions delegate;

        public ExtendedWorldInstructions(WorldInstructions delegate) {
            this.delegate = delegate;
        }

        public void modifyBlocksWithFunction(Selection pos, UnaryOperator<BlockState> function) {
            modifyBlocks(pos, function, true);
        }

        public void setBlocksOrdered(Selection selection, boolean spawnParticles, BlockState blockState) {
            setBlocks(selection, blockState, spawnParticles);
        }

        @Deprecated(forRemoval = true)
        public void modifyTileNBT(Selection selection, Consumer<CompoundTag> consumer) {
            modifyBlockEntityNBT(selection, BlockEntity.class, consumer, false);
        }

        @Deprecated(forRemoval = true)
        public void modifyTileNBT(Selection selection, Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
            modifyBlockEntityNBT(selection, BlockEntity.class, consumer, reDrawBlocks);
        }

        public void modifyBlockEntityNBT(Selection selection, Consumer<CompoundTag> consumer) {
            modifyBlockEntityNBT(selection, BlockEntity.class, consumer, false);
        }

        public void modifyBlockEntityNBT(Selection selection, boolean reDrawBlocks, Consumer<CompoundTag> consumer) {
            modifyBlockEntityNBT(selection, BlockEntity.class, consumer, reDrawBlocks);
        }

        public ElementLink<EntityElement> createEntity(EntityType<?> entityType, Vec3 position,
                                                       Consumer<Entity> consumer) {
            return createEntity(level -> {
                Entity entity = entityType.create(level);
                Objects.requireNonNull(entity, "Could not create entity of type " +
                        PonderPlatform.getEntityTypeName(entityType));
                entity.setPosRaw(position.x, position.y, position.z);
                entity.setOldPosAndRot();
                entity.lookAt(EntityAnchorArgument.Anchor.FEET, position.add(0, 0, -1));
                consumer.accept(entity);
                return entity;
            });
        }

        public ElementLink<EntityElement> createEntity(EntityType<?> entityType, Vec3 position) {
            return createEntity(entityType, position, entity -> {});
        }

        // Fix 1: use outer class addInstruction, not WorldInstructions.addInstruction
        public void removeEntity(ElementLink<EntityElement> link) {
            ExtendedSceneBuilder.this.addInstruction(scene -> {
                EntityElement resolve = scene.resolve(link);
                if (resolve != null) {
                    resolve.ifPresent(Entity::discard);
                }
            });
        }

        // --- WorldInstructions delegation ---

        @Override
        public void incrementBlockBreakingProgress(BlockPos pos) {
            delegate.incrementBlockBreakingProgress(pos);
        }

        @Override
        public void showSection(Selection selection, Direction fadeInDirection) {
            delegate.showSection(selection, fadeInDirection);
        }

        @Override
        public void showSectionAndMerge(Selection selection, Direction fadeInDirection,
                                        ElementLink<WorldSectionElement> link) {
            delegate.showSectionAndMerge(selection, fadeInDirection, link);
        }

        @Override
        public void glueBlockOnto(BlockPos position, Direction fadeInDirection, ElementLink<WorldSectionElement> link) {
            delegate.glueBlockOnto(position, fadeInDirection, link);
        }

        @Override
        public ElementLink<WorldSectionElement> showIndependentSection(Selection selection, Direction fadeInDirection) {
            return delegate.showIndependentSection(selection, fadeInDirection);
        }

        @Override
        public ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection) {
            return delegate.showIndependentSectionImmediately(selection);
        }

        @Override
        public void hideSection(Selection selection, Direction fadeOutDirection) {
            delegate.hideSection(selection, fadeOutDirection);
        }

        @Override
        public void hideIndependentSection(ElementLink<WorldSectionElement> link, Direction fadeOutDirection) {
            delegate.hideIndependentSection(link, fadeOutDirection);
        }

        @Override
        public void restoreBlocks(Selection selection) {
            delegate.restoreBlocks(selection);
        }

        @Override
        public ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection) {
            return delegate.makeSectionIndependent(selection);
        }

        @Override
        public void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation,
                                  double zRotation, int duration) {
            delegate.rotateSection(link, xRotation, yRotation, zRotation, duration);
        }

        @Override
        public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor) {
            delegate.configureCenterOfRotation(link, anchor);
        }

        @Override
        public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor) {
            delegate.configureStabilization(link, anchor);
        }

        @Override
        public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
            delegate.moveSection(link, offset, duration);
        }

        @Override
        public void setBlocks(Selection selection, BlockState state, boolean spawnParticles) {
            delegate.setBlocks(selection, state, spawnParticles);
        }

        @Override
        public void destroyBlock(BlockPos pos) {
            delegate.destroyBlock(pos);
        }

        @Override
        public void setBlock(BlockPos pos, BlockState state, boolean spawnParticles) {
            delegate.setBlock(pos, state, spawnParticles);
        }

        @Override
        public void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles) {
            delegate.replaceBlocks(selection, state, spawnParticles);
        }

        @Override
        public void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
            delegate.modifyBlock(pos, stateFunc, spawnParticles);
        }

        @Override
        public void cycleBlockProperty(BlockPos pos, Property<?> property) {
            delegate.cycleBlockProperty(pos, property);
        }

        @Override
        public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
            delegate.modifyBlocks(selection, stateFunc, spawnParticles);
        }

        @Override
        public void toggleRedstonePower(Selection selection) {
            delegate.toggleRedstonePower(selection);
        }

        @Override
        public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallBack) {
            delegate.modifyEntities(entityClass, entityCallBack);
        }

        @Override
        public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area,
                                                            Consumer<T> entityCallBack) {
            delegate.modifyEntitiesInside(entityClass, area, entityCallBack);
        }

        @Override
        public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallBack) {
            delegate.modifyEntity(link, entityCallBack);
        }

        @Override
        public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
            return delegate.createEntity(factory);
        }

        @Override
        public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
            return delegate.createItemEntity(location, motion, stack);
        }

        @Override
        public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType,
                                         Consumer<CompoundTag> consumer) {
            delegate.modifyBlockEntityNBT(selection, beType, consumer);
        }

        @Override
        public <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType,
                                                              Consumer<T> consumer) {
            delegate.modifyBlockEntity(position, beType, consumer);
        }

        @Override
        public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> type,
                                         Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
            delegate.modifyBlockEntityNBT(selection, type, consumer, reDrawBlocks);
        }
    }

    public class ExtendedSpecialInstructions implements SpecialInstructions {

        private final SpecialInstructions delegate;

        public ExtendedSpecialInstructions(SpecialInstructions delegate) {
            this.delegate = delegate;
        }

        @Override
        public ElementLink<ParrotElement> createBirb(Vec3 location, Supplier<? extends ParrotPose> pose) {
            return delegate.createBirb(location, pose);
        }

        @Override
        public void changeBirbPose(ElementLink<ParrotElement> birb, Supplier<? extends ParrotPose> pose) {
            delegate.changeBirbPose(birb, pose);
        }

        @Override
        public void movePointOfInterest(Vec3 location) {
            delegate.movePointOfInterest(location);
        }

        @Override
        public void movePointOfInterest(BlockPos location) {
            delegate.movePointOfInterest(location);
        }

        @Override
        public void rotateParrot(ElementLink<ParrotElement> link, double xRotation, double yRotation, double zRotation,
                                 int duration) {
            delegate.rotateParrot(link, xRotation, yRotation, zRotation, duration);
        }

        @Override
        public void moveParrot(ElementLink<ParrotElement> link, Vec3 offset, int duration) {
            delegate.moveParrot(link, offset, duration);
        }

        @Override
        public ElementLink<MinecartElement> createCart(Vec3 location, float angle,
                                                       MinecartElement.MinecartConstructor type) {
            return delegate.createCart(location, angle, type);
        }

        @Override
        public void rotateCart(ElementLink<MinecartElement> link, float yRotation, int duration) {
            delegate.rotateCart(link, yRotation, duration);
        }

        @Override
        public void moveCart(ElementLink<MinecartElement> link, Vec3 offset, int duration) {
            delegate.moveCart(link, offset, duration);
        }

        @Override
        public <T extends AnimatedSceneElement> void hideElement(ElementLink<T> link, Direction direction) {
            delegate.hideElement(link, direction);
        }
    }
}
