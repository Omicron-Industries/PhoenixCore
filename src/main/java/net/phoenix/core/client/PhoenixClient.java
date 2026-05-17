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
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchScreen;
import net.phoenix.core.integration.phantasia.PhantasiaKeybind;
import net.phoenix.core.integration.phantasia.PhantasiaScriptLoader;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen;
import net.phoenix.core.integration.phoenix_fission.api.block.PhoenixFissionEntities;
import net.phoenix.core.integration.phoenix_fission.client.NukePrimedRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.client.particles.TeslaSparkParticle;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaTowerRenderer;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;
import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PhoenixClient {

    private PhoenixClient() {}

    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(PhoenixShaders.class);
        MinecraftForge.EVENT_BUS.register(PhantasiaKeybind.class);
        MinecraftForge.EVENT_BUS.register(PhantasiaClientEvents.class);

        // Hook VocalVibrancyClient into the client tick so LiveAcousticTracker
        // fires every tick and sends bass data to the server.
        // Without this, VocalVibrancyClient.tick() is defined but never called,
        // meaning no S2CSoundMetadataPacket is ever sent and the machine never
        // receives live bass data.
        MinecraftForge.EVENT_BUS.register(VocalVibrancyClientTick.class);

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

    // Inner static class keeps the tick handler co-located with the rest of
    // PhoenixClient rather than scattering it into a separate file.
    // Registered on the FORGE event bus (not MOD bus) so it fires every game tick.
    public static class VocalVibrancyClientTick {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            // Only tick at END to avoid running twice per tick (START + END both fire)
            if (event.phase != TickEvent.Phase.END) return;
            VocalVibrancyClient.tick();
        }
    }

    // --- PARTICLE FACTORY REGISTRATION ---
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PhoenixParticles.TESLA_SPARK.get(), TeslaSparkProvider::new);
    }

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
            if (this.sprites != null) {
                particle.pickSprite(this.sprites);
            }
            return particle;
        }
    }

    // --- MODEL & SETUP LOGIC ---
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
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("spray_can_info", net.phoenix.core.client.render.SprayCanHudOverlay.HUD_SPRAY_CAN);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PhoenixCore.RECIPE_BUILDER_MENU.get(), RecipeBuilderScreen::new);
            MenuScreens.register(PhoenixCore.SOURCE_HATCH_MENU.get(), SourceHatchScreen::new);
            ItemBlockRenderTypes.setRenderLayer(PhoenixBlocks.COIL_TRUE_HEAT_STABLE.get(), RenderType.cutoutMipped());
            EntityRenderers.register(PhoenixFissionEntities.NUKE_PRIMED.get(), NukePrimedRenderer::new);
            PhantasiaScriptLoader.discoverAndLoad();
        });
    }

    private static void addPhantasiaMachine(com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition def) {
        if (!PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(def))
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(def);
    }
}