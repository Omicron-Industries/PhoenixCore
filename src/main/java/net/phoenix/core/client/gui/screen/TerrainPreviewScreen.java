package net.phoenix.core.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.common.worldgen.PhoenixTerrainNoise;
import net.phoenix.core.common.worldgen.PhoenixTerrainPresets;
import net.phoenix.core.common.worldgen.TerrainProfile;
import net.phoenix.core.common.worldgen.TerrainSampler;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongFunction;
import java.util.stream.IntStream;

@OnlyIn(Dist.CLIENT)
public class TerrainPreviewScreen extends Screen {

    private static final double PREVIEW_FRAC = 0.65;
    private static final int PANEL_PAD = 8;
    private static final int CTRL_H = 20;
    private static final int CTRL_GAP = 4;

    private static final ResourceLocation TEXTURE_RL = new ResourceLocation("phoenixcore", "terrain_preview");
    private DynamicTexture dynamicTexture;
    private NativeImage nativeImage;
    private final AtomicBoolean generating = new AtomicBoolean(false);
    private volatile boolean textureNeedsUpload = false;
    private volatile boolean closed = false;

    private int previewW;
    private int previewH;

    private TerrainProfile currentProfile;
    private boolean showVeins = true;

    private final List<Map.Entry<String, LongFunction<TerrainProfile>>> presets = PhoenixTerrainPresets.all();
    private int presetIndex = 0;

    private PreviewSlider sliderBaseY;
    private PreviewSlider sliderAmplitude;
    private PreviewSlider sliderFrequency;
    private PreviewSlider sliderOctaves;
    private ToggleCheckbox cbCaves;
    private ToggleCheckbox cbVolumetric;
    private ToggleCheckbox cbVeins;
    private EditBox seedField;

    private boolean showExport = false;
    private String exportCode = "";

    private record OreLayer(String name, int minY, int maxY, int argb, double scale, double threshold, long seedOff) {}

    private static final OreLayer[] ORE_LAYERS = {
            new OreLayer("Coal", -1, 256, 0xFF555555, 0.08, 0.130, 10L),
            new OreLayer("Iron", -24, 56, 0xFFD4916A, 0.10, 0.115, 20L),
            new OreLayer("Copper", -16, 112, 0xFFE07830, 0.09, 0.120, 30L),
            new OreLayer("Gold", -64, 32, 0xFFFFD700, 0.12, 0.080, 40L),
            new OreLayer("Lapis", -32, 64, 0xFF3355CC, 0.13, 0.070, 50L),
            new OreLayer("Diamond", -64, 16, 0xFF40EEEE, 0.14, 0.065, 60L),
            new OreLayer("Ancient Debris", 8, 22, 0xFFAA44AA, 0.18, 0.055, 70L),
    };

    public TerrainPreviewScreen() {
        super(Component.literal("Terrain Preview"));
        currentProfile = PhoenixTerrainPresets.plains(0L);
    }

    @Override
    protected void init() {
        closed = false;
        previewW = (int) (this.width * PREVIEW_FRAC);
        previewH = this.height;

        if (nativeImage != null) nativeImage.close();
        nativeImage = new NativeImage(NativeImage.Format.RGBA, previewW, previewH, false);

        if (dynamicTexture == null) {
            dynamicTexture = new DynamicTexture(nativeImage);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_RL, dynamicTexture);
        } else {
            dynamicTexture.setPixels(nativeImage);
        }

        int panelX = previewW + PANEL_PAD;
        int panelW = this.width - panelX - PANEL_PAD;
        int cy = PANEL_PAD + 20;

