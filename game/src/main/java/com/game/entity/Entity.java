package com.game.entity;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Base class for all game entities (player, NPCs). */
public abstract class Entity {

    public int worldX, worldY;
    public int speed;

    // 4 directions × 3 frames: [dir][frame]
    // dir: 0=DOWN, 1=UP, 2=LEFT, 3=RIGHT
    public BufferedImage[][] sprites;
    public int direction  = 0; // default facing down
    public int animFrame  = 0;
    public int animTick   = 0;
    public static final int ANIM_SPEED = 12; // game ticks per animation frame

    // Hitbox (relative to sprite top-left, i.e. worldX/worldY)
    public final Rectangle solidArea = new Rectangle(8, 16, 32, 28);

    public static final int TILE = 48;

    public abstract void update();

    public void draw(Graphics2D g) {
        if (sprites != null
                && sprites[direction] != null
                && sprites[direction][animFrame] != null) {
            g.drawImage(sprites[direction][animFrame], worldX, worldY, TILE, TILE, null);
        }
    }

    // ── direction constants ─────────────────────────────────────────────────
    public static final int DIR_DOWN  = 0;
    public static final int DIR_UP    = 1;
    public static final int DIR_LEFT  = 2;
    public static final int DIR_RIGHT = 3;
}
