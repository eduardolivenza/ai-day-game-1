package com.game.map;

import com.game.GamePanel;
import com.game.GameState;
import com.game.entity.NPC;
import com.game.tile.TileManager;

import java.awt.*;
import java.util.HashMap;

public class MapManager {

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

    public MapManager(GamePanel gp) {
        this.gp = gp;
        buildMaps();
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

        // Door: overworld (3,4) → house1, spawn at col=5,row=7
        overworld.addDoor(new DoorConnection(3, 4, "house1",
                5 * TileManager.T, 7 * TileManager.T));
        // Door: overworld (19,4) → house2, spawn at col=5,row=7
        overworld.addDoor(new DoorConnection(19, 4, "house2",
                5 * TileManager.T, 7 * TileManager.T));
        // Door: house1 (5,8) → overworld, spawn in front of left house
        house1.addDoor(new DoorConnection(5, 8, "overworld",
                3 * TileManager.T, 5 * TileManager.T));
        // Door: house2 (5,8) → overworld, spawn in front of right house
        house2.addDoor(new DoorConnection(5, 8, "overworld",
                19 * TileManager.T, 5 * TileManager.T));

        // ── NPCs ──────────────────────────────────────────────────────────

        NPC elderOryn = new NPC(
                "Elder Oryn",
                "You are Elder Oryn, an ancient and wise guardian of a small village. "
                + "You have lived for centuries and know many hidden secrets of the land. "
                + "You speak warmly but with gravitas. You often share cryptic hints about "
                + "a great darkness stirring in the east. Keep all replies to 2-3 sentences.",
                14, 10,
                new Color(0x708090));  // slate gray robe

        NPC mira = new NPC(
                "Mira",
                "You are Mira, a cheerful 12-year-old girl who loves flowers and is the "
                + "self-appointed village gossip. You know everything that happens around here. "
                + "You speak with excitement and often ask the adventurer questions. "
                + "Keep all replies to 2-3 sentences.",
                5, 13,
                new Color(0xff8fb0));   // pink

        NPC brom = new NPC(
                "Brom",
                "You are Brom, the gruff but kind innkeeper and merchant who runs the local inn. "
                + "You are always trying to sell something or strike a deal. "
                + "You speak directly and are slightly suspicious of strangers at first. "
                + "Keep all replies to 2-3 sentences.",
                5, 3,
                new Color(0x8b5e2a));   // brown vest

        NPC aelara = new NPC(
                "Aelara",
                "You are Aelara, a mysterious elven sage who dwells alone. "
                + "You have visions of the future and speak in riddles and metaphors. "
                + "You sense great potential in the adventurer. "
                + "Keep all replies to 2-3 sentences.",
                7, 3,
                new Color(0x6040a0));   // purple robe

        overworld.addNpc(elderOryn);
        overworld.addNpc(mira);
        house1.addNpc(brom);
        house2.addNpc(aelara);

        npcRegistry.put("Elder Oryn", elderOryn);
        npcRegistry.put("Mira",       mira);
        npcRegistry.put("Brom",       brom);
        npcRegistry.put("Aelara",     aelara);

        maps.put("overworld", overworld);
        maps.put("house1",    house1);
        maps.put("house2",    house2);
    }

    // ── tile data ─────────────────────────────────────────────────────────
    //  0=GRASS 1=WALL 2=WATER 3=PATH 4=DOOR 5=TREE 6=SAND 7=FLOOR 8=INT_WALL 9=FLOWER

    private TileMap buildOverworld() {
        int[][] data = {
            // 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
            {  5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 }, // row 0
            {  5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5 }, // row 1
            {  5, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 5 }, // row 2
            {  5, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 5 }, // row 3
            {  5, 0, 1, 4, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 1, 0, 0, 0, 5 }, // row 4  doors
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
            {  5, 0, 0, 0, 0, 5, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 5, 0, 0, 0, 0, 0, 5 }, // row 16
            {  5, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 5 }, // row 17
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
