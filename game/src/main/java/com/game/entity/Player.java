package com.game.entity;

import com.game.GamePanel;
import com.game.GameState;
import com.game.input.KeyHandler;
import com.game.map.DoorConnection;
import com.game.map.TileMap;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    private final GamePanel gp;
    private final KeyHandler keys;

    // Talk cooldown: prevents instantly re-triggering dialog
    private int talkCooldown = 0;

    public Player(GamePanel gp) {
        this.gp    = gp;
        this.keys  = gp.keyHandler;
        this.speed = 4;
        // Default spawn (overridden by save or map transition)
        this.worldX = 11 * TILE;
        this.worldY = 14 * TILE;
        generateSprites();
    }

    @Override
    public void update() {
        if (talkCooldown > 0) talkCooldown--;

        boolean moving = keys.up || keys.down || keys.left || keys.right;

        // Determine facing direction
        if      (keys.up)    direction = DIR_UP;
        else if (keys.down)  direction = DIR_DOWN;
        else if (keys.left)  direction = DIR_LEFT;
        else if (keys.right) direction = DIR_RIGHT;

        // Move
        if (moving) {
            int dx = 0, dy = 0;
            if (keys.left)  dx = -speed;
            if (keys.right) dx =  speed;
            if (keys.up)    dy = -speed;
            if (keys.down)  dy =  speed;

            if (!wouldCollide(dx, 0)) worldX += dx;
            if (!wouldCollide(0, dy)) worldY += dy;

            // Animate
            animTick++;
            if (animTick >= ANIM_SPEED) {
                animTick  = 0;
                animFrame = (animFrame + 1) % 3;
            }
        } else {
            animFrame = 0;
        }

        // Check door
        checkDoor();

        // Check talk and vault proximity
        checkTalkProximity();
        checkVaultProximity();
        if (keys.enterPressed) {
            keys.consumeEnter();
            if (talkCooldown == 0) {
                NPC nearby = getNearestTalkableNPC();
                if (nearby != null) {
                    talkCooldown = 30;
                    gp.dialogBox.startWith(nearby);
                    gp.gameState = GameState.DIALOG;
                } else if (gp.vaultManager.playerNearby) {
                    talkCooldown = 30;
                    gp.openVault();
                }
            }
        }
    }

    // ── collision ─────────────────────────────────────────────────────────

    private boolean wouldCollide(int dx, int dy) {
        TileMap map = gp.mapManager.getCurrentMap();
        int left   = worldX + solidArea.x + dx;
        int right  = worldX + solidArea.x + solidArea.width  + dx;
        int top    = worldY + solidArea.y + dy;
        int bottom = worldY + solidArea.y + solidArea.height + dy;

        int tl = map.getTile(left  / TILE, top    / TILE);
        int tr = map.getTile(right / TILE, top    / TILE);
        int bl = map.getTile(left  / TILE, bottom / TILE);
        int br = map.getTile(right / TILE, bottom / TILE);

        if (gp.tileManager.tiles[tl].solid) return true;
        if (gp.tileManager.tiles[tr].solid) return true;
        if (gp.tileManager.tiles[bl].solid) return true;
        if (gp.tileManager.tiles[br].solid) return true;

        // NPC collision – skip if player is already overlapping this NPC
        // (allows escaping after dialog without getting stuck)
        for (NPC npc : map.getNpcs()) {
            Rectangle npcBox = new Rectangle(
                    npc.worldX + npc.solidArea.x,
                    npc.worldY + npc.solidArea.y,
                    npc.solidArea.width,
                    npc.solidArea.height);
            Rectangle currentBox = new Rectangle(
                    worldX + solidArea.x,
                    worldY + solidArea.y,
                    solidArea.width,
                    solidArea.height);
            if (currentBox.intersects(npcBox)) continue; // already overlapping – let player move away
            Rectangle nextBox = new Rectangle(left, top, solidArea.width, solidArea.height);
            if (nextBox.intersects(npcBox)) return true;
        }

        return false;
    }

    // ── door transition ───────────────────────────────────────────────────

    private void checkDoor() {
        TileMap map = gp.mapManager.getCurrentMap();
        int col = (worldX + solidArea.x + solidArea.width  / 2) / TILE;
        int row = (worldY + solidArea.y + solidArea.height / 2) / TILE;

        if (map.getTile(col, row) == 4) { // DOOR tile
            DoorConnection door = map.getDoorAt(col, row);
            if (door != null && gp.gameState == GameState.PLAYING) {
                gp.mapManager.startTransition(door);
            }
        }
    }

    // ── NPC proximity ─────────────────────────────────────────────────────

    private void checkTalkProximity() {
        double talkRange = TILE * 1.6;
        int cx = worldX + TILE / 2;
        int cy = worldY + TILE / 2;
        for (NPC npc : gp.mapManager.getCurrentMap().getNpcs()) {
            int nx = npc.worldX + TILE / 2;
            int ny = npc.worldY + TILE / 2;
            double dist = Math.hypot(cx - nx, cy - ny);
            npc.playerNearby = (dist <= talkRange);
        }
    }

    private void checkVaultProximity() {
        if (!"overworld".equals(gp.mapManager.getCurrentMap().getId())) {
            gp.vaultManager.playerNearby = false;
            return;
        }
        var loc = gp.vaultManager.getCurrent();
        int cx = worldX + TILE / 2;
        int cy = worldY + TILE / 2;
        int vx = loc.col() * TILE + TILE / 2;
        int vy = loc.row() * TILE + TILE / 2;
        gp.vaultManager.playerNearby = (Math.hypot(cx - vx, cy - vy) <= TILE * 1.6);
    }

    private NPC getNearestTalkableNPC() {
        double talkRange = TILE * 1.6;
        int cx = worldX + TILE / 2;
        int cy = worldY + TILE / 2;
        NPC nearest = null;
        double nearest_d = Double.MAX_VALUE;
        for (NPC npc : gp.mapManager.getCurrentMap().getNpcs()) {
            int nx = npc.worldX + TILE / 2;
            int ny = npc.worldY + TILE / 2;
            double dist = Math.hypot(cx - nx, cy - ny);
            if (dist <= talkRange && dist < nearest_d) {
                nearest   = npc;
                nearest_d = dist;
            }
        }
        return nearest;
    }

    // ── sprite generation ─────────────────────────────────────────────────

    private void generateSprites() {
        Color shirt = new Color(0x3a6bc1); // player blue
        Color pants = new Color(0x2a2a6a);
        Color skin  = new Color(0xFFDBAA);
        Color hair  = new Color(0x5c3d1e);

        sprites = new BufferedImage[4][3];
        for (int dir = 0; dir < 4; dir++) {
            for (int frame = 0; frame < 3; frame++) {
                BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                switch (dir) {
                    case DIR_DOWN  -> NPC.drawFront(g, shirt, pants, skin, hair, frame);
                    case DIR_UP    -> NPC.drawBack (g, shirt, pants, skin, hair, frame);
                    case DIR_LEFT  -> NPC.drawSide (g, shirt, pants, skin, hair, frame, true);
                    case DIR_RIGHT -> NPC.drawSide (g, shirt, pants, skin, hair, frame, false);
                }
                g.dispose();
                sprites[dir][frame] = img;
            }
        }
    }
}
