package net.phoenix.core.integration.matter_manipulater.api;

import net.minecraft.network.chat.Component;

import lombok.Getter;

@Getter
public enum PhoenixManipulatorMode {

    LINE("Line", "Places pipes in a direct line", 0xFF00E5FF),
    WALL("Wall", "Creates a 2D plane of pipes", 0xFFFFFF00),
    GRID("Grid", "Fills a 3D volume (Cuboid)", 0xFFFFFFFF),
    CONNECT_ONLY("Connect", "Fixes/Forces connections", 0xFF00FF00),
    DISCONNECT("Disconnect", "Severs all connections", 0xFFFF0000);

    private final String name;
    private final String description;
    private final int color;

    PhoenixManipulatorMode(String name, String description, int color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public Component getDisplayName() {
        return Component.literal("Mode: " + name).withStyle(s -> s.withColor(this.color & 0xFFFFFF));
    }

    public PhoenixManipulatorMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public float[] getRGB() {
        return new float[] {
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f
        };
    }
}
