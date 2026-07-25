package net.phoenix.core.conflux.tools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class AxiomSpriteExporter {

    static final int S = 512;
    static final int CX = S / 2, CY = S / 2;

    public static void main(String[] args) throws IOException {
        String root = args.length > 0 ? args[0] : "src/main/resources/assets/phoenixcore/textures/gui/axiom";
        File outDir = new File(root);
        outDir.mkdirs();

        System.out.println("Baking Axiom sprites → " + outDir.getAbsolutePath());

        bakeVoidStars(outDir);
        bakeVoidDisk(outDir);
        bakePhoenixBg(outDir);
        bakeSculkVeins(outDir);
        bakeSealedDoc(outDir);

        System.out.println("Done.");
    }

    static BufferedImage newFrame() {
        return new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
    }

    static BufferedImage newSheet(int frames) {
        return new BufferedImage(S * frames, S, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    static void save(BufferedImage img, File dir, String name) throws IOException {
        File f = new File(dir, name);
        ImageIO.write(img, "PNG", f);
        System.out.printf("  wrote %s  (%dx%d)%n", name, img.getWidth(), img.getHeight());
    }

    static Color col(int argb) {
        int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return new Color(r, g, b, a);
    }

    static Color colA(int rgb, int alpha) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return new Color(r, g, b, Math.max(0, Math.min(255, alpha)));
    }

    static long lcg(long i) {
        return (i * 6364136223846793005L + 1442695040888963407L) ^ (i * 7919L + 99991L);
    }

    static int lerp(int a, int b, float t) {
        return a + (int) ((b - a) * t);
    }

    static Color lerpCol(Color a, Color b, float t, int alpha) {
        return new Color(lerp(a.getRed(), b.getRed(), t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(), b.getBlue(), t), alpha);
    }

    static void bakeVoidStars(File dir) throws IOException {
        BufferedImage img = newFrame();
        Graphics2D g = gfx(img);

        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, S, S);
        g.setComposite(AlphaComposite.SrcOver);

        int STAR_CNT = 210;
        for (int i = 0; i < STAR_CNT; i++) {
            long h = lcg(i);
            float sx = ((h & 0xFFFFL) / 65535f) * S;
            float sy = (((h >> 16) & 0xFFFFL) / 65535f) * S;
            float sz = (((h >> 32) & 0xFFFFL) / 65535f);
            int br = (int) (60 + sz * 195);
            if (sz > 0.85f) {

                int flareA = (int) ((sz - 0.85f) / 0.15f * 80);
                g.setColor(new Color(200, 210, 255, flareA));
                g.fillRect((int) sx - 3, (int) sy - 1, 7, 3);
                g.fillRect((int) sx - 1, (int) sy - 3, 3, 7);
            }
            g.setColor(new Color(br, br, Math.min(255, br + 30), Math.min(255, br)));
            int size = sz > 0.85f ? 2 : 1;
            g.fillRect((int) sx - size / 2, (int) sy - size / 2, size, size);
        }

        for (int nc = 0; nc < 5; nc++) {
            long nh = lcg(nc + 1000);
            float nx = ((nh & 0xFFFFL) / 65535f) * S;
            float ny = (((nh >> 16) & 0xFFFFL) / 65535f) * S;
            float nr = 40 + ((nh >> 32 & 0xFFL) / 255f) * 60;
            RadialGradientPaint nebulaGrad = new RadialGradientPaint(
                    nx, ny, nr,
                    new float[] { 0f, 1f },
                    new Color[] { new Color(20, 0, 40, 18), new Color(0, 0, 0, 0) });
            g.setPaint(nebulaGrad);
            g.fill(new Ellipse2D.Float(nx - nr, ny - nr, nr * 2, nr * 2));
        }

        g.dispose();
        save(img, dir, "void_stars.png");
    }

    static void bakeVoidDisk(File dir) throws IOException {
        int FRAMES = 32;
        BufferedImage sheet = newSheet(FRAMES);
        Graphics2D gs = gfx(sheet);
        gs.setComposite(AlphaComposite.Clear);
        gs.fillRect(0, 0, sheet.getWidth(), S);
        gs.setComposite(AlphaComposite.SrcOver);
        gs.dispose();

        float DISK_IN = 70f;
        float DISK_OUT = 118f;
        float DISK_SQSH = 0.38f;

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage frame = newFrame();
            Graphics2D g = gfx(frame);
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, S, S);
            g.setComposite(AlphaComposite.SrcOver);

            float angleOffset = (float) (f) / FRAMES * (float) (2 * Math.PI);

            int LAYERS = 10, STEPS = 140;
            for (int layer = 0; layer < LAYERS; layer++) {
                float lt = (float) layer / LAYERS;
                float rin = DISK_IN + lt * (DISK_OUT - DISK_IN) * 0.2f;
                float rout = DISK_IN + lt * (DISK_OUT - DISK_IN);
                float alpha = 0.6f - lt * 0.42f;

                Color hot = lt < 0.3f ?
                        lerpCol(new Color(255, 255, 220), new Color(255, 160, 40), lt / 0.3f, (int) (alpha * 255)) :
                        lt < 0.7f ?
                                lerpCol(new Color(255, 160, 40), new Color(200, 40, 10), (lt - 0.3f) / 0.4f,
                                        (int) (alpha * 255)) :
                                lerpCol(new Color(200, 40, 10), new Color(80, 0, 0), (lt - 0.7f) / 0.3f,
                                        (int) (alpha * 0.5f * 255));

                for (int s = 0; s < STEPS; s++) {
                    float t = (float) s / STEPS;
                    float a = angleOffset + t * (float) (2 * Math.PI);
                    float r = rin + (rout - rin) * (float) Math.sin(t * (float) Math.PI);
                    float dx = (float) (r * Math.cos(a));
                    float dy = (float) (r * Math.sin(a)) * DISK_SQSH;
                    float wobble = 1f + 0.12f * (float) Math.sin(t * 7 + layer);
                    int px = CX + (int) (dx * wobble), py = CY + (int) (dy * wobble);
                    int dotR = 2 + (layer < 3 ? 1 : 0);
                    g.setColor(hot);
                    g.fillOval(px - dotR, py - dotR, dotR * 2, dotR * 2);
                }
            }

            g.setComposite(AlphaComposite.Clear);
            g.fillOval(CX - (int) DISK_IN, (int) (CY - DISK_IN * DISK_SQSH), (int) (DISK_IN * 2),
                    (int) (DISK_IN * DISK_SQSH * 2));
            g.setComposite(AlphaComposite.SrcOver);

            Graphics2D gs2 = sheet.createGraphics();
            gs2.drawImage(frame, f * S, 0, null);
            gs2.dispose();
            g.dispose();
        }

        save(sheet, dir, "void_disk.png");
    }

    static void bakePhoenixBg(File dir) throws IOException {
        int FRAMES = 16;
        BufferedImage sheet = newSheet(FRAMES);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage frame = newFrame();
            Graphics2D g = gfx(frame);
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, S, S);
            g.setComposite(AlphaComposite.SrcOver);

            float rayAngle = (float) f / FRAMES * (float) (2 * Math.PI);

            Color[] heatCols = {
                    new Color(255, 255, 240), new Color(255, 250, 200), new Color(255, 240, 150),
                    new Color(255, 220, 80), new Color(255, 180, 40), new Color(240, 130, 20),
                    new Color(220, 80, 10), new Color(200, 50, 5), new Color(170, 25, 5),
                    new Color(140, 10, 5), new Color(110, 5, 5), new Color(80, 2, 2),
                    new Color(50, 0, 0), new Color(20, 0, 0)
            };
            int[] alphas = { 255, 230, 200, 175, 150, 130, 110, 95, 75, 55, 38, 25, 15, 8 };
            for (int layer = 13; layer >= 0; layer--) {
                float r = (layer + 1) / (14f) * CX * 1.05f;
                RadialGradientPaint rg = new RadialGradientPaint(CX, CY, r,
                        new float[] { 0f, 1f },
                        new Color[] {
                                new Color(heatCols[layer].getRed(), heatCols[layer].getGreen(),
                                        heatCols[layer].getBlue(), alphas[layer]),
                                new Color(0, 0, 0, 0) });
                g.setPaint(rg);
                g.fillOval(CX - (int) r, CY - (int) r, (int) (r * 2), (int) (r * 2));
            }

            int ARM_COUNT = 8;
            for (int arm = 0; arm < ARM_COUNT; arm++) {
                float a = rayAngle + arm * (float) (2 * Math.PI) / ARM_COUNT;
                float RAY_LEN = CX * 0.95f;
                int RAY_STEPS = 55;
                for (int s = 0; s < RAY_STEPS; s++) {
                    float t = (float) s / RAY_STEPS;
                    float r = t * RAY_LEN;
                    float intensity = 1f - t;
                    int rx = CX + (int) (r * Math.cos(a)), ry = CY + (int) (r * Math.sin(a));
                    int thick = Math.max(1, (int) (intensity * 9 * (1 - t * 0.6f)));
                    int alpha = (int) (intensity * intensity * 120);
                    g.setColor(new Color(255, 180, 60, alpha));
                    g.fillOval(rx - thick, ry - thick, thick * 2, thick * 2);
                }
            }

            RadialGradientPaint core = new RadialGradientPaint(CX, CY, 28f,
                    new float[] { 0f, 0.6f, 1f },
                    new Color[] { new Color(255, 255, 255, 255), new Color(255, 250, 220, 220),
                            new Color(255, 200, 100, 0) });
            g.setPaint(core);
            g.fillOval(CX - 28, CY - 28, 56, 56);

            Graphics2D gs = sheet.createGraphics();
            gs.drawImage(frame, f * S, 0, null);
            gs.dispose();
            g.dispose();
        }

        save(sheet, dir, "phoenix_bg.png");
    }

    static void bakeSculkVeins(File dir) throws IOException {
        BufferedImage img = newFrame();
        Graphics2D g = gfx(img);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, S, S);
        g.setComposite(AlphaComposite.SrcOver);

        float VEIN_GRID = 85f;
        int cols = (int) Math.ceil(S / VEIN_GRID) + 1, rows = (int) Math.ceil(S / VEIN_GRID) + 1;
        java.util.Random rng = new java.util.Random(0xDEAD5C01L);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float ax = col * VEIN_GRID + rng.nextFloat() * VEIN_GRID * 0.6f;
                float ay = row * VEIN_GRID + rng.nextFloat() * VEIN_GRID * 0.6f;
                int di = rng.nextInt(4);
                float bx, by;
                switch (di) {
                    case 0 -> {
                        bx = ax + VEIN_GRID + rng.nextFloat() * 40 - 20;
                        by = ay + rng.nextFloat() * 30 - 15;
                    }
                    case 1 -> {
                        bx = ax + rng.nextFloat() * 30 - 15;
                        by = ay + VEIN_GRID + rng.nextFloat() * 40 - 20;
                    }
                    case 2 -> {
                        bx = ax - VEIN_GRID * 0.5f + rng.nextFloat() * 30;
                        by = ay + VEIN_GRID * 0.6f + rng.nextFloat() * 30;
                    }
                    default -> {
                        bx = ax + VEIN_GRID * 0.7f + rng.nextFloat() * 30;
                        by = ay + VEIN_GRID * 0.5f + rng.nextFloat() * 20;
                    }
                }
                float span = Math.max(0.01f, (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay)));
                float px = -(by - ay) / span * (20 + rng.nextFloat() * 30),
                        py = (bx - ax) / span * (20 + rng.nextFloat() * 30);
                float mid = 0.3f + rng.nextFloat() * 0.4f;
                float cx0 = ax + (bx - ax) * mid + px, cy0 = ay + (by - ay) * mid + py;
                float cx1 = ax + (bx - ax) * (1 - mid) - px * 0.6f, cy1 = ay + (by - ay) * (1 - mid) - py * 0.6f;
                boolean purple = rng.nextFloat() < 0.15f;

                Path2D.Float path = new Path2D.Float();
                path.moveTo(ax, ay);
                path.curveTo(cx0, cy0, cx1, cy1, bx, by);
                int STEPS = 14;
                GeneralPath dot = null;
                for (int i = 0; i <= STEPS; i++) {
                    float t = i / (float) STEPS, u = 1 - t;
                    float ppx = u * u * u * ax + 3 * u * u * t * cx0 + 3 * u * t * t * cx1 + t * t * t * bx;
                    float ppy = u * u * u * ay + 3 * u * u * t * cy0 + 3 * u * t * t * cy1 + t * t * t * by;
                    float heat = (float) Math.sin(t * Math.PI);
                    int alpha = (int) (25 * heat + 4);
                    if (purple) g.setColor(new Color(100, 20, 160, alpha));
                    else g.setColor(new Color(0, (int) (100 * heat + 20), 40, alpha));
                    g.fillRect((int) ppx - 1, (int) ppy - 1, 2, 2);
                }
            }
        }

        g.dispose();
        save(img, dir, "sculk_veins.png");
    }

    static void bakeSealedDoc(File dir) throws IOException {
        BufferedImage img = newFrame();
        Graphics2D g = gfx(img);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, S, S);
        g.setComposite(AlphaComposite.SrcOver);

        g.setColor(new Color(26, 22, 16, 255));
        g.fillRect(S / 6, S / 8, S * 2 / 3, S * 3 / 4);

        g.setColor(new Color(150, 60, 60, 50));
        g.fillRect(S / 6 + 18, S / 8, 1, S * 3 / 4);
        g.fillRect(S * 5 / 6 - 19, S / 8, 1, S * 3 / 4);

        g.setColor(new Color(20, 15, 10, 255));
        g.fillRect(S / 6, S / 8, S * 2 / 3, 16);
        g.setColor(new Color(60, 45, 28, 255));
        g.fillRect(S / 6, S / 8 + 14, S * 2 / 3, 2);

        int marginL = S / 6 + 22, marginR = S * 5 / 6 - 22;
        int top = S / 8 + 20, bottom = S * 7 / 8 - 10;
        java.util.Random dr = new java.util.Random(0xDEADC0DEL);

        g.setColor(new Color(200, 180, 150, 60));
        g.fillRect(marginL, top + 2, 60 + dr.nextInt(40), 4);

        int y = top + 20;
        while (y < bottom - 8) {
            float r = dr.nextFloat();
            if (r < 0.30f) {

                int x0 = marginL + dr.nextInt(40);
                int x1 = Math.min(marginR, x0 + 60 + dr.nextInt(140));
                g.setColor(new Color(5, 4, 2, 255));
                g.fillRect(x0, y, x1 - x0, 5);
            } else if (r < 0.75f) {

                int x0 = marginL + dr.nextInt(20);
                int x1 = marginL + 30 + dr.nextInt(marginR - marginL - 30);
                g.setColor(new Color(170, 150, 120, 18));
                g.fillRect(x0, y, x1 - x0, 2);
            } else if (r < 0.88f) {

                g.setColor(new Color(120, 90, 60, 30));
                g.fillRect(S / 6 + 2, y, 12, 2);
            } else {
                y += 6;
                continue;
            }
            y += 9 + dr.nextInt(5);
        }

        g.setColor(new Color(14, 12, 9, 255));
        g.fillRect(0, 0, S / 6, S);
        g.fillRect(S * 5 / 6, 0, S / 6, S);
        g.fillRect(0, 0, S, S / 8);
        g.fillRect(0, S * 7 / 8, S, S / 8);

        g.dispose();
        save(img, dir, "sealed_doc.png");
    }
}
