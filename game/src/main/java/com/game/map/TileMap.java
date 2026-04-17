package com.game.map;

import com.game.entity.NPC;

import java.util.ArrayList;
import java.util.List;

/** Immutable tile data for one map plus its door connections and NPC list. */
public class TileMap {

    private final String id;
    private final int[][] tiles;   // [row][col]
    private final int width;
    private final int height;
    private final List<DoorConnection> doors = new ArrayList<>();
    private final List<NPC> npcs = new ArrayList<>();

    public TileMap(String id, int[][] tiles) {
        this.id     = id;
        this.tiles  = tiles;
        this.height = tiles.length;
        this.width  = (tiles.length > 0) ? tiles[0].length : 0;
    }

    public String getId()  { return id; }
    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public int getTile(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return 1; // treat OOB as solid
        return tiles[row][col];
    }

    public void addDoor(DoorConnection d) { doors.add(d); }
    public List<DoorConnection> getDoors() { return doors; }

    public void addNpc(NPC npc) { npcs.add(npc); }
    public List<NPC> getNpcs()  { return npcs; }

    /** Returns the DoorConnection for a given tile position, or null. */
    public DoorConnection getDoorAt(int col, int row) {
        for (DoorConnection d : doors)
            if (d.fromCol == col && d.fromRow == row) return d;
        return null;
    }
}
