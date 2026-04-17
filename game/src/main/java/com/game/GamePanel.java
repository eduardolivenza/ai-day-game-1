package com.game;

import com.game.dialog.DialogBox;
import com.game.entity.NPC;
import com.game.entity.Player;
import com.game.input.KeyHandler;
import com.game.map.MapManager;
import com.game.tile.TileManager;
import com.game.util.Config;
import com.game.util.SaveManager;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {

    // ── screen constants ──────────────────────────────────────────────────
    public static final int TILE_SIZE     = 48;
    public static final int SCREEN_COLS   = 16;
    public static final int SCREEN_ROWS   = 12;
    public static final int SCREEN_WIDTH  = TILE_SIZE * SCREEN_COLS;  // 768
    public static final int SCREEN_HEIGHT = TILE_SIZE * SCREEN_ROWS;  // 576

    // ── camera ─────────────────────────────────────────────────────────
    public int cameraX = 0;
    public int cameraY = 0;

    // ── game state ──────────────────────────────────────────────────────
    public GameState gameState = GameState.PLAYING;

    // ── components ──────────────────────────────────────────────────────
    public final KeyHandler  keyHandler;
    public final TileManager tileManager;
    public final MapManager  mapManager;
    public final Player      player;
    public final DialogBox   dialogBox;
    public final SaveManager saveManager;

    private Thread gameThread;

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        keyHandler  = new KeyHandler();
        tileManager = new TileManager();

        addKeyListener(keyHandler);
        setFocusable(true);

        Config config = new Config();
        mapManager  = new MapManager(this);
        player      = new Player(this);
        dialogBox   = new DialogBox(this, config);
        saveManager = new SaveManager(this);

        saveManager.loadGame();

        // Save on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(saveManager::saveGame));
    }

    public void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.setDaemon(true);
        gameThread.start();
    }

    // ── game loop (fixed timestep, ~60 FPS) ──────────────────────────────

    @Override
    public void run() {
        double interval = 1_000_000_000.0 / 60;
        double delta    = 0;
        long   last     = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last   = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

            try { Thread.sleep(1); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── update ─────────────────────────────────────────────────────────

    private void update() {
        switch (gameState) {
            case PLAYING -> {
                player.update();
                for (NPC npc : mapManager.getCurrentMap().getNpcs())
                    npc.update();
                updateCamera();
            }
            case DIALOG     -> dialogBox.update();
            case TRANSITION -> mapManager.updateTransition();
        }
    }

    // ── rendering ──────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,   RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // World-space rendering (shifted by camera)
        g2.translate(-cameraX, -cameraY);
        mapManager.draw(g2);
        // Draw NPCs then player (player on top)
        for (NPC npc : mapManager.getCurrentMap().getNpcs())
            npc.draw(g2);
        player.draw(g2);
        g2.translate(cameraX, cameraY);

        // Screen-space UI
        drawHUD(g2);
        if (gameState == GameState.DIALOG)
            dialogBox.draw(g2);
        if (gameState == GameState.TRANSITION)
            mapManager.drawTransition(g2);
    }

    private void drawHUD(Graphics2D g) {
        // Map name badge
        String mapName = switch (mapManager.getCurrentMap().getId()) {
            case "house1"    -> "Brom's Inn";
            case "house2"    -> "Aelara's Study";
            case "house3"    -> "Empty house";
            default          -> "The Village of Elmswick";
        };
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(mapName);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(SCREEN_WIDTH / 2 - tw / 2 - 8, 8, tw + 16, 22, 8, 8);
        g.setColor(new Color(0xd4af37));
        g.drawString(mapName, SCREEN_WIDTH / 2 - tw / 2, 24);

        // Talk hint
        for (NPC npc : mapManager.getCurrentMap().getNpcs()) {
            if (npc.playerNearby && gameState == GameState.PLAYING) {
                String hint = "Press ENTER to talk to " + npc.name;
                int hw = g.getFontMetrics().stringWidth(hint);
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRoundRect(SCREEN_WIDTH / 2 - hw / 2 - 8, SCREEN_HEIGHT - 36, hw + 16, 22, 8, 8);
                g.setColor(Color.WHITE);
                g.drawString(hint, SCREEN_WIDTH / 2 - hw / 2, SCREEN_HEIGHT - 20);
                break;
            }
        }
    }

    // ── camera ─────────────────────────────────────────────────────────

    private void updateCamera() {
        int mapW = mapManager.getCurrentMap().getWidth()  * TILE_SIZE;
        int mapH = mapManager.getCurrentMap().getHeight() * TILE_SIZE;
        int tx   = player.worldX - SCREEN_WIDTH  / 2 + TILE_SIZE / 2;
        int ty   = player.worldY - SCREEN_HEIGHT / 2 + TILE_SIZE / 2;
        cameraX  = Math.max(0, Math.min(tx, mapW - SCREEN_WIDTH));
        cameraY  = Math.max(0, Math.min(ty, mapH - SCREEN_HEIGHT));
    }
}
