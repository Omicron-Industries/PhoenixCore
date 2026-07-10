package net.phoenix.core.client.gui.screen;

import com.mojang.blaze3d.platform.NativeImage;
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
import net.phoenix.core.common.worldgen.PhoenixTerrainPresets;
import net.phoenix.core.common.worldgen.TerrainProfile;
import net.phoenix.core.common.worldgen.TerrainSampler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongFunction;

@OnlyIn(Dist.CLIENT)
public class TerrainPreviewScreen extends Screen {

    // ── layout constants ──────────────────────────────────────────────────────
    private static final double PREVIEW_FRAC = 0.65; // left fraction for terrain image
    private static final int PANEL_PAD = 8;
    private static final int CTRL_H = 20;
    private static final int CTRL_GAP = 4;

    // ── GPU texture ───────────────────────────────────────────────────────────
    private static final ResourceLocation TEXTURE_RL =
            new ResourceLocation("phoenixcore", "terrain_preview");
    private DynamicTexture dynamicTexture;
    private NativeImage nativeImage;
    private final AtomicBoolean generating = new AtomicBoolean(false);
    private volatile boolean textureNeedsUpload = false;

    // ── preview image size (set in init) ──────────────────────────────────────
    private int previewW;
    private int previewH;

    // ── current profile ───────────────────────────────────────────────────────
    private TerrainProfile currentProfile;

    // ── preset selector ───────────────────────────────────────────────────────
    private final List<Map.Entry<String, LongFunction<TerrainProfile>>> presets =
            PhoenixTerrainPresets.all();
    private int presetIndex = 0;

    // ── controls (initialised in init) ────────────────────────────────────────
    private PreviewSlider sliderBaseY;
    private PreviewSlider sliderAmplitude;
    private PreviewSlider sliderFrequency;
    private PreviewSlider sliderOctaves;
    private ToggleCheckbox cbCaves;
    private ToggleCheckbox cbVolumetric;
    private EditBox seedField;

    // ── export overlay ────────────────────────────────────────────────────────
    private boolean showExport = false;
    private String exportCode = "";

    // ─────────────────────────────────────────────────────────────────────────

    public TerrainPreviewScreen() {
        super(Component.literal("Terrain Preview"));
        currentProfile = PhoenixTerrainPresets.plains(0L);
    }

    @Override
    protected void init() {
        previewW = (int) (this.width * PREVIEW_FRAC);
        previewH = this.height;

        // Allocate / reallocate the NativeImage & DynamicTexture
        if (nativeImage != null) nativeImage.close();
        nativeImage = new NativeImage(NativeImage.Format.RGBA, previewW, previewH, false);

        if (dynamicTexture == null) {
            dynamicTexture = new DynamicTexture(nativeImage);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_RL, dynamicTexture);
        }

        int panelX = previewW + PANEL_PAD;
        int panelW = this.width - panelX - PANEL_PAD;
        int cy = PANEL_PAD + 20; // start below title

        // ── Preset selector ──
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

        // ── Sliders ──
        int totalYRange = currentProfile.maxY() - currentProfile.minY();

        sliderBaseY = new PreviewSlider(panelX, cy, panelW, CTRL_H,
                "Base Y", currentProfile.minY(), currentProfile.maxY(),
                currentProfile.baseY());
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

        // ── Checkboxes ──
        cbCaves = new ToggleCheckbox(panelX, cy, panelW, CTRL_H,
                Component.literal("Caves"), currentProfile.caves());
        addRenderableWidget(cbCaves);
        cy += CTRL_H + CTRL_GAP;

        cbVolumetric = new ToggleCheckbox(panelX, cy, panelW, CTRL_H,
                Component.literal("Volumetric"), currentProfile.volumetric());
        addRenderableWidget(cbVolumetric);
        cy += CTRL_H + CTRL_GAP + 4;

        // ── Seed field ──
        seedField = new EditBox(this.font, panelX, cy, panelW, CTRL_H,
                Component.literal("Seed"));
        seedField.setMaxLength(20);
        seedField.setValue(String.valueOf(currentProfile.seed()));
        addRenderableWidget(seedField);
        cy += CTRL_H + CTRL_GAP + 4;

        // ── Buttons ──
        addRenderableWidget(Button.builder(Component.literal("Regenerate"), b -> regenerate())
                .bounds(panelX, cy, panelW, CTRL_H).build());
        cy += CTRL_H + CTRL_GAP;

        addRenderableWidget(Button.builder(Component.literal("Export Code"), b -> openExport())
                .bounds(panelX, cy, panelW, CTRL_H).build());

