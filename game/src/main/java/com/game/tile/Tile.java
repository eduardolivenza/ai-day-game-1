package com.game.tile;

import java.awt.image.BufferedImage;

public class Tile {
    public final BufferedImage image;
    public final boolean solid;

    public Tile(BufferedImage image, boolean solid) {
        this.image = image;
        this.solid = solid;
    }
}
