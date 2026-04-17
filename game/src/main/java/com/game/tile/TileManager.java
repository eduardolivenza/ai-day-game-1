package com.game.tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Generates all tile images procedurally and stores them by ID:
 *  0 = GRASS      (not solid)
 *  1 = WALL       (solid – stone/rock exterior)
 *  2 = WATER      (solid)
 *  3 = PATH       (not solid)
 *  4 = DOOR       (not solid – transition trigger)
 *  5 = TREE       (solid)
 *  6 = SAND       (not solid)
 *  7 = FLOOR      (not solid – interior wood)
 *  8 = INT_WALL   (solid – interior wall)
 *  9 = FLOWER     (not solid)
 */
public class TileManager {

    public static final int T = 48; // tile size in pixels
    public final Tile[] tiles = new Tile[10];

    public TileManager() {
        tiles[0] = new Tile(grass(),      false);
        tiles[1] = new Tile(wall(),       true);
        tiles[2] = new Tile(water(),      true);
        tiles[3] = new Tile(path(),       false);
        tiles[4] = new Tile(door(),       false);
        tiles[5] = new Tile(tree(),       true);
        tiles[6] = new Tile(sand(),       false);
        tiles[7] = new Tile(floor(),      false);
        tiles[8] = new Tile(intWall(),    true);
        tiles[9] = new Tile(flower(),     false);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static BufferedImage make() {
        return new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D g2(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return g;
    }

    // ── tile generators ──────────────────────────────────────────────────────

    private BufferedImage grass() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0x4e8b52));
        g.fillRect(0, 0, T, T);
        // Subtle darker patches
        g.setColor(new Color(0x3d7040));
        Random r = new Random(1);
        for (int i = 0; i < 10; i++)
            g.fillRect(r.nextInt(42), r.nextInt(42), r.nextInt(4) + 2, r.nextInt(4) + 2);
        // Lighter blades
        g.setColor(new Color(0x65a86a));
        for (int i = 0; i < 6; i++)
            g.fillRect(r.nextInt(44), r.nextInt(44), 2, 4);
        g.dispose();
        return img;
    }

    private BufferedImage wall() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0x7a7060));
        g.fillRect(0, 0, T, T);
        // Brick pattern
        g.setColor(new Color(0x5a5248));
        g.drawLine(0, 12, T, 12);
        g.drawLine(0, 24, T, 24);
        g.drawLine(0, 36, T, 36);
        g.drawLine(24, 0,  24, 12);
        g.drawLine(12, 12, 12, 24);
        g.drawLine(36, 12, 36, 24);
        g.drawLine(24, 24, 24, 36);
        g.drawLine(12, 36, 12, T);
        g.drawLine(36, 36, 36, T);
        // Highlight
        g.setColor(new Color(0x9a9080));
        g.drawLine(1, 1, 22, 1);
        g.drawLine(1, 13, 10, 13);
        g.drawLine(25, 13, 34, 13);
        g.dispose();
        return img;
    }

    private BufferedImage water() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0x2878c0));
        g.fillRect(0, 0, T, T);
        // Wave lines
        g.setColor(new Color(0x4fa0e0));
        for (int row = 0; row < 3; row++) {
            int y = 6 + row * 16;
            for (int x = 0; x < T; x += 8)
                g.drawArc(x, y, 8, 4, 0, 180);
        }
        g.setColor(new Color(0x1a5898));
        g.drawLine(0, T - 2, T, T - 2);
        g.dispose();
        return img;
    }

    private BufferedImage path() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0xc0a070));
        g.fillRect(0, 0, T, T);
        g.setColor(new Color(0xa88858));
        Random r = new Random(3);
        for (int i = 0; i < 14; i++)
            g.fillOval(r.nextInt(44), r.nextInt(44), r.nextInt(4) + 2, 2);
        g.dispose();
        return img;
    }

    private BufferedImage door() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        // Frame
        g.setColor(new Color(0x7a5c30));
        g.fillRect(0, 0, T, T);
        // Door opening (dark wood)
        g.setColor(new Color(0x4a3010));
        g.fillRect(10, 8, 28, 40);
        // Panels
        g.setColor(new Color(0x6a4820));
        g.drawRect(12, 10, 10, 16);
        g.drawRect(26, 10, 10, 16);
        g.drawRect(12, 28, 10, 16);
        g.drawRect(26, 28, 10, 16);
        // Knob
        g.setColor(new Color(0xd4af37));
        g.fillOval(32, 26, 5, 5);
        g.dispose();
        return img;
    }

    private BufferedImage tree() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        // Grass base
        g.setColor(new Color(0x4e8b52));
        g.fillRect(0, 0, T, T);
        // Trunk
        g.setColor(new Color(0x6b3c0f));
        g.fillRect(19, 30, 10, 18);
        // Canopy shadow
        g.setColor(new Color(0x1a4a15));
        g.fillOval(5, 4, 38, 34);
        // Canopy mid
        g.setColor(new Color(0x2d7022));
        g.fillOval(7, 2, 34, 30);
        // Highlight
        g.setColor(new Color(0x48a03a));
        g.fillOval(12, 4, 18, 14);
        g.dispose();
        return img;
    }

    private BufferedImage sand() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0xe8d090));
        g.fillRect(0, 0, T, T);
        g.setColor(new Color(0xd0b870));
        Random r = new Random(6);
        for (int i = 0; i < 12; i++)
            g.fillOval(r.nextInt(44), r.nextInt(44), 3, 2);
        g.dispose();
        return img;
    }

    private BufferedImage floor() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0xd4a86a));
        g.fillRect(0, 0, T, T);
        // Planks
        g.setColor(new Color(0xb88a50));
        for (int y = 0; y < T; y += 16)
            g.drawLine(0, y, T, y);
        // Grain
        g.setColor(new Color(0xc89858));
        for (int x = 6; x < T; x += 12)
            g.drawLine(x, 0, x - 3, T);
        g.dispose();
        return img;
    }

    private BufferedImage intWall() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        g.setColor(new Color(0x9e8c7a));
        g.fillRect(0, 0, T, T);
        g.setColor(new Color(0x7a6a58));
        g.drawLine(0, 12, T, 12);
        g.drawLine(0, 24, T, 24);
        g.drawLine(0, 36, T, 36);
        g.drawLine(24, 0,  24, 12);
        g.drawLine(12, 12, 12, 24);
        g.drawLine(36, 12, 36, 24);
        g.drawLine(24, 24, 24, 36);
        g.drawLine(12, 36, 12, T);
        g.drawLine(36, 36, 36, T);
        g.setColor(new Color(0xb8a090));
        g.drawLine(1, 1, 22, 1);
        g.dispose();
        return img;
    }

    private BufferedImage flower() {
        BufferedImage img = make();
        Graphics2D g = g2(img);
        // Grass base
        g.setColor(new Color(0x4e8b52));
        g.fillRect(0, 0, T, T);
        // Stem
        g.setColor(new Color(0x3a6b30));
        g.fillRect(23, 20, 2, 20);
        // Petals
        g.setColor(new Color(0xff88aa));
        g.fillOval(16, 10, 10, 10);
        g.fillOval(22, 8,  10, 10);
        g.fillOval(28, 10, 10, 10);
        g.fillOval(14, 16, 10, 10);
        g.fillOval(30, 16, 10, 10);
        // Center
        g.setColor(new Color(0xffdd22));
        g.fillOval(19, 13, 10, 10);
        g.dispose();
        return img;
    }
}