        int arrowW = 20;
        int presetLabelW = panelW - arrowW * 2 - 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            presetIndex = (presetIndex - 1 + presets.size()) % presets.size();
            applyPreset();
        }).bounds(panelX, cy, arrowW, CTRL_H).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            presetIndex = (presetIndex + 1) % presets.size();
            applyPreset();
        }).bounds(panelX + arrowW + presetLabelW + 4, cy, arrowW, CTRL_H).build());
        cy += CTRL_H + CTRL_GAP + 2;

        sliderBaseY = new PreviewSlider(panelX, cy, panelW, CTRL_H,
                "Base Y", currentProfile.minY(), currentProfile.maxY(), currentProfile.baseY());
        addRenderableWidget(sliderBaseY);
        cy += CTRL_H + CTRL_GAP;

        sliderAmplitude = new PreviewSlider(panelX, cy, panelW, CTRL_H,
                "Amplitude", 1, 300, currentProfile.amplitude());
        addRenderableWidget(sliderAmplitude);
        cy += CTRL_H + CTRL_GAP;

        sliderFrequency = new PreviewSlider(panelX, cy, panelW, CTRL_H,
                "Freq ×1000", 1, 50, currentProfile.frequency() * 1000.0);
        addRenderableWidget(sliderFrequency);
        cy += CTRL_H + CTRL_GAP;

        sliderOctaves = new PreviewSlider(panelX, cy, panelW, CTRL_H,
                "Octaves", 1, 8, currentProfile.octaves());
        addRenderableWidget(sliderOctaves);
        cy += CTRL_H + CTRL_GAP + 4;

        cbCaves = new ToggleCheckbox(panelX, cy, panelW, CTRL_H,
                Component.literal("Caves"), currentProfile.caves());
        addRenderableWidget(cbCaves);
        cy += CTRL_H + CTRL_GAP;

        cbVolumetric = new ToggleCheckbox(panelX, cy, panelW, CTRL_H,
                Component.literal("Volumetric"), currentProfile.volumetric());
        addRenderableWidget(cbVolumetric);
        cy += CTRL_H + CTRL_GAP;

        cbVeins = new ToggleCheckbox(panelX, cy, panelW, CTRL_H,
                Component.literal("Show Veins"), showVeins) {

            @Override
            public void onPress() {
                super.onPress();
                showVeins = isChecked();
                scheduleRender(currentProfile);
            }
        };
        addRenderableWidget(cbVeins);
        cy += CTRL_H + CTRL_GAP + 4;

        seedField = new EditBox(this.font, panelX, cy, panelW, CTRL_H, Component.literal("Seed"));
        seedField.setMaxLength(20);
        seedField.setValue(String.valueOf(currentProfile.seed()));
        addRenderableWidget(seedField);
        cy += CTRL_H + CTRL_GAP + 4;

        addRenderableWidget(Button.builder(Component.literal("Regenerate"), b -> regenerate())
                .bounds(panelX, cy, panelW, CTRL_H).build());
        cy += CTRL_H + CTRL_GAP;

        addRenderableWidget(Button.builder(Component.literal("Export Code"), b -> openExport())
                .bounds(panelX, cy, panelW, CTRL_H).build());

        scheduleRender(currentProfile);
    }

    private void applyPreset() {
        long seed = parseSeed();
        TerrainProfile preset = presets.get(presetIndex).getValue().apply(seed);
        currentProfile = preset;
        sliderBaseY.setRawValue(preset.baseY());
        sliderAmplitude.setRawValue(preset.amplitude());
        sliderFrequency.setRawValue(preset.frequency() * 1000.0);
        sliderOctaves.setRawValue(preset.octaves());
        cbCaves.setValue(preset.caves());
        cbVolumetric.setValue(preset.volumetric());
        scheduleRender(preset);
    }

    private void regenerate() {
        long seed = parseSeed();
        currentProfile = TerrainProfile.builder(presets.get(presetIndex).getKey())
                .seed(seed)
                .minY(currentProfile.minY()).maxY(currentProfile.maxY()).seaLevel(currentProfile.seaLevel())
                .baseY(sliderBaseY.getRawValue())
                .amplitude(sliderAmplitude.getRawValue())
                .frequency(sliderFrequency.getRawValue() / 1000.0)
                .octaves((int) Math.round(sliderOctaves.getRawValue()))
                .caves(cbCaves.isChecked())
                .volumetric(cbVolumetric.isChecked())
                .build();
        scheduleRender(currentProfile);
    }

    private long parseSeed() {
        try {
            return Long.parseLong(seedField.getValue().trim());
        } catch (NumberFormatException e) {
            return seedField.getValue().hashCode();
        }
    }

    private void scheduleRender(TerrainProfile profile) {
        if (generating.getAndSet(true)) return;
        int imgW = previewW;
        int imgH = previewH;
        boolean veins = this.showVeins;

        CompletableFuture.runAsync(() -> {
            try {
                renderTerrainToImage(profile, imgW, imgH, veins);
                textureNeedsUpload = true;
            } finally {
                generating.set(false);
            }
        });
    }

    private void renderTerrainToImage(TerrainProfile profile, int imgW, int imgH, boolean withVeins) {
        TerrainSampler sampler = profile.sampler();
        int minY = profile.minY(), maxY = profile.maxY(), seaLevel = profile.seaLevel();
        int yRange = maxY - minY;

        final TerrainSampler[] veinSamplers;
        if (withVeins) {
            veinSamplers = new TerrainSampler[ORE_LAYERS.length];
            for (int i = 0; i < ORE_LAYERS.length; i++) {
                veinSamplers[i] = PhoenixTerrainNoise.vein(
                        profile.seed() + ORE_LAYERS[i].seedOff(),
                        ORE_LAYERS[i].scale(),
                        ORE_LAYERS[i].threshold());
            }
        } else {
            veinSamplers = null;
        }

        int[] pixels = new int[imgW * imgH];

        IntStream.range(0, imgW).parallel().forEach(px -> {
            int worldX = (int) (px / (double) imgW * 256) - 128;

            boolean[] solidCol = new boolean[imgH];
            int surf = minY;
            boolean foundSurface = false;

            for (int py = 0; py < imgH; py++) {
                int worldY = maxY - (int) (py / (double) imgH * yRange);
                solidCol[py] = sampler.sample(worldX, worldY, 0) > 0;
                if (!foundSurface && solidCol[py]) {
                    surf = worldY;
                    foundSurface = true;
                }
            }

            final int surfFinal = surf;
            for (int py = 0; py < imgH; py++) {
                int worldY = maxY - (int) (py / (double) imgH * yRange);
                pixels[py * imgW + px] = colorPixel(worldX, worldY, solidCol[py],
                        surfFinal, seaLevel, minY, maxY, veinSamplers);
            }
        });

        NativeImage img = nativeImage;
        if (img == null || closed) return;
        synchronized (img) {
            if (closed) return;
            for (int py = 0; py < imgH; py++) {
                for (int px = 0; px < imgW; px++) {
                    img.setPixelRGBA(px, py, pixels[py * imgW + px]);
                }
            }
        }
    }

    private int colorPixel(int worldX, int worldY, boolean solid, int surfY,
                           int seaLevel, int minY, int maxY, TerrainSampler[] veinSamplers) {
        if (!solid) {
            if (worldY >= seaLevel) {
                float t = (float) (worldY - seaLevel) / (maxY - seaLevel);
                int r = (int) (0x87 + (0xC0 - 0x87) * (1 - t));
                int gv = (int) (0xCE + (0xE8 - 0xCE) * (1 - t));
                int b = (int) (0xEB + (0xF8 - 0xEB) * (1 - t));
                return toABGR(255, r, gv, b);
            } else {
                float depth = (float) (seaLevel - worldY) / Math.max(1, seaLevel - minY);
                return toABGR(200,
                        (int) (0x1A * (1 - depth * 0.5f)),
                        (int) (0x3A * (1 - depth * 0.4f)),
                        (int) (0x6B + 0x40 * depth));
            }
        }

        if (veinSamplers != null) {
            for (int i = 0; i < ORE_LAYERS.length; i++) {
                OreLayer ore = ORE_LAYERS[i];
                if (worldY >= ore.minY() && worldY <= ore.maxY() && veinSamplers[i].sample(worldX, worldY, 0) < 0) {
                    return argbToABGR(ore.argb());
                }
            }
        }

        int distFromSurface = surfY - worldY;
        int totalDepth = surfY - minY;
        float depthFrac = totalDepth > 0 ? (float) distFromSurface / totalDepth : 0;

        if (distFromSurface == 0) {
            return toABGR(255, 0x5A, 0x7C, 0x3A);
        } else if (distFromSurface <= 4) {
            float blend = distFromSurface / 4.0f;
            return toABGR(255, 0x7B, (int) (0x52 - blend * 10), (int) (0x31 - blend * 5));
        } else if (depthFrac < 0.8f) {
            float t = depthFrac / 0.8f;
            int v = (int) (0x88 - (0x88 - 0x44) * t);
            return toABGR(255, v, v, v);
        } else {
            float t = (depthFrac - 0.8f) / 0.2f;
            int r = (int) (0x44 + 0x10 * t);
            int gv = (int) (0x44 - 0x30 * t);
            int bv = (int) (0x44 - 0x30 * t);
            return toABGR(255, Math.max(0, r), Math.max(0, gv), Math.max(0, bv));
        }
    }

    private static int toABGR(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    private static int argbToABGR(int argb) {
        return toABGR((argb >> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    private void openExport() {
        TerrainProfile p = currentProfile;
        exportCode = String.format("""
                TerrainProfile.builder("%s")
                    .seed(%dL)
                    .minY(%d).maxY(%d).seaLevel(%d)
                    .baseY(%.1f).amplitude(%.1f)
                    .frequency(%.5f).octaves(%d)
                    .caves(%b).volumetric(%b)
                    .build();
                """,
                p.name(), p.seed(), p.minY(), p.maxY(), p.seaLevel(),
                p.baseY(), p.amplitude(), p.frequency(), p.octaves(),
                p.caves(), p.volumetric());
        showExport = true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (textureNeedsUpload && !generating.get()) {
            textureNeedsUpload = false;
            synchronized (nativeImage) {
                dynamicTexture.upload();
            }
        }

        g.fill(0, 0, this.width, this.height, 0xFF101010);
        g.blit(TEXTURE_RL, 0, 0, 0, 0, previewW, previewH, previewW, previewH);

        if (generating.get()) {
            g.fill(0, 0, previewW, 12, 0xAA000000);
            g.drawString(this.font, "Generating...", 4, 2, 0xFFFF55);
        }

        TerrainProfile p = currentProfile;
        int seaLineY = (int) ((double) (p.maxY() - p.seaLevel()) / (p.maxY() - p.minY()) * previewH);
        g.fill(0, seaLineY, previewW, seaLineY + 1, 0xAA0066FF);

        int panelX = previewW;
        g.fill(panelX, 0, this.width, this.height, 0xCC1A1A2E);
        g.drawCenteredString(this.font, "§b§lTerrain Preview",
                panelX + (this.width - panelX) / 2, PANEL_PAD, 0xFFFFFF);

        int panelW = this.width - panelX - PANEL_PAD;
        int arrowW = 20;
        String presetName = presets.get(presetIndex).getKey();
        int presetLabelW = panelW - arrowW * 2 - 4;
        g.drawCenteredString(this.font, presetName,
                panelX + PANEL_PAD + arrowW + presetLabelW / 2 + 2,
                PANEL_PAD + 20 + 6, 0xFFFFAA);

        super.render(g, mouseX, mouseY, partialTick);

        if (showVeins) {
            int legendY = this.height - ORE_LAYERS.length * 10 - 6;
            for (OreLayer ore : ORE_LAYERS) {
                int oreR = (ore.argb() >> 16) & 0xFF;
                int oreG = (ore.argb() >> 8) & 0xFF;
                int oreB = ore.argb() & 0xFF;
                int swatchColor = 0xFF000000 | (oreR << 16) | (oreG << 8) | oreB;
                g.fill(panelX + PANEL_PAD, legendY, panelX + PANEL_PAD + 6, legendY + 8, swatchColor);
                g.drawString(this.font, ore.name() + " [" + ore.minY() + "→" + ore.maxY() + "]",
                        panelX + PANEL_PAD + 9, legendY, swatchColor);
                legendY += 10;
            }
        }

        if (showExport) {
            int ox = 20, oy = 20, ow = this.width - 40, oh = this.height - 40;
            g.fill(ox, oy, ox + ow, oy + oh, 0xEE0A0A14);
            g.drawString(this.font, "§a§lJava Code Snippet  (press Esc to close)", ox + 6, oy + 6, 0xFFFFFF);
            String[] lines = exportCode.split("\n");
            for (int i = 0; i < lines.length; i++) {
                g.drawString(this.font, lines[i], ox + 6, oy + 20 + i * 10, 0xCCCCCC);
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (showExport) {
            if (key == 256) {
                showExport = false;
                return true;
            }
            return false;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closed = true;
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_RL);
            dynamicTexture = null;
        }
        if (nativeImage != null) {
            nativeImage.close();
            nativeImage = null;
        }
        super.onClose();
    }

    private static class PreviewSlider extends AbstractSliderButton {

        private final String label;
        private final double min;
        private final double max;

        PreviewSlider(int x, int y, int w, int h, String label, double min, double max, double initial) {
            super(x, y, w, h, Component.empty(), (initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double v = getRawValue();
            String fmt = (max - min <= 10) ? "%.0f" : (max - min <= 300) ? "%.1f" : "%.3f";
            setMessage(Component.literal(label + ": " + String.format(fmt, v)));
        }

        @Override
        protected void applyValue() {}

        public double getRawValue() {
            return min + this.value * (max - min);
        }

        public void setRawValue(double v) {
            this.value = Math.max(0, Math.min(1, (v - min) / (max - min)));
            updateMessage();
        }
    }

    private static class ToggleCheckbox extends Button {

        private boolean checked;

        ToggleCheckbox(int x, int y, int w, int h, Component label, boolean initial) {
            super(x, y, w, h, buildMsg(label.getString(), initial), b -> {}, DEFAULT_NARRATION);
            this.checked = initial;
        }

        @Override
        public void onPress() {
            checked = !checked;
            String raw = getMessage().getString();
            String labelPart = raw.length() > 4 ? raw.substring(4) : raw;
            setMessage(buildMsg(labelPart.trim(), checked));
        }

        private static Component buildMsg(String label, boolean checked) {
            return Component.literal((checked ? "[x] " : "[ ] ") + label);
        }

        public boolean isChecked() {
            return checked;
        }

        public void setValue(boolean v) {
            if (checked != v) onPress();
        }
    }
}
