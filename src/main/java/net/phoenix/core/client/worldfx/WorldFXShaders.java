package net.phoenix.core.client.worldfx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class WorldFXShaders {

    private WorldFXShaders() {}

    private static final Logger LOGGER = LogManager.getLogger();

    public static ShaderInstance BLACK_HOLE;

    public static ShaderInstance NEBULA;

    public static ShaderInstance ATMOSPHERE_GRADE;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        register(event, "phoenixcore:phoenix_black_hole", DefaultVertexFormat.POSITION, s -> BLACK_HOLE = s);
        register(event, "phoenixcore:phoenix_nebula", DefaultVertexFormat.POSITION, s -> NEBULA = s);
        register(event, "phoenixcore:phoenix_atmosphere_grade", DefaultVertexFormat.POSITION,
                s -> ATMOSPHERE_GRADE = s);
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
