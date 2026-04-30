package net.phoenix.core.integration.ponder.particles;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.core.particles.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleInstructions {

    private final SceneBuilder scene;

    public ParticleInstructions(SceneBuilder scene) {
        this.scene = scene;
    }

    public void simple(int ticks, ParticleType<?> type, Vec3 pos) {
        if (type instanceof SimpleParticleType simple) {
            scene.addInstruction(ponderScene -> {
                for (int i = 0; i < ticks; i++) {
                    ponderScene.getWorld().addParticle(simple, pos.x, pos.y, pos.z, 0, 0, 0);
                }
            });
            return;
        }
        throw new IllegalArgumentException("Particle type is null or not simple.");
    }

    public void item(int ticks, ItemStack item, Vec3 pos) {
        ItemParticleOption options = new ItemParticleOption(ParticleTypes.ITEM, item);
        scene.addInstruction(ponderScene -> {
            for (int i = 0; i < ticks; i++) {
                ponderScene.getWorld().addParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        });
    }

    public void block(int ticks, BlockState blockState, Vec3 pos) {
        BlockParticleOption options = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
        scene.addInstruction(ponderScene -> {
            for (int i = 0; i < ticks; i++) {
                ponderScene.getWorld().addParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        });
    }
}
