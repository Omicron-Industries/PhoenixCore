package net.phoenix.core.client.worldfx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Registers all Phoenix world-effect shaders via {@link RegisterShadersEvent}.
 *
 * <p>Subscribe this in {@code PhoenixClient} on the MOD bus:
 * <pre>{@code
 * modBus.addListener(WorldFXShaders::onRegisterShaders);
 * }</pre>
 *
 * <p>All shader JSON files live in {@code assets/phoenixcore/shaders/program/}.
 */
public final class WorldFXShaders {

    private WorldFXShaders() {}

    private static final Logger LOGGER = LogManager.getLogger();

    // ── Shader instances (set by RegisterShadersEvent callbacks) ──────────────

    /** Gravitational lensing + accretion disk for black holes. */
    public static ShaderInstance BLACK_HOLE;

    /** Procedural nebula rendered as a full-sky dome. */
    public static ShaderInstance NEBULA;

    /** Dynamic atmosphere colour grading (soul level, tint, vignette). */
    public static ShaderInstance ATMOSPHERE_GRADE;

    // ── Registration ──────────────────────────────────────────────────────────

    public static void onRegisterShaders(RegisterShadersEvent event) {
        register(event, "phoenixcore:phoenix_black_hole",    DefaultVertexFormat.POSITION, s -> BLACK_HOLE       = s);
        register(event, "phoenixcore:phoenix_nebula",        DefaultVertexFormat.POSITION, s -> NEBULA           = s);
        register(event, "phoenixcore:phoenix_atmosphere_grade", DefaultVertexFormat.POSITION, s -> ATMOSPHERE_GRADE = s);
    }

    private static void register(RegisterShadersEvent event,
                                 String name,
                                 com.mojang.blaze3d.vertex.VertexFormat format,
                                 java.util.function.Consumer<ShaderInstance> onLoad) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), name, format),
                    onLoad);
        } catch (IOException e) {
            LOGGER.error("[PhoenixCore/WorldFX] Failed to register shader '{}': {}", name, e.getMessage());
        }
    }
}
