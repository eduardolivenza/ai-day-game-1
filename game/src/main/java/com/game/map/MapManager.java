package com.game.map;

import com.game.GamePanel;
import com.game.GameState;
import com.game.dialog.GameApiClient;
import com.game.entity.NPC;
import com.game.entity.NpcDefinition;
import com.game.tile.TileManager;
import com.game.util.Config;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MapManager {

    private record SpawnSlot(String mapId, int col, int row) {}

    private static final List<SpawnSlot> SPAWN_SLOTS = List.of(
        new SpawnSlot("overworld", 14, 10),
        new SpawnSlot("overworld",  5, 13),
        new SpawnSlot("overworld", 20, 10),
        new SpawnSlot("overworld",  5, 10),
        new SpawnSlot("house1",     5,  3),
        new SpawnSlot("house2",     7,  3),
        new SpawnSlot("house3",     5,  3)
    );

    private final GamePanel gp;
    private final java.util.Map<String, TileMap> maps = new HashMap<>();
    private TileMap currentMap;

    // Transition state
    private DoorConnection pendingDoor;
    private float transitionAlpha = 0f;
    private enum Phase { NONE, FADE_OUT, FADE_IN }
    private Phase phase = Phase.NONE;

    // All NPCs (for save/load access)
    private final java.util.Map<String, NPC> npcRegistry = new HashMap<>();

    public MapManager(GamePanel gp, Config config) {
        this.gp = gp;
        buildMaps();
        List<NpcDefinition> defs = new GameApiClient(config).fetchNpcDefinitions();
        placeNpcs(defs);
        currentMap = maps.get("overworld");
    }

    public TileMap getCurrentMap()  { return currentMap; }
    public TileMap getMap(String id) { return maps.get(id); }
    public java.util.Map<String, NPC> getNpcRegistry() { return npcRegistry; }

    public void setCurrentMapDirect(String id) {
        TileMap m = maps.get(id);
        if (m != null) currentMap = m;
    }

    public boolean isSolid(int col, int row) {
        int id = currentMap.getTile(col, row);
        return gp.tileManager.tiles[id].solid;
    }

    // ── rendering ─────────────────────────────────────────────────────────

    public void draw(Graphics2D g) {
        int T = TileManager.T;
        int cols = currentMap.getWidth();
        int rows = currentMap.getHeight();

        int startCol = gp.cameraX / T;
        int startRow = gp.cameraY / T;
        int endCol   = Math.min(startCol + GamePanel.SCREEN_COLS + 1, cols);
        int endRow   = Math.min(startRow + GamePanel.SCREEN_ROWS + 1, rows);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int tileId = currentMap.getTile(col, row);
                g.drawImage(gp.tileManager.tiles[tileId].image, col * T, row * T, null);
            }
        }
    }

    // ── transition ────────────────────────────────────────────────────────

    public void startTransition(DoorConnection door) {
        pendingDoor    = door;
        phase          = Phase.FADE_OUT;
        transitionAlpha = 0f;
        gp.gameState   = GameState.TRANSITION;
    }

    public void updateTransition() {
        float speed = 0.05f;
        switch (phase) {
            case FADE_OUT -> {
                transitionAlpha += speed;
                if (transitionAlpha >= 1f) {
                    transitionAlpha = 1f;
                    // Switch map
                    currentMap = maps.get(pendingDoor.targetMapId);
                    gp.player.worldX = pendingDoor.targetX;
                    gp.player.worldY = pendingDoor.targetY;
                    gp.cameraX = 0;
                    gp.cameraY = 0;
                    phase = Phase.FADE_IN;
                }
            }
            case FADE_IN -> {
                transitionAlpha -= speed;
                if (transitionAlpha <= 0f) {
                    transitionAlpha = 0f;
                    phase           = Phase.NONE;
                    gp.gameState    = GameState.PLAYING;
                }
            }
        }
    }

    public void drawTransition(Graphics2D g) {
        if (phase == Phase.NONE) return;
        g.setColor(new Color(0, 0, 0, (int)(transitionAlpha * 255)));
        g.fillRect(0, 0, GamePanel.SCREEN_WIDTH, GamePanel.SCREEN_HEIGHT);
    }

    // ── map construction ──────────────────────────────────────────────────

    private void buildMaps() {
        TileMap overworld = buildOverworld();
        TileMap house1    = buildHouse("house1");
        TileMap house2    = buildHouse("house2");
        TileMap house3    = buildHouse("house3");

        // Door: overworld (3,4) → house1, spawn at col=5,row=7
        overworld.addDoor(new DoorConnection(3, 4, "house1",
                5 * TileManager.T, 7 * TileManager.T));
        // Door: overworld (19,4) → house2, spawn at col=5,row=7
        overworld.addDoor(new DoorConnection(19, 4, "house2",
                5 * TileManager.T, 7 * TileManager.T));
        overworld.addDoor(new DoorConnection(10, 4, "house3",
                5 * TileManager.T, 7 * TileManager.T));

        // Door: house1 (5,8) → overworld, spawn in front of left house
        house1.addDoor(new DoorConnection(5, 8, "overworld",
                3 * TileManager.T, 5 * TileManager.T));
        // Door: house2 (5,8) → overworld, spawn in front of right house
        house2.addDoor(new DoorConnection(5, 8, "overworld",
                19 * TileManager.T, 5 * TileManager.T));
        house3.addDoor(new DoorConnection(5, 8, "overworld",
                10 * TileManager.T, 5 * TileManager.T));

        maps.put("overworld", overworld);
        maps.put("house1",    house1);
        maps.put("house2",    house2);
        maps.put("house3",    house3);
    }

    private void placeNpcs(List<NpcDefinition> defs) {
        List<SpawnSlot> slots = new ArrayList<>(SPAWN_SLOTS);
        Collections.shuffle(slots);
        int count = Math.min(defs.size(), slots.size());
        for (int i = 0; i < count; i++) {
            NpcDefinition def  = defs.get(i);
            SpawnSlot     slot = slots.get(i);
            NPC npc = new NPC(def.name(), slot.col(), slot.row(), def.shirtColor());
            maps.get(slot.mapId()).addNpc(npc);
            npcRegistry.put(def.name(), npc);
        }
    }

    // ── tile data ─────────────────────────────────────────────────────────
    //  0=GRASS 1=WALL 2=WATER 3=PATH 4=DOOR 5=TREE 6=SAND 7=FLOOR 8=INT_WALL 9=FLOWER

    private TileMap buildOverworld() {
        int[][] data = {
            // 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
            {  5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 }, // row 0
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 1
            {  5, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 5 }, // row 2
            {  5, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 5 }, // row 3
            {  5, 0, 1, 4, 1, 0, 0, 0, 0, 1, 4, 1, 0, 0, 0, 0, 0, 0, 1, 4, 1, 0, 0, 0, 5 }, // row 4  doors
            {  5, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 5 }, // row 5
            {  5, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 5 }, // row 6
            {  5, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0, 5 }, // row 7  main path
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 8
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 9  lake top
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 10
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 11
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 12 lake bot
            {  5, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 5 }, // row 13
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 14 player start
            {  5, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 5 }, // row 15
            {  5, 0, 0, 0, 0, 5, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 16
            {  5, 0, 0, 0, 0, 5, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 5 }, // row 17
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 18
            {  5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 }, // row 19
        };
        return new TileMap("overworld", data);
    }

    private TileMap buildHouse(String id) {
        int[][] data = {
            { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 }, // row 0
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 1
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 2
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 3
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 4
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 5
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 6
            { 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8 }, // row 7
            { 8, 8, 8, 8, 8, 4, 8, 8, 8, 8, 8, 8 }, // row 8  door at col 5
            { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 }, // row 9
        };
        return new TileMap(id, data);
    }
}
