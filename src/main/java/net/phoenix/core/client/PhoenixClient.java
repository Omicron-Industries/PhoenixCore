package net.phoenix.core.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchScreen;
import net.phoenix.core.integration.phoenix_fission.api.block.PhoenixFissionEntities;
import net.phoenix.core.integration.phoenix_fission.client.NukePrimedRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.client.particles.TeslaSparkParticle;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaTowerRenderer;

import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PhoenixClient {

    private PhoenixClient() {}

    // Inside your Client Setup / Event Bus Subscriber class

    /**
     * Called from your main Mod class (PhoenixCore) constructor or CommonSetup.
     */
    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(PhoenixShaders.class);
        // GTCEu Dynamic Renders
        DynamicRenderManager.register(PhoenixCore.id("eye_of_harmony"), EyeOfHarmonyRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("artificial_star"), ArtificialStarRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("plasma_arc_furnace"), PlasmaArcFurnaceRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("custom_fluid"), CustomFluidRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("helical_fusion"), HelicalFusionRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("honey_chamber"), HoneyChamberDynamicRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("tesla_tower"), TeslaTowerRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("engine_gearbox"), EngineGearboxRenderer.TYPE);
    }

    // --- 2. PARTICLE FACTORY REGISTRATION ---
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        // Reference the common registry object
        event.registerSpriteSet(PhoenixParticles.TESLA_SPARK.get(), TeslaSparkProvider::new);
    }

    /**
     * The bridge between the Particle Engine and your TeslaSparkParticle class.
     */
    public static class TeslaSparkProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public TeslaSparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            TeslaSparkParticle particle = new TeslaSparkParticle(level, x, y, z);

            // Safety check: only pick if sprites exist
            if (this.sprites != null) {
                particle.pickSprite(this.sprites);
            }

            return particle;
        }
    }

    // --- 3. MODEL & SETUP LOGIC ---
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EyeOfHarmonyRender.SPACE_SHELL_MODEL_RL);
        event.register(EyeOfHarmonyRender.STAR_MODEL_RL);
        EyeOfHarmonyRender.ORBIT_OBJECTS_RL.forEach(event::register);
        event.register(ArtificialStarRender.ARTIFICIAL_STAR_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.RINGS_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.SPHERE_MODEL_RL);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            MenuScreens.register(PhoenixCore.SOURCE_HATCH_MENU.get(), SourceHatchScreen::new);
            ItemBlockRenderTypes.setRenderLayer(PhoenixBlocks.COIL_TRUE_HEAT_STABLE.get(), RenderType.cutoutMipped());
            EntityRenderers.register(PhoenixFissionEntities.NUKE_PRIMED.get(), NukePrimedRenderer::new);
        });
    }
}
