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
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.ConfluxRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.conflux.client.ConfluxEditorCommand;
import net.phoenix.core.conflux.tools.capture.SpriteCaptureRegistry;
import net.phoenix.core.conflux.tools.capture.ExportSpritesCommand;
import net.phoenix.core.client.command.TerrainPreviewCommand;
import net.phoenix.core.conflux.tools.capture.bakers.AxiomVoidBaker;
import net.phoenix.core.conflux.tools.capture.bakers.AxiomPhoenixBaker;
import net.phoenix.core.conflux.tools.capture.bakers.AxiomSculkBaker;
import net.phoenix.core.conflux.tools.capture.bakers.AxiomSealedBaker;

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
        MinecraftForge.EVENT_BUS.register(VocalVibrancyClientTick.class);
        modBus.addListener(PhoenixClient::registerDynamicPipeModels);
        modBus.addListener(WorldFXShaders::onRegisterShaders);
    }

    @SubscribeEvent
    public static void registerDynamicPipeModels(RegisterDynamicResourcesEvent event) {
        GTBlockstateProvider provider = GTBlockstateProvider.getCurrentProvider();
        if (provider == null) return;
        for (ConfluxDataType dt : ConfluxDataType.values()) {
            ConfluxRegistry.PIPES.get(dt).get()
                    .createPipeModel(provider)
                    .dynamicModel();
        }
    }

    public static class VocalVibrancyClientTick {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            VocalVibrancyClient.tick();
        }
    }

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
        event.registerAboveAll("spray_can_info", net.phoenix.core.client.renderer.SprayCanHudOverlay.HUD_SPRAY_CAN);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        DynamicRenderManager.register(PhoenixCore.id("eye_of_harmony"), EyeOfHarmonyRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("artificial_star"), ArtificialStarRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("plasma_arc_furnace"), PlasmaArcFurnaceRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("custom_fluid"), CustomFluidRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("helical_fusion"), HelicalFusionRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("honey_chamber"), HoneyChamberDynamicRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("tesla_tower"), TeslaTowerRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("engine_gearbox"), EngineGearboxRenderer.TYPE);

        event.enqueueWork(() -> {
            MenuScreens.register(PhoenixCore.RECIPE_BUILDER_MENU.get(), RecipeBuilderScreen::new);
        });

        // Register sprite bakers
        SpriteCaptureRegistry.register(new AxiomVoidBaker());
        SpriteCaptureRegistry.register(new AxiomPhoenixBaker());
        SpriteCaptureRegistry.register(new AxiomSculkBaker());
        SpriteCaptureRegistry.register(new AxiomSealedBaker());
    }

    // Nested subscriber specifically targeting Bus.FORGE for client events
    @Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            ConfluxEditorCommand.register(event);
            ExportSpritesCommand.register(event);
            TerrainPreviewCommand.register(event);
        }
    }
}
