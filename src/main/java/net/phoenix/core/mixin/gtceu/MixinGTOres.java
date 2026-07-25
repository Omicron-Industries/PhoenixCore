package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGenLayers;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.DikeVeinGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.LayeredVeinGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.VeinedVeinGenerator;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTOres;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.common.data.worldgen.CrystalRoseIndicatorGenerator;
import net.phoenix.core.mixin.accessor.GTOresAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = GTOres.class, remap = false)
public abstract class MixinGTOres {

    @Inject(method = "init", at = @At("TAIL"))
    private static void phoenix$bloomCrystalRoses(CallbackInfo ci) {
        Map<ResourceLocation, GTOreDefinition> veins = GTOresAccessor.getToReRegister();
        RandomSource dummyRandom = RandomSource.create();

        veins.forEach((id, def) -> {
            Material primaryMat = null;

            for (var ig : def.indicatorGenerators()) {
                if (ig instanceof SurfaceIndicatorGenerator sig) {
                    var blockEither = sig.block();
                    if (blockEither != null && blockEither.right().isPresent()) {
                        primaryMat = blockEither.right().get();
                        break;
                    }
                }
            }

            if (primaryMat == null) {
                var generator = def.veinGenerator();

                if (generator instanceof LayeredVeinGenerator layered) {
                    var patterns = layered.getLayerPatterns();
                    if (!patterns.isEmpty()) {
                        for (var pattern : patterns) {
                            for (var layer : pattern.layers) {
                                var rolled = layer.rollBlock(dummyRandom);
                                if (rolled.right().isPresent()) {
                                    primaryMat = rolled.right().get();
                                    break;
                                }
                            }
                            if (primaryMat != null) break;
                        }
                    }
                } else if (generator instanceof DikeVeinGenerator dike) {
                    if (dike.blocks != null && !dike.blocks.isEmpty()) {
                        var rolled = dike.blocks.get(0).block();
                        if (rolled.right().isPresent()) primaryMat = rolled.right().get();
                    }
                } else if (generator instanceof VeinedVeinGenerator veined) {
                    if (veined.oreBlocks != null && !veined.oreBlocks.isEmpty()) {
                        var rolled = veined.oreBlocks.get(0).block();
                        if (rolled.right().isPresent()) primaryMat = rolled.right().get();
                    }
                }
            }

            if (primaryMat != null) {
                def.indicatorGenerators().clear();

                SurfaceIndicatorGenerator.IndicatorPlacement placement;
                if (def.layer() == WorldGenLayers.NETHERRACK) {
                    placement = SurfaceIndicatorGenerator.IndicatorPlacement.BELOW;
                } else {
                    placement = SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE;
                }

                def.indicatorGenerators().add(new CrystalRoseIndicatorGenerator(def)
                        .state(phoenixCore$getCrystalRoseState(primaryMat))
                        .placement(placement)
                        .radius(3)
                        .density(0.4f));
            }
        });
    }

    @Unique
    private static BlockState phoenixCore$getCrystalRoseState(Material material) {
        var roseEntry = GTMaterialBlocks.MATERIAL_BLOCKS.get(PhoenixMaterialFlags.crystal_rose, material);
        return roseEntry != null ? roseEntry.get().defaultBlockState() : Blocks.POPPY.defaultBlockState();
    }
}
