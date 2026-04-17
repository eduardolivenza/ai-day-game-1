package com.game.map;

/** Links a door tile (col, row) on one map to a spawn point on another map. */
public class DoorConnection {
    public final int fromCol;
    public final int fromRow;
    public final String targetMapId;
    public final int targetX; // world pixel X of player spawn
    public final int targetY; // world pixel Y of player spawn

    public DoorConnection(int fromCol, int fromRow, String targetMapId, int targetX, int targetY) {
        this.fromCol    = fromCol;
        this.fromRow    = fromRow;
        this.targetMapId = targetMapId;
        this.targetX    = targetX;
        this.targetY    = targetY;
    }
}