        // Kick off initial render
        scheduleRender(currentProfile);
    }

    // ── Preset application ────────────────────────────────────────────────────

    private void applyPreset() {
        long seed = parseSeed();
        TerrainProfile preset = presets.get(presetIndex).getValue().apply(seed);
        currentProfile = preset;

        // Sync controls
        sliderBaseY.setRawValue(preset.baseY());
        sliderAmplitude.setRawValue(preset.amplitude());
        sliderFrequency.setRawValue(preset.frequency() * 1000.0);
        sliderOctaves.setRawValue(preset.octaves());
        cbCaves.setValue(preset.caves());
        cbVolumetric.setValue(preset.volumetric());

        scheduleRender(preset);
    }

    // ── Regenerate from controls ──────────────────────────────────────────────

    private void regenerate() {
        long seed = parseSeed();
        double baseY = sliderBaseY.getRawValue();
        double amplitude = sliderAmplitude.getRawValue();
        double frequency = sliderFrequency.getRawValue() / 1000.0;
        int octaves = (int) Math.round(sliderOctaves.getRawValue());
        boolean caves = cbCaves.isChecked();
        boolean volumetric = cbVolumetric.isChecked();

        currentProfile = TerrainProfile.builder(presets.get(presetIndex).getKey())
                .seed(seed)
                .minY(currentProfile.minY())
                .maxY(currentProfile.maxY())
                .seaLevel(currentProfile.seaLevel())
                .baseY(baseY)
                .amplitude(amplitude)
                .frequency(frequency)
                .octaves(octaves)
                .caves(caves)
                .volumetric(volumetric)
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

    // ── Async render ──────────────────────────────────────────────────────────

    private void scheduleRender(TerrainProfile profile) {
        if (generating.getAndSet(true)) return; // debounce

        int imgW = previewW;
        int imgH = previewH;

        CompletableFuture.runAsync(() -> {
            try {
                renderTerrainToImage(profile, imgW, imgH);
                textureNeedsUpload = true;
            } finally {
                generating.set(false);
            }
        });
    }

    private void renderTerrainToImage(TerrainProfile profile, int imgW, int imgH) {
        TerrainSampler sampler = profile.sampler();
        int minY = profile.minY();
        int maxY = profile.maxY();
        int seaLevel = profile.seaLevel();
        int yRange = maxY - minY;

        // X axis: -128 to +128 mapped across imgW pixels
        // Y axis: maxY at top, minY at bottom

        // First pass: find the surface Y for each column (for colouring)
        int[] surfaceY = new int[imgW];
        for (int px = 0; px < imgW; px++) {
            int worldX = (int) (px / (double) imgW * 256) - 128;
            surfaceY[px] = minY; // default if nothing solid
            for (int worldY = maxY; worldY >= minY; worldY--) {
                if (sampler.sample(worldX, worldY, 0) > 0) {
                    surfaceY[px] = worldY;
                    break;
                }
            }
        }

        // Second pass: colour each pixel
        NativeImage img = nativeImage;
        if (img == null) return;

        synchronized (img) {
            for (int px = 0; px < imgW; px++) {
                int worldX = (int) (px / (double) imgW * 256) - 128;
                int surf = surfaceY[px];

                for (int py = 0; py < imgH; py++) {
                    // py=0 is top (maxY), py=imgH-1 is bottom (minY)
                    int worldY = maxY - (int) (py / (double) imgH * yRange);

                    boolean solid = sampler.sample(worldX, worldY, 0) > 0;
                    int abgr;

                    if (!solid) {
                        // Air
                        if (worldY >= seaLevel) {
                            // Sky gradient: deep sky blue at top, lighter lower
                            float t = (float) (worldY - seaLevel) / (maxY - seaLevel);
                            // #87CEEB at top, #C0E8F8 at sea level
                            int r = (int) (0x87 + (0xC0 - 0x87) * (1 - t));
                            int g = (int) (0xCE + (0xE8 - 0xCE) * (1 - t));
                            int b = (int) (0xEB + (0xF8 - 0xEB) * (1 - t));
                            abgr = toABGR(255, r, g, b);
                        } else {
                            // Underwater: dark blue #1A3A6B
                            float depth = (float) (seaLevel - worldY) / (seaLevel - minY);
                            int r = (int) (0x1A * (1 - depth * 0.5f));
                            int g = (int) (0x3A * (1 - depth * 0.4f));
                            int b = (int) (0x6B + (0x40) * depth);
                            abgr = toABGR(200, r, g, b);
                        }
                    } else {
                        // Solid
                        int distFromSurface = surf - worldY;
                        int totalDepth = surf - minY;
                        float depthFrac = totalDepth > 0 ? (float) distFromSurface / totalDepth : 0;

                        if (distFromSurface == 0) {
                            // Surface: green #5A7C3A
                            abgr = toABGR(255, 0x5A, 0x7C, 0x3A);
                        } else if (distFromSurface <= 4) {
                            // Near-surface: brown/dirt #7B5231
                            float blend = distFromSurface / 4.0f;
                            int r = (int) (0x7B);
                            int g = (int) (0x52 - blend * 10);
                            int b = (int) (0x31 - blend * 5);
                            abgr = toABGR(255, r, g, b);
                        } else if (depthFrac < 0.8f) {
                            // Mid-depth: gray gradient #888888 → #444444
                            float t = depthFrac / 0.8f;
                            int v = (int) (0x88 - (0x88 - 0x44) * t);
                            abgr = toABGR(255, v, v, v);
                        } else {
                            // Very deep: near-black with slight red tint
                            float t = (depthFrac - 0.8f) / 0.2f;
                            int r = (int) (0x44 - 0x22 * t + 0x22 * t); // slight red
                            int gv = (int) (0x44 - 0x30 * t);
                            int bv = (int) (0x44 - 0x30 * t);
                            r = (int) (0x44 - 0x10 * t + 0x20 * t);
                            abgr = toABGR(255, Math.max(0, r), Math.max(0, gv), Math.max(0, bv));
                        }
                    }

                    img.setPixelRGBA(px, py, abgr);
                }
            }
        }
    }

    /** Pack r,g,b,a into ABGR int (NativeImage RGBA format stores as ABGR in memory). */
    private static int toABGR(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    // ── Export ────────────────────────────────────────────────────────────────

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

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Upload texture if background thread finished
        if (textureNeedsUpload && !generating.get()) {
            textureNeedsUpload = false;
            synchronized (nativeImage) {
                dynamicTexture.upload();
            }
        }

        // Dark background for whole screen
        g.fill(0, 0, this.width, this.height, 0xFF101010);

        // ── Preview pane ──
        g.blit(TEXTURE_RL, 0, 0, 0, 0, previewW, previewH, previewW, previewH);

        // "Generating..." overlay
        if (generating.get()) {
            g.fill(0, 0, previewW, 12, 0xAA000000);
            g.drawString(this.font, "Generating...", 4, 2, 0xFFFF55);
        }

        // Sea level line
        TerrainProfile p = currentProfile;
        int seaLineY = (int) ((double) (p.maxY() - p.seaLevel()) / (p.maxY() - p.minY()) * previewH);
        g.fill(0, seaLineY, previewW, seaLineY + 1, 0x880066FF);

        // ── Right panel background ──
        int panelX = previewW;
        g.fill(panelX, 0, this.width, this.height, 0xCC1A1A2E);

        // Title
        g.drawCenteredString(this.font, "§b§lTerrain Preview", panelX + (this.width - panelX) / 2, PANEL_PAD, 0xFFFFFF);

        // Preset name centred between arrows
        int panelW = this.width - panelX - PANEL_PAD;
        int arrowW = 20;
        String presetName = presets.get(presetIndex).getKey();
        int presetLabelX = panelX + arrowW + 2 + PANEL_PAD;
        int presetLabelW = panelW - arrowW * 2 - 4;
        g.drawCenteredString(this.font, presetName, panelX + PANEL_PAD + arrowW + presetLabelW / 2 + 2,
                PANEL_PAD + 20 + 6, 0xFFFFAA);

        // Render children (buttons, sliders, etc.)
        super.render(g, mouseX, mouseY, partialTick);

        // ── Export overlay ──
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
            if (key == 256) { showExport = false; return true; } // Esc
            return false;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        // Clean up GPU resources
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

    // ── Inner widgets ─────────────────────────────────────────────────────────

    /** Slider that holds a raw double value in [min, max]. */
    private static class PreviewSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;

        PreviewSlider(int x, int y, int w, int h, String label, double min, double max, double initialValue) {
            super(x, y, w, h, Component.empty(), (initialValue - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double v = getRawValue();
            String display;
            if (max - min <= 10) {
                display = String.format("%.0f", v);
            } else if (max - min <= 300) {
                display = String.format("%.1f", v);
            } else {
                display = String.format("%.3f", v);
            }
            setMessage(Component.literal(label + ": " + display));
        }

        @Override
        protected void applyValue() {
            // No-op — value read on demand via getRawValue()
        }

        public double getRawValue() {
            return min + this.value * (max - min);
        }

        public void setRawValue(double v) {
            this.value = (v - min) / (max - min);
            this.value = Math.max(0, Math.min(1, this.value));
            updateMessage();
        }
    }

    /** Simple checkbox-style button toggle. */
    private static class ToggleCheckbox extends Button {
        private boolean checked;

        ToggleCheckbox(int x, int y, int w, int h, Component label, boolean initial) {
            super(x, y, w, h, buildMsg(label.getString(), initial), b -> {
                // handled in onPress override
            }, DEFAULT_NARRATION);
            this.checked = initial;
        }

        @Override
        public void onPress() {
            checked = !checked;
            // rebuild message
            String raw = getMessage().getString();
            // strip the checkbox prefix
            String labelPart = raw.length() > 4 ? raw.substring(4) : raw;
            setMessage(buildMsg(labelPart.trim(), checked));
        }

        private static Component buildMsg(String label, boolean checked) {
            return Component.literal((checked ? "[x] " : "[ ] ") + label);
        }

        public boolean isChecked() { return checked; }

        public void setValue(boolean v) {
            if (checked != v) onPress();
        }
    }
}
